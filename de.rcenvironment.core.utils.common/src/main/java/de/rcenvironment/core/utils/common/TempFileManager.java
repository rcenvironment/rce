/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Utilities for creating automatically cleaned-up temporary directories and files. <br/>
 * The major design goals were:
 * <ul>
 * <li>Provide central cleanup of temp files, to avoid temp files getting left behind after crashes or components/tools that do not
 * implement proper cleanup</li>
 * <li>Prevent collisions between multiple RCE instances on acquisition or cleanup</li>
 * <li>Prevent accidential deletes outside of the created temp folders, as far as possible</li>
 * <li>Heap usage and the number of file locks should not increase with the number of acquired temp files and directories, to prevent
 * resource drain in long-running instances</li>
 * <li>(to be continued: specific-filename temp files etc.)</li>
 * </ul>
 * <br/>
 * Basic approach:
 * <ul>
 * <li>TODO</li>
 * </ul>
 * 
 * TODO (p2) add/fix missing authors
 * 
 * @author Robert Mischke
 */
public class TempFileManager {

    /**
     * The placeholder that marks the place of the "random" part in filename patterns.
     */
    public static final String FILENAME_PATTERN_PLACEHOLDER = "*";

    private static final String LOCK_FILE_NAME = "tmpdir.lock";

    private static final int MAX_ROOT_DIR_ANTI_COLLISION_ATTEMPTS = 20;

    private static final int MAX_TEMP_FILE_ANTI_COLLISION_ATTEMPTS = 10;

    // TODO implement explicit cleanup

    private File globalRootDir;

    private File instanceRootDir;

    /**
     * The current directory where {@link #createTempFileWithFixedFilename()} tries to create files; replaced with a new directory on a
     * filename collision.
     */
    private File currentDirectoryForTempFiles;

    /**
     * The lock file to mark a temporary directory as "in use". Note that Java {@link FileLock}s are NOT necessarily "hard" OS file locks;
     * they should be treated as "advisory" (see {@link FileLock} documentation).
     */
    private FileLock instanceRootDirLock;

    private AtomicLong lastInstanceRootDirNumber = new AtomicLong(0);

    private AtomicLong tempFileFromPatternSequenceNumber = new AtomicLong(0);

    private Log log = LogFactory.getLog(TempFileManager.class);

    private final TempFileServiceImpl serviceImplementation;

    private final String instanceDirPrefix;

    /**
     * A shutdown hook to delete the temp directory; mostly relevant for cleaning up after unit tests. Extracted as a nested class to mark
     * it as a shutdown hook for code quality checks.
     *
     * @author Robert Mischke (extracted)
     */
    private final class CleanUpShutdownHook extends Thread {

        @Override
        @TaskDescription("Shutdown hook to delete a root temp directory used by unit tests")
        public void run() {
            try {
                deleteInstanceDirectoryForUnitTest();
            } catch (IOException e) {
                log.info("Failed to delete Instance Directory", e);
            }
        }
    }

    /**
     * Default {@link TempFileService} implementation.
     * 
     * @author Robert Mischke
     */
    protected final class TempFileServiceImpl implements TempFileService {

        @Override
        public File createManagedTempDir() throws IOException {
            return createManagedTempDir(null);
        }

        @Override
        // this method is thread-safe; no synchronization needed
        public File createManagedTempDir(String infoText) throws IOException {
            // generate filename
            String tempDirName = Long.toString(lastInstanceRootDirNumber.incrementAndGet());
            if (infoText != null && infoText.length() != 0) {
                tempDirName = tempDirName + "-" + infoText;
            }
            // create dir and check
            File tempDir = new File(getInstanceRootDir(), tempDirName);
            if (!tempDir.mkdirs()) {
                // throw specific exceptions to track down a case where mkdirs() actually failed (Mantis
                // #6425)
                if (tempDir.isDirectory()) {
                    throw new IOException("Unexpected collision: New temporary directory does already exist: " + tempDir);
                } else if (tempDir.isFile()) {
                    throw new IOException("Unexpected collision: New temporary directory is blocked by a equally-named file: "
                        + tempDir);
                } else {
                    throw new IOException("Failed to create new managed temporary directory "
                        + "(maybe lack of permissions, or the target drive is full?): " + tempDir);
                }
            }
            return tempDir;
        }

        @Override
        public File createTempFileFromPattern(String filenamePattern) throws IOException {
            // validate pattern
            if (filenamePattern == null || filenamePattern.length() == 0) {
                throw new IllegalArgumentException("Filename pattern must not be empty");
            }
            if (!filenamePattern.contains(FILENAME_PATTERN_PLACEHOLDER)) {
                throw new IllegalArgumentException("Filename pattern must contain the placeholder pattern " + FILENAME_PATTERN_PLACEHOLDER);
            }
            // increment the global sequence counter
            String tempPart = Long.toString(tempFileFromPatternSequenceNumber.incrementAndGet());
            // generate filename
            String filename = filenamePattern.replace(FILENAME_PATTERN_PLACEHOLDER, tempPart);
            // delegate
            return createTempFileWithFixedFilename(filename);
        }

        @Override
        public synchronized File createTempFileWithFixedFilename(String filename) throws IOException {
            // catch some basic errors
            if (filename.contains("\\") || filename.contains("/")) {
                throw new IOException("Relative filenames are not allowed in this call");
            }
            // TODO Not all filenames are valid on all platforms, but we cannot simply enforce the check here before we have not checked
            // each call of createTempFileWithFixedFilename, to ensure that our code does not create invalid filenames.
            // CrossPlatformFilenameUtils.throwIOExceptionIfFilenameNotValid(filename);

            // create a managed directory if not done yet or deleted meanwhile
            if (currentDirectoryForTempFiles == null || !currentDirectoryForTempFiles.exists()) {
                currentDirectoryForTempFiles = createManagedTempDir();
            }

            IOException lastException = null;
            File newFile;
            for (int i = 0; i < MAX_TEMP_FILE_ANTI_COLLISION_ATTEMPTS; i++) {
                // try to generate new file
                newFile = new File(currentDirectoryForTempFiles, filename);
                try {
                    if (newFile.createNewFile()) {
                        // success -> leave retry loop
                        return newFile;
                    }
                } catch (IOException e) {
                    lastException = e;
                    log.debug("Collision while trying to create temporary file '" + newFile + "'; retrying, " + (i + 1)
                        + " failed attempt(s) so far");
                }
                // on a filename collision or error, create a new temp directory for the next attempt
                currentDirectoryForTempFiles = createManagedTempDir();
            }
            // max retries reached -> throw exception
            if (lastException != null) {
                throw new IOException("Giving up after " + MAX_TEMP_FILE_ANTI_COLLISION_ATTEMPTS
                    + " attempts to create a temporary file named '" + filename + "'; at least one I/O exception occurred while trying",
                    lastException);
            } else {
                throw new IOException("Giving up after " + MAX_TEMP_FILE_ANTI_COLLISION_ATTEMPTS
                    + " attempts to create a temporary file named '" + filename + "'; no exception occurred");
            }
        }

        @Override
        public File writeInputStreamToTempFile(InputStream is) throws IOException {
            File file = createTempFileFromPattern("stream-to-file-" + FILENAME_PATTERN_PLACEHOLDER);
            FileUtils.copyInputStreamToFile(is, file);
            IOUtils.closeQuietly(is);
            return file;
        }

        @Override
        public void disposeManagedTempDirOrFile(File tempFileOrDir) throws IOException {
            if (instanceRootDir == null) {
                throw new IOException("disposeManagedTempDirOrFile() was called with no instanceRootDir set");
            }
            String givenPath = tempFileOrDir.getCanonicalPath();
            String rootPath = instanceRootDir.getCanonicalPath();
            if (!givenPath.startsWith(rootPath)) {
                throw new IOException(StringUtils
                    .format("Temporary file or directory '%s' does not match "
                        + "the root temp directory '%s' -- ignoring delete request", givenPath, rootPath));
            }

            try {
                if (tempFileOrDir.isDirectory()) {
                    FileUtils.deleteDirectory(tempFileOrDir);
                } else {
                    // TODO react if return value is false?
                    tempFileOrDir.delete();
                }
            } catch (IOException e) {
                throw new IOException("Error deleting temporary file or directory " + givenPath, e);
            }
        }

        protected synchronized File getInstanceRootDir() throws IOException {
            // lazy init
            if (instanceRootDir == null) {
                instanceRootDir = initializeInstanceRootDir();
                File lockFile = new File(instanceRootDir, LOCK_FILE_NAME);
                instanceRootDirLock = attemptLock(lockFile);
                // should never happen, but catch it anyway
                if (instanceRootDirLock == null) {
                    throw new IOException("Failed to acquire lock in new temporary directory: " + lockFile.getAbsolutePath());
                }
                if (log.isDebugEnabled()) {
                    log.debug(StringUtils.format("Initialized top-level managed temp directory %s", instanceRootDir.getAbsolutePath()));
                }
            }
            return instanceRootDir;
        }

        /**
         * Retrieves the configured global root directory. So far, this method is only used for unit testing.
         * 
         * @return the global root directory, as set by {@link #setGlobalRootDir(File)}
         */
        protected synchronized File getGlobalRootDir() {
            return globalRootDir;
        }

    }

    protected TempFileManager(File globalRootDir, String instanceDirPrefix) throws IOException {
        setGlobalRootDir(globalRootDir);
        this.instanceDirPrefix = instanceDirPrefix;
        serviceImplementation = new TempFileServiceImpl();
    }

    protected TempFileManager(File globalRootDir, String instanceDirPrefix, boolean unitTest) throws IOException {
        setGlobalRootDir(globalRootDir);
        this.instanceDirPrefix = instanceDirPrefix;
        serviceImplementation = new TempFileServiceImpl();
        if (unitTest) {
            Runtime.getRuntime().addShutdownHook(new CleanUpShutdownHook());
        }
    }

    /**
     * Performs a "garbage collection" (GC) that deletes subfolders of the "global root directory" (called "instance root directories") that
     * are not locked by any RCE instance.
     * 
     * @throws IOException on I/O errors
     */
    public synchronized void runGCOnGlobalRootDir() throws IOException {
        Collection<File> deleteSet = determineGCDeleteSetForGlobalRootDir();
        for (File childFolderToDelete : deleteSet) {
            // TODO not deleting yet
            log.info("GC: (simulation) deleting unused temp file folder " + childFolderToDelete.getCanonicalPath());
        }
    }

    /**
     * Determines the set of directories that a call to {@link #runGCOnGlobalRootDir()} would delete. Intended for unit testing.
     * 
     * @return the list of directories for deletion
     * @throws IOException on I/O errors
     */
    protected synchronized Collection<File> determineGCDeleteSetForGlobalRootDir() throws IOException {
        // check the reliability of isActualSubfolderOf() to prevent deletion of paths like
        // "rootdir/..". note that this MAY NOT guard against degenerate file system constructs
        // like symlink loops to parent folders - misc_ro
        File dotDotOfGlobalRootDir = new File(globalRootDir, "..");
        if (!isActualSubfolderOf(dotDotOfGlobalRootDir, globalRootDir)) {
            throw new IOException("Unsafe behaviour of File.getCanonicalPath(); "
                + "not running garbage collection");
        }
        // cross-check that the opposite condition is true
        if (isActualSubfolderOf(globalRootDir, dotDotOfGlobalRootDir)) {
            throw new IOException("Internal consistency violation of File.getCanonicalPath(); "
                + "not running garbage collection");
        }
        log.debug("Tested proper detection of folder parent relations");
        List<File> globalRootContent = getActualDirectoryContent(globalRootDir);
        // be extra conservative: check that all children are directories first
        for (File childElement : globalRootContent) {
            if (!childElement.isDirectory()) {
                throw new IOException(
                    "Unexpected state: child element of root folder was not a directory "
                        + "(not running garbage collection): " + childElement.getAbsolutePath());
            }
        }

        List<File> deleteSet = new ArrayList<File>();
        for (File childFolder : globalRootContent) {
            // FIXME broken!
            File lockFile = new File(childFolder, LOCK_FILE_NAME);
            FileLock testLock = attemptLock(lockFile);
            if (testLock != null) {
                deleteSet.add(childFolder);
                releaseLock(testLock);
            } else {
                log.info("GC: identified active temp file folder " + childFolder.getCanonicalPath());
            }
        }
        return deleteSet;
    }

    // TODO add code to track RandomAccessFiles?
    @SuppressWarnings("resource")
    private FileLock attemptLock(File lockFile) throws IOException, FileNotFoundException {
        // create an OS-level file lock
        RandomAccessFile randomAccessFile = new RandomAccessFile(lockFile, "rw");
        FileLock testLock;
        try {
            testLock = randomAccessFile.getChannel().tryLock();
            return testLock;
        } catch (OverlappingFileLockException e) {
            // lock held by same JVM
            randomAccessFile.getChannel().close();
            randomAccessFile.close();
            return null;
        }
    }

    private void releaseLock(FileLock testLock) throws IOException {
        testLock.release();
        testLock.channel().close();
    }

    /**
     * Retrieves the configured global root directory. So far, this method is only used for unit testing.
     * 
     * @return the global root directory, as set by {@link #setGlobalRootDir(File)}
     */
    protected synchronized File getGlobalRootDir() {
        return globalRootDir;
    }

    protected TempFileServiceImpl getServiceImplementation() {
        return serviceImplementation;
    }

    protected static List<File> getActualDirectoryContent(File parentDir) {
        File[] filesInGlobalRoot = parentDir.listFiles();
        List<File> trueEntries = new ArrayList<File>();
        for (File entry : filesInGlobalRoot) {
            String name = entry.getName();
            if (!name.equals(".") && !name.equals("..")) {
                trueEntries.add(entry);
            }
        }
        return trueEntries;
    }

    /**
     * Sets a new directory to use as the root of all managed temp files and folders. All previously generated temp files and folders will
     * be released for cleanup (deletion), so this method should usually be called only once, typically on application startup.
     * 
     * If this method was not called before one of the utility methods is used, a default root is chosen below the "java.io.tmpdir" path.
     * This is undesirable from a cleanup standpoint, so a warning is logged, but this avoids the hassle of defining temp file roots in
     * affected unit tests.
     * 
     * @param newRootDir the new root directory; may already exist
     * @throws IOException when the directory could not be created
     */
    private synchronized void setGlobalRootDir(File newRootDir) throws IOException {
        // if set, release the old lock to allow cleanup
        if (instanceRootDirLock != null) {
            // check if the same directory is already set (for example by a previous unit test)
            if (globalRootDir.getAbsolutePath().equals(newRootDir.getAbsolutePath())) {
                if (log.isTraceEnabled()) {
                    log.trace("New temp root directory is the same as the existing one; ignoring change request ("
                        + newRootDir.getAbsolutePath() + ")");
                }
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("Releasing lock file in directory " + instanceRootDir.getAbsolutePath());
            }
            releaseLock(instanceRootDirLock);
        }
        instanceRootDir = null;
        globalRootDir = newRootDir;
    }

    private File initializeInstanceRootDir() throws IOException {
        if (globalRootDir == null) {
            throw new IllegalStateException("Internal consistency error: initialized without a global root directory");
        }
        String finalPrefix = "";
        if (instanceDirPrefix != null && !instanceDirPrefix.isEmpty()) {
            finalPrefix = instanceDirPrefix + "-";
        }
        String timestamp = Long.toString(System.currentTimeMillis());
        int antiCollisionAttempt = 0;
        String antiCollisionSuffix = "";
        File newInstanceRootDir = null;
        while (antiCollisionAttempt < MAX_ROOT_DIR_ANTI_COLLISION_ATTEMPTS) {
            String instanceDirectoryName = StringUtils.format("%s%s%s", finalPrefix, timestamp, antiCollisionSuffix);
            newInstanceRootDir = new File(globalRootDir, instanceDirectoryName);
            if (!newInstanceRootDir.isDirectory()) {
                // does not exist, try to create
                if (newInstanceRootDir.mkdirs()) {
                    // successfully created (this check is safe against concurrent creation)
                    return newInstanceRootDir;
                }
            }
            antiCollisionAttempt++;
            antiCollisionSuffix = "(" + antiCollisionAttempt + ")";
        }
        if (newInstanceRootDir != null) {
            throw new IOException(StringUtils.format(
                "Failed to create unique instance temp directory after %s attempts; last attempted path was %s",
                MAX_ROOT_DIR_ANTI_COLLISION_ATTEMPTS,
                newInstanceRootDir.getAbsolutePath()));
        } else {
            throw new IOException(StringUtils.format(
                "Failed to create unique instance temp directory after %s attempts; last attempted path was null",
                MAX_ROOT_DIR_ANTI_COLLISION_ATTEMPTS));
        }
    }

    private static boolean isActualSubfolderOf(File expectedInner, File expectedOuter) throws IOException {
        String canonicalExpectedOuter = expectedOuter.getCanonicalPath();
        String canonicalExpectedInner = expectedInner.getCanonicalPath();
        return ((canonicalExpectedInner.length() < canonicalExpectedOuter.length()) && canonicalExpectedOuter
            .startsWith(canonicalExpectedInner));
    }

    /**
     * 
     * Convenient method to delete temp directory created by a unit test. There should only be the lock file left in the folder
     * 
     * @throws IOException on error
     */
    private void deleteInstanceDirectoryForUnitTest() throws IOException {
        if (instanceRootDirLock != null) {
            instanceRootDirLock.release();
            instanceRootDirLock.channel().close();
        }
        if (instanceRootDir != null) {
            List<File> files = (List<File>) FileUtils.listFiles(instanceRootDir, null, true);
            if (files.size() == 1 && files.get(0).getName().equals(LOCK_FILE_NAME)) {
                FileUtils.deleteDirectory(instanceRootDir);
                log.info("Deleted instance directory: " + instanceRootDir.getAbsolutePath());
            } else {
                log.warn("Did not delete temp directory: " + instanceRootDir.getAbsolutePath() + " since it is not empty.");
            }
        }

    }

}
