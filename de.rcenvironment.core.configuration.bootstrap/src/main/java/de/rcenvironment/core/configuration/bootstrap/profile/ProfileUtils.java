/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility methods for profile handling.
 *
 * @author Tobias Brieden
 */
public final class ProfileUtils {

    /**
     * A system property that can be used to override the default profile id/path (default id: "default"). It is only applied if no "-p"
     * launch parameter is set, and can be either a relative or absolute path.
     */
    public static final String SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH = "rce.profile.default";

    /**
     * System property that defines the user's home directory.
     */
    public static final String SYSTEM_PROPERTY_USER_HOME = "user.home";

    /**
     * A system property that can be used to override the parent directory for profiles defined by a relative path or id (default "~/.rce").
     */
    public static final String SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE = "rce.profiles.parentDir";

    private static final String FAILED_TO_CREATE_PROFILE_PARENT_MESSAGE_TEMLATE = "Failed to create the profile root directory \"%s\"";

    private static final String PROFILE_PARENT_NEEDS_TO_BE_ABSOLUTE_MESSAGE =
        "The path used to override the profile parent directory needs to be absolute.";

    private static final String SYSTEM_PROPERTY_SYSTEM_TEMP_DIR = "java.io.tmpdir";

    private ProfileUtils() {}

    /**
     * This method returns the parent directory for all profiles. Creates the directory on the file system, if the determined directory does
     * not exist yet.
     * 
     * By default a hidden subfolder named '.rce' within the user's home directory is used as the profiles parent directory. This default
     * can be overwritten by a system property.
     * 
     * @return The parent directory for all profiles as a File.
     * @throws ProfileException Thrown, if the determined profile parent directory cannot be created or it is not a directory.
     */
    public static File getProfilesParentDirectory() throws ProfileException {
        String parentPathOverride = System.getProperty(SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE);
        File profilesRootDir;
        if (parentPathOverride == null) {
            String userHome = System.getProperty(SYSTEM_PROPERTY_USER_HOME);
            profilesRootDir = new File(userHome, ".rce").getAbsoluteFile();
        } else {
            profilesRootDir = new File(parentPathOverride);
            if (!profilesRootDir.isAbsolute()) {
                throw new ProfileException(
                    StringUtils.format(PROFILE_PARENT_NEEDS_TO_BE_ABSOLUTE_MESSAGE + " %s", profilesRootDir.getAbsolutePath()));
            }
        }

        profilesRootDir.mkdirs();
        if (!profilesRootDir.isDirectory()) {
            throw new ProfileException(
                StringUtils.format(FAILED_TO_CREATE_PROFILE_PARENT_MESSAGE_TEMLATE, profilesRootDir.getAbsolutePath()));
        }

        return profilesRootDir;
    }

    /**
     * Returns the default profile. The default profile is determined in the following order:
     * 
     * 1. Check if the rce.profile.default system property is set
     * 
     * 2. Check if a default profile is selected in the common profile
     * 
     * 3. Return the implicit default profile ("default")
     * 
     * @return The absolute path to the determined default profile.
     * @throws ProfileException Thrown if the profiles parent directory cannot be accessed.
     */
    public static File getDefaultProfilePath() throws ProfileException {

        // 1. system property
        String explicitDefault = System.getProperty(SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH);
        if (explicitDefault != null) {
            // make relative paths absolute
            File explicitDefaultPath = new File(explicitDefault);
            if (!explicitDefaultPath.isAbsolute()) {
                return new File(ProfileUtils.getProfilesParentDirectory(), explicitDefault);
            } else {
                return explicitDefaultPath;
            }
        }

        // 2. saved default
        File savedDefaultProfile = null;
        try {
            savedDefaultProfile = CommonProfileUtils.getSavedDefaultProfile();
        } catch (CommonProfileException e) {
            // ignore, since the null check is performed in the next line
        }
        if (savedDefaultProfile != null) {
            return savedDefaultProfile;
        }

        // 3. implicit default
        File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();
        return new File(profileParentDirectory, "default");
    }

    /**
     * 
     * @param profileParentDirectory Needs to be a valid directory.
     * @return A list of all profiles within the given directory.
     */
    public static List<Profile> listProfiles(File profileParentDirectory) {
        List<Profile> profiles = new LinkedList<Profile>();

        for (File profileDirectory : profileParentDirectory.listFiles()) {

            try {
                Profile profile = new Profile.Builder(profileDirectory).create(false).migrate(false).buildUserProfile();
                profiles.add(profile);
            } catch (ProfileException e) {
                // Ignore, profileDirectory was not a valid profileDirectory
            }
        }

        return profiles;
    }

    /**
     * @return The fall-back profile.
     * @throws ProfileException Thrown, if the fall-back profile cannot be created.
     */
    public static Profile getFallbackProfile() throws ProfileException {
        String fallbackProfileName = "rce-fallback-profile-" + System.currentTimeMillis();
        File fallbackProfileDirectory = new File(System.getProperty(SYSTEM_PROPERTY_SYSTEM_TEMP_DIR), fallbackProfileName);
        return new Profile.Builder(fallbackProfileDirectory).create(true).migrate(true).buildUserProfile();
    }

}
