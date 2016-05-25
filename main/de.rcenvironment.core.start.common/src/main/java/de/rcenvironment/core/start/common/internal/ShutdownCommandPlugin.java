/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.internal;

import java.util.ArrayList;
import java.util.Collection;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.start.common.Instance;

/**
 * Provides the (synonymous) "stop" and "shutdown" console commands.
 * 
 * @author Robert Mischke
 */
public class ShutdownCommandPlugin implements CommandPlugin {

    private static final String CMD_SHUTDOWN = "shutdown";

    private static final String CMD_STOP = "stop";

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_SHUTDOWN, "", false, "shut down RCE"));
        contributions.add(new CommandDescription(CMD_STOP, "", false, "shut down RCE (alias of \"shutdown\")"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        String cmd = context.consumeNextToken();
        if (CMD_SHUTDOWN.equals(cmd)) {
            performStop(context);
        } else if (CMD_STOP.equals(cmd)) {
            performStop(context);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Handler for the "stop" sub-command.
     * 
     * @return String the console output
     */
    private void performStop(CommandContext context) {
        context.println("Shutting down; if you are on an interactive OSGi console, type 'exit' to close it");
        Instance.shutdown();
    }
    
}
