/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines the common minimum baseline for various kinds of ids. Currently affected ids are:
 * <ul>
 * <li>component/tool ids and versions
 * <li>remote access component/tool ids and versions
 * <li>palette group ids
 * <li>authorization group ids
 * </ul>
 * <p>
 * For obvious reasons, any change to these rules must be carefully checked for side effects.
 * <p>
 * Implementation note: If only a simple true/false decision was needed, these individual rules could be easily merged into a single regular
 * expression. However, this is not done in favor of returning more specific validation messages, and to define precedence between them.
 *
 * @author Robert Mischke
 */
public final class CommonIdRules {

    /**
     * A regular expression to check whether a given tool id contains characters that are forbidden in all kinds of ids. Note that
     * generating a proper message is somewhat tricky from a usability perspective: It is not universally possible to list the allowed
     * characters in the message, as the specific allowed set may be smaller than this generic set.
     * <p>
     * The empty string is accepted by this to prevent confusing error messages.
     */
    protected static final Pattern ID_OR_VERSION_STRING_INVALID_CHARACTERS_REGEXP = Pattern.compile("[^a-zA-Z0-9 _\\.,\\-\\+\\(\\)]");

    /**
     * The human-readable error message for a charset violation; see the pattern JavaDoc for the difficulties in phrasing this message.
     */
    // TODO consider making the "allowed characters" message part a parameter to resolve this problem
    protected static final String VALID_ID_OR_VERSION_STRING_CHARSET_ERROR_MESSAGE =
        "Invalid character at position %d (\"%s\") - anything except a-z, A-Z, digits, spaces, and _.,-+() is forbidden";

    /**
     * A regular expression to check whether a given tool id starts with an allowed character. The empty string is accepted by this to
     * prevent confusing error messages.
     */
    protected static final Pattern VALID_ID_FIRST_CHARACTER_REGEXP = Pattern.compile("^[a-zA-Z_].*");

    /**
     * The human-readable error message for an invalid first character.
     */
    protected static final String VALID_ID_FIRST_CHARACTER_ERROR_MESSAGE =
        "It must begin with one of the letters a-z, A-Z, or the underscore (\"_\")";

    /**
     * A regular expression to check whether a given tool id starts with an allowed character. The empty string is accepted by this to
     * prevent confusing error messages.
     */
    protected static final Pattern VALID_VERSION_STRING_FIRST_CHARACTER_REGEXP = Pattern.compile("^[a-zA-Z0-9_].*");

    /**
     * The human-readable error message for an invalid first character.
     */
    protected static final String VALID_VERSION_STRING_FIRST_CHARACTER_ERROR_MESSAGE =
        "It must begin with one of the letters a-z, A-Z, a digit, or the underscore (\"_\")";

    /**
     * A regular expression to check whether a given id ends with a valid character. The empty string is accepted by this to prevent
     * confusing error messages.
     */
    protected static final Pattern VALID_ID_OR_VERSION_STRING_LAST_CHARACTER_REGEXP = Pattern.compile(".*[^ ]$"); // "^ " = no space

    /**
     * The human-readable error message for an invalid last character.
     */
    protected static final String VALID_ID_OR_VERSION_STRING_LAST_CHARACTER_ERROR_MESSAGE =
        "Spaces are allowed, but cannot be the last character";

    private CommonIdRules() {}

    /**
     * Checks whether the given string is either empty, or is comprised only of characters from the "common valid input set" (see
     * {@link #VALID_ID_OR_VERSION_STRING_CHARSET_ERROR_MESSAGE} for a human-readable description), and matches additional rules for the
     * first and last characters.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) the input string is not empty AND there is a violation.
     *         Note that because the returned validation error must be context-neutral, it is typically prefixed with an explanatory string
     *         like "Invalid tool id: " before presenting it to a user.
     */
    public static Optional<String> validateCommonIdRules(String input) {
        if (input.isEmpty()) {
            return Optional.empty(); // passed
        }
        Optional<String> characterValidationError = checkForInvalidCharacters(input); // shared rules
        if (characterValidationError.isPresent()) {
            return characterValidationError;
        }
        if (!VALID_ID_FIRST_CHARACTER_REGEXP.matcher(input).matches()) { // id rules
            return Optional.of(VALID_ID_FIRST_CHARACTER_ERROR_MESSAGE);
        }
        if (!VALID_ID_OR_VERSION_STRING_LAST_CHARACTER_REGEXP.matcher(input).matches()) { // shared rules
            return Optional.of(VALID_ID_OR_VERSION_STRING_LAST_CHARACTER_ERROR_MESSAGE);
        }
        return Optional.empty(); // passed
    }

    /**
     * Checks whether the given string is either empty, or is comprised only of characters from the "common valid input set" (see
     * {@link #VALID_ID_OR_VERSION_STRING_CHARSET_ERROR_MESSAGE} for a human-readable description), and matches additional rules for the
     * first and last characters. The first character rules are different from the common id rules; the other checks are currently equal.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) the input string is not empty AND there is a violation.
     *         Note that because the returned validation error must be context-neutral, it is typically prefixed with an explanatory string
     *         like "Invalid tool version: " before presenting it to a user.
     */
    public static Optional<String> validateCommonVersionStringRules(String input) {
        if (input.isEmpty()) {
            return Optional.empty(); // passed
        }
        Optional<String> characterValidationError = checkForInvalidCharacters(input); // shared rules
        if (characterValidationError.isPresent()) {
            return characterValidationError;
        }
        if (!VALID_VERSION_STRING_FIRST_CHARACTER_REGEXP.matcher(input).matches()) { // version string rules
            return Optional.of(VALID_VERSION_STRING_FIRST_CHARACTER_ERROR_MESSAGE);
        }
        if (!VALID_ID_OR_VERSION_STRING_LAST_CHARACTER_REGEXP.matcher(input).matches()) { // shared rules
            return Optional.of(VALID_ID_OR_VERSION_STRING_LAST_CHARACTER_ERROR_MESSAGE);
        }
        return Optional.empty(); // passed
    }

    private static Optional<String> checkForInvalidCharacters(String input) {
        Matcher invalidCharMatcher = ID_OR_VERSION_STRING_INVALID_CHARACTERS_REGEXP.matcher(input);
        if (invalidCharMatcher.find()) {
            return Optional.of(StringUtils.format(VALID_ID_OR_VERSION_STRING_CHARSET_ERROR_MESSAGE, invalidCharMatcher.start(0) + 1,
                invalidCharMatcher.group(0)));
        }
        return Optional.empty(); // no invalid characters
    }
}
