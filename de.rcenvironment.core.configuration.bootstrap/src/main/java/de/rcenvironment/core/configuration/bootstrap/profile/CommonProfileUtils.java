/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * This utility class needs to be used to access the common profile. All read and write methods in this utility class are synchronized to
 * avoid data races within a single RCE instance. Furthermore, the profile's file based lock is used so avoid concurrent access to the
 * common profile from different RCE instances.
 *
 * @author Tobias Brieden
 */
public final class CommonProfileUtils {

    private static final int MILLIS_BETWEEN_LOCKING_ATTEMPTS = 500;

    private static final String SAVED_DEFAULT_PROFILE_ERROR_TEMPLATE = "Unable to read the default profile path from %s.";

    private static final int MAX_LOCKING_ATTEMPTS = 20;

    private static CommonProfileUtils instance;

    private CommonProfile commonProfile;

    private File profilesInformationDirectory;

    private File defaultProfileFile;

    private File recentlyUsedProfilesFile;

    private File profileParent;

    private ObjectMapper mapper;

    private CommonProfileUtils() throws CommonProfileException {
        try {
            profileParent = ProfileUtils.getProfilesParentDirectory();
        } catch (ProfileException e) {
            throw new CommonProfileException("Error while determining the profiles parent directory.", e);
        }
        File commonProfileDirectory = Paths.get(profileParent.getAbsolutePath(), "common").toFile();

        try {
            commonProfile = new CommonProfile.Builder(commonProfileDirectory).create(true).migrate(false).buildCommonProfile();
        } catch (ProfileException e) {
            throw new CommonProfileException("The common profile cannot be initialized.", e);
        }

        profilesInformationDirectory = new File(commonProfile.getProfileDirectory(), "profiles");
        defaultProfileFile = new File(profilesInformationDirectory, "defaultProfile");
        recentlyUsedProfilesFile = new File(profilesInformationDirectory, "recentlyUsed");
        mapper = JsonUtils.getDefaultObjectMapper();
    }

    /**
     * Methods that should be executed while having exclusive read/write access to the common profile should implemented this interface and
     * should then be executed using the {@link CommonProfileUtils#lockExecuteRelease(Command)} method.
     *
     * @param <T> the return type of the executed method
     */
    protected interface Command<T> {

        T execute() throws CommonProfileException;
    }

    protected static synchronized <T> T lockExecuteRelease(Command<T> command) throws CommonProfileException {

        if (instance == null) {
            instance = new CommonProfileUtils();
        } else {
            // check if the profiles parent is still the same. This normally doesn't happen in normal operation but can happen in unit
            // tests
            try {
                if (!instance.profileParent.equals(ProfileUtils.getProfilesParentDirectory())) {
                    // and reinitialize if the user home changed
                    instance = new CommonProfileUtils();
                }
            } catch (ProfileException e) {
                throw new CommonProfileException("Error while determining the profiles parent directory.", e);
            }
        }

        // try to lock the common profile
        boolean lockAquired = false;
        for (int i = 0; i < MAX_LOCKING_ATTEMPTS; i++) {

            try {
                lockAquired = instance.commonProfile.attemptToLockProfileDirectory();
                if (lockAquired) {
                    break;
                } else {
                    Thread.sleep(MILLIS_BETWEEN_LOCKING_ATTEMPTS);
                }
            } catch (ProfileException | InterruptedException e) {
                throw new CommonProfileException("Unable to lock the common profile.", e);
            }
        }
        if (!lockAquired) {
            throw new CommonProfileException("Unable to lock the common profile.");
        }

        try {
            return command.execute();
        } finally {
            // we even need to release the lock if an exception was thrown
            try {
                CommonProfileUtils.instance.commonProfile.releaseLock();
            } catch (IOException e) {
                throw new CommonProfileException("Unable to release the common profile lock.", e);
            }
        }
    }

    /**
     * 
     * @return Returns a list of the recently used profiles. Most recently used profile first. The list contains only valid profiles that
     *         can be accesses.
     * @throws CommonProfileException If the file containing the list of all recently used profiles cannot be read.
     */
    public static List<Profile> getRecentlyUsedProfiles() throws CommonProfileException {

        return lockExecuteRelease(new Command<List<Profile>>() {

            @Override
            public List<Profile> execute() throws CommonProfileException {

                List<Profile> recentlyUsedProfiles = new LinkedList<>();

                if (instance.recentlyUsedProfilesFile.exists()) {
                    List<String> lines;
                    try {

                        lines = Files.readAllLines(instance.recentlyUsedProfilesFile.toPath(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new CommonProfileException("Unable to read the list of recently used profiles.", e);
                    }

                    for (String line : lines) {
                        try {
                            Profile recentlyUsedProfile =
                                new Profile.Builder(new File(line)).create(false).migrate(false).buildUserProfile();
                            recentlyUsedProfiles.add(recentlyUsedProfile);
                        } catch (ProfileException e) {
                            // Ignore, profileDirectory was not a valid profileDirectory. Do not delete the profile from the
                            // recentlyUsedProfilesFile since the profile might become available again (e.g. if a network storage becomes
                            // visible again).
                        }
                    }
                }

                return recentlyUsedProfiles;
            }
        });
    }

    /**
     * Marks a given profile as recently used by storing this information in the common profile.
     * 
     * @param profile The profile that should be marked.
     * @throws CommonProfileException Chained exception that is thrown if an {@link IOException} occurs.
     */
    public static void markAsRecentlyUsed(final Profile profile) throws CommonProfileException {

        lockExecuteRelease(new Command<Void>() {

            @Override
            public Void execute() throws CommonProfileException {
                try {
                    // create the profiles sub-directory within the common profile
                    instance.profilesInformationDirectory.mkdirs();

                    instance.recentlyUsedProfilesFile.createNewFile();

                    // check if the profile is already listed in the recently used profiles ...
                    List<String> lines = Files.readAllLines(instance.recentlyUsedProfilesFile.toPath(), StandardCharsets.UTF_8);
                    String lineRepresentingCurrentProfile = null;
                    for (String line : lines) {
                        File recentlyUsedProfile = new File(line);
                        if (recentlyUsedProfile.getCanonicalPath().equals(profile.getProfileDirectory().getCanonicalPath())) {
                            lineRepresentingCurrentProfile = line;
                            break;
                        }
                    }

                    // ... if this is the case, move the profile to the first position
                    if (lineRepresentingCurrentProfile != null) {
                        lines.remove(lineRepresentingCurrentProfile);
                    }
                    lines.add(0, profile.getProfileDirectory().getCanonicalPath());

                    // write all lines back to the file
                    try (Writer writer =
                        new OutputStreamWriter(new FileOutputStream(instance.recentlyUsedProfilesFile), StandardCharsets.UTF_8)) {
                        for (String line : lines) {
                            writer.write(line);
                            writer.write(System.lineSeparator());
                        }
                    }
                } catch (IOException e) {
                    throw new CommonProfileException("Cannot mark the profile as recently used.", e);
                }

                return null;
            }
        });

    }

    /**
     * @return Returns either a map from installation directories to their saved default profile directories, or an empty map if the file
     *         does not exist.
     */
    private static Map<File, File> readSavedDefaultProfiles() throws JsonParseException, JsonMappingException, IOException {

        Map<File, File> installationToDefaultProfileMap;
        if (instance.defaultProfileFile.isFile()) {
            installationToDefaultProfileMap =
                instance.mapper.readValue(instance.defaultProfileFile, new TypeReference<HashMap<File, File>>() {
                });
        } else {
            installationToDefaultProfileMap = new HashMap<File, File>();
        }

        return installationToDefaultProfileMap;
    }

    /**
     * Marks a profile as the default profile.
     * 
     * @param profile The profile which should be marked as the default profile.
     * @throws CommonProfileException Chained exception if an {@link IOException} occurs.
     */
    public static void markAsDefaultProfile(final Profile profile) throws CommonProfileException {

        lockExecuteRelease(new Command<Void>() {

            @Override
            public Void execute() throws CommonProfileException {
                // create the profiles sub-directory within the common profile
                instance.profilesInformationDirectory.mkdirs();

                try {
                    // instance.defaultProfileFile.createNewFile();

                    // read all default profiles
                    Map<File, File> installationToDefaultProfileMap = readSavedDefaultProfiles();

                    // override the value for the current installation directory
                    File currentInstallationDir = BootstrapConfiguration.getInstallationDir();
                    installationToDefaultProfileMap.put(currentInstallationDir, profile.getProfileDirectory());

                    // write the map back into the file
                    instance.mapper.writeValue(instance.defaultProfileFile, installationToDefaultProfileMap);
                } catch (IOException e) {
                    throw new CommonProfileException("Error while trying to access the file storing the default profiles.", e);
                }

                return null;
            }
        });

    }

    /**
     * @return Either returns the path to the saved default profile or null if no profile was marked as default or the marked profile does
     *         not exist or is invalid.
     * @throws CommonProfileException If the file storing the default value cannot be accessed or is corrupted.
     */
    public static File getSavedDefaultProfile() throws CommonProfileException {

        return lockExecuteRelease(new Command<File>() {

            @Override
            public File execute() throws CommonProfileException {

                // the defaultProfile file need to exist ...
                if (instance.defaultProfileFile.isFile()) {

                    try {
                        Map<File, File> installationToDefaultProfileMap = readSavedDefaultProfiles();
                        File currentInstallationDir = BootstrapConfiguration.getInstallationDir();
                        File savedDefaultPath = installationToDefaultProfileMap.get(currentInstallationDir);

                        if (savedDefaultPath == null) {
                            return null;
                        } else if (savedDefaultPath.isAbsolute()) {

                            try {
                                // Check if the profile at the stored location is actually a valid profile. This should be done in a more
                                // elegant way, but for now, we try to construct an instance of Profile and see if that succeeds. Since
                                // Profile performs a number of validity checks in its constructor, a successful instantiation implies
                                // validity of the profile on the file system.
                                new Profile.Builder(savedDefaultPath).create(false).migrate(false).build();
                                return savedDefaultPath;
                            } catch (ProfileException e) {
                                // if the path does not point to a valid profile this call will throw a ProfileException
                                return null;
                            }
                        } else {
                            throw new CommonProfileException(
                                StringUtils.format(SAVED_DEFAULT_PROFILE_ERROR_TEMPLATE, instance.defaultProfileFile.getAbsolutePath()));
                        }
                    } catch (IOException e) {
                        throw new CommonProfileException(
                            StringUtils.format(SAVED_DEFAULT_PROFILE_ERROR_TEMPLATE, instance.defaultProfileFile.getAbsolutePath()), e);
                    }
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Clear the default profile selection.
     * 
     * @throws CommonProfileException This exception is never thrown in this method.
     */
    public static void clearDefaultProfile() throws CommonProfileException {
        lockExecuteRelease(new Command<Void>() {

            @Override
            public Void execute() throws CommonProfileException {

                try {
                    if (instance.defaultProfileFile.isFile()) {
                        Map<File, File> installationToDefaultProfileMap = readSavedDefaultProfiles();
                        File currentInstallationDir = BootstrapConfiguration.getInstallationDir();
                        installationToDefaultProfileMap.remove(currentInstallationDir);
                        instance.mapper.writeValue(instance.defaultProfileFile, installationToDefaultProfileMap);
                    }
                } catch (IOException e) {
                    throw new CommonProfileException("Error while trying to access the file storing the default profiles.", e);
                }

                return null;
            }
        });

    }

}
