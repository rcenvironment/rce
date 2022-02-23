/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal;

import java.util.HashSet;
import java.util.Optional;
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
 * @author Alexander Weinert (replace plain HashMap by class CommandPlugins, allow for multiple plugins serving same toplevel command)
 */
public class CommandPluginDispatcher implements SingleCommandHandler {

    private CommandPlugins pluginsByTopLevelCommand = new CommandPlugins();

    @Override
    public void execute(CommandContext context) throws CommandException {
        if (context.consumeNextTokenIfEquals("explain")) {
            context.println("Parsed command tokens: " + context.consumeRemainingTokens());
            return;
        }
        Optional<CommandPlugin> plugin = null;
        plugin = findBestFit(context);
        if (plugin.isPresent()) {
            try {
                plugin.get().execute(context);
            } catch (RuntimeException e) {
                LogFactory.getLog(getClass()).error("Uncaught exception in command handler", e);
                throw CommandException.executionError("Uncaught exception in command handler: " + e.toString(), context);
            }
        } else {
            // no command recognized
            throw CommandException.unknownCommand(context);
        }
    }

    private Optional<CommandPlugin> findBestFit(CommandContext context) {
        Set<CommandPlugin> plugins;
        String topLevelCommand = context.peekNextToken();
        synchronized (pluginsByTopLevelCommand) {
            if (topLevelCommand == null) {
                throw new IllegalArgumentException("Empty command");
            }
            plugins = pluginsByTopLevelCommand.getPluginsForTopLevelCommand(topLevelCommand);
        }
        
        Optional<CommandPlugin> plugin = Optional.empty();
        int maxNumberOfTokensMatched = 0;
        
        for (CommandPlugin pluginIterator : plugins) {
            for (CommandDescription desc : pluginIterator.getCommandDescriptions()) {
                for (int i = maxNumberOfTokensMatched + 1; i <= context.getOriginalTokens().size(); ++i) {
                    final String attemptToMatch = String.join(" ", context.getOriginalTokens().subList(0, i));
                    if (desc.getStaticPart().equals(attemptToMatch)) {
                        maxNumberOfTokensMatched = i;
                        plugin = Optional.of(pluginIterator);
                    }
                }
            }
        }
        
        return plugin;
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
                /*
                 * final CommandPlugin registeredPlugin = pluginsByTopLevelCommand.getPluginForTopLevelCommand(command); if
                 * (registeredPlugin != null) { LogFactory.getLog(getClass()).warn(StringUtils.format(
                 * "Ignoring new command plugin %s as plugin %s already handles command %s", plugin, registeredPlugin, command)); continue;
                 * }
                 */
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
        synchronized (pluginsByTopLevelCommand) {
            pluginsByTopLevelCommand.removePlugin(plugin);
        }
        /*
         * Set<String> topLevelCommands = determineTopLevelCommands(plugin); synchronized (pluginsByTopLevelCommand) { for (String command :
         * topLevelCommands) { final CommandPlugin registeredPlugin = pluginsByTopLevelCommand.getPluginForTopLevelCommand(command); if
         * (registeredPlugin != plugin) { LogFactory.getLog(getClass()).warn(StringUtils.format("Processing shutdown of command plugin %s, "
         * + "but the provided command %s is registered as being provided by plugin %s", plugin, command, registeredPlugin)); continue; }
         * pluginsByTopLevelCommand.removeTopLevelCommand(command); } }
         */
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
