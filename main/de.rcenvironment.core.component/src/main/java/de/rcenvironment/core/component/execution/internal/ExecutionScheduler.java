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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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
import de.rcenvironment.core.datamodel.api.DataType;
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
    
    private final String componentExeutionIdentifier;
    
    private final String logMessagPrefix;
    
    private final BlockingDeque<EndpointDatum> endpointDatumsToProcess;
    
    private final BlockingDeque<EndpointDatum> validatedEndpointDatumsToProcess = new LinkedBlockingDeque<>();
    
    private final ComponentStateMachine stateMachine;
    
    private final Map<String, EndpointGroupDefinition> endpointGroupDefinitions = new HashMap<>();
    
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
    
    private int inputsCount = 0;
    
    private final Map<String, Set<String>> idsOfIndefiniteDatumsReceived = new HashMap<>();
    
    private final Set<String> idsIndefiniteDatumsSent = Collections.synchronizedSet(new HashSet<String>());
    
    private final Set<String> resetDataIdsForwarded = Collections.synchronizedSet(new HashSet<String>());
    
    private AtomicBoolean loopResetRequested = new AtomicBoolean(false);
    
    private final Set<String> resetDataIdsSent = Collections.synchronizedSet(new HashSet<String>());
    
    private AtomicBoolean loopReset = new AtomicBoolean(false);
    
    private State state = State.IDLING;
    
    private EndpointDatum resetDatumToFoward;
    
    private Future<?> schedulingFuture;
    
    /**
     * States a component can be form a scheduling perspective.
     * 
     * @author Doreen Seider
     */
    protected enum State {
        IDLING,
        PROCESS_INPUT_DATA,
        PROCESS_INPUT_DATA_WITH_INDEFINITE_DATA,
        FINISHED,
        RESET,
        LOOP_RESET;
    }
    
    protected ExecutionScheduler(ComponentExecutionContext compExeContext, BlockingDeque<EndpointDatum> endpointDatumsToProcess,
        ComponentStateMachine stateMachine) {
        componentExeutionIdentifier = compExeContext.getExecutionIdentifier();
        logMessagPrefix =
            StringUtils.format("'%s' (%s) of workflow '%s' (%s): ", compExeContext.getInstanceName(),
                compExeContext.getExecutionIdentifier(), compExeContext.getWorkflowInstanceName(),
                compExeContext.getWorkflowExecutionIdentifier());
        this.endpointDatumsToProcess = endpointDatumsToProcess;
        this.stateMachine = stateMachine;
    }
    
    protected void initialize(ComponentExecutionContext compExeContext) throws ComponentExecutionException {
        EndpointDescriptionsManager endpointDescriptionsManager = compExeContext.getComponentDescription().getInputDescriptionsManager();
        for (EndpointGroupDefinition groupDefinition : endpointDescriptionsManager.getEndpointGroupDefinitions()) {
            endpointGroupDefinitions.put(groupDefinition.getIdentifier(), groupDefinition);
        }
        
        for (EndpointDescription endpointDescription : endpointDescriptionsManager.getEndpointDescriptions()) {
            endpointDatums.put(endpointDescription.getName(), new LinkedList<EndpointDatum>());
            Map<String, String> metaData = endpointDescription.getMetaData();
            String inputHandling = metaData.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING);
            if (inputHandling == null) {
                inputHandling = endpointDescription.getDeclarativeEndpointDescription().getDefaultInputDatumHandling().name();
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
                inputExecutionConstraint = endpointDescription.getDeclarativeEndpointDescription()
                    .getDefaultInputExecutionConstraint().name();
            }
            if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.RequiredIfConnected.name())) {
                if (endpointDescription.isConnected()) {
                    addToRequiredInputsOrGroups(endpointDescriptionsManager, endpointDescription);
                }
            } else if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.None.name())) {
                if (endpointDescription.getGroupName() == null) {
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
                    throw new ComponentExecutionException(StringUtils.format("Input '%s' of component '%s' is declared as required, "
                        + "but it is not connected to an ouput.",
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
                        throw new ComponentExecutionException(logMessagPrefix + StringUtils.format(
                            "A second value at input '%s' type 'constant' received. Only one value is allowed. "
                            + "First: %s. Second: %s. (Except in inner loops. There, one value is allowed for each inner loop run.)",
                            endpointDatum.getInputName(), inputsOccupied.get(endpointDatum.getInputName()), endpointDatum));
                    } else if (!queuedConsumingInputs.contains(endpointDatum.getInputName())) {
                        throw new ComponentExecutionException(logMessagPrefix + StringUtils.format(
                            "A new value at input '%s' of type 'single' received, but the current one was not consumed yet. "
                            + "Current: %s. New: %s. Queue of values is not allowed at inputs of type 'single'. "
                            + "Use input type 'queue' if queuing is intended.",
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
        if (schedulingFuture != null) {
            schedulingFuture.cancel(true);
            try {
                schedulingFuture.get();
            } catch (CancellationException e) {
                // intended
                e = null;
            }
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
        if (endpointDescription.getGroupName() == null) {
            requiredInputsOrGroups.add(endpointDescription.getName());
        } else {
            requiredInputsOrGroups.add(getTopLevelGroup(endpointDescriptionsManager, endpointDescription.getGroupName()));
            fillGroups(endpointDescriptionsManager, endpointDescription.getName(), endpointDescription.getGroupName());
        }
    }
    
    private void fillGroups(EndpointDescriptionsManager endpointDescriptionsManager, String inputOrGroupName, String groupName) {
        
        if (!groups.containsKey(groupName)) {
            groups.put(groupName, new HashSet<String>());
        }
        groups.get(groupName).add(inputOrGroupName);
        
        if (endpointDescriptionsManager.getEndpointGroupDefnition(groupName).getGroupName() != null) {
            fillGroups(endpointDescriptionsManager, groupName, endpointDescriptionsManager
                .getEndpointGroupDefnition(groupName).getGroupName());
        }
    }
    
    private String getTopLevelGroup(EndpointDescriptionsManager endpointDescriptionsManager, String groupName) {
        if (endpointDescriptionsManager.getEndpointGroupDefnition(groupName).getGroupName() != null) {
            return getTopLevelGroup(endpointDescriptionsManager, endpointDescriptionsManager
                .getEndpointGroupDefnition(groupName).getGroupName());
        } else {
            return groupName;
        }
    }
    
    protected State getSchedulingState() throws InterruptedException, ComponentExecutionException {
        updateState();
        if (state != State.PROCESS_INPUT_DATA && state != State.PROCESS_INPUT_DATA_WITH_INDEFINITE_DATA) {
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
        InternalTDImpl resetDatum = (InternalTDImpl) resetDatumToFoward.getValue();
        resetDatumToFoward = null;
        return resetDatum;
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
                throw new ComponentExecutionException(logMessagPrefix + StringUtils.format(
                    "Received input at '%s' of type 'Internal (Finished)', "
                    + "but component is waiting for datums of type 'Internal (Reset)'",
                    endpointDatum.getInputName()));
            } else {
                throw new ComponentExecutionException(logMessagPrefix + StringUtils.format(
                    "Received input at '%s' of type '%s', "
                    + "but component is waiting for datums of type 'Internal (Reset)'",
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
        case NestedLoopReset:
            if (!loopResetRequested.get()) {
                if (!internalDatum.getHopsToTraverse().peek().getHopExecutionIdentifier().equals(componentExeutionIdentifier)) {
                    throw new ComponentExecutionException(logMessagPrefix
                        + "Received reset datum, but component is not the latest recipient");
                } else if (resetDataIdsForwarded.contains(internalDatum.getIdentifier())) {
                    throw new ComponentExecutionException(logMessagPrefix + StringUtils.format(
                        "Received reset datum forwarded at input '%s' again", endpointDatum.getInputName()));
                } else {
                    resetDatumToFoward = endpointDatum;
                    resetDataIdsForwarded.add(internalDatum.getIdentifier());
                }
            } else {
                if (!resetDataIdsSent.remove(internalDatum.getIdentifier())) {
                    throw new ComponentExecutionException(logMessagPrefix + StringUtils.format(
                        "Received unexpected (wrong identifier) input at '%s' of type '%s'-Reset",
                        endpointDatum.getInputName(), endpointDatum.getValue().getDataType().getDisplayName()));
                }
                if (resetDataIdsSent.isEmpty()) {
                    loopReset.set(true);
                }
            }
            break;
        default:
            break;
        }
    }
    
    private void handleNonInternalEndpointDatumAdded(EndpointDatum endpointDatum) throws ComponentExecutionException {
        if (endpointDatum.getValue().getDataType().equals(DataType.NotAValue)) {
            NotAValueTD datum = (NotAValueTD) endpointDatum.getValue();
            if (idsOfIndefiniteDatumsReceived.containsKey(endpointDatum.getInputName())
                && idsOfIndefiniteDatumsReceived.get(endpointDatum.getInputName()).contains(datum.getIdentifier())) {
                throw new ComponentExecutionException(logMessagPrefix + "Received 'not a value' datum twice"
                    + " I.e., no component handled it appropriately within this loop.");

            } else if (idsIndefiniteDatumsSent.contains(datum.getIdentifier())) {
                throw new ComponentExecutionException(logMessagPrefix + "Received own 'not a value' datum"
                    + " I.e., no component handled it appropriately within this loop.");
            } else {
                if (!idsOfIndefiniteDatumsReceived.containsKey(endpointDatum.getInputName())) {
                    idsOfIndefiniteDatumsReceived.put(endpointDatum.getInputName(), new HashSet<String>());
                }
                idsOfIndefiniteDatumsReceived.get(endpointDatum.getInputName()).add(datum.getIdentifier());
            }
        }
        finishedInputs.remove(endpointDatum.getInputName());
        endpointDatums.get(endpointDatum.getInputName()).add(endpointDatum);
    }
    
    protected void addIndefiniteDatumSent(String identifier) {
        idsIndefiniteDatumsSent.add(identifier);
    }
    
    protected void addResetDataIdsSent(Set<String> identifiers) {
        resetDataIdsSent.addAll(identifiers);
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
        } else if (resetDatumToFoward != null) {
            checkIfDatumAtConsumingInputsLeft();
            resetConstantInputs();
            state = State.RESET;
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
                logMessage = logMessagPrefix + StringUtils.format("Component is finished or reset, "
                    + "but values for input '%s' are not processed yet: %s", inputName, buffer.toString());
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
            if (endpointGroupDefinitions.containsKey(identifier)) {
                if (!checkGroupForExecutable(identifier)) {
                    return false;
                }
            } else {
                if (endpointDatums.get(identifier).isEmpty()) {
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
            if (endpointGroupDefinitions.containsKey(identifier)) {
                if (checkGroupForExecutable(identifier)) {
                    return true;
                }
            } else if (!endpointDatums.get(identifier).isEmpty()
                && (!constantInputs.contains(identifier) || !constantInputsProcessed.contains(identifier))) {
                inputsWithValue.add(identifier);
                return true;
            }
        }
        return false;
    }

    private boolean checkGroupForExecutable(String groupName) {
        EndpointGroupDefinition groupDefinition = endpointGroupDefinitions.get(groupName);
        if (groupDefinition.getType().equals(EndpointGroupDefinition.Type.And)) {
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
                newState = State.PROCESS_INPUT_DATA_WITH_INDEFINITE_DATA;
                break;
            }
        }
        return newState;
    }
    
}
