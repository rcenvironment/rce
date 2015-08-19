/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Abstract base class for "instance runners". A single implementation is invoked by the main application to perform startup steps that are
 * specific to either GUI or headless mode.
 * 
 * The concrete implementation is injected into {@link Platform} by specific launcher bundles. This avoids dependencies from the main
 * application to GUI code, which would trigger GUI bundles to start in headless mode.
 * 
 * @author Robert Mischke
 */
public abstract class InstanceRunner {

    private static volatile CommandExecutionService commandExecutionService;

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Injects the {@link CommandExecutionService} instance to use for dispatching commands.
     * 
     * @param newService the new instance
     */
    public void bindCommandExecutionService(CommandExecutionService newService) {
        InstanceRunner.commandExecutionService = newService;
    }

    /**
     * Invokes this runner.
     * 
     * @return the return code for the main application.
     * @throws Exception on uncaught exceptions
     */
    public abstract int run() throws Exception;

    /**
     * May (optionally) present user feedback about startup validation errors.
     * 
     * @param messages the messages describing the validation failures.
     * @return
     */
    // TODO refactor to avoid validation-specific parameter?
    public void onValidationErrors(List<PlatformMessage> messages) {}

    /**
     * Custom hook that is fired before the common code of {@link Platform#awaitShutdown()}.
     */
    public void beforeAwaitShutdown() {}

    /**
     * Performs custom actions when {@link Platform#shutdown()} is called.
     */
    public void triggerShutdown() {}

    protected Future<CommandExecutionResult> initiateAsyncCommandExecution(final String[] execCommandTokens, final String taskDescription,
        final boolean isBatchMode) {
        if (commandExecutionService == null) {
            log.error("Command execution service not available; ignoring provided command(s) " + execCommandTokens);
            return null;
        }
        final String taskDescriptionWithTokens = taskDescription + " (\"" + StringUtils.join(execCommandTokens, " ") + "\")";

        final PrintStream stdout = System.out;
        TextOutputReceiver outputReceiver = new TextOutputReceiver() {

            @Override
            public void addOutput(String line) {
                if (isBatchMode) {
                    log.debug("Output of command-line batch command(s): " + line);
                    stdout.println(line);
                } else {
                    log.info("Output of command-line startup command(s): " + line);
                }
            }

            @Override
            public void onStart() {
                log.debug("Starting " + taskDescriptionWithTokens);
            }

            @Override
            public void onFatalError(Exception e) {
                log.error("Error during " + taskDescription, e);
            }

            @Override
            public void onFinished() {
                log.debug("Finished " + taskDescriptionWithTokens);
            }
        };
        return commandExecutionService.asyncExecMultiCommand(Arrays.asList(execCommandTokens), outputReceiver, taskDescription);
    }
}
