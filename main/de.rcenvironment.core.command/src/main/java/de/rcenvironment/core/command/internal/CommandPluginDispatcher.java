/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
                if (pluginsByTopLevelCommand.get(command) != null) {
                    LogFactory.getLog(getClass()).warn(
                        "Ignoring new command plugin registration as another plugin already handles the command " + command);
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
                if (pluginsByTopLevelCommand.get(command) != plugin) {
                    LogFactory.getLog(getClass()).warn(
                        "Another plugin is handling the command " + command);
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
