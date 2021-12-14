/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Defines the rules a valid component group path.
 *
 * @author Kathrin Schaffert
 */
public final class ComponentGroupPathRules {

    protected static final String RCE_COMPONENTS = "RCE Components";

    protected static final int MINIMUM_ID_LENGTH = 2;

    protected static final String MINIMUM_ID_LENGTH_ERROR_MESSAGE =
        "The minimum allowed length is at least " + MINIMUM_ID_LENGTH + " characters.";

    protected static final int MAXIMUM_ID_LENGTH = 100;

    protected static final String MAXIMUM_ID_LENGTH_ERROR_MESSAGE = "The maximum allowed length is %d characters.";

    /**
     * A regular expression to check whether a group path contains characters that are forbidden.
     */
    protected static final Pattern GROUP_PATH_VALID_CHARACTERS_REGEXP = Pattern.compile("[^a-zA-Z0-9 _\\.,\\-\\+\\(\\)\\/]");

    /**
     * The human-readable error message for a charset violation.
     */
    protected static final String INVALID_GROUP_PATH_CHARSET_ERROR_MESSAGE =
        "Invalid character at position %d (\"%s\") - anything except a-z, A-Z, digits, spaces, and _.,-+()/ is forbidden.";

    /**
     * A regular expression to check whether a group path starts with an allowed character. The empty string is accepted by this to prevent
     * confusing error messages.
     */
    protected static final Pattern VALID_GROUP_PATH_FIRST_CHARACTER_REGEXP = Pattern.compile("^[a-zA-Z_].*");

    /**
     * The human-readable error message for an invalid first character.
     */
    protected static final String VALID_GROUP_PATH_FIRST_CHARACTER_ERROR_MESSAGE =
        "It must begin with one of the letters a-z, A-Z, or the underscore (\"_\")";

    /**
     * A regular expression to check whether a given group path ends with a valid character. The empty string is accepted by this to prevent
     * confusing error messages.
     */
    protected static final Pattern VALID_GROUP_PATH_LAST_CHARACTER_REGEXP = Pattern.compile(".*[^ \\/]$"); // "^ " = no space

    /**
     * The human-readable error message for an invalid last character.
     */
    protected static final String VALID_GROUP_PATH_LAST_CHARACTER_ERROR_MESSAGE =
        "Spaces or Slashes are allowed, but cannot be the last character.";

    /**
     * A regular expression to check whether a given group path contains a slash with leading spaces.
     */
    protected static final Pattern INVALID_LEADING_SPACES = Pattern.compile("\\s+\\/");

    /**
     * A regular expression to check whether a given group path contains a slash with trading spaces. confusing error messages.
     */
    protected static final Pattern INVALID_TRADING_SPACES = Pattern.compile("\\/\\s+");

    /**
     * The human-readable error message for an invalid leading and trading spaces.
     */
    protected static final String INVALID_LEADING_TRADING_SPACES_ERROR_MESSAGE =
        "Spaces are not allwoed before or after any slash.";

    private ComponentGroupPathRules() {}

    /**
     * Checks whether the given string is a valid group path for a component/tool.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation.
     */
    public static Optional<String> validateComponentGroupPathRules(String input) {
        Optional<String> commonValidationError = validateCommonRules(input);
        if (commonValidationError.isPresent()) {
            return commonValidationError;
        }
        Optional<String> groupPathError = validateGroupPathRules(input);
        if (groupPathError.isPresent()) {
            return groupPathError;
        }
        return Optional.empty(); // passed
    }

    public static Optional<String> validateCommonRules(String input) {
        if (input == null) {
            return Optional.empty();
        }
        if (input.length() < MINIMUM_ID_LENGTH) {
            return Optional.of(MINIMUM_ID_LENGTH_ERROR_MESSAGE);
        }
        if (input.length() > MAXIMUM_ID_LENGTH) {
            return Optional.of(StringUtils.format(MAXIMUM_ID_LENGTH_ERROR_MESSAGE, MAXIMUM_ID_LENGTH));
        }
        if (input.isEmpty()) {
            return Optional.empty(); // passed
        }
        if (!VALID_GROUP_PATH_FIRST_CHARACTER_REGEXP.matcher(input).matches()) {
            return Optional.of(VALID_GROUP_PATH_FIRST_CHARACTER_ERROR_MESSAGE);
        }
        if (INVALID_LEADING_SPACES.matcher(input).find() || INVALID_TRADING_SPACES.matcher(input).find()) {
            return Optional.of(INVALID_LEADING_TRADING_SPACES_ERROR_MESSAGE);
        }
        return Optional.empty(); // passed
    }

    private static Optional<String> validateGroupPathRules(String input) {
        Matcher invalidCharMatcher = GROUP_PATH_VALID_CHARACTERS_REGEXP.matcher(input);
        if (invalidCharMatcher.find()) {
            return Optional.of(StringUtils.format(INVALID_GROUP_PATH_CHARSET_ERROR_MESSAGE, invalidCharMatcher.start(0) + 1,
                invalidCharMatcher.group(0)));
        }
        if (!VALID_GROUP_PATH_LAST_CHARACTER_REGEXP.matcher(input).matches()) {
            return Optional.of(VALID_GROUP_PATH_LAST_CHARACTER_ERROR_MESSAGE);
        }
        return Optional.empty(); // passed
    }


}
