/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.command;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.utils.common.StringUtils;

public class WfIntegrateCommandPluginTest {

    private static final String SOME_UUID_1 = "c6f85f3f-5dff-4940-a4d9-bb91363d5668";

    private static final String SOME_UUID_2 = "3c88f1d1-f043-4327-9413-2b2bb158b9f8";

    private static final String SOME_UUID_3 = "ba0bfcef-e70b-4040-a125-79cacc859b43";

    private static final String SOME_EXTERNAL_OUTPUT_NAME = "externalOutput";

    private static final String SOME_INTERNAL_OUTPUT_NAME = "internalOutput";

    private static final String SOME_INTERNAL_OUTPUT_NAME_2 = "internalInput2";

    private static final String VERBOSE_FLAG = "-v";

    private static final String SOME_INTERNAL_INPUT_NAME = "internalInput";

    private static final String SOME_INTERNAL_INPUT_NAME_2 = "someInput2";

    private static final String SOME_EXTERNAL_INPUT_NAME = "externalInput";

    private static final String SOME_COMPONENT_ID = "componentId";

    private static final String SOME_WORKFLOWFILE_PATH = "pathToFile";

    private static final String SOME_WORKFLOW_IDENTIFIER = "workflowIdentifier";

    private static final String SOME_INVALID_EXPOSURE_PARAMETER = "loremipsum:dolor:sit:amet";

    private static final String SOME_NODE_ID_WITH_SPACES = "some node";

    private static final String SOME_INTERNAL_INPUT_NAME_WITH_SPACES = "some input";

    private static final String SOME_INVALID_COMPONENT_ID = "LPT1";

    private WfIntegrateCommandPluginTestHarness pluginHarness;

    @Before
    public void createTestHarness() {
        this.pluginHarness = new WfIntegrateCommandPluginTestHarness(new WfIntegrateCommandPlugin());
    }

    @Test
    public void hasSingleCommandDescription() {
        // WHEN
        final Collection<CommandDescription> commandDescriptions = pluginHarness.when().getCommandDescriptions();

        // THEN
        singleCommandDescriptionReturned(commandDescriptions);
        final CommandDescription singleCommandDescription = commandDescriptions.iterator().next();
        commandDescriptionHasStaticPart(singleCommandDescription, "wf integrate");
        commandDescriptionHasDynamicPart(singleCommandDescription, "<toolname> <workflow file>");
        commandDescriptionHasFirstLine(singleCommandDescription, "integrate a workflow file as a component");
        commandDescriptionHasNoAdditionalLines(singleCommandDescription);
    }

    private void singleCommandDescriptionReturned(final Collection<CommandDescription> commandDescriptions) {
        assertEquals(1, commandDescriptions.size());
    }

    private void commandDescriptionHasStaticPart(final CommandDescription commandDescription, String expectedStaticPart) {
        assertEquals(expectedStaticPart, commandDescription.getStaticPart());
    }

    private void commandDescriptionHasDynamicPart(final CommandDescription commandDescription, String expectedDynamicPart) {
        assertEquals(expectedDynamicPart, commandDescription.getDynamicPart());
    }

    private void commandDescriptionHasFirstLine(final CommandDescription commandDescription, String expectedLine) {
        assertEquals(expectedLine, commandDescription.getFirstLine());
    }

    private void commandDescriptionHasNoAdditionalLines(final CommandDescription commandDescription) {
        assertEquals(0, commandDescription.getAdditionalLines().length);
    }

    @Test
    public void commandWithoutComponentId() throws CommandException, WorkflowFileException, IOException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsAbsent()
            .workflowIntegrationServiceIsAbsent();

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand();

        // THEN
        pluginHarness.then()
            .outputReceiverWasNeverCalled()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(wfIntegrateCommand())
            .exceptionHasMessage("Missing component name");
    }
    
    @Test
    public void commandWithInvalidComponentId() throws CommandException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsAbsent()
            .workflowIntegrationServiceIsAbsent();
        
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_INVALID_COMPONENT_ID);
        
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(String.join(" ", wfIntegrateCommand(), SOME_INVALID_COMPONENT_ID))
            .exceptionHasMessage("Invalid component ID");
    }

    @Test
    public void commandWithoutWorkflowfilePath() throws CommandException, WorkflowFileException, IOException {
        // GIVEN
        this.pluginHarness.given()
            .workflowLoaderServiceIsAbsent()
            .workflowIntegrationServiceIsAbsent();

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID);

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(String.join(" ", wfIntegrateCommand(), SOME_COMPONENT_ID))
            .exceptionHasMessage("Missing filename");
    }

    @Test
    public void whenWorkflowLoadingFailsThenTheCommandFailsGracefully() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderThrowsExceptionOnLoad();

        pluginHarness.given()
            .workflowIntegrationServiceIsAbsent();

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH);

        // THEN
        pluginHarness.then()
            .loaderServiceWasNotCalled()
            .executionErrorWasThrown()
            .exceptionHasMessage("Workflow file at 'pathToFile' could not be parsed");
    }

    @Test
    public void whenExposureFlagIsNotGivenThenTheCommandFailsGracefully() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER);

        pluginHarness.given()
            .workflowIntegrationServiceIsAbsent();

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, SOME_INVALID_EXPOSURE_PARAMETER);

        // THEN
        pluginHarness.then()
            .loaderServiceWasNotCalled()
            .syntaxErrorWasThrown()
            .exceptionHasMessage(parseErrorMessageHeader() + unexpectedExposureFlagErrorMessage(SOME_INVALID_EXPOSURE_PARAMETER));
    }

    @Test
    public void whenExposureParameterParsingFailsThenTheCommandFailsGracefully() throws WorkflowFileException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER);

        pluginHarness.given()
            .workflowIntegrationServiceIsAbsent();

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(), SOME_INVALID_EXPOSURE_PARAMETER);

        // THEN
        pluginHarness.then()
            .loaderServiceWasNotCalled()
            .syntaxErrorWasThrown()
            .exceptionHasMessage(exposureParameterErrorMessage(SOME_INVALID_EXPOSURE_PARAMETER));
    }

    private String unexpectedExposureFlagErrorMessage(String givenExposureFlag) {
        return "Unexpected exposure flag '" + givenExposureFlag + "'. "
            + "Expected '--expose', '--expose-input[s]', or '--expose-output[s]'. "
            + "Skipping this token.";
    }

    @Test
    public void whenWorkflowIntegrationFailsThenTheCommandFailsGracefully() throws WorkflowFileException, IOException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER);

        pluginHarness.given()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationThrowsError(SOME_COMPONENT_ID);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH);

        // THEN
        pluginHarness.then()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled();

        pluginHarness.then()
            .executionErrorWasThrown()
            .exceptionHasMessage("Could not integrate workflow 'pathToFile' as component 'componentId'");
    }

    @Test
    public void validCommandWithoutExposureParameters() throws CommandException, WorkflowFileException, IOException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER);

        pluginHarness.given()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH);

        // THEN
        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEmptyEndpointAdapters()
            .outputReceiverWasNeverCalled();
    }

    @Test
    public void validCommandWithSingleExposureParameter() throws CommandException, WorkflowFileException, IOException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER);

        pluginHarness.given()
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_INTERNAL_INPUT_NAME);

        pluginHarness.given()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then().noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)
            .outputReceiverWasNeverCalled();
    }

    @Test
    public void validVerboseCommandWithInputAndOutputExposureParameter() throws CommandException, WorkflowFileException, IOException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER);

        pluginHarness.given()
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_INTERNAL_INPUT_NAME)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_OUTPUT_NAME, SOME_INTERNAL_OUTPUT_NAME);

        pluginHarness.given()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME), exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_OUTPUT_NAME, SOME_EXTERNAL_OUTPUT_NAME));

        // THEN
        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_OUTPUT_NAME, SOME_EXTERNAL_OUTPUT_NAME)
            .outputReceiverHasPrinted(verboseOutput());
    }

    @Test
    public void validCommandWithAbsentNodeThrowsException() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(String.join(" ", wfIntegrateCommand(), VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH,
                exposeFlag(), exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)))
            .exceptionHasMessage(nodeNotPresentErrorMessage(SOME_COMPONENT_ID));
    }

    private String wfIntegrateCommand() {
        return "wf integrate";
    }

    @Test
    public void validCommandWithAmbiguousNodeThrowsException() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .loadedWorkflowDescriptionHasNode(SOME_UUID_1, SOME_UUID_2);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(
                String.join(" ", wfIntegrateCommandVerbose(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                    exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)))
            .exceptionHasMessage(ambiguousNodeErrorMessage(SOME_UUID_1,
                nameWithId(SOME_UUID_1, SOME_UUID_2),
                nameWithId(SOME_COMPONENT_ID, SOME_UUID_1)));
    }

    private String nameWithId(String name, String id) {
        return StringUtils.format("%s [ID: %s]", name, id);
    }

    @Test
    public void validCommandWithAbsentEndpointsThrowsException() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(
                String.join(" ", wfIntegrateCommandVerbose(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                    exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)))
            .exceptionHasMessage(
                absentEndpointErrorMessage(SOME_INTERNAL_INPUT_NAME, SOME_COMPONENT_ID, SOME_UUID_1, presentInputs(), presentOutputs()));
    }

    @Test
    public void validCommandWithAbsentEndpointProvidesExistingInputs() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME_2));

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(
                String.join(" ", wfIntegrateCommandVerbose(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                    exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME_2)))
            .exceptionHasMessage(
                absentEndpointErrorMessage(SOME_INTERNAL_INPUT_NAME_2, SOME_COMPONENT_ID, SOME_UUID_1,
                    presentInputs(nameWithId(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)), presentOutputs()));
    }


    @Test
    public void validCommandWithAbsentEndpointProvidesExistingOutputs() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_OUTPUT_NAME, SOME_UUID_2);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_OUTPUT_NAME_2));

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(
                String.join(" ", wfIntegrateCommandVerbose(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                    exposureParameter(SOME_UUID_1, SOME_INTERNAL_OUTPUT_NAME_2)))
            .exceptionHasMessage(
                absentEndpointErrorMessage(SOME_INTERNAL_OUTPUT_NAME_2, SOME_COMPONENT_ID, SOME_UUID_1,
                    presentInputs(), presentOutputs(nameWithId(SOME_INTERNAL_OUTPUT_NAME, SOME_UUID_2))));
    }

    private String presentInputs(String... inputs) {
        final String header = "  Inputs:  ";
        if (inputs.length == 0) {
            return "";
        } else {
            // We pad each output such that the list of outputs is left aligned. Hence, the number of spaces is the same as the number of
            // characters in the header
            return header + String.join("\n           ", inputs);
        }
    }

    private String presentOutputs(String... outputs) {
        final String header = "  Outputs: ";
        if (outputs.length == 0) {
            return "";
        } else {
            // We pad each output such that the list of outputs is left aligned. Hence, the number of spaces is the same as the number of
            // characters in the header
            return header + String.join("\n           ", outputs);
        }
    }

    private String wfIntegrateCommandVerbose() {
        return "wf integrate -v";
    }

    @Test
    public void validCommandWithAmbiguousEndpointThrowsException() throws WorkflowFileException {
        // GIVEN
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowLoaderReturnsWorkflowDescription(WfIntegrateCommandPluginTest.SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_3);

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(VERBOSE_FLAG, SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(String.join(" ", wfIntegrateCommandVerbose(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH,
                exposeFlag(), exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)))
            .exceptionHasMessage(ambiguousEndpointErrorMessage());
    }

    @Test
    public void metadataOfEndpointsIsParsedCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatEndpointHasMetadata(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING, InputDatumHandling.Queue.name())
            .thatEndpointHasMetadata(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                InputExecutionContraint.RequiredIfConnected.name());

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)
            .thatEndpointAdapterHasDatumHandling(InputDatumHandling.Queue)
            .thatEndpointHasExecutionConstraint(InputExecutionContraint.RequiredIfConnected)
            .outputReceiverWasNeverCalled();
    }

    @Test
    public void partiallyAbsentMetadataOfEndpointIsParsedCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(
                SOME_INTERNAL_INPUT_NAME, SOME_UUID_2, InputDatumHandling.Constant, InputExecutionContraint.RequiredIfConnected)
            .thatEndpointHasMetadata(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING, InputDatumHandling.Queue.name());

        // WHEN
        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        // THEN
        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)
            .thatEndpointAdapterHasDatumHandling(InputDatumHandling.Constant)
            .thatEndpointHasExecutionConstraint(InputExecutionContraint.RequiredIfConnected)
            .outputReceiverWasNeverCalled();
    }

    @Test
    public void exposeInputIsParsedCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeInputFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)
            .thatEndpointAdapterIsInputAdapter();

    }

    @Test
    public void exposeOutputIsParsedCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeOutputFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_EXTERNAL_INPUT_NAME)
            .thatEndpointAdapterIsOutputAdapter();
    }

    @Test
    public void simpleExposureParameterIsParsedCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1, SOME_INTERNAL_INPUT_NAME));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_INTERNAL_INPUT_NAME);
    }

    @Test
    public void componentExposureParameterIsParsedCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_OUTPUT_NAME, SOME_UUID_3);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_UUID_1));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_INTERNAL_INPUT_NAME)
            .thatEndpointAdapterIsInputAdapter()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_OUTPUT_NAME, SOME_INTERNAL_OUTPUT_NAME)
            .thatEndpointAdapterIsOutputAdapter();
    }

    @Test
    public void whenExposeInputsFlagIsGivenAllInputsAreExposed() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_OUTPUT_NAME, SOME_UUID_3);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeInputsFlag(),
                exposureParameter(SOME_UUID_1));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_INPUT_NAME, SOME_INTERNAL_INPUT_NAME)
            .thatEndpointAdapterIsInputAdapter()
            .noOtherEndpointAdaptersWereGiven();
    }

    @Test
    public void whenExposeOutputsFlagIsGivenAllOutputsAreExposed() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2)
            .thatNodeHasIntegerOutput(SOME_INTERNAL_OUTPUT_NAME, SOME_UUID_3);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeOutputsFlag(),
                exposureParameter(SOME_UUID_1));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(SOME_INTERNAL_OUTPUT_NAME, SOME_INTERNAL_OUTPUT_NAME)
            .thatEndpointAdapterIsOutputAdapter()
            .noOtherEndpointAdaptersWereGiven();
    }

    @Test
    public void componentsAndEndpointsWithSpacesAreHandledCorrectly() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_NODE_ID_WITH_SPACES, SOME_UUID_1)
            .thatNodeHasIntegerInput(SOME_INTERNAL_INPUT_NAME_WITH_SPACES, SOME_UUID_2);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_NODE_ID_WITH_SPACES, SOME_INTERNAL_INPUT_NAME_WITH_SPACES));

        pluginHarness.then()
            .noCommandExceptionWasThrown()
            .loaderServiceWasCalled()
            .integrationServiceWasCalled()
            .workflowIntegrationServiceWasCalledWithEndpointAdapter(
                SOME_INTERNAL_INPUT_NAME_WITH_SPACES, SOME_INTERNAL_INPUT_NAME_WITH_SPACES)
            .thatEndpointAdapterIsInputAdapter()
            .noOtherEndpointAdaptersWereGiven();
    }

    private String exposeInputsFlag() {
        return "--expose-inputs";
    }

    private String exposeOutputsFlag() {
        return "--expose-outputs";
    }

    @Test
    public void componentExposureOfAbsentComponentFailsGracefully() throws WorkflowFileException, IOException {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID));

        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(String.join(" ", wfIntegrateCommand(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID)))
            .exceptionHasMessage(
                parseErrorMessageHeader() + StringUtils.format("There are no endpoints present to expose on node '%s'", SOME_COMPONENT_ID));
    }
    
    @Test
    public void exposureOfEndpointsWithoutDefinitionFailsGracefully() throws Exception {
        pluginHarness.given()
            .workflowLoaderServiceIsPresent()
            .workflowIntegrationServiceIsPresent()
            .workflowIntegrationSucceeds(SOME_COMPONENT_ID)
            .workflowLoaderReturnsWorkflowDescription(SOME_WORKFLOW_IDENTIFIER)
            .loadedWorkflowDescriptionHasNode(SOME_COMPONENT_ID, SOME_UUID_1)
            .thatNodeHasEndpointWithoutDefinition(SOME_INTERNAL_INPUT_NAME, SOME_UUID_2);

        pluginHarness.when()
            .executeWfIntegrateCommand(SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_INPUT_NAME));

        pluginHarness.then()
            .syntaxErrorWasThrown()
            .exceptionHasCommandString(String.join(" ", wfIntegrateCommand(), SOME_COMPONENT_ID, SOME_WORKFLOWFILE_PATH, exposeFlag(),
                exposureParameter(SOME_COMPONENT_ID, SOME_INTERNAL_INPUT_NAME)))
            .exceptionHasMessage(
                parseErrorMessageHeader()
                + StringUtils.format(
                    "Could not determine input datum handling of endpoint '%s' [ID: %s]. "
                    + "This may be due to unavailability of the component. "
                    + "Please make sure that all components whose endpoints are adapted are available at the time of integration.",
                    SOME_INTERNAL_INPUT_NAME, SOME_UUID_2));
        
    }

    private String exposeInputFlag() {
        return "--expose-input";
    }

    private String exposeOutputFlag() {
        return "--expose-output";
    }

    private String exposureParameter(String componentName) {
        return componentName;
    }

    private String exposureParameter(String componentName, String endpoint) {
        return String.join(":", componentName, endpoint);
    }

    private String exposureParameter(String componentName, String internalEndpoint, String externalEndpoint) {
        return String.join(":", componentName, internalEndpoint, externalEndpoint);
    }

    private String exposeFlag() {
        return "--expose";
    }

    private String parseErrorMessageHeader() {
        return "Could not parse endpoint adapter definitions: \n";
    }

    private String nodeNotPresentErrorMessage(String nodeName) {
        return parseErrorMessageHeader()
            + "Given node '" + nodeName + "' is not present in workflow";
    }

    private String ambiguousNodeErrorMessage(String givenNodeId, String... candidateLines) {
        return parseErrorMessageHeader()
            + "Given node " + givenNodeId + " is ambiguous. Candidates:\n  "
            + String.join("\n  ", candidateLines);
    }

    private String exposureParameterErrorMessage(String exposureParameter) {
        return parseErrorMessageHeader()
            + "Could not parse endpoint adapter definition '" + exposureParameter + "'. "
            + "Expected format: Either '<component id>:<endpoint id>:<external name>' or '<component id>:<endpoint id>'";
    }

    private String absentEndpointErrorMessage(String expectedEndpointName, String expectedNodeName, String expectedNodeID,
        String expectedInputs, String expectedOutputs) {
        final StringBuilder returnValueBuilder = new StringBuilder(parseErrorMessageHeader());
        returnValueBuilder.append(StringUtils.format("Given endpoint '%s' is not present on node '%s' [ID: %s]. ",
            expectedEndpointName, expectedNodeName, expectedNodeID));

        if (expectedInputs.isEmpty() && expectedOutputs.isEmpty()) {
            returnValueBuilder.append("That node does not contain any endpoints.");
        } else {
            returnValueBuilder.append("The following endpoints are present on that node:");
            if (!expectedInputs.isEmpty()) {
                returnValueBuilder.append("\n");
                returnValueBuilder.append(expectedInputs);
            }
            
            if (!expectedOutputs.isEmpty()) {
                returnValueBuilder.append("\n");
                returnValueBuilder.append(expectedOutputs);
            }
        }
        return returnValueBuilder.toString();
    }

    private String ambiguousEndpointErrorMessage() {
        return parseErrorMessageHeader()
            + "Given endpoint 'internalInput' is ambiguous on node '" + SOME_COMPONENT_ID + "' [ID: " + SOME_UUID_1 + "]. Candidates:\n"
            + "  internalInput [ID: " + SOME_UUID_2 + "]\n"
            + "  internalInput [ID: " + SOME_UUID_3 + "]";

    }

    public String[] verboseOutput() {
        return new String[] {
            StringUtils.format("Input Adapter : %s --[Integer,Single,Required]-> %s @ %s", SOME_EXTERNAL_INPUT_NAME,
                SOME_INTERNAL_INPUT_NAME, SOME_UUID_1),
            StringUtils.format("Output Adapter: %s @ %s --[Integer]-> %s", SOME_INTERNAL_OUTPUT_NAME, SOME_UUID_1,
                SOME_EXTERNAL_OUTPUT_NAME)
        };
    }
}
