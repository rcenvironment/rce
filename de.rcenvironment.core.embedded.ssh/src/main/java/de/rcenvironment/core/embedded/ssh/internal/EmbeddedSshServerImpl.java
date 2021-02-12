/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.embedded.ssh.api.EmbeddedSshServerControl;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;
import de.rcenvironment.core.embedded.ssh.internal.IncomingSessionTracker.SessionHandle;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLogIds;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Implementation of an embedded SSH server with OSGi lifecycle methods.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden (added public key authentication)
 */
@Component(immediate = true)
public class EmbeddedSshServerImpl implements EmbeddedSshServerControl {

    private static final int INCREASE_BY_ONE = 1;

    private static final int DECREASE_BY_ONE = -1;

    private static final int DYNAMIC_ACCOUNT_FILE_MODIFICATION_CHECK_INTERVAL_MSEC = 10 * 1000;

    private static final String HOST_KEY_STORAGE_FILE_NAME = "ssh_host_key.dat";

    private static final String EVENT_LOG_KEY_CONNECTION_TYPE = "type";

    private static final String EVENT_LOG_VALUE_CONNECTION_TYPE = "ssh/uplink";

    private static final int NUM_ALLOWED_AUTH_ATTEMPTS = 3; // TODO hardcoded for now; make configurable?

    private ConfigurationService configurationService;

    private CommandExecutionService commandExecutionService;

    private SshConfiguration sshConfiguration;

    private File hostKeyStorageDirectory; // used at activation only

    private ScpContextManager scpContextManager;

    private ServerSideUplinkSessionService uplinkSessionService;

    private SshServer sshd;

    private boolean sshServerEnabled = false; // enabled by configuration?

    private boolean sshServerRunning = false; // actually running?

    private final Map<String, String> announcedVersionEntries = new HashMap<>();

    private final IncomingSessionTracker<Session> sessionTracker = new IncomingSessionTracker<Session>(EVENT_LOG_VALUE_CONNECTION_TYPE);

    private final Log logger = LogFactory.getLog(getClass());

    private Path dynamicAccountsFilePath;

    private AsyncTaskService asyncTaskService;

    private FileTime dynamicAccountsFileLastModified;

    /**
     * OSGi-DS life-cycle method.
     */
    @Activate
    public void activate() {

        // TODO rework this to service injection; must be injected by tests, too
        asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

        if (!CommandLineArguments.isConfigurationShellRequested()) {

            // configure from live environment
            ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("sshServer");
            try {
                sshConfiguration = new SshConfiguration(configurationSegment);
            } catch (ConfigurationException | IOException e) {
                sshConfiguration = new SshConfiguration();
                logger.error(e.getMessage());
            }
            hostKeyStorageDirectory = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA);

            // select where to look for a separate dynamic accounts file; existence of this file is checked in performStartup()
            dynamicAccountsFilePath =
                configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_CONFIGURATION_DATA).toPath().resolve("accounts.json");

            asyncTaskService.execute("Embedded SSH server startup", this::performStartup);
        }
    }

    /**
     * An entry point for integration tests to set up a live SSH server without mocking the whole {@link ConfigurationService}.
     * 
     * @param configuration the SSH configuration segment
     * @param customAccountsFilePath the path to a test accounts file, or null to leave this feature disabled
     * @param hostKeyStorageDir the directory that the SSH server will store its host key file in
     * @param serverSideUplinkSessionService the {@link ServerSideUplinkEndpointService} implementation to use
     */
    public void applyMockConfigurationAndStart(SshConfiguration configuration, Path customAccountsFilePath,
        File hostKeyStorageDir, ServerSideUplinkSessionService serverSideUplinkSessionService) {

        // configure from mock data
        this.sshConfiguration = configuration;
        this.dynamicAccountsFilePath = customAccountsFilePath;
        this.hostKeyStorageDirectory = hostKeyStorageDir;

        // substitute OSGi bind injection
        this.uplinkSessionService = serverSideUplinkSessionService;

        // start synchronously
        performStartup();
    }

    /**
     * Verifies if the server was started normally and is still running.
     * 
     * @return false if the server was never started, already stopped, or if startup failed, e.g. on a port collision
     */
    public boolean isRunning() {
        return sshServerRunning;
    }

    /**
     * OSGi-DS lifecycle method.
     */
    @Deactivate
    public void deactivate() {
        performShutdown();
    }

    @Override
    public synchronized void setAnnouncedVersionOrProperty(String key, String value) {
        announcedVersionEntries.put(key, value);
        if (sshServerEnabled) {
            updateServerBannerWithAnnouncementData(sshd);
        }
    }

    private synchronized void performStartup() {

        sshServerEnabled = getActivationSettingFromConfig(sshConfiguration);
        if (!sshServerEnabled) {
            logger.debug("Not running an SSH server as there is either no SSH configuration at all, "
                + "or the \"enabled\" property is not \"true\", or the configuration data (including account settings) has errors");
            return;
        }

        sshd = createSSHServerAndApplySettings();

        // includes loading the list of static accounts
        SshAuthenticationManager authenticationManager =
            new SshAuthenticationManager(sshConfiguration, sessionTracker, NUM_ALLOWED_AUTH_ATTEMPTS);

        // path may be null to allow tests to disable dynamic account loading
        if (dynamicAccountsFilePath != null && Files.exists(dynamicAccountsFilePath)) {
            try {
                dynamicAccountsFileLastModified = Files.getLastModifiedTime(dynamicAccountsFilePath);
                Map<String, ConfigurationSegment> sshAccounts = attemptToLoadDynamicAccountData();
                applyDynamicSshAccounts(sshAccounts);
            } catch (IOException e) {
                logger.error("Error loading account file " + dynamicAccountsFilePath + ": " + e.toString());
            }
            // regardless of whether the dynamic account file could be loaded initially, enable modification monitoring
            initiateMonitoringOfDynamicAccountsFile(dynamicAccountsFileLastModified);
        } else {
            logger.debug("No custom account file found, using data from main configuration file");
        }

        writeStatusToAuditLog(AuditLogIds.ACCOUNTS_INITIALIZED);

        sshd.setPasswordAuthenticator(authenticationManager);
        sshd.setPublickeyAuthenticator(authenticationManager);

        // TODO review: why not use a single factory instance for both?
        sshd.setShellFactory(new CustomSshCommandFactory(authenticationManager, scpContextManager, commandExecutionService,
            uplinkSessionService, sshConfiguration));
        // don't use ScpCommandFactory. Delegate is not called!
        sshd.setCommandFactory(new CustomSshCommandFactory(authenticationManager, scpContextManager, commandExecutionService,
            uplinkSessionService, sshConfiguration));

        registerConnectionLifecycleListeners(sshd);

        try {
            sshd.start();
            logServerStartupEvent();
            sshServerRunning = true;
        } catch (IOException e) {
            logger.error(
                StringUtils.format("Failed to start embedded SSH server on port %s (attempted to bind to IP address %s): %s",
                    sshConfiguration.getPort(), sshConfiguration.getHost(), e.toString()));
        }
    }

    private void initiateMonitoringOfDynamicAccountsFile(FileTime lastModifiedTime) {
        // TODO rework this to event-driven file monitoring
        asyncTaskService.scheduleAtFixedInterval("Check dynamic accounts file for modification", this::reloadDynamicAccountsFileIfModified,
            DYNAMIC_ACCOUNT_FILE_MODIFICATION_CHECK_INTERVAL_MSEC);
    }

    private synchronized void reloadDynamicAccountsFileIfModified() {
        try {
            FileTime newModifiedTime = Files.getLastModifiedTime(dynamicAccountsFilePath);
            if (newModifiedTime.equals(dynamicAccountsFileLastModified)) {
                return; // not modified
            }
            Map<String, ConfigurationSegment> sshAccounts = attemptToLoadDynamicAccountData();
            if (checkForPotentialConcurrentModification(newModifiedTime)) {
                return; // stored "last modified" time unchanged, so the next timed trigger will cause a new reload attempt
            }
            logger.debug("Detected modification of " + dynamicAccountsFilePath + ", reloading SSH account data");
            applyDynamicSshAccounts(sshAccounts);
            writeStatusToAuditLog(AuditLogIds.ACCOUNTS_UPDATED);
            dynamicAccountsFileLastModified = newModifiedTime;
        } catch (IOException e) {
            logger.error("Error checking or reloading account file " + dynamicAccountsFilePath + ": " + e.toString());
            // do not abort monitoring; the file may return to a readable state
        }
    }

    private boolean checkForPotentialConcurrentModification(FileTime newModifiedTime) throws IOException {
        try {
            final int shortWaitForModificationDetection = 500;
            Thread.sleep(shortWaitForModificationDetection);
        } catch (InterruptedException e) {
            logger.error("Interrupted; most likely, the application is shutting down");
            return true; // abort in this case, too
        }
        // has the file potentially changed right as it was being reloaded? if so, abort
        if (!newModifiedTime.equals(Files.getLastModifiedTime(dynamicAccountsFilePath))) {
            logger.debug(
                "It seems as if " + dynamicAccountsFilePath + " has been modified while it was being reloaded; postponing the reload");
            return true;
        }
        return false;
    }

    private void applyDynamicSshAccounts(Map<String, ConfigurationSegment> sshAccounts) {
        if (!sshAccounts.isEmpty()) {
            logger.debug("Read " + sshAccounts.size() + " SSH account(s) from " + dynamicAccountsFilePath);
        } else {
            logger.debug("Read account configuration file " + dynamicAccountsFilePath
                + ", but it contained no SSH accounts; using SSH accounts from the main configuration file (if any)");
        }
        sshConfiguration.applyDynamicSshAccountData(sshAccounts);
    }

    private Map<String, ConfigurationSegment> attemptToLoadDynamicAccountData() throws IOException {
        ConfigurationSegment data = configurationService.loadCustomConfigurationFile(dynamicAccountsFilePath);
        Map<String, ConfigurationSegment> sshAccounts = data.listElements("ssh");
        return sshAccounts;
    }

    private SshServer createSSHServerAndApplySettings() {
        SshServer serverInstance;
        serverInstance = SshServer.setUpDefaultServer();
        // TODO also use this to announce the RCE product version?
        updateServerBannerWithAnnouncementData(serverInstance);
        Path hostKeyFilePath = new File(hostKeyStorageDirectory, HOST_KEY_STORAGE_FILE_NAME).getAbsoluteFile().toPath();
        logger.debug("Using SSH server key storage " + hostKeyFilePath);
        serverInstance.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyFilePath));
        serverInstance.setHost(sshConfiguration.getHost());
        serverInstance.setPort(sshConfiguration.getPort());

        logger.debug("Configuring SSH session idle timeout of " + sshConfiguration.getIdleTimeoutSeconds() + " seconds");
        serverInstance.getProperties().put(SshServer.IDLE_TIMEOUT, TimeUnit.SECONDS.toMillis(sshConfiguration.getIdleTimeoutSeconds()));
        // set the allowed time before the first authentication attempt; default is 120 seconds
        // TODO make this configurable?
        serverInstance.getProperties().put(SshServer.AUTH_TIMEOUT, TimeUnit.SECONDS.toMillis(10));
        return serverInstance;
    }

    private synchronized void performShutdown() {
        sshServerEnabled = false;
        sshServerRunning = false;
        if (sshd != null) {
            try {
                sshd.stop(true);
                logServerShutdownEvent();
            } catch (IOException e) {
                logger.error("Exception during shutdown of embedded SSH server", e);
            }
        }

        sessionTracker.logLeftoverSessions();
    }

    // note: should only be called from synchronized methods
    private void updateServerBannerWithAnnouncementData(SshServer sshServer) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RCE");
        for (Entry<String, String> entry : announcedVersionEntries.entrySet()) {
            buffer.append(" ");
            buffer.append(entry.getKey());
            buffer.append("/");
            buffer.append(entry.getValue());
        }
        sshServer.getProperties().put(SshServer.SERVER_IDENTIFICATION, buffer.toString());
    }

    private void registerConnectionLifecycleListeners(SshServer serverInstance) {

        // Add listeners for logging high-level events. On an incoming connection, the earliest available callback seems to be
        // Session.established. -- misc_ro
        serverInstance.addSessionListener(new SessionListener() {

            @Override
            public void sessionEstablished(Session session) {
                SessionHandle sessionTrackingHandle = sessionTracker.registerSessionStart(session);

                // this seems to be the earliest available callback, so log this here
                InetSocketAddress remoteAddressAndPort = (InetSocketAddress) session.getRemoteAddress();

                // note: session.userName() is not defined here yet
                sessionTrackingHandle
                    .addLogData("remote_ip", remoteAddressAndPort.getAddress().getHostAddress())
                    .addLogData("remote_port", remoteAddressAndPort.getPort())
                    .addLogData("server_port", sshConfiguration.getPort());
            }

            @Override
            public void sessionCreated(Session session) {
                updateSessionLivenessAndRegisterAsClosedIfZero(session, "sessionCreated()", INCREASE_BY_ONE);
            }

            @Override
            public void sessionClosed(Session session) {
                // TODO 10.3.0+: update this description in relation to the new close detection mechanism

                // The only reliable marker of the end of a connection seems to be that at least one of this or channelClosed() is called.
                // In some connection life cycles, this method is called exclusively; in others, this is called after channelClosed(), so
                // allow duplicate registerSessionClosed() calls without considering it an error (2nd parameter 'true'). -- misc_ro

                updateSessionLivenessAndRegisterAsClosedIfZero(session, "sessionClosed()", DECREASE_BY_ONE);
            }

        });

        // TODO 10.3.0+: update this description in relation to the new close detection mechanism

        // On disconnect from RCE (via GUI), or on connection failure, however, neither
        // Session.closed, .disconnect, nor .exception are called. There is also no
        // sessionEvent() callback in that case. Therefore, channel events are used instead.
        // Channel.closed seems to be called last, and in every scenario except EOF. -- misc_ro
        serverInstance.addChannelListener(new ChannelListener() {

            @Override
            public void channelStateChanged(Channel channel, String hint) {
                // the EOF case does not seem to trigger the other shutdown events, so log this
                if ("SSH_MSG_CHANNEL_EOF".equals(hint)) {
                    try {
                        sessionTracker.forSession(channel.getSession()).registerDataStreamEOF();
                    } catch (OperationFailureException e) {
                        logger.warn("Failed to register stream EOF: " + e.getMessage());
                    }
                }
            }

            @Override
            public void channelInitialized(Channel channel) {
                updateSessionLivenessAndRegisterAsClosedIfZero(channel.getSession(), "channelInitialized()", INCREASE_BY_ONE);
            }

            @Override
            public void channelClosed(Channel channel, Throwable reason) {
                updateSessionLivenessAndRegisterAsClosedIfZero(channel.getSession(), "channelClosed()", DECREASE_BY_ONE);

                // TODO 10.3.0+: update this description in relation to the new close detection mechanism

                // The only reliable marker of the end of a connection seems to be that at least one of this or sessionClosed() is called.
                // In some connection life cycles, this method is called exclusively; in others, this is called before sessionClosed().
                // In both cases, it should never be a repeated call, so if this happens, this should be considered a consistency error (2nd
                // parameter 'false'). -- misc_ro
            }

        });
    }

    private void logServerStartupEvent() {
        AuditLog.append(AuditLog.newEntry(AuditLogIds.NETWORK_SERVERPORT_OPEN)
            .set(EVENT_LOG_KEY_CONNECTION_TYPE, EVENT_LOG_VALUE_CONNECTION_TYPE)
            .set("bind_ip", sshConfiguration.getHost())
            .set("port", sshConfiguration.getPort()));
        logger.info(StringUtils.format("SSH server started on port %s (bound to IP %s)", sshConfiguration.getPort(),
            sshConfiguration.getHost()));
    }

    private void logServerShutdownEvent() {
        AuditLog.append(AuditLog.newEntry(AuditLogIds.NETWORK_SERVERPORT_CLOSE)
            .set(EVENT_LOG_KEY_CONNECTION_TYPE, EVENT_LOG_VALUE_CONNECTION_TYPE)
            .set("bind_ip", sshConfiguration.getHost())
            .set("port", sshConfiguration.getPort()));
        logger.debug("Embedded SSH server shut down");
    }

    private boolean getActivationSettingFromConfig(SshConfiguration currentConfig) {
        boolean result = false;
        if (currentConfig != null && currentConfig.isEnabled()) {
            result = currentConfig.validateConfiguration(logger);
        }
        return result;
    }

    @Reference
    protected void bindScpContextManager(ScpContextManager newInstance) {
        this.scpContextManager = newInstance;
    }

    @Reference
    protected void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
    }

    @Reference
    protected void bindCommandExecutionService(CommandExecutionService newInstance) {
        this.commandExecutionService = newInstance;
    }

    @Reference
    protected void bindServerSideUplinkSessionService(ServerSideUplinkSessionService newInstance) {
        this.uplinkSessionService = newInstance;
    }

    /*
     * TODO 10.3.0+: add more detailed explanation of this mechanism
     * 
     * Short version: Initially, this approach was needed as a workaround; later, it was kept to handle cases where the last channel close
     * and the session close could be fired out of order. As it does not seem to have any downsides, there is currently no reason to change
     * it. -- misc_ro
     */
    private void updateSessionLivenessAndRegisterAsClosedIfZero(Session session, String eventName, int delta) {
        try {
            int newSessionLivenessCount = sessionTracker.forSession(session).modifyCustomCounter(delta);
            // TODO 10.3.0+: consider leaving this in as verbose log option
            // logger.debug("SSH session " + session + " " + eventName + ", new liveness value: " + newSessionLivenessCount);
            if (newSessionLivenessCount == 0) {
                // the second parameter was intended for an earlier approach and is probably obsolete; always "false",
                // as duplicate calls to this method should always be considered a consistency violation for now. --misc_ro
                // TODO 10.3.0+: examine and consider removing the parameter
                sessionTracker.registerSessionClosed(session, false);
            }
        } catch (OperationFailureException e) {
            logger.warn("SSH session " + session + ": Failed to register " + eventName + " event: " + e.getMessage());
            return;
        }
    }

    private void writeStatusToAuditLog(String eventType) {
        AuditLog.append(
            AuditLog.newEntry(eventType)
                .set("type", "ssh")
                // TODO decision: for now, only logging the total count; log the actual accounts instead?
                .set("number_of_accounts", sshConfiguration.getCurrentNumberOfAccouts())
                .set("origin", sshConfiguration.getAccountDataOriginInfo()));
    }

}
