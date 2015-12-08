/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionControllerImpl.ComponentStateMachine;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionControllerImpl.ComponentStateMachineEvent;
import de.rcenvironment.core.component.execution.internal.ComponentExecutionControllerImpl.ComponentStateMachineEventType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDescription;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Decides if a component can be executed. It takes all input values, checks for execution readiness after each new input. If input value
 * are requested, they are removed from the stack of input values except the input is defined as immutable.
 * 
 * The class is not totally thread-safe. It is intended that adding an input can be done from any thread, but checking execution readiness
 * and requesting values are within one.
 * 
 * @author Doreen Seider
 */
public class ExecutionScheduler {
    
    private final String compExeId;
    
    private final BlockingDeque<EndpointDatum> endpointDatumsToProcess;
    
    private final BlockingDeque<EndpointDatum> validatedEndpointDatumsToProcess = new LinkedBlockingDeque<>();
    
    private final ComponentStateMachine stateMachine;
    
    private final Map<String, EndpointGroupDescription> endpointGroupDescriptions = new HashMap<>();
    
    private final Map<String, EndpointDatum> inputsOccupied = Collections.synchronizedMap(new HashMap<String, EndpointDatum>());
    
    private final Map<String, Deque<EndpointDatum>> endpointDatums = new HashMap<>();
    
    private final Set<String> queuedConsumingInputs = new HashSet<>();
    
    private final Set<String> consumingInputs = new HashSet<>();
    
    private final Set<String> constantInputs = new HashSet<>();
    
    private final Set<String> constantInputsProcessed = Collections.synchronizedSet(new HashSet<String>());
    
    private final Set<String> requiredInputsOrGroups = new HashSet<>();
    
    private final Set<String> notRequiredInputs = new HashSet<>();
    
    private final Set<String> inputsWithValue = new HashSet<>();
    
    private final Map<String, Set<String>> groups = new HashMap<>();
    
    private final Set<String> finishedInputs = new HashSet<>();
    
    private final Map<String, Set<String>> idsOfNotAValueDatumsReceived = new HashMap<>();
    
    private final Set<String> idsNotAValueDatumsSent = Collections.synchronizedSet(new HashSet<String>());
    
    private final Set<String> resetDataIdsForwarded = Collections.synchronizedSet(new HashSet<String>());
    
    private final Set<String> failureDataIdsForwarded = Collections.synchronizedSet(new HashSet<String>());
    
    private final AtomicBoolean loopResetRequested = new AtomicBoolean(false);
    
    private final Set<String> resetDataIdsSent = Collections.synchronizedSet(new HashSet<String>());
    
    private final AtomicBoolean loopReset = new AtomicBoolean(false);
    
    private final AtomicReference<EndpointDatum> resetDatumToFoward = new AtomicReference<>(null);
    
    private final AtomicReference<EndpointDatum> failureDatumToFoward = new AtomicReference<>(null);
    
    private Future<?> schedulingFuture;
    
    private int inputsCount = 0;

    private State state = State.IDLING;
    
    private TypedDatumFactory typedDatumFactory;
    
    /**
     * States a component can be form a scheduling perspective.
     * 
     * @author Doreen Seider
     */
    protected enum State {
        IDLING,
        PROCESS_INPUT_DATA,
        PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA,
        FINISHED,
        RESET,
        FAILURE_FORWARD,
        LOOP_RESET;
    }
    
    protected ExecutionScheduler(ComponentExecutionContext compExeContext, BlockingDeque<EndpointDatum> endpointDatumsToProcess,
        ComponentStateMachine stateMachine) {
        compExeId = compExeContext.getExecutionIdentifier();
        this.endpointDatumsToProcess = endpointDatumsToProcess;
        this.stateMachine = stateMachine;
    }
    
    protected void initialize(ComponentExecutionContext compExeContext) throws ComponentExecutionException {
        EndpointDescriptionsManager endpointDescriptionsManager = compExeContext.getComponentDescription().getInputDescriptionsManager();
        for (EndpointGroupDescription groupDescription : endpointDescriptionsManager.getEndpointGroupDescriptions()) {
            endpointGroupDescriptions.put(groupDescription.getName(), groupDescription);
        }
        
        for (EndpointDescription endpointDescription : endpointDescriptionsManager.getEndpointDescriptions()) {
            endpointDatums.put(endpointDescription.getName(), new LinkedList<EndpointDatum>());
            Map<String, String> metaData = endpointDescription.getMetaData();
            String inputHandling = metaData.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING);
            if (inputHandling == null) {
                inputHandling = endpointDescription.getEndpointDefinition().getDefaultInputDatumHandling().name();
            }
            if (inputHandling.equals(EndpointDefinition.InputDatumHandling.Constant.name())) {
                constantInputs.add(endpointDescription.getName());
            } else {
                consumingInputs.add(endpointDescription.getName());
                if (inputHandling.equals(EndpointDefinition.InputDatumHandling.Queue.name())) {
                    queuedConsumingInputs.add(endpointDescription.getName());
                }
            }
            
            String inputExecutionConstraint = metaData.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);
            if (inputExecutionConstraint == null) {
                inputExecutionConstraint = endpointDescription.getEndpointDefinition()
                    .getDefaultInputExecutionConstraint().name();
            }
            if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.RequiredIfConnected.name())) {
                if (endpointDescription.isConnected()) {
                    addToRequiredInputsOrGroups(endpointDescriptionsManager, endpointDescription);
                }
            } else if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.None.name())) {
                if (endpointDescription.getParentGroupName() == null) {
                    throw new ComponentExecutionException(StringUtils.format(
                        "Input '%s' of component '%s' is declared as not required, but it is not part of an input group of type 'or'",
                        endpointDescription.getName(), compExeContext.getInstanceName()));
                } else if (endpointDescription.isConnected()) {
                    addToRequiredInputsOrGroups(endpointDescriptionsManager, endpointDescription);
                }
            } else if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.NotRequired.name())) {
                if (endpointDescription.isConnected()) {
                    addToNotRequiredInputs(endpointDescription);
                }
            } else {
                if (!endpointDescription.isConnected()) {
                    throw new ComponentExecutionException(StringUtils.format("The execution constraint of input '%s' of component '%s' "
                        + "is declared as 'required', but the input is not connected to an output. Either connect it to an output or "
                        + "alter its execution constraint (e.g., to 'required if connected') or delete the input at all. Note: "
                        + "The two latter options might not be applicable in this particular case.",
                        endpointDescription.getName(), compExeContext.getInstanceName()));
                }
                addToRequiredInputsOrGroups(endpointDescriptionsManager, endpointDescription);
            }
        }
        
    }
    
    protected synchronized void start() {
        if (schedulingFuture == null) {
            schedulingFuture = SharedThreadPool.getInstance().submit(new Runnable() {
                
                @TaskDescription("Pre-process input values")
                @Override
                public void run() {
                    while (true) {
                        EndpointDatum datum;
                        try {
                            datum = endpointDatumsToProcess.take();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        try {
                            checkIfConstantAndSingleConstraintIsMatched(datum);
                            validatedEndpointDatumsToProcess.add(datum);
                        } catch (ComponentExecutionException e) {
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.SCHEDULING_FAILED, e));
                            return;
                        }
                    }
                }
            });
        }
    }
    
    private void checkIfConstantAndSingleConstraintIsMatched(EndpointDatum endpointDatum) throws ComponentExecutionException {
        if (endpointDatum.getValue().getDataType() != DataType.Internal) {
            synchronized (inputsOccupied) {
                if (inputsOccupied.containsKey(endpointDatum.getInputName())) {
                    if (constantInputs.contains(endpointDatum.getInputName())) {
                        throw new ComponentExecutionException(StringUtils.format(
                            "A second value at input '%s' type 'constant' received. Only one value is allowed. "
                            + "First: %s. Second: %s. (Except in inner loops. There, one value is allowed for each inner loop run.)",
                            endpointDatum.getInputName(), inputsOccupied.get(endpointDatum.getInputName()), endpointDatum));
                    } else if (!queuedConsumingInputs.contains(endpointDatum.getInputName())) {
                        throw new ComponentExecutionException(StringUtils.format(
                            "A new value at input '%s' of type 'single' received, but the current one was not consumed yet. "
                            + "Current: %s. New: %s. Queue of values is not allowed at inputs of type 'single'. "
                            + "Use input type 'queue' if queuing is allowed and intended.",
                            endpointDatum.getInputName(), inputsOccupied.get(endpointDatum.getInputName()), endpointDatum));
                    }
                } else {
                    if (constantInputs.contains(endpointDatum.getInputName())
                        || !queuedConsumingInputs.contains(endpointDatum.getInputName())) {
                        inputsOccupied.put(endpointDatum.getInputName(), endpointDatum);
                    }
                }
            }
        }
    }
    
    protected synchronized void stop() throws InterruptedException, ExecutionException {
        if (schedulingFuture != null && !schedulingFuture.isCancelled()) {
            schedulingFuture.cancel(true);
        }
        schedulingFuture = null;
    }
    
    private void addToNotRequiredInputs(EndpointDescription endpointDescription) {
        inputsCount++;
        notRequiredInputs.add(endpointDescription.getName());
    }
    
    private void addToRequiredInputsOrGroups(EndpointDescriptionsManager endpointDescriptionsManager,
        EndpointDescription endpointDescription) {
        inputsCount++;
        if (endpointDescription.getParentGroupName() == null || endpointDescription.getParentGroupName().equals("null")) {
            requiredInputsOrGroups.add(endpointDescription.getName());
        } else {
            requiredInputsOrGroups.add(getTopLevelGroup(endpointDescriptionsManager, endpointDescription.getParentGroupName()));
            fillGroups(endpointDescriptionsManager, endpointDescription.getName(), endpointDescription.getParentGroupName());
        }
    }
    
    private void fillGroups(EndpointDescriptionsManager endpointDescriptionsManager, String inputOrGroupName, String groupName) {
        
        if (!groups.containsKey(groupName)) {
            groups.put(groupName, new HashSet<String>());
        }
        groups.get(groupName).add(inputOrGroupName);
        
        if (endpointDescriptionsManager.getEndpointGroupDescription(groupName).getParentGroupName() != null) {
            fillGroups(endpointDescriptionsManager, groupName, endpointDescriptionsManager
                .getEndpointGroupDescription(groupName).getParentGroupName());
        }
    }
    
    private String getTopLevelGroup(EndpointDescriptionsManager endpointDescriptionsManager, String groupName) {
        if (endpointDescriptionsManager.getEndpointGroupDescription(groupName).getParentGroupName() != null) {
            return getTopLevelGroup(endpointDescriptionsManager, endpointDescriptionsManager
                .getEndpointGroupDescription(groupName).getParentGroupName());
        } else {
            return groupName;
        }
    }
    
    protected State getSchedulingState() throws InterruptedException, ComponentExecutionException {
        updateState();
        if (state != State.PROCESS_INPUT_DATA && state != State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA) {
            addEndpointDatum(validatedEndpointDatumsToProcess.take());
            updateState();
        }
        return state;
    } 
    
    protected Map<String, EndpointDatum> getEndpointDatums() {
        Map<String, EndpointDatum> datums = new HashMap<>();
        for (String inputName : inputsWithValue) {
            datums.put(inputName, getEndpointDatumReturnedForExecution(inputName));
        }
        for (String inputName : notRequiredInputs) {
            if (!endpointDatums.get(inputName).isEmpty()) {
                datums.put(inputName, getEndpointDatumReturnedForExecution(inputName));
            }
        }
        inputsWithValue.clear();
        return datums;
    }
    
    private EndpointDatum getEndpointDatumReturnedForExecution(String inputName) {
        if (constantInputs.contains(inputName)) {
            constantInputsProcessed.add(inputName);
            return endpointDatums.get(inputName).peekFirst();                
        } else {
            if (!queuedConsumingInputs.contains(inputName)) {
                synchronized (inputsOccupied) {
                    inputsOccupied.remove(inputName);
                }
            }
            return endpointDatums.get(inputName).pollFirst();
        }
    }
    
    protected InternalTDImpl getResetDatum() {
        InternalTDImpl resetDatum = (InternalTDImpl) resetDatumToFoward.get().getValue();
        resetDatumToFoward.set(null);
        return resetDatum;
    }
    
    protected InternalTDImpl getFailureDatum() {
        InternalTDImpl failureDatum = (InternalTDImpl) failureDatumToFoward.get().getValue();
        failureDatumToFoward.set(null);
        return failureDatum;
    }
    
    private void addEndpointDatum(EndpointDatum endpointDatum) throws ComponentExecutionException {
        performFirstSanityCheckForEndpointDatumAdded(endpointDatum);
        switch (endpointDatum.getValue().getDataType()) {
        case Internal:
            handleInternalEndpointDatumAdded(endpointDatum);
            break;
        default:
            handleNonInternalEndpointDatumAdded(endpointDatum);
            break;
        }
    }
    
    private void performFirstSanityCheckForEndpointDatumAdded(EndpointDatum endpointDatum) throws ComponentExecutionException {
        
        if (loopResetRequested.get() && (endpointDatum.getValue().getDataType() != DataType.Internal
            || ((InternalTDImpl) endpointDatum.getValue()).getType() != InternalTDImpl.InternalTDType.NestedLoopReset)) {
            if (endpointDatum.getValue().getDataType() == DataType.Internal) {
                throw new ComponentExecutionException(StringUtils.format(
                    "Received input at '%s' of type 'Internal (Finished)', but component is waiting for datums of type 'Internal (Reset)'."
                    + " Review the connections of your (nested) loop(s). Refer to the user guide if in doubt.",
                    endpointDatum.getInputName()));
            } else {
                throw new ComponentExecutionException(StringUtils.format(
                    "Received input at '%s' of type '%s', but component is waiting for datums of type 'Internal (Reset)'."
                    + " Review the connections of your (nested) loop(s). Refer to the user guide if in doubt.",
                    endpointDatum.getInputName(), endpointDatum.getValue().getDataType().getDisplayName()));
            }
        }
        
    }
    
    private void handleInternalEndpointDatumAdded(EndpointDatum endpointDatum) throws ComponentExecutionException {
        InternalTDImpl internalDatum = (InternalTDImpl) endpointDatum.getValue();
        switch (internalDatum.getType()) {
        case WorkflowFinish:
            finishedInputs.add(endpointDatum.getInputName());
            break;
        case FailureInLoop:
            if (internalDatum.getHopsToTraverse().isEmpty()) { // final component
                handleNonInternalEndpointDatumAdded(convertEndpointDatum(endpointDatum, Long.valueOf(internalDatum.getPayload())));
            } else if (!internalDatum.getHopsToTraverse().peek().getHopExecutionIdentifier().equals(compExeId)) { // sanity check
                throw new ComponentExecutionException("Internal error: Received failure datum, but component is not the recipient,"
                    + " , there are still hops to traverse left: " + internalDatum.getHopsToTraverse());
            } else if (failureDataIdsForwarded.contains(internalDatum.getIdentifier())) { // sanity check
                throw new ComponentExecutionException(StringUtils.format(
                    "Received failure datum twice (was forwarded at input '%s'); id: " + internalDatum.getIdentifier()
                    +  "; Review the connections of your (nested) loop(s). Refer to the user guide if in doubt.",
                    endpointDatum.getInputName()));
            } else {
                failureDatumToFoward.set(endpointDatum);
                failureDataIdsForwarded.add(internalDatum.getIdentifier());
            }
            break;
        case NestedLoopReset:
            if (loopResetRequested.get()) {
                if (!internalDatum.getHopsToTraverse().isEmpty()) { // not final component
                    LogFactory.getLog(getClass()).warn("Internal error: Initiated reset, received own reset datum, but component"
                        + " is not the final recipient, there are still hops to traverse left: " + internalDatum.getHopsToTraverse());
                }
                if (!resetDataIdsSent.remove(internalDatum.getIdentifier())) {
                    throw new ComponentExecutionException(StringUtils.format(
                        "Internal error: Received unexpected (wrong identifier) input at '%s' of type '%s'",
                        endpointDatum.getInputName(), endpointDatum.getValue().getDataType().getDisplayName()));
                }
                if (resetDataIdsSent.isEmpty()) {
                    loopReset.set(true);
                }
            } else {
                if (internalDatum.getHopsToTraverse().isEmpty()) { // sanity check
                    throw new ComponentExecutionException("Internal error: Received reset datum and component is the final recipient,"
                        + " but no loop reset was requested");
                } else if (!internalDatum.getHopsToTraverse().peek().getHopExecutionIdentifier().equals(compExeId)) { // sanity check
                    throw new ComponentExecutionException("Internal error: Received reset datum, but component is not the final"
                        + " recipient; there are still hops to traverse left: " + internalDatum.getHopsToTraverse());
                } else if (resetDataIdsForwarded.contains(internalDatum.getIdentifier())) { // sanity check
                    throw new ComponentExecutionException(StringUtils.format(
                        "Received reset datum twice (was forwarded at input '%s'); id: " + internalDatum.getIdentifier()
                        +  "; Review the connections of your (nested) loop(s). Refer to the user guide if in doubt.",
                        endpointDatum.getInputName()));
                } else {
                    resetDatumToFoward.set(endpointDatum);
                    resetDataIdsForwarded.add(internalDatum.getIdentifier());
                }
            }
            break;
        default:
            break;
        }
    }
    
    private EndpointDatum convertEndpointDatum(EndpointDatum endpointDatumToConvert, Long dmId) {
        EndpointDatumImpl endpointDatumToAdd = new EndpointDatumImpl();
        endpointDatumToAdd.setEndpointDatumRecipient(endpointDatumToConvert.getEndpointDatumRecipient());
        endpointDatumToAdd.setValue(typedDatumFactory.createNotAValue(
            ((InternalTDImpl) endpointDatumToConvert.getValue()).getIdentifier()));
        endpointDatumToAdd.setDataManagementId(dmId);
        endpointDatumToAdd.setWorkfowNodeId(endpointDatumToConvert.getWorkflowNodeId());
        endpointDatumToAdd.setOutputsComponentExecutionIdentifier(endpointDatumToConvert.getOutputsComponentExecutionIdentifier());
        endpointDatumToAdd.setOutputsNodeId(endpointDatumToConvert.getOutputsNodeId());
        endpointDatumToAdd.setWorkflowExecutionIdentifier(endpointDatumToConvert.getWorkflowExecutionIdentifier());
        return endpointDatumToAdd;
        
    }
    
    private void handleNonInternalEndpointDatumAdded(EndpointDatum endpointDatum) throws ComponentExecutionException {
        if (endpointDatum.getValue().getDataType().equals(DataType.NotAValue)) {
            NotAValueTD datum = (NotAValueTD) endpointDatum.getValue();
            if (idsOfNotAValueDatumsReceived.containsKey(endpointDatum.getInputName())
                && idsOfNotAValueDatumsReceived.get(endpointDatum.getInputName()).contains(datum.getIdentifier())) {
                throw new ComponentExecutionException("Internal error: Received 'not a value' datum twice"
                    + " I.e., no component handled it appropriately within this loop.");

            } else if (idsNotAValueDatumsSent.contains(datum.getIdentifier())) {
                throw new ComponentExecutionException("Received own 'not a value' datum"
                    + " I.e., no component handled it appropriately within this loop"
                    +  "; Review the components and connections of your (nested) loop(s). Refer to the user guide if in doubt.");
            } else {
                if (!idsOfNotAValueDatumsReceived.containsKey(endpointDatum.getInputName())) {
                    idsOfNotAValueDatumsReceived.put(endpointDatum.getInputName(), new HashSet<String>());
                }
                idsOfNotAValueDatumsReceived.get(endpointDatum.getInputName()).add(datum.getIdentifier());
            }
        }
        finishedInputs.remove(endpointDatum.getInputName());
        endpointDatums.get(endpointDatum.getInputName()).add(endpointDatum);
    }
    
    protected void addNotAValueDatumSent(String identifier) {
        idsNotAValueDatumsSent.add(identifier);
    }
    
    protected void addResetDataIdSent(String identifier) {
        resetDataIdsSent.add(identifier);
        loopResetRequested.set(true);
    }
    
    protected boolean isLoopResetRequested() {
        return loopResetRequested.get();
    }
    
    private void updateState() throws ComponentExecutionException {
        if (isExecutable()) {
            state = checkForIndefiniteDatums();
        } else if (finishedInputs.size() == inputsCount) {
            checkIfDatumAtConsumingInputsLeft();
            state = State.FINISHED;
        } else if (resetDatumToFoward.get() != null) {
            checkIfDatumAtConsumingInputsLeft();
            resetConstantInputs();
            state = State.RESET;
        } else if (failureDatumToFoward.get() != null) {
            state = State.FAILURE_FORWARD;
        } else if (loopReset.get()) {
            loopReset.set(false);
            loopResetRequested.set(false);
            resetConstantInputs();
            state = State.LOOP_RESET;
        } else {
            state = State.IDLING;
        }
    }
    
    private void resetConstantInputs() {
        for (String constantInputName : constantInputs) {
            if (!finishedInputs.contains(constantInputName)) {
                endpointDatums.get(constantInputName).clear();
                constantInputsProcessed.remove(constantInputName);
                synchronized (inputsOccupied) {
                    inputsOccupied.remove(constantInputName);
                }
            }
        }
    }
    
    private void checkIfDatumAtConsumingInputsLeft() throws ComponentExecutionException {
        String logMessage = null;
        for (String inputName : endpointDatums.keySet()) {
            if (consumingInputs.contains(inputName)
                && !endpointDatums.get(inputName).isEmpty()) {
                // log here as the relevant typed datums are not intended to leave this class
                StringBuffer buffer = new StringBuffer();
                for (EndpointDatum datum : endpointDatums.get(inputName)) {
                    buffer.append(datum.getValue().toString());
                    buffer.append(", ");
                }
                buffer.delete(buffer.length() - 3, buffer.length() - 1);
                logMessage = StringUtils.format("Component is finished or reset, "
                    + "but there are values for input '%s' left that are not processed yet: %s", inputName, buffer.toString());
                if (notRequiredInputs.contains(inputName)) {
                    LogFactory.getLog(ExecutionScheduler.class).warn(logMessage);
                    logMessage = null;
                } else {
                    LogFactory.getLog(ExecutionScheduler.class).error(logMessage);
                }
            }
        }
        if (logMessage != null) {
            throw new ComponentExecutionException(logMessage);
        }
    }
    
    private boolean isExecutable() {
        return constantInputsProcessed.size() < inputsCount && isExecutableWithAndCondition(requiredInputsOrGroups);
    }
    
    private boolean isExecutableWithAndCondition(Set<String> inputsOrGroupIds) {
        if (inputsOrGroupIds.isEmpty()) {
            return false;
        }
        for (String identifier : inputsOrGroupIds) {
            if (endpointGroupDescriptions.containsKey(identifier)) {
                if (!checkGroupForExecutable(identifier)) {
                    inputsWithValue.clear();
                    return false;
                }
            } else {
                if (endpointDatums.get(identifier).isEmpty()) {
                    inputsWithValue.clear();
                    return false;
                } else {
                    inputsWithValue.add(identifier);
                }
            }
        }
        return true;
    }
    
    private boolean isExecutableWithOrCondition(Set<String> inputsOrGroupIds) {
        for (String identifier : inputsOrGroupIds) {
            if (endpointGroupDescriptions.containsKey(identifier)) {
                if (checkGroupForExecutable(identifier)) {
                    return true;
                }
            } else if (!endpointDatums.get(identifier).isEmpty()
                && (!constantInputs.contains(identifier) || !constantInputsProcessed.contains(identifier))) {
                inputsWithValue.add(identifier);
                return true;
            }
        }
        inputsWithValue.clear();
        return false;
    }

    private boolean checkGroupForExecutable(String groupName) {
        EndpointGroupDescription groupDescription = endpointGroupDescriptions.get(groupName);
        if (groupDescription.getEndpointGroupDefinition().getLogicOperation().equals(EndpointGroupDefinition.LogicOperation.And)) {
            return isExecutableWithAndCondition(groups.get(groupName));
        } else {
            return isExecutableWithOrCondition(groups.get(groupName));
        }        
    }
    
    private State checkForIndefiniteDatums() {
        State newState = State.PROCESS_INPUT_DATA;
        for (String inputIdentifier : endpointDatums.keySet()) {
            if (!endpointDatums.get(inputIdentifier).isEmpty()
                && endpointDatums.get(inputIdentifier).getFirst().getValue().getDataType() == DataType.NotAValue) {
                newState = State.PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA;
                break;
            }
        }
        return newState;
    }
    
    protected void setTypedDatumFactory(TypedDatumFactory typedDatumFactory) {
        this.typedDatumFactory = typedDatumFactory;
    }
    
}
