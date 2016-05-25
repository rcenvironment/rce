/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Common base class for {@link TextOutputReceiver}s that display {@link CommandExecutionService} results in an interactive command shell.
 * Could be expanded with more common methods/features in the future.
 * 
 * Subclasses must implement {@link #addOutput(String)}, and may optionally override {@link #onStart()} and {@link #onFinished()}.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractInteractiveCommandConsole implements TextOutputReceiver {

    private final CommandExecutionService commandExecutionService;

    private final Log log = LogFactory.getLog(getClass());

    private String commandPrefix;

    public AbstractInteractiveCommandConsole(CommandExecutionService commandExecutionService) {
        // default: no command prefix
        this(commandExecutionService, "");
    }

    public AbstractInteractiveCommandConsole(CommandExecutionService commandExecutionService, String commandPrefix) {
        this.commandExecutionService = commandExecutionService;
        this.commandPrefix = commandPrefix;
    }

    @Override
    public void onStart() {
        // empty default implementation; override as needed
    }

    @Override
    public abstract void addOutput(String line);

    @Override
    public void onFinished() {
        // empty default implementation; override as needed
    }

    @Override
    // note: made final to avoid accidental "custom" implementations; only make non-final if there is a good reason - misc_ro
    public final void onFatalError(Exception e) {
        if (e instanceof CommandException) {
            CommandException ce = (CommandException) e;
            switch (ce.getType()) {
            case SYNTAX_ERROR:
                // do not log anything
                // log.info(StringUtils.format("Syntax error in command \"%s\"; message=%s", ce.getCommandString(), ce.getMessage()));
                if (ce.getMessage() != null) {
                    addOutput(StringUtils.format("Syntax error: %s", ce.getMessage()));
                    addOutput(StringUtils.format("Use \"%shelp\" to see all available commands.", commandPrefix));
                } else {
                    addOutput(StringUtils.format("Syntax error. Use \"%shelp\" to see all available commands.", commandPrefix));
                }
                break;
            case UNKNOWN_COMMAND:
                // do not log anything
                addOutput(StringUtils.format("Unknown command \"%s\". Use \"%shelp\" to see all available commands.",
                    ce.getCommandString(), commandPrefix));
                break;
            case EXECUTION_ERROR:
                log.warn(StringUtils.format("Error executing command \"%s\"; message=%s", ce.getCommandString(), ce.getMessage()));
                if (ce.getMessage() != null) {
                    addOutput(StringUtils.format("Error executing command \"%s\": %s", ce.getCommandString(), ce.getMessage()));
                } else {
                    addOutput(StringUtils.format("Error executing command \"%s\". "
                        + "No message text available; check the log file for more information.", ce.getCommandString()));
                }
                break;
            case HELP_REQUESTED:
                // TODO backwards compatibility hack; pass prefix as parameter instead - misc_ro
                boolean useCommandPrefix = commandPrefix != null && !commandPrefix.isEmpty();
                commandExecutionService.printHelpText(useCommandPrefix, ce.shouldPrintDeveloperHelp(), this);
                break;
            default:
                // should never happen
                log.error("Unhandled CommandException sub-type", e);
                addOutput("Internal error: Unhandled CommandException sub-type (" + e.toString() + ")");
            }
        } else {
            log.info("Error during command execution", e);
            addOutput("Error during command execution (" + e.toString() + "); check log file for details");
        }
    }
}
