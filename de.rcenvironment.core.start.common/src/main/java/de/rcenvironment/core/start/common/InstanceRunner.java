/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
 * @author David Scholz (minor change: added check for shutdown request)
 * @author Brigitte Boden
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

        if (Instance.isShutdownRequested()) {
            return IApplication.EXIT_OK;
        }
        if (!validateInstance()) {
            return IApplication.EXIT_OK;
        } else {
            return performRun();
        }
    }

    private boolean validateInstance() {
        boolean repeatValidation;
        do {
            repeatValidation = false;
            final Map<InstanceValidationResultType, List<InstanceValidationResult>> validationResults =
                instanceValidationService.validateInstance();

            final int passed = validationResults.get(InstanceValidationResultType.PASSED).size();
            final int failedWithConfirmationRequired =
                validationResults.get(InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED).size();
            final int failedWithRecoveryRequired =
                validationResults.get(InstanceValidationResultType.FAILED_RECOVERY_REQUIRED).size();
            final int failedWithShutdownRequired = validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED).size();

            log.debug(StringUtils.format(
                "Instance validation results [%d in total]: %d passed, %d failed with confirmation required, "
                + "%d passed with recovery required, %d failed with shutdown required",
                passed + failedWithConfirmationRequired + failedWithRecoveryRequired + failedWithShutdownRequired,
                passed, failedWithConfirmationRequired, failedWithRecoveryRequired, failedWithShutdownRequired));

            for (InstanceValidationResult result : validationResults.get(InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED)) {
                log.error(StringUtils.format("Instance validation '%s' failed: %s", result.getValidationDisplayName(),
                    result.getLogMessage()));
            }

            for (InstanceValidationResult result : validationResults.get(InstanceValidationResultType.FAILED_RECOVERY_REQUIRED)) {
                log.debug(StringUtils.format("Instance validation '%s' failed recoverably: %s", result.getValidationDisplayName(),
                    result.getLogMessage()));
            }

            for (InstanceValidationResult result : validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED)) {
                log.error(StringUtils.format("Instance validation '%s' failed irrecoverably: %s. RCE is shutting down",
                    result.getValidationDisplayName(), result.getLogMessage()));
            }
            
            final boolean shutdownRequired = (failedWithShutdownRequired > 0);
            if (shutdownRequired) {
                onShutdownRequired(validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED));
                return false;
            }
            
            final boolean recoveryRequired = (failedWithRecoveryRequired > 0);
            if (recoveryRequired) {
                final boolean recoverySucceeded =
                    onRecoveryRequired(validationResults.get(InstanceValidationResultType.FAILED_RECOVERY_REQUIRED));
                if (recoverySucceeded) {
                    repeatValidation = true;
                } else {
                    return false;
                }
            }
            
            final boolean confirmationRequired = (failedWithConfirmationRequired > 0);
            if (confirmationRequired) {
                final boolean confirmationGiven =
                    onConfirmationRequired(validationResults.get(InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED));
                if (!confirmationGiven) {
                    return false;
                }
            }
        } while (repeatValidation);
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
     * Called if the validation determines that RCE needs to be shut down. Must be overridden to inform the users about the reason for the
     * shutdown.
     * 
     * We consciously do not offer an empty default-implementation since subclasses shall have to make a conscious choice about silently
     * ignoring the reasons for the error.
     * 
     * @param validationResults The validation results that cause the necessary shutdown.
     */
    public abstract void onShutdownRequired(List<InstanceValidationResult> validationResults);
    
    /**
     * Called if the validation determines that RCE may be started if the user explicitly confirms the recovery from a failed validation.
     * Must be overridden to actually query the user. If the user confirms the recovery actions, this necessitates another round of
     * validation in order to ensure that the recovery indeed resulted in a valid instance of RCE.
     * 
     * We consciously do not offer an empty default-implementation since subclasses shall have to make a conscious choice about silently
     * ignoring the reasons for the error.
     * 
     * @param validationResults The validation results that cause the necessity of recovery.
     * @return True if the user confirmed all recoveries, false otherwise.
     */
    public abstract boolean onRecoveryRequired(List<InstanceValidationResult> validationResults);

    /**
     * Called if the validation determines that RCE may be started if the user explicitly confirms the startup. Must be overridden to
     * actually query the user. If the user confirms the startup, this does not necessitate another round of validation.
     * 
     * We consciously do not offer an empty default-implementation since subclasses shall have to make a conscious choice about silently
     * ignoring the reasons for the error.
     * 
     * @param validationResults The validation results that cause the necessary confirmation.
     * @return True if the user confirmed all validation results, false otherwise.
     */
    public abstract boolean onConfirmationRequired(List<InstanceValidationResult> validationResults);

    /**
     * Called if the validation determines that RCE may be started without further action.
     * 
     * In contrast to the other event handlers regarding validation, we do offer an empty default implementation since there is no immediate
     * need to inform the user about a successful validation.
     * 
     * @param validationResults The validation results of the successful validations.
     */
    public void onValidationSuccess(List<InstanceValidationResult> validationResults) {}

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
