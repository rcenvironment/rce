/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionService;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionSetup;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Execution handler for "uplink [...]" commands.
 *
 * @author Brigitte Boden
 */
@Component
public class SshUplinkConnectionSetupCommandPlugin implements CommandPlugin {

    private static final String CONNECTION_WITH_ID = "Connection with id ";

    private static final String CMD_SSH_UPLINK = "uplink";

    @Reference
    private SshUplinkConnectionService sshConnectionService;

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.command.spi.SingleCommandHandler#execute(de.rcenvironment.core.command.spi.CommandContext)
     */
    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(CMD_SSH_UPLINK);
        String subCmd = context.consumeNextToken();
        if (subCmd == null) {
            // "uplink" -> "uplink list" by default
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

    private void performAdd(final CommandContext context,
        List<String> parameters) throws CommandException {
        if (parameters.size() < 6 || parameters.size() > 7) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        final String connectionName = parameters.get(0);
        final String host = parameters.get(1);
        final int port = Integer.parseInt(parameters.get(2));
        final String username = parameters.get(3);
        final String keyfileLocation = parameters.get(4);
        final boolean isGateway;
        final String clientId = parameters.get(5);
        if (parameters.size() == 7) {
            isGateway = Boolean.parseBoolean(parameters.get(6));
        } else {
            isGateway = false;
        }

        SshConnectionContext contextUplinkSSH = new SshConnectionContext(null,
            connectionName, clientId, host, port, username, keyfileLocation,
            false, false, false, isGateway);

        if (sshConnectionService
            .sshUplinkConnectionAlreadyExists(contextUplinkSSH)) {
            context.println(StringUtils.format("Connection setup to host '%s:%d' already exists.", contextUplinkSSH.getDestinationHost(),
                contextUplinkSSH.getPort()));
            return;
        }
        ConcurrencyUtils.getAsyncTaskService()
            .execute("Create new Uplink Connection.", new Runnable() {

                @Override
                public void run() {
                    String id = sshConnectionService
                        .addSshUplinkConnection(contextUplinkSSH);
                    context.println(
                        "Added Uplink connection setup, created id "
                            + id);
                }
            });
    }

    private void performList(CommandContext context) {
        Collection<SshUplinkConnectionSetup> setups = sshConnectionService
            .getAllSshConnectionSetups();
        for (SshUplinkConnectionSetup setup : setups) {
            context.println(StringUtils.format(
                "%s: %s:%s username: %s, keyfile: %s clientId: %s isGateway: %s (id: %s) CONNECTED: %s",
                setup.getDisplayName(), setup.getHost(), setup.getPort(),
                setup.getUsername(), setup.getKeyfileLocation(),
                setup.getQualifier(), setup.isGateway(), setup.getId(),
                setup.isConnected()));
        }
    }

    private void performStart(CommandContext context, List<String> parameters)
        throws CommandException {
        if (parameters.size() < 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        final String connectionId = parameters.get(0);

        ConcurrencyUtils.getAsyncTaskService().execute("Start SSH Connection.",
            new Runnable() {

                @Override
                public void run() {
                    if (sshConnectionService
                        .getConnectionSetup(connectionId) == null) {
                        context.println(CONNECTION_WITH_ID + connectionId
                            + " does not exist.");
                    } else if (sshConnectionService
                        .getConnectionSetup(connectionId)
                        .isConnected()) {
                        context.println(CONNECTION_WITH_ID + connectionId
                            + "is already connected.");
                    } else {
                        sshConnectionService.connectSession(connectionId);
                    }
                }
            });
    }

    private void performStop(CommandContext context, List<String> parameters)
        throws CommandException {
        if (parameters.size() < 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String connectionId = parameters.get(0);
        ConcurrencyUtils.getAsyncTaskService().execute("Start SSH Connection.",
            new Runnable() {

                @Override
                public void run() {
                    if (sshConnectionService
                        .getConnectionSetup(connectionId) != null) {
                        sshConnectionService
                            .disconnectSession(connectionId);
                    } else {
                        context.println(CONNECTION_WITH_ID + connectionId
                            + " does not exist.");
                    }
                }
            });
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.command.spi.CommandPlugin#getCommandDescriptions()
     */
    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_SSH_UPLINK, "", false,
            "short form of \"uplink list\""));
        contributions.add(new CommandDescription(CMD_SSH_UPLINK + " add",
            "<displayName> <host> <port> <username> <keyfileLocation> "
                + "<clientId>  <isGateway>",
            false, "add a new uplink connection"));
        contributions.add(new CommandDescription(CMD_SSH_UPLINK + " list", "",
            false,
            "lists all uplink connections, including ids and connection states"));
        contributions.add(new CommandDescription(CMD_SSH_UPLINK + " start",
            "<id>", false, "starts/connects an uplink connection (use \" "
                + CMD_SSH_UPLINK + " list\" to get the id)"));
        contributions.add(new CommandDescription(CMD_SSH_UPLINK + " stop",
            "<id>", false, "stops/disconnects an uplink connection (use \" "
                + CMD_SSH_UPLINK + " list\" to get the id)"));
        return contributions;
    }

}
