/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jna.Platform;

import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Wrapper for running the {@link CommonExecutorTests} with a {@link LocalCommandLineExecutor}.
 * 
 * @author Robert Mischke
 * 
 */
public class LocalApacheCommandLineExecutorTest extends CommonExecutorTests {

    private static final String UNKOWN_PLATFORM = "Unkown platform. Currently only Windows and Linux are supported.";

    private static final int HUNDRED_SECONDS = 100;

    private LocalApacheCommandLineExecutor executor;

    private File workDir;

    /**
     * Static test setup.
     */
    @BeforeClass
    public static void classSetUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Setup.
     * 
     * @throws IOException on I/O errors
     */
    @Before
    public void setUp() throws IOException {
        workDir = TempFileServiceAccess.getInstance().createManagedTempDir("-unittest");
        executor = new LocalApacheCommandLineExecutor(workDir);
    }

    /**
     * Cleanup.
     * 
     * @throws IOException on I/O errors
     */
    @After
    public void tearDown() throws IOException {
        if (workDir != null) {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(workDir);
        }
    }

    /**
     * Tests {@link CommandLineExecutor#start(String)} with a single "echo" command.
     * 
     * @throws IOException on test exception
     * @throws InterruptedException on test exception
     */
    @Test
    public void testCrossPlatformEcho() throws IOException, InterruptedException {
        testCrossPlatformEcho(executor);
    }

    /**
     * Tests {@link CommandLineExecutor#startMultiLineCommand(String[])} with multiple "echo" commands.
     * 
     * @throws IOException on test exception
     * @throws InterruptedException on test exception
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testCrossPlatformMultiLineEcho() throws IOException, InterruptedException {
        testCrossPlatformMultiLineEcho(executor);
    }

    /**
     * Tests if environment variables are being properly set under Linux.
     * 
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testLinuxEnvironmentSetting() throws IOException, InterruptedException {
        if (!OSFamily.isLinux()) {
            log.info(MESSAGE_LINUX_SPECIFIC_TEST_SKIPPED);
            return;
        }
        testLinuxEnvironmentSetting(executor);
    }

    /**
     * Tests if a provided input stream properly reaches the command line under Linux.
     * 
     * @throws UnsupportedEncodingException if UTF-8 somehow ceased to exist
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testLinuxProvidedInputStream() throws UnsupportedEncodingException, IOException, InterruptedException {
        if (!OSFamily.isLinux()) {
            log.info(MESSAGE_LINUX_SPECIFIC_TEST_SKIPPED);
            return;
        }
        testLinuxProvidedInputStream(executor);
    }

    /**
     * Tests if environment variables are being properly set under Windows.
     * 
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testWindowsEnvironmentSetting() throws IOException, InterruptedException {
        if (!OSFamily.isWindows()) {
            log.info(MESSAGE_WINDOWS_SPECIFIC_TEST_SKIPPED);
            return;
        }
        testWindowsEnvironmentSetting(executor);
    }

    /**
     * Tests if a provided input stream properly reaches the command line under Windows.
     * 
     * @throws UnsupportedEncodingException if UTF-8 somehow ceased to exist
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testWindowsProvidedInputStream() throws UnsupportedEncodingException, IOException, InterruptedException {
        if (!OSFamily.isWindows()) {
            log.info(MESSAGE_WINDOWS_SPECIFIC_TEST_SKIPPED);
            return;
        }
        testWindowsProvidedInputStream(executor);
    }

    /**
     * Tests the correct cancellation prior to the start of the execution.
     * 
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test
    public void testCancellationOfUnstartedExecutor() throws IOException, InterruptedException {
        executor.cancel();
        String commandString;
        if (Platform.isWindows()) {
            commandString = "";
        } else if (Platform.isLinux()) {
            commandString = ":";
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM);
        }
        executor.start(commandString);

        int exitCode = executor.waitForTermination();
        assertEquals(1, exitCode); // indicates failure during execution
    }

    /**
     * Tests the correct cancellation after the start of the execution.
     * 
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test
    public void testCancellationOfStartedExecutor() throws IOException, InterruptedException {
        String commandString;
        if (Platform.isWindows()) {
            commandString = StringUtils.format(ProcessUtilsTests.WINDOWS_WAIT_COMMAND_TEMPLATE, HUNDRED_SECONDS);
        } else if (Platform.isLinux()) {
            commandString = StringUtils.format(ProcessUtilsTests.LINUX_WAIT_COMMAND_TEMPLATE, HUNDRED_SECONDS);
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM);
        }
        executor.start(commandString);
        Thread.sleep(ProcessUtilsTests.ONE_SECOND_AS_MILLIS);
        assertTrue(executor.cancel());
        int exitCode = executor.waitForTermination();
        assertThat(exitCode, not(0)); // indicates failure during execution        
    }

    /**
     * Tests the correct cancellation after the start of the execution.
     * 
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption
     */
    @Test
    public void testCancellationOfFinishedExecutor() throws IOException, InterruptedException {
        String commandString;
        if (Platform.isWindows()) {
            commandString = StringUtils.format(ProcessUtilsTests.WINDOWS_WAIT_COMMAND_TEMPLATE, 5);
        } else if (Platform.isLinux()) {
            commandString = StringUtils.format(ProcessUtilsTests.LINUX_WAIT_COMMAND_TEMPLATE, 5);
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM);
        }
        executor.start(commandString);
        Thread.sleep(ProcessUtilsTests.ONE_SECOND_AS_MILLIS);
        int exitCode = executor.waitForTermination();
        assertEquals(exitCode, 0); // indicates failure during execution
        assertTrue(executor.cancel());
    }
}
