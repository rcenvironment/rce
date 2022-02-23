/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.Collection;

/**
 * A plugin for handling one or more top-level commands (e.g. the "x" in "rce x ...").
 * 
 * @author Robert Mischke
 */
public interface CommandPlugin extends SingleCommandHandler {

    /**
     * @return a list of this plugin's contributions to the "help" command output
     */
    Collection<CommandDescription> getCommandDescriptions();

}
