/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.fileinfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchCommandLineExecutor;

/**
 * Test cases for {@link UnixFileServiceTest}.
 * 
 * Note: Deactivate tests because they won't run as long no test server is available (which is configured in
 * src/test/resources/unixFileService.test.properties.template). Use embedded test server.
 * 
 * @author Christian Weiss
 */
public class UnixFileServiceTest {

    /** If test is set up as interactive one. */
    public static final boolean INTERACTIVE = System.getProperty("test.interactive") != null
        && System.getProperty("test.interactive").equals("true");

    private static final String HOME = "~/";

    private static final String CURRENT = ".";

    private static Log log = LogFactory.getLog(UnixFileServiceTest.class);

    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("^([^\\:]+)(?:\\:(\\d+))?$");

    private static final Pattern USERNAME_PASSWORD_PATTERN = Pattern.compile("^([^\\:]+)(?:\\:(.+))?$");

    private static final Logger CONNECTION_LOGGER = new Logger() {

        @Override
        public void log(int arg0, String arg1) {
            if (arg0 > 1) {
                log.info("Jsch log: " + arg0 + ": " + arg1);
            }
        }

        @Override
        public boolean isEnabled(int arg0) {
            return true;
        }
    };

    private static Session session;

    private static Session initTestSession() {
        final Properties testUtil;
        try {
            testUtil = loadSettings("unixFileService.test.properties");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String host = testUtil.getProperty("test.destinationHost");
        String user = testUtil.getProperty("test.sshAuthUser");
        String keyfile = testUtil.getProperty("test.sshKeyFileLocation");
        String passphrase = testUtil.getProperty("test.sshAuthPhrase");
        try {
            final Session s = JschSessionFactory.setupSession(host, 22, user,
                keyfile, passphrase, CONNECTION_LOGGER);
            return s;
        } catch (JSchException e) {
            throw new RuntimeException(e);
        } catch (SshParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private static Session initInteractiveSession() {
        final JSchConnectionData connectionData = readConnectionDataFromConsole();
        try {
            final JSch jSch = new JSch();
            final Session s = jSch.getSession(connectionData.username, connectionData.host, connectionData.port);
            s.setPassword(connectionData.password);
            s.setConfig("StrictHostKeyChecking", "no");
            s.connect();
            return s;
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set up.
     * 
     * @throws JSchException on initializing errors.
     **/
    // @BeforeClass
    public static void setupSessionData() throws JSchException {
        if (INTERACTIVE) {
            session = initInteractiveSession();
        } else {
            session = initTestSession();
        }
    }

    private static JSchConnectionData readConnectionDataFromConsole() {
        final JSchConnectionData result = new JSchConnectionData();
        try (final Scanner in = new Scanner(System.in)) {
            String line;
            Matcher matcher;
            do {
                log.debug("host[:port] >> ");
                line = in.nextLine();
                matcher = HOST_PORT_PATTERN.matcher(line);
            } while (!matcher.matches());
            result.host = matcher.group(1);
            if (matcher.group(2) != null) {
                result.port = Integer.parseInt(matcher.group(2));
            }
            do {
                log.debug("username[:password] >> ");
                line = in.nextLine();
                matcher = USERNAME_PASSWORD_PATTERN.matcher(line);
            } while (!matcher.matches());
            result.username = matcher.group(1);
            if (matcher.group(2) != null) {
                result.password = matcher.group(2);
            }
        }
        return result;
    }

    /**
     * 
     * Encapsulates data for SSH connection.
     * 
     * @author Christian Weiss
     */
    private static class JSchConnectionData {

        private static final int DEFAULT_PORT = 22;

        private String host;

        private int port = DEFAULT_PORT;

        private String username;

        private String password = "";
    }

    /**
     * Test.
     * 
     * @throws IOException on I/O errors.
     **/
    @Ignore
    @Test
    public void testIsDirectory() throws IOException {
        final CommandLineExecutor executor = new JSchCommandLineExecutor(session, CURRENT);
        final UnixFileInfoService service = new UnixFileInfoService(executor);
        Assert.assertTrue(service.isDirectory("/home"));
        Assert.assertTrue(service.isDirectory(HOME));
        Assert.assertFalse(service.isDirectory("/tmp/nonsense_dir/absolute/fake"));
    }

    /**
     * Test.
     * 
     * @throws IOException on I/O errors.
     **/
    @Ignore
    @Test
    public void testListFiles() throws IOException {
        final CommandLineExecutor executor = new JSchCommandLineExecutor(session, CURRENT);
        final UnixFileInfoService service = new UnixFileInfoService(executor);
        final Collection<FileInfo> files = service.listFiles("~/.rce", true);
        Assert.assertTrue(files.size() > 0);
        for (final FileInfo file : files) {
            Assert.assertFalse(file.isDirectory());
            final String relativePath = file.getAbsolutePath();
            Assert.assertNotNull(relativePath);
            Assert.assertFalse(relativePath.isEmpty());
            final String name = file.getName();
            Assert.assertNotNull(name);
            Assert.assertFalse(name.isEmpty());
            Assert.assertTrue(relativePath.endsWith(name));
            Assert.assertNotNull(file.getModificationDate());
            log.debug("Found file: " + file.getAbsolutePath() + " (size: " + file.getSize() + " bytes)");
        }
    }

    /**
     * Test.
     * 
     * @throws IOException on I/O errors.
     **/
    @Ignore
    @Test
    public void testListContent() throws IOException {
        final CommandLineExecutor executor = new JSchCommandLineExecutor(session, CURRENT);
        final UnixFileInfoService service = new UnixFileInfoService(executor);
        final Collection<FileInfo> files = service.listContent(HOME, true);
        Assert.assertTrue(files.size() > 0);
        for (final FileInfo file : files) {
            final String relativePath = file.getAbsolutePath();
            Assert.assertNotNull(relativePath);
            Assert.assertFalse(relativePath.isEmpty());
            final String name = file.getName();
            Assert.assertNotNull(name);
            Assert.assertFalse(name.isEmpty());
            Assert.assertTrue(relativePath.endsWith(name));
            Assert.assertNotNull(file.getModificationDate());
        }
    }

    /**
     * Test.
     * 
     * @throws IOException on I/O errors.
     **/
    @Ignore
    @Test
    public void testSize() throws IOException {
        final CommandLineExecutor executor = new JSchCommandLineExecutor(session, CURRENT);
        final UnixFileInfoService service = new UnixFileInfoService(executor);
        Assert.assertEquals(new Long(0L), service.size(HOME));
        Assert.assertTrue(service.size("/etc/hosts") > 0L);
    }

    /**
     * Test.
     * 
     * @throws IOException on I/O errors.
     **/
    @Ignore
    @Test
    public void testSizeFailure() throws IOException {
        final CommandLineExecutor executor = new JSchCommandLineExecutor(session, CURRENT);
        final UnixFileInfoService service = new UnixFileInfoService(executor);
        Assert.assertNull(service.size("/root/hosts"));
    }

    private static Properties loadSettings(String filename) throws IOException {

        Properties testSettings = new Properties();
        InputStream stream = UnixFileInfoService.class.getResourceAsStream("/" + filename);
        if (stream == null) {
            throw new IOException("Test configuration file '" + filename + "' not found");
        }
        try {
            testSettings.load(stream);
        } finally {
            stream.close();
        }
        return testSettings;
    }
}
