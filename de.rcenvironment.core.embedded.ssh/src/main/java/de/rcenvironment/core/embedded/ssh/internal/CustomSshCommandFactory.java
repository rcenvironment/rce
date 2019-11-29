/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.shell.ShellFactory;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;

/**
 * The Shell and Command Factory for the SshConsole. Used to create SshcommandHandler instances to handle the given commands.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class CustomSshCommandFactory implements CommandFactory, ShellFactory {

    private CommandExecutionService commandExecutionService;

    private SshAuthenticationManager authenticationManager;

    private ScpContextManager scpContextManager;

    private ServerSideUplinkSessionService uplinkSessionService;

    private SshConfiguration sshConfiguration;

    // ShellFactory - Methods

    public CustomSshCommandFactory(SshAuthenticationManager authenticationManager, ScpContextManager scpContextManager,
        CommandExecutionService commandExecutionService, ServerSideUplinkSessionService uplinkSessionService,
        SshConfiguration sshConfiguration) {
        this.sshConfiguration = sshConfiguration;
        this.authenticationManager = authenticationManager;
        this.scpContextManager = scpContextManager;
        this.uplinkSessionService = uplinkSessionService;
        this.commandExecutionService = commandExecutionService;
    }

    @Override
    public Command createShell(ChannelSession channelSession) {
        return createCommand(null, null);
    }

    // CommandFactory - Methods

    @Override
    public Command createCommand(ChannelSession channelSession, String command) {
        Command result = null;
        if (command != null && command.trim().startsWith(SshConstants.SCP_COMMAND)) {
            // SCP commands
            result = new ScpCommandWrapper(command, scpContextManager);
        } else if (SshConstants.SSH_UPLINK_VIRTUAL_CONSOLE_COMMAND.equals(command)) {
            // Uplink pseudo-command execution
            result = new SshUplinkCommandHandler(uplinkSessionService, authenticationManager);
        } else {
            // SSH command execution and interactive shell
            result = new SshCommandHandler(command, authenticationManager, commandExecutionService, sshConfiguration);
        }
        return result;
    }
}
