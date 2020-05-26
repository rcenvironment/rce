/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.common;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * General utilities not provided by classes in "de.rcenvironment.extras.utils.singlejar".
 * 
 * @author Robert Mischke
 */
public final class CommonUtils {

    private CommonUtils() {}

    /**
     * Returns the default system temp directory ("java.io.tmpdir") as File, which is additionally checked for existence.
     * 
     * TODO check whether uses of this method should use {@link TempFileService} (via {@link TempFileServiceAccess}) instead.
     * 
     * @return the validated system temp directory
     * @throws IOException if the temp directory does not exist or is not a directory
     */
    public static File getValidatedSystemTempDir() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile();
        if (!dir.isDirectory()) {
            throw new IOException("System temp dir does not exist: " + dir);
        }
        return dir;
    }

    /**
     * Convenience method that applies {@link String#matches(String)}, and wraps any failure into a useful error message.
     * 
     * TODO use better exception type
     * 
     * @param input the input to check
     * @param pattern the regular expression to test against
     * @throws IOException on mismatch
     */
    public static void validateStringMatches(String input, String pattern) throws IOException {
        if (!input.matches(pattern)) {
            throw new IOException("String " + input + " does not match pattern " + pattern);
        }
    }

    /**
     * Replaces all placeholders in the given input and returns the resulting string. Keys in the given map define placeholder names, and
     * each placeholder of the text form "${<placeholder name>}" is replaced with the corresponding map value.
     * 
     * The current substitution approach is very simple, so any behavior beyond simple substitution (for example, substitution chaining) is
     * undefined.
     * 
     * @param input the input to process
     * @param substitutionMap the placeholder keys and substitution values
     * @return the input with all substitutions applied
     */
    public static String substitute(String input, Map<String, String> substitutionMap) {
        // simple but sufficient for now
        for (Entry<String, String> entry : substitutionMap.entrySet()) {
            input = input.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return input;
    }

}
