/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Low-level command-line execution API for executing commands in the RCE console.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 * 
 */
public interface RCECommandLineExecutor {

    /**
     * Starts the executor with the provided command line to run. 
     * @param commandString the command line to execute
     * 
     * @throws IOException if an I/O error occurs on start of the target executable
     */
    void start(String commandString) throws IOException;

    /**
     * Starts the executor with the provided command line to run, and an input stream as a source of
     * StdIn data. 
     * 
     * @param commandString the command line to execute
     * @param stdinStream the input stream to read standard input data from, or "null" to disable
     * 
     * @throws IOException if an I/O error occurs on start of the target executable
     */
    void start(String commandString, InputStream stdinStream) throws IOException;

    /**
     * Returns the STDOUT stream; only valid after calling one of the "start" methods.
     * 
     * @return the standard output stream of the invoked command
     * @throws IOException if the stream could not be acquired
     */
    InputStream getStdout() throws IOException;

    /**
     * Returns the STDERR stream; only valid after calling one of the "start" methods.
     * 
     * @return the standard error stream of the invoked command
     * @throws IOException if the stream could not be acquired
     */
    InputStream getStderr() throws IOException;

    /**
     * Waits for the invoked command to end and returns its exit code. Waiting may be interrupted by
     * I/O errors of thread interruption.
     * 
     * @return the command-line exit code
     * @throws IOException if an I/O error occured while waiting, for example breakdown of a network
     *         connection to the execution host
     * @throws InterruptedException if the waiting thread was interrupted
     */
    int waitForTermination() throws IOException, InterruptedException;

}
