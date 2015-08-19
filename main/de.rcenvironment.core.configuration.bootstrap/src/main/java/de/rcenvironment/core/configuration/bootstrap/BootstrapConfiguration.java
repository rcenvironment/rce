/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import de.rcenvironment.core.configuration.bootstrap.internal.LaunchParameters;

/**
 * Helper class that chooses the profile directory and related paths to use, based on command-line or .ini file parameters.
 * 
 * @author Robert Mischke
 */
public final class BootstrapConfiguration {

    /**
     * System property for exit code 1 on locked profile.
     */
    public static final String DRCE_LAUNCH_EXIT_ON_LOCKED_PROFILE = "rce.launch.exitOnLockedProfile";

    /**
     * A system property that can be used to override the parent directory for profiles defined by a relative path or id (default "~/.rce").
     */
    public static final String SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE = "rce.profiles.parentDir";

    /**
     * A system property that can be used to override the default profile id/path (default id: "default"). It is only applied if no "-p"
     * launch parameter is set, and can be either a relative or absolute path.
     */
    public static final String SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH = "rce.profile.default";

    /**
     * The relative path where the shutdown information (*not* the temporary shutdown *profile*!) of a profile is stored. Made public to
     * allow sending shutdown signals to external instances.
     */
    public static final String PROFILE_SHUTDOWN_DATA_SUBDIR = "internal";

    /**
     * The name of the lock file to signal that the containing profile is in use.
     */
    public static final String PROFILE_DIR_LOCK_FILE_NAME = "instance.lock";

    private static final String SYSTEM_PROPERTY_USER_HOME = "user.home";

    private static final String SYSTEM_PROPERTY_SYSTEM_TEMP_DIR = "java.io.tmpdir";

    /**
     * Standard OSGi "osgi.configuration.area" property.
     * 
     * This is the directory where OSGi stores all runtime data which is not stored inside the workspace (the "instance area" in OSGi
     * terms).
     */
    private static final String SYSTEM_PROPERTY_OSGI_CONFIGURATION_AREA = "osgi.configuration.area";

    private static final String PROFILE_INTERNAL_DATA_SUBDIR = "internal";

    private static final String PROFILE_RELATIVE_OSGI_STORAGE_PATH = PROFILE_INTERNAL_DATA_SUBDIR + "/osgi";

    private static final String PROFILE_LOGFILES_PATH_PROPERTY = "profile.logfiles.path"; // set by this class

    private static final String PROFILE_LOGFILES_PREFIX_PROPERTY = "profile.logfiles.prefix"; // set by this class

    // note: not using the singleton pattern so it can be reset by unit tests - misc_ro
    private static volatile BootstrapConfiguration instance;

    private final File originalProfileDirectory;

    private final boolean acquiredIntendedProfileDirectory;

    private final File finalProfileDirectory;

    private final File internalDataDirectory;

    private final String finalProfileDirectoryPath;

    // the temporary/stub profile location for the process sending the shutdown signal
    private final File shutdownProfileDirectory;

    private final File shutdownDataDirectory;

    private final boolean shutdownRequested;

    private final File profilesRootDirectory;

    private final boolean fallbackProfileDisabled;

    /**
     * Performs the bootstrap profile initialization.
     * 
     * @throws IOException on bootstrap profile path errors
     */
    private BootstrapConfiguration() throws IOException {

        PrintStream stdOut = System.out;
        PrintStream stdErr = System.err;

        final LaunchParameters launchParams = new LaunchParameters();

        profilesRootDirectory = determineProfilesParentDirectory();

        originalProfileDirectory = determineOriginalProfileDir(launchParams);
        File preliminaryProfileDir = originalProfileDirectory;

        shutdownRequested = launchParams.containsToken("--shutdown");

        // For headless mode, fallback profile is automatically disabled.
        fallbackProfileDisabled =
            System.getProperties().containsKey(DRCE_LAUNCH_EXIT_ON_LOCKED_PROFILE) || launchParams.containsToken("--headless")
                || launchParams.containsToken("--batch");

        // FIXME 6.0.0 - in case of a profile dir collision, the fallback profile will still use this for shutdown data - misc_ro
        shutdownProfileDirectory = new File(originalProfileDirectory, PROFILE_INTERNAL_DATA_SUBDIR + "/shutdown");

        // determine where shutdown data is stored
        shutdownDataDirectory = new File(originalProfileDirectory, PROFILE_INTERNAL_DATA_SUBDIR);

        String introText;

        if (shutdownRequested) {
            // if used as a shutdown trigger, use the shutdown data sub-directory as profile directory
            preliminaryProfileDir = shutdownProfileDirectory;
            introText = "Using shutdown profile directory";
        }

        acquiredIntendedProfileDirectory = attemptToLockProfileDirectory(preliminaryProfileDir);
        if (acquiredIntendedProfileDirectory) {
            finalProfileDirectory = preliminaryProfileDir;
            introText = "Using profile directory";
        } else {
            stdErr.println("Failed to lock profile directory " + preliminaryProfileDir
                + " - most likely, another instance is already using it");
            // If the "--disable-profile-fallback" option is set, shut down, else try to create a fallback profile directory
            if (fallbackProfileDisabled) {
                stdErr.println("Fallback profile is disabled, shutting down.");
                System.exit(1);
            }
            preliminaryProfileDir = determineFallbackProfileDirectory(originalProfileDirectory);
            if (attemptToLockProfileDirectory(preliminaryProfileDir)) {
                finalProfileDirectory = preliminaryProfileDir;
                introText = "Using fallback profile directory";
            } else {
                throw new IOException("Could not acquire a lock on the fallback profile directory " + preliminaryProfileDir
                    + " either - giving up");
            }
        }

        finalProfileDirectoryPath = finalProfileDirectory.getAbsolutePath();

        internalDataDirectory = new File(finalProfileDirectory, PROFILE_INTERNAL_DATA_SUBDIR);
        internalDataDirectory.mkdirs();
        if (!internalDataDirectory.isDirectory()) {
            throw new IOException("Failed to initialize internal data directory " + internalDataDirectory.getAbsolutePath());
        }

        // circumvent CheckStyle rule to generate basic output before the log system is initialized

        stdOut.println(String.format("%s %s (use -p/--profile <id or path> to override)", introText, finalProfileDirectoryPath));

        setLoggingParameters();

        // TODO/NOTE: this does not take full effect; apparently, the setting has already been read and applied
//         setOsgiStorageLocation();
    }

    /**
     * Initializes the singleton instance from system properties and launch parameters.
     * 
     * @throws IOException on bootstrap profile path errors
     */
    public static void initialize() throws IOException {
        instance = new BootstrapConfiguration();
    }

    /**
     * @return the singleton instance
     */
    public static BootstrapConfiguration getInstance() {
        return instance;
    }

    public File getProfileDirectory() {
        return finalProfileDirectory;
    }

    /**
     * Low-level access to the storage path for internal data. This method is intended for classes that need to remain independent of the
     * configuration service. Other classes should fetch the path from there.
     * 
     * @return the location for internal data files; default: "<profile dir>/internal"
     */
    public File getInternalDataDirectory() {
        return internalDataDirectory;
    }

    public File getOriginalProfileDirectory() {
        return originalProfileDirectory;
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    public File getShutdownDataDirectory() {
        return shutdownDataDirectory;
    }

    public boolean isUsingIntendedProfileDirectory() {
        return acquiredIntendedProfileDirectory;
    }

    public File getProfilesRootDirectory() {
        return profilesRootDirectory;
    }

    /**
     * Ensures that the current profiles root directory exists and is a directory.
     * 
     * @throws IOException if the conditions are not met
     */
    public void initializeProfilesRootDirectory() throws IOException {
        profilesRootDirectory.mkdirs();
        if (!profilesRootDirectory.isDirectory()) {
            throw new IOException(String.format(
                "Failed to create the default default profile root directory \"%s\"", profilesRootDirectory.getAbsolutePath()));
        }
    }

    private File determineProfilesParentDirectory() throws IOException {
        String parentPathOverride = System.getProperty(SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE);
        File profilesRootDir;
        if (parentPathOverride != null) {
            profilesRootDir = new File(parentPathOverride);
            if (!profilesRootDir.isDirectory()) {
                throw new IOException(String.format(
                    "The configured profile parent directory \"%s\" does not exist; please check your launch settings",
                    profilesRootDir.getAbsolutePath()));
            }
        } else {
            String userHome = System.getProperty(SYSTEM_PROPERTY_USER_HOME);
            profilesRootDir = new File(userHome, ".rce").getAbsoluteFile();
            // do not create yet; the specified profile directory may be absolute - misc_ro
        }
        return profilesRootDir;
    }

    private File determineOriginalProfileDir(LaunchParameters launchParams) throws IOException {

        String profilePathShortOption = launchParams.getNamedParameter("-p");
        String profilePathLongOption = launchParams.getNamedParameter("--profile");

        String profilePath;
        if (profilePathShortOption != null) {
            profilePath = profilePathShortOption;
            // sanity check: forbid "rce -p path1 --profile path2"
            if (profilePathLongOption != null) {
                // TODO use more appropriate exception type?
                throw new IOException("Invalid combination of command-line parameters: cannot specify -p and --profile at the same time");
            }
        } else {
            profilePath = profilePathLongOption; // can still be null if none of the options is used
        }

        if (profilePath == null) {
            String explicitDefault = System.getProperty(SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH);
            if (explicitDefault != null) {
                profilePath = explicitDefault;
            } else {
                profilePath = "default";
            }
        } else if (profilePath.equals("common")) {
            throw new IOException("Error: The profile \"common\" can not be used as it is reserved for cross-profile settings");
        }

        File configuredPath = new File(profilePath);
        File profileDir;
        if (configuredPath.isAbsolute()) {
            profileDir = configuredPath;
        } else {
            initializeProfilesRootDirectory();
            profileDir = new File(profilesRootDirectory, profilePath).getAbsoluteFile();
        }
        return profileDir;
    }

    private File determineFallbackProfileDirectory(File originalProfileDir) {
        String fallbackProfileName = "rce-fallback-profile-" + System.currentTimeMillis();
        return new File(System.getProperty(SYSTEM_PROPERTY_SYSTEM_TEMP_DIR), fallbackProfileName);
    }

    private void setLoggingParameters() {
        // make the profile path available to log4j/pax-logging
        System.setProperty(PROFILE_LOGFILES_PATH_PROPERTY, finalProfileDirectory.getAbsolutePath());
        if (shutdownRequested) {
            System.setProperty(PROFILE_LOGFILES_PREFIX_PROPERTY, "shutdown-");
        } else {
            System.setProperty(PROFILE_LOGFILES_PREFIX_PROPERTY, "");
        }
    }

    private void setOsgiStorageLocation() {
        File location = new File(finalProfileDirectory, PROFILE_RELATIVE_OSGI_STORAGE_PATH);
        location.mkdirs();
        System.setProperty(SYSTEM_PROPERTY_OSGI_CONFIGURATION_AREA, location.getAbsolutePath());
    }

    /**
     * Attempts to acquire an exclusive lock on the given file. Note that this is not an OS-level lock, but only protects against locks made
     * by other JVM applications; see {@link FileChannel#tryLock(long, long, boolean)} for details.
     * 
     * As a side effect of locking, this method also verifies that the profile directory exists and is actually a directory.
     * 
     * @param profileDir the profile directory to lock
     * @return true if the lock was acquired, false if the lock is already held by another JVM application
     * @throws IOException on unusual errors; should not occur on a simple failure to acquire the lock
     */
    // note: technically, this method produces a resource leak, but this is irrelevant as the lock must be held anyway
    private static boolean attemptToLockProfileDirectory(File profileDir) throws IOException {
        profileDir.mkdirs();
        if (!profileDir.isDirectory()) {
            throw new IOException("Profile directory " + profileDir.getAbsolutePath() + " can not be created or is not a directory");
        }

        File lockfile = new File(profileDir, PROFILE_DIR_LOCK_FILE_NAME);
        FileLock lock = null;
        // create lock file if it does not exist
        lockfile.createNewFile();
        // try to get a lock on this file
        try {
            lock = new RandomAccessFile(lockfile, "rw").getChannel().tryLock();
        } catch (IOException e) {
            throw new IOException("Unexpected error when trying to acquire a file lock on " + lockfile, e);
        }
        // NOTE: It is not necessary to release the lock on the file, this is automatically done
        // by the Java VM or in case of an abnormal end by the operating system
        return lock != null;
    }

}
