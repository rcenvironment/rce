/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jna.Platform;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link ProcessUtils}.
 *
 * @author Tobias Rodehutskors
 */
public class ProcessUtilsTest {

    /**
     * This seems to be a common trick for windows to wait for some time. It pings localhost 100 times and waits between each ping one
     * second.
     */
    static final String WINDOWS_WAIT_COMMAND_TEMPLATE = "ping 127.0.0.1 -n %d > NUL";

//    static final String LINUX_WAIT_COMMAND = "sleep 100";
    
    static final String LINUX_WAIT_COMMAND_TEMPLATE = "sleep %d";

    static final long ONE_SECOND_AS_MILLIS = 1000;
    
    private static final String UNKOWN_PLATFORM = "Unkown platform. Currently only Windows and Linux are supported.";

    private static final int HUNDRED_SECONDS = 100;

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
        workDir = TempFileServiceAccess.getInstance().createManagedTempDir(
            "-unittest");
    }

    /**
     * Cleanup.
     * 
     * @throws IOException on I/O errors
     */
    @After
    public void tearDown() throws IOException {
        if (workDir != null) {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(
                workDir);
        }
    }

    /**
     * Starts a short running process, waits for its completion and tries to retrieve the PID of the finished process.
     * 
     * @throws ExecuteException e
     * @throws IOException e
     * @throws InterruptedException e
     * @throws NoSuchFieldException e
     * @throws SecurityException e
     * @throws IllegalArgumentException e
     * @throws IllegalAccessException e
     */
    @Test
    public void testGetPidOfFinishedProcess() throws ExecuteException, IOException, InterruptedException, NoSuchFieldException,
        SecurityException, IllegalArgumentException, IllegalAccessException {

        DefaultExecutor executor = new DefaultExecutor();
        ProcessExtractor processExtractor = new ProcessExtractor();
        executor.setWatchdog(processExtractor);

        CommandLine cl = null;

        if (Platform.isWindows()) {
            cl = ProcessUtils.constructCommandLine("");
        } else if (Platform.isLinux()) {
            cl = ProcessUtils.constructCommandLine(":");
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM);
        }

        int exitCode = executor.execute(cl);
        assertEquals(0, exitCode);

        Process process = processExtractor.getProcess();
        assertNotNull(process);
        assertFalse(isRunning(process));

        ProcessUtils.getPid(process);
    }

    /**
     * 
     * Starts a long running process, retrieves the process id of the started process and kills it afterwards.
     * 
     * @throws ExecuteException e
     * @throws IOException e
     * @throws InterruptedException e
     * @throws NoSuchFieldException e
     * @throws SecurityException e
     * @throws IllegalArgumentException e
     * @throws IllegalAccessException e
     */
    @Test
    public void testGetPidOfRunningProcessAndKillProcess() throws ExecuteException, IOException, InterruptedException,
        NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        DefaultExecutor executor = new DefaultExecutor();
        ProcessExtractor processExtractor = new ProcessExtractor();
        executor.setWatchdog(processExtractor);

        // start a long running process
        CommandLine cl;
        if (Platform.isWindows()) {
            cl = ProcessUtils.constructCommandLine(StringUtils.format(WINDOWS_WAIT_COMMAND_TEMPLATE, HUNDRED_SECONDS));
        } else if (Platform.isLinux()) {
            cl = ProcessUtils.constructCommandLine(StringUtils.format(ProcessUtilsTest.LINUX_WAIT_COMMAND_TEMPLATE, HUNDRED_SECONDS));
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM);
        }

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        executor.execute(cl, resultHandler);

        // wait for some time to make sure that the process has started
        Thread.sleep(ONE_SECOND_AS_MILLIS);

        // extract the process and its process id
        Process process = processExtractor.getProcess();
        assertNotNull(process);
        assertTrue(isRunning(process));
        int pid = ProcessUtils.getPid(process);
        assertTrue(isRunning(process));

        // kill the process tree
        assertTrue(ProcessUtils.killProcessTree(pid));

        // verify that the process has died
        resultHandler.waitFor(10 * ONE_SECOND_AS_MILLIS);
        if (!resultHandler.hasResult()) {
            fail("The process was not killed within 10 seconds");
        }

        int exitCode = resultHandler.getExitValue();
        int expectedExitCode;
        if (Platform.isWindows()) {
            expectedExitCode = ProcessUtils.WINDOWS_EXIT_CODE_SIGTERM;
        } else {
            expectedExitCode = ProcessUtils.LINUX_EXIT_CODE_SIGTERM;
        }

        assertEquals(expectedExitCode, exitCode);
        assertFalse(isRunning(process));
    }

    /**
     * Starts a process, retrieves the process id of the started process and waits for its termination. Afterwards it tries to kills it. No
     * exception should be thrown.
     * 
     * @throws ExecuteException e
     * @throws IOException e
     * @throws InterruptedException e
     * @throws NoSuchFieldException e
     * @throws SecurityException e
     * @throws IllegalArgumentException e
     * @throws IllegalAccessException e
     */
    @Test
    public void testGetPidOfRunningProcessAndKillProcessWhenFinished() throws ExecuteException, IOException, InterruptedException,
        NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        DefaultExecutor executor = new DefaultExecutor();
        ProcessExtractor processExtractor = new ProcessExtractor();
        executor.setWatchdog(processExtractor);

        // start a long running process
        CommandLine cl;
        if (Platform.isWindows()) {
            cl = ProcessUtils.constructCommandLine(StringUtils.format(WINDOWS_WAIT_COMMAND_TEMPLATE, 5));
        } else if (Platform.isLinux()) {
            cl = ProcessUtils.constructCommandLine(StringUtils.format(ProcessUtilsTest.LINUX_WAIT_COMMAND_TEMPLATE, 5));
        } else {
            throw new IllegalStateException(UNKOWN_PLATFORM);
        }

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        executor.execute(cl, resultHandler);

        // wait for some time to make sure that the process has started
        Thread.sleep(ONE_SECOND_AS_MILLIS);

        // extract the process and its process id
        Process process = processExtractor.getProcess();
        assertNotNull(process);
        assertTrue(isRunning(process));
        int pid = ProcessUtils.getPid(process);
        assertTrue(isRunning(process));

        // wait for the process to finish
        resultHandler.waitFor(10 * ONE_SECOND_AS_MILLIS);

        // verify that the process has finished
        if (!resultHandler.hasResult()) {
            fail("The process has not finished within 10 seconds");
        }

        int exitCode = resultHandler.getExitValue();
        int expectedExitCode;
        if (Platform.isWindows()) {
            expectedExitCode = ProcessUtils.WINDOWS_EXIT_CODE_SUCCESS;
        } else {
            expectedExitCode = ProcessUtils.LINUX_EXIT_CODE_SUCCESS;
        }

        assertEquals(expectedExitCode, exitCode);
        assertFalse(isRunning(process));

        // try to kill the process tree
        assertTrue(ProcessUtils.killProcessTree(pid));
    }

    /**
     * TODO JAVA8 As soon as the code base is converted to Java 8 this call should be replaced with {@link java.lang.Process#isAlive()}.
     */
    boolean isRunning(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (java.lang.IllegalThreadStateException e) {
            return true;
        }
    }
}
