/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Low-level command-line execution API. Implementations may include local and remote command
 * execution, for example via SSH.
 * 
 * @author Robert Mischke
 * 
 */
public interface CommandLineExecutor {

    /**
     * Sets an environment key-value pair; only effective before calling {@link #prepare(String)}.
     * 
     * @param key the alphanumeric key string; must not start with a number
     * @param value the value; may be null to un-set the value for the given key
     */
    void setEnv(String key, String value);

    /**
     * Retrieves an environment value. Note that this method does *not* provide access to
     * environment variables of the target host; only values set through
     * {@link #setEnv(String, String)} can be retrieved.
     * 
     * @param key the alphanumeric key string; must not start with a number
     * @return the previously set value; otherwise, null
     */
    String getEnv(String key);

    /**
     * @return the platform-specific, absolute path of the current working directory
     */
    String getWorkDirPath();

    /**
     * Starts the executor with the provided command line to run. The command line is automatically
     * prefixed with the required wrapper commands (like "cmd.exe" or "/bin/sh") for the target
     * platform, so these should not be included.
     * 
     * @param commandString the command line to execute
     * 
     * @throws IOException if an I/O error occurs on start of the target executable
     */
    void start(String commandString) throws IOException;

    /**
     * Starts the executor with the provided command line to run, and an input stream as a source of
     * StdIn data. The command line is automatically prefixed with the required wrapper commands
     * (like "cmd.exe" or "/bin/sh") for the target platform, so these should not be included.
     * 
     * @param commandString the command line to execute
     * @param stdinStream the input stream to read standard input data from, or "null" to disable
     * 
     * @throws IOException if an I/O error occurs on start of the target executable
     */
    void start(String commandString, InputStream stdinStream) throws IOException;

    /**
     * Starts the executor with several provided command lines to run. Classes implementing this
     * method may either concatenate these commands to a single command (for example,
     * "command1 && command2", or execute them sequentially. Callers should not rely on specific
     * aspects of any approach.
     * 
     * Regardless of which approach is chosen, the required wrapper commands (like "cmd.exe" or
     * "/bin/sh") for the target platform are automatically added, so these should not be included.
     * 
     * @param commandStrings the command lines to execute
     * 
     * @throws IOException if an I/O error occurs on start of the target executable
     */
    void startMultiLineCommand(String[] commandStrings) throws IOException;

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

    /**
     * Downloads the predefined work directory.
     * 
     * @param localDir the local target directory
     * @throws IOException on I/O errors
     */
    void downloadWorkdir(File localDir) throws IOException;

    /**
     * Uploads a local file to a relative location inside the predefined work directory.
     * 
     * @param localFile the local source file
     * @param remoteLocation the target path, relative to the executor's work directory
     * @throws IOException on I/O errors
     */
    void uploadFileToWorkdir(File localFile, String remoteLocation) throws IOException;
    
    /**
     * Downloads a remote file from a relative location inside the predefined work directory.
     * 
     * @param remoteLocation the source path, relative to the executor's work directory
     * @param localFile the local target file
     * @throws IOException on I/O errors
     */
    void downloadFileFromWorkdir(String remoteLocation, File localFile) throws IOException;
    
    /**
     * Uploads a local directory to a relative location inside the predefined work directory.
     * 
     * @param localDirectory the local source directory
     * @param remoteLocation the target path, relative to the executor's work directory
     * @throws IOException on I/O errors
     */
    void uploadDirectoryToWorkdir(File localDirectory, String remoteLocation) throws IOException;
    
    /**
     * Uploads a local directory to a relative location inside the predefined work directory.
     * 
     * @param remoteLocation the relative remote path
     * @param localDirectory the local target directory
     * @throws IOException on I/O errors
     */
    void downloadDirectoryFromWorkdir(String remoteLocation, File localDirectory) throws IOException;

    /**
     * Downloads file by the given absolute path to it.
     * @param remoteLocation the absolute source path
     * @param localFile local target file
     * @throws IOException on I/O errors
     */
    void downloadFile(String remoteLocation, File localFile) throws IOException;
    
    /**
     * Downloads directory by the given absolute path to it.
     * @param remoteLocation the absolute source path
     * @param localDirectory the local target directory
     * @throws IOException on I/O errors
     */
    void downloadDirectory(String remoteLocation, File localDirectory) throws IOException;
    
    /**
     * Uploads file by the given absolute path to it.
     * @param localFile local source file
     * @param remoteLocation the absolute target path
     * @throws IOException on I/O errors
     */
    void uploadFile(File localFile, String remoteLocation) throws IOException;
    
    /**
     * Uploads directory to a remote location given by the absolute path.
     * 
     * @param localDirectory local source directory
     * @param remoteLocation the absolute target path
     * @throws IOException on I/O errors
     */
    void uploadDirectory(File localDirectory, String remoteLocation) throws IOException;
    
    /**
     * Performs a remote-to-remote file copy. The current work directory is NOT taken into account;
     * the file/path arguments are used as-is for the underlying platform. For local execution, the
     * path strings should match the return value of {@link File#getAbsolutePath()}; for remote
     * execution, the required strings are implementation-dependent.
     * 
     * The reason that this method uses absolute paths is to allow exchange between the execution
     * sandbox and other files on the execution platform, for example for remote storage of data
     * that should not be discarded on teardown of the sandbox, but should not be transfered to the
     * caller either, usually for performance reasons.
     * 
     * TODO provide "copy remotely from work dir" and "copy remotely to work dir" methods instead?
     * 
     * TODO make these path strings as platform-independent as possible
     * 
     * @param remoteSource the remote path to copy from
     * @param remoteTarget the remote path to copy to
     * @throws IOException if the remote copy operation failed
     */
    void remoteCopy(String remoteSource, String remoteTarget) throws IOException;

}
