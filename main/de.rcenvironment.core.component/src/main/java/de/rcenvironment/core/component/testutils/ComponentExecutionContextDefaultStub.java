/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.io.File;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Default mock for {@link ComponentExecutionContext}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionContextDefaultStub implements ComponentExecutionContext {

    private static final long serialVersionUID = -984226139258729791L;

    @Override
    public String getExecutionIdentifier() {
        return null;
    }

    @Override
    public String getInstanceName() {
        return null;
    }

    @Override
    public NodeIdentifier getNodeId() {
        return null;
    }

    @Override
    public NodeIdentifier getDefaultStorageNodeId() {
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
    public NodeIdentifier getWorkflowNodeId() {
        return null;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return null;
    }

    @Override
    public String getWorkflowInstanceName() {
        return null;
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


}
