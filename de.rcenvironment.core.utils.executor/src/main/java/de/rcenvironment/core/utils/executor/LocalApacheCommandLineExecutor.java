/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * A {@link CommandLineExecutor} that executes the given commands locally.
 * 
 * @author Robert Mischke
 * 
 */
public class LocalApacheCommandLineExecutor extends AbstractCommandLineExecutor implements CommandLineExecutor {

    /**
     * Sets the execution flag for the specified file.
     */
    private static final String LINUX_MAKE_FILE_EXECUTABLE_TEMPLATE = "chmod +x %s";

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

    private void checkAndCreateWorkDir() throws IOException {
        if (!workDir.isDirectory()) {
            // try to create the work directory
            workDir.mkdirs();
            if (!workDir.isDirectory()) {
                throw new IOException("Failed to create provided work directory " + workDir.getAbsolutePath());
            }
        }
    }

    private void executeCommand(CommandLine cmd, final InputStream stdinStream) throws IOException {
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

    @Override
    public void start(String commandString, final InputStream stdinStream) throws IOException {

        checkAndCreateWorkDir();

        CommandLine cmd = ProcessUtils.constructCommandLine(commandString);

        executeCommand(cmd, stdinStream);
    }

    // Executes a script by writing the script into a temporary file, setting the executable bit and executing the script.
    private void executeShebangScript(String scriptString, final InputStream stdinStream) throws IOException, InterruptedException {

        checkAndCreateWorkDir();

        // write the scriptString into a file in the temp directory
        final File scriptFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("script");
        log.debug(StringUtils.format("Writing script to %s", scriptFile.getAbsolutePath()));
        FileUtils.writeStringToFile(scriptFile, scriptString, false);

        log.debug(scriptFile.exists());
        log.debug(scriptFile.length());
        
        // make the file executable
        String makeExecutableCmd = StringUtils.format(LINUX_MAKE_FILE_EXECUTABLE_TEMPLATE, scriptFile.getAbsolutePath());
        // we cannot call start on the same executor twice since the canceling does not support this yet
        LocalApacheCommandLineExecutor makeExecutableExecutor = new LocalApacheCommandLineExecutor(workDir);
        makeExecutableExecutor.start(makeExecutableCmd);
        int exitCode = makeExecutableExecutor.waitForTermination();
        log.debug(StringUtils.format("chmod +x finished with exit code %d", exitCode));

        log.debug("scriptFile.exists(): " + scriptFile.exists());

        // execute the script
        CommandLine cmd = new CommandLine(scriptFile.getAbsolutePath());
        executeCommand(cmd, stdinStream);
        // if an interpreter is chosen, which is not available on the system, the streams are not closed correctly
        // TODO check for errors during execution, e.g. interpreter is not available

        // TODO if we dispose the script now, it might be deleted before it is executed 
        //TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(scriptFile);
    }

    /**
     * Executes a script by writing the script into a temporary file, setting the executable bit and executing the script.
     * 
     * On Linux, if the first line of the script starts either with "#!/bin/sh" or with "#!/bin/bash", the script temporarily will be
     * written to a file and executed. Other interpreters are currently not supported, instead, all lines will be concatenated and executed
     * as a single shell command.
     * 
     * @param scriptString The script that should be executed.
     * @param stdinStream the input stream to read standard input data from, or "null" to disable
     * @throws IOException On IO errors.
     * @throws InterruptedException If interrupted while waiting for an operation to finish.
     */
    public void executeScript(String scriptString, final InputStream stdinStream) throws IOException, InterruptedException {

        if (OS.isFamilyWindows()) {
            this.startMultiLineCommand(scriptString.split("\r?\n|\r"));
        } else if (OS.isFamilyUnix()) {

            // check if the scriptString contains a shebang at its start
            // we currently only support sh and bash, since these are available on all major Linux distributions
            if (scriptString.startsWith("#!/bin/sh\n") || scriptString.startsWith("#!/bin/bash\n")) {
                executeShebangScript(scriptString, stdinStream);
            } else {
                this.startMultiLineCommand(scriptString.split("\r?\n|\r"));
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
        int exitValue = resultHandler.getExitValue();
        return exitValue;
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
     * TODO what happens if this is called multiple times?
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
