/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.common.CommandException.Type;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandParser;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationService;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapters;

class WfIntegrateCommandPluginTestHarness {

    private static final String SOME_ERROR_MESSAGE = "some error message";

    private final WfIntegrateCommandPlugin plugin;

    private WorkflowDescription workflowDescription;

    private WorkflowNode lastNodeAdded;

    private Set<EndpointDescription> staticInputsOfLastNodeAdded;

    private Set<EndpointDescription> dynamicInputsOfLastNodeAdded;

    private Set<EndpointDescription> staticOutputsOfLastNodeAdded;

    private Set<EndpointDescription> dynamicOutputsOfLastNodeAdded;

    private PersistentWorkflowDescriptionLoaderService loaderService;

    private WorkflowIntegrationService workflowIntegrationService;

    private Capture<String> outputCapture;

    private Capture<EndpointAdapters> endpointAdaptersCapture;

    private Capture<CommandException> thrownException;

    WfIntegrateCommandPluginTestHarness(WfIntegrateCommandPlugin plugin) {
        this.plugin = plugin;
    }

    public GivenSteps given() {
        return new GivenSteps();
    }

    final class GivenSteps {

        private EndpointDescription lastEndpointAdded;

        // We make the constructor private in order to enforce use of the factory method given() in the enclosing class
        private GivenSteps() {}

        GivenSteps workflowLoaderThrowsExceptionOnLoad() throws WorkflowFileException {
            EasyMock.expect(loaderService.loadWorkflowDescriptionFromFile(EasyMock.anyObject(File.class), EasyMock.eq(null)))
                .andThrow(new WorkflowFileException(SOME_ERROR_MESSAGE));
            EasyMock.replay(loaderService);
            return this;
        }

        GivenSteps workflowLoaderServiceIsPresent() throws WorkflowFileException {
            loaderService = EasyMock.createMock(PersistentWorkflowDescriptionLoaderService.class);
            plugin.bindWorkflowLoaderService(loaderService);
            return this;
        }

        GivenSteps workflowIntegrationServiceIsPresent() {
            workflowIntegrationService = EasyMock.createMock(WorkflowIntegrationService.class);
            plugin.bindWorkflowIntegrationService(workflowIntegrationService);
            return this;
        }

        GivenSteps workflowIntegrationSucceeds(String expectedComponentId) throws IOException {
            endpointAdaptersCapture = Capture.newInstance();

            workflowIntegrationService.integrateWorkflowFileAsComponent(EasyMock.anyObject(WorkflowDescription.class),
                EasyMock.eq(expectedComponentId), EasyMock.capture(endpointAdaptersCapture));
            EasyMock.expectLastCall();
            EasyMock.replay(workflowIntegrationService);
            return this;
        }

        GivenSteps thatNodeHasEndpointWithoutDefinition(String endpointName, String endpointId)
            throws NoSuchMethodException, SecurityException {
            // When constructing an EndpointDescription, the constructor uses the given EndpointDefinition to determine the name of the
            // resulting endpoint. In this case, we do not want the resulting description to have a backing EndpointDefinition, but we still
            // want it to have a name. This scenario occurs in practice if, for example, the component is unavailable. Hence, we opt to
            // partially mock the endpoint description, only replacing the call to getName such that it returns the desired name.
            // getEndpointDefinition() will still return the endpoint definition given during construction, i.e., null in this case.
            final EndpointDescription mockedDescription = EasyMock.createMockBuilder(EndpointDescription.class)
                .withConstructor(EndpointDefinition.class, String.class)
                .withArgs(null, endpointId)
                .addMockedMethod("getName")
                .createMock();

            EasyMock.expect(mockedDescription.getName()).andStubReturn(endpointName);
            EasyMock.replay(mockedDescription);
            this.lastEndpointAdded = mockedDescription;
            staticInputsOfLastNodeAdded.add(this.lastEndpointAdded);
            return this;
        }

        GivenSteps thatNodeHasIntegerInput(String endpointName, String endpointId) {
            final EndpointDefinition inputDefinition = EndpointDefinition.inputBuilder()
                .name(endpointName)
                .defaultDatatype(DataType.Integer)
                .allowedDatatypes(Arrays.asList(DataType.Integer)).build();

            this.lastEndpointAdded = new EndpointDescription(inputDefinition, endpointId);
            staticInputsOfLastNodeAdded.add(this.lastEndpointAdded);
            return this;
        }

        GivenSteps thatNodeHasIntegerInput(final String endpointName, final String endpointId,
            final InputDatumHandling datumHandling, final InputExecutionContraint executionConstraint) {
            final EndpointDefinition inputDefinition = EndpointDefinition.inputBuilder()
                .name(endpointName)
                .defaultDatatype(DataType.Integer)
                .allowedDatatypes(Arrays.asList(DataType.Integer))
                .defaultInputHandling(datumHandling)
                .inputHandlings(Collections.singleton(datumHandling))
                .defaultInputExecutionConstraint(executionConstraint)
                .inputExecutionConstraints(Collections.singleton(executionConstraint)).build();

            this.lastEndpointAdded = new EndpointDescription(inputDefinition, endpointId);
            staticInputsOfLastNodeAdded.add(this.lastEndpointAdded);
            return this;
        }

        GivenSteps thatEndpointHasMetadata(String key, String value) {
            this.lastEndpointAdded.setMetaDataValue(key, value);
            return this;
        }

        GivenSteps thatNodeHasIntegerOutput(String endpointName, String endpointId) {
            final EndpointDefinition outputDefinition = EndpointDefinition.outputBuilder()
                .name(endpointName)
                .defaultDatatype(DataType.Integer)
                .allowedDatatypes(Arrays.asList(DataType.Integer)).build();

            this.lastEndpointAdded = new EndpointDescription(outputDefinition, endpointId);
            staticOutputsOfLastNodeAdded.add(this.lastEndpointAdded);
            return this;
        }

        GivenSteps loadedWorkflowDescriptionHasNode(String nodeName, String nodeIdentifier) {
            lastNodeAdded = EasyMock.createStrictMock(WorkflowNode.class);
            EasyMock.expect(lastNodeAdded.getName()).andStubReturn(nodeName);
            EasyMock.expect(lastNodeAdded.getIdentifierAsObject()).andStubReturn(new WorkflowNodeIdentifier(nodeIdentifier));

            staticInputsOfLastNodeAdded = new HashSet<>();
            dynamicInputsOfLastNodeAdded = new HashSet<>();

            final EndpointDescriptionsManager inputDescriptionsManager =
                createEndpointDescriptionsManagerMock(staticInputsOfLastNodeAdded, dynamicInputsOfLastNodeAdded);
            EasyMock.expect(lastNodeAdded.getInputDescriptionsManager()).andStubReturn(inputDescriptionsManager);

            staticOutputsOfLastNodeAdded = new HashSet<>();
            dynamicOutputsOfLastNodeAdded = new HashSet<>();

            final EndpointDescriptionsManager outputDescriptionsManager =
                createEndpointDescriptionsManagerMock(staticOutputsOfLastNodeAdded, dynamicOutputsOfLastNodeAdded);
            EasyMock.expect(lastNodeAdded.getOutputDescriptionsManager()).andStubReturn(outputDescriptionsManager);
            
            final ConfigurationDescription desc = EasyMock.createMock(ConfigurationDescription.class);
            //EasyMock.expect(desc.containsPlaceholders()).andStubReturn(false);
            EasyMock.replay(desc);
            
            EasyMock.expect(lastNodeAdded.getComponentConfiguration()).andStubReturn(desc);

            EasyMock.replay(lastNodeAdded);

            workflowDescription.addWorkflowNode(lastNodeAdded);

            return this;
        }

        protected EndpointDescriptionsManager createEndpointDescriptionsManagerMock(Set<EndpointDescription> staticEndpoints,
            Set<EndpointDescription> dynamicEndpoints) {
            final EndpointDescriptionsManager endpointDescriptionsManager = EasyMock.createStrictMock(EndpointDescriptionsManager.class);
            endpointDescriptionsManager.addPropertyChangeListener(EasyMock.anyObject(PropertyChangeListener.class));
            EasyMock.expectLastCall();
            EasyMock.expect(endpointDescriptionsManager.getStaticEndpointDescriptions()).andStubReturn(staticEndpoints);
            EasyMock.expect(endpointDescriptionsManager.getDynamicEndpointDescriptions()).andStubReturn(dynamicEndpoints);
            EasyMock.replay(endpointDescriptionsManager);
            return endpointDescriptionsManager;
        }

        GivenSteps workflowLoaderReturnsWorkflowDescription(String workflowIdentifier)
            throws WorkflowFileException {
            workflowDescription = new WorkflowDescription(workflowIdentifier);

            EasyMock.expect(loaderService.loadWorkflowDescriptionFromFile(EasyMock.anyObject(File.class), EasyMock.eq(null)))
                .andReturn(workflowDescription);

            EasyMock.replay(loaderService);
            return this;
        }

        GivenSteps workflowIntegrationThrowsError(String expectedComponentId) throws IOException {
            endpointAdaptersCapture = Capture.newInstance();

            workflowIntegrationService.integrateWorkflowFileAsComponent(EasyMock.anyObject(WorkflowDescription.class),
                EasyMock.eq(expectedComponentId), EasyMock.capture(endpointAdaptersCapture));
            EasyMock.expectLastCall().andThrow(new IOException(SOME_ERROR_MESSAGE));
            EasyMock.replay(workflowIntegrationService);
            return this;
        }

        GivenSteps workflowLoaderServiceIsAbsent() {
            // Since the plugin under test does not have a workflow loader service set by default, we do not need to unbind some service
            // here.
            // In fact, this method mainly exists to enable test writers to specifically state their intended test setup
            return this;
        }

        GivenSteps workflowIntegrationServiceIsAbsent() {
            // Since the plugin under test does not have a workflow integration service set by default, we do not need to unbind some
            // service
            // here. In fact, this method mainly exists to enable test writers to specifically state their intended test setup
            return this;
        }

        GivenSteps thatWorkflowDescriptionContainsPlaceholders() {
            // Warnings are suppressed since this stubbed node is only used in testing code and will thus never actually get serialized
            @SuppressWarnings("serial") final WorkflowNode stubbedWorkflowNode = new WorkflowNode(null) {
                @Override
                public ConfigurationDescription getComponentConfiguration() {
                    final ConfigurationDescription returnValue = EasyMock.createMock(ConfigurationDescription.class);
                    //EasyMock.expect(returnValue.containsPlaceholders()).andReturn(true);
                    EasyMock.replay(returnValue);
                    return returnValue;
                }

                @Override
                public EndpointDescriptionsManager getInputDescriptionsManager() {
                    return createEndpointDescriptionsManagerMock(null, null);
                }

                @Override
                public EndpointDescriptionsManager getOutputDescriptionsManager() {
                    return createEndpointDescriptionsManagerMock(null, null);
                }
            };

            workflowDescription.addWorkflowNode(stubbedWorkflowNode);
            
            return this;
        }
    }

    WhenSteps when() {
        return new WhenSteps();
    }

    final class WhenSteps {

        // We make the constructor private in order to enforce use of the factory method when() in the enclosing class
        private WhenSteps() {
            commandParser = new CommandParser();
            commandParser.registerCommands(getCommands());
        }

        MainCommandDescription[] getCommands() {
            return plugin.getCommands();
        }
        
        private CommandParser commandParser;
        
        /*
         * TODO: figure this out
         */
        
        void executeWfIntegrateCommand(final String... parameters) {
            final CommandContext context = wfIntegrateCommandContext(parameters);
            thrownException = Capture.newInstance();
              
            try {
                commandParser.parseCommand(context).execute();
            } catch (CommandException e) {
                thrownException.setValue(e);
            }
        }

        private CommandContext wfIntegrateCommandContext(String... tokens) {
            final List<String> defaultTokens = Arrays.asList("wf-integrate");
            final List<String> commandTokens = Arrays.asList(tokens);
            final List<String> allTokens = new LinkedList<>();
            allTokens.addAll(defaultTokens);
            allTokens.addAll(commandTokens);

            final TextOutputReceiver outputReceiver = EasyMock.createNiceMock(TextOutputReceiver.class);
            outputCapture = Capture.newInstance(CaptureType.ALL);
            outputReceiver.addOutput(EasyMock.capture(outputCapture));
            EasyMock.expectLastCall().anyTimes();
            EasyMock.replay(outputReceiver);

            return new CommandContext(allTokens, outputReceiver, null);
        }
    }

    public ThenSteps then() {
        return new ThenSteps();
    }

    class ThenSteps {

        private EndpointAdapter lastQueriedEndpointAdapter;

        private Collection<EndpointAdapter> queriedEndpointAdapters = new HashSet<>();

        ThenSteps outputReceiverWasNeverCalled() {
            assertFalse(outputCapture.hasCaptured());
            return this;
        }

        ThenSteps outputReceiverHasPrinted(String... lines) {
            final List<String> capturedLines = outputCapture.getValues();
            final List<String> expectedLines = Arrays.asList(lines);

            assertEquals(expectedLines, capturedLines);
            return this;
        }

        ThenSteps workflowIntegrationServiceWasCalledWithEmptyEndpointAdapters() {
            assertTrue(endpointAdaptersCapture.hasCaptured());
            assertTrue(endpointAdaptersCapture.getValue().isEmpty());
            return this;
        }

        ThenSteps workflowIntegrationServiceWasCalledWithEndpointAdapter(String internalEndpoint, String externalEndpointName) {
            assertTrue(endpointAdaptersCapture.hasCaptured());
            final EndpointAdapters capturedValue = endpointAdaptersCapture.getValue();

            assertTrue(StringUtils.format("No endpoint adapter with internal name '%s' was defined.", internalEndpoint),
                capturedValue.containsAdapterWithInternalEndpointName(internalEndpoint));
            this.lastQueriedEndpointAdapter = capturedValue.getByInternalEndpointName(internalEndpoint);
            assertNotNull(this.lastQueriedEndpointAdapter);
            assertEquals(externalEndpointName, this.lastQueriedEndpointAdapter.getExternalName());

            this.queriedEndpointAdapters.add(this.lastQueriedEndpointAdapter);

            return this;
        }

        ThenSteps thatEndpointAdapterHasDatumHandling(InputDatumHandling datumHandling) {
            assertEquals(datumHandling, this.lastQueriedEndpointAdapter.getInputDatumHandling());
            return this;
        }

        ThenSteps thatEndpointHasExecutionConstraint(InputExecutionContraint executionConstraint) {
            assertEquals(executionConstraint, this.lastQueriedEndpointAdapter.getInputExecutionConstraint());
            return this;
        }

        ThenSteps noCommandExceptionWasThrown() {
            if (thrownException.hasCaptured()) {
                fail("Exception was unexpectedly thrown: " + thrownException.getValue());
            }
            return this;
        }

        private void commandExceptionWasThrown() {
            assertTrue("Expected an exception, but none was thrown", thrownException.hasCaptured());
        }

        ThenSteps executionErrorWasThrown() {
            // Guard against programmer error and first check explicitly that an exception was thrown before asserting its properties
            commandExceptionWasThrown();
            final CommandException capturedException = thrownException.getValue();
            assertEquals(Type.EXECUTION_ERROR, capturedException.getType());
            return this;
        }

        ThenSteps syntaxErrorWasThrown() {
            // Guard against programmer error and first check explicitly that an exception was thrown before asserting its properties
            commandExceptionWasThrown();
            final CommandException capturedException = thrownException.getValue();
            assertEquals(Type.SYNTAX_ERROR, capturedException.getType());
            return this;
        }

        ThenSteps exceptionHasCommandString(String expectedCommandString) {
            // Guard against programmer error and first check explicitly that an exception was thrown before asserting its properties
            commandExceptionWasThrown();
            final CommandException capturedException = thrownException.getValue();
            assertEquals(expectedCommandString, capturedException.getCommandString().trim());
            return this;
        }

        ThenSteps exceptionHasMessage(String expectedMessage) {
            // Guard against programmer error and first check explicitly that an exception was thrown before asserting its properties
            commandExceptionWasThrown();
            final CommandException capturedException = thrownException.getValue();
            assertEquals(expectedMessage, capturedException.getMessage());
            return this;
        }

        ThenSteps loaderServiceWasNotCalled() {
            EasyMock.verify(loaderService);
            return this;
        }

        ThenSteps loaderServiceWasCalled() {
            EasyMock.verify(loaderService);
            return this;
        }

        ThenSteps integrationServiceWasNotCalled() {
            EasyMock.verify(workflowIntegrationService);
            return this;
        }

        ThenSteps integrationServiceWasCalled() {
            EasyMock.verify(workflowIntegrationService);
            return this;
        }

        ThenSteps thatEndpointAdapterIsInputAdapter() {
            assertTrue("Expected an input adapter, but " + this.lastQueriedEndpointAdapter + " is an output adapter",
                this.lastQueriedEndpointAdapter.isInputAdapter());
            return this;
        }

        ThenSteps thatEndpointAdapterIsOutputAdapter() {
            assertTrue("Expected an output adapter, but " + this.lastQueriedEndpointAdapter + " is an input adapter",
                this.lastQueriedEndpointAdapter.isOutputAdapter());
            return this;
        }

        public void noOtherEndpointAdaptersWereGiven() {
            final Collection<EndpointAdapter> unqueriedEndpointAdapterDefinitions = new LinkedList<>();
            for (EndpointAdapter definition : endpointAdaptersCapture.getValue()) {
                if (!queriedEndpointAdapters.contains(definition)) {
                    unqueriedEndpointAdapterDefinitions.add(definition);
                }
            }

            assertTrue("The following additional endpoint adapters were given: " + unqueriedEndpointAdapterDefinitions,
                unqueriedEndpointAdapterDefinitions.isEmpty());
        }

    }

}
