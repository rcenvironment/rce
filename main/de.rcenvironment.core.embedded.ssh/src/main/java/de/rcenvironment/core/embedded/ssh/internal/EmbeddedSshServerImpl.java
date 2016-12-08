/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.embedded.ssh.api.EmbeddedSshServerControl;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of an embedded SSH server with OSGi lifecycle methods.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden (added public key authentication)
 */
public class EmbeddedSshServerImpl implements EmbeddedSshServerControl {

    private static final String HOST_KEY_STORAGE_FILE_NAME = "ssh_host_key.dat";

    private ConfigurationService configurationService;

    private CommandExecutionService commandExecutionService;

    private SshAuthenticationManager authenticationManager;

    private SshConfiguration sshConfiguration;

    private ScpContextManager scpContextManager;

    private SshServer sshd;

    private boolean sshServerActive = false;

    private final Map<String, String> announcedVersionEntries = new HashMap<>();

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * OSGi-DS life-cycle method.
     */
    public void activate() {
        if (!CommandLineArguments.isConfigurationShellRequested()) {
            ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("sshServer");
            try {
                sshConfiguration = new SshConfiguration(configurationSegment);
            } catch (ConfigurationException | IOException e) {
                sshConfiguration = new SshConfiguration();
                logger.error(e.getMessage());
            }
            ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                @Override
                @TaskDescription("Embedded SSH server startup")
                public void run() {
                    performStartup();
                }
            });
        }
    }

    /**
     * OSGi-DS lifecycle method.
     */
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
            authenticationManager = new SshAuthenticationManager(sshConfiguration);
            sshd = SshServer.setUpDefaultServer();
            // TODO also use this to announce the RCE product version?
            updateServerBannerWithAnnouncementData();
            sshd.setPasswordAuthenticator(authenticationManager);
            sshd.setPublickeyAuthenticator(authenticationManager);
            File hostKeyFilePath = new File(configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA),
                HOST_KEY_STORAGE_FILE_NAME).getAbsoluteFile();
            logger.debug("Using SSH server key storage " + hostKeyFilePath.getPath());
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyFilePath));
            sshd.setHost(sshConfiguration.getHost());
            sshd.setPort(sshConfiguration.getPort());

            // TODO review: why not use a single factory instance for both?
            sshd.setShellFactory(new CustomSshCommandFactory(authenticationManager, scpContextManager, commandExecutionService,
                sshConfiguration));
            // don't use ScpCommandFactory. Delegate is not called!
            sshd.setCommandFactory(new CustomSshCommandFactory(authenticationManager, scpContextManager, commandExecutionService,
                sshConfiguration));
            try {
                sshd.start();
                logger.info(StringUtils.format("SSH server started on port %s (bound to IP %s)", sshConfiguration.getPort(),
                    sshConfiguration.getHost()));
            } catch (IOException e) {
                logger.error(
                    StringUtils.format("Failed to start embedded SSH server on port %s (attempted to bind to IP address %s)",
                        sshConfiguration.getPort(), sshConfiguration.getHost()), e);
            }
        } else {
            logger.debug("Not running an SSH server as there is either no SSH configuration at all, "
                + "or the \"enabled\" property is not \"true\", or the configuration data (including account settings) has errors");
        }
    }

    private synchronized void performShutdown() {
        sshServerActive = false;
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

    /**
     * OSGi-DS injection method.
     * 
     * @param newInstance the service instance to bind
     */
    protected void bindScpContextManager(ScpContextManager newInstance) {
        this.scpContextManager = newInstance;
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    protected void bindCommandExecutionService(CommandExecutionService newService) {
        this.commandExecutionService = newService;
    }

}
