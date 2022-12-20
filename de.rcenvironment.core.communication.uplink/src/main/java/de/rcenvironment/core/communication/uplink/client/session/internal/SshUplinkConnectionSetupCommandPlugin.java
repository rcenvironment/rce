/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.internal;

import java.util.Collection;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.BooleanParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.ParsedBooleanParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
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

    private static final String CONNECTION_WITH_ID = "connection with id ";

    private static final String CMD_SSH_UPLINK = "uplink";

    private static final String DESC = " list\" to get the id)";
    
    private static final StringParameter ID_PARAMETER = new StringParameter(null, "id", "id of the uplink");
    
    private static final StringParameter DISPLAY_NAME_PRAMETER = new StringParameter(null, "display name", "display name");
    
    private static final StringParameter HOST_IP_PARAMETER = new StringParameter(null, "host", "host ip adress");
    
    private static final IntegerParameter PORT_PARAMETER = new IntegerParameter(0, "port", "port for the ssh connection");
    
    private static final StringParameter USERNAME_PARAMETER = new StringParameter(null, "username", "username");
    
    private static final StringParameter KEYFILE_LOCATION_PARAMETER = new StringParameter(null, "key file location",
            "location of the keyfile");
    
    private static final StringParameter CLIENT_ID_PARAMETER = new StringParameter(null, "client id", "id of the client");
    
    private static final BooleanParameter IS_GATEWAY_PARAMETER = new BooleanParameter(false, "is gateway",
            "controls the isGateway property of the uplink");
    
    @Reference
    private SshUplinkConnectionService sshConnectionService;

    private void performAdd(final CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter displayNameParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter hostIpParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        ParsedIntegerParameter portParameter = (ParsedIntegerParameter) modifiers.getPositionalCommandParameter(2);
        ParsedStringParameter usernameParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(3);
        ParsedStringParameter keyfileLocationParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(4);
        ParsedStringParameter clientIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(5);
        ParsedBooleanParameter isGatewayParameter = (ParsedBooleanParameter) modifiers.getPositionalCommandParameter(6);

        final String connectionName = displayNameParameter.getResult();
        final String host = hostIpParameter.getResult();
        final int port = portParameter.getResult();
        final String username = usernameParameter.getResult();
        final String keyfileLocation = keyfileLocationParameter.getResult();
        final boolean isGateway = isGatewayParameter.getResult();
        final String clientId = clientIdParameter.getResult();

        SshConnectionContext contextUplinkSSH = new SshConnectionContext(null, connectionName, clientId, host, port,
                username, keyfileLocation, false, false, false, isGateway);

        if (sshConnectionService.sshUplinkConnectionAlreadyExists(contextUplinkSSH)) {
            context.println(StringUtils.format("Connection setup to host '%s:%d' already exists.",
                    contextUplinkSSH.getDestinationHost(), contextUplinkSSH.getPort()));
            return;
        }
        ConcurrencyUtils.getAsyncTaskService().execute("Create new Uplink Connection.", new Runnable() {

            @Override
            public void run() {
                String id = sshConnectionService.addSshUplinkConnection(contextUplinkSSH);
                context.println("Added Uplink connection setup, created id " + id);
            }
        });
    }

    private void performList(CommandContext context) {
        Collection<SshUplinkConnectionSetup> setups = sshConnectionService.getAllSshConnectionSetups();
        for (SshUplinkConnectionSetup setup : setups) {
            context.println(StringUtils.format(
                    "%s: %s:%s username: %s, keyfile: %s clientId: %s isGateway: %s (id: %s) CONNECTED: %s",
                    setup.getDisplayName(), setup.getHost(), setup.getPort(), setup.getUsername(),
                    setup.getKeyfileLocation(), setup.getQualifier(), setup.isGateway(), setup.getId(),
                    setup.isConnected()));
        }
    }

    private void performStart(CommandContext context) {

        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedStringParameter connectionIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        String connectionId = connectionIdParameter.getResult();
        
        ConcurrencyUtils.getAsyncTaskService().execute("Start SSH Connection.", new Runnable() {

            @Override
            public void run() {
                if (sshConnectionService.getConnectionSetup(connectionId) == null) {
                    context.println(CONNECTION_WITH_ID + connectionId + " does not exist.");
                } else if (sshConnectionService.getConnectionSetup(connectionId).isConnected()) {
                    context.println(CONNECTION_WITH_ID + connectionId + "is already connected.");
                } else {
                    sshConnectionService.connectSession(connectionId);
                }
            }
        });
    }

    private void performStop(CommandContext context) {

        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedStringParameter connectionIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        String connectionId = connectionIdParameter.getResult();
        
        ConcurrencyUtils.getAsyncTaskService().execute("Start SSH Connection.", new Runnable() {

            @Override
            public void run() {
                if (sshConnectionService.getConnectionSetup(connectionId) != null) {
                    sshConnectionService.disconnectSession(connectionId);
                } else {
                    context.println(CONNECTION_WITH_ID + connectionId + " does not exist.");
                }
            }
        });
    }

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(CMD_SSH_UPLINK, "manage uplink connections",
            "short form for \"uplink list\"", this::performList,
            new SubCommandDescription("add", "add a new uplink connection", this::performAdd,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        DISPLAY_NAME_PRAMETER,
                        HOST_IP_PARAMETER,
                        PORT_PARAMETER,
                        USERNAME_PARAMETER,
                        KEYFILE_LOCATION_PARAMETER,
                        CLIENT_ID_PARAMETER,
                        IS_GATEWAY_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("list",
                "lists all uplink connections, including ids and connection states", this::performList),
            new SubCommandDescription("start",
                "starts/connects an uplink connection (use \" " + CMD_SSH_UPLINK + DESC,
                this::performStart,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("stop",
                "stops/disconnects an uplink connection (use \" " + CMD_SSH_UPLINK + DESC,
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

}
