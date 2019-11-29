/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import de.rcenvironment.core.instancemanagement.InstanceStatus;
import de.rcenvironment.core.instancemanagement.InstanceStatus.InstanceState;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Provides the actual operations to interact with external installations and profiles. Separated from the coordinating service for
 * testability.
 * 
 * @author Robert Mischke
 * @author David Scholz
 * @author Brigitte Boden
 * @author Lukas Rosenbach
 * 
 */
public class InstanceOperationsImpl {

    /**
     * Name of the instanemanagement.lock file.
     */
    private static final String INSTANCEMANAGEMENT_LOCK = "instancemanagement.lock";

    private static final int WAIT_TIMEOUT_SEC = 60;

    private static final String STARTUP_INTERRUPTED_EXCEPTION = "Interrupted while waiting for the RCE startup to complete";

    private static final String COMMAND_ARGUMENTS_FILE_NAME = "im_command_arguments";

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
     * @param profileIdToInstanceStatusMap map of InstanceStatus-objects linked to their instances
     * @param timeout maximum time for the start up process.
     * @param userOutputReceiver provides output for the user.
     * @param startWithGUI <code>true</code> if the instance shall be started with the GUI, <code>false</code> otherwise.
     * @throws InstanceOperationException on startup failures.
     * 
     */
    public void startInstanceUsingInstallation(final List<File> profileDirList, final File installationDir,
        final ConcurrentMap<String, InstanceStatus> profileIdToInstanceStatusMap, final long timeout,
        final TextOutputReceiver userOutputReceiver, final boolean startWithGUI) throws InstanceOperationException {

        final File installationConfigDir = new File(installationDir, "configuration");

        if (!installationConfigDir.isDirectory()) {
            throw new InstanceOperationException("Expected to find an installation configuration directory at '"
                + installationConfigDir.getAbsolutePath() + "' but it does not seem to exist");
        }

        final CountDownLatch startupOutputDetected = new CountDownLatch(profileDirList.size());
        final AtomicBoolean startuptOutputIndicatesSuccess = new AtomicBoolean(true);

        long tempTimeout = calcTimeout(timeout);

        Map<String, InstanceOperationsWorkerTask> profileToWorkerTask = new HashMap<>();
        Map<String, Runnable> profileToStarterTask = new HashMap<>();

        for (File profile : profileDirList) {

            final CountDownLatch individualLatch = new CountDownLatch(1);
            final InstanceStatus status = profileIdToInstanceStatusMap.get(profile.getName());

            String commandArguments;
            try {
                if (hasCommandArgumentsFile(profile)) {
                    commandArguments = readCommandArguments(profile);
                } else {
                    // introduced to fix #0016945
                    commandArguments = "";
                }
            } catch (IOException e) {
                throw new InstanceOperationException("Failed to read command arguments file in profile directory: " + profile.getName());
            }

            Runnable r = new InstanceStarterTask(tempTimeout, startWithGUI,
                userOutputReceiver, profile, installationDir, commandArguments, individualLatch, startupOutputDetected,
                startuptOutputIndicatesSuccess, status);
            profileToStarterTask.put(profile.getName(), r);

            synchronized (taskToLatchMap) {
                taskToLatchMap.put(r, individualLatch);
            }

            InstanceOperationsWorkerTask workerTask = doInstanceOperationStep(r, profile, tempTimeout, userOutputReceiver);
            if (workerTask != null) {
                profileToWorkerTask.put(profile.getName(), workerTask);
            }
        }

        try {
            if (!startupOutputDetected.await(tempTimeout, TimeUnit.SECONDS)) {
                List<String> failedInstances = new ArrayList<>();
                for (File profile : profileDirList) {
                    String instanceName = profile.getName();
                    InstanceStatus instanceStatus = profileIdToInstanceStatusMap.get(instanceName);
                    if (instanceStatus.getInstanceState() == InstanceState.STARTING) {
                        instanceStatus.setInstanceState(InstanceState.NOTRUNNING);
                        failedInstances.add(instanceName);

                        InstanceOperationsWorkerTask workerTask = profileToWorkerTask.get(instanceName);
                        if (workerTask != null) {
                            Future<?> future = workerTask.getFutureOfTask(profileToStarterTask.get(instanceName));
                            if (future != null) {
                                future.cancel(true);
                            }
                        }
                    }
                }

                String message = "Timeout reached while waiting for startup to finish, aborting...(failed instances: ";
                for (String name : failedInstances) {
                    message += name + " ";
                }
                message += ")";
                throw new InstanceOperationException(message);
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

    private String readCommandArguments(File profile) throws IOException {
        File commandArgumentsFile = new File(profile.getPath(), COMMAND_ARGUMENTS_FILE_NAME);
        return FileUtils.readFileToString(commandArgumentsFile, Charsets.UTF_8);
    }

    /**
     * Writes the given command argument string into the standard file within the given profile.
     * 
     * @param profile the profile root directory
     * @param argumentsToWrite the argument string
     * @throws IOException on error
     */
    public void writeCommandArguments(File profile, String argumentsToWrite) throws IOException {
        File commandArgumentsFile = new File(profile.getPath(), COMMAND_ARGUMENTS_FILE_NAME);
        FileUtils.writeStringToFile(commandArgumentsFile, argumentsToWrite, Charsets.UTF_8);
    }

    /**
     * Tests whether a standard command arguments file exists within the given profile directory.
     * 
     * @param profile the profile root directory
     * @return true if a standard command arguments file exists within the given profile
     */
    public boolean hasCommandArgumentsFile(File profile) {
        return new File(profile, COMMAND_ARGUMENTS_FILE_NAME).exists();
    }

    private InstanceOperationsWorkerTask doInstanceOperationStep(Runnable r, File profile, long timeout,
        TextOutputReceiver userOutputReceiver) throws InstanceOperationException {

        // TODO possible bottleneck?
        synchronized (profileNameToStartShutdownTasksQueue) {

            if (profileNameToStartShutdownTasksQueue.containsKey(profile.getName())) {

                try {

                    profileNameToStartShutdownTasksQueue.get(profile.getName()).put(r);
                    // TODO solution without returning null
                    return null;

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
                return workerTask;
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

        private Map<Runnable, Future<?>> submittedRunnableToFutureMap;

        InstanceOperationsWorkerTask(BlockingQueue<Runnable> sharedQueue, long timeout, TextOutputReceiver userOutputreceiver,
            File profile) {
            this.sharedQueue = sharedQueue;
            this.timeout = timeout;
            this.userOutputReceiver = userOutputreceiver;
            this.profile = profile;
            this.submittedRunnableToFutureMap = new HashMap<>();
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
                    Future<?> future = ConcurrencyUtils.getAsyncTaskService().submit(task);
                    synchronized (submittedRunnableToFutureMap) {
                        submittedRunnableToFutureMap.put(task, future);
                    }
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

        public Future<?> getFutureOfTask(Runnable task) {
            synchronized (submittedRunnableToFutureMap) {
                return submittedRunnableToFutureMap.get(task);
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
