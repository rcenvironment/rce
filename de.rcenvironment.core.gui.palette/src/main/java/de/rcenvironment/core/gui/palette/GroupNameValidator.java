/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.component.api.ComponentGroupPathRules;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for custom group names.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class GroupNameValidator {

    /**
     * A regular expression to check whether a group name contains characters that are forbidden.
     */
    protected static final Pattern GROUP_NAME_VALID_CHARACTERS_REGEXP = Pattern.compile("[^a-zA-Z0-9 _\\.,\\-\\+\\(\\)]");

    /**
     * The human-readable error message for a charset violation.
     */
    protected static final String INVALID_GROUP_NAME_CHARSET_ERROR_MESSAGE =
        "Invalid character at position %d (\"%s\") - only characters a-z, A-Z, digits, spaces, and _.,-+() are allowed.";

    /**
     * A regular expression to check whether a given group path ends with a valid character. The empty string is accepted by this to prevent
     * confusing error messages.
     */
    protected static final Pattern VALID_GROUP_NAME_LAST_CHARACTER_REGEXP = Pattern.compile(".*[^ ]$"); // "^ " = no space

    /**
     * The human-readable error message for an invalid last character.
     */
    protected static final String VALID_GROUP_NAME_LAST_CHARACTER_ERROR_MESSAGE =
        "Spaces are allowed, but cannot be the last character.";

    private List<PaletteTreeNode> currentGroups;

    private boolean isTopLevelGroup;

    public GroupNameValidator(List<PaletteTreeNode> currentGroups, boolean isTopLevelGroup) {
        super();
        this.currentGroups = currentGroups;
        this.isTopLevelGroup = isTopLevelGroup;
    }

    public Optional<String> valdiateText(String text) {

        Optional<String> commonValidationError = ComponentGroupPathRules.validateCommonRules(text);
        if (commonValidationError.isPresent()) {
            return commonValidationError;
        }
        Matcher invalidCharMatcher = GROUP_NAME_VALID_CHARACTERS_REGEXP.matcher(text);
        if (invalidCharMatcher.find()) {
            return Optional.of(StringUtils.format(INVALID_GROUP_NAME_CHARSET_ERROR_MESSAGE, invalidCharMatcher.start(0) + 1,
                invalidCharMatcher.group(0)));
        }
        if (!VALID_GROUP_NAME_LAST_CHARACTER_REGEXP.matcher(text).matches()) {
            return Optional.of(VALID_GROUP_NAME_LAST_CHARACTER_ERROR_MESSAGE);
        }
        if (isTopLevelGroup && ToolType.getTopLevelGroupNames().contains(text)) {
            return Optional.of(StringUtils.format("The group names '%s' are not allowed as top level groups.",
                String.join(", ", ToolType.getTopLevelGroupNames())));
        }
        if (currentGroups.stream().anyMatch((node -> node.getNodeName().equals(text)))) {
            return Optional.of("A group with this name already exists.\nThe group may be empty and therefore not displayed.");
        }
        return Optional.empty();
    }

}
