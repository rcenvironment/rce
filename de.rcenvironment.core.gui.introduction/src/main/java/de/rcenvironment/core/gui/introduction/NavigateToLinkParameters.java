/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.introduction;

import java.util.Properties;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Wrapper around properties given when executing {@link NavigateToLinkParameters}. Simplifies and centralized access to the properties
 * actually given and check if value is allowed.
 *
 * @author Riccardo Dusi
 */
final class NavigateToLinkParameters {

    private static final String LINK_VALUE = "openLink";

    private static final String LINK_VALUE_NEWS = "news";

    private static final String LINK_VALUE_NEWSLETTER = "newsletter";

    private static final String LINK_VALUE_TWITTER = "twitter";

    private static final String LINK_VALUE_GITHUB = "github";

    private final Properties backingProperties;

    private NavigateToLinkParameters(Properties properties) {
        this.backingProperties = new Properties();
        backingProperties.putAll(properties);
    }

    public static NavigateToLinkParameters createFromProperties(final Properties properties) {
        assertParameterValue(properties);
        return new NavigateToLinkParameters(properties);
    }

    private static void assertParameterValue(Properties properties) {
        final String linkValue = properties.getProperty(LINK_VALUE);

        switch (linkValue) {
        case LINK_VALUE_NEWS:
        case LINK_VALUE_NEWSLETTER:
        case LINK_VALUE_GITHUB:
        case LINK_VALUE_TWITTER:
            return;

        default:
            final String errorMessage = StringUtils.format(
                "Properties given for constructing instance of %s do not contain valid value for key %s."
                    + " Contained value: %s. Valid values: %s, %s, %s, %s",
                NavigateToLinkParameters.class.getName(), LINK_VALUE, linkValue, LINK_VALUE_GITHUB, LINK_VALUE_NEWS, LINK_VALUE_NEWSLETTER,
                LINK_VALUE_TWITTER);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public String getKeyOfParameterValue() {
        return this.backingProperties.getProperty(LINK_VALUE);
    }
}
