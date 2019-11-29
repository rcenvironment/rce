/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A straightforward value class that contains file handles for the files comprising the representation of a profile on the file system.
 *
 * @author Alexander Weinert
 */
final class ProfileRepresentation {

    private final File profileDir;

    private final File internalDir;

    private final File versionFile;

    private ProfileRepresentation(final File profileDirParam, final File internalDirParam, final File versionFileParam) {
        this.profileDir = profileDirParam;
        this.internalDir = internalDirParam;
        this.versionFile = versionFileParam;
    }

    /**
     * This method only constructs the file objects representing the respective files, but does not actually create these files. In order to
     * actually create the directories and files, please use the methods `tryCreate...`.
     * 
     * @param profileParentDir The parent directory for the profile to be created.
     * @param profileName The name of the profile to be created.
     * @return A fresh instance of ProfileRepresentation that contains file handles for the profile directory, the internal directory, and
     *         the version file.
     * @throws IOException Thrown if creation of the profile parent folder fails.
     */
    static ProfileRepresentation create(final File profileParentDir, final String profileName)
        throws IOException {
        final File profileDir = profileParentDir.toPath().resolve(profileName).toFile();
        final File internalDir = profileDir.toPath().resolve("internal").toFile();
        final File versionFile = internalDir.toPath().resolve("profile.version").toFile();
        return new ProfileRepresentation(profileDir, internalDir, versionFile);
    }

    public File getProfileDir() {
        return profileDir;
    }

    public File getInternalDir() {
        return internalDir;
    }

    public File getVersionFile() {
        return versionFile;
    }

    /**
     * Creates the profile directory, if it does not yet exist. The profile directory must exist for this method to succeed. Successful
     * creation of the directory is asserted via JUnit.
     * 
     * Creates the profile directory, if it does not yet exist.
     * 
     * @return This object for daisy-chaining.
     */
    public ProfileRepresentation tryCreateProfileDir() {
        if (!profileDir.isDirectory()) {
            assertTrue("Creating the profile directory failed", profileDir.mkdir());
        }

        return this;
    }

    /**
     * Creates the directory for internal (i.e., non-user-accessible) data, if it does not yet exist. The profile directory must exist for
     * this method to succeed. Successful creation of the directory is asserted via JUnit.
     * 
     * @return This object for daisy-chaining.
     */
    public ProfileRepresentation tryCreateInternalDir() {
        if (!internalDir.isDirectory()) {
            assertTrue("Creating the internal directory failed", internalDir.mkdir());
        }

        return this;
    }

    /**
     * Creates the file containing the profile version, if it does not yet exist. The internal directory must exist for this method to
     * succeed. Successful creation of the file is asserted via JUnit.
     * 
     * @return This object for daisy-chaining.
     */
    public ProfileRepresentation tryCreateVersionFile() throws IOException {
        if (!versionFile.isFile()) {
            assertTrue(versionFile.createNewFile());
        }

        return this;
    }

    /**
     * Creates the file containing the profile version and writes the given version number to the file, if it does not yet exist. The
     * internal directory must exist for this method to succeed. Successful creation of the file is asserted via JUnit.
     * 
     * @param versionNumber The version number to be written to the newly created version file.
     * @return This object for daisy-chaining.
     * @throws IOException Thrown if creating or writing to the created file fails.
     */
    public ProfileRepresentation tryCreateVersionFile(int versionNumber) throws IOException {
        return this.tryCreateVersionFile(String.valueOf(versionNumber));
    }

    /**
     * Creates the file containing the profile version and writes the given content to the file, if it does not yet exist. The internal
     * directory must exist for this method to succeed. Successful creation of the file is asserted via JUnit.
     * 
     * @param content A string to be written to the newly created version file.
     * @return This object for daisy-chaining.
     * @throws IOException Thrown if creating or writing to the created file fails.
     */
    public ProfileRepresentation tryCreateVersionFile(String content) throws IOException {
        this.tryCreateVersionFile();

        FileWriter versionFileWriter = new FileWriter(versionFile);
        versionFileWriter.write(content);
        versionFileWriter.close();

        return this;
    }
}
