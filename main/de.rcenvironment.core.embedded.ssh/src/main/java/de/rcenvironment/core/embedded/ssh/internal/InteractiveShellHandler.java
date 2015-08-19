/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to handle all possibilities of chars and char sequences. Provides a robust way of generating an command for the SSH console.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class InteractiveShellHandler {

    private StringBuffer commandBuffer;

    private StringBuffer preBuffer;

    private List<String> oldCommands;

    private int pointerForOldCmds;

    private int pointerForCmdBuffer;

    private SshConsoleOutputAdapter sshConsoleHandler;

    // private final Log log = LogFactory.getLog(getClass());

    public InteractiveShellHandler(SshConsoleOutputAdapter sshConsoleHandler) {
        fullReset();
        this.sshConsoleHandler = sshConsoleHandler;
    }

    /**
     * 
     * Adds a new Char to this command. Due to specific processing the char might not be included in the comment (depending of its direct
     * context)
     * 
     * @param newCharCode - the code of the new char to be added. (use (int) charToAdd)).
     * 
     * @return true, if it should be printed to the console
     */
    public boolean processInputChar(int newCharCode) {
        boolean doPrint = false;
        int charCode = preProcessSpecialKeys(newCharCode);
        if (charCode >= SshConstants.LOWEST_USEFUL_KEY_CODE || charCode == SshConstants.TAB_KEY_CODE) {
            char newChar = (char) charCode;
            if (charCode == SshConstants.DEL_KEY_CODE) {
                if (pointerForCmdBuffer > 0) {
                    if (pointerForCmdBuffer < commandBuffer.length()) {
                        printToConsole(commandBuffer.substring(pointerForCmdBuffer + 1));
                    }
                    commandBuffer.deleteCharAt(pointerForCmdBuffer - 1);
                    pointerForCmdBuffer--;
                    doPrint = true;
                } else {
                    doPrint = false;
                }
            } else {
                // add whatever it is, when it managed to reach this point.
                if (pointerForCmdBuffer == commandBuffer.length()) {
                    commandBuffer.append(newChar);
                } else {
                    commandBuffer.insert(pointerForCmdBuffer, newChar);
                }
                pointerForCmdBuffer++;
                doPrint = true;
            }
        }
        return doPrint;
    }

    private int preProcessSpecialKeys(int newCharCode) {
        int result = newCharCode;
        String temp = null;
        if (newCharCode == SshConstants.ESC_KEY_CODE) {
            // ESC_KEY_CODE is for a new special key (in all situations) .
            preBuffer = new StringBuffer();
            result = SshConstants.NO_ADDING_INT_CODE;
        } else if (preBuffer != null && newCharCode != SshConstants.DEL_KEY_CODE) {
            // add whatever it is, when it managed to reach this point.
            preBuffer.append((char) newCharCode);
            temp = preBuffer.toString();
            if (temp.matches(SshConstants.SPECIAL_KEY_REGEX)) {
                result = SshConstants.NO_ADDING_INT_CODE;
                handleKnownCodes(temp);
            } else {
                // not a valid value. add current value to command buffer and console
                preBuffer = null;
            }
        }
        return result;
    }

    private void handleKnownCodes(String subject) {
        if (subject.matches(SshConstants.SPECIAL_KEY_LEFT)) {
            // Disabled because it causes incorrect behavior.
            // if (pointerForCmdBuffer > 0) {
            // pointerForCmdBuffer--;
            // printToConsole((char) SshConstants.ESC_KEY_CODE + subject);
            // }
            preBuffer = null;
        } else if (subject.matches(SshConstants.SPECIAL_KEY_RIGHT)) {
            // Disabled because it causes incorrect behavior.
            // if (pointerForCmdBuffer < commandBuffer.length()) {
            // pointerForCmdBuffer++;
            // printToConsole((char) SshConstants.ESC_KEY_CODE + subject);
            // }
            preBuffer = null;
        } else if (subject.matches(SshConstants.SPECIAL_KEY_ENTF)) {
            if (pointerForCmdBuffer < commandBuffer.length() - 1) {
                commandBuffer.deleteCharAt(pointerForCmdBuffer + 1);
                printToConsole((char) SshConstants.ESC_KEY_CODE + subject);
            }
            preBuffer = null;
        } else if (subject.matches(SshConstants.SPECIAL_KEY_UP)) {
            if (pointerForOldCmds > 0) {
                pointerForOldCmds--;
                scrollThroughHistory(oldCommands.get(pointerForOldCmds));
            }
            preBuffer = null;
        } else if (subject.matches(SshConstants.SPECIAL_KEY_DOWN)) {
            if (pointerForOldCmds < oldCommands.size()) {
                pointerForOldCmds++;
                if (pointerForOldCmds == oldCommands.size()) {
                    scrollThroughHistory("");
                } else {
                    scrollThroughHistory(oldCommands.get(pointerForOldCmds));
                }
            }
        } else if (subject.matches(SshConstants.SPECIAL_KEYS_IGNORE_LIST)) {
            // ignore these keys
            preBuffer = null;
        }
    }

    /**
     * 
     * Returns the command and resets this instance so a new command can be created.
     * 
     * @return the command as string.
     */
    public String getCurrentCommand() {
        String command = commandBuffer.toString();
        addToCommandHistory(command);
        reset();
        return command;
    }

    /**
     * 
     * Adds a command to the history List (oldCommands). Enables the use of key up and key down.
     * 
     * @param command - the command to be added.
     */
    public void addToCommandHistory(String command) {
        oldCommands.add(command);
        if (oldCommands.size() > SshConstants.COMMAND_HISTORY_SIZE) {
            oldCommands.remove(0);
        } else {
            pointerForOldCmds = oldCommands.size();
        }
    }

    private void scrollThroughHistory(String newCommand) {
        char delete = (char) SshConstants.DEL_KEY_CODE;
        String deleteString = "";
        for (int i = 0; i < commandBuffer.length(); i++) {
            deleteString = deleteString + delete;
        }
        reset();
        commandBuffer.append(newCommand);
        pointerForCmdBuffer = commandBuffer.length();
        printToConsole(deleteString + newCommand);
    }

    private void fullReset() {
        oldCommands = new ArrayList<String>();
        pointerForOldCmds = 0;
        reset();
    }

    private void reset() {
        commandBuffer = new StringBuffer();
        preBuffer = null;
        pointerForCmdBuffer = 0;
    }

    private void printToConsole(String string) {
        sshConsoleHandler.addOutput(string, false, false);
    }
}
