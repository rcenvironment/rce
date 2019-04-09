/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractInteractiveCommandConsole;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Class for handling command input and printing out the output.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class SshConsoleOutputAdapter extends AbstractInteractiveCommandConsole {

    private static final String OUTPUT_TAB_REPLACEMENT = "    ";

    @Deprecated
    private boolean outputEnabled = true;

    private String activeUser;

    private String consolePromptString;

    private OutputStream out;

    private OutputStream err;

    private final CommandExecutionService commandExecutionService;

    private final Log logger = LogFactory.getLog(getClass());

    // TextOutputReciever Methods - START

    public SshConsoleOutputAdapter(CommandExecutionService commandExecutionService) {
        super(commandExecutionService);
        this.commandExecutionService = commandExecutionService;
    }

    @Override
    public void onStart() {
        // not needed
        // could be used to block input of new chars until output is done
    }

    @Override
    public void addOutput(String output) {
        addOutput(output, true, true);
    }

    /**
     * 
     * Adds an Output to the console.
     * 
     * @param output the output
     * @param lineBreak true, when a line break should be added
     * @param formatString true, when a output should be "formatted"
     */
    public void addOutput(String output, boolean lineBreak, boolean formatString) {
        if (formatString) {
            output = output.replace("\n", "\r\n");
            output = output.replaceAll("\t", OUTPUT_TAB_REPLACEMENT);
        }
        if (lineBreak) {
            output = output + "\r\n";
        }
        writeToConsole(output);
    }

    // TextOutputReciever Methods - END
    // Handling the output - START

    /**
     * 
     * Prints the help text to the console.
     * 
     * @param ce - the command exception (if it exists)
     */
    public void printHelp(CommandException ce) {
        commandExecutionService.printHelpText(false, false, this);
    }

    /**
     * 
     * Prints out a welcome message to the console. Includes a call to newConsoleLine.
     * 
     * @throws IOException
     */
    public void printWelcome() {
        StringBuilder welcome = new StringBuilder();
        welcome.append("Welcome to the RCE SSH console, " + activeUser + "!");
        // TODO idea: filter the "help" output using the role filter? - misc_ro
        welcome.append("\r\nType \"help\" for a list of console commands. Please note that depending on your "
            + "\r\naccount's security settings, you may not be able to use all commands.");
        welcome.append("\r\nType \"exit\" to end the current session.");
        addOutput(welcome.toString(), true, false);
    }

    /**
     * 
     * Prints out a line break and a new console prefix.
     * 
     */
    public void printConsolePrompt() {
        if (consolePromptString == null) {
            String template = SshConstants.CONSOLE_PROMPT_TEMPLATE;
            consolePromptString = StringUtils.format(template, activeUser);
        }
        writeToConsole(consolePromptString);
    }

    /**
     * 
     * For handling the exception and centrally blocking the output if in command mode.
     * 
     * @param output
     */
    private void writeToConsole(String output) {
        if (outputEnabled) {
            try {
                out.write(output.getBytes());
                out.flush();
            } catch (IOException e) {
                logger.error("Could not print " + output, e);
            }
        } else {
            logger.warn("Tried to add output, but output was not enabled: " + output);
        }
    }

    // Handling the output - END
    // Destroy the ConsoleHandler - START

    /**
     * 
     * Closes the streams for the console.
     * 
     */
    public void destroy() {
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing stdout stream of SSH shell: " + e.toString());
        }
        try {
            err.close();
        } catch (IOException e) {
            logger.error("Error closing stderr stream of SSH shell: " + e.toString());
        }
    }

    // Destroy the ConsoleHandler - END
    // Setter and Getter - START

    public void setOutputStream(OutputStream outParam) {
        out = outParam;
    }

    public void setErrorStream(OutputStream errParam) {
        err = errParam;
    }

    public void setActiveUser(String activeUser) {
        this.activeUser = activeUser;
    }

    public String getActiveUser() {
        return activeUser;
    }

    // Setter and Getter - END

}
