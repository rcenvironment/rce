/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;

/**
 * The Shell and Command Factory for the SshConsole. Used to create SshcommandHandler instances to handle the given commands.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class CustomSshCommandFactory implements Factory<Command>, CommandFactory {

    private CommandExecutionService commandExecutionService;

    private SshAuthenticationManager authenticationManager;

    private ScpContextManager scpContextManager;

    private SshConfiguration sshConfiguration;

    // ShellFactory - Methods

    public CustomSshCommandFactory(SshAuthenticationManager authenticationManager, ScpContextManager scpContextManager,
        CommandExecutionService commandExecutionService, SshConfiguration sshConfiguration) {
        this.sshConfiguration = sshConfiguration;
        this.authenticationManager = authenticationManager;
        this.scpContextManager = scpContextManager;
        this.commandExecutionService = commandExecutionService;
    }

    @Override
    public Command create() {
        return createCommand(null);
    }

    // CommandFactory - Methods

    @Override
    public Command createCommand(String command) {
        Command result = null;
        if (command != null && command.trim().startsWith(SshConstants.SCP_COMMAND)) {
            result = new ScpCommandWrapper(command, scpContextManager);
        } else {
            result = new SshCommandHandler(command, authenticationManager, commandExecutionService, sshConfiguration);
        }
        return result;
    }
}
