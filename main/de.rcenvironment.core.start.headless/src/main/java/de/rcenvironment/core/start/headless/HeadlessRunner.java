/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.start.headless;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.equinox.app.IApplication;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.start.common.CommandLineArguments;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.Platform;
import de.rcenvironment.core.start.common.validation.PlatformValidationManager;
import de.rcenvironment.core.utils.common.VersionUtils;

/**
 * Start class for headless run.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public final class HeadlessRunner extends InstanceRunner {

    /**
     * Runs the headless mode.
     * 
     * @return state
     * @throws AuthenticationException : no user
     * @throws InterruptedException : interrupted
     */
    public int run() throws AuthenticationException, InterruptedException {
        if (!(new PlatformValidationManager()).validate(true)) {
            Platform.shutdown();
        }

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
            commandExecutionFuture =
                initiateAsyncCommandExecution(execCommandTokens, "execution of " + cliToken + " commands", isBatchMode);
        }

        // Write versions to log file
        log.debug("Core Version: " + VersionUtils.getVersionOfCoreBundles());
        log.debug("Product Version: " + VersionUtils.getVersionOfProduct());

        if (CommandLineArguments.isBatchModeRequested()) {
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
            Platform.awaitShutdown();
        }
        return IApplication.EXIT_OK;
    }
}
