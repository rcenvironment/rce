/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of an embedded SSH server with OSGi lifecycle methods.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden (added public key authentication)
 */
@Component(immediate = true)
public class EmbeddedSshServerImpl implements EmbeddedSshServerControl {

    private static final String HOST_KEY_STORAGE_FILE_NAME = "ssh_host_key.dat";

    private ConfigurationService configurationService;

    private CommandExecutionService commandExecutionService;

    private SshConfiguration sshConfiguration;

    private File hostKeyStorageDirectory; // used at activation only

    private ScpContextManager scpContextManager;

    private ServerSideUplinkSessionService uplinkSessionService;

    private SshServer sshd;

    private boolean sshServerActive = false; // enabled by configuration?

    private boolean sshServerRunning = false; // actually running?

    private final Map<String, String> announcedVersionEntries = new HashMap<>();

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * OSGi-DS life-cycle method.
     */
    @Activate
    public void activate() {
        if (!CommandLineArguments.isConfigurationShellRequested()) {
            ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("sshServer");
            try {
                sshConfiguration = new SshConfiguration(configurationSegment);
            } catch (ConfigurationException | IOException e) {
                sshConfiguration = new SshConfiguration();
                logger.error(e.getMessage());
            }
            hostKeyStorageDirectory = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA);
            ConcurrencyUtils.getAsyncTaskService().execute("Embedded SSH server startup", this::performStartup);
        }
    }

    /**
     * An entry point for integration tests to set up a live SSH server without mocking the whole {@link ConfigurationService}.
     * 
     * @param configuration the SSH configuration segment
     * @param hostKeyStorageDir the directory that the SSH server will store its host key file in
     * @param serverSideUplinkSessionService the {@link ServerSideUplinkEndpointService} implementation to use
     */
    public void mockActivateAndStart(SshConfiguration configuration, File hostKeyStorageDir,
        ServerSideUplinkSessionService serverSideUplinkSessionService) {
        this.sshConfiguration = configuration;
        this.hostKeyStorageDirectory = hostKeyStorageDir;
        this.uplinkSessionService = serverSideUplinkSessionService;
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
        if (sshServerActive) {
            updateServerBannerWithAnnouncementData();
        }
    }

    private synchronized void performStartup() {
        sshServerActive = getActivationSettingFromConfig(sshConfiguration);
        if (sshServerActive) {
            SshAuthenticationManager authenticationManager = new SshAuthenticationManager(sshConfiguration);
            sshd = SshServer.setUpDefaultServer();
            // TODO also use this to announce the RCE product version?
            updateServerBannerWithAnnouncementData();
            sshd.setPasswordAuthenticator(authenticationManager);
            sshd.setPublickeyAuthenticator(authenticationManager);
            Path hostKeyFilePath = new File(hostKeyStorageDirectory, HOST_KEY_STORAGE_FILE_NAME).getAbsoluteFile().toPath();
            logger.debug("Using SSH server key storage " + hostKeyFilePath);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyFilePath));
            sshd.setHost(sshConfiguration.getHost());
            sshd.setPort(sshConfiguration.getPort());

            logger.debug("Configuring SSH session idle timeout of " + sshConfiguration.getIdleTimeoutSeconds() + " seconds");
            sshd.getProperties().put(SshServer.IDLE_TIMEOUT, TimeUnit.SECONDS.toMillis(sshConfiguration.getIdleTimeoutSeconds()));

            // TODO review: why not use a single factory instance for both?
            sshd.setShellFactory(new CustomSshCommandFactory(authenticationManager, scpContextManager, commandExecutionService,
                uplinkSessionService, sshConfiguration));
            // don't use ScpCommandFactory. Delegate is not called!
            sshd.setCommandFactory(new CustomSshCommandFactory(authenticationManager, scpContextManager, commandExecutionService,
                uplinkSessionService, sshConfiguration));
            try {
                sshd.start();
                logger.info(StringUtils.format("SSH server started on port %s (bound to IP %s)", sshConfiguration.getPort(),
                    sshConfiguration.getHost()));
                sshServerRunning = true;
            } catch (IOException e) {
                logger.error(
                    StringUtils.format("Failed to start embedded SSH server on port %s (attempted to bind to IP address %s)",
                        sshConfiguration.getPort(), sshConfiguration.getHost()),
                    e);
            }
        } else {
            logger.debug("Not running an SSH server as there is either no SSH configuration at all, "
                + "or the \"enabled\" property is not \"true\", or the configuration data (including account settings) has errors");
        }
    }

    private synchronized void performShutdown() {
        sshServerActive = false;
        sshServerRunning = false;
        if (sshd != null) {
            try {
                sshd.stop(true);
                logger.debug("Embedded SSH server shut down");
            } catch (IOException e) {
                logger.error("Exception during shutdown of embedded SSH server", e);
            }
        }
    }

    // note: should only be called from synchronized methods
    private void updateServerBannerWithAnnouncementData() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RCE");
        for (Entry<String, String> entry : announcedVersionEntries.entrySet()) {
            buffer.append(" ");
            buffer.append(entry.getKey());
            buffer.append("/");
            buffer.append(entry.getValue());
        }
        sshd.getProperties().put(SshServer.SERVER_IDENTIFICATION, buffer.toString());
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

}
