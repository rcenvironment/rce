/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.File;
import java.util.List;
import java.util.Set;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Interface for components to the workflow engine.
 * 
 * @author Doreen Seider
 */
public interface ComponentContext extends ExecutionContext {

    /**
     * @return key set of read-only configuration
     */
    Set<String> getReadOnlyConfigurationKeys();

    /**
     * @return key set of user-writable configuration (writable before workflow execution)
     */
    Set<String> getConfigurationKeys();

    /**
     * @param key configuration key (read-only or user-writable)
     * @return configuration value or <code>null</code> if none exists for given key
     */
    String getConfigurationValue(String key);

    /**
     * @param configKey configuration key (read-only or user-writable)
     * @param metaDataKey meta data key
     * @return meta data value or <code>null</code> if none exists for given keys
     */
    String getConfigurationMetaDataValue(String configKey, String metaDataKey);

    /**
     * @return all inputs, which are "required" or which are "required if connected" and connected to an output
     */
    Set<String> getInputs();

    /**
     * @param inputName name of input
     * @return <code>true</code> if input is static, otherwise <code>false</code>
     */
    boolean isStaticInput(String inputName);

    /**
     * @param inputName name of input
     * @return <code>true</code> if input is dynamic, otherwise <code>false</code>
     */
    boolean isDynamicInput(String inputName);

    /**
     * @param outputName name of output
     * @return <code>true</code> if input is dynamic, otherwise <code>false</code>
     */
    boolean isDynamicOutput(String outputName);

    /**
     * @param inputName name of the input
     * @return dynamic input identifier or <code>null</code> if inputName belongs to a static input
     */
    String getDynamicInputIdentifier(String inputName);

    /**
     * @param inputName name of the input
     * @return {@link DataType} of the input
     */
    DataType getInputDataType(String inputName);

    /**
     * @param inputName name of the input
     * @return key set of input meta data
     */
    Set<String> getInputMetaDataKeys(String inputName);

    /**
     * @param inputName name of the input
     * @param metaDataKey meta data key
     * @return meta data value or <code>null</code> if none exists for given input and key
     */
    String getInputMetaDataValue(String inputName, String metaDataKey);

    /**
     * @return all inputs with a value. I.e. {@link #readInput(String)} will be return a {@link TypedDatum}.
     */
    Set<String> getInputsWithDatum();

    /**
     * @param inputName name of the input
     * @return {@link TypedDatum} representing the value of the input
     */
    TypedDatum readInput(String inputName);

    /**
     * @return all outputs, no matter if they are connected to an input or not
     */
    Set<String> getOutputs();

    /**
     * @param outputName name of the output
     * @return dynamic input identifier or <code>null</code> if inputName belongs to a static input
     */
    String getDynamicOutputIdentifier(String outputName);

    /**
     * @param outputName name of the output
     * @return {@link DataType} of the output
     */
    DataType getOutputDataType(String outputName);

    /**
     * @param outputName name of the output
     * @return key set of output meta data
     */
    Set<String> getOutputMetaDataKeys(String outputName);

    /**
     * @param outputName name of the output
     * @param metaDataKey meta data key
     * @return meta data value or <code>null</code> if none exists for given output and key
     */
    String getOutputMetaDataValue(String outputName, String metaDataKey);

    /**
     * Writes {@link TypedDatum} to an output. It will be sent to inputs connected to that output.
     * 
     * @param outputName name of output
     * @param value {@link TypedDatum} to send
     */
    void writeOutput(String outputName, TypedDatum value);

    /**
     * Resets output. A component must reset an output, if it controls an inner loop of a workflow to indicated that it is done. In other
     * case, it must not reset any output.
     */
    void resetOutputs();

    /**
     * Closes output. A component must close an output, if it control the workflow to indicated that it is done. In other case it must not
     * close any output.
     * 
     * @param outputName name of output
     */
    void closeOutput(String outputName);

    /**
     * Closes all outputs. A component must close an output, if it controls the workflow to indicated that it is done. In other case it must
     * not close any output.
     */
    void closeAllOutputs();

    /**
     * @param outputName name of output
     * @return <code>true</code> if output is closed, otherwise <code>false</code>
     */
    boolean isOutputClosed(String outputName);

    /**
     * @return working directory of the component. It will be deleted after component is disposed.
     */
    File getWorkingDirectory();

    /**
     * Must be called if an external program was started by the workflow component. Needed to have external program execution properly
     * represented in the workflow timeline.
     */
    void announceExternalProgramStart();

    /**
     * Must be called if an external program terminated. Needed to have external program execution properly represented in the workflow
     * timeline.
     */
    void announceExternalProgramTermination();

    /**
     * @param <T> the service class to acquire
     * @param clazz class of service to acquire
     * @return service instance of desired class or <code>null</code> if the service is not available
     */
    <T> T getService(Class<T> clazz);

    /**
     * @return current execution count of the component. Count starts with 1. It is 1 within {@link Component#start(ComponentContext)} and
     *         is 1 within {@link Component#processInputs()} if {@link Component#start(ComponentContext)} returns <code>false</code> or 2
     *         otherwise.
     */
    int getExecutionCount();

    /**
     * @return map with persisted data
     */
    PersistedComponentData getPersistedData(); // not yet implemented

    /**
     * @return execution identifier of associated workflow
     */
    String getWorkflowExecutionIdentifier();

    /**
     * @return name of associated workflow
     */
    String getWorkflowInstanceName();

    /**
     * @return hosting node of associated workflow (controller)
     */
    LogicalNodeId getWorkflowNodeId();

    /**
     * Writes intermediate history data. Each new intermediate history data will overwrite a previous one.
     * 
     * @param historyDataItem {@link ComponentHistoryDataItem} to write
     */
    void writeIntermediateHistoryData(ComponentHistoryDataItem historyDataItem); // not yet
                                                                                 // implemented

    /**
     * Writes final history data. It will overwrite any intermediate ones.
     * 
     * @param historyDataItem {@link ComponentHistoryDataItem} to write
     */
    void writeFinalHistoryDataItem(ComponentHistoryDataItem historyDataItem);

    /**
     * @return name of the component
     */
    String getComponentName();

    /**
     * @return identifier of the component
     */
    String getComponentIdentifier();

    /**
     * @return all not required and not connected inputs
     */
    Set<String> getInputsNotConnected();

    /**
     * @return a {@link ComponentLog} instance used to log tool and component messages
     */
    ComponentLog getLog();

    /**
     * @param inputName name of input
     * @return {@link EndpointCharacter} of given input
     */
    EndpointCharacter getInputCharacter(String inputName);

    /**
     * @param outputName name of output
     * @return {@link EndpointCharacter} of given output
     */
    EndpointCharacter getOutputCharacter(String outputName);

    /**
     * 
     * @param identifier of the dynamic endpoint
     * @return list with all inputs that have that identifier.
     */
    List<String> getDynamicInputsWithIdentifier(String identifier);

    /**
     * @param identifier of the dynamic endpoint
     * @return list with all outputs that have that identifier.
     */
    List<String> getDynamicOutputsWithIdentifier(String identifier);

}
