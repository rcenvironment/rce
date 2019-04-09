/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Provides the actual operations to interact with external installations and profiles. Separated from the coordinating service for
 * testability.
 * 
 * @author Robert Mischke
 * @author David Scholz
 * 
 */
public class InstanceOperationsImpl {

    /**
     * Name of the instanemanagement.lock file.
     */
    private static final String INSTANCEMANAGEMENT_LOCK = "instancemanagement.lock";

    private static final int WAIT_TIMEOUT_SEC = 60;

    private static final String STARTUP_INTERRUPTED_EXCEPTION = "Interrupted while waiting for the RCE startup to complete";

    private Map<String, BlockingQueue<Runnable>> profileNameToStartShutdownTasksQueue = new ConcurrentHashMap<>();

    private Map<Runnable, CountDownLatch> taskToLatchMap = new ConcurrentHashMap<>();

    public InstanceOperationsImpl() {

    }

    /**
     * 
     * Starts the given profile using the specified installation.
     * 
     * @param profileDirList the list of profile directories, as expected by the "--profile" parameter.
     * @param installationDir the directory containing the installation (the main executable, /plugins, /configuration, ...).
     * @param timeout maximum time for the start up process.
     * @param userOutputReceiver provides output for the user.
     * @param startWithGUI <code>true</code> if the instance shall be started with the GUI, <code>false</code> otherwise.
     * @throws InstanceOperationException on startup failures.
     * 
     */
    public void startInstanceUsingInstallation(final List<File> profileDirList, final File installationDir, final long timeout,
        final TextOutputReceiver userOutputReceiver, final boolean startWithGUI) throws InstanceOperationException {

        final File installationConfigDir = new File(installationDir, "configuration");

        if (!installationConfigDir.isDirectory()) {
            throw new InstanceOperationException("Expected to find an installation configuration directory at '"
                + installationConfigDir.getAbsolutePath() + "' but it does not seem to exist");
        }

        final CountDownLatch startupOutputDetected = new CountDownLatch(profileDirList.size());
        final AtomicBoolean startuptOutputIndicatesSuccess = new AtomicBoolean(true);

        long tempTimeout = calcTimeout(timeout);

        for (File profile : profileDirList) {

            final CountDownLatch individualLatch = new CountDownLatch(1);

            Runnable r = new InstanceStarterTask(tempTimeout, startWithGUI,
                userOutputReceiver, profile, installationDir, individualLatch, startupOutputDetected,
                startuptOutputIndicatesSuccess);

            synchronized (taskToLatchMap) {
                taskToLatchMap.put(r, individualLatch);
            }

            doInstanceOperationStep(r, profile, tempTimeout, userOutputReceiver);
        }

        try {
            if (!startupOutputDetected.await(tempTimeout, TimeUnit.SECONDS)) {
                throw new InstanceOperationException("Timeout reached while waiting for startup to finish, aborting...");
            }

            if (!startuptOutputIndicatesSuccess.get()) {
                // TODO improve error message.
                throw new InstanceOperationException("Unexpected failure during startup.");
            }

        } catch (InterruptedException e) {
            throw new InstanceOperationException(STARTUP_INTERRUPTED_EXCEPTION);
        }

    }

    /**
     * Stops the instance using the given profile.
     * 
     * @param profileDirList the list of profile directories, as expected by the "--profile" parameter.
     * @param timeout maximum time to shutdown instance.
     * @param userOutputReceiver the outputReceiver.
     * @throws InstanceOperationException on shutdown failures.
     */
    public void shutdownInstance(List<File> profileDirList, long timeout, TextOutputReceiver userOutputReceiver)
        throws InstanceOperationException {

        final CountDownLatch startupOutputDetected = new CountDownLatch(profileDirList.size());
        long tempTimeout = calcTimeout(timeout);

        for (File profile : profileDirList) {

            final CountDownLatch individualLatch = new CountDownLatch(1);

            Runnable r = new InstanceShutdownTask(profile, tempTimeout, userOutputReceiver, startupOutputDetected, individualLatch);

            synchronized (taskToLatchMap) {
                taskToLatchMap.put(r, individualLatch);
            }

            doInstanceOperationStep(r, profile, tempTimeout, userOutputReceiver);
        }

        try {

            if (!startupOutputDetected.await(tempTimeout, TimeUnit.SECONDS)) {
                throw new InstanceOperationException("Timeout reached while waiting for shutdown to finish, aborting...");
            }

        } catch (InterruptedException e) {
            throw new InstanceOperationException(STARTUP_INTERRUPTED_EXCEPTION);
        }

    }

    private void doInstanceOperationStep(Runnable r, File profile, long timeout,
        TextOutputReceiver userOutputReceiver) throws InstanceOperationException {

        // TODO possible bottleneck?
        synchronized (profileNameToStartShutdownTasksQueue) {

            if (profileNameToStartShutdownTasksQueue.containsKey(profile.getName())) {

                try {

                    profileNameToStartShutdownTasksQueue.get(profile.getName()).put(r);

                } catch (InterruptedException e) {
                    throw new InstanceOperationException(STARTUP_INTERRUPTED_EXCEPTION);
                }

            } else {

                BlockingQueue<Runnable> instanceStartupTaskQueue = new LinkedBlockingQueue<Runnable>();
                InstanceOperationsWorkerTask workerTask =
                    new InstanceOperationsWorkerTask(instanceStartupTaskQueue, timeout, userOutputReceiver,
                        profile);

                try {
                    instanceStartupTaskQueue.put(r);
                } catch (InterruptedException e) {
                    throw new InstanceOperationException(STARTUP_INTERRUPTED_EXCEPTION);
                }

                ConcurrencyUtils.getAsyncTaskService().submit(workerTask);
                profileNameToStartShutdownTasksQueue.put(profile.getName(), instanceStartupTaskQueue);

            }

        }

    }

    private long calcTimeout(long timeout) {
        long tempTimeout;
        if (timeout == 0) {
            tempTimeout = WAIT_TIMEOUT_SEC;
        } else {
            tempTimeout = timeout;
        }
        return tempTimeout;
    }

    /**
     * 
     * Starts the start and the shutdown tasks of each profile.
     *
     * @author David Scholz
     */
    private class InstanceOperationsWorkerTask implements Runnable {

        private final BlockingQueue<Runnable> sharedQueue;

        private final long timeout;

        private final TextOutputReceiver userOutputReceiver;

        private final File profile;

        InstanceOperationsWorkerTask(BlockingQueue<Runnable> sharedQueue, long timeout, TextOutputReceiver userOutputreceiver,
            File profile) {
            this.sharedQueue = sharedQueue;
            this.timeout = timeout;
            this.userOutputReceiver = userOutputreceiver;
            this.profile = profile;
        }

        @Override
        @TaskDescription("IM start/stop execution")
        public void run() {

            while (true) {

                Runnable task = sharedQueue.poll();

                if (task == null) {
                    if (doCleanUp()) {
                        return;
                    }
                    continue;
                }

                CountDownLatch latch = null;
                synchronized (taskToLatchMap) {
                    latch = taskToLatchMap.get(task);
                }

                if (task != null) {
                    ConcurrencyUtils.getAsyncTaskService().submit(task);
                    try {
                        if (latch != null) {
                            latch.await(timeout, TimeUnit.SECONDS);
                        }
                        releaseAndDeleteLockFile(profile);
                        synchronized (taskToLatchMap) {
                            taskToLatchMap.remove(task);
                        }
                    } catch (InterruptedException e) {
                        userOutputReceiver.addOutput("An error occured while waiting for instance with id: ");
                    } catch (IOException e) {
                        userOutputReceiver
                            .addOutput("Failed to release and/or delete IM lockfile. Aborted with message: " + e.getMessage());
                    }
                }

                if (doCleanUp()) {
                    return;
                }

            }

        }

        private boolean doCleanUp() {
            // clean up, but check if queue is empty first! if not, it could happen that, the worker threads terminated but at the same
            // time a new task is put into the sharedQueue
            synchronized (profileNameToStartShutdownTasksQueue) {
                if (!sharedQueue.isEmpty()) {
                    return false;
                } else {
                    profileNameToStartShutdownTasksQueue.remove(profile.getName());
                    return true;
                }
            }
        }
    }

    private void releaseIMLockFile(final File profile) throws IOException {

        File lockfile = new File(profile.getAbsolutePath() + "/" + INSTANCEMANAGEMENT_LOCK);
        FileLock lock = null;

        if (!lockfile.isFile()) {
            return;
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, InstanceOperationsUtils.IM_LOCK_FILE_ACCESS_PERMISSIONS)) {

            lock = randomAccessFile.getChannel().tryLock();

            if (lock != null) {
                lock.release();
            } else {
                throw new IOException("Could not release lock as it's hold by another instance.");
            }

        } catch (IOException e) {
            throw new IOException(InstanceOperationsUtils.UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON + lockfile, e);
        }

    }

    private void deleteIMFile(final File profile, final String name) throws IOException {

        if (profile.isDirectory()) {

            String fileName = "";
            if (name.equals("installation")) {
                fileName = name;
            } else {
                fileName = INSTANCEMANAGEMENT_LOCK;
            }

            for (File f : profile.listFiles()) {

                if (f.getName().equals(fileName)) {

                    boolean success = f.delete();
                    if (!success) {
                        throw new IOException("Failed to delete " + fileName + " file.");
                    }
                    break;

                }
            }
        }
    }

    private void releaseAndDeleteLockFile(final File profile) throws IOException {
        releaseIMLockFile(profile);
        deleteIMFile(profile, INSTANCEMANAGEMENT_LOCK);
    }

}
