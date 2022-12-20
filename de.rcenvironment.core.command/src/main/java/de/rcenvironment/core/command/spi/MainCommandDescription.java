/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for defining command groups which define the first token of a command.
 * 
 * @author Sebastian Nocke
 *
 */
public class MainCommandDescription extends AbstractCommandDescription {

    private SubCommandDescription[] subCommands;
    private Map<String, SubCommandDescription> nSubCommands = new HashMap<>();
    private String commandGroupDescription;

    public MainCommandDescription(String command, String commandGroupDescription, String description,
            SingleCommandHandler handler, CommandModifierInfo modifiers, SubCommandDescription... subCommands) {
        super(command, description, handler, modifiers);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
    }
    
    public MainCommandDescription(String command, String commandGroupDescription, String description,
            SingleCommandHandler handler, CommandModifierInfo modifiers,
            boolean isDevcommand, SubCommandDescription... subCommands) {
        super(command, description, handler, modifiers, isDevcommand);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
    }
    
    public MainCommandDescription(String command, String commandGroupDescription, String description,
            SingleCommandHandler handler, SubCommandDescription... subCommands) {
        super(command, description, handler);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
    }
    
    public MainCommandDescription(String command, String commandGroupDescription, String description,
            SingleCommandHandler handler, boolean isDevcommand, SubCommandDescription... subCommands) {
        super(command, description, handler, isDevcommand);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
    }
    
    public MainCommandDescription(String command, String commandGroupDescription,
            String description, SubCommandDescription... subCommands) {
        super(command, description);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
    }
    
    public MainCommandDescription(String command, String commandGroupDescription, String description,
            boolean isDevcommand, SubCommandDescription... subCommands) {
        super(command, description, isDevcommand);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
    }
    
    public MainCommandDescription(String command, String commandGroupDescription, String description) {
        super(command, description);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = new SubCommandDescription[] {};
    }

    public MainCommandDescription(String command, String commandGroupDescription,
            String description, boolean isDevcommand) {
        super(command, description, isDevcommand);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = new SubCommandDescription[] {};
    }
    
    public SubCommandDescription[] getSubCommands() {
        return subCommands;
    }
    
    public String getCommandGroupDescription() {
        return commandGroupDescription;
    }

    public boolean hasSubCommand(String subCommand) {
        return nSubCommands.containsKey(subCommand);
    }
    
    public SubCommandDescription getSubCommand(String subCommand) {
        return nSubCommands.get(subCommand);
    }
    
    private void putSubCommandsInMap() {
        Arrays.stream(subCommands).forEach(subCommnad -> nSubCommands.put(subCommnad.getCommand(), subCommnad));
    }
    
}
