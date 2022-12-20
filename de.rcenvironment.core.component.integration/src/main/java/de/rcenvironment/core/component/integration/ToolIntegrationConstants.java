/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.util.UUID;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants for the ToolIntegrationWizard.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert
 */
public final class ToolIntegrationConstants {

    /** Constant. */
    public static final int CURRENT_TOOLINTEGRATION_VERSION = 1;

    /** Constant. */
    public static final String KEY_TOOL_INTEGRATION_VERSION = "toolIntegrationVersion";

    /** Constant. */
    public static final String KEY_TOOL_USE_ITERATION_DIRECTORIES = "useIterationDirectories";

    /** Constant. */
    public static final String KEY_COPY_TOOL_BEHAVIOUR = "copyToolBehavior";

    /** Constant. */
    public static final String VALUE_COPY_TOOL_BEHAVIOUR_NEVER = "never";

    /** Constant. */
    public static final String VALUE_COPY_TOOL_BEHAVIOUR_ONCE = "once";

    /** Constant. */
    public static final String VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS = "always";

    /** Constant. */
    public static final String KEY_TOOL_DELETE_WORKING_DIRECTORIES_BEHAVIOUR = "deleteWorkingDirectoryBehaviour";

    /** Constant. */
    public static final String KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS = "deleteWorkingDirectoriesAfterIteration";

    /** Constant. */
    public static final String KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE = "deleteWorkingDirectoriesAfterWorkflowExecution";

    /** Constant. */
    public static final String KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER = "deleteWorkingDirectoriesNever";

    /** Constant. */
    public static final String KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ITERATION = "deleteWorkingDirectoriesKeepOnErrorIteration";

    /** Constant. */
    public static final String KEY_TOOL_DELETE_WORKING_DIRECTORIES_KEEP_ON_ERROR_ONCE = "deleteWorkingDirectoriesKeepOnErrorOnce";

    /** Constant. */
    public static final String KEY_TOOL_DIRECTORY = "toolDirectory";

    /** Constant. */
    public static final String KEY_ROOT_WORKING_DIRECTORY = "rootWorkingDirectory";

    /** Constant. */
    public static final String KEY_COMMAND_SCRIPT_WINDOWS = "commandScriptWindows";

    /** Constant. */
    public static final String KEY_COMMAND_SCRIPT_LINUX = "commandScriptLinux";

    /** Constant. */
    public static final String KEY_PRE_SCRIPT = "preScript";

    /** Constant. */
    public static final String KEY_POST_SCRIPT = "postScript";

    /** Constant. */
    public static final String KEY_MOCK_SCRIPT = "imitationScript";

    /** Constant. */
    public static final String PLACEHOLDER_PREFIX = "${";

    /** Constant. */
    public static final String PLACEHOLDER_SUFFIX = "}";

    /** Constant. */
    public static final String KEY_ENDPOINT_IDENTIFIER = "endpointIdentifier";

    /** Constant. */
    public static final String KEY_ENDPOINT_DEFAULT_TYPE = "endpointDefaultDataType";

    /** Constant. */
    public static final String KEY_ENDPOINT_DATA_TYPES = "endpointDataTypes";

    /** Constant. */
    public static final String KEY_ENDPOINT_USAGE = "endpointUsage";

    /** Constant. */
    public static final String KEY_INPUT_HANDLING_OPTIONS = "inputinputHandlingOptions";

    /** Constant. */
    public static final String KEY_INPUT_EXECUTION_CONSTRAINT_OPTIONS = "inputinputExecutionConstraintOptions";

    /** Constant. */
    public static final String KEY_PROPERTY_CONFIG_FILENAME = "propertyConfigFilename";

    /** Constant. */
    public static final String KEY_MOCK_MODE_SUPPORTED = "imitationModeSupported";

    /** Constant. */
    public static final String KEY_VERIFICATION_TOKEN_LOCATION = "verificationKeyLocation";

    /** Constant. */
    public static final String KEY_VERIFICATION_TOKEN_RECIPIENTS = "verificationKeyEmailRecipients";

    /** Constant. */
    public static final String VERIFICATION_TOKEN_RECIPIENTS_SEPARATOR = ";";

    /** Constant. */
    public static final String DEFAULT_CONFIG_FILE_SUFFIX = ".conf";

    /** Constant. */
    public static final String PLACEHOLDER_INPUT_PREFIX = "in";

    /** Constant. */
    public static final String PLACEHOLDER_SEPARATOR = ":";

    /** Constant. */
    public static final String PLACEHOLDER_OUTPUT_PREFIX = "out";

    /** Constant. */
    public static final String PLACEHOLDER_PROPERTY_PREFIX = "prop";

    /** Constant. */
    public static final String PLACEHOLDER_DIRECTORY_PREFIX = "dir";

    /** Constant. */
    public static final String[] DIRECTORIES_PLACEHOLDERS_DISPLAYNAMES = new String[] { "Config dir", "Working dir",
        "Input dir", "Tool dir", "Output dir" };

    /** Constant. */
    public static final String[] DIRECTORIES_PLACEHOLDER = new String[] { "config", "working", "input", "tool", "output" };

    /** Constant. */
    public static final String PLACEHOLDER_EXIT_CODE = "exitCode";

    /** Constant. */
    public static final String KEY_COMMAND_SCRIPT_WINDOWS_ENABLED = "enableCommandScriptWindows";

    /** Constant. */
    public static final String KEY_COMMAND_SCRIPT_LINUX_ENABLED = "enableCommandScriptLinux";

    /** Constant. */
    public static final String DEFAULT_COMPONENT_GROUP_ID = "User Integrated Tools";

    /** Constant. */
    public static final String COMPONENT_INPUT_FOLDER_NAME = "Input";

    /** Constant. */
    public static final String COMPONENT_OUTPUT_FOLDER_NAME = "Output";

    /** Constant. */
    public static final String COMPONENT_CONFIG_FOLDER_NAME = "Config";

    /** Constant. */
    // TODO review: is there an actual need for this ID to be dynamic?
    public static final String COMMON_TOOL_INTEGRATION_CONTEXT_UID = UUID.randomUUID().toString() + "_COMMON";

    /** Constant. */
    public static final String CHOSEN_DELETE_TEMP_DIR_BEHAVIOR = "chosenDeleteTempDirBehavior";

    /** Constant. */
    public static final String NEW_WIZARD = "NEW_COMMON";

    /** Constant. */
    public static final String EDIT_WIZARD = "EDIT_COMMON";

    /** Constant. */
    public static final String DONT_CRASH_ON_NON_ZERO_EXIT_CODES = "dontCrashOnNonZeroExitCodes";

    /** Constant. */
    public static final String PLACEHOLDER_ADDITIONAL_PROPERTIES_PREFIX = "addProp";

    // tool publication mixed into tool integration is disabled for now; see Mantis #16044
    // public static final String TEMP_KEY_PUBLISH_COMPONENT = "publishComponent";

    /** Constant. */
    public static final String PUBLISHED_COMPONENTS_FILENAME = "published.conf";

    /** Constant. */
    public static final String KEY_SET_TOOL_DIR_AS_WORKING_DIR = "setToolDirAsWorkingDir";

    /** Constant. */
    public static final String TEMPLATE_PATH = "tools" + File.separator + "templates";

    /** Constant. */
    public static final String[] COMPONENT_IDS = new String[] { ComponentConstants.COMMON_INTEGRATED_COMPONENT_ID_PREFIX,
        "de.rcenvironment.core.component.integration.CommonToolIntegratorComponent" };

    /** Constant. */
    public static final String KEY_ENDPOINT_METADATA = "endpointMetaData";

    /** Constant. */
    public static final String KEY_ENDPOINT_DYNAMIC_INPUTS = "dynamicInputs";

    /** Constant. */
    public static final Object KEY_ENDPOINT_DYNAMIC_OUTPUTS = "dynamicOutputs";

    /** Constant. */
    public static final String KEY_TEMPLATE_NAME = "templateName";

    /** Constant. */
    @Deprecated
    public static final String KEY_LIMIT_INSTANCES_OLD = "limitInstalltionInstances";

    /** Constant. */
    public static final String KEY_KEEP_ON_FAILURE = "keepOnFailure";

    /** Constant. */
    public static final String DOCUMENTATION_CACHED_SUFFIX = "(C)";

    /** Constant. */
    public static final String KEY_ICON_HASH = "iconHash";

    /** Constant. */
    public static final String KEY_ICON_MODIFICATION_DATE = "iconModified";

    private ToolIntegrationConstants() {

    }
}
