/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.internal;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.service.component.annotations.Component;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLogIds;

/**
 * Provides the "restart" console command.
 * 
 * @author Sascha Zur
 */
@Component
public class RestartCommandPlugin implements CommandPlugin {

    private static final String CMD_RESTART = "restart";

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_RESTART, "", false, "restart RCE"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        String cmd = context.consumeNextToken();
        if (CMD_RESTART.equals(cmd)) {
            performRestart(context);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Handler for the "restart" sub-command.
     * 
     * @return String the console output
     */
    private void performRestart(CommandContext context) {
        AuditLog.append(AuditLog.newEntry(AuditLogIds.APPLICATION_SHUTDOWN_REQUESTED)
            .set("method", "console command")
            .set("is_restart", "true"));
        context.println("Restarting RCE ...");
        Instance.restart();
    }

}
