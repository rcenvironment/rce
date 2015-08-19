/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.command.UnknownCommand;

import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.ssh.jsch.executor.JSchCommandLineExecutor;

/**
 * Provides utility methods for this test bundle.
 * @author Doreen Seider
 */
public final class Utils {

    /** Time out for test methods interacting with embedded SSH server. */
    public static final int TIMEOUT = 30000;
    
    private static final int PORT_RANGE = 1000;

    private static final int MIN_PORT = 9000;
    
    private Utils() {}
    
    /**
     * Create a {@link CommandFactory}.
     * @param succeedingCommand command which should result in success
     * @param stdout stdout returned fir succeeding command
     * @param failingCommand command which should result in failure
     * @param stderr stderr returned for failing command
     * @return {@link CommandFactory}
     */
    public static CommandFactory createDummyCommandFactory(final String succeedingCommand, final String stdout,
        final String failingCommand, final String stderr) {
        
        return new CommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (succeedingCommand.equals(command)) {
                    return new DummyCommand(stdout, null);
                } else if (failingCommand.equals(command)) {
                    return new DummyCommand(null, stderr, 1);
                } else {
                    return new DummyCommand();
                }
            }
        };
    }
    
    /**
     * @return random port number (port range: 9000 - 10000)
     */
    public static int getRandomPortNumber() {
        return new Random().nextInt(PORT_RANGE) + MIN_PORT;
    }
    /**
     * Create a {@link CommandFactory}.
     * @return {@link CommandFactory}
     */
    public static CommandFactory createDummyCommandFactory() {
        return createDummyCommandFactory("", null, "", null);
    }
    
    /**
     * Creates a file on server side.
     * @param sshServer {@link SshServer} to use
     * @param session {@link Session} to use
     * @param filename name of file to create
     * @param fileContent content of file to create
     * @param executor executor to use
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    public static void createFileOnServerSidesWorkDir(SshServer sshServer, Session session, final String filename,
        final String fileContent, JSchCommandLineExecutor executor) throws IOException, InterruptedException {
        createFileOnServerSide(sshServer, session, DummyCommand.WORKDIR_REMOTE + filename, fileContent, executor);
    }
    
    /**
     * Creates a file on server side.
     * @param sshServer {@link SshServer} to use
     * @param session {@link Session} to use
     * @param filepath absolute path to file
     * @param fileContent content of file to create
     * @param executor executor to use
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    public static void createFileOnServerSide(SshServer sshServer, Session session, final String filepath,
        final String fileContent, JSchCommandLineExecutor executor) throws IOException, InterruptedException {

        final String createFileCommand = "Create file!";

        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (command.contains(createFileCommand)) {
                    return new DummyCommand() {
                        
                        @Override
                        public void start(Environment env) throws IOException {
                            File file = new File(filepath);
                            file.createNewFile();
                            FileUtils.writeByteArrayToFile(file, fileContent.getBytes());
                            exitCallback.onExit(0);
                        }
                        
                    };                    
                } else  {
                    return new UnknownCommand(command);
                }
            }
        });

        executor.start(createFileCommand);
        executor.waitForTermination();
    }
    
    /**
     * Creates a dir on server side.
     * @param sshServer {@link SshServer} to use
     * @param session {@link Session} to use
     * @param dirname name of dir to create
     * @param executor executor to use
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    public static void createDirOnServerSidesWorkDir(SshServer sshServer, Session session, final String dirname,
        JSchCommandLineExecutor executor) throws IOException, InterruptedException {
        createDirOnServerSide(sshServer, session, DummyCommand.WORKDIR_REMOTE + dirname, executor);
    }

    /**
     * Creates a dir on server side.
     * @param sshServer {@link SshServer} to use
     * @param session {@link Session} to use
     * @param dirpath absolute path to dir to create
     * @param executor executor to use
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    public static void createDirOnServerSide(SshServer sshServer, Session session, final String dirpath,
        JSchCommandLineExecutor executor) throws IOException, InterruptedException {

        final String createFileCommand = "Create dir!";

        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (command.contains(createFileCommand)) {
                    return new DummyCommand() {
                        
                        @Override
                        public void start(Environment env) throws IOException {
                            File dir = new File(dirpath);
                            dir.mkdirs();
                            exitCallback.onExit(0);
                            dir.deleteOnExit();
                        }
                        
                    };                    
                } else  {
                    return new UnknownCommand(command);
                }
            }
        });

        executor.start(createFileCommand);
        executor.waitForTermination();
    }

    /**
     * Creates a file on server side.
     * @param sshServer {@link SshServer} to use
     * @param session {@link Session} to use
     * @param filename name of file to create
     * @param fileContent content of file to create
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    public static void createFileOnServerSide(SshServer sshServer, Session session, String filename, String fileContent)
        throws IOException, InterruptedException {
        createFileOnServerSidesWorkDir(sshServer, session, filename, fileContent,
            new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE));
        
    }
    
}
