/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
 * @author Jan Flink (version update)
 * @author Tobias Brieden
 */
public class CommonProfile {

    /**
     * We require that an instance of {@link CommonProfile} is backed by a profile folder on the file system. Hence, during instantiation of
     * CommonProfile we have to ensure that the instance is ``in sync'' with the file system. As there are a number of cases to consider as
     * to how to ensure this synchronicity, we encapsulate this code into its own class, the sole purpose of which is to construct a
     * ``valid'' instance of CommonProfile.
     * 
     * Intuitively, the class CommonProfile requires a {@link File} upon instantiation that denotes the directory backing the profile, which
     * we call the profile directory. For the sake of convenience, the {@link Builder} offers the user the possibility to create the profile
     * directory prior to instantiation ({@link #create(boolean)}).
     * 
     * Throughout the documentation of this class we use the terms 'profile directory', 'internal directory', 'version file', and
     * 'functionally empty'. The former terms refer to the directory supplied upon instantiation of the Builder, the directory 'internal' at
     * the root of the profile directory, and the file 'internal/profile.version', respectively. For a definition of 'functionally empty',
     * please refer to the documentation of the method {@link #create(boolean)}.
     * 
     * @author Alexander Weinert
     */
    public static class Builder {

        private static final String COMMON_PROFILE_DIR_NAME = "common";

        private final File profileDirectory;

        private boolean create = false;

        private boolean migrate = false;

        public Builder(final File profileDirectoryParam) {
            profileDirectory = profileDirectoryParam;
        }

        /**
         * Initializing a profile directory amounts to creating directories and files in the profile directory such that it is recognized as
         * an empty profile of the current version. Currently, this requires the creation of a directory called 'internal', as well as a
         * file 'internal/profile.version' that contains the single character '2', denoting profile version 2 (as of RCE 9.0). This will
         * most likely change in future versions of RCE.
         * 
         * Moreover, setting the create parameter to true will also cause the Builder to initialize so-called 'functionally empty' profile
         * directories. We say that a profile directory is functionally empty if it exists and if it is either 1) empty, 2) contains a
         * single folder called 'startup_logs', or 3) contains three files called 'instance.lock', 'shutdown-debug.log', and
         * 'shutdown-warnings.log'. Such profile directories are created if the startup of RCE fails at some point and is aborted.
         * 
         * @param createParam Set to true if the given profile directory shall be created and initialized as a profile directory if it is
         *        non-existent or functionally empty. Set to false by default.
         * @return This object for daisy-chaining.
         */
        public Builder create(boolean createParam) {
            create = createParam;
            return this;
        }

        /**
         * In practice, silent upgrades should be performed quite rarely during construction of a profile. Instead, the profile should first
         * be created without migration, and should only be upgraded after obtaining explicit confirmation by the user. Since we do not
         * currently support downgrading profiles, silent upgrades could leave the user with an upgraded profile after only trying out a new
         * version of RCE with no clear way to revert to an earlier version.
         * 
         * @param migrateParam Set to true if, in case the given profile directory contains a profile with a non-current version, the
         *        profile shall be silently upgraded to the most current version.
         * @return This object for daisy-chaining.
         */
        public Builder migrate(boolean migrateParam) {
            migrate = migrateParam;
            return this;
        }

        /**
         * Construct an instance of CommonProfile that is backed by the directory given upon creation of this Builder as configured by the
         * calls to {@link #create(boolean)} and {@link #migrate(boolean)}. If the profile directory given upon creation of the
         * {@link Builder} is called "common", a {@link CommonProfile} is returned, otherwise its subclass {@link Profile} is returned.
         * 
         * @return An instantiated instance of CommonProfile. Is never null.
         * @throws ProfileException If the instantiation of CommonProfile failed. See, among others, {@link #create(boolean)} and
         *         {@link #migrate(boolean)} for more information on the possible errors.
         */
        public CommonProfile build() throws ProfileException {
            processBuildOptions();

            if (profileDirectory.getName().equals(COMMON_PROFILE_DIR_NAME)) {
                return constructCommonProfile();
            } else {
                return constructUserProfile();
            }
        }

        /**
         * Construct an instance of CommonProfile that is backed by the directory given upon creation of this Builder as configured by the
         * calls to {@link #create(boolean)} and {@link #migrate(boolean)}.
         * 
         * @return An instantiated instance of CommonProfile. Is never null.
         * @throws ProfileException If the instantiation of CommonProfile failed. See, among others, {@link #create(boolean)} and
         *         {@link #migrate(boolean)} for more information on the possible errors. In particular, the profile directory supplied upon
         *         creation of this Builder must be called "common" in order for construction of CommonProfile to succeed.
         */
        public CommonProfile buildCommonProfile() throws ProfileException {
            processBuildOptions();

            if (!profileDirectory.getName().equals(COMMON_PROFILE_DIR_NAME)) {
                throw new ProfileException(String.format(
                    "Cannot construct a \"common\" profile using the backing directory \"%s\": "
                        + "Profile directory containing the \"common\" profile must be called \"common\".",
                    profileDirectory.getAbsolutePath()));
            }

            return constructCommonProfile();
        }

        /**
         * Construct an instance of Profile that is backed by the directory given upon creation of this Builder as configured by the calls
         * to {@link #create(boolean)} and {@link #migrate(boolean)}. *
         * 
         * @return An instantiated instance of Profile. Is never null.
         * @throws ProfileException If the instantiation of Profile failed. See, among others, {@link #create(boolean)} and
         *         {@link #migrate(boolean)} for more information on the possible errors. In particular, the profile directory supplied upon
         *         creation of this Builder must not be called "common" in order for construction of Profile to succeed.
         */
        public Profile buildUserProfile() throws ProfileException {
            processBuildOptions();

            if (profileDirectory.getName().equals(COMMON_PROFILE_DIR_NAME)) {
                throw new ProfileException(String.format(
                    "Cannot construct a user profile using the backing directory \"%s\": "
                        + "Profile directory containing user profiles must not be called \"common\"."
                        + "The profile \"common\" is reserved for cross-profile settings.",
                    profileDirectory.getAbsolutePath()));
            }

            return constructUserProfile();
        }

        /**
         * 
         * @throws ProfileException
         */
        private void processBuildOptions() throws ProfileException {
            ensureProfileDirectoryExists();
            ensureProfileDirectoryIsInitialized();

            // At this point we are certain that the profile directory exists and that it has been initialized if it was functionally empty,
            // either due to just having been created, or due to already existing prior to the creation of this instance. However, it may be
            // the case that the profile directory was not previously functionally empty, hence we have to check whether it is a valid
            // profile directory at this point. If this is the case, then we can subsequently assume the internal directory to exist and, if
            // it contains a version file, we can assume that version file to contain a valid profile version.
            if (!isValidProfileDirectory()) {
                throw new ProfileException(StringUtils.format("The profile at \"%s\" is corrupted or of a future profile version. \n"
                    + " Note: It is not possible to use current profiles for older versions of RCE.",
                    profileDirectory.getAbsolutePath()));
            }

            // As of now (RCE 9.0 being the next release), migration of profiles is rather straightforward and accomplished by creating a
            // version-file that contains the most recent version number. Moreover, the profile versions of the common- and user-profiles
            // have increased in unison, hence we are able to implement a very straightforward approach to migration at this point. This
            // will very probably have to be changed in the future
            if (migrate && (isLegacyProfile() || isProfileVersion1())) {
                try (FileWriter fw = new FileWriter(getVersionFile(), false); BufferedWriter bw = new BufferedWriter(fw)) {
                    // We hardcode the current version number here intentionally to highlight the fact that this migration mechanism is not
                    // a general solution and must definitely be reconsidered once we increase the profile version again
                    bw.write(String.valueOf(2));
                } catch (IOException e) {
                    final String errorMessage =
                        String.format("Could not upgrade the profile \"%s\" at \"%s\" to the most current profile version.",
                            profileDirectory.getName(), profileDirectory.getAbsolutePath());
                    throw new ProfileException(errorMessage, e);
                }
            }
        }

        private CommonProfile constructCommonProfile() throws ProfileException {
            if (isValidProfileDirectory()) {
                return new CommonProfile(profileDirectory);
            } else {
                throw new ProfileException(
                    String.format("Profile at \"%s\" has unknown version number.", profileDirectory.getAbsolutePath()));
            }
        }

        private Profile constructUserProfile() throws ProfileException {
            if (isValidProfileDirectory()) {
                return new Profile(profileDirectory);
            } else {
                throw new ProfileException(
                    String.format("Profile at \"%s\" has unknown version number.", profileDirectory.getAbsolutePath()));
            }
        }

        /**
         * As a precondition, the profile directory must exist. If the profile is functionally empty, the internal directory and the version
         * file are created, where the most current profile version is written to the version file.
         * 
         * @throws ProfileException Thrown if the profile directory was functionally empty, but either the internal directory or the version
         *         file could not be created, either due to the user not asking for creation of these objects, or due to an error during
         *         their creation.
         */
        private void ensureProfileDirectoryIsInitialized() throws ProfileException {
            if (profileDirectoryIsFunctionallyEmpty()) {
                if (create) {
                    try {
                        initializeProfileDirectory();
                    } catch (IOException e) {
                        throw new ProfileException(
                            String.format("Could not initialize profile directory for profile \"%s\"", profileDirectory.getName()), e);
                    }
                } else {
                    throw new ProfileException(
                        String.format("Profile directory \"%s\" is functionally empty, but user did not request creation of the profile",
                            profileDirectory.getName()));
                }
            }
        }

        /**
         * If the given profile directory already exists, this method does nothing. Otherwise, it tries to create if, it the user set create
         * to true during the creation of the builder object. Upon ordinary return from this method, the profile directory is guaranteed to
         * exist, although it is not guaranteed to be empty.
         * 
         * @throws ProfileException If the profile directory does not exist and could not be created, either due to the user not asking for
         *         nonexistent profiles to be created, or due to an IOException occurring during the attempted creation.
         */
        private void ensureProfileDirectoryExists() throws ProfileException {
            if (!profileDirectory.exists()) {
                if (create) {
                    try {
                        createProfileDirectory();
                    } catch (IOException e) {
                        throw new ProfileException(
                            String.format(
                                "Could not create profile directory for profile \"%s\". Please ensure that the parent directory of "
                                    + "the specified profile directory exists and that you have writing access to that location.",
                                profileDirectory.getName()),
                            e);
                    }
                } else {
                    throw new ProfileException(
                        String.format("Profile directory \"%s\" does not exist, but user did not request creation of the profile",
                            profileDirectory.getName()));
                }
            } else if (profileDirectory.isFile()) {
                throw new ProfileException(
                    String.format("Cannot use the path \"%s\" as a profile location, since it points to a file. "
                        + "Profile path must point to a directory.", profileDirectory.getAbsolutePath()));
            }
        }

        /**
         * Intuitively, we call a profile directory 'functionally empty' if it is empty in the sense that it does not yet contain a profile,
         * but it can be used as a directory backing a profile without erasing already existent content.
         * 
         * @return True if the chosen profile directory is functionally empty.
         * @throws SecurityException If a {@link SecurityManager} prevents this method from determining the contents of the chosen profile
         *         directory.
         * @throws ProfileException If the contents of the chosen profile directory cannot be determined for some other reason.
         */
        private boolean profileDirectoryIsFunctionallyEmpty() throws ProfileException {
            if (!profileDirectory.exists()) {
                return false;
            }

            if (!profileDirectory.isDirectory()) {
                return false;
            }

            final String[] profileDirectoryContents = profileDirectory.list();
            if (profileDirectoryContents == null) {
                throw new ProfileException(
                    String.format("Could not determine contents of profile directory %s", profileDirectory.getName()));
            }

            if (profileDirectoryContents.length == 0) {
                // Profile directory is empty
                return true;
            } else if (profileDirectoryContents.length == 1) {
                final File singleElement = profileDirectory.toPath().resolve(profileDirectoryContents[0]).toFile();
                final boolean singleElementIsStartupLogsDirectory =
                    singleElement.isDirectory() && singleElement.getName().equals("startup_logs");
                return singleElementIsStartupLogsDirectory;
            } else if (profileDirectoryContents.length == 3) {
                final File oldLockFile = new File(profileDirectory, PROFILE_DIR_LOCK_FILE_NAME);
                if (!oldLockFile.isFile()) {
                    return false;
                }

                final File debugLog = new File(profileDirectory, "shutdown-debug.log");
                if (!debugLog.isFile()) {
                    return false;
                }

                final File warningsLog = new File(profileDirectory, "shutdown-warnings.log");
                if (!warningsLog.isFile()) {
                    return false;
                }

                return true;
            } else {
                // Profile directory contains more than one file system object, hence it is not functionally empty by definition
                return false;
            }

        }

        /**
         * A profile directory is valid if it exists, it contains a directory called 'internal', and that directory either 1) does not
         * contain a file called 'profile.version', 2) contains a file called 'profile.version' containing the single character '1', or 3)
         * it contains a file called 'profile.version' containing the single character '2'.
         * 
         * If a profile directory is valid due to the first condition, we call it a Legacy profile. If a profile is valid due to the second
         * or third condition, we call it a profile of version 1 or version 2, respectively.
         * 
         * @return True if the profile directory contains a valid profile, false otherwise.
         * @throws ProfileException
         */
        private boolean isValidProfileDirectory() {
            if (!(profileDirectory.exists() && profileDirectory.isDirectory())) {
                return false;
            }

            final File internalDirectory = getInternalDirectory();
            if (!internalDirectory.exists()) {
                // Very early versions of the profile directory did not include an internal directory. This includes, e.g., the profiles
                // that are included in the RCE development environment.
                return true;
            }

            if (!internalDirectory.isDirectory()) {
                return false;
            }

            final File versionFile = getVersionFile();
            if (!versionFile.exists()) {
                return true;
            }

            final int versionFileContentNumber;
            try {
                versionFileContentNumber = tryReadAndParseVersionNumber(versionFile);
            } catch (IOException | NumberFormatException e) {
                // IOException: Since, in order to be valid, a profile directory must either not contain the 'internal' directory, or must
                // contain it together with a valid profile.version file, a profile containing an unreadable versionFile is invalid
                // NumberFormatException: Due to the same reasoning as above, a profile that contains a profile.version file whose content
                // cannot be parsed to an integer is invalid.
                return false;
            }

            // In order to be upwards-compatible, we may not only check for known profile versions, i.e., version 1 and 2, but we must allow
            // all future version numbers
            return 1 <= versionFileContentNumber && versionFileContentNumber <= 2;
        }

        /**
         * This method guarantees to return the parsed contents of the file 'internal/profile.version'. All error cases are reported via
         * exceptions.
         * 
         * @param versionFile The file containing the version number, i.e., 'internal/profile.version'.
         * @return The integer written to 'internal/profile.version'
         * @throws IOException If the given version file could not be read, e.g., because it does not exist, because its parent directory
         *         does not exist, or because the file is, in fact, a directory. Since this exception is thrown both if the directory
         *         'internal' does not exist and if the file 'profile.version' does not exist, it does not allow to conclude that the
         *         profile is corrupt or a Legacy profile.
         * @throws NumberFormatException If the content of the given versionFile could be read, but it could not be parsed to an integer. By
         *         definition, this implies that the profile is corrupted.
         */
        private int tryReadAndParseVersionNumber(File versionFile) throws IOException {
            final String versionFileContent = new String(Files.readAllBytes(versionFile.toPath()));
            // Integer#parseInt requires a string representing a number without any whitespace, otherwise an exception is thrown. On some
            // systems, Files#readAllBytes trims a newline at the end of a file automatically, while on others, it does not. Hence, in order
            // to be able to parse the version file in as many scenarios as possible, we trim the obtained string ourselves.
            return Integer.parseInt(versionFileContent.trim());
        }

        /**
         * We say that a profile is a Legacy profile if it 1) does not contain a directory called 'internal', or if it 2) contains such a
         * directory, but that directory does not contain a file called 'profile.version'.
         * 
         * @return True if the given profile directory contains a Legacy profile, false otherwise.
         */
        private boolean isLegacyProfile() {
            if (!profileDirectory.isDirectory()) {
                return false;
            }

            final File internalDirectory = getInternalDirectory();
            if (!internalDirectory.exists()) {
                return true;
            }

            if (!internalDirectory.isDirectory()) {
                return false;
            }

            final File profileVersionFile = getVersionFile();
            return !profileVersionFile.exists();
        }

        /**
         * We say that a profile has version 1 if it contains a directory called 'internal', if that directory contains a file called
         * 'profile.version', and if that file contains the single character '1'.
         * 
         * @return True if the given directory contains a profile of version 1, false otherwise.
         */
        private boolean isProfileVersion1() {
            if (!profileDirectory.isDirectory()) {
                return false;
            }

            final File internalDirectory = profileDirectory.toPath().resolve(PROFILE_INTERNAL_DATA_SUBDIR).toFile();
            if (!internalDirectory.isDirectory()) {
                return false;
            }

            final File versionFile = internalDirectory.toPath().resolve(PROFILE_VERSION_FILE_NAME).toFile();
            if (!versionFile.isFile()) {
                return false;
            }

            final int versionFileContentNumber;
            try {
                versionFileContentNumber = tryReadAndParseVersionNumber(versionFile);
            } catch (IOException | NumberFormatException e) {
                // IOException: We have determined above that the version file indeed exists. Hence, being unable to read its contents at
                // this point implies that the profile is corrupted. Since a corrupted profile has no version, it has, in particular, not
                // version 2, hence we return false.
                // NumberFormatException: Due to the same reasoning as above, a profile that contains a profile.version file whose content
                // cannot be parsed to an integer is invalid and thus has no version number.
                return false;
            }

            return versionFileContentNumber == 1;
        }

        private boolean isProfileVersion2() {
            if (!profileDirectory.isDirectory()) {
                return false;
            }

            final File internalDirectory = getInternalDirectory();
            if (!internalDirectory.isDirectory()) {
                return false;
            }

            final File versionFile = getVersionFile();
            if (!versionFile.isFile()) {
                return false;
            }

            final int versionFileContentNumber;
            try {
                versionFileContentNumber = tryReadAndParseVersionNumber(versionFile);
            } catch (IOException | NumberFormatException e) {
                // IOException: We have determined above that the version file indeed exists. Hence, being unable to read its contents at
                // this point implies that the profile is corrupted. Since a corrupted profile has no version, it has, in particular, not
                // version 2, hence we return false.
                // NumberFormatException: Due to the same reasoning as above, a profile that contains a
                // profile.version file whose content cannot be parsed to an integer is invalid and thus has no version number.
                return false;
            }

            return versionFileContentNumber == 2;
        }

        /**
         * As a precondition, the given profileDirectory must not yet exist. This method creates the profile directory as well as a folder
         * called 'internal' in that directory, and a file called 'profile.version' containing the most current version number.
         * 
         * @throws IOException If the creation of the directory fails.
         */
        private void createProfileDirectory() throws IOException {
            if (!profileDirectory.mkdir()) {
                throw new IOException(
                    String.format("Could not create profile directory for profile \"%s\": File.mkdir() returned false for path %s.",
                        profileDirectory.getName(), profileDirectory.getAbsolutePath()));
            }
        }

        /**
         * As a precondition, the given profileDirectory must exist. This method creates a directory called 'internal' in that directory,
         * and a file called 'profile.version' containing the most current version number.
         * 
         * @throws IOException If the creation of the directory or of the version file fails.
         */
        private void initializeProfileDirectory() throws IOException {
            final File internalDirectory = getInternalDirectory();
            if (!internalDirectory.mkdir()) {
                throw new IOException(
                    String.format("Could not create profile directory for profile \"%s\": File.mkdir() returned false for path %s.",
                        profileDirectory.getName(), internalDirectory.getAbsolutePath()));
            }

            final File versionFile = getVersionFile();
            if (!versionFile.createNewFile()) {
                throw new IOException(
                    String.format("Could not create version file for profile \"%s\": File.createNewFile() returned false for path %s.",
                        profileDirectory.getName(), versionFile.getAbsolutePath()));
            }

            try (FileWriter writer = new FileWriter(versionFile)) {
                writer.write(String.valueOf(Profile.PROFILE_VERSION_NUMBER));
            } catch (IOException e) {
                throw new IOException(String.format("Could not write version number to file \"profile.version\" for profile \"%s\".",
                    profileDirectory.getName()), e);
            }
        }

        /**
         * @return A {@link File} denoting the 'internal' subdirectory of the current profile. Does not guarantee that this {@link File}
         *         indeed exists, or that it is, in fact, a directory. Is never null.
         */
        private File getInternalDirectory() {
            return profileDirectory.toPath().resolve(PROFILE_INTERNAL_DATA_SUBDIR).toFile();
        }

        /**
         * @return A {@link File} denoting the file 'internal/profile.version'. Does not guarantee that this {@link File} indeed exists, or
         *         that it is, in fact, a file. Is never null.
         */
        private File getVersionFile() {
            return getInternalDirectory().toPath().resolve(PROFILE_VERSION_FILE_NAME).toFile();
        }
    }

    /**
     * The current profile's version number. Needs to be updated manually whenever changed in the profile require an update.
     */
    public static final Integer PROFILE_VERSION_NUMBER = 2;

    /**
     * The name of the lock file to signal that the containing profile is in use.
     */
    public static final String PROFILE_DIR_LOCK_FILE_NAME = "instance.lock";

    /** The name of the file containing the profile version. */
    public static final String PROFILE_VERSION_FILE_NAME = "profile.version";

    /**
     * The name of the internal data directory.
     */
    public static final String PROFILE_INTERNAL_DATA_SUBDIR = "internal";

    private File profileDirectory;

    private FileLock lock;

    private RandomAccessFile randomAccessLockFile;

    /**
     * Be careful! This constructor is only intended for use within this package since it doesn't call the init method automatically, which
     * is inconvenient for users.
     * 
     * @param profileDirectory
     * @param version
     */
    protected CommonProfile(File profileDirectory) {
        this.profileDirectory = profileDirectory;
    }

    protected File getVersionFile() {
        return getInternalDirectory().toPath().resolve(PROFILE_VERSION_FILE_NAME).toFile();
    }

    @Override
    public boolean equals(Object other) {
        // null instanceof Object will always return false
        if (!(other instanceof CommonProfile)) {
            return false;
        }

        try {
            return ((CommonProfile) other).getProfileDirectory().getCanonicalFile().equals(getProfileDirectory().getCanonicalFile());
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

    /**
     * @return The profile version. Is in the range {0,1,2}, where 0 denotes a legacy profile.
     * @throws ProfileException If the profile version cannot be determined, e.g., due to a corrupted version file.
     */
    public int getVersion() throws ProfileException {
        final File versionFile = getVersionFile();
        if (!versionFile.exists()) {
            return 0;
        }

        final int versionFileContentNumber;
        try {
            final String versionFileContent = new String(Files.readAllBytes(versionFile.toPath()));
            // Integer#parseInt requires a string representing a number without any whitespace, otherwise an exception is thrown. On some
            // systems, Files#readAllBytes trims a newline at the end of a file automatically, while on others, it does not. Hence, in order
            // to be able to parse the version file in as many scenarios as possible, we trim the obtained string ourselves.
            versionFileContentNumber = Integer.parseInt(versionFileContent.trim());
        } catch (IOException | NumberFormatException e) {
            throw new ProfileException(String.format("Could not read profile version of profile \"%s\".", getName()), e);
        }

        // In order to be upwards-compatible, we may not only check for known profile versions, i.e., version 1 and 2, but we must allow
        // all future version numbers
        if (1 <= versionFileContentNumber && versionFileContentNumber <= 2) {
            return versionFileContentNumber;
        } else {
            throw new ProfileException(String.format("Version file of profile \"%s\" contained invalid profile version: %d.", getName(),
                versionFileContentNumber));
        }
    }

    public File getProfileDirectory() {
        return profileDirectory;
    }

    public File getInternalDirectory() {
        return getProfileDirectory().toPath().resolve(PROFILE_INTERNAL_DATA_SUBDIR).toFile();
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

    /**
     * @return True if the version of this profile equals current profile version.
     * @throws ProfileException Thrown if the current profile number cannot be determined.
     */
    public boolean hasCurrentVersion() throws ProfileException {
        return getVersion() == PROFILE_VERSION_NUMBER;
    }

    /**
     * @return True if the version of this profile is not current, but can be upgraded to the current one.
     * @throws ProfileException Thrown if the current profile number cannot be determined.
     */
    public boolean hasUpgradeableVersion() throws ProfileException {
        /*
         * We currently only have profile versions 1 and 2 and we know how to upgrade from the former to the latter. Once we have to deal
         * with multiple profile versions, we have to check whether we can indeed determine an upgrade path, but for now, this simple check
         * suffices.
         */
        return getVersion() < PROFILE_VERSION_NUMBER;
    }

    /**
     * Upgrades the representation of this profile on the file system such that the profile is of the current version.
     * 
     * @throws IOException Thrown if there is an error during the upgrade of the backing directory.
     */
    public void upgradeToCurrentVersion() throws IOException {
        // Currently, it suffices to write the current version number to the version file in order to implement an upgrade. This may change
        // in future versions!
        final File versionFile = getVersionFile();

        if (!versionFile.exists()) {
            /*
             * SonarCube complains about the following line and suggests merging the two nested if-statements. Doing so would, however,
             * decrease the readability in my opinion, as it would merge a query (`versionFile.exists()`) and a command
             * (`ensureInternalDirectoryExists() && versionFile.createNewFile()`) in a single if-condition. Hence, we split the query and
             * the command and disable SonarCube on the following line.
             */
            if (!(ensureInternalDirectoryExists() && versionFile.createNewFile())) { // NOSONAR
                throw new IOException(String.format("Could not create version file for profile \"%s\"", getProfileDirectory().getName()));
            }
        }

        try (FileWriter fw = new FileWriter(versionFile)) {
            fw.write(String.valueOf(PROFILE_VERSION_NUMBER));
        }
    }

    private boolean ensureInternalDirectoryExists() {
        return getInternalDirectory().isDirectory() || getInternalDirectory().mkdirs();
    }
}
