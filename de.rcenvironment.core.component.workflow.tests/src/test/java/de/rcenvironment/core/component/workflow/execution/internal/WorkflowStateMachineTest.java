/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccessStub;
import de.rcenvironment.core.utils.incubator.StateChangeException;

/**
 * Tests for {@link WorkflowStateMachine} (uncompleted).
 *
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowStateMachineTest {

    /**
     * Tests the intial workflow state.
     */
    @Test
    public void testInitialWorkflowState() {
        WorkflowNode wfNodeMock1 = EasyMock.createStrictMock(WorkflowNode.class);
        EasyMock.expect(wfNodeMock1.isEnabled()).andStubReturn(true);
        EasyMock.replay(wfNodeMock1);
        WorkflowNode wfNodeMock2 = EasyMock.createStrictMock(WorkflowNode.class);
        EasyMock.expect(wfNodeMock2.isEnabled()).andStubReturn(true);
        EasyMock.replay(wfNodeMock2);
        List<WorkflowNode> wfNodeMocks = new ArrayList<>();
        wfNodeMocks.add(wfNodeMock1);
        wfNodeMocks.add(wfNodeMock2);
        WorkflowDescription clonedWfDescMock = EasyMock.createStrictMock(WorkflowDescription.class);
        WorkflowDescription wfDescMock = EasyMock.createStrictMock(WorkflowDescription.class);
        EasyMock.expect(wfDescMock.clone()).andStubReturn(clonedWfDescMock);
        EasyMock.expect(wfDescMock.getWorkflowNodes()).andStubReturn(wfNodeMocks);
        EasyMock.replay(wfDescMock);
        WorkflowExecutionContext wfExeCtxMock = EasyMock.createStrictMock(WorkflowExecutionContext.class);
        EasyMock.expect(wfExeCtxMock.getWorkflowDescription()).andStubReturn(wfDescMock);
        EasyMock.expect(wfExeCtxMock.getInstanceName()).andStubReturn("wf instance");
        EasyMock.expect(wfExeCtxMock.getExecutionIdentifier()).andStubReturn("wf-exe-id");
        EasyMock.replay(wfExeCtxMock);
        WorkflowStateMachineContext wfStateMachineCtxMock = EasyMock.createStrictMock(WorkflowStateMachineContext.class);
        EasyMock.expect(wfStateMachineCtxMock.getWorkflowExecutionContext()).andStubReturn(wfExeCtxMock);
        EasyMock.expect(wfStateMachineCtxMock.getServiceRegistryAccess()).andReturn(new ServiceRegistryAccessStub(false));
        EasyMock.replay(wfStateMachineCtxMock);
        WorkflowStateMachine machine = new WorkflowStateMachine(wfStateMachineCtxMock);
        assertEquals(WorkflowState.INIT, machine.getState());
    }

    /**
     * Tests if each {@link WorkflowStateMachineEvent} is covered by an {@link WorkflowStateMachine.EventProcessorEventProcessor}.
     * 
     * @throws StateChangeException on unexpected error
     */
    @Test
    public void testEventProcessorsInitialization() throws StateChangeException {
        @SuppressWarnings("deprecation") WorkflowStateMachine wfStateMachine = new WorkflowStateMachine();
        wfStateMachine.initializeEventProcessors();
        for (WorkflowStateMachineEventType eventType : EnumUtils.getEnumList(WorkflowStateMachineEventType.class)) {
            assertTrue(wfStateMachine.eventProcessors.containsKey(eventType));
        }
    }

}
