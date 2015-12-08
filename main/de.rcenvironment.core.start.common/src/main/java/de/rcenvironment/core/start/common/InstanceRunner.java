/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.app.IApplication;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Abstract base class for "instance runners". A single implementation is invoked by the main application to perform startup steps that are
 * specific to either GUI or headless mode.
 * 
 * The concrete implementation is injected into {@link Instance} by specific launcher bundles. This avoids dependencies from the main
 * application to GUI code, which would trigger GUI bundles to start in headless mode.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public abstract class InstanceRunner {

    protected static volatile InstanceValidationService instanceValidationService;

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
     * Injects the {@link InstanceValidationService} instance to validate the RCE instance on startup.
     * 
     * @param newService the new instance
     */
    public void bindInstanceValidationService(InstanceValidationService newService) {
        InstanceRunner.instanceValidationService = newService;
    }

    /**
     * Invokes this runner.
     * 
     * @return the return code for the main application.
     * @throws Exception on uncaught exceptions
     */
    public int run() throws Exception {
        // Write versions to log file
        log.debug("Core version: " + VersionUtils.getVersionOfCoreBundles());
        log.debug("Product version: " + VersionUtils.getVersionOfProduct());

        log.debug("Command line arguments passed: " + System.getProperty("sun.java.command"));

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        for (String vmArg : runtime.getInputArguments()) {
            log.debug("JVM argument passed: " + vmArg);
        }

        if (!validateInstance()) {
            return IApplication.EXIT_OK;
        } else {
            return performRun();
        }
    }

    private boolean validateInstance() {
        Map<InstanceValidationResultType, List<InstanceValidationResult>> validationResults = instanceValidationService.validateInstance();
        int passed = validationResults.get(InstanceValidationResultType.PASSED).size();
        int failedWithProceedingAllowed = validationResults.get(InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED).size();
        int failedWithShutdownRequired = validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED).size();

        log.debug(StringUtils.format("Instance validation results [%d in total]: %d passed, %d failed with proceeding allowed, "
            + "%d failed with shutdown required", passed + failedWithProceedingAllowed + failedWithShutdownRequired,
            passed, failedWithProceedingAllowed, failedWithShutdownRequired));

        if (validationResults.containsKey(InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED)) {
            for (InstanceValidationResult result : validationResults.get(InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED)) {
                log.error(StringUtils.format("Instance validation '%s' failed: %s", result.getValidationDisplayName(),
                    result.getLogMessage()));
            }
        }

        if (validationResults.containsKey(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED)) {
            for (InstanceValidationResult result : validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED)) {
                log.error(StringUtils.format("Instance validation '%s' failed: %s. RCE is shutting down",
                    result.getValidationDisplayName(), result.getLogMessage()));
            }
        }

        if (failedWithProceedingAllowed > 0 || failedWithShutdownRequired > 0) {
            return onInstanceValidationFailures(validationResults);
        }
        return true;
    }

    /**
     * Runs the instance. Subclasses need to implement. Common run logic goes into {@link #run()}.
     * 
     * @return the return code for the main application.
     * @throws Exception on uncaught exceptions
     */
    public abstract int performRun() throws Exception;

    /**
     * May (optionally) present user feedback about startup instance validation failures.
     * 
     * @param validationResults result of the instance validation
     * @return <code>false</code> if instance validation failed and RCE must be shut down, otherwise <code>false</code>
     */
    // TODO refactor to avoid validation-specific parameter?
    public boolean onInstanceValidationFailures(Map<InstanceValidationResultType, List<InstanceValidationResult>> validationResults) {
        if (validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED).size() > 0) {
            return false;
        }
        return true;
    }

    /**
     * Custom hook that is fired before the common code of {@link Instance#awaitShutdown()}.
     */
    public void beforeAwaitShutdown() {}

    /**
     * Performs custom actions when {@link Instance#shutdown()} is called.
     */
    public void triggerShutdown() {}

    /**
     * Restarts RCE.
     */
    public void triggerRestart() {}

    protected Future<CommandExecutionResult> initiateAsyncCommandExecution(final String[] execCommandTokens, final String taskDescription,
        final boolean isBatchMode) {
        if (commandExecutionService == null) {
            log.error("Command execution service not available; ignoring provided command(s) " + execCommandTokens);
            return null;
        }
        final String taskDescriptionWithTokens = taskDescription + " (\""
            + org.apache.commons.lang3.StringUtils.join(execCommandTokens, " ") + "\")";

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
                CommandException ce = (CommandException) e;
                if (ce.getType().equals(CommandException.Type.HELP_REQUESTED)) {
                    stdout.println(commandExecutionService.getHelpText(false, ce.shouldPrintDeveloperHelp()));
                } else {
                    log.error("Error during " + taskDescription, e);
                }
            }

            @Override
            public void onFinished() {
                log.debug("Finished " + taskDescriptionWithTokens);
            }
        };
        return commandExecutionService.asyncExecMultiCommand(Arrays.asList(execCommandTokens), outputReceiver, taskDescription);
    }

}
