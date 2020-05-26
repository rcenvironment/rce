/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 */
public class ComponentStepDefinitions extends InstanceManagementStepDefinitionBase {

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
        public void iterateActionOverInstance(ManagedInstance instance) throws Throwable {
            for (String tool : parseCommaSeparatedList(toolList)) {
                printToCommandConsole(StringUtils.format("Removing tool %s from instance %s", tool, instance));
                File toolFile = instance.getAbsolutePathFromRelative(StringUtils.format("integration/tools/%s", tool));
                if (toolFile.exists()) {
                    FileUtils.cleanDirectory(toolFile);
                }
            }

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
     * @throws Throwable on failure
     */
    @Given("^adding(?: tool[s]?)? \"([^\"]+)\" to( all)?(?: instance[s]?)?(?: \"([^\"]+)\")?$")
    public void givenAddingTools(String tools, String allFlag, String instanceIds) throws Throwable {
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
     * @throws Throwable on failure
     */
    @When("^removing(?: tool[s]?)? \"([^\"]+)\"(?: from)?( all)?(?: instance[s]?)?(?: \"([^\"]+)\")?$")
    public void whenRemovingTools(String tools, String allFlag, String instanceIds) throws Throwable {
        RemoveToolIterator removeToolIterator = new RemoveToolIterator(tools);
        iterateInstances(removeToolIterator, allFlag, instanceIds);
    }

    /**
     * Batch test for presence or absence of component installations and their properties.
     * 
     * TODO this should actually go into a separate step definition class; there should be some refactoring first, though
     * 
     * @param instanceId the instance to query
     * @param componentsTable the expected component data to see (or not see in case of the reserved "absent" marker)
     * @throws Throwable on failure
     */
    @Then("^instance \"([^\"]*)\" should see these components:$")
    public void thenInstanceSeesComponents(String instanceId, DataTable componentsTable) throws Throwable {
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


}
