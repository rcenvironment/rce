/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of an embedded SSH server with OSGi lifecycle methods.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class EmbeddedSshServerImpl {

    private static final String HOST_KEY_STORAGE_FILE_NAME = "ssh_host_key.dat";

    private ConfigurationService configurationService;

    private CommandExecutionService commandExecutionService;

    private SshAuthenticationManager authenticationManager;

    private SshConfiguration sshConfiguration;

    private ScpContextManager scpContextManager;

    private SshServer sshd;

    private final Log logger = LogFactory.getLog(getClass());

    private boolean sshServerActive = false;

    /**
     * OSGi-DS life-cycle method.
     */
    public void activate() {
        ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("sshServer");
        try {
            sshConfiguration = new SshConfiguration(configurationSegment);
        } catch (IOException e) {
            sshConfiguration = new SshConfiguration();
        }
        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription("Embedded SSH server startup")
            public void run() {
                performStartup();
            }
        });
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void deactivate() {
        performShutdown();
    }

    private synchronized void performStartup() {
        sshServerActive = getActivationSettingFromConfig(sshConfiguration);
        if (sshServerActive) {
            authenticationManager = new SshAuthenticationManager(sshConfiguration);
            sshd = SshServer.setUpDefaultServer();
            sshd.setPasswordAuthenticator(authenticationManager);
            String hostKeyFilePath = new File(configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA),
                HOST_KEY_STORAGE_FILE_NAME).getAbsolutePath();
            logger.debug("Using SSH server key storage " + hostKeyFilePath);
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
                logger.info("SSH server started on port " + sshConfiguration.getPort());
            } catch (IOException e) {
                logger.error("Failed to start embedded SSH server on port " + sshConfiguration.getPort(), e);
            }
        } else {
            logger.debug("Not running an SSH server as no SSH configuration is present, or the \"enabled\" property is not \"true\"");
        }
    }

    private synchronized void performShutdown() {
        sshServerActive = false;
        if (sshd != null) {
            try {
                sshd.stop(true);
                logger.debug("Embedded SSH server shut down");
            } catch (InterruptedException e) {
                logger.error("Exception during shutdown of embedded SSH server", e);
            }
        }
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
