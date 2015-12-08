/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Class for handling command execution. Does not handle SCP-Commands.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class SshCommandHandler implements Command, Runnable, SessionAware {

    private InputStream in;

    private String sshCommand;

    private String loginName;

    private ExitCallback callback;

    private Environment environment;

    private SshAuthenticationManager authenticationManager;

    private CommandExecutionService commandExecutionService;

    private SshConsoleOutputAdapter outputAdapter;

    private final Log logger = LogFactory.getLog(getClass());

    private SshAccount userAccount;

    public SshCommandHandler(String sshCommand, SshAuthenticationManager authenticationManager,
        CommandExecutionService commandExecutionService, SshConfiguration sshConfiguration) {
        this.sshCommand = sshCommand;
        this.authenticationManager = authenticationManager;
        this.commandExecutionService = commandExecutionService;
        this.outputAdapter = new SshConsoleOutputAdapter(commandExecutionService);
    }

    // Handling the thread - START

    @Override
    public void start(Environment env) throws IOException {
        // start thread
        environment = env;
        loginName = environment.getEnv().get(Environment.ENV_USER);
        userAccount = authenticationManager.getAccountByLoginName(loginName, false); // false = do not allow disabled
        if (userAccount == null) {
            outputAdapter.addOutput("Invalid/unknown login name: " + loginName);
            logger.warn("Blocked unrecognized SSH account " + loginName);
            callback.onExit(0);
        }
        // initialize console (if user is not a temp user)
        if (isPotentiallyAllowedToRunCommands()) {
            outputAdapter.setActiveUser(loginName);
            // TODO review: thread safety? - misc_ro
            SharedThreadPool.getInstance().execute(this);
        } else {
            outputAdapter.addOutput("Your account is not allowed to run an interactive shell or execute commands.");
            logger.warn("Blocked command/shell access for account " + loginName);
            callback.onExit(0);
        }
    }

    private boolean isPotentiallyAllowedToRunCommands() {
        // TODO temporarily allowed for both account types; review
        return true;
        // return activeUser.startsWith(SshConstants.TEMP_USER_PREFIX);
    }

    @Override
    @TaskDescription("SSH command session")
    public void run() {
        logger.debug("Started SSH command shell");
        try {
            if (sshCommand == null) {
                // run interactive shell/console
                runInteractiveShellLoop();
                callback.onExit(0);
            } else {
                // execute single provided command and exit
                CommandExecutionResult result = executeSingleCommand(sshCommand);
                switch (result) {
                case DEFAULT:
                case EXIT_REQUESTED:
                    callback.onExit(0);
                    break;
                case ERROR:
                case INTERRUPTED:
                    callback.onExit(1);
                    break;
                default:
                    throw new IllegalArgumentException();
                }
            }
        } catch (IOException e) {
            // not logging the full stacktrace as it is usually irrelevant, and this case happens frequently
            logger.error("I/O error in interactive SSH session - the client may have closed the connection: " + e.toString());
            callback.onExit(1);
        }
        // End Console (Closes the connection)
        logger.debug("Command processing for shell ended for user: " + loginName);
    }

    @Override
    public void destroy() {
        // close resources
        try {
            in.close();
        } catch (IOException e) {
            logger.error("Could not close input stream. ", e);
        }
        outputAdapter.destroy();

        // End thread and console
        callback.onExit(0);
    }

    // Handling the thread - END
    // Handling the commands - START

    private void runInteractiveShellLoop() throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        InteractiveShellHandler commandBuffer = new InteractiveShellHandler(outputAdapter);
        outputAdapter.printWelcome();
        outputAdapter.printConsolePrompt();
        while (true) {
            int newCharCode = r.read();
            // stop reading and start executing on 13 no matter what
            if (newCharCode == SshConstants.RETURN_KEY_CODE) {
                String command = commandBuffer.getCurrentCommand();
                outputAdapter.addOutput("", true, false);
                CommandExecutionResult result = executeSingleCommand(command);
                if (result == CommandExecutionResult.EXIT_REQUESTED) {
                    return;
                }
                outputAdapter.printConsolePrompt();
            } else {
                if (commandBuffer.processInputChar(newCharCode)) {
                    if (newCharCode != SshConstants.DEL_KEY_CODE) {
                        outputAdapter.addOutput("" + (char) newCharCode, false, false);
                    } else {
                        outputAdapter.addOutput("\b \b", false, false);
                    }
                }
            }
        }
    }

    private CommandExecutionResult executeSingleCommand(String command) throws IOException {
        if (command != null && !command.isEmpty()) {
            if (authenticationManager.isAllowedToExecuteConsoleCommand(loginName, command)) {
                if (command.equalsIgnoreCase(SshConstants.EXIT_COMMAND)) {
                    // stop interactive shell on exit command
                    return CommandExecutionResult.EXIT_REQUESTED;
                } else {
                    // TODO review: this logs all console commands, which may be a problem when the log is accessible from remote - misc_ro
                    logger.debug(StringUtils.format("Executing shell command '%s' for user '%s'", command, loginName));
                    // TODO pass invoker information for non-temporary accounts as well
                    return sendToExecutionService(command, userAccount);
                }
            } else {
                logger.debug("User " + loginName + " tried to execute command " + command
                    + ". Attempt was blocked because of missing role privileges.");
                outputAdapter.addOutput("\r\nCommand " + command
                    + " not executed. You either do not have the privileges to execute this command or it does not exist.", true, false);
                return CommandExecutionResult.ERROR; // TODO add more specific result?
            }
        } else {
            return CommandExecutionResult.DEFAULT;
        }
    }

    private CommandExecutionResult sendToExecutionService(String command, Object invokerInfo) {
        List<String> tokens;
        if (command.startsWith("rce")) {
            command = command.replace("rce", "");
        }
        tokens = Arrays.asList(command.trim().split("\\s+"));
        Future<CommandExecutionResult> resultFuture = commandExecutionService.asyncExecMultiCommand(tokens, outputAdapter, invokerInfo);
        try {
            // wait for termination
            return resultFuture.get();
        } catch (InterruptedException e) {
            outputAdapter.addOutput("Command execution interrupted: " + e.toString());
            logger.error("Interrupted while waiting for asynchronous command to finish", e);
            return CommandExecutionResult.INTERRUPTED;
        } catch (ExecutionException e) {
            outputAdapter.addOutput("Error during command execution: " + e.toString());
            logger.error("Error during command execution", e);
            return CommandExecutionResult.ERROR;
        }
    }

    // Handling the commands - END
    // Getter and Setter - START

    @Override
    public void setInputStream(InputStream inParam) {
        this.in = inParam;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        outputAdapter.setOutputStream(out);
    }

    @Override
    public void setErrorStream(OutputStream err) {
        outputAdapter.setErrorStream(err);
    }

    @Override
    public void setExitCallback(ExitCallback callbackParam) {
        this.callback = callbackParam;
    }

    @Override
    public void setSession(ServerSession sessionParam) {}

    // Getter and Setter - END

}
