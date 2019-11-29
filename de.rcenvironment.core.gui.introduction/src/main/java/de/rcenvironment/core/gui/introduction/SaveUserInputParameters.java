/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.introduction;

import java.util.Properties;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Wrapper around properties given when executing {@link SaveUserInput}. Simplifies and centralized access to the properties actually given.
 *
 * @author Alexander Weinert
 */
final class SaveUserInputParameters {

    private static final String CHECKBOX_VALUE_KEY = "checkboxValue";

    private static final String CHECKBOX_VALUE_CHECKED = "checked";

    private static final String CHECKBOX_VALUE_UNCHECKED = "unchecked";

    private final Properties backingProperties;

    private SaveUserInputParameters(Properties properties) {
        this.backingProperties = new Properties();
        backingProperties.putAll(properties);
    }

    public static SaveUserInputParameters createFromProperties(final Properties properties) {
        assertCheckboxValueKeyExists(properties);
        assertCheckboxValueIsValid(properties);

        return new SaveUserInputParameters(properties);
    }

    private static void assertCheckboxValueKeyExists(Properties properties) {
        if (!properties.containsKey(CHECKBOX_VALUE_KEY)) {
            final String errorMessage = StringUtils.format("Properties given for constructing instance of %s do not contain key %s",
                SaveUserInputParameters.class.getName(), CHECKBOX_VALUE_KEY);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void assertCheckboxValueIsValid(Properties properties) {
        final String checkboxValue = properties.getProperty(CHECKBOX_VALUE_KEY);

        if (CHECKBOX_VALUE_CHECKED.equals(checkboxValue)) {
            return;
        }

        if (CHECKBOX_VALUE_UNCHECKED.equals(checkboxValue)) {
            return;
        }

        final String errorMessage = StringUtils.format(
            "Properties given for constructing instance of %s do not contain valid value for key %s."
            + " Contained value: %s. Valid values: %s, %s",
            SaveUserInputParameters.class.getName(), CHECKBOX_VALUE_KEY, checkboxValue, CHECKBOX_VALUE_CHECKED, CHECKBOX_VALUE_UNCHECKED);
        throw new IllegalArgumentException(errorMessage);
    }

    public boolean isCheckboxChecked() {
        final String checkboxValue = this.backingProperties.getProperty(CHECKBOX_VALUE_KEY);
        return CHECKBOX_VALUE_CHECKED.equals(checkboxValue);
    }
}
