/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionControllerImpl.ComponentStateMachine;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionControllerImpl.ComponentStateMachineEvent;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionControllerImpl.ComponentStateMachineEventType;
import de.rcenvironment.core.component.execution.internal.ExecutionScheduler.State;
import de.rcenvironment.core.component.execution.internal.InternalTDImpl.InternalTDType;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;
import de.rcenvironment.core.component.testutils.ComponentExecutionContextMock;
import de.rcenvironment.core.component.testutils.EndpointDatumDefaultStub;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.testutils.TypedDatumFactoryDefaultStub;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;

/**
 * Test cases for the {@link ExecutionScheduler}.
 * 
 * @author Doreen Seider
 */
public class ExecutionSchedulerTest {

    private static final String EXPECTED = " expected";

    private static final int SLEEP_INTERVAL_MSEC = 100;

    private static final int TEST_TIMEOUT_MSEC = 3000;

    private static final String INPUT_1 = "input_1";

    private static final String INPUT_2 = "input_2";

    private static final String INPUT_3 = "input_3";

    private static final String INPUT_4 = "input_4";

    private static final String INPUT_5 = "input_5";

    private static final String INPUT_6 = "input_6";

    private static final String INPUT_7 = "input_6";
    
    private static final String OR_GROUP = "orGroup";
    
    private static final String AND_GROUP = "andGroup";

    private static final boolean CONNECTED = true;

    private static final boolean NOT_CONNECTED = false;

    /**
     * Set up: one input (single, required); one value and 'finish value' received. Expected: Scheduling state become
     * {@link State#PROCESS_INPUT_DATA} and {@link State#FINISHED}.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testSingleRequiredInputForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0));
        assertEquals(State.FINISHED, executionScheduler.getSchedulingState());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Set up: one input (single, required); two values received, which are queued. Expected: Failure event posted to
     * {@link ComponentStateMachine}.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testSingleRequiredInputForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);
        Thread.sleep(SLEEP_INTERVAL_MSEC);
        assertEquals(ComponentStateMachineEventType.SCHEDULING_FAILED, capturedEvent.getValue().getType());

        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Set up: one input (queue, required); multiple values and 'finish value' received. Expected: Scheduling state become multiple times
     * {@link State#PROCESS_INPUT_DATA} and finally {@link State#FINISHED}.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testQueuedInputForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(1));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(2));
        assertEquals(State.FINISHED, executionScheduler.getSchedulingState());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Set up: one input (constant, required); one value and 'finish value' received. Expected: Scheduling state become
     * {@link State#PROCESS_INPUT_DATA} and {@link State#FINISHED}.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testConstantInputForSuccess() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0));
        assertEquals(State.FINISHED, executionScheduler.getSchedulingState());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Set up: one input (constant, required); two values received. Expected: Failure event posted to {@link ComponentStateMachine}.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testConstantInputForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);
        Thread.sleep(SLEEP_INTERVAL_MSEC);
        assertEquals(ComponentStateMachineEventType.SCHEDULING_FAILED, capturedEvent.getValue().getType());

        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Setup: different kind of inputs; multiple values received. Expected: Appropriate scheduling.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
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
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend1 = new ArrayList<>();
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_3, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_3, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend1.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend1);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend1.get(0), endpointDatumsToSend1.get(1),
            endpointDatumsToSend1.get(2), endpointDatumsToSend1.get(4));

        List<EndpointDatum> endpointDatumsToSend2 = new ArrayList<>();
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_6, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_3, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_4, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend2.add(new EndpointDatumMock(INPUT_5, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend2);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend1.get(4), endpointDatumsToSend1.get(3),
            endpointDatumsToSend2.get(0), endpointDatumsToSend2.get(1));
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.FINISHED, executionScheduler.getSchedulingState());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Setup: different kind of inputs; multiple values received. Expected: Appropriate scheduling.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testHandlingIfInputValuesAreLeftAfterFinished() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new InternalTDImpl(InternalTDType.WorkflowFinish)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new InternalTDImpl(InternalTDType.WorkflowFinish)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0), endpointDatumsToSend.get(1));
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        try {
            executionScheduler.getSchedulingState();
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("not processed"));
            assertTrue(true);
        }
        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Setup: different kind of inputs; multiple values received. Expected: Appropriate scheduling.
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testHandlingIfInputsAreRequiredButNotConnected() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required, NOT_CONNECTED));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionSpecificComponentExecutionContextMock compExeCtxMock = new ExecutionSpecificComponentExecutionContextMock(inputMockInfos);
        ExecutionScheduler executionScheduler = new ExecutionScheduler(compExeCtxMock, endpointDatumsReceived,
            createComponentStateMachineMock(capturedEvent));
        try {
            executionScheduler.initialize(compExeCtxMock);
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("not connected"));
            assertTrue(true);
        }

        checkForStateMachineEvents(capturedEvent);
    }

    /**
     * Set up: two inputs (constant, single; both required); one value each; one value to input of type single; reset value to one of them,
     * one value to single, one value to constant.
     * 
     * Expected: {@link State#PROCESS_INPUT_DATA} 1) after both of the inputs have received values for the first time, 2) after value at
     * input of type single was received, 3) after both of the inputs have received values after reset was performed
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testResetComponentForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        String executionId = UUID.randomUUID().toString();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent, executionId);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));

        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);
        InternalTDImpl resetTD = new InternalTDImpl(InternalTDType.NestedLoopReset, resetCycleHops);
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, resetTD));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0), endpointDatumsToSend.get(1));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0), endpointDatumsToSend.get(2));
        assertEquals(State.RESET, executionScheduler.getSchedulingState());
        assertEquals(resetTD, executionScheduler.getResetDatum());

        endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }

    /**
     * Set up: two inputs (constant, single; both required); reset value to one of them; reset value has wrong recepient's execution
     * identifier.
     * 
     * Expected: Failure due to wrong execution identifier
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testResetComponentForFailure() throws Exception {

        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(UUID.randomUUID().toString()).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);
        InternalTDImpl resetTD = new InternalTDImpl(InternalTDType.NestedLoopReset, resetCycleHops);
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, resetTD));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        try {
            executionScheduler.getSchedulingState();
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("is not the final recipient"));
            assertTrue(true);
        }
    }
    
    /**
     * Set up: two inputs (constant, queue; both required); reset to outputs sent; reset value received at one of the inputs.
     * 
     * Expected: {@link State#LOOP_RESET} 1) after both of the reset values were received
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
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
        
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        String executionId = UUID.randomUUID().toString();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, inputGroupMockInfos, endpointDatumsReceived,
            capturedEvent, executionId);

        sendAndCheckSendingDataToLoopDriverComponent(executionScheduler, endpointDatumsReceived);
        
        executionScheduler.getEndpointDatums();
        List<String> identifiers = new ArrayList<>();
        identifiers.add(UUID.randomUUID().toString());
        identifiers.add(UUID.randomUUID().toString());
        assertFalse(executionScheduler.isLoopResetRequested());
        executionScheduler.addResetDataIdSent(identifiers.get(0));
        executionScheduler.addResetDataIdSent(identifiers.get(1));
        assertTrue(executionScheduler.isLoopResetRequested());

        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);
        
        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        for (String id : identifiers) {
            InternalTDImpl resetTD = new InternalTDImpl(InternalTDType.NestedLoopReset, id, resetCycleHops);
            endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, resetTD));
        }
        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);
        
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.LOOP_RESET, executionScheduler.getSchedulingState());
        
        sendAndCheckSendingDataToLoopDriverComponent(executionScheduler, endpointDatumsReceived);

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }
    
    private void sendAndCheckSendingDataToLoopDriverComponent(ExecutionScheduler executionScheduler,
        BlockingDeque<EndpointDatum> endpointDatumsReceived) throws InterruptedException, ComponentExecutionException {
        
        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_3, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_4, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);
        
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0), endpointDatumsToSend.get(2));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(1), endpointDatumsToSend.get(2));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(3));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(4));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(5));
    }
    
    /**
     * Set up: two inputs (constant, queue; both required); reset to outputs sent; reset value received at one of the inputs, second value
     * has wrong identifier.
     * 
     * Expected: Failure due to wrong reset value identifier received
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testResetLoopForFailure() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        String executionId = UUID.randomUUID().toString();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent, executionId);

        String id = UUID.randomUUID().toString();
        executionScheduler.addResetDataIdSent(id);
        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();

        Queue<WorkflowGraphHop> resetCycleHops = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        resetCycleHops.add(workflowGraphHopMock);
        
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2,
            new InternalTDImpl(InternalTDType.NestedLoopReset, id, resetCycleHops)));
        
        executionScheduler.addResetDataIdSent(UUID.randomUUID().toString());
        
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2,
            new InternalTDImpl(InternalTDType.NestedLoopReset, UUID.randomUUID().toString(), resetCycleHops)));
        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        try {
            executionScheduler.getSchedulingState();
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("wrong identifier"));
            assertTrue(true);
        }
        
    }
    
    /**
     * Set up: two inputs (constant, single; both required); failure value received at one of the inputs.
     * 
     * Expected: {@link State#FAILURE_FORWARD} 1) after failure values were received
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testFailureValueAtComponentForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        String executionId = UUID.randomUUID().toString();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent, executionId);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();

        Queue<WorkflowGraphHop> hopsToTraverse = new LinkedList<>();
        WorkflowGraphHop workflowGraphHopMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(workflowGraphHopMock.getHopExecutionIdentifier()).andReturn(executionId).anyTimes();
        EasyMock.replay(workflowGraphHopMock);
        hopsToTraverse.add(workflowGraphHopMock);
        InternalTDImpl failureTD1 = new InternalTDImpl(InternalTDType.FailureInLoop, hopsToTraverse);
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, failureTD1));
        InternalTDImpl failureTD2 = new InternalTDImpl(InternalTDType.FailureInLoop, hopsToTraverse);
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, failureTD2));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.FAILURE_FORWARD, executionScheduler.getSchedulingState());
        assertEquals(failureTD1, executionScheduler.getFailureDatum());
        assertEquals(State.FAILURE_FORWARD, executionScheduler.getSchedulingState());
        assertEquals(failureTD2, executionScheduler.getFailureDatum());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }
    
    /**
     * Set up: two inputs (constant, single; both required); failure value received at one of the inputs.
     * 
     * Expected: {@link State#FAILURE_FORWARD} 1) after failure values were received
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testFailureValueAtLoopDriverForSuccess() throws Exception {
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Single,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        String executionId = UUID.randomUUID().toString();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent, executionId);
        executionScheduler.setTypedDatumFactory(new TypedDatumFactoryDefaultStub());
        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();

        Queue<WorkflowGraphHop> hopsToTraverse = new LinkedList<>();
        InternalTDImpl failureTD1 = new InternalTDImpl(InternalTDType.FailureInLoop, "id-1", hopsToTraverse, "11");
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, failureTD1));
        InternalTDImpl failureTD2 = new InternalTDImpl(InternalTDType.FailureInLoop, "id-2", hopsToTraverse, "21");
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, failureTD2));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.IDLING, executionScheduler.getSchedulingState());
        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, executionScheduler.getSchedulingState());
        Map<String, EndpointDatum> endpointDatumsReturned = executionScheduler.getEndpointDatums();
        assertEquals(2, endpointDatumsReturned.size());
        assertTrue(endpointDatumsReturned.get(INPUT_1).getValue() instanceof NotAValueTD);
        assertEquals("id-1", ((NotAValueTD) endpointDatumsReturned.get(INPUT_1).getValue()).getIdentifier());
        assertTrue(endpointDatumsReturned.get(INPUT_2).getValue() instanceof NotAValueTD);
        assertEquals("id-2", ((NotAValueTD) endpointDatumsReturned.get(INPUT_2).getValue()).getIdentifier());

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }
    
    /**
     * Set up: two inputs (constant, queue; both required); same parent group for both of them; three values.
     * 
     * Expected: {@link State#PROCESS_INPUT_DATA} 1) after each value received
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testOrGroupForSuccess() throws Exception {
        
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Constant,
            EndpointDefinition.InputExecutionContraint.Required, OR_GROUP, CONNECTED));
        inputMockInfos.add(new InputMockInformation(INPUT_2, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required, OR_GROUP, CONNECTED));
        
        List<InputGroupMockInformation> inputGroupMockInfos = new ArrayList<>();
        inputGroupMockInfos.add(new InputGroupMockInformation(OR_GROUP, EndpointGroupDefinition.LogicOperation.Or));
        
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, inputGroupMockInfos, endpointDatumsReceived,
            capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_2, new TypedDatumMock(DataType.Float)));

        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(1));
        assertEquals(State.PROCESS_INPUT_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(2));

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }
    
    /**
     * Set up: one input (queue; required); 'not a value' datum received.
     * 
     * Expected: {@link State#PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA} after value was received
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testNotAValueDataForSuccess() throws Exception {
        
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new NotAValueTDMock()));
        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0));

        checkForStateMachineEvents(capturedEvent);
        tearDownExecutionScheduler(executionScheduler);
    }
    
    /**
     * Set up: one input (queue; required); 'not a value' datum received, which was sent or which was received before.
     * 
     * Expected: Failure because 'not a value' was received again or twice
     * 
     * @throws Exception on test failure
     */
    @Test(timeout = TEST_TIMEOUT_MSEC)
    public void testNotAValueDataForFailure() throws Exception {
        
        List<InputMockInformation> inputMockInfos = new ArrayList<>();
        inputMockInfos.add(new InputMockInformation(INPUT_1, EndpointDefinition.InputDatumHandling.Queue,
            EndpointDefinition.InputExecutionContraint.Required));
        final BlockingDeque<EndpointDatum> endpointDatumsReceived = new LinkedBlockingDeque<>();
        Capture<ComponentStateMachineEvent> capturedEvent = new Capture<>();
        ExecutionScheduler executionScheduler = setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent);

        List<EndpointDatum> endpointDatumsToSend = new ArrayList<>();
        String id = UUID.randomUUID().toString();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new NotAValueTDMock(id)));
        executionScheduler.addNotAValueDatumSent(id);
        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);

        try {
            executionScheduler.getSchedulingState();
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("own 'not a value' datum"));
            assertTrue(true);
        }
        
        endpointDatumsToSend = new ArrayList<>();
        id = UUID.randomUUID().toString();
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new NotAValueTDMock(id)));
        endpointDatumsToSend.add(new EndpointDatumMock(INPUT_1, new NotAValueTDMock(id)));
        sendValuesToExecutionScheduler(endpointDatumsReceived, endpointDatumsToSend);
        
        assertEquals(State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA, executionScheduler.getSchedulingState());
        checkEndpointDatums(executionScheduler.getEndpointDatums(), endpointDatumsToSend.get(0));
        
        try {
            executionScheduler.getSchedulingState();
            fail(ComponentExecutionException.class.getSimpleName() + EXPECTED);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("'not a value' datum twice"));
            assertTrue(true);
        }
    }

    private ExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        BlockingDeque<EndpointDatum> endpointDatumsReceived, Capture<ComponentStateMachineEvent> capturedEvent)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, endpointDatumsReceived, capturedEvent, UUID.randomUUID().toString());
    }
    
    private ExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        List<InputGroupMockInformation> inputGroupMockInfos, BlockingDeque<EndpointDatum> endpointDatumsReceived,
        Capture<ComponentStateMachineEvent> capturedEvent) throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, inputGroupMockInfos, endpointDatumsReceived, capturedEvent,
            UUID.randomUUID().toString());
    }
    
    private ExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        BlockingDeque<EndpointDatum> endpointDatumsReceived, Capture<ComponentStateMachineEvent> capturedEvent, String executionId)
        throws ComponentExecutionException {
        return setUpExecutionScheduler(inputMockInfos, new ArrayList<InputGroupMockInformation>(),
            endpointDatumsReceived, capturedEvent, executionId);
    }

    private ExecutionScheduler setUpExecutionScheduler(List<InputMockInformation> inputMockInfos,
        List<InputGroupMockInformation> inputGroupMockInfos, BlockingDeque<EndpointDatum> endpointDatumsReceived,
        Capture<ComponentStateMachineEvent> capturedEvent, String executionId) throws ComponentExecutionException {
        ExecutionSpecificComponentExecutionContextMock compExeCtxMock =
            new ExecutionSpecificComponentExecutionContextMock(executionId, inputMockInfos, inputGroupMockInfos);
        ExecutionScheduler executionScheduler = new ExecutionScheduler(compExeCtxMock, endpointDatumsReceived,
            createComponentStateMachineMock(capturedEvent));
        executionScheduler.initialize(compExeCtxMock);
        executionScheduler.start();
        return executionScheduler;
    }

    private ComponentStateMachine createComponentStateMachineMock(Capture<ComponentStateMachineEvent> capturedEvent) {
        ComponentStateMachine stateMachineMock = EasyMock.createStrictMock(ComponentStateMachine.class);
        stateMachineMock.postEvent(EasyMock.capture(capturedEvent));
        EasyMock.replay(stateMachineMock);
        return stateMachineMock;
    }

    private void sendValuesToExecutionScheduler(final BlockingDeque<EndpointDatum> dequeEndpointDatumsSentTo,
        final List<EndpointDatum> endpointDatumsToSend) {
        for (EndpointDatum endpointDatum : endpointDatumsToSend) {
            dequeEndpointDatumsSentTo.addLast(endpointDatum); // think about delays between additions
        }
    }

    private void checkEndpointDatums(Map<String, EndpointDatum> endpointDatumsReturned, EndpointDatum... endpointDatumsExpected) {
        checkEndpointDatums(endpointDatumsReturned, Arrays.asList(endpointDatumsExpected));
    }

    private void checkEndpointDatums(Map<String, EndpointDatum> endpointDatumsReturned, List<EndpointDatum> endpointDatumsExpected) {
        assertEquals(endpointDatumsExpected.size(), endpointDatumsReturned.size());
        for (EndpointDatum endpointDatum : endpointDatumsExpected) {
            assertTrue(endpointDatumsReturned.containsKey(endpointDatum.getInputName()));
            assertTrue(endpointDatumsReturned.containsValue(endpointDatum));
        }
    }

    private void checkForStateMachineEvents(Capture<ComponentStateMachineEvent> capturedEvent) throws InterruptedException {
        Thread.sleep(SLEEP_INTERVAL_MSEC);
        assertFalse(capturedEvent.hasCaptured());
    }

    private void tearDownExecutionScheduler(ExecutionScheduler executionScheduler) throws InterruptedException, ExecutionException {
        executionScheduler.stop();
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
     * Mock for arbitrary {@link TypedDatum} objects.
     * 
     * @author Doreen Seider
     */
    private final class NotAValueTDMock implements NotAValueTD {

        private final String identifier;

        public NotAValueTDMock() {
            this.identifier = UUID.randomUUID().toString();
        }
        
        public NotAValueTDMock(String identifier) {
            this.identifier = identifier;
        }
        
        @Override
        public DataType getDataType() {
            return DataType.NotAValue;
        }

        @Override
        public String getIdentifier() {
            return identifier;
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

        private List<InputMockInformation> inputMockInformations;
        
        private List<InputGroupMockInformation> inputGroupMockInformations;

        private ExecutionSpecificComponentExecutionContextMock(List<InputMockInformation> inputMockInformations) {
            this(inputMockInformations, new ArrayList<InputGroupMockInformation>());
        }

        private ExecutionSpecificComponentExecutionContextMock(String executionId, List<InputMockInformation> inputMockInformations) {
            this(executionId, inputMockInformations, new ArrayList<InputGroupMockInformation>());
        }
        
        private ExecutionSpecificComponentExecutionContextMock(String executionId, List<InputMockInformation> inputMockInformations,
            List<InputGroupMockInformation> inputGroupMockInformations) {
            super(executionId);
            this.inputMockInformations = inputMockInformations;
            this.inputGroupMockInformations = inputGroupMockInformations;
        }
        
        private ExecutionSpecificComponentExecutionContextMock(List<InputMockInformation> inputMockInformations,
            List<InputGroupMockInformation> inputGroupMockInformations) {
            super();
            this.inputMockInformations = inputMockInformations;
            this.inputGroupMockInformations = inputGroupMockInformations;
        }

        @Override
        public ComponentDescription getComponentDescription() {
            ComponentDescription componentDescriptionMock = EasyMock.createStrictMock(ComponentDescription.class);
            EasyMock.expect(componentDescriptionMock.getInputDescriptionsManager())
                .andReturn(InputDescriptionManagerMockFactory.createInputDescriptionManagerMock(inputMockInformations,
                    inputGroupMockInformations)).anyTimes();
            EasyMock.replay(componentDescriptionMock);
            return componentDescriptionMock;
        }

    }

}
