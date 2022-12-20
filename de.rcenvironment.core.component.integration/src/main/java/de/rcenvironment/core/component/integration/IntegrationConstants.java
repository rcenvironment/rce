/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

/**
 * Constants for the Integration of Tools and Workflows.
 * 
 * @author Kathrin Schaffert
 */
public final class IntegrationConstants {

    /** Constant. */
    public static final String INTEGRATION_TYPE = "integrationType";

    /** Constant. */
    public static final String KEY_COMPONENT_NAME = "toolName";

    /** Constant. */
    public static final String KEY_ICON_PATH = "toolIconPath";

    /** Constant. */
    public static final String KEY_COPY_ICON = "uploadIcon";

    /** Constant. */
    public static final String KEY_GROUPNAME = "groupName";

    /** Constant. */
    public static final String KEY_DOC_FILE_PATH = "documentationFilePath";

    /** Constant. */
    public static final String KEY_DESCRIPTION = "toolDescription";

    /** Constant. */
    public static final String KEY_VERSION = "version";

    /** Constant. */
    public static final String KEY_LIMIT_INSTANCES = "limitInstallationInstances";

    /** Constant. */
    public static final String KEY_LIMIT_INSTANCES_COUNT = "limitInstallationInstancesNumber";

    /** Constant. */
    public static final String KEY_INTEGRATOR_NAME = "toolIntegratorName";

    /** Constant. */
    public static final String KEY_INTEGRATOR_EMAIL = "toolIntegratorE-Mail";

    /** Constant. */
    public static final String KEY_ENDPOINT_OUTPUTS = "outputs";

    /** Constant. */
    public static final String KEY_ENDPOINT_INPUTS = "inputs";

    /** Constant. */
    public static final String KEY_ENDPOINT_FILENAME = "endpointFileName";

    /** Constant. */
    public static final String KEY_INPUT_HANDLING = "inputHandling";

    /** Constant. */
    public static final String KEY_ENDPOINT_DATA_TYPE = "endpointDataType";

    /** Constant. */
    public static final String KEY_DEFAULT_INPUT_HANDLING = "defaultInputHandling";

    /** Constant. */
    public static final String KEY_DEFAULT_INPUT_EXECUTION_CONSTRAINT = "defaultInputExecutionConstraint";

    /** Constant. */
    public static final String KEY_PROPERTY_CREATE_CONFIG_FILE = "propertyCreateConfigFile";

    /** Constant. */
    public static final String KEY_ENDPOINT_FOLDER = "endpointFolder";

    /** Constant. */
    public static final String KEY_INPUT_EXECUTION_CONSTRAINT = "inputExecutionConstraint";

    /** Constant. */
    public static final String KEY_ENDPOINT_NAME = "endpointName";

    /** Constant. */
    public static final String KEY_PROPERTIES = "toolProperties";

    /** Constant. */
    public static final String KEY_PROPERTY_KEY = "propertyKey";

    /** Constant. */
    public static final String KEY_PROPERTY_DISPLAYNAME = "propertyDisplayName";

    /** Constant. */
    public static final String KEY_PROPERTY_DEFAULT_VALUE = "propertyDefaultValue";

    /** Constant. */
    public static final String KEY_PROPERTY_COMMENT = "propertyComment";

    /** Constant. */
    public static final String KEY_LAUNCH_SETTINGS = "launchSettings";

    /** Constant. */
    public static final String KEY_HOST = "host";

    /** Constant. */
    public static final String VALUE_LOCALHOST = "RCE";

    /** Constant. */
    public static final String[] VALID_DOCUMENTATION_EXTENSIONS = new String[] { "pdf", "txt" };

    /** Constant. */
    public static final String DOCS_DIR_NAME = "docs";

    /** Constant. */
    public static final String IS_ACTIVE = "isActive";


    private IntegrationConstants() {}
}
