/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.internal;

import java.util.Collection;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Execution handler for "cn [...]" commands.
 * 
 * @author Robert Mischke
 */
public class ConnectionSetupCommandPlugin implements CommandPlugin {

    private static final String CMD_CN = "cn";
    
    private static final String ID_DESC = "id";

    private static final StringParameter TARGET_PARAMETER = new StringParameter(null, "target", "target of the connection");
    
    private static final StringParameter DESCRIPTION_PARAMETER = new StringParameter(null, "description",
            "description of the connection");
    
    private static final IntegerParameter ID_PARAMETER = new IntegerParameter(null, ID_DESC, "id of the connection");
    
    private ConnectionSetupService connectionSetupService;

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(CMD_CN, "manage network connections",
            "alias for \"cn list\"", this::performList,
            new SubCommandDescription("add", "add a new network connection (Example: cn add "
                +  "activemq-tcp:rceserver.example.com:20001 \"Our RCE Server\")", this::performAdd,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        TARGET_PARAMETER,
                        DESCRIPTION_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("list", "lists all network connections, including ids and connection states",
                this::performList),
            new SubCommandDescription("start", "starts/connects a READY or DISCONNECTED connection (use \"cn list\" to get the id)",
                this::performStart,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("stop", "stops/disconnects an ESTABLISHED connection (use \"cn list\" to get the id)",
                this::performStop,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            )
        );
        return new MainCommandDescription[] { commands };
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
            context.println(StringUtils.format("  (%d) '%s' [%s] - %s%s", setup.getId(), setup.getDisplayName(),
                setup.getNetworkContactPointString(), setup.getState(), optionalSuffix));
        }
    }

    private void performAdd(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter descParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        String contactPointStr = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        
        NetworkContactPoint ncp;
        try {
            ncp = NetworkContactPointUtils.parseStringRepresentation(contactPointStr);
        } catch (IllegalArgumentException e1) {
            context.println("Invalid target description: " + contactPointStr);
            return;
        }
        String displayName;
        if (descParameter.getResult() != null) {
            displayName = descParameter.getResult();
        } else {
            displayName = "<" + contactPointStr + ">";
        }

        if (connectionSetupService.connectionAlreadyExists(ncp)) {
            context.println(StringUtils.format("Connection setup to host '%s:%d' already exists.", ncp.getHost(), ncp.getPort()));
            return;
        }

        ConnectionSetup setup = connectionSetupService.createConnectionSetup(ncp, displayName, true);
        context.println("Connection added, id=" + setup.getId());
        performList(context);
    }

    private void performStart(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        long id = ((ParsedIntegerParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        
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

    private void performStop(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        long id = ((ParsedIntegerParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        
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
