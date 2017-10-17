/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * This class represents an RCE profile with its representation on the file system.
 *
 * @author Tobias Brieden
 */
public class BaseProfile {

    /**
     * The name of the lock file to signal that the containing profile is in use.
     */
    public static final String PROFILE_DIR_LOCK_FILE_NAME = "instance.lock";

    /** The name of the file containing the profile version. */
    public static final String PROFILE_VERSION_FILE_NAME = "profile.version";

    private static final String PROFILE_INTERNAL_DATA_SUBDIR = "internal";

    private static final String CONFIGURED_PROFILE_TEMPLATE = "The configured profile directory \"%s\"";

    private static final String PROFILE_PATH_POINTS_TO_FILE_TEMPLATE = CONFIGURED_PROFILE_TEMPLATE
        + " points to a file. It must either point to an existing profile directory or must be a path pointing to a not yet existing"
        + " directory. Please check your launch settings.";

    private static final String PROFILE_WITH_CORRUPTED_VERSION_TEMPLATE = CONFIGURED_PROFILE_TEMPLATE
        + " has a corrupted profile version number.";

    private static final String NO_PROFILE_AND_NOT_EMPTY_TEMPLATE = CONFIGURED_PROFILE_TEMPLATE
        + " is neither an existing profile directory nor is it empty.";

    private static final String PROFILE_NOT_ACCESSIBLE_TEMPLATE = CONFIGURED_PROFILE_TEMPLATE
        + " is either not readable and/or not writeable. "
        + " Choose another profile directory. (See the user guide for more information about the profile directory.)";

    private static final String NO_PROFILE_TEMPLATE = CONFIGURED_PROFILE_TEMPLATE
        + " does not contain an existing profile.";

    protected int version;

    private File profileDirectory;

    private File internalDirectory = null;

    private FileLock lock;

    private RandomAccessFile randomAccessLockFile;

    /**
     * Be careful! This constructor is only intended for use within this package since it doesn't call the init method automatically, which
     * is inconvenient for users.
     * 
     * @param profileDirectory
     * @param version
     */
    protected BaseProfile(File profileDirectory, int version) {
        this.profileDirectory = profileDirectory;
        this.version = version;
    }

    public BaseProfile(File profileDirectory, int version, boolean create) throws ProfileException {
        this.profileDirectory = profileDirectory;
        this.version = version;

        init(create, false);
    }

    protected void init(boolean create, boolean allowLegacyProfile) throws ProfileException {

        if (profileDirectory.exists() && !profileDirectory.isDirectory()) {
            throw new ProfileException(StringUtils.format(PROFILE_PATH_POINTS_TO_FILE_TEMPLATE, profileDirectory.getAbsolutePath()));
        }

        internalDirectory = new File(profileDirectory, PROFILE_INTERNAL_DATA_SUBDIR);

        // check if the version file exists and is valid or try to create it
        File versionFile = new File(internalDirectory, PROFILE_VERSION_FILE_NAME);
        if (versionFile.isFile()) {
            try {
                String content = new String(Files.readAllBytes(versionFile.toPath()));
                this.version = Integer.parseInt(content);
            } catch (NumberFormatException | IOException e) {
                throw new ProfileException(StringUtils.format(PROFILE_WITH_CORRUPTED_VERSION_TEMPLATE, profileDirectory.getAbsolutePath()));
            }
        } else {
            
            if (!allowLegacyProfile && profileDirectory.exists()) {
                String[] profileDirectoryContent = profileDirectory.list();
                // only empty existing directories can be used as new profile directories ...
                if (profileDirectoryContent == null || !(profileDirectoryContent.length == 0
                    // or directories that contain a single sub-directory name "startup_logs"
                    || (profileDirectoryContent.length == 1 && profileDirectoryContent[0].equals("startup_logs")))) {
                    throw new ProfileException(StringUtils.format(NO_PROFILE_AND_NOT_EMPTY_TEMPLATE, profileDirectory.getAbsolutePath()));
                }
            }

            if (create) {
                // write the version number to the profile
                try {
                    writeProfileVersionNumberToProfile(versionFile, this.version);
                } catch (IOException e) {
                    throw new ProfileException(StringUtils.format(PROFILE_NOT_ACCESSIBLE_TEMPLATE, profileDirectory.getAbsolutePath()));
                }
            } else {
                // legacy profiles cannot be opened without the "create" option
                throw new ProfileException(StringUtils.format(NO_PROFILE_TEMPLATE, profileDirectory.getAbsolutePath()));
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        // null instanceof Object will always return false
        if (!(other instanceof BaseProfile)) {
            return false;
        }

        try {
            return ((BaseProfile) other).getProfileDirectory().getCanonicalFile().equals(profileDirectory.getCanonicalFile());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        throw new IllegalStateException("Not implemented.");
    }

    public String getName() {
        return profileDirectory.getName();
    }

    public int getVersion() {
        return version;
    }

    public File getProfileDirectory() {
        return profileDirectory;
    }

    public File getInternalDirectory() {
        return internalDirectory;
    }

    private void writeProfileVersionNumberToProfile(File versionFile, int versionNumber) throws IOException {
        // if version file's parent folder does not exist, create it
        if (!versionFile.getParentFile().exists()) {
            versionFile.getParentFile().mkdirs();
            if (!versionFile.getParentFile().isDirectory()) {
                throw new IOException("Failed to initialize internal data directory " + internalDirectory.getAbsolutePath());
            }
        }
        // if file does not exist, create it
        if (!versionFile.exists()) {
            versionFile.createNewFile();
        }
        // don't append but overwrite file's content
        FileWriter fw = new FileWriter(versionFile, false);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(String.valueOf(versionNumber));
        bw.close();
    }

    /**
     * Attempts to acquire an exclusive lock on the given file. Note that this is not an OS-level lock, but only protects against locks made
     * by other JVM applications; see {@link FileChannel#tryLock(long, long, boolean)} for details.
     * 
     * @return true if the lock was acquired, false if the lock is already held by another JVM application
     * @throws ProfileException Either if an IOException occurs while trying to access the lock file or if this method was called twice.
     *         Does not occur on a simple failure to acquire the lock.
     */
    public boolean attemptToLockProfileDirectory() throws ProfileException {
        File lockfile = new File(profileDirectory, PROFILE_DIR_LOCK_FILE_NAME);
        lock = null;
        try {
            // create lock file if it does not exist
            lockfile.createNewFile();
            // try to get a lock on this file
            randomAccessLockFile = new RandomAccessFile(lockfile, "rw");
            lock = randomAccessLockFile.getChannel().tryLock();
        } catch (IOException | OverlappingFileLockException e) {
            throw new ProfileException("Unexpected error when trying to acquire a file lock on " + lockfile);
        }
        // NOTE: It is not necessary to release the lock on the file, this is automatically done
        // by the Java VM or in case of an abnormal end by the operating system
        return lock != null;
    }

    public boolean isLocked() {
        return lock != null && lock.isValid();
    }

    /**
     * By default the lock is released if the VM is shut down.
     * 
     * This method only exists to be used by unit tests.
     * 
     * @throws IOException unexpected
     */
    public void releaseLock() throws IOException {
        if (isLocked()) {
            lock.release();
        }

        if (randomAccessLockFile != null) {
            randomAccessLockFile.close();
        }
    }
}
