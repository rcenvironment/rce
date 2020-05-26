/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.command;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Handling the storage and retrieval of used commands.
 *
 * @author Marc Stammerjohann
 */
public class CommandHandler {

    private static final String KEYCOMMAND = "UsedCommands";

    private static final String SAVEDCOMMANDCOUNTER = "SavedCommandCounter";

    /** Constant. Command History is limited to 30 entries. */
    private static final int COMMAND_LIMIT = 30;

    /** Recently used commands. User can iterate over it through key up and down. */
    private final List<String> usedCommands = new ArrayList<String>();

    private PersistentSettingsService persistentSettingsService;

    public CommandHandler() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        persistentSettingsService = serviceRegistryAccess.getService(PersistentSettingsService.class);

        retrieveCommandHistory();
    }

    public List<String> getUsedCommands() {
        return usedCommands;
    }

    /** Retrieve commands from persistent service. */
    private void retrieveCommandHistory() {
        String commands = persistentSettingsService.readStringValue(KEYCOMMAND);
        if (commands != null) {
            String[] commandSplit = StringUtils.splitAndUnescape(commands);
            for (String element : commandSplit) {
                addUsedCommand(element);
            }
        }
    }

    /**
     * Adding used commands at the first position of {@link CommandHandler#usedCommands}. Removes the last command, if the list size is
     * equals {@link CommandHandler#COMMAND_LIMIT}.
     * 
     * @param command recently used
     */
    public void addUsedCommand(String command) {
        usedCommands.remove(command);
        int size = usedCommands.size();
        if (size == COMMAND_LIMIT) {
            usedCommands.remove(size - 1);
        }
        usedCommands.add(0, command);
    }

    /**
     * Save command with persistent service. Used as History. Commands are available after restart.
     * 
     * @param command to be saved
     */
    public void saveCommand(String command) {
        String savedCommands = persistentSettingsService.readStringValue(KEYCOMMAND);
        String savedCommandCounter = persistentSettingsService.readStringValue(SAVEDCOMMANDCOUNTER);
        String escapeAndConcat = null;
        int savedCounter;
        if (savedCommandCounter == null) {
            savedCounter = 0;
        } else {
            savedCounter = Integer.parseInt(savedCommandCounter);
        }
        if (savedCounter < COMMAND_LIMIT) {
            // saves new command
            if (savedCommands == null) {
                escapeAndConcat = StringUtils.escapeAndConcat(command);
            } else {
                String[] splitAndUnescape = splitAndUnescapeCommand(savedCommands, command, savedCounter);
                escapeAndConcat = StringUtils.escapeAndConcat(splitAndUnescape);
            }
            persistentSettingsService.saveStringValue(SAVEDCOMMANDCOUNTER, "" + ++savedCounter);
        } else {
            // if limit of saving commands is reached, last command will be removed
            String[] splitAndUnescape = splitAndRemoveLast(savedCommands, command);
            escapeAndConcat = StringUtils.escapeAndConcat(splitAndUnescape);
        }
        persistentSettingsService.saveStringValue(KEYCOMMAND, escapeAndConcat);
    }

    /** Splits saved commands and adds new command to be saved. */
    private String[] splitAndUnescapeCommand(String savedCommands, String command, int savedCounter) {
        String[] splitAndUnescape = new String[savedCounter + 1];
        String[] commandSplit = StringUtils.splitAndUnescape(savedCommands);
        for (int i = 0; i < commandSplit.length; i++) {
            splitAndUnescape[i] = commandSplit[i];
        }
        splitAndUnescape[splitAndUnescape.length - 1] = command;
        return splitAndUnescape;
    }

    /** Removes last command. */
    private String[] splitAndRemoveLast(String savedCommands, String command) {
        String[] splitAndUnescape = StringUtils.splitAndUnescape(savedCommands);
        for (int i = 0; i < splitAndUnescape.length - 1; i++) {
            splitAndUnescape[i] = splitAndUnescape[i + 1];
        }
        splitAndUnescape[splitAndUnescape.length - 1] = command;
        return splitAndUnescape;
    }

}
