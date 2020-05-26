/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor.testutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
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
     * Defines a boolean filter for individual output lines.
     * 
     * @author Robert Mischke
     */
    public interface LineFilter {

        /**
         * @param line a text line, typically from some sort of execution
         * @return true if this line should be kept for subsequent processing
         */
        boolean accept(String line);
    }

    /**
     * Holder for execution results, ie the exit code and stdout/stderr.
     * 
     * @author Robert Mischke
     */
    public static class ExecutionResult {

        private static final int INITIAL_STRING_CONTAINER_SIZE = 64;

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
         * The stdout output, parsed into single lines and filtered for convenience; if no {@link LineFilter} is provided to the
         * constructor, it points to the unfiltered list.
         */
        public final List<String> filteredStdoutLines;

        /**
         * The raw captured stderr output.
         */
        public final String stderr;

        /**
         * The stderr output, parsed into single lines for convenience.
         */
        public final List<String> stderrLines;

        /**
         * The stderr output, parsed into single lines and filtered for convenience; if no {@link LineFilter} is provided to the
         * constructor, it points to the unfiltered list.
         */
        public final List<String> filteredStderrLines;

        public ExecutionResult(int exitCode, String stdout, String stderr) throws IOException {
            this(exitCode, stdout, stderr, null);
        }

        public ExecutionResult(int exitCode, String stdout, String stderr, LineFilter lineFilter) throws IOException {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.stdoutLines = parseIntoLines(stdout);
            this.stderrLines = parseIntoLines(stderr);
            if (lineFilter != null) {
                filteredStdoutLines = filterLines(stdoutLines, lineFilter);
                filteredStderrLines = filterLines(stderrLines, lineFilter);
            } else {
                filteredStdoutLines = stdoutLines;
                filteredStderrLines = stderrLines;
            }
        }

        private ExecutionResult(int exitCode, String stdout, String stderr, List<String> stdoutLines, List<String> stderrLines,
            LineFilter lineFilter) throws IOException {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.stdoutLines = stdoutLines;
            this.stderrLines = stderrLines;
            if (lineFilter != null) {
                filteredStdoutLines = filterLines(stdoutLines, lineFilter);
                filteredStderrLines = filterLines(stderrLines, lineFilter);
            } else {
                filteredStdoutLines = null;
                filteredStderrLines = null;
            }
        }

        /**
         * Applies a new {@link LineFilter} to an existing {@link ExecutionResult}, returning a result as if the filter had been applied
         * during construction.
         * 
         * @param lineFilter the new filter to apply
         * @return a new (immutable) {@link ExecutionResult} object
         */
        public ExecutionResult applyLineFilter(LineFilter lineFilter) {
            try {
                return new ExecutionResult(exitCode, stdout, stderr, stdoutLines, stderrLines, lineFilter);
            } catch (IOException e) {
                throw new RuntimeException("Unexpected exception while re-filtering", e);
            }
        }

        private List<String> parseIntoLines(String rawOutput) throws IOException {
            List<String> container = new ArrayList<>(INITIAL_STRING_CONTAINER_SIZE);
            BufferedReader reader = new BufferedReader(new StringReader(rawOutput));
            String line;
            while ((line = reader.readLine()) != null) {
                container.add(line);
            }
            return Collections.unmodifiableList(container);
        }

        private List<String> filterLines(List<String> input, LineFilter lineFilter) {
            List<String> container = new ArrayList<>(INITIAL_STRING_CONTAINER_SIZE);
            for (String line : input) {
                if (lineFilter.accept(line)) {
                    container.add(line);
                }
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
        CapturingTextOutReceiver stdoutCapture = new CapturingTextOutReceiver();
        CapturingTextOutReceiver stderrCapture = new CapturingTextOutReceiver();
        final TextStreamWatcher stdoutWatcher = TextStreamWatcherFactory.create(executor.getStdout(), stdoutCapture).start();
        final TextStreamWatcher stderrWatcher = TextStreamWatcherFactory.create(executor.getStderr(), stderrCapture).start();
        int exitCode = executor.waitForTermination();
        stdoutWatcher.waitForTermination();
        stderrWatcher.waitForTermination();
        return new ExecutionResult(exitCode, stdoutCapture.getBufferedOutput(), stderrCapture.getBufferedOutput());
    }
}
