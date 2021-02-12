/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.workflow.execution.function.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.testutils.WorkflowNodeMockBuilder;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.workflow.execution.SynchronousWorkflowExecutionService;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;
import de.rcenvironment.core.workflow.execution.function.InputAdapterComponent;
import de.rcenvironment.core.workflow.execution.function.OutputAdapterComponent;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunction;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionException;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionInputs;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionResult;

/**
 * @author Alexander Weinert
 */
public class WorkflowFunctionServiceImplTest {

    private class WorkflowDescriptionMock extends WorkflowDescription {

        public WorkflowDescriptionMock(String identifier) {
            super(identifier);
        }

        @Override
        public WorkflowDescription clone() {
            return this;
        }

    }

    private WorkflowFunctionServiceImpl service;

    private File inputDirectory;

    private File inputsFile;

    private File outputDirectory;

    private File outputsFile;

    private Capture<Map<String, String>> inputFilesWritten;

    private Capture<File> outputFilesRead;

    private Capture<WorkflowExecutionContext> workflowsExecuted;

    private Map<String, String> outputMap;

    private TypedDatumSerializer typedDatumSerializer;

    private ComponentDataManagementService componentDataManagement;

    private boolean shouldWorkflowExecutionSucceed = true;

    @Before
    public void initializeWorkflowFunctionService() throws ComponentException, IOException {
        service = new WorkflowFunctionServiceImpl();

        createAndBindTypedDatumService();
        service.bindPlatformService(EasyMock.createMock(PlatformService.class));
        createAndBindComponentDataManagementService();

        createAndBindWorkflowExecutionService();
        createAndBindTempFileService();
        createAndBindFileCreator();

        createAndBindObjectWriterSuplier();
        createAndBindObjectMapperSupplier();
    }

    private void createAndBindComponentDataManagementService() {
        componentDataManagement = EasyMock.createMock(ComponentDataManagementService.class);
        service.bindComponentDataManagementService(componentDataManagement);
    }

    private void createAndBindTypedDatumService() {
        final TypedDatumService typedDatumService = EasyMock.createMock(TypedDatumService.class);

        typedDatumSerializer = EasyMock.createMock(TypedDatumSerializer.class);
        EasyMock.expect(typedDatumService.getSerializer()).andStubReturn(typedDatumSerializer);

        EasyMock.replay(typedDatumService);
        service.bindTypedDatumService(typedDatumService);
    }

    private void createAndBindWorkflowExecutionService() throws ComponentException {
        this.workflowsExecuted = Capture.newInstance(CaptureType.ALL);

        final SynchronousWorkflowExecutionService executionService = EasyMock.createMock(SynchronousWorkflowExecutionService.class);
        EasyMock.expect(executionService.executeWorkflow(EasyMock.capture(this.workflowsExecuted)))
            .andStubAnswer(() -> this.shouldWorkflowExecutionSucceed);
        EasyMock.replay(executionService);

        service.bindSynchronousWorkflowExecutionService(executionService);
    }

    private void createAndBindObjectWriterSuplier() throws IOException, JsonGenerationException, JsonMappingException {
        final ObjectWriter mockedWriter = EasyMock.createMock(ObjectWriter.class);
        inputFilesWritten = Capture.newInstance(CaptureType.ALL);
        mockedWriter.writeValue(EasyMock.eq(inputsFile), EasyMock.capture(inputFilesWritten));
        EasyMock.expectLastCall();

        EasyMock.replay(mockedWriter);

        service.bindObjectWriterSupplier(() -> mockedWriter);
    }

    private void createAndBindTempFileService() throws IOException {
        final TempFileService tempFileService = EasyMock.createMock(TempFileService.class);

        inputDirectory = new File("inputDirectory");
        EasyMock.expect(tempFileService.createManagedTempDir("inputdirectory")).andReturn(inputDirectory);

        outputDirectory = new File("outputDirectory");
        EasyMock.expect(tempFileService.createManagedTempDir("outputdirectory")).andReturn(outputDirectory);

        EasyMock.replay(tempFileService);

        service.bindTempFileService(tempFileService);
    }

    private void createAndBindFileCreator() {
        inputsFile = new File("inputFile");
        outputsFile = new File("outputFile");

        final BiFunction<File, String, File> creator = (parent, child) -> {
            if (parent == inputDirectory && child.equals("inputs.json")) {
                return inputsFile;
            } else if (parent == outputDirectory && child.equals("outputs.json")) {
                return outputsFile;
            } else {
                return null;
            }
        };

        service.bindFileCreator(creator);
    }

    private void createAndBindObjectMapperSupplier() throws IOException, JsonParseException, JsonMappingException {
        this.outputMap = new HashMap<>();

        final ObjectMapper objectMapper = EasyMock.createMock(ObjectMapper.class);
        outputFilesRead = Capture.newInstance(CaptureType.ALL);
        EasyMock.expect(objectMapper.readValue(EasyMock.capture(outputFilesRead), EasyMock.eq(Map.class)))
            .andStubAnswer(() -> this.outputMap);
        EasyMock.replay(objectMapper);
        service.bindObjectMapperSupplier(() -> objectMapper);
    }

    @Test
    public void whenFunctionIsCreatedWithoutWorkflowDescriptionThenAnExceptionShouldBeThrown() {
        NullPointerException exception = null;
        try {
            service.createBuilder().build();
        } catch (NullPointerException e) {
            exception = e;
        }

        assertNotNull("Expected a NullPointerException to be thrown", exception);
        assertTrue("The message of the NullPointerException should contain a hint to the missing workflow description",
            exception.getMessage().contains("WorkflowDescription"));
    }
    
    @Test
    public void whenWorkflowExecutionFailsThenWorkflowFunctionReturnsAnError()
        throws WorkflowFunctionException, JsonGenerationException, JsonMappingException, IOException {

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(new WorkflowDescription(workflowDescriptionIdentifier()))
            .build();

        this.shouldWorkflowExecutionSucceed = false;

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));
        
        assertTrue(result.isFailure());
    }

    @Test
    public void whenNoEndpointAdaptersAreGivenThenEmptyInputsFileIsWritten()
        throws WorkflowFunctionException, JsonGenerationException, JsonMappingException, IOException {

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(new WorkflowDescription(workflowDescriptionIdentifier()))
            .build();

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));

        assertFalse(result.isFailure());
        assertTrue(inputFilesWritten.hasCaptured());
        assertTrue(inputFilesWritten.getValue().isEmpty());
    }

    @Test
    public void whenNoEndpointAdaptersAreGivenThenNoOutputFileIsRead() throws WorkflowFunctionException {
        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(new WorkflowDescription(workflowDescriptionIdentifier()))
            .build();

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));

        assertFalse(result.isFailure());
        assertTrue(!outputFilesRead.hasCaptured());
    }

    /**
     * In order to pass the inputs to the function to the executed workflow and to obtain its results, the workflow function uses
     * {@link InputAdapterComponent}s and {@link OutputAdapterComponent}s, respectively. This test asserts that no adapters are used if
     * there are no adapted endpoints.
     */
    @Test
    public void whenNoEndpointAdaptersAreGivenThenNoAdapterComponentsAreInjected() throws WorkflowFunctionException {
        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(new WorkflowDescription(workflowDescriptionIdentifier()))
            .build();

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));

        assertFalse(result.isFailure());
        assertTrue(this.workflowsExecuted.hasCaptured());
        assertEquals(1, workflowsExecuted.getValues().size());

        final WorkflowExecutionContext executedWorkflow = workflowsExecuted.getValues().get(0);

        assertTrue("WorkflowDescription of executed workflow should not contain any nodes",
            executedWorkflow.getWorkflowDescription().getWorkflowNodes().isEmpty());
    }

    @Test
    public void whenOnlyInputAdaptersAreGivenThenOnlyAnInputAdapterIsInjected() throws WorkflowFunctionException, ComponentException {
        final WorkflowDescription workflowToExecute = new WorkflowDescriptionMock(workflowDescriptionIdentifier());

        final EndpointDefinition inputDefinition = createIntegerQueueInputDefinition(internalInputName());

        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier("some workflow node identifier")
            .addStaticInput("some input identifier", inputDefinition)
            .build();

        workflowToExecute.addWorkflowNode(node);
        final LogicalNodeId controller = EasyMock.createMock(LogicalNodeId.class);
        EasyMock.replay(controller);
        workflowToExecute.setControllerNode(controller);

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(workflowToExecute)
            .withEndpointAdapters(
                new EndpointAdapters.Builder()
                    .addEndpointAdapter(EndpointAdapter.inputAdapterBuilder()
                        .internalEndpointName(internalInputName())
                        .externalEndpointName(externalInputName())
                        .dataType(DataType.Integer)
                        .inputHandling(EndpointDefinition.InputDatumHandling.Queue)
                        .inputExecutionConstraint(InputExecutionContraint.Required)
                        .workflowNodeIdentifier("some workflow node identifier")
                        .build())
                    .build())
            .build();

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));

        assertFalse(result.isFailure());
        assertTrue(this.workflowsExecuted.hasCaptured());
        assertEquals(1, workflowsExecuted.getValues().size());

        final WorkflowDescription executedWorkflow = workflowsExecuted.getValues().get(0).getWorkflowDescription();

        assertEquals("WorkflowDescription of executed workflow should contain two nodes", 2,
            executedWorkflow.getWorkflowNodes().size());
        assertTrue(executedWorkflow.getWorkflowNodes().stream()
            .anyMatch(nodeParam -> nodeParam.getIdentifierAsObject().toString().equals("some workflow node identifier")));
        assertTrue("There should be an input adapter", executedWorkflow.getWorkflowNodes().stream().anyMatch(this::isInputAdapter));

        assertTrue("There should be a connection from the input adapter to the adapted input",
            executedWorkflow.getConnections().stream().anyMatch(conn -> connectsFromInputAdapter(conn) && connectsToAdaptedInput(conn)));

    }

    @Test
    public void whenOnlyOutputAdaptersAreGivenThenOnlyAnOutputAdapterIsInjected() throws ComponentException, WorkflowFunctionException {
        final WorkflowDescription workflowToExecute = new WorkflowDescriptionMock(workflowDescriptionIdentifier());

        final EndpointDefinition outputDefinition = createIntegerOutputDefinition(internalOutputName());

        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier("some workflow node identifier")
            .addStaticOutput("some output identifier", outputDefinition)
            .build();

        workflowToExecute.addWorkflowNode(node);
        final LogicalNodeId controller = EasyMock.createMock(LogicalNodeId.class);
        EasyMock.replay(controller);
        workflowToExecute.setControllerNode(controller);

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(workflowToExecute)
            .withEndpointAdapters(
                new EndpointAdapters.Builder()
                    .addEndpointAdapter(EndpointAdapter.outputAdapterBuilder()
                        .internalEndpointName(internalOutputName())
                        .externalEndpointName(externalOutputName())
                        .dataType(DataType.Integer)
                        .workflowNodeIdentifier("some workflow node identifier")
                        .build())
                    .build())
            .build();

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));

        assertFalse(result.isFailure());
        assertTrue(this.workflowsExecuted.hasCaptured());
        assertEquals(1, workflowsExecuted.getValues().size());

        final WorkflowDescription executedWorkflow = workflowsExecuted.getValues().get(0).getWorkflowDescription();

        assertEquals("WorkflowDescription of executed workflow should contain two nodes", 2,
            executedWorkflow.getWorkflowNodes().size());
        assertTrue(executedWorkflow.getWorkflowNodes().stream()
            .anyMatch(nodeParam -> nodeParam.getIdentifierAsObject().toString().equals("some workflow node identifier")));
        assertTrue("There should be an output adapter", executedWorkflow.getWorkflowNodes().stream().anyMatch(this::isOutputAdapter));

        assertTrue("There should be a connection from the adapted output to the output adapter",
            executedWorkflow.getConnections().stream()
                .anyMatch(conn -> connectsToOutputAdapter(conn) && connectsFromAdaptedOutput(conn)));

    }

    @Test
    public void whenInputAndOutputAdaptersAreGivenThenInputAndOutputAdaptersAreInjected() throws ComponentException, WorkflowFunctionException {
        final WorkflowDescription workflowToExecute = new WorkflowDescriptionMock(workflowDescriptionIdentifier());

        final EndpointDefinition inputDefinition = createIntegerQueueInputDefinition(internalInputName());
        final EndpointDefinition outputDefinition = createIntegerOutputDefinition(internalOutputName());

        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier("some workflow node identifier")
            .addStaticInput("some input identifier", inputDefinition)
            .addStaticOutput("some output identifier", outputDefinition)
            .build();

        workflowToExecute.addWorkflowNode(node);
        final LogicalNodeId controller = EasyMock.createMock(LogicalNodeId.class);
        EasyMock.replay(controller);
        workflowToExecute.setControllerNode(controller);

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(workflowToExecute)
            .withEndpointAdapters(
                new EndpointAdapters.Builder()
                    .addEndpointAdapter(EndpointAdapter.outputAdapterBuilder()
                        .internalEndpointName(internalOutputName())
                        .externalEndpointName(externalOutputName())
                        .dataType(DataType.Integer)
                        .workflowNodeIdentifier("some workflow node identifier")
                        .build())
                    .addEndpointAdapter(EndpointAdapter.inputAdapterBuilder()
                        .internalEndpointName(internalInputName())
                        .externalEndpointName(externalInputName())
                        .dataType(DataType.Integer)
                        .inputHandling(EndpointDefinition.InputDatumHandling.Queue)
                        .inputExecutionConstraint(InputExecutionContraint.Required)
                        .workflowNodeIdentifier("some workflow node identifier")
                        .build())
                    .build())
            .build();

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));

        assertFalse(result.isFailure());
        assertTrue(this.workflowsExecuted.hasCaptured());
        assertEquals(1, workflowsExecuted.getValues().size());

        final WorkflowDescription executedWorkflow = workflowsExecuted.getValues().get(0).getWorkflowDescription();

        assertEquals("WorkflowDescription of executed workflow should contain three nodes", 3,
            executedWorkflow.getWorkflowNodes().size());
        assertTrue("The original workflow node should still be present in the executed workflow",
            executedWorkflow.getWorkflowNodes().stream()
                .anyMatch(nodeParam -> nodeParam.getIdentifierAsObject().toString().equals("some workflow node identifier")));
        assertTrue("There should be an input adapter", executedWorkflow.getWorkflowNodes().stream().anyMatch(this::isInputAdapter));
        assertTrue("There should be an output adapter", executedWorkflow.getWorkflowNodes().stream().anyMatch(this::isOutputAdapter));

        assertTrue("There should be a connection from the input adapter to the adapted input",
            executedWorkflow.getConnections().stream()
                .anyMatch(conn -> (connectsFromInputAdapter(conn) && connectsToAdaptedInput(conn))));

        assertTrue("There should be a connection from the adapted output to the output adapter",
            executedWorkflow.getConnections().stream()
                .anyMatch(conn -> (connectsToOutputAdapter(conn) && connectsFromAdaptedOutput(conn))));
    }

    @Test
    public void whenFileOutputIsAdaptedThenTheFileIsLoadedIntoDatamanagement() throws ComponentException, IOException, WorkflowFunctionException {
        final WorkflowDescription workflowToExecute = new WorkflowDescriptionMock(workflowDescriptionIdentifier());

        final EndpointDefinition outputDefinition = createFileOutputDefinition(internalOutputName());

        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier("some workflow node identifier")
            .addStaticOutput("some output identifier", outputDefinition)
            .build();

        workflowToExecute.addWorkflowNode(node);
        final LogicalNodeId controller = EasyMock.createMock(LogicalNodeId.class);
        EasyMock.replay(controller);
        workflowToExecute.setControllerNode(controller);

        final ComponentContext context = new ComponentContextMock();

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(workflowToExecute)
            .setComponentContext(context)
            .withEndpointAdapters(
                new EndpointAdapters.Builder()
                    .addEndpointAdapter(EndpointAdapter.outputAdapterBuilder()
                        .internalEndpointName(internalOutputName())
                        .externalEndpointName(externalOutputName())
                        .dataType(DataType.FileReference)
                        .workflowNodeIdentifier("some workflow node identifier")
                        .build())
                    .build())
            .build();

        this.outputMap.put(externalOutputName(), "internal file reference");

        final ShortTextTD internalFilePath = createShortTextTDMock("/somedirectory/somefile");
        EasyMock.expect(this.typedDatumSerializer.deserialize("internal file reference")).andStubReturn(internalFilePath);
        EasyMock.replay(this.typedDatumSerializer);
        
        final FileReferenceTD replacedFile = EasyMock.createMock(FileReferenceTD.class);
        EasyMock.replay(replacedFile);

        final Capture<String> nameWrittenToDataManagement = Capture.newInstance();
        EasyMock.expect(this.componentDataManagement
            .createFileReferenceTDFromLocalFile(EasyMock.eq(context), EasyMock.anyObject(File.class),
                EasyMock.capture(nameWrittenToDataManagement)))
            .andStubReturn(replacedFile);
        EasyMock.replay(this.componentDataManagement);

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));
        
        final Optional<TypedDatum> returnedFile = result.getResultByIdentifier(externalOutputName());
        assertTrue(returnedFile.isPresent());
        assertTrue(nameWrittenToDataManagement.hasCaptured());
        assertEquals("somefile", nameWrittenToDataManagement.getValue());
        assertSame(replacedFile, returnedFile.get());
    }

    @Test
    public void whenDirectoryOutputIsAdaptedThenTheDirectoryIsLoadedIntoDatamanagement() throws ComponentException, IOException, WorkflowFunctionException {
        final WorkflowDescription workflowToExecute = new WorkflowDescriptionMock(workflowDescriptionIdentifier());

        final EndpointDefinition outputDefinition = createDirectoryOutputDefinition(internalOutputName());

        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier("some workflow node identifier")
            .addStaticOutput("some output identifier", outputDefinition)
            .build();

        workflowToExecute.addWorkflowNode(node);
        final LogicalNodeId controller = EasyMock.createMock(LogicalNodeId.class);
        EasyMock.replay(controller);
        workflowToExecute.setControllerNode(controller);

        final ComponentContext context = new ComponentContextMock();

        final WorkflowFunction function = service.createBuilder()
            .withInternalName(internalWorkflowName())
            .withExternalName(externalWorkflowName())
            .withWorkflowDescription(workflowToExecute)
            .setComponentContext(context)
            .withEndpointAdapters(
                new EndpointAdapters.Builder()
                    .addEndpointAdapter(EndpointAdapter.outputAdapterBuilder()
                        .internalEndpointName(internalOutputName())
                        .externalEndpointName(externalOutputName())
                        .dataType(DataType.DirectoryReference)
                        .workflowNodeIdentifier("some workflow node identifier")
                        .build())
                    .build())
            .build();

        this.outputMap.put(externalOutputName(), "internal file reference");

        final ShortTextTD internalFilePath = createShortTextTDMock("/someparent/somechild");
        EasyMock.expect(this.typedDatumSerializer.deserialize("internal file reference")).andStubReturn(internalFilePath);
        EasyMock.replay(this.typedDatumSerializer);
        
        final DirectoryReferenceTD replacedDirectory = EasyMock.createMock(DirectoryReferenceTD.class);
        EasyMock.replay(replacedDirectory);

        final Capture<String> nameWrittenToDataManagement = Capture.newInstance();
        EasyMock.expect(this.componentDataManagement
            .createDirectoryReferenceTDFromLocalDirectory(EasyMock.eq(context), EasyMock.anyObject(File.class),
                EasyMock.capture(nameWrittenToDataManagement)))
            .andStubReturn(replacedDirectory);
        EasyMock.replay(this.componentDataManagement);

        final WorkflowFunctionResult result = function.execute(WorkflowFunctionInputs.createFromMap(new HashMap<>()));
        
        final Optional<TypedDatum> returnedDirectory = result.getResultByIdentifier(externalOutputName());
        assertTrue(returnedDirectory.isPresent());
        assertTrue(nameWrittenToDataManagement.hasCaptured());
        assertEquals("somechild", nameWrittenToDataManagement.getValue());
        assertSame(replacedDirectory, returnedDirectory.get());
    }

    private ShortTextTD createShortTextTDMock(String value) {
        final ShortTextTD outputFilePath = EasyMock.createMock(ShortTextTD.class);
        EasyMock.expect(outputFilePath.getShortTextValue()).andStubReturn(value);
        EasyMock.replay(outputFilePath);
        return outputFilePath;
    }

    private boolean isInputAdapter(WorkflowNode nodeParam) {
        return nodeParam.getComponentDescription().getComponentInstallation().getComponentRevision()
            .getClassName().equals(InputAdapterComponent.class.getCanonicalName());
    }

    private boolean connectsToAdaptedInput(Connection connection) {
        return connection.getInput().getIdentifier().equals("some input identifier");
    }

    private boolean connectsFromInputAdapter(Connection connection) {
        return isInputAdapter(connection.getSourceNode());
    }

    private boolean isOutputAdapter(final WorkflowNode nodeParam) {
        return nodeParam.getComponentDescription().getComponentInstallation().getComponentRevision()
            .getClassName().equals(OutputAdapterComponent.class.getCanonicalName());
    }

    private boolean connectsFromAdaptedOutput(Connection connection) {
        return connection.getOutput().getIdentifier().equals("some output identifier");
    }

    private boolean connectsToOutputAdapter(Connection connection) {
        return isOutputAdapter(connection.getTargetNode());
    }

    private String internalOutputName() {
        return "internal output";
    }

    private String externalOutputName() {
        return "external output";
    }

    private String internalInputName() {
        return "internal input";
    }

    private String externalInputName() {
        return "external input";
    }

    private String externalWorkflowName() {
        return "some external name";
    }

    private String internalWorkflowName() {
        return "some internal name";
    }

    private String workflowDescriptionIdentifier() {
        return "some workflow description identifier";
    }

    private EndpointDefinition createIntegerQueueInputDefinition(String name) {
        return EndpointDefinition.inputBuilder()
            .name(name)
            .inputHandlings(new HashSet<>(Arrays.asList(EndpointDefinition.InputDatumHandling.Queue)))
            .defaultInputHandling(EndpointDefinition.InputDatumHandling.Queue)
            .allowedDatatype(DataType.Integer)
            .defaultDatatype(DataType.Integer)
            .build();
    }

    private EndpointDefinition createIntegerOutputDefinition(String name) {
        return EndpointDefinition.outputBuilder()
            .name(name)
            .allowedDatatype(DataType.Integer)
            .defaultDatatype(DataType.Integer)
            .build();
    }

    private EndpointDefinition createFileOutputDefinition(String name) {
        return EndpointDefinition.outputBuilder()
            .name(name)
            .allowedDatatype(DataType.FileReference)
            .defaultDatatype(DataType.FileReference)
            .build();
    }

    private EndpointDefinition createDirectoryOutputDefinition(String name) {
        return EndpointDefinition.outputBuilder()
            .name(name)
            .allowedDatatype(DataType.DirectoryReference)
            .defaultDatatype(DataType.DirectoryReference)
            .build();
    }
}
