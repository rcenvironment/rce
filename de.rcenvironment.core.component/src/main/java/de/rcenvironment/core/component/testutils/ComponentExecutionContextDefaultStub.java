/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.io.File;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Default mock for {@link ComponentExecutionContext}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionContextDefaultStub implements ComponentExecutionContext {

    /**
     * Default execution identifier.
     */
    public static final String COMP_EXE_ID = "comp-exe-id";

    /**
     * Default workflow execution identifier.
     */
    public static final String WF_EXE_ID = "wf-exe-id";

    /**
     * Default component instance name.
     */
    public static final String COMP_INSTANCE_NAME = "comp instance name";

    /**
     * Default workflow instance name.
     */
    public static final String WF_INSTANCE_NAME = "wf instance name";

    private static final long serialVersionUID = -984226139258729791L;

    @Override
    public String getExecutionIdentifier() {
        return COMP_EXE_ID;
    }

    @Override
    public ComponentExecutionIdentifier getExecutionIdentifierAsObject() {
        return new ComponentExecutionIdentifier(COMP_EXE_ID);
    }

    @Override
    public String getInstanceName() {
        return COMP_INSTANCE_NAME;
    }

    @Override
    public LogicalNodeId getNodeId() {
        return null;
    }

    @Override
    public LogicalNodeId getStorageNodeId() {
        return null;
    }

    @Override
    public LogicalNodeId getStorageNetworkDestination() {
        return null;
    }

    @Override
    public ComponentDescription getComponentDescription() {
        return null;
    }

    @Override
    public boolean isConnectedToEndpointDatumSenders() {
        return false;
    }

    @Override
    public Map<String, List<EndpointDatumRecipient>> getEndpointDatumRecipients() {
        return null;
    }

    @Override
    public LogicalNodeId getWorkflowNodeId() {
        return null;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return WF_EXE_ID;
    }

    @Override
    public String getWorkflowInstanceName() {
        return WF_INSTANCE_NAME;
    }

    @Override
    public WorkflowGraph getWorkflowGraph() {
        return null;
    }

    @Override
    public File getWorkingDirectory() {
        return null;
    }

    @Override
    public Long getInstanceDataManagementId() {
        return null;
    }

    @Override
    public Long getWorkflowInstanceDataManagementId() {
        return null;
    }

    @Override
    public Map<String, Long> getInputDataManagementIds() {
        return null;
    }

    @Override
    public Map<String, Long> getOutputDataManagementIds() {
        return null;
    }

    @Override
    public ServiceCallContext getServiceCallContext() {
        return null;
    }
}
