/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor.testutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * A wrapper/utility class to simplify execution of external commands in integration tests.
 * 
 * @author Robert Mischke
 */
public class IntegrationTestExecutorUtils {

    /**
     * Holder for execution results, ie the exit code and stdout/stderr.
     * 
     * @author Robert Mischke
     */
    public static class ExecutionResult {

        // Note: fields are left public for simplicity; encapsulate when necessary

        /**
         * The invoked process' exit code.
         */
        public final int exitCode;

        /**
         * The raw captured stdout output.
         */
        public final String stdout;

        /**
         * The stdout output, parsed into single lines for convenience.
         */
        public final List<String> stdoutLines;

        /**
         * The raw captured stderr output.
         */
        public final String stderr;

        /**
         * The stderr output, parsed into single lines for convenience.
         */
        public final List<String> stderrLines;

        public ExecutionResult(int exitCode, String stdout, String stderr) throws IOException {
            super();
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.stdoutLines = parseIntoLines(stdout);
            this.stderrLines = parseIntoLines(stderr);
        }

        private List<String> parseIntoLines(String rawOutput) throws IOException {
            List<String> container = new LinkedList<>();
            BufferedReader reader = new BufferedReader(new StringReader(rawOutput));
            String line;
            while ((line = reader.readLine()) != null) {
                container.add(line);
            }
            return Collections.unmodifiableList(container);
        }
    }

    private final File workingDir;

    public IntegrationTestExecutorUtils(File workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * Invokes the given command line and waits for the external process to complete.
     * 
     * @param command the command line to execute
     * @return a {@link ExecutionResult} holder with exit code and stdout/stderr
     * @throws IOException on I/O errors
     * @throws InterruptedException on thread interruption (e.g. by test timeout)
     */
    public ExecutionResult executeAndWait(String command) throws IOException, InterruptedException {
        LocalApacheCommandLineExecutor executor = new LocalApacheCommandLineExecutor(workingDir);
        executor.start(command);
        CapturingTextOutReceiver stdoutCapture = new CapturingTextOutReceiver("");
        CapturingTextOutReceiver stderrCapture = new CapturingTextOutReceiver("");
        final TextStreamWatcher stdoutWatcher = new TextStreamWatcher(executor.getStdout(), stdoutCapture).start();
        final TextStreamWatcher stderrWatcher = new TextStreamWatcher(executor.getStderr(), stderrCapture).start();
        int exitCode = executor.waitForTermination();
        stdoutWatcher.waitForTermination();
        stderrWatcher.waitForTermination();
        return new ExecutionResult(exitCode, stdoutCapture.getBufferedOutput(), stderrCapture.getBufferedOutput());
    }
}
