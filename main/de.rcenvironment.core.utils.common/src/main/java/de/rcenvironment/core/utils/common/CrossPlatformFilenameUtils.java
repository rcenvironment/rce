/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common;

import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for handling filenames.
 *
 * @author Jan Flink
 */
public final class CrossPlatformFilenameUtils {



    /**
     * Forbidden characaters on any platform.
     */
    public static final String[] COMMON_FORBIDDEN_FILENAME_PATTERNS = {
        "/"
    };

    /**
     * Forbidden characters on windows platforms.
     */
    public static final String[] WINDOWS_FORBIDDEN_FILENAME_PATTERNS = {
        "\\",
        ":",
        "\"",
        "*",
        "?",
        "<",
        ">",
        "|"
    };

    private static final int MAXIMUM_FILENAME_LENGTH = 240;

    private static final String FORWARD_SLASH = "/";

    private CrossPlatformFilenameUtils() {};

    private static Pattern getForbiddenFilenamePattern() {
        String regExp =
            "[\\s]+|[".concat(StringUtils.join(COMMON_FORBIDDEN_FILENAME_PATTERNS));
        regExp = regExp.concat(StringUtils.join(WINDOWS_FORBIDDEN_FILENAME_PATTERNS));
        regExp = regExp.concat("]+");
        return Pattern.compile(regExp);
    }


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
        return !getForbiddenFilenamePattern().matcher(filename).find();
    }

    /**
     * Checks the validity of a given path on all platforms.
     * 
     * @param path The path to check
     * @return True, if path is valid on all platforms
     */
    public static boolean isPathValid(String path) {
        path = FilenameUtils.separatorsToUnix(path);
        // Remove path separators and drive letters
        path = path.replaceAll(FORWARD_SLASH, "").replaceAll("(^[a-zA-Z]{1}[:]{1})", "");
        return !getForbiddenFilenamePattern().matcher(path).find();
    }

}
