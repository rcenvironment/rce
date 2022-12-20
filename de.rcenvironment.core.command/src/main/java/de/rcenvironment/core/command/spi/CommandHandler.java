/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import de.rcenvironment.core.command.common.CommandException;

/**
 * Syntax definition for methods that are used to implement commands.
 * 
 * @author Sebastian Nocke
 *
 */
@FunctionalInterface
public interface CommandHandler {

    void handle(CommandContext context) throws CommandException;

    static CommandHandler throwing(CommandHandler handler) {

        return context -> {
            try {
                handler.handle(context);
            } catch (CommandException ex) {
                throw ex;
            }
        };
    }
}
