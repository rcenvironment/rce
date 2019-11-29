/*
 * Copyright 2006-2019 DLR, Germany
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

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.SingleCommandHandler;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Dispatches a single command to the appropriate {@link CommandPlugin}, or generates an error message if no matching {@link CommandPlugin}
 * is registered.
 * 
 * @author Robert Mischke
 */
public class CommandPluginDispatcher implements SingleCommandHandler {

    private Map<String, CommandPlugin> pluginsByTopLevelCommand = new HashMap<String, CommandPlugin>();

    @Override
    public void execute(CommandContext context) throws CommandException {
        if (context.consumeNextTokenIfEquals("explain")) {
            context.println("Parsed command tokens: " + context.consumeRemainingTokens());
            return;
        }
        CommandPlugin plugin = null;
        String topLevelCommand = context.peekNextToken();
        synchronized (pluginsByTopLevelCommand) {
            if (topLevelCommand == null) {
                throw new IllegalArgumentException("Empty command");
            }
            plugin = pluginsByTopLevelCommand.get(topLevelCommand);
        }
        if (plugin != null) {
            try {
                plugin.execute(context);
            } catch (RuntimeException e) {
                LogFactory.getLog(getClass()).error("Uncaught exception in command handler", e);
                throw CommandException.executionError("Uncaught exception in command handler: " + e.toString(), context);
            }
        } else {
            // no command recognized
            throw CommandException.unknownCommand(context);
        }
    }

    /**
     * Registers a new {@link CommandPlugin}, which is then used for handling commands matching the descriptions provided by
     * {@link CommandPlugin#getCommandDescriptions()}.
     * 
     * @param plugin the new plugin
     */
    public void registerPlugin(CommandPlugin plugin) {
        Set<String> topLevelCommands = determineTopLevelCommands(plugin);
        synchronized (pluginsByTopLevelCommand) {
            for (String command : topLevelCommands) {
                final CommandPlugin registeredPlugin = pluginsByTopLevelCommand.get(command);
                if (registeredPlugin != null) {
                    LogFactory.getLog(getClass()).warn(StringUtils.format(
                        "Ignoring new command plugin %s as plugin %s already handles command %s", plugin, registeredPlugin, command));
                    continue;
                }
                pluginsByTopLevelCommand.put(command, plugin);
            }
        }
    }

    /**
     * Unregisters a new {@link CommandPlugin}.
     * 
     * @param plugin the plugin to remove
     */
    public void unregisterPlugin(CommandPlugin plugin) {
        Set<String> topLevelCommands = determineTopLevelCommands(plugin);
        synchronized (pluginsByTopLevelCommand) {
            for (String command : topLevelCommands) {
                final CommandPlugin registeredPlugin = pluginsByTopLevelCommand.get(command);
                if (registeredPlugin != plugin) {
                    LogFactory.getLog(getClass()).warn(StringUtils.format("Processing shutdown of command plugin %s, "
                        + "but the provided command %s is registered as being provided by plugin %s", plugin, command, registeredPlugin));
                    continue;
                }
                pluginsByTopLevelCommand.remove(command);
            }
        }
    }

    private Set<String> determineTopLevelCommands(CommandPlugin plugin) {
        Set<String> topLevelCommands = new HashSet<>();
        for (CommandDescription cd : plugin.getCommandDescriptions()) {
            String[] staticPartSegments = cd.getStaticPart().split(" ");
            // handle "a/b ..." command aliases
            String[] tlcs = staticPartSegments[0].split("/");
            for (String tlc : tlcs) {
                topLevelCommands.add(tlc);
            }
        }
        return topLevelCommands;
    }
}
