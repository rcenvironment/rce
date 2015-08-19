/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.console.osgi;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * {@link CommandProvider} for the 'rce' command on the Equinox OSGi console.
 * 
 * @author Robert Mischke
 */
public class RCECommandProvider implements CommandProvider {

    private CommandExecutionService commandExecutionService;

    /**
     * Handler for the RCE top-level command. Delegates to the {@link CommandExecutionService}.
     * 
     * @param interpreter the provided {@link CommandInterpreter}
     * @return null
     */
    public Object _rce(final CommandInterpreter interpreter) {
        final List<String> tokens = getTokens(interpreter);
        final TextOutputReceiver outputReceiver = new OsgiConsoleOutputAdapter(interpreter, commandExecutionService);
        commandExecutionService.asyncExecMultiCommand(tokens, outputReceiver, "osgi console");
        return null;
    }

    private List<String> getTokens(final CommandInterpreter interpreter) {
        final List<String> tokens = new LinkedList<String>();
        String argument;
        while ((argument = interpreter.nextArgument()) != null) {
            tokens.add(argument);
        }
        return tokens;
    }

    protected void bindCommandExecutionService(CommandExecutionService newService) {
        this.commandExecutionService = newService;
    }

    @Override
    public String getHelp() {
        return commandExecutionService.getHelpText(true, false); // with prefix, no developer commands
    }

}
