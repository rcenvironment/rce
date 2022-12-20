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
 * Provides the "restart" console command.
 * 
 * @author Sascha Zur
 */
@Component
public class RestartCommandPlugin implements CommandPlugin {

    private static final String CMD_RESTART = "restart";

    private static final String DESC = "restart RCE";
    
    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(CMD_RESTART, DESC, DESC, this::performRestart);
        return new MainCommandDescription[] { commands };
    }

    /**
     * Handler for the "restart" sub-command.
     * 
     * @return String the console output
     */
    private void performRestart(CommandContext context) {
        context.println("Restarting RCE ...");
        Instance.restart("console command");
    }

}
