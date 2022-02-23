/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.ssh.jsch;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;
import de.rcenvironment.core.utils.testing.ParameterizedTestUtils;
import de.rcenvironment.core.utils.testing.TestParametersProvider;

/**
 * Manual test case.
 * 
 * @author Brigitte Boden
 */
public class JSchFileTransferTest {

    private static final String WORKDIR_LOCAL = "local-workdir/";

    private String ip;

    private int port;

    private String username;

    private String password;

    private String remoteWorkdir;

    private File localWorkdir;

    private TestParametersProvider testParameters;

    private Log log = LogFactory.getLog(getClass());

    private Session session;

    /**
     * Set up test environment.
     * 
     * @throws IOException on error
     * @throws SshParameterException on error
     * @throws JSchException on error
     * @throws InterruptedException on error
     **/
    @Before
    public void setUp() throws IOException, JSchException, SshParameterException, InterruptedException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        testParameters = new ParameterizedTestUtils().readDefaultPropertiesFile(getClass());
        ip = testParameters.getNonEmptyString("ip");
        port = testParameters.getExistingInteger("port");
        username = testParameters.getNonEmptyString("username");
        password = testParameters.getNonEmptyString("passphrase");

        session = JschSessionFactory.setupSession(ip, port, username,
            null, password, null);

        // Initialize scp context
        JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
        String sessionToken;

        rceExecutor.start("ra init --compact");
        try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
            rceExecutor.waitForTermination();
            sessionToken = IOUtils.toString(stdoutStream).trim();
        }

        remoteWorkdir = StringUtils.format("/ra/%s/input", sessionToken);
    }

    /**
     * Set up work dir.
     * 
     * @throws IOException on error
     **/
    @Before
    public void createWorkDir() throws IOException {
        localWorkdir = new File(WORKDIR_LOCAL);
        localWorkdir.mkdir();
        log.info("Temp file dir: " + localWorkdir.getAbsolutePath());
    }

    /**
     * Delete work dir.
     * 
     * @throws IOException on error
     **/
    @After
    public void deleteWorkDir() throws IOException {
        FileUtils.deleteQuietly(localWorkdir);
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test
    public void testUploadDownloadFile() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5);
        final String targetFilename = RandomStringUtils.randomAlphabetic(5);

        final String remotePath = RandomStringUtils.randomAlphabetic(5);

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        File srcFile = new File(localWorkdir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        JschFileTransfer.uploadFile(session, srcFile, remoteWorkdir + remotePath);
        FileUtils.deleteQuietly(srcFile);

        File localTargetFile = new File(localWorkdir, targetFilename);
        JschFileTransfer.downloadFile(session, remoteWorkdir + remotePath, localTargetFile);
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
    @Test
    public void testUploadDownloadDirectory() throws JSchException, SshParameterException, IOException, InterruptedException {

        final String srcFilename = RandomStringUtils.randomAlphabetic(5) + ".txt";
        final String srcDirname = "somedir";

        final String fileContent = RandomStringUtils.randomAlphabetic(9);

        File srcDir = new File(localWorkdir, srcDirname);
        srcDir.mkdirs();

        File srcFile = new File(srcDir, srcFilename);
        FileUtils.write(srcFile, fileContent);
        assertEquals(1, srcDir.listFiles().length);
        assertEquals(srcFilename, srcDir.listFiles()[0].getName());

        JschFileTransfer.uploadDirectoryToRCEInstance(session, srcDir, remoteWorkdir + srcDirname);
        FileUtils.deleteQuietly(srcDir);

        File localTargetDir = new File(localWorkdir, RandomStringUtils.randomAlphabetic(5));
        localTargetDir.mkdirs();

        JschFileTransfer.downloadDirectory(session, remoteWorkdir + srcDirname, localTargetDir);
        assertEquals(1, localTargetDir.listFiles().length);
        assertEquals(1, localTargetDir.listFiles()[0].listFiles().length);
        assertEquals(srcFilename, localTargetDir.listFiles()[0].listFiles()[0].getName());
        assertEquals(fileContent, FileUtils.readFileToString(localTargetDir.listFiles()[0].listFiles()[0]));
        FileUtils.deleteQuietly(localTargetDir);
    }

}
