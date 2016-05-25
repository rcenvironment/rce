/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.internal;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.internal.handlers.BuiltInCommandPlugin;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;

/**
 * Default implementation of {@link CommandExecutionService}.
 * 
 * @author Robert Mischke
 */
public class CommandExecutionServiceImpl implements CommandExecutionService {

    private static final String INDENT = "\t";

    private final CommandPluginDispatcher commandPluginDispatcher;

    private final SharedThreadPool threadPool;

    private final Set<CommandDescription> commandDescriptions = new TreeSet<CommandDescription>();
    
    private ConfigurationService configurationService;

    public CommandExecutionServiceImpl() {
        commandPluginDispatcher = new CommandPluginDispatcher();
        threadPool = SharedThreadPool.getInstance();
    }

    /**
     * OSGi-DS activation method.
     */
    public void activate() {
        registerCommandPlugin(new BuiltInCommandPlugin());
    }

    /**
     * Registers a {@link CommandPlugin}; may be called by OSGi-DS or manually.
     * 
     * @param plugin the plugin to add
     */
    public void registerCommandPlugin(CommandPlugin plugin) {
        commandPluginDispatcher.registerPlugin(plugin);
        synchronized (commandDescriptions) {
            commandDescriptions.addAll(plugin.getCommandDescriptions());
        }
    }

    /**
     * Unregisters a {@link CommandPlugin}; may be called by OSGi-DS or manually.
     * 
     * @param plugin the plugin to remove
     */
    public void unregisterCommandPlugin(CommandPlugin plugin) {
        // TODO unregister help contributions once plugins are actually removed dynamically;
        // right now, this only exists for symmetry
        commandPluginDispatcher.unregisterPlugin(plugin);
    }
    
    /**
     * Bind configuration service method.
     *
     * @param service The service to bind.
     */
    public void bindConfigurationService(ConfigurationService service) {
        configurationService = service;
    }

    @Override
    public Future<CommandExecutionResult> asyncExecMultiCommand(List<String> tokens, TextOutputReceiver outputReceiver, Object initiator) {
        
        File profileOutput = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT);
        MultiCommandHandler multiCommandHandler = new MultiCommandHandler(tokens, outputReceiver, commandPluginDispatcher, profileOutput);
        multiCommandHandler.setInitiatorInformation(initiator);
        return threadPool.submit(multiCommandHandler);
    }

    @Override
    public void printHelpText(boolean addCommonPrefix, boolean showDevCommands, TextOutputReceiver outputReceiver) {
        synchronized (commandDescriptions) {
            if (showDevCommands) {
                outputReceiver.addOutput("RCE User Commands:");
            } else {
                outputReceiver.addOutput("RCE Console Commands:");
            }
            // only show non-developer commands in this block
            for (CommandDescription contribution : commandDescriptions) {
                if (!contribution.isDeveloperCommand()) {
                    printHelpContribution(outputReceiver, contribution, addCommonPrefix);
                }
            }
            if (showDevCommands) {
                // only show developer commands in this block
                outputReceiver.addOutput("RCE Developer Commands:");
                for (CommandDescription contribution : commandDescriptions) {
                    if (contribution.isDeveloperCommand()) {
                        printHelpContribution(outputReceiver, contribution, addCommonPrefix);
                    }
                }
            }
        }
    }

    @Override
    public String getHelpText(boolean addCommonPrefix, boolean showDevCommands) {
        CapturingTextOutReceiver capturingReceiver = new CapturingTextOutReceiver("");
        printHelpText(addCommonPrefix, showDevCommands, capturingReceiver);
        return capturingReceiver.getBufferedOutput();
    }

    private void printHelpContribution(TextOutputReceiver outputReceiver, CommandDescription contribution, boolean addCommonPrefix) {
        // construct first line
        StringBuilder buffer = new StringBuilder();
        buffer.append(INDENT);
        if (addCommonPrefix) {
            buffer.append("rce ");
        }
        buffer.append(contribution.getStaticPart());
        if (contribution.hasDynamicPart()) {
            buffer.append(" ");
            buffer.append(contribution.getDynamicPart());
        }
        buffer.append(" - ");
        buffer.append(contribution.getFirstLine());
        outputReceiver.addOutput(buffer.toString());
        // additional lines
        for (String additionalLine : contribution.getAdditionalLines()) {
            outputReceiver.addOutput(INDENT + INDENT + additionalLine);
        }
    }

}
