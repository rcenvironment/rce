/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.command.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.command.spi.CommandPlugin;

/**
 * @author Alexander Weinert
 */
class CommandPlugins {
    
    private final Map<String, Set<CommandPlugin>> map = new HashMap<>();
    
    Set<CommandPlugin> getPluginsForTopLevelCommand(String topLevelCommand) {
        return this.map.computeIfAbsent(topLevelCommand, ignoredCommand -> new HashSet<>());
    }
    
    void put(String topLevelCommand, CommandPlugin plugin) {
        this.map.computeIfAbsent(topLevelCommand, ignoredCommand -> new HashSet<>()).add(plugin);
    }

    
    void removeTopLevelCommand(String topLevelCommand) {
        this.map.remove(topLevelCommand);
    }
    
    void removePlugin(CommandPlugin plugin) {
        for (Set<CommandPlugin> plugins : this.map.values()) {
            plugins.remove(plugin);
        }
    }
}
