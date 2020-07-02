/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.DummyCommand;
import de.rcenvironment.core.utils.ssh.jsch.DummyPasswordAuthenticator;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.SshTestUtils;

/**
 * Test case for {@link JSchCommandLineExecutor}.
 * 
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class JSchCommandLineExecutorTest {

    private static final String WRONG_COMMAND = "Wrong command: ";

    private static final String LOCALHOST = "localhost";

    private static final String FULL_COMMAND_TEMPLATE = "cd %s && %s";

    private SshServer sshServer;

    private int port;

    private File localWorkdir;

    private File remoteWorkdir;

    private TempFileService tempFileService = TempFileServiceAccess.getInstance();

    /**
     * Initial set up of test environment.
     * 
     * @throws IOException on unexpected error
     */
    @BeforeClass
    public static void initialSetUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Set up test environment.
     * 
     * @throws IOException on unexpected error
     **/
    @SuppressWarnings("serial")
    @Before
    public void setUp() throws IOException {
        port = SshTestUtils.getRandomPortNumber();
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setUserAuthFactories(new ArrayList<UserAuthFactory>() {

            {
                add(new UserAuthPasswordFactory());
            }
        });
        sshServer.setPasswordAuthenticator(new DummyPasswordAuthenticator());
        sshServer.start();

        remoteWorkdir = tempFileService.createManagedTempDir();
        localWorkdir = tempFileService.createManagedTempDir();
    }

    /**
     * Tear down test environment.
     * 
     * @throws InterruptedException on error when stopping the server
     * @throws IOException on unexpected error
     **/
    @After
    public void tearDown() throws InterruptedException, IOException {
        sshServer.stop();
        tempFileService.disposeManagedTempDirOrFile(remoteWorkdir);
        tempFileService.disposeManagedTempDirOrFile(localWorkdir);
    }

    /**
     * Test correct stdout stream.
     * 
     * @throws SshParameterException on error
     * @throws JSchException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     **/
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testStdout() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String out = "console standard output";
        final String commandStdout = "command - exit value: 0, stdout";

        sshServer.setCommandFactory(new CommandFactory() {

            @Override
            public Command createCommand(ChannelSession session, String commandString) {
                if (commandString.equals(StringUtils.format(FULL_COMMAND_TEMPLATE, remoteWorkdir.getAbsolutePath(), commandStdout))) {
                    return new DummyCommand(out, null, 0);
                } else {
                    throw new IllegalArgumentException(WRONG_COMMAND + commandString);
                }
            }
        });

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        executor.start(commandStdout);
        try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {
            int exitValue = executor.waitForTermination();
            assertEquals(out, IOUtils.toString(stdoutStream));
            assertEquals(DummyCommand.EMPTY_STRING, IOUtils.toString(stderrStream));
            assertEquals(0, exitValue);
        }
    }

    /**
     * Test stderr and stdout stream in parallel.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testStdoutStderrResult() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String out = "console standard output";
        final String err = "console standard error";
        final String commandStdoutStderr = "command - exit value: 0, stdout, stderr";

        sshServer.setCommandFactory(new CommandFactory() {

            @Override
            public Command createCommand(ChannelSession session, String commandString) {
                if (commandString.equals(StringUtils.format(FULL_COMMAND_TEMPLATE, remoteWorkdir.getAbsolutePath(), commandStdoutStderr))) {
                    return new DummyCommand(out, err, 0);
                } else {
                    throw new IllegalArgumentException(WRONG_COMMAND + commandString);
                }
            }
        });

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        executor.start(commandStdoutStderr);
        try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {
            int exitValue = executor.waitForTermination();
            assertEquals(0, exitValue);
            assertEquals(out, IOUtils.toString(stdoutStream));
            assertEquals(err, IOUtils.toString(stderrStream));
        }
    }

    /**
     * Test correct stderr stream.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testStderrResult() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String err = "console standard error";
        final String commandStderr = "command - exit value: 1, stderr";

        sshServer.setCommandFactory(new CommandFactory() {

            @Override
            public Command createCommand(ChannelSession session, String commandString) {
                if (commandString.equals(StringUtils.format(FULL_COMMAND_TEMPLATE, remoteWorkdir.getAbsolutePath(), commandStderr))) {
                    return new DummyCommand(null, err, 1);
                } else {
                    throw new IllegalArgumentException(WRONG_COMMAND + commandString);
                }
            }
        });

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        executor.start(commandStderr);
        try (InputStream stdoutStream = executor.getStdout(); InputStream stderrStream = executor.getStderr();) {
            int exitValue = executor.waitForTermination();
            assertEquals(DummyCommand.EMPTY_STRING, IOUtils.toString(stdoutStream));
            assertEquals(err, IOUtils.toString(stderrStream));
            assertEquals(1, exitValue);
        }
    }

    // TODO test with stdin - seid_do

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testGetRemoteWorkDir() throws JSchException, SshParameterException {
        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());
        assertTrue(executor.getWorkDirPath().contains(remoteWorkdir.getAbsolutePath()));
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testDownloadWorkdir() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String fileContent = RandomStringUtils.randomAlphabetic(6);

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        SshTestUtils.createFileOnServerSide(sshServer, session, remoteWorkdir.getAbsolutePath() + RandomStringUtils.randomAlphabetic(6),
            fileContent, executor);

        sshServer.setCommandFactory(new ScpCommandFactory());

        File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
        executor.downloadWorkdir(dir);
        assertEquals(1, dir.listFiles().length);

        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testUploadDownloadFileToFromWorkdir() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String targetFilename = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        sshServer.setCommandFactory(new ScpCommandFactory());

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);

        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

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
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testUploadDownloadDirToFromWorkdir() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String srcDirname = RandomStringUtils.randomAlphabetic(5);
        final String targetDirname = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        sshServer.setCommandFactory(new ScpCommandFactory() {

            @Override
            public Command createCommand(ChannelSession session, String command) throws IOException {
                if (command.startsWith("mkdir")) {
                    new File(command.split(" ")[2]).mkdirs();
                    return new DummyCommand();
                } else {
                    return super.createCommand(session, command);
                }
            }
        });

        File srcDir = new File(localWorkdir.getAbsolutePath(), srcDirname);
        srcDir.mkdirs();
        File srcFile = new File(srcDir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        assertEquals(1, srcDir.listFiles().length);
        assertEquals(srcFilename, srcDir.listFiles()[0].getName());

        executor.uploadDirectoryToWorkdir(srcDir, targetDirname);
        FileUtils.deleteQuietly(srcDir);

        File targetDir = new File(localWorkdir.getAbsolutePath(), RandomStringUtils.randomAlphabetic(5));
        targetDir.mkdirs();

        executor.downloadDirectoryFromWorkdir(targetDirname + "/" + srcDir.getName(), targetDir);
        assertEquals(1, targetDir.listFiles().length);
        assertEquals(srcFilename, targetDir.listFiles()[0].listFiles()[0].getName());
        assertEquals(fileContent, FileUtils.readFileToString(targetDir.listFiles()[0].listFiles()[0]));
        FileUtils.deleteQuietly(targetDir);
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testUploadDownloadFile() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String targetFilename = RandomStringUtils.randomAlphabetic(5);

        final String remotePath = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        sshServer.setCommandFactory(new ScpCommandFactory());

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);

        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        File srcFile = new File(localWorkdir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        executor.uploadFile(srcFile, remoteWorkdir.getAbsolutePath() + remotePath);
        FileUtils.deleteQuietly(srcFile);

        File localTargetFile = new File(localWorkdir, targetFilename);
        executor.downloadFile(remoteWorkdir.getAbsolutePath() + remotePath, localTargetFile);
        assertEquals(fileContent, FileUtils.readFileToString(localTargetFile));
        FileUtils.deleteQuietly(localTargetFile);
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testUploadDownloadDirectory() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String srcDirname = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        final String remoteDirpath = RandomStringUtils.randomAlphabetic(5);

        sshServer.setCommandFactory(new ScpCommandFactory() {

            @Override
            public Command createCommand(ChannelSession session, String command) throws IOException {
                if (command.startsWith("mkdir")) {
                    new File(command.split(" ")[2]).mkdirs();
                    return new DummyCommand();
                } else {
                    return super.createCommand(session, command);
                }
            }
        });

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);

        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

        File srcDir = new File(remoteWorkdir.getAbsolutePath(), srcDirname);
        srcDir.mkdirs();
        File srcFile = new File(srcDir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        assertEquals(1, srcDir.listFiles().length);
        assertEquals(srcFilename, srcDir.listFiles()[0].getName());

        executor.uploadDirectory(srcDir, remoteWorkdir.getAbsolutePath() + remoteDirpath);
        FileUtils.deleteQuietly(srcDir);

        File targetDir = new File(localWorkdir.getAbsolutePath(), RandomStringUtils.randomAlphabetic(5));
        targetDir.mkdirs();

        executor.downloadDirectory(remoteWorkdir.getAbsolutePath() + remoteDirpath + "/" + srcDir.getName(), targetDir);
        assertEquals(1, targetDir.listFiles().length);
        assertEquals(srcFilename, targetDir.listFiles()[0].listFiles()[0].getName());
        assertEquals(fileContent, FileUtils.readFileToString(targetDir.listFiles()[0].listFiles()[0]));
        FileUtils.deleteQuietly(targetDir);
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    // TODO combine test: upload, remote copy, download - seid_do
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testRemoteCopy() throws JSchException, SshParameterException, IOException, InterruptedException {
        String src = "src";
        String target = "target";

        final String cpCommand = "cp " + src + " " + target;
        final String failingCpCommand = "cp " + target + " " + src;

        sshServer.setCommandFactory(SshTestUtils.createDummyCommandFactory(cpCommand, "stdout", failingCpCommand, "stderr"));

        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            null, DummyPasswordAuthenticator.PASSWORD, null);
        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());
        executor.remoteCopy(src, target);
        try {
            executor.remoteCopy(target, src);
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }
    }

}
