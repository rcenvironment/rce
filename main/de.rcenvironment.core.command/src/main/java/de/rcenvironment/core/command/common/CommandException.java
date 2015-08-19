/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.common;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.rcenvironment.core.command.spi.CommandContext;

/**
 * Exception class for malformed RCE commands.
 * 
 * @author Robert Mischke
 */
public final class CommandException extends Exception {

    /**
     * Type of the generated exception.
     * 
     * @author Robert Mischke
     */
    public enum Type {
        /**
         * Pseudo-exception for commands that actively request help information.
         */
        HELP_REQUESTED,

        /**
         * Unknown command.
         */
        UNKNOWN_COMMAND,

        /**
         * Command syntax error.
         */
        SYNTAX_ERROR,

        /**
         * Indicates that the command was syntactically correct, but has encountered an error.
         */
        EXECUTION_ERROR
    }

    private static final long serialVersionUID = 405551760124111120L;

    private final Type type;

    private final List<String> commandTokens;

    private final boolean showDeveloperHelp;

    private CommandException(Type type, String message, CommandContext context) {
        super(message);
        this.type = type;
        this.commandTokens = context.getOriginalTokens();
        this.showDeveloperHelp = context.isDeveloperCommandSetEnabled(); // TODO move to getter?
    }

    /**
     * Creates a custom {@link Type#SYNTAX_ERROR} exception.
     * 
     * @param message the message text
     * @param context the {@link CommandContext}
     * @return the generated exception
     */
    public static CommandException syntaxError(String message, CommandContext context) {
        return new CommandException(Type.SYNTAX_ERROR, message, context);
    }

    /**
     * Creates a "wrong number of parameters" {@link Type#SYNTAX_ERROR} exception.
     * 
     * @param context the {@link CommandContext}
     * @return the generated exception
     */
    public static CommandException wrongNumberOfParameters(CommandContext context) {
        return new CommandException(Type.SYNTAX_ERROR, "Wrong number of parameters", context);
    }

    /**
     * Creates a custom {@link Type#EXECUTION_ERROR} exception.
     * 
     * @param message the message text
     * @param context the {@link CommandContext}
     * @return the generated exception
     */
    public static CommandException executionError(String message, CommandContext context) {
        return new CommandException(Type.EXECUTION_ERROR, message, context);
    }

    /**
     * Creates an {@link Type#UNKNOWN_COMMAND} exception.
     * 
     * @param context the {@link CommandContext}
     * @return the generated exception
     */
    public static CommandException unknownCommand(CommandContext context) {
        return new CommandException(Type.UNKNOWN_COMMAND, null, context);
    }

    /**
     * Creates a {@link Type#HELP_REQUESTED} pseudo-exception to request help output.
     * 
     * @param context the {@link CommandContext}
     * @return the generated exception
     */
    public static CommandException requestHelp(CommandContext context) {
        return new CommandException(Type.HELP_REQUESTED, null, context);
    }

    public Type getType() {
        return type;
    }

    public String getCommandString() {
        return StringUtils.join(commandTokens, ' ');
    }

    /**
     * @return true if the problematic command was a developer command; in this case, developer syntax help should be printed
     */
    public boolean shouldPrintDeveloperHelp() {
        return showDeveloperHelp;
    }

    @Override
    public String toString() {
        return de.rcenvironment.core.utils.common.StringUtils.format("Type=%s, Tokens=%s, ShowDevHelp=%s, Message=%s", type, commandTokens,
            showDeveloperHelp, getMessage());
    }

}
