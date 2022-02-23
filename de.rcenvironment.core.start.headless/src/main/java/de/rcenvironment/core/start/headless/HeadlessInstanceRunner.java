/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.headless;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.equinox.app.IApplication;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.bootstrap.ui.ErrorTextUI;
import de.rcenvironment.core.configuration.ui.LanternaUtils;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.core.mail.SMTPServerConfigurationService;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.CallbackException;
import de.rcenvironment.core.start.headless.textui.ConfigurationTextUI;
import de.rcenvironment.core.start.headless.textui.QuestionDialogTextUI;
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
            final SshAccountConfigurationService sshConfigurationService = serviceAccess.getService(SshAccountConfigurationService.class);
            final SMTPServerConfigurationService smtpServerConfigurationService =
                serviceAccess.getService(SMTPServerConfigurationService.class);
            new ConfigurationTextUI(sshConfigurationService, smtpServerConfigurationService).run();
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
            Instance.awaitShutdown();
        }

        return exitCode.get();
    }

    @Override
    public void onShutdownRequired(List<InstanceValidationResult> validationResults) {
        InstanceValidationResult result = validationResults.get(0);
        final String errorMessage =
            String.format("Instance validation failure: %s%n%nRCE will be shut down.", result.getGuiDialogMessage());
        new ErrorTextUI(LanternaUtils.applyWordWrapping(errorMessage)).run();
    }

    @Override
    public boolean onRecoveryRequired(List<InstanceValidationResult> validationResults) {
        for (InstanceValidationResult result : validationResults) {
            final String dialogQuestion = result.getGuiDialogMessage();
            final boolean attemptRecovery;
            try {
                attemptRecovery =
                    new QuestionDialogTextUI("Instance validation failure", LanternaUtils.applyWordWrapping(dialogQuestion)).run();
            } catch (RuntimeException t) {
                final StringBuilder logMessageBuilder = new StringBuilder("A recoverable instance validation failure occured. "
                    + "The user could not be queried whether or not recovery should proceed due to the exception below. "
                    + "Aborting startup.");
                final Optional<String> userHint = result.getUserHint();
                if (userHint.isPresent()) {
                    logMessageBuilder.append('\n');
                    logMessageBuilder.append(userHint.get());
                }
                log.error(logMessageBuilder.toString(), t);
                return false;
            }

            if (attemptRecovery) {
                try {
                    result.getCallback().onConfirmation();
                } catch (CallbackException e) {
                    new ErrorTextUI(String.format("Error during recovery from instance validation error: %s. See log for more details.",
                        e.getMessage()));
                    log.error("Exception thrown during recovery from instance validation failure", e);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onConfirmationRequired(List<InstanceValidationResult> validationResults) {
        for (InstanceValidationResult result : validationResults) {
            if (!new QuestionDialogTextUI("Instance validation failure",
                result.getGuiDialogMessage() + "\n\nDo you like to proceed anyway?").run()) {
                return false;
            }
        }
        return true;
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
