/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.shutdown.HeadlessShutdown;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.AbstractTextOutputReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * Provides the actual operations to interact with external installations and profiles. Separated from the coordinating service for
 * testability.
 * 
 * @author Robert Mischke
 */
public class InstanceOperationsImpl {

    private static final int STARTUP_WAIT_TIMEOUT_SEC = 60;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Starts the given profile using the specified installation.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter
     * @param installationDir the directory containing the installation (the main executable, /plugins, /configuration, ...)
     * @throws IOException on startup failure
     */
    public void startInstanceUsingInstallation(final File profileDir, final File installationDir) throws IOException {
        final File installationConfigDir = new File(installationDir, "configuration");
        if (!installationConfigDir.isDirectory()) {
            throw new IOException("Expected to find an installation configuration directory at '" + installationConfigDir.getAbsolutePath()
                + "' but it does not seem to exist");
        }

        final CountDownLatch startupOutputDetected = new CountDownLatch(1);
        final AtomicBoolean startupOutputIndicatesSuccess = new AtomicBoolean();

        try {
            final LocalApacheCommandLineExecutor executor = new LocalApacheCommandLineExecutor(installationDir);
            if (OSFamily.isWindows()) {
                // note: using "-p" because "--profile" was not available in 6.0.x
                executor.start(String.format("rce --headless -nosplash -p \"%s\"", profileDir.getAbsolutePath()));
            } else {
                // note: using "-p" because "--profile" was not available in 6.0.x
                executor.start(String.format("./rce --headless -nosplash -p \"%s\"", profileDir.getAbsolutePath()));
            }
            TextStreamWatcher outWatcher = new TextStreamWatcher(executor.getStdout(), new AbstractTextOutputReceiver() {

                @Override
                public void addOutput(String line) {
                    log.debug("Instance stdout: " + line);
                    if (line.startsWith("Using profile")) {
                        startupOutputIndicatesSuccess.set(true);
                        startupOutputDetected.countDown();
                    }
                }
            });
            TextStreamWatcher errWatcher = new TextStreamWatcher(executor.getStderr(), new AbstractTextOutputReceiver() {

                @Override
                public void addOutput(String line) {
                    log.debug("Instance stderr: " + line);
                    if (line.startsWith("Failed to lock profile ")) {
                        // TODO capture fallback location? (and maybe shut it down immediately?)
                        startupOutputIndicatesSuccess.set(false);
                        startupOutputDetected.countDown();
                    }
                }
            });
            outWatcher.start();
            errWatcher.start();
            // TODO determine proper timeout
            if (!startupOutputDetected.await(STARTUP_WAIT_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                throw new IOException("Timeout reached while waiting for output of started instance");
            }
            if (!startupOutputIndicatesSuccess.get()) {
                throw new IOException(
                    "The startup process indicated an error; most likely, the specified profile is in use or cannot be created");
            }

            SharedThreadPool.getInstance().execute(new Runnable() {

                @Override
                @TaskDescription("Instance Management: Asynchronously wait for launcher termination")
                public void run() {
                    try {
                        executor.waitForTermination();
                    } catch (IOException | InterruptedException e) {
                        log.error("Error during instance launcher execution", e);
                    }
                    log.info("Instance launcher terminated");
                }
            });

        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for the RCE startup to complete", e);
        }
    }

    /**
     * Tests whether the given profile directory is locked by a running instance.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter
     * @return true if the directory is locked
     * @throws IOException on I/O exceptions while testing
     */
    public boolean isProfileLocked(File profileDir) throws IOException {
        if (!profileDir.isDirectory()) {
            throw new IOException("Profile directory " + profileDir.getAbsolutePath() + " can not be created or is not a directory");
        }

        File lockfile = new File(profileDir, BootstrapConfiguration.PROFILE_DIR_LOCK_FILE_NAME);
        FileLock lock = null;
        if (!lockfile.isFile()) {
            return false;
        }
        // try to get a lock on this file
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, "rw")) {
            lock = randomAccessFile.getChannel().tryLock();
            if (lock != null) {
                lock.release();
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            throw new IOException("Unexpected error when trying to acquire a file lock on " + lockfile, e);
        }
    }

    /**
     * Stops the instance using the given profile.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter
     * @throws IOException on I/O exceptions while sending the shutdown signal
     */
    public void shutdownInstance(File profileDir) throws IOException {
        if (!isProfileLocked(profileDir)) {
            // TODO use a return code instead?
            throw new IOException("Tried to shut down a non-running profile");
        }
        try {
            final int initialWaitBeforeSendingSignal = 1000;
            final int maxWaitIterations = 20;
            final int singleWaitDuration = 500;

            // TODO improve; poll for shutdown info file to appear instead of just waiting
            Thread.sleep(initialWaitBeforeSendingSignal);

            new HeadlessShutdown().shutdownExternalInstance(profileDir);

            // after sending the signal, wait for the instance JVM to terminate, which releases the lock
            for (int i = 0; i < maxWaitIterations; i++) {
                if (!isProfileLocked(profileDir)) {
                    // success --> delete instance.lock
                    deleteInstanceLockFromProfileFolder(profileDir);
                    return;
                } else {
                    Thread.sleep(singleWaitDuration); // keep waiting
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting", e);
        }
    }

    private void deleteInstanceLockFromProfileFolder(File profileDir) {
        for (File fileInProfileDir : profileDir.listFiles()) {
            if (fileInProfileDir.isFile()
                && BootstrapConfiguration.PROFILE_DIR_LOCK_FILE_NAME.equals(fileInProfileDir.getName())) {
                fileInProfileDir.delete();
                break;
            }
        }
    }
}
