/*
 * Copyright (C) 2006-2017 DLR, Germany
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.rcenvironment.core.configuration.bootstrap.profile.BaseProfile;

/**
 * 
 * Utility class for {@link InstanceOperationsImpl}.
 *
 * @author David Scholz
 */
public final class InstanceOperationsUtils {

    /**
     * Timeout reached error message for acquiring lock file.
     */
    public static final String TIMEOUT_REACHED_MESSAGE =
        "Timeout reached while trying to acquire the lock, aborting startup of instance with id: %s.";

    /**
     * Error message if acquiring lock file fails.
     */
    public static final String UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON =
        "Unexpected error when trying to acquire a file lock on ";
    
    /**
     * 
     */
    public static final String IM_LOCK_FILE_ACCESS_PERMISSIONS = "rw";
    
    /**
     * Name of the file, which contains the installation id of a running profile.
     */
    public static final String INSTALLATION_ID_FILE_NAME = "installation";

    private static final String IM_LOCK_FILE_NAME = "instancemanagement.lock";

    
    /**
     * Name of the shutdown.dat file.
     */
    private static final String SHUTDOWN_FILE_NAME = "shutdown.dat";
    
    private static final String SLASH = "/";

    private InstanceOperationsUtils() {

    }

    /**
     * 
     * Tries to acquire a lock on the IM lock file.
     * 
     * @param profile the profile to lock.
     * @param timeout the maximum time trying to acquire the lock.
     * @return <code>true</code> if locking was successfull, else <code>false</code> is returned.
     * @throws IOException on failure.
     */
    public static boolean lockIMLockFile(final File profile, final long timeout) throws IOException {
        File lockfile = new File(profile.getAbsolutePath() + SLASH + IM_LOCK_FILE_NAME);
        lockfile.createNewFile();
        FileLock lock = null;

        if (!lockfile.isFile()) {
            throw new IOException("Lockfile isn't available.");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, IM_LOCK_FILE_ACCESS_PERMISSIONS)) {

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
                return true;
            }

        } catch (InterruptedException e) {
            throw new IOException("Unexpected error when trying to acquire a file lock on ");
        }

        return false;

    }

    /**
     * Tests whether the given profile directory is locked by a running instance.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter.
     * @return true if the directory is locked.
     * @throws IOException on failure.
     */
    public static boolean isProfileLocked(File profileDir) throws IOException {
        if (!profileDir.isDirectory()) {
            throw new IOException("Profile directory " + profileDir.getAbsolutePath() + " can not be created or is not a directory");
        }

        File lockfile = new File(profileDir, BaseProfile.PROFILE_DIR_LOCK_FILE_NAME);
        FileLock lock = null;
        if (!lockfile.isFile()) {
            return false;
        }
        // try to get a lock on this file
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, IM_LOCK_FILE_ACCESS_PERMISSIONS)) {
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
     * 
     * Simple watchdog for the shutdown file.
     * 
     * @param path the path to the location, where the shutdown file should be located.
     * @return <code>true</code> if shutdown file was found, <code>false</code> otherwise.
     * @throws IOException on failure.
     */
    public static boolean detectShutdownFile(final String path) throws IOException {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path shutdownFile = Paths.get(path + SLASH + BaseProfile.PROFILE_INTERNAL_DATA_SUBDIR + "/" + SHUTDOWN_FILE_NAME);
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
                throw new IOException("Shutdown watcher task was interrupted.");
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
     * Deletes the instance.lock file from the profile folder.
     * 
     * @param profileDir the profile directory.
     */
    public static void deleteInstanceLockFromProfileFolder(File profileDir) {
        for (File fileInProfileDir : profileDir.listFiles()) {
            if (fileInProfileDir.isFile()
                && BaseProfile.PROFILE_DIR_LOCK_FILE_NAME.equals(fileInProfileDir.getName())) {
                fileInProfileDir.delete();
                break;
            }
        }
    }

}
