/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;

/**
 * Execution handler for "cn [...]" commands.
 * 
 * @author Robert Mischke
 */
public class ConnectionSetupCommandPlugin implements CommandPlugin {

    private static final String CMD_CN = "cn";

    private ConnectionSetupService connectionSetupService;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_CN, "", false, "short form of \"cn list\""));
        contributions.add(new CommandDescription("cn add", "<target> [\"<description>\"]", false, "add a new network connection",
            "(Example: cn add activemq-tcp:rceserver.example.com:20001 \"Our RCE Server\")"));
        contributions.add(new CommandDescription("cn list", "", false,
            "lists all network connections, including ids and connection states"));
        contributions.add(new CommandDescription("cn start", "<id>", false,
            "starts/connects a READY or DISCONNECTED connection (use \"cn list\" to get the id)"));
        contributions.add(new CommandDescription("cn stop", "<id>", false,
            "stops/disconnects an ESTABLISHED connection (use \"cn list\" to get the id)"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(CMD_CN);
        String subCmd = context.consumeNextToken();
        if (subCmd == null) {
            // "cn" -> "cn list" by default
            performList(context);
        } else {
            List<String> parameters = context.consumeRemainingTokens();
            if ("add".equals(subCmd)) {
                // "rce net add <...>"
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

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindConnectionSetupService(ConnectionSetupService newInstance) {
        this.connectionSetupService = newInstance;
    }

    private void performList(CommandContext context) {
        Collection<ConnectionSetup> setups = connectionSetupService.getAllConnectionSetups();
        for (ConnectionSetup setup : setups) {
            String optionalSuffix = "";
            if (true) { // show channel ids
                String currentChannelId = setup.getCurrentChannelId();
                if (currentChannelId != null) {
                    optionalSuffix = " [" + currentChannelId + "]";
                }
            }
            context.println(String.format("  (%d) \"%s\" [%s] - %s%s", setup.getId(), setup.getDisplayName(),
                setup.getNetworkContactPointString(),
                setup.getState(), optionalSuffix));
        }
    }

    private void performAdd(CommandContext context, List<String> parameters) throws CommandException {
        if (parameters.size() < 1 || parameters.size() > 2) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String contactPointStr = parameters.get(0);
        NetworkContactPoint ncp;
        try {
            ncp = NetworkContactPointUtils.parseStringRepresentation(contactPointStr);
        } catch (IllegalArgumentException e1) {
            context.println("Invalid target description: " + contactPointStr);
            return;
        }
        String displayName;
        if (parameters.size() == 2) {
            displayName = parameters.get(1);
        } else {
            displayName = "<" + contactPointStr + ">";
        }
        ConnectionSetup setup = connectionSetupService.createConnectionSetup(ncp, displayName, true);
        context.println("Connection added, id=" + setup.getId());
        performList(context);
    }

    private void performStart(CommandContext context, List<String> parameters) {
        if (parameters.size() < 1) {
            context.println("Error: missing connection id");
            // TODO print standard help?
            return;
        }
        long id;
        try {
            id = Long.parseLong(parameters.get(0));
        } catch (NumberFormatException e) {
            context.println("Error: invalid connection id");
            return;
        }
        ConnectionSetup setup = connectionSetupService.getConnectionSetupById(id);
        if (setup == null) {
            context.println("Error: unknown connection id");
            return;
        }
        // validated -> perform action
        setup.signalStartIntent();

        // TODO add synchronous option as well; decide which should be default
        // try {
        // } catch (CommunicationException e) {
        // log.error("Error on connection attempt", e);
        // context.println("Error on connection attempt: " + e.toString());
        // return;
        // }
    }

    private void performStop(CommandContext context, List<String> parameters) {
        if (parameters.size() < 1) {
            context.println("Error: missing connection id");
            // TODO print standard help?
            return;
        }
        long id;
        try {
            id = Long.parseLong(parameters.get(0));
        } catch (NumberFormatException e) {
            context.println("Error: invalid connection id");
            return;
        }
        ConnectionSetup setup = connectionSetupService.getConnectionSetupById(id);
        if (setup == null) {
            context.println("Error: unknown connection id");
            return;
        }
        // validated -> perform action
        setup.signalStopIntent();

        // TODO add synchronous option as well; decide which should be default
    }
}
