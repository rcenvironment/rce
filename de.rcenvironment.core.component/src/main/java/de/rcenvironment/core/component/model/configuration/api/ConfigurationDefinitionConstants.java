/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

/**
 * Constants used in JSON files to describe endpoints.
 * 
 * @author Doreen Seider
 * @author Kathrin Schaffert (added KEY_METADATA_COMMENT)
 */
public final class ConfigurationDefinitionConstants {

    /** JSON key used in .json definition file. */
    public static final String JSON_KEY_CONFIGURATION = "configuration";

    /** JSON key used in .json definition file. */
    public static final String JSON_KEY_PLACEHOLDERS = "placeholders";

    /** JSON key used in .json definition file. */
    public static final String JSON_KEY_ACTIVATION_FILTER = "activationFilter";

    /** Format string for placeholders. */
    public static final String PLACEHOLDER_FORMAT_STRING = "${%s}";

    /** Key for the configuration key. */
    public static final String KEY_CONFIGURATION_KEY = "key";

    /** Key to identifying to which config key the metadata belongs. */
    public static final String KEY_METADATA_CONFIG_KEY = "key";

    /** Key to get the display name of the configuration. */
    public static final String KEY_METADATA_GUI_NAME = "guiName";

    /** Key to get the comment of a property configuration. */
    public static final String KEY_METADATA_COMMENT = "comment";

    /** Key to get the group in the gui. */
    public static final String KEY_METADATA_GUI_GROUP_NAME = "guiGroupPosition";

    /** Key to get the order position of the configuration. */
    public static final String KEY_METADATA_GUI_POSITION = "guiPosition";

    private ConfigurationDefinitionConstants() {}

}
