/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Processes method callbacks from {@link ComponentContext} performed by the {@link Component} during execution.
 *
 * @author Doreen Seider
 */
public class ComponentContextBridge {

    private static final String NOT_CONVERTIBLE_MESSAGE = "Datum of type '%s' is not convertible to data type '%s' expected by input '%s'";

    private static final Log LOG = LogFactory.getLog(ComponentContextBridge.class);

    private static TypedDatumService typedDatumService;
    
    private final Map<String, DataType> inputDataTypes = new HashMap<>();

    private ComponentExecutionRelatedInstances compExeRelatedInstances;

    private Map<String, EndpointDatum> endpointDatumsForExecution = Collections.synchronizedMap(new HashMap<String, EndpointDatum>());

    private Map<String, Boolean> closedOutputs = Collections.synchronizedMap(new HashMap<String, Boolean>());
    
    private List<OutputHolder> outputsOnHold = Collections.synchronizedList(new ArrayList<OutputHolder>());

    private final Object historyDataLock = new Object();

    @Deprecated
    public ComponentContextBridge() {}

    public ComponentContextBridge(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        this.compExeRelatedInstances = compExeRelatedInstances;
        for (EndpointDescription epDesc : compExeRelatedInstances.compExeCtx.getComponentDescription().getInputDescriptionsManager()
            .getEndpointDescriptions()) {
            inputDataTypes.put(epDesc.getName(), epDesc.getDataType());
        }
    }

    /**
     * Writes given {@link TypedDatum} to the output. If the output is connected to inputs, the {@link TypedDatum} will be delivered
     * to all inputs.
     * 
     * @param outputName name of the output to write
     * @param datumToSend {@link TypedDatum} to be sent
     */
    public synchronized void writeOutput(String outputName, TypedDatum datumToSend) {
        EndpointDescription endpointDescription =
            compExeRelatedInstances.compExeCtx.getComponentDescription().getOutputDescriptionsManager()
                .getEndpointDescription(outputName);
        Long outputDmId = null;
        try {
            outputDmId = compExeRelatedInstances.compExeStorageBridge.addOutput(outputName,
                typedDatumService.getSerializer().serialize(datumToSend));
        } catch (ComponentExecutionException e) {
            throw new RuntimeException(e);
        }
        if (endpointDescription.isConnected()) {
            validateOutputDataType(outputName, endpointDescription.getDataType(), datumToSend);
            if (ComponentExecutionUtils.isVerificationRequired(compExeRelatedInstances.compExeCtx.getComponentDescription()
                .getConfigurationDescription().getComponentConfigurationDefinition())) {
                holdOutput(outputName, datumToSend, outputDmId);
            } else {
                sendOutput(outputName, datumToSend, outputDmId);
            }
        }
        closedOutputs.put(outputName, false);
    }

    private void sendOutput(String outputName, TypedDatum datumToSend, Long outputDmId) {
        compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutput(outputName, datumToSend, outputDmId);
        if (datumToSend.getDataType().equals(DataType.NotAValue)) {
            compExeRelatedInstances.compExeScheduler.addNotAValueDatumSent(((NotAValueTD) datumToSend).getIdentifier());
        }
    }
    
    protected void holdOutput(String outputName, TypedDatum datumToSend, Long outputDmId) {
        outputsOnHold.add(new OutputHolder(outputName, datumToSend, outputDmId));
    }
    
    protected void flushOutputs() {
        for (OutputHolder outputHolder : outputsOnHold) {
            sendOutput(outputHolder.outputName, outputHolder.datumToSend, outputHolder.outputDmId);
        }
    }

    protected void setEndpointDatumsForExecution(Map<String, EndpointDatum> endpointDatumsForExecution) throws ComponentExecutionException {
        this.endpointDatumsForExecution.clear();
        this.endpointDatumsForExecution.putAll(endpointDatumsForExecution);
        validateEndpointDatumsForExecution();
    }

    protected Map<String, EndpointDatum> getEndpointDatumsForExecution() {
        return Collections.unmodifiableMap(endpointDatumsForExecution);
    }

    private void validateIfNestedLoopComponent(String outputName) {
        if (!compExeRelatedInstances.isNestedLoopDriver) {
            throw new RuntimeException(getLogMessagesPrefix() + StringUtils.format(
                "Received reset datum at output '%s' for a non nested loop component. "
                    + "Reset datums are only allowed to send by nested loop components.",
                outputName));
        }
    }

    private void validateEndpointDatumsForExecution() throws ComponentExecutionException {
        for (EndpointDatum endpointDatum : endpointDatumsForExecution.values()) {
            DataType inputDataType = inputDataTypes.get(endpointDatum.getInputName());
            if (endpointDatum.getValue().getDataType() != inputDataType && endpointDatum.getValue().getDataType() != DataType.NotAValue) {
                if (!typedDatumService.getConverter().isConvertibleTo(endpointDatum.getValue(), inputDataType)) {
                    throw new ComponentExecutionException(
                        StringUtils.format(NOT_CONVERTIBLE_MESSAGE,
                            endpointDatum.getValue().getDataType(), inputDataType, endpointDatum.getInputName()));
                }
            }
        }
    }
    
    private void validateOutputDataType(String outputName, DataType dataType, TypedDatum datumToSent) {
        if (!datumToSent.getDataType().equals(DataType.NotAValue) && !datumToSent.getDataType().equals(DataType.Internal)) {
            if (datumToSent.getDataType() != dataType) {
                TypedDatumConverter converter = typedDatumService.getConverter();
                if (converter.isConvertibleTo(datumToSent, dataType)) {
                    try {
                        datumToSent = converter.castOrConvert(datumToSent, dataType);
                    } catch (DataTypeException e) {
                        // should not be reached because of isConvertibleTo check before
                        throw new RuntimeException(getLogMessagesPrefix() + StringUtils.format("Failed to convert "
                            + "the value for output '" + outputName + "' from type " + datumToSent.getDataType()
                            + " to required data type " + dataType));
                    }
                } else {
                    throw new RuntimeException(getLogMessagesPrefix() + StringUtils.format("Value for output '" + outputName
                        + "' has invalid data type. Output requires " + dataType + " or a convertible one, but it is of type "
                        + datumToSent.getDataType()));
                }
            }
        }
    }

    /**
     * @return all inputs with a value. I.e. {@link #readInput(String)} will be return a {@link TypedDatum}.
     */
    public Set<String> getInputsWithDatum() {
        return new HashSet<>(endpointDatumsForExecution.keySet());
    }

    /**
     * Reads input with the given name.
     * 
     * @param inputName name of the input to read
     * @return {@link TypedDatum} currently available at the input
     * @throws NoSuchElementException if there is no {@link TypedDatum} available
     */
    public TypedDatum readInput(String inputName) {
        if (endpointDatumsForExecution.containsKey(inputName)) {
            final EndpointDatum endpointDatum = endpointDatumsForExecution.get(inputName);
            DataType inputDataType = inputDataTypes.get(endpointDatum.getInputName());

            if (endpointDatum.getValue().getDataType() == inputDataType || endpointDatum.getValue().getDataType() == DataType.NotAValue) {
                return endpointDatum.getValue();
            } else {
                try {
                    return typedDatumService.getConverter().castOrConvert(endpointDatum.getValue(), inputDataType);
                } catch (DataTypeException e) { // should not happen due to early validation by validateEndpointDatumsForExecution()
                    throw new RuntimeException(
                        StringUtils.format(NOT_CONVERTIBLE_MESSAGE, endpointDatum.getValue().getDataType(), inputDataType, inputName));
                }
            }
        } else {
            throw new NoSuchElementException(getLogMessagesPrefix() + StringUtils.format("No datum at input '%s'", inputName));
        }
    }

    /**
     * Prints given line to the workflow console. It is temporarily also used to send timeline events.
     * 
     * @param line line to print
     * @param consoleRowType type of the console row. Must be one of {@link ConsoleRow.Type.STDOUT} or
     *        {@link ConsoleRow.Type.STDERR} are allowed
     */
    public void printConsoleRow(String line, Type consoleRowType) {
        if (consoleRowType.equals(Type.LIFE_CYCLE_EVENT)) {
            compExeRelatedInstances.consoleRowsSender.sendTimelineEventAsConsoleRow(consoleRowType, line);
        } else {
            compExeRelatedInstances.consoleRowsSender.sendLogMessageAsConsoleRow(consoleRowType, line,
                compExeRelatedInstances.compExeRelatedStates.executionCount.get());
        }
    }

    /**
     * Closes all outputs.
     */
    public void closeAllOutputs() {
        for (EndpointDescription output : compExeRelatedInstances.compExeCtx.getComponentDescription()
            .getOutputDescriptionsManager().getEndpointDescriptions()) {
            closeOutput(output.getName());
        }
    }

    /**
     * Closes an output with given name.
     * 
     * @param outputName name of output to close.
     */
    public synchronized void closeOutput(String outputName) {
        if (!closedOutputs.containsKey(outputName) || !closedOutputs.get(outputName)) {
            closedOutputs.put(outputName, true);
            compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutput(outputName,
                new InternalTDImpl(InternalTDImpl.InternalTDType.WorkflowFinish));
        } else {
            printConsoleRow(StringUtils.format("Output '%s' already closed. "
                + "Ignored further closing request.", outputName), ConsoleRow.Type.COMPONENT_WARN);
        }
    }

    /**
     * @param outputName name of output
     * @return <code>true</code> if output is closed, otherwise <code>false</code>
     */
    public synchronized boolean isOutputClosed(String outputName) {
        return closedOutputs.containsKey(outputName) && closedOutputs.get(outputName);
    }

    /**
     * Resets an output with given name.
     * 
     * @param outputName name of output
     */
    public void resetOutput(String outputName) {
        validateIfNestedLoopComponent(outputName);
        writeResetOutputData(outputName);
    }

    private void writeResetOutputData(String outputName) {
        try {
            for (Queue<WorkflowGraphHop> hops : compExeRelatedInstances.compExeCtx.getWorkflowGraph()
                .getHopsToTraverseWhenResetting(compExeRelatedInstances.compExeCtx.getExecutionIdentifier())
                .get(outputName)) {
                WorkflowGraphHop firstHop = hops.poll();
                InternalTDImpl resetDatum = new InternalTDImpl(InternalTDImpl.InternalTDType.NestedLoopReset, hops);
                compExeRelatedInstances.compExeScheduler.addResetDataIdSent(resetDatum.getIdentifier());
                compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(outputName, resetDatum,
                    firstHop.getTargetExecutionIdentifier(), firstHop.getTargetInputName());
            }
        } catch (ComponentExecutionException e) {
            throw new RuntimeException("Failed to reset the loop. Double-check your loop. Data between loops must "
                + "only be exchanged via evaluation driver components via appropriate inputs and outputs (self, outer, inner)", e);
        }
    }

    /**
     * @return current execution count of the component. Count starts with 1. It is 1 within {@link Component#start(ComponentContext)} and
     *         is 1 within {@link Component#processInputs()} if {@link Component#start(ComponentContext)} returns <code>false</code> or 2
     *         otherwise.
     */
    public int getExecutionCount() {
        return compExeRelatedInstances.compExeRelatedStates.executionCount.get();
    }

    /**
     * Writes intermediate history data. Each new intermediate history data will overwrite a previous one.
     * 
     * @param compHistoryDataItem {@link ComponentHistoryDataItem} to write
     */
    public void writeIntermediateHistoryData(ComponentHistoryDataItem compHistoryDataItem) {
        if (writeHistoryDataItem(compHistoryDataItem)) {
            compExeRelatedInstances.compExeRelatedStates.intermediateHistoryDataWritten.set(true);
        }
    }

    /**
     * Writes final history data. It will overwrite any intermediate ones.
     * 
     * @param compHistoryDataItem {@link ComponentHistoryDataItem} to write
     */
    public void writeFinalHistoryDataItem(ComponentHistoryDataItem compHistoryDataItem) {
        synchronized (historyDataLock) {
            if (writeHistoryDataItem(compHistoryDataItem)) {
                compExeRelatedInstances.compExeRelatedStates.finalHistoryDataItemWritten.set(true);
            }
        }
    }
    
    /**
     * @return data management id of latest component execution
     */
    public Long getComponentExecutionDataManagementId() {
        ComponentState compState = compExeRelatedInstances.compStateMachine.getState();
        if ((compState.equals(ComponentState.STARTING) || compState.equals(ComponentState.PROCESSING_INPUTS))
            && compExeRelatedInstances.compExeRelatedStates.isComponentCancelled.get()) {
            return null;
        }
        return compExeRelatedInstances.compExeStorageBridge.getComponentExecutionDataManagementId();
    }

    private boolean writeHistoryDataItem(ComponentHistoryDataItem componentHistoryDataItem) {
        if (componentHistoryDataItem == null) {
            printConsoleRow("Failed to store additional workflow data, because data item was 'null'"
                + " which is a developer error", Type.COMPONENT_ERROR);
            LOG.error(StringUtils.format("Failed to store history data item for component '%s' (%s) of workflow '%s' (%s)"
                + " because it was null", compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier()));
            return false;
        }
        try {
            compExeRelatedInstances.compExeStorageBridge.setOrUpdateHistoryDataItem(componentHistoryDataItem.serialize(
                typedDatumService.getSerializer()));
            return true;
        } catch (IOException | ComponentExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    private String getLogMessagesPrefix() {
        return StringUtils.format("Component '%s' (%s) of workflow '%s' (%s): ",
            compExeRelatedInstances.compExeCtx.getInstanceName(),
            compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
            compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
            compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier());
    }
    
    /**
     * Holder class for outputs that are on hold because verification needs to be requested first.
     * 
     * @author Doreen Seider
     */
    class OutputHolder {
        
        private final String outputName;
        
        private final TypedDatum datumToSend;
        
        private final Long outputDmId;

        OutputHolder(String outputName, TypedDatum datumToSend, Long outputDmId) {
            this.outputName = outputName;
            this.datumToSend = datumToSend;
            this.outputDmId = outputDmId;
        }
    }
    

    protected void bindTypedDatumService(TypedDatumService newService) {
        ComponentContextBridge.typedDatumService = newService;
    }

}
