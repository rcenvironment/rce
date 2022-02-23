/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.internal;

import static de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationServiceImplMatchers.hasCreatedFilesInToolIntegrationDirectory;
import static de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationServiceImplMatchers.hasDisabledFileWatcherDuringIntegration;
import static de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationServiceImplMatchers.hasIntegratedWorkflowComponent;
import static de.rcenvironment.core.utils.testing.ByteArrayOutputStreamMatchers.isUTF8StringThat;
import static de.rcenvironment.core.utils.testing.JsonMatchers.isJsonList;
import static de.rcenvironment.core.utils.testing.JsonMatchers.isJsonObject;
import static de.rcenvironment.core.utils.testing.ListMatchers.containingInAnyOrder;
import static de.rcenvironment.core.utils.testing.ListMatchers.emptyList;
import static de.rcenvironment.core.utils.testing.ListMatchers.list;
import static de.rcenvironment.core.utils.testing.MapMatchers.containsMapping;
import static de.rcenvironment.core.utils.testing.MapMatchers.map;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

public class WorkflowIntegrationServiceImplTest {
    
    private WorkflowIntegrationServiceImplUnderTest serviceUnderTest;

    @Before
    public void createServiceUnderTest() {
        this.serviceUnderTest = new WorkflowIntegrationServiceImplUnderTest();

        createAndBindToolIntegrationService();
        createAndBindConfigurationService();
        createAndBindToolIntegrationServiceRegistry();
    }

    private void createAndBindToolIntegrationService() {
        final ToolIntegrationService toolIntegrationService = EasyMock.createMock(ToolIntegrationService.class);
        toolIntegrationService.setFileWatcherActive(EasyMock.captureBoolean(serviceUnderTest.getSetFileWatcherActiveParameters()));
        EasyMock.expectLastCall();

        toolIntegrationService.registerRecursive(EasyMock.capture(serviceUnderTest.getToolIntegrationNameParameter()),
            EasyMock.capture(serviceUnderTest.getToolIntegrationContextParameter()));
        EasyMock.expectLastCall();

        toolIntegrationService.setFileWatcherActive(EasyMock.captureBoolean(serviceUnderTest.getSetFileWatcherActiveParameters()));
        EasyMock.expectLastCall();

        EasyMock.replay(toolIntegrationService);

        serviceUnderTest.bindToolIntegrationService(toolIntegrationService);
    }

    private void createAndBindConfigurationService() {
        final ConfigurationService configurationService = EasyMock.createStrictMock(ConfigurationService.class);
        final File integrationRoot = new File("foo");
        EasyMock.expect(configurationService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT))
            .andStubReturn(integrationRoot);
        EasyMock.replay(configurationService);
        serviceUnderTest.bindConfigurationService(configurationService);
    }

    private void createAndBindToolIntegrationServiceRegistry() {
        final ToolIntegrationContextRegistry registry = EasyMock.createStrictMock(ToolIntegrationContextRegistry.class);
        final WorkflowIntegrationContext context = new WorkflowIntegrationContext();
        EasyMock.expect(registry.getToolIntegrationContextByType("workflow")).andStubReturn(context);
        EasyMock.replay(registry);
        serviceUnderTest.bindToolIntegrationServiceRegistry(registry);
    }

    @Test
    public void integrationServiceDisablesFileWatcherDuringIntegration() throws ComponentException, IOException {
        final EndpointAdapters eads = new EndpointAdapters.Builder().build();
        final WorkflowDescription workflowDescription = new WorkflowDescription(someWorkflowDescriptionIdentifier());
        serviceUnderTest.integrateWorkflowFileAsComponent(workflowDescription, someToolName(), eads);
        
        assertThat(serviceUnderTest, hasDisabledFileWatcherDuringIntegration());
    }
    
    @Test
    public void integrationServiceIntegratesWorkflowComponent() throws ComponentException, IOException {
        final EndpointAdapters eads = new EndpointAdapters.Builder().build();
        final WorkflowDescription workflowDescription = new WorkflowDescription(someWorkflowDescriptionIdentifier());
        serviceUnderTest.integrateWorkflowFileAsComponent(workflowDescription, someToolName(), eads);
        
        assertThat(serviceUnderTest, hasIntegratedWorkflowComponent(someToolName()));
    }
    
    public void integrationServiceCreatesWorkflowAndConfigurationFiles() throws IOException, ComponentException {
        final EndpointAdapters eads = new EndpointAdapters.Builder().build();
        final WorkflowDescription workflowDescription = new WorkflowDescription(someWorkflowDescriptionIdentifier());
        serviceUnderTest.integrateWorkflowFileAsComponent(workflowDescription, someToolName(), eads);
        
        assertThat(serviceUnderTest, hasCreatedFilesInToolIntegrationDirectory(
                someToolName() + File.separator + "workflow.wf",
                someToolName() + File.separator + "configuration.json"));
    }

    @Test
    public void integrationServiceWritesCorrectConfigurationForEmptyEndpointAdapters() throws IOException, ComponentException {
        final EndpointAdapters eads = new EndpointAdapters.Builder().build();
        serviceUnderTest.integrateWorkflowFileAsComponent(new WorkflowDescription(someWorkflowDescriptionIdentifier()), someToolName(),
            eads);

        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(allOf(
                containsMapping("outputs", emptyList()),
                containsMapping("inputs", emptyList()),
                containsMapping("endpointAdapters", isJsonList(emptyList())
            )))));
    }
    
    @Test
    public void integrationServiceWritesCorrectToolNameToConfigurationFile() throws ComponentException, IOException {
        final EndpointAdapters eads = new EndpointAdapters.Builder().build();

        serviceUnderTest.integrateWorkflowFileAsComponent(new WorkflowDescription(someWorkflowDescriptionIdentifier()), someToolName(),
            eads);
        
        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(
            containsMapping("toolName", someToolName()))));
    }
    
    @Test
    public void integrationServiceWritesIntegrationTypeWorkflowToConfigurationFile() throws ComponentException, IOException {
        final EndpointAdapters eads = new EndpointAdapters.Builder().build();

        serviceUnderTest.integrateWorkflowFileAsComponent(new WorkflowDescription(someWorkflowDescriptionIdentifier()), someToolName(),
            eads);
        
        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(
            containsMapping("integrationType", "Workflow"))));
    }
    
    @Test
    public void integrationServiceWritesCorrectConfigurationForSingleInputAdapter() throws ComponentException, IOException {
        final EndpointAdapters eads = new EndpointAdapters.Builder()
            .addEndpointAdapter(EndpointAdapter.inputAdapterBuilder()
                .externalEndpointName(someExternalInputName())
                .internalEndpointName(someInternalInputName())
                .workflowNodeIdentifier(someWorkflowNodeId())
                .dataType(DataType.Integer)
                .inputExecutionConstraint(InputExecutionContraint.Required)
                .inputHandling(InputDatumHandling.Queue)
                .build())
            .build();

        serviceUnderTest.integrateWorkflowFileAsComponent(new WorkflowDescription(someWorkflowDescriptionIdentifier()), someToolName(),
            eads);

        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(
            containsMapping("endpointAdapters", isJsonList(containingInAnyOrder(
                    map(allOf(
                        containsMapping("internalName", someInternalInputName()),
                        containsMapping("identifier", someWorkflowNodeId()),
                        containsMapping("externalName", someExternalInputName()),
                        containsMapping("inputHandling", InputDatumHandling.Queue.toString()),
                        containsMapping("inputExecutionConstraint", InputExecutionContraint.Required.toString()),
                        containsMapping("type", "INPUT")
                    ))
                ))))));
        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(
            containsMapping("inputs", list(containingInAnyOrder(map(allOf(
                containsMapping("endpointFileName", ""),
                containsMapping("inputHandling", "Queue"),
                containsMapping("defaultInputHandling", "Queue"),
                containsMapping("endpointDataType", "Integer"),
                containsMapping("endpointName", someExternalInputName()),
                containsMapping("inputExecutionConstraint", "Required"),
                containsMapping("defaultInputExecutionConstraint", "Required"),
                containsMapping("endpointFolder", "")
                )))))
            )));
    }
    
    @Test
    public void integrationServiceWritesCorrectConfigurationForSingleOutputAdapter() throws ComponentException, IOException {
        final EndpointAdapters eads = new EndpointAdapters.Builder()
            .addEndpointAdapter(EndpointAdapter.outputAdapterBuilder()
                .externalEndpointName("externalOutputName")
                .internalEndpointName("internalOutputName")
                .workflowNodeIdentifier(someWorkflowNodeId())
                .dataType(DataType.Float)
                .build())
            .build();

        serviceUnderTest.integrateWorkflowFileAsComponent(new WorkflowDescription(someWorkflowDescriptionIdentifier()), someToolName(),
            eads);

        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(allOf(
            containsMapping("endpointAdapters", isJsonList(containingInAnyOrder(
                    map(allOf(
                        containsMapping("internalName", "internalOutputName"),
                        containsMapping("identifier", someWorkflowNodeId()),
                        containsMapping("externalName", "externalOutputName"),
                        containsMapping("type", "OUTPUT")
                    ))
                )))
            ))));

        assertThat(serviceUnderTest.getConfigurationFileOutputStream(), isUTF8StringThat(isJsonObject(allOf(
            containsMapping("outputs", list(containingInAnyOrder(map(allOf(
                containsMapping("endpointFileName", ""),
                containsMapping("inputHandling", "-"),
                containsMapping("endpointDataType", "Float"),
                containsMapping("endpointName", "externalOutputName"),
                containsMapping("inputExecutionConstraint", "Required"),
                containsMapping("endpointFolder", "")
                )))))
            ))));
    }

    private String someWorkflowDescriptionIdentifier() {
        return "someWorkflowDescriptionIdentifier";
    }

    private String someToolName() {
        return "someToolName";
    }

    private String someWorkflowNodeId() {
        return "someWorkflowNodeId";
    }

    private String someInternalInputName() {
        return "internalInputName";
    }

    private String someExternalInputName() {
        return "externalInputName";
    }
}
