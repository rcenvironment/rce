/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.extras.testscriptrunner.definitions.common.AbstractStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;

/**
 * Steps for executing and testing single- and multi-instance workflows.
 *
 * @author Robert Mischke
 */
public class WorkflowStepDefinitions extends AbstractStepDefinitionBase {

    private static final String ESCAPED_DOUBLE_QUOTE = "\"";

    private static final String USE_REGEXP_MARKER = "the pattern "; // could be any non-null string

    private String lastWorkflowLogContent;

    private ManagedInstance lastWorkflowInitiatingInstance;

    public WorkflowStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Executes a workflow via IM SSH access on the given instance.
     * 
     * TODO decide: clean up workflow log files on test success?
     * 
     * @param workflowName the name of the workflow in the "workflows" sub-folder directory; the .wf suffix is optional
     * @param instanceId the instance to execute the command on
     * @throws Throwable on failure
     */
    @When("^executing (?:the )?workflow \"([^\"]*)\" on (?:instance )?\"([^\"]*)\"$")
    public void whenExecutingWorkflowOnInstance(String workflowName, String instanceId) throws Throwable {
        if (!workflowName.endsWith(".wf")) {
            workflowName = workflowName + ".wf";
        }
        Path originalWfFileLocation = executionContext.getTestScriptLocation().toPath().resolve("workflows").resolve(workflowName);
        if (!Files.isRegularFile(originalWfFileLocation)) {
            throw new AssertionError("No workflow file found at expected location " + originalWfFileLocation);
        }

        // copy .wf file to a clean temporary directory
        final File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("bdd-wf");
        Path wfFileLocation = tempDir.toPath().resolve(originalWfFileLocation.getFileName());
        Files.copy(originalWfFileLocation, wfFileLocation);

        final ManagedInstance instance = resolveInstance(instanceId);
        // TODO should workflows be actively deleted/disposed afterwards, or should this be left to general post-test profile cleanup?
        printToCommandConsole("Executing workflow " + wfFileLocation.toString() + " on instance " + instanceId);
        String commandOutput =
            executeCommandOnInstance(instance, "wf run --delete never \"" + wfFileLocation.toString() + ESCAPED_DOUBLE_QUOTE, false);
        log.debug("'wf run' command output:\n" + commandOutput);

        instance.setLastCommandOutput(commandOutput);
        lastInstanceWithSingleCommandExecution = instance;

        Pattern logDirPattern = Pattern.compile("Loading: '.+'; log directory: (.+) \\(full path:");
        final Matcher matcher = logDirPattern.matcher(commandOutput);
        if (!matcher.find()) {
            fail("Log output directory pattern not found in 'wf run' command output; full output:\n" + commandOutput);
        }
        Path logFilesDirectory = Paths.get(matcher.group(1));
        if (!Files.isDirectory(logFilesDirectory)) {
            fail("Found log output directory location '" + logFilesDirectory
                + "' in command output, but it does not point to an actual directory");
        }

        Path wfLogFile = logFilesDirectory.resolve("workflow.log");
        if (!Files.isRegularFile(wfLogFile)) {
            fail("Found log output directory location '" + logFilesDirectory + "', but there is no workflow.log file inside");
        }
        final String wfLogFileContent = FileUtils.readFileToString(wfLogFile.toFile());
        // log.debug("Workflow log file content:\n" + wfLogFileContent);

        this.lastWorkflowLogContent = wfLogFileContent;
        this.lastWorkflowInitiatingInstance = instance;

        // TODO delete temporary workflow file and logs directory
    }

    /**
     * Verifies the content of the last run workflow's log file.
     * 
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param substring the substring or pattern expected to be present or absent in the workflow log file
     * @throws Throwable on failure
     */
    @Then("^the workflow log should (not )?contain (the pattern )?\"(.*)\"$")
    public void thenTheWorkflowLogShouldContain(String negationFlag, String useRegexpMarker, String substring) throws Throwable {
        if (lastWorkflowLogContent == null) {
            fail("Test error: No workflow log present yet");
            return; // to satisfy flow check; never reached
        }
        if (lastWorkflowLogContent.isEmpty()) {
            fail("Test error: Workflow log was stored, but is empty");
        }
        assertPropertyOfTextOutput(lastWorkflowInitiatingInstance, negationFlag, useRegexpMarker, substring, lastWorkflowLogContent,
            "workflow log");
    }

    /**
     * Verifies that a certain workflow state was reached, typically FINISHED.
     * 
     * @param state the state to test for
     * @throws Throwable on failure
     */
    @Then("^the workflow should have reached the (\\w+) state$")
    public void theWorkflowShouldHaveReachedTheState(String state) throws Throwable {
        // delegate
        thenTheWorkflowLogShouldContain(null, null, "NEW_STATE:" + state);
    }

    /**
     * Verifies that a certain instance was used as the workflow controller, with an optional node id test.
     * 
     * @param nodeName the node name to test for
     * @param nodeId optionally, the node id to test for
     * @throws Throwable on failure
     */
    @Then("^the workflow controller should have been \"([^\"]+)\"(?: using node id \"(\\w+)\")?$")
    public void theWorkflowControllerShouldHaveBeen(String nodeName, String nodeId) throws Throwable {
        // delegate
        thenTheWorkflowLogShouldContain(null, null, "Location of workflow controller: \"" + nodeName + ESCAPED_DOUBLE_QUOTE);
    }

    /**
     * Verifies that a certain instance was used as the execution location for the given workflow component, with an optional node id test.
     * 
     * @param compName the workflow name of the component to test for
     * @param nodeName the node name to test for
     * @param uplinkFlag name of the uplink instance via which the component is made accessible
     * @param nodeId optionally, the node id to test for
     * @throws Throwable on failure
     */
    @Then("^workflow component \"([^\"]+)\" should have been run on \"([^\"]+)\"( via uplink)?(?: using node id \"(\\w+)\")?$")
    public void workflowComponentShouldHaveBeenRunOn(String compName, String nodeName, String uplinkFlag, String nodeId) throws Throwable {
        if (uplinkFlag != null) {
            ManagedInstance instance = resolveInstance(nodeName);
            nodeName += " \\(via " + instance.getUplinkUserName() + "\\/" + nodeName + "\\)";
        }
        String regexp = "Location of workflow component \"" + compName + "\" [^:]+: \"" + nodeName + ESCAPED_DOUBLE_QUOTE;
        if (nodeId != null) {
            regexp += " \\[" + nodeId + ":0\\]";
        }
        thenTheWorkflowLogShouldContain(null, USE_REGEXP_MARKER, regexp);
    }
}
