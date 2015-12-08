/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.headless;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.equinox.app.IApplication;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.headless.textui.ConfigurationTextUI;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Start class for headless run.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Doreen Seider
 */
public final class HeadlessInstanceRunner extends InstanceRunner {

    private AtomicInteger exitCode = new AtomicInteger(IApplication.EXIT_OK);

    /**
     * Runs the RCE instance in headless mode.
     * 
     * @return exit code
     * @throws InterruptedException if waiting for the RCE instance to shut down is interrupted
     */
    @Override
    public int performRun() throws InterruptedException {

        String[] execCommandTokens = CommandLineArguments.getExecCommandTokens();
        Future<CommandExecutionResult> commandExecutionFuture = null;
        if (execCommandTokens != null) {
            final boolean isBatchMode = CommandLineArguments.isBatchModeRequested();
            String cliToken;
            if (isBatchMode) {
                cliToken = CommandLineArguments.BATCH_OPTION_TOKEN;
            } else {
                cliToken = CommandLineArguments.EXEC_OPTION_TOKEN;
            }
            commandExecutionFuture = initiateAsyncCommandExecution(execCommandTokens,
                "execution of " + cliToken + " commands", isBatchMode);
        }

        if (CommandLineArguments.isConfigurationShellRequested()) {
            log.debug("Running text-mode configuration UI");
            final ServiceRegistryAccess serviceAccess = ServiceRegistry.createAccessFor(this);
            final ConfigurationService configurationService = serviceAccess.getService(ConfigurationService.class);
            final SshAccountConfigurationService sshConfigurationService = serviceAccess.getService(SshAccountConfigurationService.class);
            new ConfigurationTextUI(configurationService, sshConfigurationService).run();
            log.debug("Shutting down after text-mode configuration UI has terminated");
        } else if (CommandLineArguments.isBatchModeRequested()) {
            if (commandExecutionFuture != null) {
                try {
                    commandExecutionFuture.get();
                } catch (ExecutionException e) {
                    log.error("Uncaught error in batch command execution", e);
                }
            } else {
                // Future could be null if the command service is unavailable
                log.error("Failed to initialize batch command execution");
            }
        } else {
            // "standard" headless mode
            Instance.awaitShutdown();
        }

        return exitCode.get();
    }

    @Override
    public void triggerRestart() {
        setExitCode(IApplication.EXIT_RESTART);
        Instance.shutdown();
    }

    private void setExitCode(int newExitCode) {
        exitCode.set(newExitCode);
    }
}
