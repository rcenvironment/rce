/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link CommandLineExecutor} that executes the given commands locally.
 * 
 * @author Robert Mischke
 * 
 */
public class LocalApacheCommandLineExecutor extends AbstractCommandLineExecutor implements CommandLineExecutor {

    private File workDir;

    private final Log log = LogFactory.getLog(getClass());

    private DefaultExecutor executor;

    private ExecuteWatchdog watchdog;

    private PipedInputStream pipedStdInputStream;

    private PipedInputStream pipedErrInputStream;

    private DefaultExecuteResultHandler resultHandler;

    private PipedOutputStream pipedStdOutputStream;

    private PipedOutputStream pipedErrOutputStream;

    private ExtendedPumpStreamHandler streamHandler;

    private ProcessExtractor processExtractor;

    private boolean cancelRequested = false;

    /**
     * Creates a local executor with the given path as its working directory. If the given path is not a directory, it is created.
     * 
     * @param workDirPath the directory on the local system to use for execution
     * 
     * @throws IOException if the given {@link File} is not a directory and also could not be created
     */
    public LocalApacheCommandLineExecutor(File workDirPath) throws IOException {

        this.workDir = workDirPath;
    }

    @Override
    public void start(String commandString) throws IOException {
        start(commandString, null);
    }

    @Override
    public void start(String commandString, final InputStream stdinStream) throws IOException {

        if (!workDir.isDirectory()) {
            // try to create the work directory
            workDir.mkdirs();
            if (!workDir.isDirectory()) {
                throw new IOException("Failed to create provided work directory " + workDir.getAbsolutePath());
            }
        }

        CommandLine cmd = ProcessUtils.constructCommandLine(commandString);

        pipedStdOutputStream = new PipedOutputStream();
        pipedErrOutputStream = new PipedOutputStream();
        pipedStdInputStream = new PipedInputStream(pipedStdOutputStream);
        pipedErrInputStream = new PipedInputStream(pipedErrOutputStream);

        watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        executor = new DefaultExecutor();
        executor.setWatchdog(watchdog);
        resultHandler = new DefaultExecuteResultHandler();
        streamHandler = new ExtendedPumpStreamHandler(pipedStdOutputStream, pipedErrOutputStream, stdinStream);
        executor.setStreamHandler(streamHandler);
        executor.setWorkingDirectory(workDir);
        // set a special watchdog to get access to the process object
        processExtractor = new ProcessExtractor();
        executor.setWatchdog(processExtractor);

        // this block is synchronized to avoid a race condition where the initial cancelRequested check in this block is already passed and
        // then the cancel method is called, which would lead to an uncanceled process.
        synchronized (this) {
            if (cancelRequested) {
                resultHandler.onProcessComplete(1);
            } else {
                if (env.isEmpty()) {
                    executor.execute(cmd, resultHandler);
                } else {
                    executor.execute(cmd, env, resultHandler);
                }
            }
        }
    }

    /**
     * Destroys the running process manually.
     */
    public void manuallyDestroyProcess() {
        watchdog.destroyProcess();
    }

    @Override
    public String getWorkDirPath() {
        return workDir.getAbsolutePath();
    }

    @Override
    public InputStream getStdout() {
        return pipedStdInputStream;
    }

    @Override
    public InputStream getStderr() {
        return pipedErrInputStream;
    }

    @Override
    public int waitForTermination() throws IOException, InterruptedException {
        resultHandler.waitFor();
        return resultHandler.getExitValue();
    }

    public DefaultExecuteResultHandler getResultHandler() {
        return resultHandler;
    }

    /**
     * This class overrides the normal {@link PumpStreamHandler} because the closeWhenExhausted flag when creating a pump must be set.
     * 
     * @author Sascha Zur
     * 
     */
    private class ExtendedPumpStreamHandler extends PumpStreamHandler {

        ExtendedPumpStreamHandler(
            PipedOutputStream pipedStdOutputStream,
            PipedOutputStream pipedErrOutputStream, InputStream stdinStream) {
            super(pipedStdOutputStream, pipedErrOutputStream, stdinStream);
        }

        @Override
        protected Thread createPump(final InputStream is, final OutputStream os) {
            return createPump(is, os, true);
        }
    }

    @Override
    public void uploadFileToWorkdir(File localFile, String remoteLocation) throws IOException {
        File targetFile = new File(workDir, remoteLocation);
        log.debug("Local copy from " + localFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
        FileUtils.copyFile(localFile, targetFile);
    };

    @Override
    public void downloadFileFromWorkdir(String remoteLocation, File localFile) throws IOException {
        FileUtils.copyFile(new File(workDir, remoteLocation), localFile);
    }

    @Override
    public void downloadWorkdir(File localDir) throws IOException {
        FileUtils.copyDirectory(workDir, localDir);
    }

    @Override
    public void remoteCopy(String remoteSource, String remoteTarget) throws IOException {
        FileUtils.copyFile(new File(remoteSource), new File(remoteTarget));
    }

    @Override
    public void uploadDirectoryToWorkdir(File localDirectory, String remoteLocation) throws IOException {
        File targetDirectory = new File(workDir, remoteLocation);
        targetDirectory.mkdirs();
        FileUtils.copyDirectory(localDirectory, targetDirectory);
    }

    @Override
    public void downloadDirectoryFromWorkdir(String remoteLocation, File localDirectory) throws IOException {
        FileUtils.copyDirectory(new File(workDir, remoteLocation), localDirectory);
    }

    @Override
    public void downloadFile(String remoteLocation, File localFile) throws IOException {
        FileUtils.copyFile(new File(remoteLocation), localFile);
    }

    @Override
    public void downloadDirectory(String remoteLocation, File localDirectory) throws IOException {
        FileUtils.copyDirectory(new File(remoteLocation), localDirectory);
    }

    @Override
    public void uploadFile(File localFile, String remoteLocation) throws IOException {
        FileUtils.copyFile(localFile, new File(remoteLocation));
    }

    @Override
    public void uploadDirectory(File localDirectory, String remoteLocation) throws IOException {
        FileUtils.copyDirectory(localDirectory, new File(remoteLocation));
    }

    public File getWorkDir() {
        return workDir;
    }

    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    /**
     * Requests cancellation of the started process. This will kill all descending processes too. If cancel is called before start, the
     * execution will not be started.
     * 
     * TODO This implementation currently only supports one call of start(). If it is necessary that multiple commands are started with the
     * same LocalApacheCommandLineExecutor, this method needs to be modified to receive the process object which should be killed. ~rode_to
     * 
     * @return true, if the process was canceled, otherwise false.
     */
    public synchronized boolean cancel() {
        cancelRequested = true;

        // start() has not been called yet
        if (processExtractor == null) {
            return false;
        }

        Process process = processExtractor.getProcess();

        // the process was not started yet
        if (process == null) {
            return false;
        }

        try {
            int pid = ProcessUtils.getPid(process);
            ProcessUtils.killProcessTree(pid);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
            | IllegalAccessException | IOException | InterruptedException e) {

            log.error("Unable to cancel the process.", e);
            return false;
        }

        return true;
    }
}
