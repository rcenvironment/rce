/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.function.DataManagementServiceStub;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.testutils.WorkflowNodeMockBuilder;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionResult;
import de.rcenvironment.core.workflow.execution.function.WorkflowFunctionService;
import de.rcenvironment.core.workflow.execution.function.testutils.WorkflowFunctionBuilderMock;
import de.rcenvironment.core.workflow.execution.function.testutils.WorkflowFunctionMock;

public class WorkflowIntegratorComponentTest {

    private WorkflowFunctionBuilderMock workflowFunctionBuilder;

    private WorkflowDescription workflowDescription;

    private DataManagementServiceStub dataManagementService;

    private class WorkflowIntegratorComponentUnderTest extends WorkflowIntegratorComponent {

        private final File tempFile = new File("/some path");

        private final File tempDir = new File("/some other path");

        @Override
        protected boolean isFile(File workflowFile) {
            return true;
        }

        @Override
        protected boolean fileExists(File workflowFile) {
            return true;
        }

        @Override
        protected File createTempFile() throws IOException {
            return tempFile;
        }

        @Override
        protected File createTempDir() throws IOException {
            return tempDir;
        }

        public File getTempFile() {
            return this.tempFile;
        }

        public File getTempDirectory() {
            return this.tempDir;
        }
    }

    private WorkflowIntegratorComponentContextMock createMockComponentContext() {
        final WorkflowIntegratorComponentContextMock context = new WorkflowIntegratorComponentContextMock();

        initializeAndBindConfigurationServiceToComponentContext(context);
        initializeAndBindWorkflowLoaderServiceToComponentContext(context);
        initializeAndBindWorkflowFunctionService(context);
        createAndBindDataManagementService(context);

        return context;
    }

    private void initializeAndBindConfigurationServiceToComponentContext(final ComponentContextMock context) {
        final ConfigurationService configurationService = EasyMock.createMock(ConfigurationService.class);
        EasyMock.expect(configurationService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT))
            .andStubReturn(new File("/some/absolute/path"));
        EasyMock.replay(configurationService);

        context.addService(ConfigurationService.class, configurationService);
    }

    private void initializeAndBindWorkflowLoaderServiceToComponentContext(ComponentContextMock context) {
        final PersistentWorkflowDescriptionLoaderService loaderService =
            EasyMock.createMock(PersistentWorkflowDescriptionLoaderService.class);

        try {
            // We only compute the answer lazily here in order to be able to initialize `this.workflowDescription` at any point during the
            // test. If we passed `this.workflowDescription` as the return value here, tests would have to initialize that variable prior to
            // constructing the component context
            EasyMock.expect(loaderService.loadWorkflowDescriptionFromFileConsideringUpdates(EasyMock.anyObject(), EasyMock.anyObject()))
                .andStubAnswer(() -> this.workflowDescription);
        } catch (WorkflowFileException e) {
            // Will not occur since we only call the potentially throwing method on mock object
        }

        EasyMock.replay(loaderService);

        context.addService(PersistentWorkflowDescriptionLoaderService.class, loaderService);
    }

    private void initializeAndBindWorkflowFunctionService(ComponentContextMock context) {
        WorkflowFunctionService service = EasyMock.createMock(WorkflowFunctionService.class);

        workflowFunctionBuilder = new WorkflowFunctionBuilderMock();
        EasyMock.expect(service.createBuilder()).andStubReturn(workflowFunctionBuilder);

        EasyMock.replay(service);

        context.addService(WorkflowFunctionService.class, service);
    }

    private void createAndBindDataManagementService(ComponentContextMock context) {
        dataManagementService = new DataManagementServiceStub();
        dataManagementService.bindToComponentContext(context);
    }

    @Test
    public void whenEndpointAdaptersAreEmptyThenWorkflowFunctionIsCalledWithEmptyInputs() throws ComponentException {
        final WorkflowIntegratorComponentUnderTest component = new WorkflowIntegratorComponentUnderTest();

        final WorkflowIntegratorComponentContextMock componentContext = createMockComponentContext();
        componentContext.setEndpointAdapterConfiguration();
        component.setComponentContext(componentContext);

        final WorkflowFunctionMock workflowFunction = new WorkflowFunctionMock();
        workflowFunctionBuilder.setProduct(workflowFunction);

        WorkflowFunctionResult returnValue = emptySuccessfulWorkflowFunctionResult();
        workflowFunction.setResult(returnValue);

        component.processInputs();

        // Merely reaching this point suffices as assertion, since we want to assert that the component wrote no outputs. If an output had
        // been written to, the component context would have thrown an exception
        assertFalse(workflowFunction.getCapturedInputs().getInputNames().iterator().hasNext());
    }

    @Test
    public void whenFileInputIsGivenThenTheFileIsWrittenToTheFilesystem() throws ComponentException {
        final WorkflowIntegratorComponentUnderTest component = new WorkflowIntegratorComponentUnderTest();

        final WorkflowIntegratorComponentContextMock componentContext = createMockComponentContext();
        componentContext.setEndpointAdapterConfiguration(inputAdapterConfiguration("someInternalName", "someExternalName"));
        component.setComponentContext(componentContext);

        this.workflowDescription = new WorkflowDescriptionMockBuilder()
            .withWorkflowNode(workflowNodeMockWithSingleInput(DataType.FileReference))
            .build();

        final WorkflowFunctionMock workflowFunction = new WorkflowFunctionMock();
        workflowFunctionBuilder.setProduct(workflowFunction);

        this.dataManagementService.expectCopyReferenceToLocalFile();

        componentContext.addFileInput("someExternalName", "some file reference");

        WorkflowFunctionResult returnValue = emptySuccessfulWorkflowFunctionResult();
        workflowFunction.setResult(returnValue);

        component.processInputs();

        final Iterator<String> workflowFunctionInputNamesIterator = workflowFunction.getCapturedInputs().getInputNames().iterator();
        assertEquals("someExternalName", workflowFunctionInputNamesIterator.next());

        final TypedDatum value = workflowFunction.getCapturedInputs().getValueByName("someExternalName");
        assertTrue(value instanceof ShortTextTD);
        assertTrue(((ShortTextTD) value).getShortTextValue().endsWith("some path"));

        assertFalse(workflowFunctionInputNamesIterator.hasNext());

        assertEquals("some file reference", this.dataManagementService.getCapturedFileReference());
        assertEquals(component.getTempFile(), this.dataManagementService.getCapturedCopyTarget());
    }

    @Test
    public void whenDirectoryInputIsGivenThenTheDirectoryIsWrittenToTheFilesystem() throws ComponentException {
        final WorkflowIntegratorComponentUnderTest component = new WorkflowIntegratorComponentUnderTest();

        final WorkflowIntegratorComponentContextMock componentContext = createMockComponentContext();
        componentContext.setEndpointAdapterConfiguration(inputAdapterConfiguration("someInternalName", "someExternalName"));
        component.setComponentContext(componentContext);

        this.workflowDescription = workflowDescription(workflowNodeMockWithSingleInput(DataType.DirectoryReference));

        final WorkflowFunctionMock workflowFunction = new WorkflowFunctionMock();
        workflowFunctionBuilder.setProduct(workflowFunction);

        this.dataManagementService.expectCopyReferenceToLocalDirectory();

        componentContext.addDirectoryInput("someExternalName", "some directory name", "some directory reference");

        WorkflowFunctionResult returnValue = emptySuccessfulWorkflowFunctionResult();
        workflowFunction.setResult(returnValue);

        component.processInputs();

        final Iterator<String> workflowFunctionInputNamesIterator = workflowFunction.getCapturedInputs().getInputNames().iterator();
        assertEquals("someExternalName", workflowFunctionInputNamesIterator.next());

        final TypedDatum value = workflowFunction.getCapturedInputs().getValueByName("someExternalName");
        assertTrue(value instanceof ShortTextTD);
        assertTrue(((ShortTextTD) value).getShortTextValue().endsWith("some directory name"));

        assertFalse(workflowFunctionInputNamesIterator.hasNext());

        assertEquals("some directory reference", this.dataManagementService.getCapturedDirectoryReference());
        assertEquals(component.getTempDirectory(), this.dataManagementService.getCapturedCopyTarget());
    }

    private static WorkflowDescription workflowDescription(WorkflowNode workflowNode) {
        return new WorkflowDescriptionMockBuilder()
            .withWorkflowNode(workflowNode)
            .build();
    }

    private static WorkflowNode workflowNodeMockWithSingleInput(DataType dataType) {
        return new WorkflowNodeMockBuilder()
            .identifier("someIdentifier")
            .addStaticInput("someIdentifier", EndpointDefinition.inputBuilder()
                .name("someInternalName")
                .inputHandlings(Collections.singleton(InputDatumHandling.Queue))
                .defaultInputHandling(InputDatumHandling.Queue)
                .allowedDatatypes(Collections.singleton(dataType))
                .defaultDatatype(dataType)
                .build())
            .build();
    }

    private static String inputAdapterConfiguration(String internalName, String externalName) {
        return "{"
            + "\"type\":\"INPUT\","
            + "\"identifier\":\"someIdentifier\","
            + "\"inputHandling\":\"Queue\","
            + "\"inputExecutionConstraint\":\"Required\","
            + "\"internalName\":\"" + internalName + "\","
            + "\"externalName\":\"" + externalName + "\""
            + "}";
    }

    private static WorkflowFunctionResult emptySuccessfulWorkflowFunctionResult() {
        WorkflowFunctionResult returnValue = EasyMock.createMock(WorkflowFunctionResult.class);
        EasyMock.expect(returnValue.isFailure()).andStubReturn(false);
        EasyMock.expect(returnValue.toMap()).andStubReturn(new HashMap<>());
        EasyMock.replay(returnValue);
        return returnValue;
    }
}
