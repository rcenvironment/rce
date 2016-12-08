/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.app.IApplication;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.shutdown.HeadlessShutdown;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.AbstractTextOutputReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Provides the actual operations to interact with external installations and profiles. Separated from the coordinating service for
 * testability.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class InstanceOperationsImpl implements InstanceOperations {

    private static final String DONE = "Done.";

    private static final List<InstanceOperationCallbackListener> CALL_BACK_LIST = new ArrayList<InstanceOperationCallbackListener>();

    private static final String SLASH = "/";

    private static final String RW = "rw";

    private static final String TIMEOUT_REACHED_MESSAGE =
        "Timeout reached while waiting for output of started instance with id %s, aborting...";

    private static final int WAIT_TIMEOUT_SEC = 60;

    private final Log log = LogFactory.getLog(getClass());

    private AsyncTaskService threadPool;

    private DualHashBidiMap<File, Future<Integer>> profileToFutureMap = new DualHashBidiMap<File, Future<Integer>>();

    private Map<File, FileLock> profileToLockMap = new ConcurrentHashMap<>();

    public InstanceOperationsImpl() {
        this.threadPool = ConcurrencyUtils.getAsyncTaskService();
    }

    /**
     * Starts the given profile using the specified installation.
     * 
     * @param profileDirList the list of profile directories, as expected by the "--profile" parameter
     * @param installationDir the directory containing the installation (the main executable, /plugins, /configuration, ...)
     * @param timeout maximum time for the start up process.
     * @param userOutputReceiver the outputReceiver.
     * @param startWithGUI true if instance shall be started with GUI
     * @throws IOException on startup failure
     * @throws InstanceOperationException on error.
     */
    @Override
    // TODO method too large; needs refactoring
    public void startInstanceUsingInstallation(final List<File> profileDirList, final File installationDir, final long timeout,
        final TextOutputReceiver userOutputReceiver, boolean startWithGUI)
        throws InstanceOperationException {
        final File installationConfigDir = new File(installationDir, "configuration");
        if (!installationConfigDir.isDirectory()) {
            throw new InstanceOperationException("Expected to find an installation configuration directory at '"
                + installationConfigDir.getAbsolutePath() + "' but it does not seem to exist", null);
        }
        final CountDownLatch startupOutputDetected = new CountDownLatch(profileDirList.size());
        final AtomicBoolean startupOutputIndicatesSuccess = new AtomicBoolean(true);
        try {
            // this won't cause any problems as {@link InstanceOperationsImpl#createAndgetExecutors()} never returns null.
            Collection<IMLocalApacheCommandLineExecutor> executors = null;
            try {
                executors = createAndGetExecutors(installationDir, profileDirList);
            } catch (IOException e) {
                throw new InstanceOperationException(e, null);
            }
            final List<File> failedProfileList = new ArrayList<>(profileDirList.size());
            final List<Future<Integer>> futureList = new ArrayList<Future<Integer>>(profileDirList.size());
            final List<File> successList = new ArrayList<File>();
            for (final IMLocalApacheCommandLineExecutor executor : executors) {
                if (timeout == 0) {
                    Future<Integer> future =
                        threadPool.submit(new LauncherTask(executor, WAIT_TIMEOUT_SEC, new StdoutStderrCallbackListener() {

                            @Override
                            public void startStdout() {
                                TextStreamWatcherFactory.create(executor.getStdout(), new AbstractTextOutputReceiver() {

                                    private final File profile = executor.getProfile();

                                    @Override
                                    public void addOutput(String line) {
                                        log.debug("Instance stdout: " + line);
                                        if (line.contains("complete")) { // intended to allow some text changes without breaking
                                            startupOutputIndicatesSuccess.compareAndSet(true, true); // TODO review: isn't this a NOP?
                                            successList.add(profile);
                                            executor.indicateSuccess();
                                            startupOutputDetected.countDown();
                                            userOutputReceiver.addOutput("Successfully started instance " + profile.getName());
                                        }
                                    }
                                }).start();
                            }

                            @Override
                            public void startStderr() {
                                TextStreamWatcherFactory.create(executor.getStderr(), new AbstractTextOutputReceiver() {

                                    private final File profile = executor.getProfile();

                                    @Override
                                    public void addOutput(String line) {
                                        log.debug("Instance stderr: " + line);
                                        // TODO check whether this actually overrides the success check above
                                        if (line.startsWith("Failed to lock profile ")) {
                                            // TODO capture fallback location? (and maybe shut it down immediately?)
                                            startupOutputIndicatesSuccess.compareAndSet(true, false);
                                            failedProfileList.add(profile);
                                            startupOutputDetected.countDown();
                                        }
                                    }
                                }).start();
                            }
                        }, startWithGUI));

                    futureList.add(future);
                    synchronized (profileToFutureMap) {
                        profileToFutureMap.put(executor.getProfile(), future);
                    }
                } else {
                    Future<Integer> future = threadPool.submit(new LauncherTask(executor, timeout, new StdoutStderrCallbackListener() {

                        @Override
                        public void startStdout() {
                            TextStreamWatcherFactory.create(executor.getStdout(), new AbstractTextOutputReceiver() {

                                private final File profile = executor.getProfile();

                                @Override
                                public void addOutput(String line) {
                                    log.debug("Instance stdout: " + line);
                                    if (line.startsWith("Using profile")) {
                                        startupOutputIndicatesSuccess.compareAndSet(true, true);
                                        successList.add(profile);
                                        executor.indicateSuccess();
                                        startupOutputDetected.countDown();
                                    }
                                }
                            }).start();
                        }

                        @Override
                        public void startStderr() {
                            TextStreamWatcherFactory.create(executor.getStderr(), new AbstractTextOutputReceiver() {

                                private final File profile = executor.getProfile();

                                @Override
                                public void addOutput(String line) {
                                    log.debug("Instance stderr: " + line);
                                    if (line.startsWith("Failed to lock profile ")) {
                                        // TODO capture fallback location? (and maybe shut it down immediately?)
                                        startupOutputIndicatesSuccess.compareAndSet(true, false);
                                        failedProfileList.add(profile);
                                        startupOutputDetected.countDown();
                                    }
                                }
                            }).start();
                        }
                    }, startWithGUI));
                    futureList.add(future);
                    synchronized (profileToFutureMap) {
                        profileToFutureMap.put(executor.getProfile(), future);
                    }
                }
            }
            if (timeout == 0) {
                if (!startupOutputDetected.await(WAIT_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    throw createInstanceOperationExceptionForStartuptimeoutReached(futureList, successList);
                }
            } else {
                if (!startupOutputDetected.await(timeout, TimeUnit.SECONDS)) {
                    throw createInstanceOperationExceptionForStartuptimeoutReached(futureList, successList);
                }
            }
            if (!startupOutputIndicatesSuccess.get()) {
                String message = formatListOfFailedProfiles(failedProfileList);
                throw new InstanceOperationException(message, failedProfileList);
            }
            synchronized (profileToFutureMap) {
                profileToFutureMap.clear();
            }
            userOutputReceiver.addOutput(DONE);
        } catch (InterruptedException e) {
            throw new InstanceOperationException("Interrupted while waiting for the RCE startup to complete", e, null);
        }
    }

    private String formatListOfFailedProfiles(final List<File> failedProfileList) {
        StringBuilder sb = new StringBuilder();
        sb.append("The startup process of the following instances indicated an error; most likely, "
            + "the specified profile is in use or cannot be created: ");
        sb.append("\n");
        int i = 0;
        for (File profile : failedProfileList) {
            if (i == failedProfileList.size()) {
                sb.append(profile.getName());
            } else {
                sb.append(profile.getName() + ",");
            }
            sb.append("\n");
            i++;
        }
        String message = sb.toString();
        return message;
    }

    private Collection<IMLocalApacheCommandLineExecutor> createAndGetExecutors(final File installationDir, final List<File> profileList)
        throws IOException {
        List<IMLocalApacheCommandLineExecutor> executorList = new ArrayList<>();
        for (File profile : profileList) {
            final IMLocalApacheCommandLineExecutor executor = new IMLocalApacheCommandLineExecutor(installationDir, profile);
            executorList.add(executor);
        }

        return Collections.unmodifiableCollection(executorList);
    }

    private InstanceOperationException createInstanceOperationExceptionForStartuptimeoutReached(List<Future<Integer>> futureList,
        List<File> sucessList) {
        List<Throwable> exceptions = new ArrayList<>();
        List<File> failedProfile = new ArrayList<>();
        for (Future<Integer> future : futureList) {
            try {
                future.get(1, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                exceptions.add(e.getCause());
                synchronized (profileToFutureMap) {
                    failedProfile.add(profileToFutureMap.getKey(future));
                }
            } catch (TimeoutException e) {
                synchronized (profileToFutureMap) {
                    for (File profile : sucessList) {
                        if (!profileToFutureMap.get(profile).equals(future)) {
                            exceptions.add(new IOException(
                                "Unexpected error: no exception was thrown after timeout was reached."));
                            failedProfile.add(profile);
                        }
                    }
                }
            } catch (InterruptedException e) {
                return new InstanceOperationException("Interrupted while waiting for the RCE startup to complete", e, null);
            }
        }
        // should never happen
        if (exceptions.isEmpty()) {
            return new InstanceOperationException("Timeout reached while waiting for startup to finish, aborting...", null);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Timeout reached while waiting for startup to finish, aborting...\n");
            for (Throwable throwable : exceptions) {
                sb.append(throwable.getMessage());
                sb.append("\n");
            }
            return new InstanceOperationException(sb.toString(), failedProfile);
        }
    }

    /**
     * 
     * Callback for starting {@link TextStreamWatcher} stdout and stderr.
     * 
     * @author David Scholz
     */
    private interface StdoutStderrCallbackListener {

        void startStdout();

        void startStderr();
    }

    /**
     * 
     * Task, which asynchronously waits for launcher termination.
     * 
     * @author David Scholz
     */
    private class LauncherTask implements Callable<Integer> {

        private final IMLocalApacheCommandLineExecutor executor;

        private final long timeout;

        private final boolean startWithGUI;

        private final StdoutStderrCallbackListener callbackListener;

        private volatile int exitcode = 0;

        LauncherTask(IMLocalApacheCommandLineExecutor executor, long timeout, final StdoutStderrCallbackListener stdCallback,
            boolean startWithGUI) {
            this.executor = executor;
            this.timeout = timeout;
            this.callbackListener = stdCallback;
            this.startWithGUI = startWithGUI;
        }

        @Override
        @TaskDescription("Instance Management: Asynchronously starts a RCE instance")
        public Integer call() throws IOException {
            synchronized (executor.getProfile()) {

                boolean success = false;
                try {
                    success = lockIMLockFile(executor.getProfile(), timeout);
                } catch (IOException e) {
                    fireCommandFinishEvent(executor.getProfile());
                    throw e;
                }
                if (!success) {
                    fireCommandFinishEvent(executor.getProfile());
                    throw new IOException("Timeout reached while trying to acquire the lock, aborting startup of instance with id: "
                        + executor.getProfile().getName());
                }
                if (OSFamily.isWindows()) {
                    if (!startWithGUI) {
                        // note: using "-p" because "--profile" was not available in 6.0.x
                        executor.start(StringUtils.format("rce --headless -nosplash -p \"%s\"", executor.getProfile().getAbsolutePath()));
                    } else {
                        executor.start(StringUtils.format("rce -p \"%s\"", executor.getProfile().getAbsolutePath()));
                    }
                } else {
                    if (!startWithGUI) {
                        // note: using "-p" because "--profile" was not available in 6.0.x
                        executor.start(StringUtils.format("./rce --headless -nosplash -p \"%s\"", executor.getProfile().getAbsolutePath()));
                    } else {
                        executor.start(StringUtils.format("./rce -p \"%s\"", executor.getProfile().getAbsolutePath()));
                    }
                }
                Future<Integer> future = threadPool.submit(new Callable<Integer>() {

                    @Override
                    @TaskDescription("Instance Management: Asynchronously wait for launcher termination")
                    public Integer call() throws IOException {
                        try {
                            exitcode = executor.waitForTermination();
                        } catch (IOException | InterruptedException e) {
                            executor.indicateSuccess();
                            throw new IOException("Error during instance launcher execution of instance with id: "
                                + executor.getProfile().getName());
                        }
                        if (exitcode != IApplication.EXIT_OK) {
                            executor.indicateSuccess();
                            throw new IOException("Failed to start instance. Launcher terminated with exit code: " + exitcode);
                        }
                        executor.indicateSuccess();
                        return exitcode;
                    }
                });
                callbackListener.startStdout();
                callbackListener.startStderr();
                try {
                    if (!executor.waitForSucces(timeout)) {
                        fireCommandFinishEvent(executor.getProfile());
                        throw new IOException(StringUtils.format(TIMEOUT_REACHED_MESSAGE, executor.getProfile().getName()));
                    }
                } catch (InterruptedException e) {
                    fireCommandFinishEvent(executor.getProfile());
                }
                fireCommandFinishEvent(executor.getProfile());
                try {
                    future.get(1, TimeUnit.MILLISECONDS);
                } catch (ExecutionException e) {
                    throw new IOException(e);
                } catch (InterruptedException e) {
                    throw new IOException("Launcher task was interrupted.");
                } catch (TimeoutException e) {
                    log.info("Instance launcher terminated");
                }
            }

            return exitcode;
        }
    }

    /**
     * 
     * Special Implementation of the {@link LocalApacheCommandLineExecutor} to connect an executor with a profile directory.
     * 
     * @author David Scholz
     */
    private class IMLocalApacheCommandLineExecutor extends LocalApacheCommandLineExecutor {

        private final CountDownLatch latch = new CountDownLatch(1);

        private final File profile;

        IMLocalApacheCommandLineExecutor(File workDirPath, File profile) throws IOException {
            super(workDirPath);
            this.profile = profile;
        }

        public void indicateSuccess() {
            latch.countDown();
        }

        public boolean waitForSucces(long timeout) throws InterruptedException {
            return latch.await(timeout, TimeUnit.SECONDS);
        }

        public File getProfile() {
            return profile;
        }

    }

    /**
     * 
     * Task, which asynchronously terminates a rce instance.
     * 
     * @author David Scholz
     */
    private class ShutdownTask implements Callable<InstanceShutdownCodeWrapper> {

        private final File profile;

        private final CountDownLatch latch;

        private final long timeout;

        ShutdownTask(final File profile, final CountDownLatch latch, final long timeout) {
            this.profile = profile;
            this.latch = latch;
            this.timeout = timeout;
        }

        @Override
        @TaskDescription("Instance Management: Asynchronously terminates rce instance")
        public InstanceShutdownCodeWrapper call() throws IOException {
            synchronized (profile) {
                boolean success = false;
                try {
                    success = lockIMLockFile(profile, timeout);
                } catch (IOException e) {
                    fireCommandFinishEvent(profile);
                    throw e;
                }
                if (!success) {
                    fireCommandFinishEvent(profile);
                    return new InstanceShutdownCodeWrapper(profile, InstanceShutdownCode.FAILED_SHUTDOWN);
                }
                if (!isProfileLocked(profile)) {
                    latch.countDown();
                    fireCommandFinishEvent(profile);
                    return new InstanceShutdownCodeWrapper(profile, InstanceShutdownCode.NON_RUNNING_PROFILE);
                }
                final int maxWaitIterations = 20;
                final int singleWaitDuration = 500;
                if (!detectShutdownFile(profile.getAbsolutePath())) {
                    latch.countDown();
                    fireCommandFinishEvent(profile);
                    return new InstanceShutdownCodeWrapper(profile, InstanceShutdownCode.FAILED_SHUTDOWN);
                }
                new HeadlessShutdown().shutdownExternalInstance(profile);

                // after sending the signal, wait for the instance JVM to terminate, which releases the lock
                for (int i = 0; i < maxWaitIterations; i++) {
                    if (!isProfileLocked(profile)) {
                        // success --> delete instance.lock
                        deleteInstanceLockFromProfileFolder(profile);
                        latch.countDown();
                        fireCommandFinishEvent(profile);
                        return new InstanceShutdownCodeWrapper(profile, InstanceShutdownCode.SUCCESSFUL_SHUTDOWN);
                    } else {
                        try {
                            Thread.sleep(singleWaitDuration);
                        } catch (InterruptedException e) {
                            latch.countDown();
                            fireCommandFinishEvent(profile);
                            return new InstanceShutdownCodeWrapper(profile, InstanceShutdownCode.INTERRUPTED_WHILE_WAITING);
                        }
                    }
                }
                latch.countDown();
                fireCommandFinishEvent(profile);
                return new InstanceShutdownCodeWrapper(profile, InstanceShutdownCode.FAILED_SHUTDOWN);
            }
        }
    }

    private boolean detectShutdownFile(final String path) throws IOException {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path shutdownFile = Paths.get(path + SLASH + BootstrapConfiguration.PROFILE_SHUTDOWN_DATA_SUBDIR + SLASH + SHUTDOWN_FILE_NAME);
        Path shutdownFileDir = shutdownFile.getParent();
        File file = new File(shutdownFileDir.toString());
        final int maxWaitingTime = 200;
        shutdownFileDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
        WatchKey watchKey;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (file.isDirectory()) {
                    for (File f : file.listFiles()) {
                        if (f.getName().equals(SHUTDOWN_FILE_NAME)) {
                            return true;
                        }
                    }
                }
                watchKey = watcher.take();
                if (!watchKey.isValid()) {
                    continue;
                }
            } catch (InterruptedException e) {
                log.info("detect shutdown file task was interrupted: " + e);
                break;
            }
            final List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
            for (WatchEvent<?> event : watchEvents) {
                if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                    Path createdFileRelativePath = (Path) event.context();
                    Path createdFileAbsolutePath = shutdownFileDir.resolve(createdFileRelativePath);
                    if (createdFileAbsolutePath.equals(shutdownFile)) {
                        for (int i = 0; i < 2; i++) {
                            if (Files.size(Paths.get(createdFileAbsolutePath.toUri())) == 0) {
                                try {
                                    Thread.sleep(maxWaitingTime);
                                    continue;
                                } catch (InterruptedException e) {
                                    throw new IOException("Interrupted while waiting for shutdown file to appear");
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 
     * Wrapper for actual {@link InstanceShutdownCode}.
     * 
     * @author David Scholz
     */
    private class InstanceShutdownCodeWrapper {

        private final File profile;

        private final InstanceShutdownCode code;

        InstanceShutdownCodeWrapper(final File profile, final InstanceShutdownCode code) {
            this.profile = profile;
            this.code = code;
        }

        public File getProfile() {
            return profile;
        }

        public InstanceShutdownCode getShutdownCode() {
            return code;
        }

    }

    /**
     * 
     * Exit codes for instance shutdown.
     * 
     * @author David Scholz
     */
    private enum InstanceShutdownCode {

        NON_RUNNING_PROFILE,

        INTERRUPTED_WHILE_WAITING,

        SUCCESSFUL_SHUTDOWN,

        FAILED_SHUTDOWN;
    }

    /**
     * Tests whether the given profile directory is locked by a running instance.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter
     * @return true if the directory is locked
     * @throws IOException on failure.
     */
    @Override
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
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, RW)) {
            lock = randomAccessFile.getChannel().tryLock();
            if (lock != null) {
                lock.release();
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            throw new IOException(UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON + lockfile, e);
        }
    }

    /**
     * Stops the instance using the given profile.
     * 
     * @param profileDirList the list of profile directories, as expected by the "--profile" parameter
     * @param timeout maximum time to shutdown instance.
     * @param userOutputReceiver the outputReceiver.
     * @throws InstanceOperationException on shutdown failures.
     */
    @Override
    public void shutdownInstance(List<File> profileDirList, final long timeout, TextOutputReceiver userOutputReceiver)
        throws InstanceOperationException {

        final CountDownLatch latch = new CountDownLatch(profileDirList.size());
        List<Future<InstanceShutdownCodeWrapper>> futureList = new ArrayList<>();
        for (File profile : profileDirList) {
            futureList.add(threadPool.submit(new ShutdownTask(profile, latch, timeout)));
        }

        if (timeout == 0) {
            try {
                if (!latch.await(WAIT_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    throw new InstanceOperationException(TIMEOUT_REACHED_MESSAGE, null);
                }
            } catch (InterruptedException e) {
                throw new InstanceOperationException("Interrupted while waiting for shutdown task to finish.", null);
            }
        } else {
            try {
                if (!latch.await(timeout, TimeUnit.SECONDS)) {
                    throw new InstanceOperationException(TIMEOUT_REACHED_MESSAGE, null);
                }
            } catch (InterruptedException e) {
                throw new InstanceOperationException("Interrupted while waiting for shutdown task to finish.", null);
            }
        }
        List<String> exceptionMessageList = new ArrayList<>();
        List<File> failedInstances = new ArrayList<>();
        for (Future<InstanceShutdownCodeWrapper> future : futureList) {
            InstanceShutdownCodeWrapper code = null;
            try {
                code = future.get(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new InstanceOperationException(e, null);
            }
            if (code == null) {
                throw new InstanceOperationException("unexpected failure.", null);
            }
            switch (code.getShutdownCode()) {
            case NON_RUNNING_PROFILE:
                failedInstances.add(code.getProfile());
                exceptionMessageList
                    .add(new String("tried to shutdown non-running instance with instance id: " + code.getProfile().getName()));
                break;
            case FAILED_SHUTDOWN:
                failedInstances.add(code.getProfile());
                exceptionMessageList.add(new String("failed to shutdown instance with instance id: " + code.getProfile().getName()));
                break;
            case INTERRUPTED_WHILE_WAITING:
                failedInstances.add(code.getProfile());
                exceptionMessageList.add(new String("interrupted while waiting for shutdown termination of instance with instance id: "
                    + code.getProfile().getName()));
                break;
            case SUCCESSFUL_SHUTDOWN:
                failedInstances.add(code.getProfile());
                userOutputReceiver.addOutput("Successfully stopped instance " + code.getProfile().getName());
                break;
            default:
                throw new InstanceOperationException("Instance Management: unexpected failure", null);
            }

        }
        if (!exceptionMessageList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String exceptionMessage : exceptionMessageList) {
                sb.append(exceptionMessage);
                sb.append("\n");
            }
            userOutputReceiver.addOutput(DONE);
            throw new InstanceOperationException(sb.toString(), failedInstances);
        }
        userOutputReceiver.addOutput(DONE);
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

    // boolean return value <code>true</code> if locking was successfull, else false.
    private boolean lockIMLockFile(final File profile, long timeout) throws IOException {
        synchronized (profile) {
            File lockfile = new File(profile.getAbsolutePath() + SLASH + INSTANCEMANAGEMENT_LOCK);
            lockfile.createNewFile();
            FileLock lock = null;
            if (!lockfile.isFile()) {
                throw new IOException("Lockfile isn't available.");
            }

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, RW)) {

                lock = randomAccessFile.getChannel().tryLock();
                if (lock == null) {
                    final long timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                    final int maxWaitIterations = 20;
                    while (timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < (-timeout)) {
                        lock = randomAccessFile.getChannel().tryLock();
                        if (lock != null) {
                            return true;
                        }
                        Thread.sleep(maxWaitIterations);
                    }
                } else {
                    // this will not deadlock!!
                    synchronized (profileToLockMap) {
                        profileToLockMap.put(profile, lock);
                    }
                    return true;
                }

            } catch (IOException | InterruptedException e) {
                throw new IOException(UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON + lockfile, e);
            }

            return false;
        }
    }

    @Override
    public void registerInstanceOperationCallbackListener(InstanceOperationCallbackListener callbackListener) {
        synchronized (CALL_BACK_LIST) {
            CALL_BACK_LIST.add(callbackListener);
        }
    }

    @Override
    public void unregisterInstanceOperationCallbackListener(InstanceOperationCallbackListener callbackListener) {
        synchronized (CALL_BACK_LIST) {
            CALL_BACK_LIST.remove(callbackListener);
        }
    }

    @Override
    public void fireCommandFinishEvent(File profile) throws IOException {
        synchronized (profileToLockMap) {
            profileToLockMap.remove(profile);
        }
        synchronized (CALL_BACK_LIST) {
            for (InstanceOperationCallbackListener t : CALL_BACK_LIST) {
                t.onCommandFinish(profile);
            }
        }
    }

}
