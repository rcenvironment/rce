/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Execution handler for "ssh [...]" commands.
 *
 * @author Brigitte Boden
 */
public class SshConnectionSetupCommandPlugin implements CommandPlugin {

    private static final String CMD_SSH = "ssh";

    private SshConnectionService sshConnectionService;

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.command.spi.SingleCommandHandler#execute(de.rcenvironment.core.command.spi.CommandContext)
     */
    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(CMD_SSH);
        String subCmd = context.consumeNextToken();
        if (subCmd == null) {
            // "ssh" -> "ssh list" by default
            performList(context);
        } else {
            List<String> parameters = context.consumeRemainingTokens();
            if ("add".equals(subCmd)) {
                performAdd(context, parameters);
            } else if ("list".equals(subCmd)) {
                performList(context);
            } else if ("start".equals(subCmd)) {
                performStart(context, parameters);
            } else if ("stop".equals(subCmd)) {
                performStop(context, parameters);
            } else {
                throw CommandException.unknownCommand(context);
            }
        }
    }

    private void performAdd(final CommandContext context, List<String> parameters) throws CommandException {
        if (parameters.size() < 5 || parameters.size() > 5) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        final String connectionName = parameters.get(0);
        final String host = parameters.get(1);
        final int port = Integer.parseInt(parameters.get(2));
        final String username = parameters.get(3);
        final String keyfileLocation = parameters.get(4);

        SshConnectionContext contextSSH = new SshConnectionContext(null, connectionName, null, host, port, username,
            keyfileLocation, false, false, false, false);

        if (sshConnectionService.sshConnectionAlreadyExists(contextSSH)) {
            context.println(StringUtils.format("Connection setup to host '%s:%d' already exists.", contextSSH.getDestinationHost(),
                contextSSH.getPort()));
            return;
        }

        ConcurrencyUtils.getAsyncTaskService().execute("Create new SSH Connection.", () -> {
            // add contextSSH here instead of new SshConnectionContext
            String id = sshConnectionService.addSshConnection(contextSSH);
            context.println("Added SSH connection setup, created id " + id);
        });

    }

    private void performList(CommandContext context) {
        Collection<SshConnectionSetup> setups = sshConnectionService.getAllSshConnectionSetups();
        for (SshConnectionSetup setup : setups) {
            context.println(StringUtils.format("%s: %s:%s username: %s, keyfile: %s (id: %s)", setup.getDisplayName(), setup.getHost(),
                setup.getPort(), setup.getUsername(), setup.getKeyfileLocation(), setup.getId()));
        }
    }

    private void performStart(CommandContext context, List<String> parameters) throws CommandException {
        if (parameters.size() < 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        final String connectionId = parameters.get(0);

        ConcurrencyUtils.getAsyncTaskService().execute("Start SSH Connection.", () -> {

            sshConnectionService.connectSession(connectionId);

        });
    }

    private void performStop(CommandContext context, List<String> parameters) throws CommandException {
        if (parameters.size() < 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String connectionId = parameters.get(0);
        sshConnectionService.disconnectSession(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.command.spi.CommandPlugin#getCommandDescriptions()
     */
    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_SSH, "", false, "short form of \"ssh list\""));
        contributions.add(new CommandDescription(CMD_SSH + " add", "<displayName> <host> <port> <username> <keyfileLocation>", false,
            "add a new ssh connection", "(Example: TODO)"));
        contributions.add(new CommandDescription(CMD_SSH + " list", "", false,
            "lists all ssh connections, including ids and connection states"));
        contributions.add(new CommandDescription(CMD_SSH + " start", "<id>", false,
            "starts/connects an ssh connection (use \" " + CMD_SSH + " list\" to get the id)"));
        contributions.add(new CommandDescription(CMD_SSH + " stop", "<id>", false,
            "stops/disconnects an ssh connection (use \" " + CMD_SSH + " list\" to get the id)"));
        return contributions;
    }

    /**
     * OSGI bind method.
     * 
     * @param newInstance instance of {@link SshConnectionService} to bind.
     */
    public void bindSshConnectionSetupService(SshConnectionService newInstance) {
        this.sshConnectionService = newInstance;
    }
}
