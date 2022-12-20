/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Base class for {@link MainCommandDescription} and {@link SubCommandDescription} which define the commands of the RCE console.
 * 
 * @author Sebastian Nocke
 *
 */
public abstract class AbstractCommandDescription {

    private String command;
    private String description;
    private SingleCommandHandler handler;
    private boolean isDevcommand;
    
    private CommandModifierInfo modifiers;
    
    AbstractCommandDescription(String command, String description, SingleCommandHandler handler) {
        this.command = command;
        this.description = description;
        this.handler = handler;
        this.modifiers = new CommandModifierInfo();
    }

    AbstractCommandDescription(String command, String description, SingleCommandHandler handler, boolean isDevcommand) {
        this.command = command;
        this.description = description;
        this.handler = handler;
        this.modifiers = new CommandModifierInfo();
        this.isDevcommand = isDevcommand;
    }
    
    AbstractCommandDescription(String command, String description, SingleCommandHandler handler, CommandModifierInfo modifiers) {
        this.command = command;
        this.description = description;
        this.handler = handler;
        this.modifiers = modifiers;
    }
    
    AbstractCommandDescription(String command, String description, SingleCommandHandler handler,
            CommandModifierInfo modifiers, boolean isDevcommand) {
        this.command = command;
        this.description = description;
        this.handler = handler;
        this.modifiers = modifiers;
        this.isDevcommand = isDevcommand;
    }
    
    AbstractCommandDescription(String command, String description) {
        this.command = command;
        this.description = description;
        this.modifiers = new CommandModifierInfo();
    }
    
    AbstractCommandDescription(String command, String description, boolean isDevcommand) {
        this.command = command;
        this.description = description;
        this.modifiers = new CommandModifierInfo();
        this.isDevcommand = isDevcommand;
    }
    
    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public boolean isExecutable() {
        return handler != null;
    }
    
    public SingleCommandHandler getHandler() {
        return handler;
    }
    
    public CommandModifierInfo getModifiers() {
        return modifiers;
    }
    
    public boolean getIsDevelopercommand() {
        return isDevcommand;
    }
    
}
