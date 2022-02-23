/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * Step definitions for adding, removing, altering, etc. commponents.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke (based on code from)
 * @author Alexander Weinert (component information steps)
 * @author Kathrin Schaffert (#17502, #17506)
 */
public class ComponentStepDefinitions extends InstanceManagementStepDefinitionBase {

    private String lastStaticInputQueried;

    private String lastStaticOutputQueried;

    private final List<Endpoint> staticInputsOfLastComponent = new LinkedList<>();

    private final List<Endpoint> dynamicInputsOfLastComponent = new LinkedList<>();

    private final List<Endpoint> staticOutputsOfLastComponent = new LinkedList<>();

    private final List<Endpoint> dynamicOutputsOfLastComponent = new LinkedList<>();

    public ComponentStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Class implementing InstanceIterator for adding tools.
     * 
     * @author Marlon Schroeter
     */
    private class AddToolIterator implements InstanceIterator {

        private String toolList;

        AddToolIterator(String toolList) {
            this.toolList = toolList;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws IOException {
            for (final String tool : parseCommaSeparatedList(toolList)) {
                printToCommandConsole(StringUtils.format("Adding tool %s to instance %s", tool, instance));
                File toolFile = instance.getAbsolutePathFromRelative(StringUtils.format("integration/tools/%s", tool));
                if (toolFile.exists()) {
                    FileUtils.cleanDirectory(toolFile);
                } else {
                    toolFile.mkdir();
                }
                File testToolTemplateDir = Paths.get(executionContext.getTestScriptLocation().toString(), "tools", tool).toFile();
                FileUtils.copyDirectory(testToolTemplateDir, toolFile);
            }
        }
    }

    /**
     * Represents the expected vs. the actual visibility and authorization state of a local or remote component.
     *
     * @author Robert Mischke
     */
    private class ComponentVisibilityState {

        private String componentName;

        private String nodeName;

        private String expectedState;

        private String actualState;

        ComponentVisibilityState(String componentName, String nodeName, String expectedState, String actualState) {
            this.componentName = componentName;
            this.nodeName = nodeName;
            setExpectedState(expectedState);
            setActualState(actualState);
        }

        public void setExpectedState(String expectedState) {
            this.expectedState = Optional.ofNullable(expectedState).orElse(StepDefinitionConstants.ABSENT_COMPONENT_STRING);
        }

        public void setActualState(String actualState) {
            this.actualState = Optional.ofNullable(actualState).orElse(StepDefinitionConstants.ABSENT_COMPONENT_STRING);
        }

        public boolean stateMatches() {
            return expectedState.equals(actualState);
        }

        @Override
        public String toString() {
            return StringUtils.format("%s | %s | expected: %s | found: %s", nodeName, componentName, expectedState, actualState);
        }

    }

    /**
     * Class implementing InstanceIterator for removing tools.
     * 
     * @author Marlon Schroeter
     */
    private class RemoveToolIterator implements InstanceIterator {

        private String toolList;

        RemoveToolIterator(String toolList) {
            this.toolList = toolList;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws Exception {
            for (String tool : parseCommaSeparatedList(toolList)) {
                printToCommandConsole(StringUtils.format("Removing tool %s from instance %s", tool, instance));
                File toolFile = instance.getAbsolutePathFromRelative(StringUtils.format("integration/tools/%s", tool));
                if (toolFile.exists()) {
                    FileUtils.cleanDirectory(toolFile);
                }
            }

        }

    }

    private static class Endpoint {

        private String name;

        private String defaultDatatype;

        private String possibleDatatypes;

        private String defaultInputHandling;

        private String possibleInputHandlings;

        private String defaultExecutionConstraint;

        private String possibleExecutionConstraints;

        /**
         * Factory method for @{link {@link Endpoint}. Should be a static method of that class, which is not possible due to that class
         * being an inner class, which may not
         * 
         * @param outputLine A line obtained from the output of `components show` describing a single endpoint
         * @return
         */
        private static Endpoint parseComponentsShowOutputLine(String outputLine) {
            final String[] outputComponents = outputLine.split("|");

            // An endpoint is either an input or an output. An output has only a name, a default datatype, and a list of admissible
            // datatypes, while an input additionally has a default and admissible input handlings and input execution constraints.
            // Hence, the information about an endpoint consists of either three (for an output) or seven (for an input) items.
            assertTrue(outputComponents.length == 3 || outputComponents.length == 7);

            final Endpoint product = new Endpoint();

            product.name = outputComponents[0];
            product.defaultDatatype = outputComponents[1];
            product.possibleDatatypes = outputComponents[2];

            if (outputComponents.length == 7) {
                product.defaultInputHandling = outputComponents[3];
                product.possibleInputHandlings = outputComponents[4];

                product.defaultExecutionConstraint = outputComponents[5];
                product.possibleExecutionConstraints = outputComponents[6];
            }

            return product;
        }
    }

    /**
     * Adds one or more tools to one or more instances.
     * 
     * @param tools comma-separated list of tools to be added
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     */
    @Given("^adding(?: tool[s]?)? \"([^\"]+)\" to( all)?(?: instance[s]?)?(?: \"([^\"]+)\")?$")
    public void givenAddingTools(String tools, String allFlag, String instanceIds) throws Exception {
        AddToolIterator addToolIterator = new AddToolIterator(tools);
        iterateInstances(addToolIterator, allFlag, instanceIds);
    }

    /**
     * Adds one or more tools to one or more instances.
     * 
     * @param tools comma-separated list of tools to be removed
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     */
    @When("^removing(?: tool[s]?)? \"([^\"]+)\"(?: from)?( all)?(?: instance[s]?)?(?: \"([^\"]+)\")?$")
    public void whenRemovingTools(String tools, String allFlag, String instanceIds) throws Exception {
        RemoveToolIterator removeToolIterator = new RemoveToolIterator(tools);
        iterateInstances(removeToolIterator, allFlag, instanceIds);
    }

    @When("^integrating workflow \"([^\"]*)\" as component \"([^\"]*)\" on instance \"([^\"]*)\" with the following endpoint definitions:$")
    public void whenIntegratingWorkflow(String workflowName, String componentName, String instanceId, DataTable endpointDefinitionTable) {
        final List<List<String>> endpointDefinitions = endpointDefinitionTable.cells(0);

        final String endpointsDefinitionsString = endpointDefinitions.stream()
            .map(row -> ("--expose " + row.get(0)))
            .collect(Collectors.joining(" "));

        final Path originalWfFileLocation = executionContext.getTestScriptLocation().toPath().resolve("workflows").resolve(workflowName);
        final String command =
            StringUtils.format("wf integrate %s \"%s\" %s", componentName, originalWfFileLocation, endpointsDefinitionsString);
        executeCommandOnInstance(resolveInstance(instanceId), command, false);
    }

    /**
     * Batch test for presence or absence of component installations and their properties.
     * 
     * TODO this should actually go into a separate step definition class; there should be some refactoring first, though
     * 
     * @param instanceId the instance to query
     * @param componentsTable the expected component data to see (or not see in case of the reserved "absent" marker)
     */
    @Then("^instance \"([^\"]*)\" should see these components:$")
    public void thenInstanceSeesComponents(String instanceId, DataTable componentsTable) {
        final ManagedInstance instance = resolveInstance(instanceId);

        Map<String, ComponentVisibilityState> visibilityMap = new HashMap<>();

        // parse expectations
        for (List<String> criteriaRow : componentsTable.cells(0)) {
            String argNodeName = criteriaRow.get(0);
            String argCompName = criteriaRow.get(1);
            String expectedState = criteriaRow.get(2);

            final String mapKey = argNodeName + "/" + argCompName;

            ComponentVisibilityState entry = new ComponentVisibilityState(argCompName, argNodeName, expectedState, null);
            visibilityMap.put(mapKey, entry);

            log.debug("Parsed component expectation: " + entry);
        }

        // parse actual state
        String output = executeCommandOnInstance(instance, "components list --as-table --auth", false);
        // log.debug(output);
        for (String line : output.split("\n")) {
            if (line.startsWith("Finished executing command")) {
                // synthetic final line; not part of the actual output
                continue;
            }
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            String[] lineParts = trimmedLine.split("\\|");
            if (lineParts.length != 6) {
                log.error("Ignoring output line with unexpected number of elements: " + line);
                continue;
            }
            final String componentRefName = lineParts[2];
            final String nodeName = lineParts[0];
            final String actualState = lineParts[5];

            final String mapKey = nodeName + "/" + componentRefName;
            ComponentVisibilityState existing = visibilityMap.get(mapKey);
            if (existing != null) {
                existing.setActualState(actualState);
            }
        }

        boolean hasMismatch = false;
        StringBuilder errorLines = new StringBuilder();
        for (ComponentVisibilityState entry : visibilityMap.values()) {
            if (!entry.stateMatches()) {
                final String errorLine = "  Unexpected component state: " + entry;
                errorLines.append("\n");
                errorLines.append(errorLine);
                printToCommandConsole(errorLine);
                hasMismatch = true;
            }
        }

        if (hasMismatch) {
            fail("At least one component had an unexpected visibility/authorization state: " + errorLines.toString());
        }
    }

    @Then("^instance \"([^\"]*)\" should see the component \"([^\"]*)\"$")
    public void instanceShouldSeeTheComponent(String instanceId, String componentId) {
        final ManagedInstance instance = resolveInstance(instanceId);
        final String command = StringUtils.format("components show %s", componentId);
        final String output = executeCommandOnInstance(instance, command, false);

        final String[] outputLines = output.split("\n");

        final String expectedExternalIdLine = StringUtils.format("External ID: %s", componentId);
        assertEquals(expectedExternalIdLine, outputLines[0]);

        // The second line of the output (i.e., outputLines[1]) contains the internal ID of the component. We skip validation of that line
        // as it is irrelevant for user-observed behavior

        assertTrue(outputLines[2].equals("Static Inputs:"));
        final List<String> staticInputLines = new LinkedList<>();
        int currentIndex = 3;
        String currentLine = outputLines[currentIndex];
        while (!currentLine.equals("Dynamic Inputs:")) {
            staticInputLines.add(currentLine);
            currentIndex += 1;
            currentLine = outputLines[currentIndex];
        }
        currentIndex += 1;
        currentLine = outputLines[currentIndex];

        final List<String> dynamicInputLines = new LinkedList<>();
        while (!currentLine.equals("Static Outputs:")) {
            dynamicInputLines.add(currentLine);
            currentIndex += 1;
            currentLine = outputLines[currentIndex];
        }
        currentIndex += 1;
        currentLine = outputLines[currentIndex];

        final List<String> staticOutputLines = new LinkedList<>();
        while (!currentLine.equals("Dynamic Outputs:")) {
            staticOutputLines.add(currentLine);
            currentIndex += 1;
            currentLine = outputLines[currentIndex];
        }
        currentIndex += 1;

        final List<String> dynamicOutputLines = new LinkedList<>();
        while (currentIndex < outputLines.length) {
            currentLine = outputLines[currentIndex];
            dynamicOutputLines.add(currentLine);
            currentIndex += 1;
        }

    }

    @Then("^that component should have a static input with name \"([^\"]*)\"$")
    public void thatComponentShouldHaveInputWithName(String inputName) {
        this.lastStaticInputQueried = inputName;
        assertTrue(this.staticInputsOfLastComponent.stream().anyMatch(endpoint -> endpoint.name.equals(inputName)));
    }

    @Then("^that input should have the default data type \"([^\"]*)\"$")
    public void thatInputShouldBeOfType(String expectedDefaultDataType) {
        final Endpoint lastInputQueried = getLastStaticInputQueried();
        assertEquals(expectedDefaultDataType, lastInputQueried.defaultDatatype);
    }

    @Then("^that input should have the input handling \"([^\"]*)\"$")
    public void thatInputShouldHaveTheInputHandling(String expectedInputHandling)  {
        final Endpoint lastInputQueried = getLastStaticInputQueried();
        assertEquals(expectedInputHandling, lastInputQueried.defaultInputHandling);
    }

    @Then("^that input should have the execution constraint \"([^\"]*)\"$")
    public void thatInputShouldHaveTheExecutionConstraint(String expectedExecutionConstraint) {
        final Endpoint lastInputQueried = getLastStaticInputQueried();
        assertEquals(expectedExecutionConstraint, lastInputQueried.defaultExecutionConstraint);
    }

    @Then("^that component should have a static output with name \"([^\"]*)\"$")
    public void thatComponentShouldHaveAnOutputWithName(String outputName) {
        this.lastStaticOutputQueried = outputName;
        assertTrue(this.staticOutputsOfLastComponent.stream().anyMatch(endpoint -> endpoint.name.equals(outputName)));
    }

    @Then("^that output should have the default data type \"([^\"]*)\"$")
    public void thatOutputShouldBeOfType(String expectedDefaultDataType) {
        final Endpoint lastOutputQueried = getLastStaticOutputQueried();
        assertEquals(expectedDefaultDataType, lastOutputQueried.defaultDatatype);
    }

    private Endpoint getLastStaticInputQueried() {
        return this.staticInputsOfLastComponent.stream()
            .filter(endpoint -> endpoint.name.equals(this.lastStaticInputQueried))
            .findAny().get();
    }

    private Endpoint getLastStaticOutputQueried() {
        return this.staticOutputsOfLastComponent.stream()
            .filter(endpoint -> endpoint.name.equals(this.lastStaticOutputQueried))
            .findAny().get();
    }

}
