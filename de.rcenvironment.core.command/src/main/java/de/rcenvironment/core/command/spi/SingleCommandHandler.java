/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A minimal interface for handling a single command.
 * 
 * @author Robert Mischke
 */
public interface SingleCommandHandler {

    /**
     * Synchronously executes a single command.
     * 
     * @param commandContext the {@link CommandContext} containing the list of tokens and a
     *        {@link TextOutputReceiver}
     * @throws CommandException on syntax or execution errors
     */
    void execute(CommandContext commandContext) throws CommandException;
}
