/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionScheduler.State;
import de.rcenvironment.core.component.execution.internal.InternalTDImpl.InternalTDType;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;
import de.rcenvironment.core.component.testutils.ComponentExecutionContextMock;
import de.rcenvironment.core.component.testutils.EndpointDatumDefaultStub;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.testutils.NotAValueTDStub;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;

/**
 * Test cases for the {@link ComponentExecutionScheduler}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionSchedulerTest {

    private static final String EXPECTED = " expected";

    private static final String INPUT_1 = "input_1";

    private static final String INPUT_2 = "input_2";

    private static final String INPUT_3 = "input_3";

    private static final String INPUT_4 = "input_4";

    private static final String INPUT_5 = "input_5";

    private static final String INPUT_6 = "input_6";

    private static final String INPUT_7 = "input_7";

    private static final String OR_GROUP = "orGroup";

    private static final String AND_GROUP = "andGroup";

    private static final boolean CONNECTED = true;

    private static final boolean NOT_CONNECTED = false;

    /**
     * Set up: one input (single, required); one value and 'finish value' received. Expected: Scheduling state become
     * {@link State#PROCESS_INPUT_DATA} and {@link State#FINISHED}.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testSingleRequiredInputForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
    }

    /**
     * Set up: one input (single, required); two values received, which are queued. Expected: Failure event posted to
     * {@link ComponentStateMachine}.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testSingleRequiredInputForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);
        assertNewSchedulingStateAndSchedulingFailureEventPosted(capturedEvent);
    }

    /**
     * Set up: one input (queue, required); multiple values and 'finish value' received. Expected: Scheduling state become multiple times
     * {@link State#PROCESS_INPUT_DATA} and finally {@link State#FINISHED}.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testQueuedInputForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(1));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(2));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
    }

    /**
     * Set up: one input (constant, required); one value and 'finish value' received. Expected: Scheduling state become
     * {@link State#PROCESS_INPUT_DATA} and {@link State#FINISHED}.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testConstantInputForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
    }

    /**
     * Set up: one input (constant, required); two values received. Expected: Failure event posted to {@link ComponentStateMachine}.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testConstantInputForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);
        assertNewSchedulingStateAndSchedulingFailureEventPosted(capturedEvent);
    }

    /**
     * Setup: different kind of inputs; multiple values received. Expected: Appropriate scheduling.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testMultipleInputsForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_3, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_4, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_5, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.RequiredIfConnected, NOT_CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_6, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.NotRequired, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_7, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.NotRequired, NOT_CONNECTED));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend1 = new ArrayList<>();
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_3, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_3, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsToSend1);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsToSend1.get(0),
            endpointDatumsToSend1.get(1), endpointDatumsToSend1.get(2), endpointDatumsToSend1.get(4));

        List<EndpointDatum> endpointDatumsToSend2 = new ArrayList<>();
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_6, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_3, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_4, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_6, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsToSend2);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsToSend1.get(4),
            endpointDatumsToSend1.get(3), endpointDatumsToSend2.get(0), endpointDatumsToSend2.get(1));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
    }

    /**
     * Setup: different kind of inputs; multiple values received. Expected: Appropriate scheduling.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testHandlingIfInputValuesAreLeftAfterFinished() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0),
            endpointDatumsSent.get(1));
        assertSchedulingFailureEventPosted(capturedEvent);
    }

    /**
     * Setup: different kind of inputs; multiple values received. Expected: Appropriate scheduling.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testHandlingIfInputsAreRequiredButNotConnected() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, NOT_CONNECTED));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionSpecificComponentExecutionContextMock compExeCtxMock = new ExecutionSpecificComponentExecutionContextMock(inputMockInfos);
        ComponentExecutionScheduler compExeScheduler =
            new ComponentExecutionScheduler(createCompExeRelatedInstancesStub(compExeCtxMock,
                createComponentStateMachineMock(capturedEvent)));
        try {
            compExeScheduler.initialize(compExeCtxMock);
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("not connected"));
            assertTrue(true);
        }
    }

    /**
     * Set up: two inputs (constant, single; both required); one value each; one value to input of type single; reset value to one of them,
     * one value to single, one value to constant.
     * 
     * Expected: {@link State#PROCESS_INPUT_DATA} 1) after both of the inputs have received values for the first time, 2) after value at
     * input of type single was received, 3) after both of the inputs have received values after reset was performed
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testResetComponentForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        String executionId = UUID.randomUUID().toString();
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent, executionId);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));

        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);
        InternalTDImpl resetTD = new InternalTDImpl(InternalTDType.NestedLoopReset, resetCycleHops);
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, resetTD));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0),
            endpointDatumsSent.get(1));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0),
            endpointDatumsSent.get(2));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.RESET, compExeScheduler.getSchedulingState());
        assertEquals(resetTD, compExeScheduler.getResetDatum());
        compExeScheduler.enable();

        endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
    }

    /**
     * Set up: two inputs (constant, single; both required); reset value to one of them; reset value has wrong recepient's execution
     * identifier.
     * 
     * Expected: Failure due to wrong execution identifier
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testResetComponentForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(UUID.randomUUID().toString()).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);
        InternalTDImpl resetTD = new InternalTDImpl(InternalTDType.NestedLoopReset, resetCycleHops);
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, resetTD));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);
        assertSchedulingFailureEventPosted(capturedEvent);
    }

    /**
     * Set up: two inputs (constant, queue; both required); reset to outputs sent; reset value received at one of the inputs.
     * 
     * Expected: {@link State#LOOP_RESET} 1) after both of the reset values were received
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testResetLoopForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required, AND_GROUP, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required, AND_GROUP, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_3, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required, OR_GROUP, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_4, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required, OR_GROUP, CONNECTED));

        List<InputGroupMockInformation> inputGroupMockInfos = new ArrayList<>();
        inputGroupMockInfos.add(new InputGroupMockInformation(OR_GROUP, EndpointGroupDefinition.LogicOperation.Or));
        inputGroupMockInfos.add(new InputGroupMockInformation(AND_GROUP, EndpointGroupDefinition.LogicOperation.And, OR_GROUP));

        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        String executionId = UUID.randomUUID().toString();
        ComponentExecutionScheduler compExeScheduler =
            setUpExecutionScheduler(inputMockInfos, inputGroupMockInfos, capturedEvent, executionId);

        sendAndCheckSendingDataToLoopDriverComponent(compExeScheduler, capturedEvent);

        compExeScheduler.fetchEndpointDatums();
        List<String> identifiers = new ArrayList<>();
        identifiers.add(UUID.randomUUID().toString());
        identifiers.add(UUID.randomUUID().toString());
        assertFalse(compExeScheduler.isLoopResetRequested());
        compExeScheduler.addResetDataIdSent(identifiers.get(0));
        compExeScheduler.addResetDataIdSent(identifiers.get(1));
        assertTrue(compExeScheduler.isLoopResetRequested());

        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        for (String id : identifiers) {
            InternalTDImpl resetTD = new InternalTDImpl(InternalTDType.NestedLoopReset, id, resetCycleHops);
            endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, resetTD));
        }
        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.LOOP_RESET, compExeScheduler.getSchedulingState());
        compExeScheduler.enable();
        sendAndCheckSendingDataToLoopDriverComponent(compExeScheduler, capturedEvent);
    }

    private void sendAndCheckSendingDataToLoopDriverComponent(ComponentExecutionScheduler compExeScheduler,
        Capture<ComponentStateMachineEvent> capturedEvent) throws InterruptedException, ComponentExecutionException {

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_3, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0),
            endpointDatumsSent.get(2));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(1),
            endpointDatumsSent.get(2));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(3));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(4));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(5));
    }

    /**
     * Set up: two inputs (constant, queue; both required); reset to outputs sent; reset value received at one of the inputs, second value
     * has wrong identifier.
     * 
     * Expected: Failure due to wrong reset value identifier received
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testResetLoopForFailure() throws Exception {

    }

    /**
     * Set up: two inputs (constant, single; both required); failure value received at one of the inputs.
     * 
     * Expected: {@link State#FAILURE_FORWARD} 1) after failure values were received
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testFailureValueAtComponentForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        String executionId = UUID.randomUUID().toString();
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent, executionId);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();

        Queue<WorkflowGraphHop> hopsToTraverse = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        hopsToTraverse.add(workflowGraphHopMock);
        InternalTDImpl failureTD1 = new InternalTDImpl(InternalTDType.FailureInLoop, hopsToTraverse);
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, failureTD1));
        InternalTDImpl failureTD2 = new InternalTDImpl(InternalTDType.FailureInLoop, hopsToTraverse);
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, failureTD2));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FAILURE_FORWARD, compExeScheduler.getSchedulingState());
        assertEquals(failureTD1, compExeScheduler.getFailureDatum());
        compExeScheduler.enable();
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FAILURE_FORWARD, compExeScheduler.getSchedulingState());
        assertEquals(failureTD2, compExeScheduler.getFailureDatum());
    }

    /**
     * Set up: two inputs (constant, single; both required); failure value received at one of the inputs.
     * 
     * Expected: {@link State#FAILURE_FORWARD} 1) after failure values were received
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testFailureValueAtLoopDriverForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        String executionId = UUID.randomUUID().toString();
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent, executionId);
        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();

        Queue<WorkflowGraphHop> hopsToTraverse = new LinkedList<>();
        InternalTDImpl failureTD1 = new InternalTDImpl(InternalTDType.FailureInLoop, "id-1", hopsToTraverse, "11");
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, failureTD1));
        InternalTDImpl failureTD2 = new InternalTDImpl(InternalTDType.FailureInLoop, "id-2", hopsToTraverse, "21");
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, failureTD2));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, compExeScheduler.getSchedulingState());
        Map<String, EndpointDatum> endpointDatumsReturned = compExeScheduler.fetchEndpointDatums();
        assertEquals(2, endpointDatumsReturned.size());
        assertTrue(endpointDatumsReturned.get(INPUT_1).getValue() instanceof NotAValueTD);
        assertEquals("id-1", ((NotAValueTD) endpointDatumsReturned.get(INPUT_1).getValue()).getIdentifier());
        assertTrue(endpointDatumsReturned.get(INPUT_2).getValue() instanceof NotAValueTD);
        assertEquals("id-2", ((NotAValueTD) endpointDatumsReturned.get(INPUT_2).getValue()).getIdentifier());
    }

    /**
     * Set up: two inputs (constant, queue; both required); same parent group for both of them; three values.
     * 
     * Expected: {@link State#PROCESS_INPUT_DATA} 1) after each value received
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testOrGroupForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required, OR_GROUP, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required, OR_GROUP, CONNECTED));

        List<InputGroupMockInformation> inputGroupMockInfos = new ArrayList<>();
        inputGroupMockInfos.add(new InputGroupMockInformation(OR_GROUP, EndpointGroupDefinition.LogicOperation.Or));

        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ComponentExecutionScheduler compExeScheduler =
            setUpExecutionScheduler(inputMockInfos, inputGroupMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(1));
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(2));
    }

    /**
     * Set up: one input (queue; required); 'not a value' datum received.
     * 
     * Expected: {@link State#PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA} after value was received
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testNotAValueDataForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new NotAValueTDStub()));
        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0));
    }

    /**
     * Set up: one input (queue; required); 'not a value' datum received, which was sent or which was received before.
     * 
     * Expected: Failure because 'not a value' was received again or twice
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testNotAValueDataForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        String id = UUID.randomUUID().toString();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new NotAValueTDStub(id)));
        compExeScheduler.addNotAValueDatumSent(id);
        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertSchedulingFailureEventPosted(capturedEvent);

        capturedEvent.reset();
        compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        endpointDatumsSent.clear();
        id = UUID.randomUUID().toString();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new NotAValueTDStub(id)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new NotAValueTDStub(id)));
        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, compExeScheduler.getSchedulingState());
        checkEndpointDatumsSetSchedulerActiveAgain(compExeScheduler, compExeScheduler.fetchEndpointDatums(), endpointDatumsSent.get(0));

        assertSchedulingFailureEventPosted(capturedEvent);
    }

    /**
     * Set up: one input (queue; required); datum received: 1) NotAValue 2) Internal 3) whose data type is not compatible with input's data
     * type.
     * 
     * Expected: 1, 2: not failure; 3: failure
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testDataTypeCheck() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent
            .add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.FailureInLoop, new LinkedList<WorkflowGraphHop>(), "1")));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new NotAValueTDStub("id")));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, compExeScheduler.getSchedulingState());

        endpointDatumsSent.clear();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Matrix)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);

        assertSchedulingFailureEventPosted(capturedEvent);
    }

    /**
     * Tests if the {@link ComponentExecutionScheduler} only post events to the {@link ComponentStateMachine} if set to active.
     * 
     * @throws Exception on unexpected error
     */
    @Test
    public void testIdleMode() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent);
        compExeScheduler.disable();

        List<EndpointDatum> endpointDatumsSent = new ArrayList<>();
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsSent.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(compExeScheduler, endpointDatumsSent);
        assertNoNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.IDLING, compExeScheduler.getSchedulingState());

        compExeScheduler.disable();
        assertFalse(capturedEvent.hasCaptured());
        compExeScheduler.enable();
        assertNewSchedulingStateEventPosted(capturedEvent);
    }

    /**
     * Tests if the {@link ComponentExecutionScheduler} considers exclusively inputs with character of {@link EndpointCharacter#SAME_LOOP}
     * for finish detection if not outer loop input exists and the component is not a loop driver.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testFinishNoLoopDriverWithoutOuterLoopInputs() throws ComponentExecutionException {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.SAME_LOOP));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.SAME_LOOP));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler = setUpExecutionScheduler(inputMockInfos, capturedEvent, false, false);

        EndpointDatum endpointDatum1 = new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum1);
        assertNoNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.IDLING, compExeScheduler.getSchedulingState());
        EndpointDatum endpointDatum2 = new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum2);
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
        assertFalse(compExeScheduler.isEnabled());
    }
    
    /**
     * Tests if the {@link ComponentExecutionScheduler} considers exclusively inputs with character of {@link EndpointCharacter#SAME_LOOP}
     * for finish detection if component is a loop driver having outer loop inputs.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testFinishLoopDriverWithOuterLoopInputs() throws ComponentExecutionException {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.OUTER_LOOP));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.SAME_LOOP));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler =
            setUpExecutionScheduler(inputMockInfos, capturedEvent, true, false);

        EndpointDatum endpointDatum1 = new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum1);
        assertNoNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.IDLING, compExeScheduler.getSchedulingState());
        EndpointDatum endpointDatum2 = new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum2);
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
        assertFalse(compExeScheduler.isEnabled());
    }

    /**
     * Tests if the {@link ComponentExecutionScheduler} considers exclusively inputs with character of {@link EndpointCharacter#OUTER_LOOP}
     * for finish detection if at least one exists and the component is no loop driver.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testFinishNonDriverWithOuterLoopInputs() throws ComponentExecutionException {
        testFinishConsiderOuterLoopInputs(false);
    }
    
    /**
     * Tests if the {@link ComponentExecutionScheduler} considers exclusively inputs with character of {@link EndpointCharacter#OUTER_LOOP}
     * for finish detection if component is a nested loop driver.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testFinishNestedLoopDriver() throws ComponentExecutionException {
        testFinishConsiderOuterLoopInputs(true);
    }

    private void testFinishConsiderOuterLoopInputs(boolean isNestedLoopDriver) throws ComponentExecutionException {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.OUTER_LOOP));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.OUTER_LOOP));
        inputMockInfos.add(new InputMockInformation(INPUT_3, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, EndpointCharacter.SAME_LOOP));
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>(CaptureType.ALL);
        ComponentExecutionScheduler compExeScheduler =
            setUpExecutionScheduler(inputMockInfos, capturedEvent, isNestedLoopDriver, isNestedLoopDriver);

        EndpointDatum endpointDatum1 = new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum1);
        assertNoNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.IDLING, compExeScheduler.getSchedulingState());
        EndpointDatum endpointDatum3 = new EndpointDatumMock(INPUT_3, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum3);
        assertNoNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.IDLING, compExeScheduler.getSchedulingState());
        EndpointDatum endpointDatum2 = new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish));
        compExeScheduler.validateAndQueueEndpointDatum(endpointDatum2);
        assertNewSchedulingStateEventPosted(capturedEvent);
        assertEquals(State.FINISHED, compExeScheduler.getSchedulingState());
        assertFalse(compExeScheduler.isEnabled());
    }
    
    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        Capture<ComponentStateMachineEvent> capturedEvent)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, capturedEvent, UUID.randomUUID().toString());
    }
    
    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        Capture<ComponentStateMachineEvent> capturedEvent, boolean isLoopDriver, boolean isNestedLoopDriver)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, capturedEvent, UUID.randomUUID().toString(), isLoopDriver, isNestedLoopDriver);
    }

    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        List<InputGroupMockInformation> inputGroupMockInfos, Capture<ComponentStateMachineEvent> capturedEvent)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, inputGroupMockInfos, capturedEvent, UUID.randomUUID().toString());
    }

    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        Capture<ComponentStateMachineEvent> capturedEvent, String executionId, boolean isLoopDriver, boolean isNestedLoopDriver)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, new ArrayList<InputGroupMockInformation>(), capturedEvent, executionId, isLoopDriver,
            isNestedLoopDriver);
    }
    
    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        Capture<ComponentStateMachineEvent> capturedEvent, String executionId)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, new ArrayList<InputGroupMockInformation>(), capturedEvent, executionId);
    }

    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        List<InputGroupMockInformation> inputGroupMockInfos, Capture<ComponentStateMachineEvent> capturedEvent, String executionId)
        throws ComponentExecutionException {
        ExecutionSpecificComponentExecutionContextMock compExeCtxMock =
            new ExecutionSpecificComponentExecutionContextMock(executionId, inputMockInfos, inputGroupMockInfos);
        ComponentExecutionScheduler compExeScheduler =
            new ComponentExecutionScheduler(
                createCompExeRelatedInstancesStub(compExeCtxMock, createComponentStateMachineMock(capturedEvent)));
        compExeScheduler.bindTypedDatumService(new TypedDatumServiceDefaultStub());
        compExeScheduler.initialize(compExeCtxMock);
        compExeScheduler.enable();
        return compExeScheduler;
    }
    
    private ComponentExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        List<InputGroupMockInformation> inputGroupMockInfos, Capture<ComponentStateMachineEvent> capturedEvent, String executionId,
        boolean isLoopDriver, boolean isNestedLoopDriver) throws ComponentExecutionException {
        ExecutionSpecificComponentExecutionContextMock compExeCtxMock = new ExecutionSpecificComponentExecutionContextMock(
            executionId, inputMockInfos, inputGroupMockInfos, isLoopDriver, isNestedLoopDriver);
        ComponentExecutionScheduler compExeScheduler =
            new ComponentExecutionScheduler(
                createCompExeRelatedInstancesStub(compExeCtxMock, createComponentStateMachineMock(capturedEvent)));
        compExeScheduler.bindTypedDatumService(new TypedDatumServiceDefaultStub());
        compExeScheduler.initialize(compExeCtxMock);
        compExeScheduler.enable();
        return compExeScheduler;
    }

    private ComponentStateMachine createComponentStateMachineMock(Capture<ComponentStateMachineEvent> capturedEvent) {
        ComponentStateMachine stateMachineMock = EasyMock.createStrictMock(ComponentStateMachine.class);
        stateMachineMock.postEvent(EasyMock.capture(capturedEvent));
        EasyMock.expectLastCall().asStub();
        EasyMock.replay(stateMachineMock);
        return stateMachineMock;
    }

    private ComponentExecutionRelatedInstances createCompExeRelatedInstancesStub(ComponentExecutionContext compExeCtx,
        ComponentStateMachine compStateMachine) {
        ComponentExecutionRelatedInstances compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeCtx = compExeCtx;
        compExeRelatedInstances.compStateMachine = compStateMachine;
        return compExeRelatedInstances;
    }

    private void sendValuesToExecutionScheduler(ComponentExecutionScheduler compExeScheduler,
        final List<EndpointDatum> endpointDatumsSent) {
        for (EndpointDatum endpointDatum : endpointDatumsSent) {
            compExeScheduler.validateAndQueueEndpointDatum(endpointDatum);
        }
    }

    private void checkEndpointDatumsSetSchedulerActiveAgain(ComponentExecutionScheduler compExeScheduler,
        Map<String, EndpointDatum> endpointDatumsReturned,
        EndpointDatum... endpointDatumsExpected) {
        assertEquals(Arrays.asList(endpointDatumsExpected).size(), endpointDatumsReturned.size());
        for (EndpointDatum endpointDatum : endpointDatumsExpected) {
            assertTrue(endpointDatumsReturned.containsKey(endpointDatum.getInputName()));
            assertTrue(endpointDatumsReturned.containsValue(endpointDatum));
        }
        compExeScheduler.enable();
    }

    private void assertNoNewSchedulingStateEventPosted(Capture<ComponentStateMachineEvent> capturedEvent) {
        assertFalse(capturedEvent.hasCaptured());
    }

    private void assertNewSchedulingStateAndSchedulingFailureEventPosted(Capture<ComponentStateMachineEvent> capturedEvent) {
        assertTrue(capturedEvent.hasCaptured());
        assertEquals(2, capturedEvent.getValues().size());
        assertEquals(ComponentStateMachineEventType.NEW_SCHEDULING_STATE, capturedEvent.getValues().get(0).getType());
        assertEquals(ComponentStateMachineEventType.SCHEDULING_FAILED, capturedEvent.getValues().get(1).getType());
        capturedEvent.reset();
    }

    private void assertNewSchedulingStateEventPosted(Capture<ComponentStateMachineEvent> capturedEvent) {
        assertTrue(capturedEvent.hasCaptured());
        assertEquals(ComponentStateMachineEventType.NEW_SCHEDULING_STATE, capturedEvent.getValue().getType());
        capturedEvent.reset();
    }

    private void assertSchedulingFailureEventPosted(Capture<ComponentStateMachineEvent> capturedEvent) {
        assertTrue(capturedEvent.hasCaptured());
        assertEquals(ComponentStateMachineEventType.SCHEDULING_FAILED, capturedEvent.getValue().getType());
        capturedEvent.reset();
    }

    /**
     * Mock for arbitrary {@link TypedDatum} objects.
     * 
     * @author Doreen Seider
     */
    private final class TypedDatumMock implements TypedDatum {

        private final DataType dataType;

        private TypedDatumMock(DataType dataType) {
            this.dataType = dataType;
        }

        @Override
        public DataType getDataType() {
            return dataType;
        }

    }

    /**
     * Mock for {@link EndpointDatum} object, which mocks only scheduling-related methods.
     * 
     * @author Doreen Seider
     */
    private final class EndpointDatumMock extends EndpointDatumDefaultStub {

        private final String inputName;

        private final TypedDatum value;

        private EndpointDatumMock(String inputName, TypedDatum value) {
            this.inputName = inputName;
            this.value = value;
        }

        @Override
        public String getInputName() {
            return inputName;
        }

        @Override
        public TypedDatum getValue() {
            return value;
        }

        @Override
        public EndpointDatumRecipient getEndpointDatumRecipient() {
            return EndpointDatumRecipientFactory.createEndpointDatumRecipient(inputName, null, null, null);
        }

    }

    /**
     * Mock for {@link ComponentExecutionContext} objects, which mocks only scheduling-related methods.
     * 
     * @author Doreen Seider
     */
    private final class ExecutionSpecificComponentExecutionContextMock extends ComponentExecutionContextMock {

        private static final long serialVersionUID = 6234658796478889718L;

        private final List<InputMockInformation> inputMockInformations;

        private final List<InputGroupMockInformation> inputGroupMockInformations;

        private final boolean isLoopDriver;

        private final boolean isNestedLoopDriver;

        private ExecutionSpecificComponentExecutionContextMock(List<InputMockInformation> inputMockInformations) {
            this(inputMockInformations, new ArrayList<InputGroupMockInformation>());
        }

        private ExecutionSpecificComponentExecutionContextMock(String executionId, List<InputMockInformation> inputMockInformations,
            List<InputGroupMockInformation> inputGroupMockInformations, boolean isLoopDriver, boolean isNestedLoopDriver) {
            super(executionId);
            this.inputMockInformations = inputMockInformations;
            this.inputGroupMockInformations = inputGroupMockInformations;
            this.isLoopDriver = isLoopDriver;
            this.isNestedLoopDriver = isNestedLoopDriver;
        }

        private ExecutionSpecificComponentExecutionContextMock(String executionId, List<InputMockInformation> inputMockInformations,
            List<InputGroupMockInformation> inputGroupMockInformations) {
            this(executionId, inputMockInformations, inputGroupMockInformations, false, false);
        }

        private ExecutionSpecificComponentExecutionContextMock(List<InputMockInformation> inputMockInformations,
            List<InputGroupMockInformation> inputGroupMockInformations) {
            super();
            this.inputMockInformations = inputMockInformations;
            this.inputGroupMockInformations = inputGroupMockInformations;
            this.isLoopDriver = false;
            this.isNestedLoopDriver = false;
        }

        @Override
        public ComponentDescription getComponentDescription() {
            ComponentDescription componentDescriptionMock = EasyMock.createStrictMock(ComponentDescription.class);

            ComponentInterface compInterfaceMock = EasyMock.createStrictMock(ComponentInterface.class);
            EasyMock.expect(compInterfaceMock.getIsLoopDriver()).andStubReturn(isLoopDriver);
            EasyMock.replay(compInterfaceMock);
            EasyMock.expect(componentDescriptionMock.getComponentInterface()).andStubReturn(compInterfaceMock);

            ConfigurationDescription configDescMock = EasyMock.createStrictMock(ConfigurationDescription.class);
            EasyMock.expect(configDescMock.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP))
                .andStubReturn(String.valueOf(isNestedLoopDriver));
            EasyMock.replay(configDescMock);

            EasyMock.expect(componentDescriptionMock.getConfigurationDescription()).andStubReturn(configDescMock);

            EasyMock.expect(componentDescriptionMock.getInputDescriptionsManager())
                .andStubReturn(InputDescriptionManagerMockFactory.createInputDescriptionManagerMock(inputMockInformations,
                    inputGroupMockInformations));
            EasyMock.replay(componentDescriptionMock);
            return componentDescriptionMock;
        }

    }

}
