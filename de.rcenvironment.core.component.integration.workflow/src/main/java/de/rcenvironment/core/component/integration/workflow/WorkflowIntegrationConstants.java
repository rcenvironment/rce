/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.workflow;

/**
 * Constants for the Integration of Workflows only.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public final class WorkflowIntegrationConstants {

    /** Constant. */
    public static final String WORKFLOW_INTEGRATOR_COMPONENT_ID_PREFIX = "de.rcenvironment.integration.workflow.";

    /** Constant. */
    public static final String[] COMPONENT_IDS = new String[] { WORKFLOW_INTEGRATOR_COMPONENT_ID_PREFIX };

 // This functionality will be added in a future release. 
 // K.Schaffert, 27.07.2022
//    /** Constant. */
//    public static final String KEY_WORKFLOW_IMAGE_FILE = "addImageFile";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTERS = "endpointAdapters";

    /** Constant. */
    public static final String KEY_INTERNAL_NAME = "internalName";

    /** Constant. */
    public static final String INTEGRATED_WORKFLOW_FILENAME = "workflow.wf";

    /** Constant. */
    public static final String VALUE_ENDPOINT_ADAPTER_OUTPUT = "OUTPUT";

    /** Constant. */
    public static final String VALUE_ENDPOINT_ADAPTER_INPUT = "INPUT";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_NODE_IDENTIFIER = "identifier";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_EXTERNAL_NAME = "externalName";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_INTERNAL_NAME = "internalName";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_INPUT_EXECUTION_CONSTRAINT = "inputExecutionConstraint";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_INPUT_HANDLING = "inputHandling";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_TYPE = "type";

    /** Constant. */
    public static final String KEY_ENDPOINT_ADAPTER_DATA_TYPE_KEY = "endpointDataType";

    /** Constant. */
    public static final String DEFAULT_GROUP_ID = "User Integrated Workflows";

    private WorkflowIntegrationConstants() {}
}
