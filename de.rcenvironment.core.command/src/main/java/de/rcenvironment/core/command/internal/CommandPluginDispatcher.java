/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.SingleCommandHandler;

/**
 * TODO: RCE 10.5: this class can probably be removed
 * 
 * Dispatches a single command to the appropriate {@link CommandPlugin}, or generates an error message if no matching {@link CommandPlugin}
 * is registered.
 * 
 * @author Robert Mischke
 * @author Alexander Weinert (replace plain HashMap by class CommandPlugins, allow for multiple plugins serving same toplevel command)
 */
public class CommandPluginDispatcher implements SingleCommandHandler {

    private Map<String, CommandPlugin> pluginsByTopLevelCommand = new HashMap<String, CommandPlugin>();

    private final Log log = LogFactory.getLog(getClass());
    
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
                log.debug("would call plugin.execute(context)");
                //plugin.execute(context);
            } catch (RuntimeException e) {
                LogFactory.getLog(getClass()).error("Uncaught exception in command handler", e);
                throw CommandException.executionError("Uncaught exception in command handler: " + e.toString(), context);
            }
        } else {
            // no command recognized
            throw CommandException.unknownCommand(context);
        }
    }

    /*
     * TODO: find out if it is neccesary with new system
     */
    
//    private Optional<CommandPlugin> findBestFit(CommandContext context) {
//        Set<CommandPlugin> plugins;
//        String topLevelCommand = context.peekNextToken();
//        synchronized (pluginsByTopLevelCommand) {
//            if (topLevelCommand == null) {
//                throw new IllegalArgumentException("Empty command");
//            }
//            plugins = pluginsByTopLevelCommand.getPluginsForTopLevelCommand(topLevelCommand);
//        }
//        
//        Optional<CommandPlugin> plugin = Optional.empty();
//        int maxNumberOfTokensMatched = 0;
//        
//        for (CommandPlugin pluginIterator : plugins) {
//            for (CommandDescription desc : pluginIterator.getCommandDescriptions()) {
//                for (int i = maxNumberOfTokensMatched + 1; i <= context.getOriginalTokens().size(); ++i) {
//                    final String attemptToMatch = String.join(" ", context.getOriginalTokens().subList(0, i));
//                    if (desc.getStaticPart().equals(attemptToMatch)) {
//                        maxNumberOfTokensMatched = i;
//                        plugin = Optional.of(pluginIterator);
//                    }
//                }
//            }
//        }
//        
//        return plugin;
//    }

    /**
     * Registers a new {@link CommandPlugin}, which is then used for handling commands matching the descriptions provided by
     * {@link CommandPlugin#getCommandDescriptions()}.
     * 
     * @param plugin the new plugin
     */
//    public void registerPlugin(CommandPlugin plugin) {
//        Set<String> topLevelCommands = determineTopLevelCommands(plugin);
//        synchronized (pluginsByTopLevelCommand) {
//            for (String command : topLevelCommands) {
//                final CommandPlugin registeredPlugin = pluginsByTopLevelCommand.get(command);
//                if (registeredPlugin != null) {
//                    LogFactory.getLog(getClass()).warn(StringUtils.format(
//                        "Ignoring new command plugin %s as plugin %s already handles command %s", plugin, registeredPlugin, command));
//                    continue;
//                }
//                pluginsByTopLevelCommand.put(command, plugin);
//            }
//        }
//    }

    /**
     * Unregisters a new {@link CommandPlugin}.
     * 
     * @param plugin the plugin to remove
     */
//    public void unregisterPlugin(CommandPlugin plugin) {
//        Set<String> topLevelCommands = determineTopLevelCommands(plugin);
//        synchronized (pluginsByTopLevelCommand) {
//            for (String command : topLevelCommands) {
//                final CommandPlugin registeredPlugin = pluginsByTopLevelCommand.get(command);
//                if (registeredPlugin != plugin) {
//                    LogFactory.getLog(getClass()).warn(StringUtils.format("Processing shutdown of command plugin %s, "
//                        + "but the provided command %s is registered as being provided by plugin %s", plugin, command, registeredPlugin));
//                    continue;
//                }
//                pluginsByTopLevelCommand.remove(command);
//            }
//        }
//    }
}
