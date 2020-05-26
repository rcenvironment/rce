/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.model.api.ComponentColor;
import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.api.ComponentSize;

/**
 * Class holding component constants.
 * 
 * @author Jens Ruehmkorf
 * @author Doreen Seider
 * @author Heinrich Wendel
 */
public final class ComponentConstants {

    /**
     * Prefix for component identifiers.
     */
    public static final String COMPONENT_IDENTIFIER_PREFIX = "de.rcenvironment.";

    /**
     * Key to identify a created component controller instance at the service registry.
     */
    public static final String COMP_INSTANCE_ID_KEY = "rce.component.controller.instance";

    /**
     * Key to identify a created component context controller instance at the service registry.
     */
    public static final String COMP_CONTEXT_INSTANCE_ID_KEY = "rce.component.workflow.instance";

    /**
     * Separator used in component controller instance key.
     */
    public static final String COMPONENT_ID_SEPARATOR = "_";

    /**
     * Key for full component's identifier.
     */
    public static final String COMPONENT_ID_KEY = "rce.component.id";

    /**
     * Key for full qualified name of the component implementing class.
     */
    public static final String COMPONENT_CLASS_KEY = "rce.component.class";
    
    /**
     * Key for componentValidator id.
     */
    public static final String COMPONENT_VALIDATOR_ID_KEY = "rce.component.validator.identifier";

    /** Key for component's name. */
    public static final String COMPONENT_NAME_KEY = "rce.component.name";

    /** Key for component's group. */
    public static final String COMPONENT_NAME_GROUP = "rce.component.group";

    /**
     * Key used within the properties of the component to define its version.
     */
    public static final String VERSION_DEF_KEY = "rce.component.version";

    /**
     * Key that specifies the location of the 16x16px icon for the component to show in the GUI.
     */
    public static final String ICON_16_KEY = "rce.component.icon-16";

    /**
     * Key that specifies the location of the 24x24px icon for the component to show in the GUI.
     */
    public static final String ICON_24_KEY = "rce.component.icon-24";

    /**
     * Key that specifies the location of the 32x32px icon for the component to show in the GUI.
     */
    public static final String ICON_32_KEY = "rce.component.icon-32";

    /**
     * Key that specifies the bundle in which the icons can be found. This is an optional key.
     */
    public static final String COMPONENT_ICON_BUNDLE_NAME_KEY = "rce.component.icon.bundlename";

    /**
     * Key for component's indefinite data type behavior.
     */
    public static final String COMPONENT_CAN_HANDLE_NAV_INPUT_DATA_TYPES = "rce.component.canHandleNotAValueDataTypes";

    /**
     * Key that specifies if a loop driver supports discard of inner loop runs.
     */
    public static final String LOOP_DRIVER_SUPPORTS_DISCARD = "rce.component.loopDriverSupportsDiscard";
    
    /**
     * Key for component's color.
     */
    public static final String COMPONENT_COLOR_KEY = "rce.component.color";

    /**
     * Key for component's shape.
     */
    public static final String COMPONENT_SHAPE_KEY = "rce.component.shape";

    /**
     * Key for component's size.
     */
    public static final String COMPONENT_SIZE_KEY = "rce.component.size";

    /**
     * Key that specifies if the component should be drawn small or not..
     */
    public static final String IS_SMALL_COMPONENT = "rce.component.small";

    /**
     * Key used within the properties of the component to define inputs as a comma separated list.
     */
    public static final String INPUTS_DEF_KEY = "rce.component.inputs";

    /**
     * Key used within the properties of the component to define outputs as a comma separated list.
     */
    public static final String OUTPUTS_DEF_KEY = "rce.component.outputs";

    /**
     * Key used within the properties of the component to define possible configuration keys as a comma separated list.
     */
    public static final String CONFIGURATION_DEF_KEY = "rce.component.configuration";

    /**
     * The entry of the Manifest indicating that the bundle provides at least one integrated {@link Component}.
     */
    public static final String MANIFEST_ENTRY_RCE_COMPONENT = "RCE-Component";

    /**
     * Manifest header key for definition of additional (regarding to an existing component) configuration definitions.
     */
    public static final String MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_CONFIGURATION = "RCE-ComponentExtension-Configuration";

    /**
     * Manifest header key for definition of additional (regarding to an existing component) input meta data definitions.
     */
    public static final String MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_INPUT_META_DATA = "RCE-ComponentExtension-InputMetaData";

    /**
     * Manifest header key for definition of additional (regarding to an existing component) output meta data definitions.
     */
    public static final String MANIFEST_ENTRY_RCE_COMPONENT_EXTENSION_OUTPUT_META_DATA = "RCE-ComponentExtension-OutputMetaData";

    /**
     * Separator in notification ids.
     */
    public static final String NOTIFICATION_ID_SEPARATOR = ":";

    /**
     * Prefix for identifier of output notifications.
     */
    public static final String OUTPUT_NOTIFICATION_ID_PREFIX = "rce.component.output:";

    /**
     * Notification identifier for state notifications.
     */
    public static final String STATE_NOTIFICATION_ID_PREFIX = "rce.component.state:";

    /**
     * Notification identifier for notifications about number or runs.
     */
    public static final String ITERATION_COUNT_NOTIFICATION_ID_PREFIX = "rce.component.noofruns:";

    /**
     * Notification identifier for finshed state notifications.
     */
    public static final String FINISHED_STATE_NOTIFICATION_ID_PREFIX = "rce.component.state.finished:";

    /**
     * Notification identifier for falied state notifications.
     */
    public static final String FAILED_STATE_NOTIFICATION_ID_PREFIX = "rce.component.state.failed:";

    /**
     * Notification identifier for input values notifications.
     */
    public static final String NOTIFICATION_ID_PREFIX_PROCESSED_INPUT = "rce.component.input:";

    /** Substring of identifier of placeholder component used if a given one is not available. */
    public static final String PLACEHOLDER_COMPONENT_IDENTIFIER_CLASS = "de.rcenvironment.rce.component.Placeholder_";

    /** Group name for unknown components. */
    public static final String COMPONENT_GROUP_UNKNOWN = "Other";

    /** Group name for unknown components. */
    public static final String COMPONENT_GROUP_TEST = "Test";

    /** usage type 'required': input value must be provided by previous component. */
    public static final String INPUT_USAGE_TYPE_REQUIRED = "required";

    /** usage type 'initial': input value must be provided at least once by previous component. */
    public static final String INPUT_USAGE_TYPE_INITIAL = "initial";

    /**
     * usage type 'optional': input value can be provided by previous component but doesn't have to.
     */
    public static final String INPUT_USAGE_TYPE_OPTIONAL = "optional";

    /** usage types for dynamic inputs. */
    public static final String[] INPUT_USAGE_TYPES = { INPUT_USAGE_TYPE_REQUIRED, INPUT_USAGE_TYPE_INITIAL, INPUT_USAGE_TYPE_OPTIONAL };

    /** meta data key for defining usage of dynamic inputs. */
    public static final String METADATAKEY_INPUT_USAGE = "usage";

    /** meta data key for defining scheduling attribute. */
    public static final String INPUT_METADATA_KEY_INPUT_DATUM_HANDLING = "inputHandling_73b1056e";

    /** meta data key for defining scheduling attribute. */
    public static final String INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT = "inputExecutionConstraint_4aae3eea";

    /** Constant. */
    public static final String KEY_NAME = "name";

    /** Constant. */
    public static final String KEY_DATATYPE = "dataType";

    /** Constant. */
    public static final String KEY_DEFAULT_VALUE = "defaultValue";

    /** Constant for ID separation. */
    public static final String ID_SEPARATOR = "/";

    /** Constant. */
    public static final String CONFIG_KEY_ENABLE_INPUT_TAB = "enableDeprecatedInputTab";

    /** Constant. */
    public static final String CONFIG_KEY_STORE_DATA_ITEM = "storeComponentHistoryData";

    /**
     * Constant. Note: Semantically, it is related to tool integration, but as it is needed in {@link ComponentStateMachine} class, it is
     * put here.
     */
    public static final String COMPONENT_CONFIG_KEY_IS_MOCK_MODE = "isImitationMode";

    /**
     * Constant. Note: Semantically, it is related to tool integration, but as it is needed in {@link ComponentStateMachine} class, it is
     * put here.
     */
    public static final String COMPONENT_CONFIG_KEY_REQUIRES_OUTPUT_APPROVAL = "requiresOutputApproval";

    /** Constant. */
    public static final ComponentColor COMPONENT_COLOR_STANDARD = ComponentColor.YELLOW;

    /** Constant. */
    public static final ComponentSize COMPONENT_SIZE_STANDARD = ComponentSize.MEDIUM;

    /** Constant. */
    public static final ComponentShape COMPONENT_SHAPE_STANDARD = ComponentShape.SQUARE;

    /** Finished component states. */
    public static final List<ComponentState> FININISHED_COMPONENT_STATES = new ArrayList<>();
    
    /** Failed component states. */
    public static final List<ComponentState> FAILED_COMPONENT_STATES = new ArrayList<>();

    /** Final component states. */
    public static final List<ComponentState> FINAL_COMPONENT_STATES = new ArrayList<>();

    /** Final component states. */
    public static final List<ComponentState> FINAL_COMPONENT_STATES_WITH_DISPOSED = new ArrayList<>();

    static {
        FININISHED_COMPONENT_STATES.add(ComponentState.FINISHED);
        FININISHED_COMPONENT_STATES.add(ComponentState.FINISHED_WITHOUT_EXECUTION);
        FAILED_COMPONENT_STATES.add(ComponentState.FAILED);
        FAILED_COMPONENT_STATES.add(ComponentState.RESULTS_REJECTED);
        FINAL_COMPONENT_STATES.addAll(FININISHED_COMPONENT_STATES);
        FINAL_COMPONENT_STATES.addAll(FAILED_COMPONENT_STATES);
        FINAL_COMPONENT_STATES.add(ComponentState.CANCELED);
        FINAL_COMPONENT_STATES_WITH_DISPOSED.addAll(FINAL_COMPONENT_STATES);
        FINAL_COMPONENT_STATES_WITH_DISPOSED.add(ComponentState.DISPOSED);
    }

    /** Private Constructor. */
    private ComponentConstants() {
        // NOP
    }
}
