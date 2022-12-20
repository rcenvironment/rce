/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.internal;

import java.util.Collection;

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

    private static final String DESC = " list\" to get the id)";
    
    private static final StringParameter ID_PARAMETER = new StringParameter(null, "id", "id for the ssh connection");
    
    private static final StringParameter DISPLAY_NAME_PRAMETER = new StringParameter(null, "display name",
            "display name for the ssh connection");
    
    private static final StringParameter HOST_IP_PARAMETER = new StringParameter(null, "host",
            "host for the ssh connection");
    
    private static final IntegerParameter PORT_PARAMETER = new IntegerParameter(0, "port", "port for the ssh connection");
    
    private static final StringParameter USERNAME_PARAMETER = new StringParameter(null, "username",
            "username for the ssh connection");
    
    private static final StringParameter KEYFILE_LOCATION_PARAMETER = new StringParameter(null, "key file location",
            "location of the key file");
    
    private SshConnectionService sshConnectionService;

    private void performAdd(final CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter displayNameParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter hostIpParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        ParsedIntegerParameter portParameter = (ParsedIntegerParameter) modifiers.getPositionalCommandParameter(2);
        ParsedStringParameter usernameParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(3);
        ParsedStringParameter keyfileLocationParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(4);

        final String connectionName = displayNameParameter.getResult();
        final String host = hostIpParameter.getResult();
        final int port = portParameter.getResult();
        final String username = usernameParameter.getResult();
        final String keyfileLocation = keyfileLocationParameter.getResult();

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

    private void performStart(CommandContext context) {
        
        final String connectionId = ((ParsedStringParameter) context.getParsedModifiers().getPositionalCommandParameter(0)).getResult();

        ConcurrencyUtils.getAsyncTaskService().execute("Start SSH Connection.", () -> 
            sshConnectionService.connectSession(connectionId));
    }

    private void performStop(CommandContext context) {
        
        String connectionId = ((ParsedStringParameter) context.getParsedModifiers().getPositionalCommandParameter(0)).getResult();
        
        sshConnectionService.disconnectSession(connectionId);
    }

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription("ssh", "manage ssh connections",
                "short form of \"ssh list\"", this::performList,
            new SubCommandDescription("add", "add a new ssh connection", this::performAdd,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        DISPLAY_NAME_PRAMETER,
                        HOST_IP_PARAMETER,
                        PORT_PARAMETER,
                        USERNAME_PARAMETER,
                        KEYFILE_LOCATION_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("list", "lists all ssh connections, including ids and connection states", this::performList),
            new SubCommandDescription("start", "starts/connects an ssh connection (use \" " + CMD_SSH + DESC, this::performStart,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("stop", "stops/disconnects an ssh connection (use \" " + CMD_SSH + DESC, this::performStop,
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
     * OSGI bind method.
     * 
     * @param newInstance instance of {@link SshConnectionService} to bind.
     */
    public void bindSshConnectionSetupService(SshConnectionService newInstance) {
        this.sshConnectionService = newInstance;
    }
    
}
