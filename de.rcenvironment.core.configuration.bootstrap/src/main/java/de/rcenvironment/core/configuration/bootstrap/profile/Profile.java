/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import java.io.File;

/**
 * This class extends {@link CommonProfile}. In addition, the profile can be marked as default and as recently used.
 *
 * @author Tobias Brieden
 */
public class Profile extends CommonProfile {

    /**
     * The relative path where the shutdown information (*not* the temporary shutdown *profile*!) of a profile is stored. Made public to
     * allow sending shutdown signals to external instances.
     */
    public static final String PROFILE_SHUTDOWN_DATA_SUBDIR = "internal";

    Profile(final File profileDirectory) {
        super(profileDirectory);
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
