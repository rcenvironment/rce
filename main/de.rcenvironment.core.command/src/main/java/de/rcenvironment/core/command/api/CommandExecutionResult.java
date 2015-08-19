/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.api;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Represents the result of a single or multipl command execution.
 * 
 * @author Robert Mischke
 */
public enum CommandExecutionResult {
    /**
     * The command finished normally; note that this does *not* specify whether the command performed the intended action!
     */
    DEFAULT,

    /**
     * An error occurred during execution of the command. Internally, this indicates that a {@link CommandException} was thrown by at least
     * one command handler.
     */
    ERROR,

    /**
     * The command indicated that the calling interactive shell (if any) should be closed, although no error occurred.
     */
    EXIT_REQUESTED,

    /**
     * The command's execution was interrupted (for example, by shutdown).
     */
    INTERRUPTED;
}
