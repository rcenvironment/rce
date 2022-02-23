/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectWriter;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.workflow.execution.SynchronousWorkflowExecutionService;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionException;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionInputs;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionResult;

/**
 * This class represents a function whose result is computed by executing a workflow. In contrast to functions as understood in the context
 * of Java, such a function may return more than a single result value.
 * 
 * @author Alexander Weinert
 */
class WorkflowFunctionImpl implements WorkflowFunction {

    public static class Builder implements WorkflowFunction.Builder {

        private final WorkflowFunctionImpl product = new WorkflowFunctionImpl();
        
        private String internalName;
        
        private String externalName;
        
        private String callingWorkflowName;

        @Override
        public Builder withWorkflowDescription(WorkflowDescription descriptionParam) {
            this.product.workflowDescription = descriptionParam;
            return this;
        }

        @Override
        public Builder withEndpointAdapters(EndpointAdapters endpointAdapterDefinitionsMap) {
            this.product.endpointAdapters = endpointAdapterDefinitionsMap;
            return this;
        }

        @Override
        public Builder withInternalName(final String internalNameParam) {
            this.internalName = internalNameParam;
            return this;
        }

        @Override
        public Builder withExternalName(final String externalNameParam) {
            this.externalName = externalNameParam;
            return this;
        }

        @Override
        public Builder withCallingWorkflowName(final String callingWorkflowNameParam) {
            this.callingWorkflowName = callingWorkflowNameParam;
            return this;
        }

        @Override
        public Builder setComponentContext(ComponentContext context) {
            this.product.componentContext = context;
            return this;
        }

        public Builder bindComponentDataManagementService(ComponentDataManagementService service) {
            this.product.componentDataManagementService = service;
            return this;
        }

        public Builder bindPlatformService(PlatformService service) {
            this.product.idOfStartingNode = service.getLocalDefaultLogicalNodeId();
            return this;
        }

        public Builder bindTypedDatumSerializer(TypedDatumSerializer serializer) {
            this.product.typedDatumSerializer = serializer;
            return this;
        }

        public Builder bindWorkflowExecutionService(SynchronousWorkflowExecutionService service) {
            this.product.synchronousWorkflowExecutionService = service;
            return this;
        }
        
        public Builder bindFileUtils(final FileUtils utils) {
            this.product.fileUtils = utils;
            return this;
        }

        @Override
        public WorkflowFunction build() {
            Objects.requireNonNull(this.product.workflowDescription, "No WorkflowDescription given for construction of WorkflowFunction");

            if (this.product.endpointAdapters == null) {
                this.product.endpointAdapters = new EndpointAdapters.Builder().build();
            }
            
            this.product.workflowExecutionName = StringUtils.format("%s running as component '%s' of workflow '%s'", this.internalName,
                this.externalName, this.callingWorkflowName);

            return this.product;
        }

    }

    private WorkflowDescription workflowDescription;

    private EndpointAdapters endpointAdapters;

    /**
     * The context of the component executing this function. Required in order to move the output resulting from the execution of the
     * underlying workflow into the local datamanagement.
     */
    private ComponentContext componentContext;

    /** The ID of the node that shall execute the underlying workflow. */
    private LogicalNodeId idOfStartingNode;
    
    /** The name under which the underlying workflow shall be executed. */
    private String workflowExecutionName;

    private WorkflowEditor.Factory workflowEditorFactory = new WorkflowEditor.Factory();

    private TypedDatumSerializer typedDatumSerializer;

    private ComponentDataManagementService componentDataManagementService;

    private SynchronousWorkflowExecutionService synchronousWorkflowExecutionService;
    
    private FileUtils fileUtils;

    @Override
    public WorkflowFunctionResult execute(WorkflowFunctionInputs inputs) throws WorkflowFunctionException {
        final File inputDirectory = createInputDirectory();
        logCreationOfInputDirectory(inputDirectory);

        final File outputDirectory = createOutputDirectory();
        logCreationOfOutputDirectory(outputDirectory);

        workflowEditorFactory
            .setInputDirectory(inputDirectory)
            .setOutputDirectory(outputDirectory);

        final WorkflowDescription augmentedWorkflowDescription = augmentWorkflowWithEndpointAdapterComponents();

        writeWorkflowInputsToFile(inputDirectory, inputs);
        final boolean workflowExecutionSucceeded = executeWorkflow(augmentedWorkflowDescription);

        if (!workflowExecutionSucceeded) {
            return WorkflowFunctionResult.buildFailure();
        }

        return createWorkflowFunctionResultFromOutput(outputDirectory);
    }

    private WorkflowFunctionResult createWorkflowFunctionResultFromOutput(final File outputDirectory) throws WorkflowFunctionException {
        // If the workflow that we call as a function does not have any outputs, not output adapter has run. Hence, no file 'outputs.json'
        // was created, thus, we will receive an IOException when trying to parse it. Thus, we check this special case here and exit early
        // before attempting to parse the outputs of the workflow.
        if (!this.endpointAdapters.containsOutputAdapters()) {
            return WorkflowFunctionResult.successBuilder().build();
        }

        Map<String, String> outputValueMap = readOutputValueMap(outputDirectory);

        LogFactory.getLog(this.getClass()).debug(StringUtils.format("Read output values '%s'", outputValueMap));

        return createWorkflowFunctionResultFromOutputMap(outputValueMap);
    }

    private boolean executeWorkflow(final WorkflowDescription augmentedWorkflowDescription) throws WorkflowFunctionException {
        final WorkflowExecutionContext workflowExecutionContext = new WorkflowExecutionContextBuilder(augmentedWorkflowDescription)
            .setInstanceName(this.workflowExecutionName)
            .setNodeIdentifierStartedExecution(idOfStartingNode)
            .build();

        final boolean workflowExecutionSucceeded;
        try {
            workflowExecutionSucceeded = synchronousWorkflowExecutionService.executeWorkflow(workflowExecutionContext);
        } catch (ComponentException e) {
            throw new WorkflowFunctionException("Could not execute underlying workflow synchronously", e);
        }
        return workflowExecutionSucceeded;
    }

    private WorkflowFunctionResult createWorkflowFunctionResultFromOutputMap(Map<String, String> outputValueMap)
        throws WorkflowFunctionException {
        final WorkflowFunctionResult.Builder resultBuilder = WorkflowFunctionResult.successBuilder();
        for (Entry<String, String> output : outputValueMap.entrySet()) {
            final DataType outputDataType = this.endpointAdapters.getByExternalEndpointName(output.getKey()).getDataType();

            final TypedDatum replacementDatum;
            if (outputDataType.equals(DataType.FileReference)) {
                replacementDatum = copyOutputfileIntoLocalDataManagement(output.getValue());
            } else if (outputDataType.equals(DataType.DirectoryReference)) {
                replacementDatum = copyOutputDirectoryIntoLocalDataManagement(resultBuilder, output);
            } else {
                replacementDatum = typedDatumSerializer.deserialize(output.getValue().toString());
            }
            resultBuilder.addResult(output.getKey(), replacementDatum);
        }

        final WorkflowFunctionResult result = resultBuilder.build();
        return result;
    }

    private TypedDatum copyOutputDirectoryIntoLocalDataManagement(final WorkflowFunctionResult.Builder resultBuilder,
        Entry<String, String> output) throws WorkflowFunctionException {
        final ShortTextTD directoryPath = (ShortTextTD) typedDatumSerializer.deserialize(output.getValue());
        try {
            final File dirToLoad = new File(directoryPath.getShortTextValue());
            final TypedDatum replacementDatum = componentDataManagementService
                .createDirectoryReferenceTDFromLocalDirectory(componentContext, dirToLoad, dirToLoad.getName());
            LogFactory.getLog(this.getClass())
                .info(StringUtils.format("Read directory at '%s' into datamanagement", dirToLoad.getAbsolutePath()));
            return replacementDatum;
        } catch (IOException e) {
            throw new WorkflowFunctionException("Could not store directory into local data management", e);
        }
    }

    private TypedDatum copyOutputfileIntoLocalDataManagement(String path)
        throws WorkflowFunctionException {
        final ShortTextTD filePath = (ShortTextTD) typedDatumSerializer.deserialize(path);
        try {
            final File fileToLoad = new File(filePath.getShortTextValue());
            final TypedDatum replacementDatum = componentDataManagementService
                .createFileReferenceTDFromLocalFile(componentContext, fileToLoad, fileToLoad.getName());
            LogFactory.getLog(this.getClass())
                .info(StringUtils.format("Read file at '%s' into datamanagement", fileToLoad.getAbsolutePath()));
            return replacementDatum;
        } catch (IOException e) {
            throw new WorkflowFunctionException("Could not store file into local data management", e);
        }
    }

    @SuppressWarnings("unchecked") // See initialization of outputValueMap for reasoning
    protected Map<String, String> readOutputValueMap(final File outputDirectory) throws WorkflowFunctionException {
        final File outputConfiguration = this.fileUtils.createFile(outputDirectory, "outputs.json");
        Map<String, String> outputValueMap;
        try {
            // readValue returns a Map without generics. Since we know that we are parsing a JsonFile here, we can safely cast this to a
            // Map<String, String>
            outputValueMap = (Map<String, String>) this.fileUtils.getObjectMapper().readValue(outputConfiguration, Map.class);
        } catch (IOException e) {
            throw new WorkflowFunctionException(StringUtils.format("Could not read output values from file '%s'", outputConfiguration), e);
        }
        return outputValueMap;
    }

    protected WorkflowDescription augmentWorkflowWithEndpointAdapterComponents() {
        final WorkflowEditor editor = workflowEditorFactory.buildFromWorkflowDescription(workflowDescription);
        for (final EndpointAdapter endpointAdapterDefinition : endpointAdapters) {
            if (endpointAdapterDefinition.isInputAdapter()) {
                editor.addInputAdapter(endpointAdapterDefinition);
            } else if (endpointAdapterDefinition.isOutputAdapter()) {
                editor.addOutputAdapter(endpointAdapterDefinition);
            }
        }
        return editor.getResult();
    }

    protected File createOutputDirectory() throws WorkflowFunctionException {
        final File outputDirectory;
        try {
            outputDirectory = this.fileUtils.createTempDir("outputdirectory");
        } catch (IOException e) {
            throw new WorkflowFunctionException("Could not create temporary output directory", e);
        }
        return outputDirectory;
    }

    protected File createInputDirectory() throws WorkflowFunctionException {
        final File inputDirectory;
        try {
            inputDirectory = this.fileUtils.createTempDir("inputdirectory");
        } catch (IOException e) {
            throw new WorkflowFunctionException("Could not create temporary input directory", e);
        }
        return inputDirectory;
    }

    protected void writeWorkflowInputsToFile(final File inputDirectory, WorkflowFunctionInputs inputs) throws WorkflowFunctionException {
        final Map<String, Object> inputsMap = new HashMap<>();
        for (final String inputName : inputs.getInputNames()) {
            inputsMap.put(inputName, typedDatumSerializer.serialize(inputs.getValueByName(inputName)));
        }

        final File inputsFile = createInputsFile(inputDirectory);
        ObjectWriter writer = this.fileUtils.getObjectWriter();
        try {
            writer.writeValue(inputsFile, inputsMap);
        } catch (IOException e) {
            throw new WorkflowFunctionException("Could not write inputs file", e);
        }
    }

    protected File createInputsFile(final File inputDirectory) {
        return this.fileUtils.createFile(inputDirectory, "inputs.json");
    }

    protected void logCreationOfInputDirectory(final File inputDirectory) {
        LogFactory.getLog(this.getClass())
            .debug(StringUtils.format("Created temporary inputs directory at '%s'", inputDirectory.getAbsolutePath()));
    }

    protected void logCreationOfOutputDirectory(final File outputDirectory) {
        LogFactory.getLog(this.getClass())
            .debug(StringUtils.format("Created temporary outputs directory at '%s'", outputDirectory.getAbsolutePath()));
    }
}
