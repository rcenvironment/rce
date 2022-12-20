/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * A plugin for handling one or more top-level commands (e.g. the "x" in "rce x ...").
 * 
 * @author Robert Mischke
 */
public interface CommandPlugin {

    /**
     * @return all commands for this plugin in a tree structure
     */
    MainCommandDescription[] getCommands();
    
}
