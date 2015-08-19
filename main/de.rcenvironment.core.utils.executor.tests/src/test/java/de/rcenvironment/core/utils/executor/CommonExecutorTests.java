/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;

import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;

/**
 * Common test setups for all {@link CommandLineExecutor} implementations to ensure consistency and
 * avoid code duplication. For each implementation, a subclass should provide top-level JUnit tests
 * that call each method of this class with a configured {@link CommandLineExecutor}.
 * 
 * @author Robert Mischke
 * 
 */
abstract class CommonExecutorTests {

    protected static final String MESSAGE_UNEXPECTED_STDOUT = "Unexpected StdOut: ";

    protected static final String MESSAGE_UNEXPECTED_STDERR = "Unexpected StdErr: ";

    protected static final String MESSAGE_LINUX_SPECIFIC_TEST_SKIPPED = "Test is only runnable on Linux; skipping";

    protected static final String MESSAGE_WINDOWS_SPECIFIC_TEST_SKIPPED = "Test is only runnable on Windows; skipping";

    protected static final int DEFAULT_TEST_TIMEOUT = 10000;

    /**
     * A simple holder for StdOut/StdErr strings.
     * 
     */
    public final class OutputHolder {

        private String stdout;

        private String stderr;

        public OutputHolder(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

    }

    protected final Log log = LogFactory.getLog(getClass());

    // FIXME new tests not cross-checked on Linux yet!

    protected void testCrossPlatformEcho(CommandLineExecutor executor) throws IOException, InterruptedException {
        executor.start("echo expected test output");

        OutputHolder output = waitAndCaptureOutput(executor);

        // check StdErr first as it may contain helpful information on failure
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDERR, "", output.getStderr());
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDOUT, "expected test output\n", output.getStdout());
    }

    protected void testCrossPlatformMultiLineEcho(CommandLineExecutor executor) throws IOException, InterruptedException {
        executor.startMultiLineCommand(new String[] { "echo expected line1", "echo expected line2" });

        OutputHolder output = waitAndCaptureOutput(executor);

        // check StdErr first as it may contain helpful information on failure
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDERR, "", output.getStderr());
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDOUT, "expected line1\n" + "expected line2\n", output.getStdout());
    }

    protected void testLinuxEnvironmentSetting(CommandLineExecutor executor) throws IOException, InterruptedException {
        executor.setEnv("TEST1", "val1");
        executor.start("echo key1=$TEST1, key2=$UNDEF");

        OutputHolder output = waitAndCaptureOutput(executor);

        // check StdErr first as it may contain helpful information on failure
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDERR, "", output.getStderr());
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDOUT, "key1=val1, key2=\n", output.getStdout());
    }

    protected void testLinuxProvidedInputStream(CommandLineExecutor executor) throws UnsupportedEncodingException, IOException,
        InterruptedException {
        // data to be sent to STDIN
        final String testInputData = "test input";
       
        ByteArrayInputStream stdinStream = new ByteArrayInputStream(testInputData.getBytes("UTF-8"));
        executor.start("read in; echo received: $in", stdinStream);
        OutputHolder output = waitAndCaptureOutput(executor);

        // STDERR should be empty, and the test data should have been piped to STDOUT with a prefix
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDERR, "", output.getStderr());
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDOUT, "received: " + testInputData + "\n", output.getStdout());
    }

    protected void testWindowsEnvironmentSetting(CommandLineExecutor executor) throws IOException, InterruptedException {
        executor.setEnv("TEST1", "val1");
        executor.start("echo key1=%TEST1%, key2=");

        OutputHolder output = waitAndCaptureOutput(executor);
        // check StdErr first as it may contain helpful information on failure
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDERR, "", output.getStderr());
        Assert.assertEquals(MESSAGE_UNEXPECTED_STDOUT, "key1=val1, key2=\n", output.getStdout());
    }

    protected void testWindowsProvidedInputStream(CommandLineExecutor executor) throws UnsupportedEncodingException, IOException,
        InterruptedException {
        // data to be sent to STDIN
//        final String testInputData = "test input\n";
//
//        ByteArrayInputStream stdinStream = new ByteArrayInputStream(testInputData.getBytes("UTF-8"));
//        executor.start("SET /P tmpVar= && echo %tmpVar%", stdinStream);
//        executor.start("echo %tmpVar%");
//        
//        OutputHolder output = waitAndCaptureOutput(executor);
//
        // STDERR should be empty, and the test data should have been piped to STDOUT with a prefix
//        Assert.assertEquals(MESSAGE_UNEXPECTED_STDERR, "", output.getStderr());
//        Assert.assertEquals(MESSAGE_UNEXPECTED_STDOUT, "received: " + testInputData + "\n", output.getStdout());
    }

    
    /**
     * Common utility method.
     */
    protected void waitAndGatherOutput(CommandLineExecutor executor, final CapturingTextOutReceiver outReceiver,
        final CapturingTextOutReceiver errReceiver) throws IOException, InterruptedException {
        final TextStreamWatcher stdoutWatcher = new TextStreamWatcher(executor.getStdout(), outReceiver).start();
        final TextStreamWatcher stderrWatcher = new TextStreamWatcher(executor.getStderr(), errReceiver).start();
        executor.waitForTermination();
        stdoutWatcher.waitForTermination();
        stderrWatcher.waitForTermination();
    }

    /**
     * Common utility method.
     */
    protected OutputHolder waitAndCaptureOutput(CommandLineExecutor executor) throws IOException, InterruptedException {
        // execute
        final CapturingTextOutReceiver outReceiver = new CapturingTextOutReceiver("");
        final CapturingTextOutReceiver errReceiver = new CapturingTextOutReceiver("");
        waitAndGatherOutput(executor, outReceiver, errReceiver);
        // wrap & return output
        return new OutputHolder(outReceiver.getBufferedOutput(), errReceiver.getBufferedOutput());
    }
}
