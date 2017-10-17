/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import java.io.File;

/**
 * This class extends {@link BaseProfile}. In addition, the profile can be marked as default and as recently used.
 *
 * @author Tobias Brieden
 */
public class Profile extends BaseProfile {

    /**
     * The current profile's version number. Needs to be updated manually whenever changed in the profile require an update.
     * 
     * TODO Currently, no update mechanism is implemented. This needs to be done if the profile version number is increased.
     */
    public static final Integer PROFILE_VERSION_NUMBER = 1;

    /**
     * The relative path where the shutdown information (*not* the temporary shutdown *profile*!) of a profile is stored. Made public to
     * allow sending shutdown signals to external instances.
     */
    public static final String PROFILE_SHUTDOWN_DATA_SUBDIR = "internal";

    public Profile(File profileDirectory) throws ProfileException {
        this(profileDirectory, PROFILE_VERSION_NUMBER, true, false);
    }

    public Profile(File profileDirectory, boolean create) throws ProfileException {
        this(profileDirectory, PROFILE_VERSION_NUMBER, create, false);
    }

    public Profile(File profileDirectory, boolean create, boolean allowLegacyProfile) throws ProfileException {
        this(profileDirectory, PROFILE_VERSION_NUMBER, create, allowLegacyProfile);
    }
    
    public Profile(File profileDirectory, int version, boolean create) throws ProfileException {
        this(profileDirectory, version, create, false);
    }

    /**
     * This constructor allows to create profiles with arbitrary version numbers. This is intended to be used in unit test and not in
     * production code!
     * 
     * @param profileDirectory Path to the directory that should be used as a profile.
     * @param create If set to true, the given profile directory will be initialized, if it is an empty directory. If set to false, this
     *        constructor will throw a {@link ProfileException} if the given profile directory is not already containing a valid profile.
     * @param allowLegacyProfile Needs to be set to <code>true</code> if legacy profiles (without a profile.version file) should be allowed
     * @throws ProfileException If the path is not valid or the profile is corrupted.
     */
    public Profile(File profileDirectory, int version, boolean create, boolean allowLegacyProfile) throws ProfileException {
        super(profileDirectory, version);

        if (profileDirectory.getName().equals("common")) {
            throw new ProfileException("Error: The profile \"common\" can not be used as it is reserved for cross-profile settings");
        }

        // This method needs to be called separately, since the constructor isn't doing it. This makes it possible to check the condition
        // above before initializing the object correctly.
        super.init(create, allowLegacyProfile);
    }

    /**
     * @return True if the version of this profile equals current profile version.
     */
    public boolean hasValidVersion() {
        return this.version == PROFILE_VERSION_NUMBER;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Marks this profile as the default profile.
     * 
     * @throws ProfileException Chained exception if an IOException occurs.
     */
    public void markAsDefaultProfile() throws ProfileException {

        try {
            CommonProfileUtils.markAsDefaultProfile(this);
        } catch (CommonProfileException e) {
            throw new ProfileException(e.getMessage(), e);
        }
    }

    /**
     * Marks this profile as recently used.
     * 
     * @throws ProfileException Chained exception that is thrown if an IOException occurs.
     */
    public void markAsRecentlyUsed() throws ProfileException {

        try {
            CommonProfileUtils.markAsRecentlyUsed(this);
        } catch (CommonProfileException e) {
            throw new ProfileException(e.getMessage(), e);
        }
    }

    /**
     * @return Returns the name of the profile if the profile is located within the profiles parent dir. Otherwise, it returns the absolute
     *         path to the profile directory as a String.
     */
    public String getLocationDependentName() {

        File profileParentDir;
        try {
            profileParentDir = ProfileUtils.getProfilesParentDirectory();
        } catch (ProfileException e) {
            profileParentDir = null;
        }

        if (profileParentDir != null && profileParentDir.equals(this.getProfileDirectory().getParentFile())) {
            return this.getName();
        } else {
            return this.getProfileDirectory().getAbsolutePath();
        }

    }
}
