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
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A {@link CommandLineExecutor} that executes the given commands locally.
 * 
 * @author Robert Mischke
 * 
 */
public class LocalCommandLineExecutor extends AbstractCommandLineExecutor implements CommandLineExecutor {

    /** Top-level command token template for Linux invocation. */
    private static final String[] LINUX_SHELL_TOKENS = { "/bin/sh", "-c", "[command]" };

    /** Top-level command token template for Windows invocation. */
    private static final String[] WINDOWS_SHELL_TOKENS = { "cmd.exe", "/c", "[command]" };

    private File workDir;

    private Process process;

    private Log log = LogFactory.getLog(getClass());

    /**
     * Creates a local executor with the given path as its working directory. If the given path is
     * not a directory, it is created.
     * 
     * @param workDirPath the directory on the local system to use for execution
     * 
     * @throws IOException if the given {@link File} is not a directory and also could not be
     *         created
     */
    public LocalCommandLineExecutor(File workDirPath) throws IOException {

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

        // build environment key/value array
        Set<Entry<String, String>> entries = env.entrySet();
        String[] envArray = new String[entries.size()];
        int pos = 0;
        for (Entry<String, String> entry : entries) {
            envArray[pos++] = entry.getKey() + "=" + entry.getValue();
        }

        // build the top-level token array
        String[] commandTokens;
        if (OS.isFamilyWindows()) {
            commandTokens = Arrays.copyOf(WINDOWS_SHELL_TOKENS, WINDOWS_SHELL_TOKENS.length);
            commandTokens[WINDOWS_SHELL_TOKENS.length - 1] = commandString;
        } else {
            commandTokens = Arrays.copyOf(LINUX_SHELL_TOKENS, LINUX_SHELL_TOKENS.length);
            commandTokens[LINUX_SHELL_TOKENS.length - 1] = commandString;
        }

        // TODO rewrite to use Apache Commons-Exec?
        process = Runtime.getRuntime().exec(commandTokens, envArray, workDir);

        if (stdinStream != null) {
            final OutputStream stdin = process.getOutputStream();
            ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                @Override
                @TaskDescription("LocalCommandLineExecutor input stream pipe")
                public void run() {
                    try {
                        IOUtils.copy(stdinStream, stdin);
                        stdin.close();
                    } catch (IOException e) {
                        // TODO propagate to outside?
                        log.error("Error writing STDIN stream", e);
                    }
                };
            });
        }
    }

    @Override
    public String getWorkDirPath() {
        return workDir.getAbsolutePath();
    }

    @Override
    public InputStream getStdout() {
        // TODO make buffered?
        return process.getInputStream();
    }

    @Override
    public InputStream getStderr() {
        // TODO make buffered?
        return process.getErrorStream();
    }

    @Override
    public int waitForTermination() throws IOException, InterruptedException {
        return process.waitFor();
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
}
