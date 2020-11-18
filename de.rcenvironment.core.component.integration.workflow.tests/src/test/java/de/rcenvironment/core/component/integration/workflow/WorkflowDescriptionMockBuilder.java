/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow;

import java.util.LinkedList;
import java.util.List;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

class WorkflowDescriptionMockBuilder {
    
    private final WorkflowDescription description = EasyMock.createMock(WorkflowDescription.class);
    
    private final List<WorkflowNode> workflowNodes = new LinkedList<>();
    
    public WorkflowDescriptionMockBuilder mockedControllerNode() {
        final LogicalNodeId nodeId = EasyMock.createMock(LogicalNodeId.class);
        EasyMock.replay(nodeId);
        EasyMock.expect(description.getControllerNode()).andStubReturn(nodeId);
        return this;
    }
    
    public WorkflowDescriptionMockBuilder controllerNode(LogicalNodeId nodeId) {
        EasyMock.expect(description.getControllerNode()).andStubReturn(nodeId);
        return this;
    }
    
    public WorkflowDescriptionMockBuilder withoutWorkflowNodes() {
        return this;
    }
    
    public WorkflowDescriptionMockBuilder withWorkflowNode(final WorkflowNode node) {
        this.workflowNodes.add(node);
        return this;
    }
    
    public WorkflowDescription build() {
        EasyMock.expect(description.clone()).andStubReturn(description);
        EasyMock.expect(description.getWorkflowNodes()).andStubReturn(this.workflowNodes);
        EasyMock.replay(description);
        return description;
    }

}
