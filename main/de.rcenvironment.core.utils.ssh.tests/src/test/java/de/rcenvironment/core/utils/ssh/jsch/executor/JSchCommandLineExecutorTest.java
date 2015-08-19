/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.DummyCommand;
import de.rcenvironment.core.utils.ssh.jsch.DummyPasswordAuthenticator;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.Utils;

/**
 * Test case for {@link JSchCommandLineExecutor}.
 * 
 * @author Doreen Seider
 */
public class JSchCommandLineExecutorTest {

    private static final String WRONG_COMMAND = "Wrong command: ";

    private static final String LOCALHOST = "localhost";
    
    private static final String FULL_COMMAND_TEMPLATE = "cd %s && %s";

    private SshServer sshServer;
    
    private int port;

    private File localWorkdir;

    private File remoteWorkdir;

    /**
     * Set up test environment. 
     * @throws IOException on error
     **/
    @SuppressWarnings("serial")
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        port = Utils.getRandomPortNumber();
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setUserAuthFactories(new ArrayList<NamedFactory<UserAuth>>() {{ add(new UserAuthPassword.Factory()); }});
        sshServer.setPasswordAuthenticator(new DummyPasswordAuthenticator());
        sshServer.start();
    }
    
    /**
     * Set up work dir.
     * 
     * @throws IOException on error
     **/
    @Before
    public void createWorkDir() throws IOException {
        remoteWorkdir = new File(DummyCommand.WORKDIR_REMOTE);
        remoteWorkdir.mkdir();
        localWorkdir = new File(DummyCommand.WORKDIR_LOCAL);
        localWorkdir.mkdir();
    }
    
    /**
     * Tear down test environment.
     * @throws InterruptedException 
     **/
    @After
    public void tearDown() throws InterruptedException {
        sshServer.stop();
    }
    
    /**
     *Delete work dir.
     * 
     * @throws IOException on error
     **/
    @After
    public void deleteWorkDir() throws IOException {
        FileUtils.deleteQuietly(localWorkdir);
        FileUtils.deleteQuietly(remoteWorkdir);
    }

    /**
     * Test correct stdout stream.
     *  
     * @throws SshParameterException on error
     * @throws JSchException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     **/
    @Test(timeout = Utils.TIMEOUT)
    public void testStdout() throws JSchException, SshParameterException, IOException, InterruptedException {
        
        final String out = "nice";
        final String commandStdout = "command - exit value: 0, stdout";
        
        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String commandString) {
                if (commandString.equals(String.format(FULL_COMMAND_TEMPLATE, DummyCommand.WORKDIR_REMOTE,  commandStdout))) {
                    return new DummyCommand(out, null, 0);
                } else {
                    throw new IllegalArgumentException(WRONG_COMMAND + commandString);
                }
            }
        });
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        executor.start(commandStdout);
        InputStream stdoutStream = executor.getStdout();
        InputStream stderrStream = executor.getStderr();
        int exitValue = executor.waitForTermination();
        assertEquals(out, IOUtils.toString(stdoutStream));
        assertEquals(DummyCommand.EMPTY_STRING, IOUtils.toString(stderrStream));
        assertEquals(0, exitValue);
    }

    /**
     * Test stderr and stdout stream in parallel.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testStdoutStderrResult() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String out = "nice";
        final String err = "not so nice";
        final String commandStdoutStderr = "command - exit value: 0, stdout, stderr";
        
        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String commandString) {
                if (commandString.equals(String.format(FULL_COMMAND_TEMPLATE, DummyCommand.WORKDIR_REMOTE,  commandStdoutStderr))) {
                    return new DummyCommand(out, err, 0);
                } else {
                    throw new IllegalArgumentException(WRONG_COMMAND + commandString);
                }
            }
        });
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        executor.start(commandStdoutStderr);
        InputStream stdoutStream = executor.getStdout();
        InputStream stderrStream = executor.getStderr();
        int exitValue = executor.waitForTermination();
        assertEquals(out, IOUtils.toString(stdoutStream));
        assertEquals(err, IOUtils.toString(stderrStream));
        assertEquals(0, exitValue);                
    }

    /**
     * Test correct stderr stream.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testStderrResult() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String err = "not so nice";
        final String commandStderr = "command - exit value: 1, stderr";
        
        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String commandString) {
                if (commandString.equals(String.format(FULL_COMMAND_TEMPLATE, DummyCommand.WORKDIR_REMOTE,  commandStderr))) {
                    return new DummyCommand(null, err, 1);
                } else {
                    throw new IllegalArgumentException(WRONG_COMMAND + commandString);
                }
            }
        });
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        executor.start(commandStderr);
        InputStream stdoutStream = executor.getStdout();
        InputStream stderrStream = executor.getStderr();
        int exitValue = executor.waitForTermination();
        assertEquals(DummyCommand.EMPTY_STRING, IOUtils.toString(stdoutStream));
        assertEquals(err, IOUtils.toString(stderrStream));
        assertEquals(1, exitValue);        
    }

    // TODO test with stdin - seid_do            

    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testGetRemoteWorkDir() throws JSchException, SshParameterException {
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        assertTrue(executor.getWorkDirPath().contains(DummyCommand.WORKDIR_REMOTE));
    }
    
    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testDownloadWorkdir() throws JSchException, SshParameterException, IOException, InterruptedException {
        
        final String fileContent = RandomStringUtils.randomAlphabetic(6);
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        Utils.createFileOnServerSidesWorkDir(sshServer, session, RandomStringUtils.randomAlphabetic(6), fileContent, executor);
        
        sshServer.setCommandFactory(new ScpCommandFactory());
        
        File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
        executor.downloadWorkdir(dir);
        assertEquals(1, dir.listFiles().length);

        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
    }
    
    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testUploadDownloadFileToFromWorkdir() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String targetFilename = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        sshServer.setCommandFactory(new ScpCommandFactory());
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        File srcFile = new File(localWorkdir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        
        executor.uploadFileToWorkdir(srcFile, srcFilename);
        FileUtils.deleteQuietly(srcFile);
        
        File targetFile = new File(localWorkdir, targetFilename);
        
        executor.downloadFileFromWorkdir(srcFilename, targetFile);
        assertEquals(fileContent, FileUtils.readFileToString(targetFile));
        
        FileUtils.deleteQuietly(targetFile);
    }
    
    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testUploadDownloadDirToFromWorkdir() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String srcDirname = RandomStringUtils.randomAlphabetic(5);
        final String targetDirname = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);

        sshServer.setCommandFactory(new ScpCommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (command.startsWith("mkdir")) {
                    new File(command.split(" ")[2]).mkdirs();
                    return new DummyCommand();
                } else {
                    return super.createCommand(command);                    
                }
            }
        });

        File srcDir = new File(DummyCommand.WORKDIR_LOCAL, srcDirname);
        srcDir.mkdirs();
        File srcFile = new File(srcDir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        assertEquals(1, srcDir.listFiles().length);
        assertEquals(srcFilename, srcDir.listFiles()[0].getName());
        
        executor.uploadDirectoryToWorkdir(srcDir, targetDirname);
        FileUtils.deleteQuietly(srcDir);
        
        File targetDir = new File(DummyCommand.WORKDIR_LOCAL, RandomStringUtils.randomAlphabetic(5));
        targetDir.mkdirs();

        executor.downloadDirectoryFromWorkdir(targetDirname + "/" + srcDir.getName(), targetDir);
        assertEquals(1, targetDir.listFiles().length);
        assertEquals(srcFilename, targetDir.listFiles()[0].listFiles()[0].getName());
        assertEquals(fileContent, FileUtils.readFileToString(targetDir.listFiles()[0].listFiles()[0]));
        FileUtils.deleteQuietly(targetDir);
    }
    
    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testUploadDownloadFile() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String targetFilename = RandomStringUtils.randomAlphabetic(5);

        final String remotePath = RandomStringUtils.randomAlphabetic(5);
        
        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        sshServer.setCommandFactory(new ScpCommandFactory());

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
         
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        File srcFile = new File(localWorkdir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        executor.uploadFile(srcFile, DummyCommand.WORKDIR_REMOTE + remotePath);
        FileUtils.deleteQuietly(srcFile);

        File localTargetFile = new File(localWorkdir, targetFilename);
        executor.downloadFile(DummyCommand.WORKDIR_REMOTE + remotePath, localTargetFile);
        assertEquals(fileContent, FileUtils.readFileToString(localTargetFile));
        FileUtils.deleteQuietly(localTargetFile);
    }
    
    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = Utils.TIMEOUT)
    public void testUploadDownloadDirectory() throws JSchException, SshParameterException, IOException, InterruptedException {
        
        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String srcDirname = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);
        
        final String remoteDirpath = RandomStringUtils.randomAlphabetic(5);
        
        sshServer.setCommandFactory(new ScpCommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (command.startsWith("mkdir")) {
                    new File(command.split(" ")[2]).mkdirs();
                    return new DummyCommand();
                } else {
                    return super.createCommand(command);                    
                }
            }
        });
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
         
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        
        File srcDir = new File(DummyCommand.WORKDIR_LOCAL, srcDirname);
        srcDir.mkdirs();
        File srcFile = new File(srcDir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        assertEquals(1, srcDir.listFiles().length);
        assertEquals(srcFilename, srcDir.listFiles()[0].getName());
        
        executor.uploadDirectory(srcDir, DummyCommand.WORKDIR_REMOTE + remoteDirpath);
        FileUtils.deleteQuietly(srcDir);

        File targetDir = new File(DummyCommand.WORKDIR_LOCAL, RandomStringUtils.randomAlphabetic(5));
        targetDir.mkdirs();

        executor.downloadDirectory(DummyCommand.WORKDIR_REMOTE + remoteDirpath + "/" + srcDir.getName(), targetDir);
        assertEquals(1, targetDir.listFiles().length);
        assertEquals(srcFilename, targetDir.listFiles()[0].listFiles()[0].getName());
        assertEquals(fileContent, FileUtils.readFileToString(targetDir.listFiles()[0].listFiles()[0]));
        FileUtils.deleteQuietly(targetDir);
    }
    
    /**
     * Test.
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    // TODO combine test: upload, remote copy, download - seid_do
    @Test(timeout = Utils.TIMEOUT)
    public void testRemoteCopy() throws JSchException, SshParameterException, IOException, InterruptedException {
        String src = "src";
        String target = "target";
        
        final String cpCommand = "cp " + src + " " + target;
        final String failingCpCommand = "cp " + target + " " + src;
        
        sshServer.setCommandFactory(Utils.createDummyCommandFactory(cpCommand, "stdout", failingCpCommand, "stderr"));
        
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);
        executor.remoteCopy(src, target);
        try {
            executor.remoteCopy(target, src);
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }
    }
    
}
