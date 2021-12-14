/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Optional;

import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.utils.common.CommonIdRules;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Defines the rules for valid component ids. These rules are an extension of the {@link CommonIdRules}; these are applied internally.
 * 
 * @author Robert Mischke
 */
public final class ComponentIdRules {

    protected static final int MINIMUM_ID_LENGTH = 2;

    protected static final String MINIMUM_ID_LENGTH_ERROR_MESSAGE =
        "It must consist of at least " + MINIMUM_ID_LENGTH + " characters";

    protected static final int MAXIMUM_ID_LENGTH = 100;

    protected static final String MAXIMUM_ID_LENGTH_ERROR_MESSAGE =
        "The maximum allowed length is %d characters";

    protected static final String ID_INVALID_AS_FILENAME_ERROR_MESSAGE =
        "It violates the rules for valid filenames of at least one operating system";

    private static final int MAXIMUM_VERSION_STRING_LENGTH = 32;

    private ComponentIdRules() {}

    /**
     * Checks whether the given string is a valid component/tool id. See {@link CommonIdRules#validateCommonIdRules(String)} for the basic
     * id rules; additionally, the length of the given id is checked against an allowed range.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation. Note that unlike
     *         {@link CommonIdRules#validateCommonIdRules(String)}, the empty string is not accepted because it violates the minimum length
     *         check. Also note that because the returned validation error must be context-neutral, it is typically prefixed with an
     *         explanatory string like "Invalid tool id: " before presenting it to a user.
     */
    public static Optional<String> validateComponentIdRules(String input) {
        Optional<String> commonValidationError = CommonIdRules.validateCommonIdRules(input); // note: id rule set
        if (commonValidationError.isPresent()) {
            return commonValidationError;
        }
        if (input.length() < MINIMUM_ID_LENGTH) {
            return Optional.of(MINIMUM_ID_LENGTH_ERROR_MESSAGE);
        }
        if (input.length() > MAXIMUM_ID_LENGTH) {
            return Optional.of(StringUtils.format(MAXIMUM_ID_LENGTH_ERROR_MESSAGE, MAXIMUM_ID_LENGTH));
        }
        // additionally, check whether the given id violates any platform-specific rules for filenames; for example, this rules out "LPT1"
        if (!CrossPlatformFilenameUtils.isFilenameValid(input)) {
            return Optional.of(ID_INVALID_AS_FILENAME_ERROR_MESSAGE);
        }
        return Optional.empty(); // passed
    }

    /**
     * Checks whether the given string is a valid component/tool version string. Note that this method accepts the empty string as valid,
     * because it is context dependent whether this is allowed.
     * 
     * @param input the input string to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation.
     */
    public static Optional<String> validateComponentVersionRules(String input) {
        Optional<String> commonValidationError = CommonIdRules.validateCommonVersionStringRules(input); // note: version rule set
        if (commonValidationError.isPresent()) {
            return commonValidationError;
        }
        if (input.length() > MAXIMUM_VERSION_STRING_LENGTH) {
            return Optional.of(StringUtils.format(MAXIMUM_ID_LENGTH_ERROR_MESSAGE, MAXIMUM_VERSION_STRING_LENGTH));
        }
        // additionally, check whether the given version string violates any platform-specific rules for filenames
        if (!CrossPlatformFilenameUtils.isFilenameValid(input)) {
            return Optional.of(ID_INVALID_AS_FILENAME_ERROR_MESSAGE);
        }
        return Optional.empty(); // passed
    }

    /**
     * Checks the tool id, version, and group path properties of the given {@link ComponentInterface} using the individual valiation methods
     * above.
     * 
     * @param componentInterface the {@link ComponentInterface} to test
     * @return An {@link Optional} human-readable error message if (and only if) there is a violation.
     */
    public static Optional<String> validateComponentInterfaceIds(ComponentInterface componentInterface) {
        Optional<String> validationError;
        validationError = validateComponentIdRules(componentInterface.getIdentifier());
        if (validationError.isPresent()) {
            return Optional.of("Invalid component name/id: " + validationError.get());
        }
        validationError = validateComponentVersionRules(componentInterface.getVersion());
        if (validationError.isPresent()) {
            return Optional.of("Invalid component version: " + validationError.get());
        }
        if (componentInterface.getGroupName() != null) {
            validationError = ComponentGroupPathRules.validateComponentGroupPathRules(componentInterface.getGroupName());
            if (validationError.isPresent()) {
                return Optional.of("Invalid component group name: " + validationError.get());
            }
        }
        return Optional.empty(); // passed
    }
}
