/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.utils.common.CommonIdRules;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Defines the rules for valid authorization group ids, as well as their exported form. These rules are an extension of the
 * {@link CommonIdRules}; these are applied internally.
 * 
 * @author Robert Mischke
 */
public final class AuthorizationIdRules {

    protected static final String INTERNAL_ERROR_THE_NAME_MUST_NOT_BE_NULL = "Internal error: The name must not be null";

    protected static final String INVALID_KEY_IMPORT_DATA = "Invalid key import data: ";

    // Set up as a pattern that matches invalid characters so the first invalid character can be located
    protected static final Pattern INVALID_CHARACTER_CHECK_PATTERN = Pattern.compile("[^a-zA-Z0-9_\\(\\)]");

    protected static final String INVALID_CHARACTERS_ERROR_MESSAGE =
        "Invalid character at position %d (\"%s\"): only the letters a-z, A-Z, digits, and the special characters _() are allowed";

    protected static final int MINIMUM_ID_LENGTH = 2;

    protected static final String MINIMUM_ID_LENGTH_ERROR_MESSAGE =
        "Group names must be at least " + MINIMUM_ID_LENGTH + " characters long";

    protected static final int MAXIMUM_ID_LENGTH = 32;

    protected static final String MAXIMUM_ID_LENGTH_ERROR_MESSAGE =
        "The maximum length for group names is " + MAXIMUM_ID_LENGTH + " characters";

    protected static final Pattern GROUP_ID_RANDOM_PART_REGEXP = Pattern.compile("[0-9a-f]{"
        + AuthorizationService.GROUP_ID_SUFFIX_LENGTH + "}");

    // note: this regexp intentionally ignores rules that were already checked to reduce future adaptations; e.g. the group id charset
    protected static final Pattern EXPECTED_IMPORT_STRING_PATTERN = Pattern.compile("[^:]+:[0-9a-f]+:[0-9]+:[\\w-_]{43}");

    private AuthorizationIdRules() {}

    /**
     * Checks whether the given string is a valid authorization group id.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation. Note that unlike
     *         {@link CommonIdRules#validateCommonIdRules(String)}, the empty string is not accepted because it violates the minimum length
     *         check.
     */
    public static Optional<String> validateAuthorizationGroupId(String input) {
        if (input == null) {
            return Optional.of(INTERNAL_ERROR_THE_NAME_MUST_NOT_BE_NULL);
        }
        if (input.contains(" ")) {
            return Optional.of("Group ids can not contain spaces; consider using underscores (\"_\") instead");
        }
        Matcher matcher = INVALID_CHARACTER_CHECK_PATTERN.matcher(input);
        if (matcher.find()) {
            return Optional.of(StringUtils.format(INVALID_CHARACTERS_ERROR_MESSAGE,
                matcher.start(0) + 1, matcher.group(0)));
        }
        if (input.length() < MINIMUM_ID_LENGTH) {
            return Optional.of(MINIMUM_ID_LENGTH_ERROR_MESSAGE);
        }
        if (input.length() > MAXIMUM_ID_LENGTH) {
            return Optional.of(MAXIMUM_ID_LENGTH_ERROR_MESSAGE);
        }
        if (input.trim().length() != input.length()) {
            return Optional.of("Uncaught leading or training whitespace"); // internal error; should not happen
        }
        return Optional.empty(); // passed
    }

    /**
     * Checks whether the given string is a valid "full" authorization group id, ie a valid group name with an additional hex string of the
     * expected length. Leading or trailing whitespace is ignored. An empty string causes a validation error, as this method is not
     * typically used for validating manual input, but for ids read from configuration files or storage.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation.
     */
    public static Optional<String> validateAuthorizationGroupFullId(String input) {
        if (input == null) {
            return Optional.of(INTERNAL_ERROR_THE_NAME_MUST_NOT_BE_NULL);
        }

        // ignore leading and trailing spaces for validation; these will be stripped in parsing
        input = input.trim();
        if (input.isEmpty()) {
            return Optional.of("Empty id string"); // give a semi-useful message if a whitespace-only string was checked
        }

        String[] parts = input.split(AuthorizationService.ID_SEPARATOR);
        if (parts.length != 2) {
            return Optional.of(
                "Expected a complete authorization group id, which consists of a valid authorization group name, "
                    + "a colon, and an auto-generated part consisting of " + AuthorizationService.GROUP_ID_SUFFIX_LENGTH
                    + " hex characters");
        }

        Optional<String> groupIdError = validateAuthorizationGroupId(parts[0]);
        if (groupIdError.isPresent()) {
            return groupIdError;
        }

        if (!GROUP_ID_RANDOM_PART_REGEXP.matcher(parts[1]).matches()) {
            return Optional.of(
                "The second part of the authorization group id is malformed; it is expected to be a sequence of "
                    + AuthorizationService.GROUP_ID_SUFFIX_LENGTH + " hex characters. ");
        }

        return Optional.empty(); // passed
    }

    /**
     * Checks whether the given string looks like a valid import/invitation string for an authorization group.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation. Note that unlike
     *         {@link CommonIdRules#validateCommonIdRules(String)}, the empty string is not accepted because it violates the minimum length
     *         check.
     */
    public static Optional<String> validateAuthorizationGroupImportString(String input) {
        if (input == null) {
            return Optional.of(INTERNAL_ERROR_THE_NAME_MUST_NOT_BE_NULL);
        }

        // ignore leading and trailing spaces for validation; these will be stripped in parsing
        input = input.trim();
        if (input.isEmpty()) {
            return Optional.of("No key import data found"); // give a semi-useful message if only whitespace was entered
        }

        String[] parts = input.split(AuthorizationService.ID_SEPARATOR);
        if (parts.length != 4) {
            return Optional.of(INVALID_KEY_IMPORT_DATA + "Valid keys consist of 4 colon-separated parts");
        }

        // part 0: group id
        Optional<String> groupNameValidationError = validateAuthorizationGroupId(parts[0]);
        if (groupNameValidationError.isPresent()) {
            return Optional.of("Invalid group id: " + groupNameValidationError.get());
        }

        // part 1: group id suffix
        if (parts[1].length() != AuthorizationService.GROUP_ID_SUFFIX_LENGTH) {
            return Optional.of(INVALID_KEY_IMPORT_DATA + "Invalid length of the generated group id part; "
                + "was this group key generated with a pre-release version of RCE?");
        }

        // part 2: key data version
        if (!parts[2].equals("1")) {
            return Optional.of(INVALID_KEY_IMPORT_DATA + "Unrecognized key version; "
                + "was this group key generated with an incompatible future version of RCE?");
        }

        // overall pattern check
        if (!EXPECTED_IMPORT_STRING_PATTERN.matcher(input).matches()) {
            return Optional.of(INVALID_KEY_IMPORT_DATA + "Invalid data or key format");
        }
        return Optional.empty(); // passed
    }
}
