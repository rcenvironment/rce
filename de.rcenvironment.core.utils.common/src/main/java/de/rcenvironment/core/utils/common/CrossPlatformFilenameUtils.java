/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility class for handling filenames.
 *
 * @author Jan Flink
 * @author Tobias Rodehutskors
 */
public final class CrossPlatformFilenameUtils {

    /**
     * There is a bunch of filenames which are forbidden on Windows, even though they are composed of valid characters.
     */
    public static final String[] FORBIDDEN_WINDOWS_FILENAMES = { "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
        "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9" };

    /**
     * Forbidden characters for filenames.
     */
    public static final char[] FORBIDDEN_CHARACTERS = {
        '/', // forbidden on Linux
        '\\', // forbidden on ...
        ':',
        '\"',
        '*',
        '?',
        '<',
        '>',
        '|' }; // ... Windows

    /**
     * Forbidden characters on windows platforms.
     */
    private static final String[] WINDOWS_FORBIDDEN_FILENAME_PATTERNS = {
        // control characters are not allowed
        "\\x00-\\x1F",
        "\\x7F",
        // whitespace is not allowed (space is allowed)
        "\\t",
        "\\n",
        "\\f",
        "\\r",
    };

    /**
     * Other forbidden regex.
     */
    private static final String[] FORBIDDEN_REGEX = {

        /**
         * .* matches any character (except for line terminators) between zero and unlimited times \.* matches the character . literally
         * (case sensitive) + Matches between one and unlimited times $ asserts position at the end of the string
         */
        ".*\\.+$"
    };

    private static final int MAXIMUM_FILENAME_LENGTH = 240;

    private static final String FORWARD_SLASH = "/";

    private static Pattern forbiddenCharacterPattern;
    static {
        String regExp = "[".concat(new String(FORBIDDEN_CHARACTERS));
        regExp = regExp.concat(org.apache.commons.lang3.StringUtils.join(WINDOWS_FORBIDDEN_FILENAME_PATTERNS));
        regExp = regExp.concat("]");
        // compile this pattern once and store it for reuse
        forbiddenCharacterPattern = Pattern.compile(regExp);
    }

    private static Pattern forbiddenFilenamePattern;
    static {
        // some filenames are forbidden, and also not be used with an optional extension:
        // (\\..*) is a dot followed by * arbitrary characters, e.g. ".test"
        String regExp = FORBIDDEN_WINDOWS_FILENAMES[0] + "(\\..*)?";

        for (int i = 1; i < FORBIDDEN_WINDOWS_FILENAMES.length; i++) {
            regExp = regExp.concat("|" + FORBIDDEN_WINDOWS_FILENAMES[i] + "(\\..*)?");
        }
        for (String current : FORBIDDEN_REGEX) {
            regExp = regExp.concat("|" + current);
        }
        // compile this pattern once and store it for reuse
        forbiddenFilenamePattern = Pattern.compile(regExp);
    }
    
    private static Pattern nfsFilePattern;
    static {
        // .nfsXXXX files are an artifact of the NFS "silly rename" (http://nfs.sourceforge.net/#faq_d2) and should be ignored
        String regExp = "^\\.nfs.+$";
        // compile this pattern once and store it for reuse
        nfsFilePattern = Pattern.compile(regExp);
    }

    private CrossPlatformFilenameUtils() {};

    /**
     * Checks the validity of a given filename on all platforms.
     * 
     * @param filename The filename to check
     * @return True, if the filename is valid on all platforms.
     */
    public static boolean isFilenameValid(String filename) {
        if (filename.length() == 0 || filename.length() > MAXIMUM_FILENAME_LENGTH) {
            return false;
        }

        // the filename should neither contain a forbidden character nor should it be forbidden by itself
        return !forbiddenCharacterPattern.matcher(filename).find() && !forbiddenFilenamePattern.matcher(filename).matches();
    }

    /**
     * Checks the validity of a given filename on all platforms and throws an {@link InvalidFilenameException} if it is not valid.
     * 
     * @param filename The filename to check
     * @throws InvalidFilenameException if the filename is not valid on all platforms
     */
    public static void throwExceptionIfFilenameNotValid(String filename) throws InvalidFilenameException {
        if (!isFilenameValid(filename)) {
            throw new InvalidFilenameException(filename);
        }
    }

    /**
     * Checks the validity of a given filename on all platforms and throws an {@link IOException} if it is not valid.
     * 
     * @param filename The filename to check
     * @throws IOException if the filename is not valid on all platforms
     */
    public static void throwIOExceptionIfFilenameNotValid(String filename) throws IOException {
        if (!isFilenameValid(filename)) {
            throw new IOException(StringUtils.format(InvalidFilenameException.INVALID_FILENAME_MESSAGE_TEMPLATE, filename));
        }
    }

    /**
     * Checks the validity of a given path on all platforms.
     * 
     * @param path The path to check
     * @return True, if path is valid on all platforms
     */
    public static boolean isPathValid(String path) {
        path = FilenameUtils.separatorsToUnix(path);
        // Remove the drive letters
        path = path.replaceAll("(^[a-zA-Z]{1}[:]{1})", "");

        for (String pathSegment : path.split(FORWARD_SLASH)) {
            if (pathSegment.isEmpty()) {
                continue;
            }

            if (!isFilenameValid(pathSegment)) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Checks if the filename is a .nfsXXXX file.
     * 
     * @param filename The file name to check.
     * @return True, if it is a .nfsXXX file.
     */
    public static boolean isNFSFile(String filename) {
        return nfsFilePattern.matcher(filename).matches();
    }
}
