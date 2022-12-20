/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.internal;

import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.start.common.Instance;

/**
 * Provides the (synonymous) "stop" and "shutdown" console commands.
 * 
 * @author Robert Mischke
 */
@Component
public class ShutdownCommandPlugin implements CommandPlugin {

    private static final String CMD_SHUTDOWN = "shutdown";

    private static final String CMD_STOP = "stop";

    @Override
    public MainCommandDescription[] getCommands() {
        return new MainCommandDescription[] {
            new MainCommandDescription(CMD_SHUTDOWN, "shut down RCE", "shut down RCE", this::performStop),
            new MainCommandDescription(CMD_STOP, "shut down RCE (alias of \"shutdown\")",
                "shut down RCE (alias of \"shutdown\")", this::performStop)
        };
    }
    
    /**
     * Handler for the "stop" sub-command.
     * 
     * @return String the console output
     */
    private void performStop(CommandContext context) {
        context.println("Shutting down; if you are on an interactive OSGi console, type 'exit' to close it");
        Instance.shutdown("console command");
    }

}
