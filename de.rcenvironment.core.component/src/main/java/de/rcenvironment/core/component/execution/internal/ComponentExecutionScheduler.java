/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDescription;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Defines the scheduling state of a component. Inputs received are intended to be added immediately. They are validated and queued. If the
 * {@link ComponentExecutionScheduler} is enabled, the scheduling state is calculated on each new input. If disabled, nothing is calculated
 * when a new input was queued. The calculation starts immediately when the {@link ComponentExecutionScheduler} is enabled again.
 * 
 * If input values are fetched, they are removed from the stack of input values except if the input is defined as constant.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionScheduler {

    private static TypedDatumFactory typedDatumFactory;

    private static TypedDatumConverter typedDatumConverter;

    private ComponentExecutionRelatedInstances compExeRelatedInstances;

    private final Deque<EndpointDatum> validatedEndpointDatumsToProcess = new LinkedList<>();

    private final Map<String, DataType> endpointDataTypes = new HashMap<>();

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

    private final AtomicBoolean loopResetRequested = new AtomicBoolean(false);

    private final Set<String> resetDataIdsSent = Collections.synchronizedSet(new HashSet<String>());

    private final AtomicBoolean loopReset = new AtomicBoolean(false);

    private final AtomicReference<EndpointDatum> resetDatumToFoward = new AtomicReference<>(null);

    private final AtomicReference<EndpointDatum> failureDatumToFoward = new AtomicReference<>(null);

    private int inputsCount = 0;

    private Set<String> inputsConsideredForFinished = new HashSet<>();

    private AtomicReference<State> state = new AtomicReference<>(State.IDLING);

    private boolean isEnabled = false;

    private volatile boolean schedulingFailed = false;

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

    @Deprecated
    public ComponentExecutionScheduler() {}

    protected ComponentExecutionScheduler(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        this.compExeRelatedInstances = compExeRelatedInstances;
    }

    protected void initialize(ComponentExecutionContext compExeContext) throws ComponentExecutionException {

        EndpointDescriptionsManager inputDescriptionsManager = compExeContext.getComponentDescription().getInputDescriptionsManager();
        for (EndpointGroupDescription groupDescription : inputDescriptionsManager.getEndpointGroupDescriptions()) {
            endpointGroupDescriptions.put(groupDescription.getName(), groupDescription);
        }

        int inputsOuterCount = 0;
        Set<String> inputsSame = new HashSet<>();
        for (EndpointDescription endpointDescription : inputDescriptionsManager.getEndpointDescriptions()) {

            switch (endpointDescription.getEndpointDefinition().getEndpointCharacter()) {
            case OUTER_LOOP:
                inputsOuterCount++;
                break;
            case SAME_LOOP:
                inputsSame.add(endpointDescription.getName());
                break;
            default:
                throw new IllegalArgumentException(
                    "Endpoint type unknown: " + endpointDescription.getEndpointDefinition().getEndpointCharacter());
            }

            endpointDataTypes.put(endpointDescription.getName(), endpointDescription.getDataType());
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
                    addToRequiredInputsOrGroups(inputDescriptionsManager, endpointDescription);
                }
            } else if (inputExecutionConstraint.equals(EndpointDefinition.InputExecutionContraint.None.name())) {
                if (endpointDescription.getParentGroupName() == null) {
                    throw new ComponentExecutionException(StringUtils.format(
                        "Input '%s' of component '%s' is declared as not required, but it is not part of an input group of type 'or'",
                        endpointDescription.getName(), compExeContext.getInstanceName()));
                } else if (endpointDescription.isConnected()) {
                    addToRequiredInputsOrGroups(inputDescriptionsManager, endpointDescription);
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
                addToRequiredInputsOrGroups(inputDescriptionsManager, endpointDescription);
            }
        }

        if (!isDriver(compExeContext) && inputsOuterCount > 0 || isNestedDriver(compExeContext)) {
            inputsConsideredForFinished.removeAll(inputsSame);
        }

    }

    boolean isNestedDriver(ComponentExecutionContext compExeContext) {
        return Boolean.valueOf(compExeContext.getComponentDescription().getConfigurationDescription()
            .getConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP));
    }

    boolean isDriver(ComponentExecutionContext compExeContext) {
        return compExeContext.getComponentDescription().getComponentInterface().getIsLoopDriver();
    }

    protected synchronized void validateAndQueueEndpointDatum(EndpointDatum datum) {
        try {
            checkDataType(datum);
            checkIfConstantAndSingleConstraintIsMatched(datum);
            validatedEndpointDatumsToProcess.add(datum);
        } catch (ComponentExecutionException e) {
            postSchedulingFailedEvent(e);
            return;
        }

        if (isEnabled) {
            updateSchedulingState();
        }
    }

    private void postSchedulingFailedEvent(ComponentExecutionException e) {
        if (!schedulingFailed) {
            compExeRelatedInstances.compStateMachine
                .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.SCHEDULING_FAILED, e));
            schedulingFailed = true;
            isEnabled = false;
        }
    }

    private void postNewSchedulingStateEvent() {
        compExeRelatedInstances.compStateMachine
            .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.NEW_SCHEDULING_STATE));
        isEnabled = false;
    }

    protected synchronized boolean isEnabled() {
        return isEnabled;
    }

    protected synchronized void enable() {
        if (isEnabled) { // sanity check
            LogFactory.getLog(getClass())
                .warn("Component execution scheduler was requested to get enabled even if it is already enabled; ignored enabling request");
            return;
        }
        setEnabled(true);
    }

    protected synchronized void disable() {
        setEnabled(false);
    }

    private void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (isEnabled) {
            updateSchedulingState();
        }
    }

    protected State getSchedulingState() {
        return state.get();
    }

    private void checkDataType(EndpointDatum endpointDatum) throws ComponentExecutionException {
        DataType sourceDataType = endpointDatum.getValue().getDataType();
        DataType targetDataType = endpointDataTypes.get(endpointDatum.getInputName());
        if (sourceDataType != DataType.Internal && sourceDataType != DataType.NotAValue && sourceDataType != targetDataType
            && !typedDatumConverter.isConvertibleTo(sourceDataType, targetDataType)) {
            throw new ComponentExecutionException(StringUtils.format(
                "Value of type '%s' at input '%s' received that is not convertible to expected data type '%s'", sourceDataType,
                endpointDatum.getInputName(), targetDataType));
        }
    }

    private void checkIfConstantAndSingleConstraintIsMatched(EndpointDatum endpointDatum) throws ComponentExecutionException {
        if (endpointDatum.getValue().getDataType() != DataType.Internal) {
            synchronized (inputsOccupied) {
                if (inputsOccupied.containsKey(endpointDatum.getInputName())) {
                    if (constantInputs.contains(endpointDatum.getInputName())) {
                        throw new ComponentExecutionException(StringUtils.format(
                            "A second value at input '%s' of type 'constant' received. Only one value is allowed. "
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

    private void addToNotRequiredInputs(EndpointDescription endpointDescription) {
        increaseInputCount(endpointDescription);
        notRequiredInputs.add(endpointDescription.getName());
    }

    private void addToRequiredInputsOrGroups(EndpointDescriptionsManager endpointDescriptionsManager,
        EndpointDescription endpointDescription) {
        increaseInputCount(endpointDescription);
        if (endpointDescription.getParentGroupName() == null || endpointDescription.getParentGroupName().equals("null")) {
            requiredInputsOrGroups.add(endpointDescription.getName());
        } else {
            requiredInputsOrGroups.add(getTopLevelGroup(endpointDescriptionsManager, endpointDescription.getParentGroupName()));
            fillGroups(endpointDescriptionsManager, endpointDescription.getName(), endpointDescription.getParentGroupName());
        }
    }

    private void increaseInputCount(EndpointDescription endpointDescription) {
        inputsCount++;
        inputsConsideredForFinished.add(endpointDescription.getName());
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

    private void updateSchedulingState() {
        try {
            State newState = calculateSchedulingState();
            while (!validatedEndpointDatumsToProcess.isEmpty() && newState == State.IDLING) {
                addEndpointDatum(validatedEndpointDatumsToProcess.poll());
                newState = calculateSchedulingState();
            }
            state.set(newState);
            if (newState != State.IDLING) {
                postNewSchedulingStateEvent();
            }
        } catch (ComponentExecutionException e) {
            postSchedulingFailedEvent(e);
        }
    }

    protected synchronized Map<String, EndpointDatum> fetchEndpointDatums() {
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
            EndpointDatum pollFirst = endpointDatums.get(inputName).pollFirst();
            return pollFirst;
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
            } else if (!internalDatum.getHopsToTraverse().peek().getHopExecutionIdentifier()
                .equals(compExeRelatedInstances.compExeCtx.getExecutionIdentifier())) { // sanity check
                throw new ComponentExecutionException("Internal error: Received failure datum, but component is not the recipient,"
                    + " , there are still hops to traverse left: " + internalDatum.getHopsToTraverse());
            } else {
                failureDatumToFoward.set(endpointDatum);
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
                } else if (!internalDatum.getHopsToTraverse().peek().getHopExecutionIdentifier()
                    .equals(compExeRelatedInstances.compExeCtx.getExecutionIdentifier())) { // sanity check
                    throw new ComponentExecutionException("Internal error: Received reset datum, but component is not the final"
                        + " recipient; there are still hops to traverse left: " + internalDatum.getHopsToTraverse());
                } else {
                    resetDatumToFoward.set(endpointDatum);
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
            ((InternalTDImpl) endpointDatumToConvert.getValue()).getIdentifier(), NotAValueTD.Cause.Failure));
        endpointDatumToAdd.setDataManagementId(dmId);
        endpointDatumToAdd.setWorkflowNodeId(endpointDatumToConvert.getWorkflowNodeId());
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
                    + "; Review the components and connections of your (nested) loop(s). Refer to the user guide if in doubt.");
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

    private State calculateSchedulingState() throws ComponentExecutionException {
        State newState;
        if (isExecutable()) {
            newState = checkForNotAValueDatums();
        } else if (finishedInputs.containsAll(inputsConsideredForFinished)) {
            checkIfDatumAtConsumingInputsLeft();
            newState = State.FINISHED;
        } else if (resetDatumToFoward.get() != null) {
            checkIfDatumAtConsumingInputsLeft();
            resetConstantInputs();
            newState = State.RESET;
        } else if (failureDatumToFoward.get() != null) {
            newState = State.FAILURE_FORWARD;
        } else if (loopReset.get()) {
            loopReset.set(false);
            loopResetRequested.set(false);
            resetConstantInputs();
            newState = State.LOOP_RESET;
        } else {
            newState = State.IDLING;
        }
        return newState;
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
                StringBuilder strBuilder = new StringBuilder();
                for (EndpointDatum datum : endpointDatums.get(inputName)) {
                    strBuilder.append(datum.getValue().toString());
                    strBuilder.append(", ");
                }
                strBuilder.delete(strBuilder.length() - 3, strBuilder.length() - 1);
                logMessage = StringUtils.format("Component is finished or reset, "
                    + "but there are values for input '%s' left that are not processed yet: %s", inputName, strBuilder.toString());
                if (notRequiredInputs.contains(inputName)) {
                    LogFactory.getLog(ComponentExecutionScheduler.class).warn(logMessage);
                    logMessage = null;
                } else {
                    LogFactory.getLog(ComponentExecutionScheduler.class).error(logMessage);
                }
            }
        }
        if (logMessage != null) {
            throw new ComponentExecutionException(logMessage);
        }
    }

    protected boolean isExecutable() {
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
                if (endpointDatums.get(identifier).isEmpty() || allGroupInputsConstantAndSent(inputsOrGroupIds)) {
                    inputsWithValue.clear();
                    return false;
                }
                inputsWithValue.add(identifier);
            }
        }
        return true;
    }

    private boolean allGroupInputsConstantAndSent(Set<String> inputsOrGroupIds) {
        for (String identifier : inputsOrGroupIds) {
            if (!(constantInputs.contains(identifier) && constantInputsProcessed.contains(identifier))) {
                return false;
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

    private State checkForNotAValueDatums() {
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

    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        ComponentExecutionScheduler.typedDatumFactory = typedDatumService.getFactory();
        ComponentExecutionScheduler.typedDatumConverter = typedDatumService.getConverter();
    }

}
