/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.common.CommandException;
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

    protected static final String ERROR_MESSAGE_INCORRECT_LOGGING_CONFIG = "Failed to initialize background logging properly."
        + " Most likely, because RCE was started from another directory than its installation directory. "
        + "(The installation directory is the directory, which contains the 'rce' executable.) ";
    
    protected static final String INFO_MESSAGE_INCORRECT_LOGGING_CONFIG = "RCE will be shutdown. "
        + "Start it again from its installation directory.";
    
    private static volatile CommandExecutionService commandExecutionService;

    private static volatile ConfigurationAdmin configurationAdmin;
    
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
     * Injects the {@link ConfigurationAdmin} instance to check configuration of pax logging.
     * 
     * @param newService the new instance
     */
    public void bindConfigurationAdmin(ConfigurationAdmin newService) {
        configurationAdmin = newService;
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

    /**
     * Restarts RCE.
     */
    public void triggerRestart() {

    }
    
    protected boolean isLoggingConfiguredProperly() {
        
        boolean isConfiguredProperly = false;
        Configuration paxLoggingConfiguration = null;
        try {
            String paxLoggingPid = "org.ops4j.pax.logging";
            paxLoggingConfiguration = configurationAdmin.getConfiguration(paxLoggingPid);
        } catch (IOException e) {
            log.error("Failed to get configuration of pax logging from the configuration admin service. "
                + "Most likely, logging is not configured properly.", e);
            // as there is nothing to do from a user's perspective, the error is just logged and not provided to the user
            return false;
        }
        String nonDefaultPaxConfigKey = "log4j.appender.DEBUG_LOG";
        isConfiguredProperly = paxLoggingConfiguration.getProperties() != null
            && paxLoggingConfiguration.getProperties().get(nonDefaultPaxConfigKey) != null;            
        if (!isConfiguredProperly) {
            log.error(ERROR_MESSAGE_INCORRECT_LOGGING_CONFIG);
        }

        return isConfiguredProperly;
    }

}
