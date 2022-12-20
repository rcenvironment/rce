/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Used for defining sub commands of a command group {@link MainCommandDescription}.
 * 
 * @author Sebastian Nocke
 *
 */
public class SubCommandDescription extends AbstractCommandDescription {

    public SubCommandDescription(String command, String description, SingleCommandHandler handler, CommandModifierInfo modifiers) {
        super(command, description, handler, modifiers);
    }
    
    public SubCommandDescription(String command, String description,
            SingleCommandHandler handler, CommandModifierInfo modifiers, boolean isDevCommand) {
        super(command, description, handler, modifiers, isDevCommand);
    }
    
    public SubCommandDescription(String command, String description, SingleCommandHandler handler) {
        super(command, description, handler);
    }
    
    public SubCommandDescription(String command, String description, SingleCommandHandler handler, boolean isDevcommand) {
        super(command, description, handler, isDevcommand);
    }
    
}
