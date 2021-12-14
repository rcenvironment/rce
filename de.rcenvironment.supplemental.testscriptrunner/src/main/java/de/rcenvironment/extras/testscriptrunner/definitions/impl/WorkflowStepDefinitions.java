/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.jcraft.jsch.JSchException;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;
import de.rcenvironment.extras.testscriptrunner.internal.GildedManagedInstance;
import de.rcenvironment.extras.testscriptrunner.internal.GoldenMaster;
import de.rcenvironment.extras.testscriptrunner.internal.GoldenMasters;
import junit.framework.AssertionFailedError;

/**
 * Steps for executing and testing single- and multi-instance workflows.
 *
 * @author Robert Mischke
 * @author Marlon Schroeter
 * @author Kathrin Schaffert (bug fixes)
 */
public class WorkflowStepDefinitions extends InstanceManagementStepDefinitionBase {

    private static final String WORKFLOWS = "workflows";

    private static final String JSON = ".json";

    private static final String QUOTATION_MARK = "\"";

    private static final String BACKSLASH = "\\";

    private static final String ESCAPED_BACKSLASH = "\\\\";

    private static final String WORKFLOW_EXTENSION = ".wf";

    private static final String LIST_WORKFLOWS_COMMAND = "wf";

    private static final String EXPORTED_WORKFLOW_RUNS_SUB_DIR = "workspace\\exported_wfs";

    private String lastWorkflowName;

    private Path lastWorkflowLogDir;

    private ManagedInstance lastWorkflowInitiatingInstance;

    // only contains workflows executed via dedicated teststep
    private Map<String, String> runningWorkflows = new ConcurrentHashMap<>();

    private final GoldenMasters goldenMasters;

    public WorkflowStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
        this.goldenMasters = new GoldenMasters(executionContext.getTestScriptLocation().toPath().resolve("golden_masters"));
    }

    /**
     * Class implementing InstanceAction of asserting instance sees all given workflows.
     * 
     * @author Marlon Schroeter
     */
    private class AssertWorkflowVisibilityAction implements InstanceAction {

        private List<String> workflowNames;

        AssertWorkflowVisibilityAction(List<String> workflowNames) {
            this.workflowNames = workflowNames;
        }

        @Override
        public void performActionOnInstance(ManagedInstance instance, long timeout) throws IOException {
            String commandOutput = executeCommandOnInstance(instance, LIST_WORKFLOWS_COMMAND, false);
            instance.setLastCommandOutput(commandOutput);
            executionContext.setLastInstanceWithSingleCommandExecution(instance);

            for (String workflowName : workflowNames) {
                if (!commandOutput.contains(workflowName)) {
                    fail(StringUtils.format("Instance %s could not see %s. It saw these workflows: \n %s",
                        instance, workflowName, commandOutput));
                }
            }
        }

    }

    /**
     * Class implementing InstanceAction of asserting instances have identical information on workflow data.
     * 
     * @author Marlon Schroeter
     */
    private class AssertWorkflowDataAction implements InstanceAction {

        protected final ManagedInstance comparator;

        protected File tempDir;

        protected File tmpDirBase;

        private final List<String> workflowNames;

        AssertWorkflowDataAction(ManagedInstance comparator, List<String> workflowNames) {
            this.comparator = comparator;
            this.workflowNames = workflowNames;
            try {
                this.tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("wf_run_export");
                this.tmpDirBase = new File(tempDir, "base");
            } catch (IOException e) {
                fail("Could not create temporary directory");
                this.tempDir = null;
                this.tmpDirBase = null;
            }
        }

        @Override
        public void performActionOnInstance(ManagedInstance instance, long timeout) throws IOException {
            String commandOutput = executeCommandOnInstance(instance, LIST_WORKFLOWS_COMMAND, false);
            instance.setLastCommandOutput(commandOutput);
            executionContext.setLastInstanceWithSingleCommandExecution(instance);

            File tmpDirInst = new File(tempDir, instance.getId());

            boolean createGoldenMasters = !tmpDirBase.exists();

            for (String workflowName : workflowNames) {
                for (String workflowRun : findWorkflowRunsByPattern(instance, workflowName)) {
                    final String masterPath = StringUtils.format("%s\\%s.json", tmpDirBase, workflowRun.replace(":", "_"));

                    if (createGoldenMasters) {
                        createGoldenMaster(workflowRun);
                    }

                    final String exportedRunPath = exportWorkflowRun(instance, tmpDirInst, workflowRun);

                    boolean runsAreEqual = compareWorkflowRuns(masterPath, exportedRunPath);
                    if (!runsAreEqual) {
                        fail(StringUtils.format("The workflow run %s is not identical on instance %s and instance %s", workflowRun,
                            comparator, instance));
                    }
                }
            }
        }

        protected Iterable<String> findWorkflowRunsByPattern(final ManagedInstance instance, String workflowName) {
            String commandOutput = executeCommandOnInstance(instance, LIST_WORKFLOWS_COMMAND, false);
            instance.setLastCommandOutput(commandOutput);
            executionContext.setLastInstanceWithSingleCommandExecution(instance);

            Pattern p = Pattern.compile("'(" + workflowName + "[\\d-_:]+)'");
            Matcher m = p.matcher(commandOutput);
            final List<String> returnValue = new LinkedList<>();
            while (m.find()) {
                returnValue.add(m.group(1));
            }
            return returnValue;
        }

        protected void createGoldenMaster(String workflowRun) {
            String outputExportBase = executeCommandOnInstance(comparator,
                StringUtils.format("tc export_wf_run %s %s", tmpDirBase, workflowRun), false);
            printToCommandConsole(outputExportBase);
            if (!outputExportBase.contains(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_EXPORT)) {
                fail(StringUtils.format("The workflow run %s could not be exported from instance %s", workflowRun, comparator));
            }
        }

        protected String exportWorkflowRun(ManagedInstance instance, File tmpDirInst, String workflowRun) {
            String outputExportInst =
                executeCommandOnInstance(instance, StringUtils.format("tc export_wf_run %s %s", tmpDirInst, workflowRun), false);
            printToCommandConsole(outputExportInst);
            if (!outputExportInst.contains(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_EXPORT)) {
                fail(StringUtils.format("The workflow run %s could not be exported from instance %s", workflowRun, instance));
            }
            return StringUtils.format("%s\\%s.json", tmpDirInst, workflowRun.replace(":", "_"));
        }

        protected boolean compareWorkflowRuns(final String masterPath, final String exportedRunPath) {
            String outputComparison = executeCommandOnInstance(comparator, StringUtils.format("tc compare_wf_runs %s %s",
                masterPath, exportedRunPath), false);
            printToCommandConsole(outputComparison);
            return outputComparison.contains(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_COMPARISON_IDENTICAL);
        }

    }

    /**
     * Executes a workflow via IM SSH access on the given instance.
     * 
     * @param workflowInputSequence comma separated list of names of the workflows in the "workflows" sub-folder directory; the .wf suffix
     *        is optional; optional corresponding placeholder file is to be given in brackets - e.g. "wf [placeholder]"
     * @param instanceId the instance to execute the command on
     * @param timeoutString custom timeout in seconds, after which waiting for execution of workflow is aborted
     */
    @When("^executing (?:the )?workflow[s]? \"([^\"]*)\" on (?:instance )?\"([^\"]*)\"$(?: in (\\d+) seconds)?")
    public void whenExecutingWorkflowOnInstance(final String workflowInputSequence, final String instanceId, final String timeoutString)
        throws Exception {
        for (final String workflowInput : parseCommaSeparatedList(workflowInputSequence)) {

            final String workflowName;
            final String placeholderFile;
            if (containsPlaceholderFile(workflowInput)) {
                final String[] workflowInputSplit = splitWorkflowInput(workflowInput);
                workflowName = workflowInputSplit[0];
                final String placeholderTemplate = workflowInputSplit[1];
                placeholderFile = injectValuesIntoPlaceholderFile(placeholderTemplate);

            } else {
                workflowName = workflowInput.trim();
                placeholderFile = null;
            }

            File wfFile = Paths.get(new File(workflowName).getPath()).toFile();

            whenStartingWorkflowOnInstance(wfFile.getPath(), placeholderFile, instanceId);
            whenWaitingUntilWorkflowReachedState(workflowInput.trim(), instanceId, "finished", timeoutString);
        }
    }

    private String injectValuesIntoPlaceholderFile(String placeholderTemplate) throws IOException {

        Path testLocation = executionContext.getTestScriptLocation().toPath();
        Path subdir = testLocation.resolve(Paths.get(WORKFLOWS, "placeholder_values"));
        Path originalPlaceholderFileLocation = subdir.resolve(placeholderTemplate);

        List<String> template = Files.readAllLines(originalPlaceholderFileLocation);
        File placeholderFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("*" + JSON);

        try (FileWriter fileWriter = new FileWriter(placeholderFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            for (String line : template) {
                final String replacedLine;
                if (line.contains("$$TEMP_DIR$$")) {
                    File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
                    replacedLine = line.replace("$$TEMP_DIR$$",
                        QUOTATION_MARK + tempDir.getAbsolutePath().replace(BACKSLASH, ESCAPED_BACKSLASH) + QUOTATION_MARK);
                    // replace backslash with escaped backslash, so that the path could be resolved properly on Windows OS
                    // since on Linux the path string does not contain any \, the replace method will do nothing
                    // K. Schaffert, 23.04.2020
                } else if (line.contains("$$TEMP_FILE$$")) {
                    File tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("*");
                    replacedLine = line.replace("$$TEMP_FILE$$",
                        QUOTATION_MARK + tempFile.getAbsolutePath().replace(BACKSLASH, ESCAPED_BACKSLASH) + QUOTATION_MARK);
                } else {
                    replacedLine = line;
                }
                bufferedWriter.write(replacedLine);
                bufferedWriter.newLine();
            }
        }
        return placeholderFile.getAbsolutePath();
    }

    private String[] splitWorkflowInput(final String workflowInput) {
        final String[] workflowInputParts = workflowInput.trim().split("\\[");
        final String workflowName = workflowInputParts[0].trim();
        final String placeholderFileUnadjusted = workflowInputParts[1].trim();
        final String placeholderFile = placeholderFileUnadjusted.substring(0, placeholderFileUnadjusted.length() - 1);
        return new String[] { workflowName, placeholderFile };
    }

    private boolean containsPlaceholderFile(final String workflowInput) {
        return workflowInput.contains("[");
    }

    /**
     * Copies a file or directory located inside the scripts directory.
     * 
     * @param fileOrDir file or directory in the scripts directory, which is to be copied
     * @param instanceId instance who is the target of the copying
     */
    @When("^copying \"([^\"]*)\" into (?:the )?workspace of \"([^\"]*)\"")
    public void whenCopyingIntoWorkspace(String fileOrDir, String instanceId) throws Exception {
        ManagedInstance instance = resolveInstance(instanceId);
        printToCommandConsole(StringUtils.format("Copying %s to instance %s", fileOrDir, instance));
        TestContext.setWorkflowProjectDirectory(fileOrDir);
        File workspace = instance.getAbsolutePathFromRelative("workspace");
        if (!workspace.exists()) {
            workspace.mkdir();
        }

        File origin = Paths.get(executionContext.getTestScriptLocation().toString(), new File(fileOrDir).getPath()).toFile();
        File dest = new File(workspace, origin.getName());
        if (origin.isDirectory()) {
            FileUtils.copyDirectory(origin, dest);
        } else if (origin.isFile()) {
            FileUtils.copyFile(origin, dest);
        } else {
            fail(StringUtils.format("%s is neither directory nor file", fileOrDir));
        }
    }

    /**
     * Copies configuration files to the installation site of the software under test in preparation of running a workflow.
     * Note: It uses (via "TestContext") the given build of test step "using the ... build".
     * Note: It uses (via "TestContext") the origin project directory  of test step "copying ... into workspace( of ...").
     * Both of the above are set during their steps; so without these steps this step is likely to fail (there are no default values or so in TestContext)!
     * This step is required as a special copy action for input files, for now (2021-10-26) especially of workflow 0203.
     * 
     * @param fileNamesToCopy list of (configuration) files which have to be copied
     * @param workflowGroupDir target directory of the workflow for the copying (e. g. "02_Component Groups")
     *///(?: workflow[s]?)? \"([^\"]+)\"
    @When("^copying configuration file[s]? \"([^\"]+)\" of workflow group \"([^\"]*)\" into installation workspace")
    public void whenPreparingInstallationWorkspace(String fileNamesToCopy, String workflowGroupDir) throws Exception {
        String originDir = TestContext.getWorkflowProjectDirectory() + File.separator + workflowGroupDir;
        //String[] filesToCopy = {"CPACS.xml", "MappingRules.xsl", "XMLMerger_Integrate.xml"};
        String projectName = "Workflow Examples Project";
        String sutDir = TestContext.getTestedInstanceInstallationRoot() + File.separator + "workspace" + File.separator + projectName + File.separator + workflowGroupDir;
        printToCommandConsole("   +++   SUT DIR: " + sutDir);
        
        String[] filesToCopy = fileNamesToCopy.split(",[ ]");
        for (String fileToCopy : filesToCopy) {
            File originFile = Paths.get(executionContext.getTestScriptLocation().toString(), new File(originDir + File.separator + fileToCopy).getPath()).toFile();
            File destFile = new File(sutDir + File.separator + fileToCopy);
            // Delete previous versions on the destination location
            if (destFile.exists()) {
                FileUtils.forceDelete(destFile);
            }
            printToCommandConsole("   +++   copy files: " + originFile + destFile);
            FileUtils.copyFile(originFile, destFile);
        }
    }

    /**
     * Starts a workflow via IM SSH access on the given instance.
     * 
     * TODO decide: clean up workflow log files on test success? TODO synchronize alteration of runningWorkflowMap
     * 
     * @param workflowNameInput the name of the workflow in the "workflows" sub-folder directory; the .wf suffix is optional
     * @param placeholderFile the name of the placeholder file in the "placeholder_values" sub-folder directory; the .json suffix is
     *        optional
     * @param instanceId the instance to execute the command on
     */
    @When("^starting (?:the )?workflow \"([^\"]*)\" (?:using \"([^\"]*)\" as placeholder file)?on (?:instance )?\"([^\"]*)\"$")
    public void whenStartingWorkflowOnInstance(String workflowNameInput, String placeholderFile, String instanceId) throws Exception {
        final ManagedInstance instance = resolveInstance(instanceId);
        String workflowPath = addExtension(workflowNameInput, WORKFLOW_EXTENSION);
        if (placeholderFile != null && !placeholderFile.endsWith(JSON)) {
            placeholderFile = addExtension(placeholderFile, JSON);
        }
        String[] workflowInfo = startWorkflowOnInstance(instance, workflowPath, placeholderFile);
        final String workflowName = convertWorkflowPathToName(workflowPath);
        if (!workflowInfo[0].equals(workflowName)) {
            fail("obtained workflowInfo for wrong workflow execution.");
        }
        String workflowLogDir = workflowInfo[1];
        String workflowId = workflowInfo[2];

        String workflowKey = getWorkflowKey(workflowName, instanceId);
        if (runningWorkflows.containsKey(workflowKey)) {
            log.warn("Another workflow of the same file is still running. Cannot store the new workflow id.");
        } else {
            runningWorkflows.put(workflowKey, workflowId);
            startJobCheckingTermiantion(instanceId, instance, workflowName, workflowId, workflowKey);
        }

        Path logFilesDirectory = Paths.get(workflowLogDir);
        if (!Files.isDirectory(logFilesDirectory)) {
            fail(StepDefinitionConstants.FOUND_LOG_OUTPUT_DIRECTORY_LOCATION + logFilesDirectory
                + "' in command output, but it does not point to an actual directory");
        }
        this.lastWorkflowLogDir = logFilesDirectory;
        this.lastWorkflowName = workflowName;
        this.lastWorkflowInitiatingInstance = instance;
    }

    private void startJobCheckingTermiantion(String instanceId, final ManagedInstance instance, final String workflowName,
        String workflowId, String workflowKey) {
        new Job(StringUtils.format("check for termination workflow %s", workflowId)) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {

                Pattern workflowFinishedPattern = Pattern.compile("(FINISHED|FAILED|CANCELLED) \\[" + workflowId + "\\]");
                while (true) {
                    try {
                        if (INSTANCE_MANAGEMENT_SERVICE.isInstanceRunning(instanceId)) {
                            String commandOutput = null;
                            try {
                                commandOutput = executeCommandOnInstance(instance, LIST_WORKFLOWS_COMMAND, false);
                            } catch (AssertionFailedError e) {
                                // In this case the remote instance is already shut down and no workflows are running.
                                runningWorkflows.remove(workflowKey);
                                return Status.OK_STATUS;
                            }
                            Matcher m = workflowFinishedPattern.matcher(commandOutput);
                            if (m.find()) {
                                runningWorkflows.remove(workflowKey);
                                break;
                            }

                            try {
                                Thread.sleep(StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS);
                            } catch (InterruptedException e) {
                                log.error("Exception while trying to sleep tread", e);
                                return Status.CANCEL_STATUS;
                            }
                        } else {
                            printToCommandConsole(
                                StringUtils.format("instance %s not running anymore. Stopping to check for state of workflow %s.",
                                    instanceId, workflowName));
                            break;
                        }
                    } catch (IOException e) {
                        log.error("Exception while checking running state of instance", e);
                        return Status.CANCEL_STATUS;
                    }

                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    /**
     * Executes operation regarding running workflow on instance. Workflow has to have been started via teststep.
     * 
     * @param operation one of the following operations is possible: cancelling, deleting, opening, pausing, resuming
     * @param workflowNameInput name of the workflow file
     * @param instanceId instance on which the workflow is running
     */
    @When("^(cancelling|deleting|opening|pausing|resuming) workflow \"([^\"]*)\" on (?:instance )?\"([^\"]*)\"$")
    public void operationOnRunningWorkflow(String operation, String workflowNameInput, String instanceId) {
        final ManagedInstance instance = resolveInstance(instanceId);
        final String workflowName = convertWorkflowPathToName(addExtension(workflowNameInput, WORKFLOW_EXTENSION));
        String workflowKey = getWorkflowKey(workflowName, instanceId);
        if (!runningWorkflows.containsKey(workflowKey)) {
            fail("Could not execute operation on workflow, since workflow id could not be obtained.");
        } else {
            String workflowId = runningWorkflows.get(workflowKey);

            String command;
            switch (operation) {
            case ("cancelling"):
                command = StringUtils.format("wf cancel %s", workflowId);
                break;
            case ("deleting"):
                command = StringUtils.format("wf delete %s", workflowId);
                break;
            case ("opening"):
                command = StringUtils.format("wf open %s", workflowId);
                break;
            case ("pausing"):
                command = StringUtils.format("wf pause %s", workflowId);
                break;
            case ("resuming"):
                command = StringUtils.format("wf resume %s", workflowId);
                break;
            default:
                fail(StringUtils.format("%s a unsupported operation.", operation));
                return;
            }

            String output = executeCommandOnInstance(instance, command, false);
            printToCommandConsole(output);
        }
    }

    /**
     * @param workflowNameInput name of the workflow that is to be waited for
     * @param instanceId Instance on which the workflow is running
     * @param state state for which is waited
     * @param timeoutString custom timeout in seconds, after which waiting is aborted
     */
    @When("^waiting until workflow \"([^\"]*)\" on (?:instance )?\"([^\"]*)\" (?:has|is) (finished|cancelled|canceling|failed)"
        + "(?: or (\\d+) seconds have passed)?")
    public void whenWaitingUntilWorkflowReachedState(String workflowNameInput, String instanceId, String state, String timeoutString) {
        final ManagedInstance instance = resolveInstance(instanceId);
        final String workflowName = convertWorkflowPathToName(addExtension(workflowNameInput, WORKFLOW_EXTENSION));
        final int timeoutInSecs = parseTimeout(timeoutString);

        printToCommandConsole(StringUtils.format("Waiting for workflow %s to be %s. Aborting after %s seconds if not happend by then.",
            workflowName, state, timeoutInSecs));

        String workflowKey = getWorkflowKey(workflowName, instanceId);
        if (!runningWorkflows.containsKey(workflowKey)) {
            printToCommandConsole(StringUtils.format("Workflow %s is not running on instance %s", workflowName, instanceId));
            return;
        }
        String workflowId = runningWorkflows.get(workflowKey);

        long countMillis = TimeUnit.SECONDS.toMillis(timeoutInSecs);
        Pattern workflowFinishedPattern = Pattern.compile(state.toUpperCase() + " \\[" + workflowId + "\\]");
        while (countMillis > 0) {
            String commandOutput = executeCommandOnInstance(instance, "wf list", false);
            Matcher m = workflowFinishedPattern.matcher(commandOutput);

            try {
                if (m.find()) {
                    printToCommandConsole(StringUtils.format("Workflow %s has reached state %s on %s.", workflowName, state, instanceId));
                    return;
                }
                Thread.sleep(StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS);
                countMillis -= StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS;
            } catch (InterruptedException e) {
                fail(StringUtils.format("InterruptedException caused when waiting for workflow to reach state. Exception: \n%s", e));
                return;
            }
        }
        fail(StringUtils.format("%s second(s) have passed and workflow %s has not reached state %s. Timeout was reached.", timeoutInSecs,
            workflowName, state));
    }

    @Then("^that workflow run should be identical to \"([^\"]*)\"$")
    public void thenThatWorkflowRunShouldBeIdenticalTo(String goldenMasterId) throws Throwable {
        final Optional<GoldenMaster> master = this.goldenMasters.get(goldenMasterId);

        if (!master.isPresent()) {
            fail(StringUtils.format("Golden master '%s' not present", goldenMasterId));
            return; // Superfluous return to terminate the control flow for analysis by SonarCube
        }

        final File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
        final GildedManagedInstance instance = new GildedManagedInstance(
            lastWorkflowInitiatingInstance,
            this::printToCommandConsole,
            (instanceParam, command) -> executeCommandOnInstance(instanceParam, command, false));

        final File tmpDirInst = new File(tempDir, lastWorkflowInitiatingInstance.getId());

        for (String workflowRunName : instance.findWorkflowRunsByPattern(master.get().getWorkflowName())) {

            final String exportedRunPath = instance.exportWorkflowRun(tmpDirInst, workflowRunName);

            boolean runsAreEqual = instance.compareWorkflowRuns(master.get().getAbsolutePath(), exportedRunPath);
            if (!runsAreEqual) {
                fail(StringUtils.format("The workflow run %s is not identical to the golden master on instance %s", workflowRunName,
                    lastWorkflowInitiatingInstance));
            }
        }
    }

    /**
     * @param instanceId The id of the instance whose workflows have to be finished
     * @param timeoutString custom timeout in seconds, after which waiting is aborted
     */
    @When("^waiting until all workflows on \"([^\"]*)\" are not running anymore(?: or (\\d+) seconds have passed)?")
    public void whenWaitingUntilAllWorkflowsFinished(String instanceId, String timeoutString) throws Exception {

        final int timeoutInSecs = parseTimeout(timeoutString);
        long countMillis = TimeUnit.SECONDS.toMillis(timeoutInSecs);
        while (countMillis > 0) {
            ManagedInstance instance = resolveInstance(instanceId);
            String commandOutput = executeCommandOnInstance(instance, LIST_WORKFLOWS_COMMAND, false);
            Pattern logDirPattern = Pattern.compile("-- TOTAL COUNT: \\d+ workflow\\(s\\): (\\d+) running");
            final Matcher matcher = logDirPattern.matcher(commandOutput);
            if (!matcher.find()) {
                fail(StringUtils.format("Could not identify the number of running workflows by trying to match \"%s\"; full output:\n %s",
                    logDirPattern, commandOutput));
            } else {
                if (Integer.parseInt(matcher.group(1)) == 0) {
                    return;
                }
            }
            Thread.sleep(StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS);
            countMillis -= StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS;
        }
        printToCommandConsole(
            StringUtils.format("%s second(s) have passed and not all workflows are finished. Timeout was reached.", timeoutInSecs));
    }

    /**
     * @param instanceId instance whose workflow runs are to be exported
     * @param relativeExportPath relative directory within instances workspace in which to export the workflow runs
     */
    @When("^exporting all workflow runs from \"([^\"]*)\" to \"([^\"]*)\"")
    public void whenExportingAllWorkflows(String instanceId, String relativeExportPath) {
        ManagedInstance instance = resolveInstance(instanceId);
        final File exportDir =
            instance.getAbsolutePathFromRelative(StringUtils.format("%s\\%s", EXPORTED_WORKFLOW_RUNS_SUB_DIR, relativeExportPath));
        executeCommandOnInstance(instance, StringUtils.format("tc export_all_wf_runs %s", exportDir), false);
    }

    /**
     * Verifies all previously exported workflow runs are identical.
     * 
     * @param instanceId the instance whose exported workflow runs are to be identical.
     */
    @Then("all exported workflow run directories from \"([^\"]*)\" should be identical$")
    public void thenAllExportWorkflowRunsIdenical(String instanceId) {
        ManagedInstance instance = resolveInstance(instanceId);
        final File wfParentDir = instance.getAbsolutePathFromRelative(EXPORTED_WORKFLOW_RUNS_SUB_DIR);

        File[] subdirectories = wfParentDir.listFiles(File::isDirectory);
        int length = subdirectories.length;

        if (!wfParentDir.exists() || length == 0) {
            fail(StringUtils.format("There are no exported workflow run directories for instance %s", instanceId));
        }
        if (length == 1) {
            fail(StringUtils.format("There is only one exported workflow run directory for instance %s. At least two are necessary.",
                instanceId));
        }
        File compareDir = subdirectories[0];
        File[] compareDirFiles = compareDir.listFiles(File::isFile);
        for (int i = 1; i < subdirectories.length; i++) {
            File[] files = subdirectories[i].listFiles(File::isFile);
            if (compareDirFiles.length != files.length) {
                fail(StringUtils.format("Subdirectories %s and %s do not contain the same amount of exported wf runs.", compareDir,
                    subdirectories[i]));
            }
            for (int j = 0; j < files.length; j++) {
                String output = executeCommandOnInstance(instance,
                    StringUtils.format("tc compare_wf_runs %s %s", compareDirFiles[j].getAbsolutePath(), files[j].getAbsolutePath()),
                    false);
                if (!output.contains(StepDefinitionConstants.SUCCESS_MESSAGE_WORKFLOW_COMPARISON_IDENTICAL)) {
                    fail(StringUtils.format("The workflow runs %s and %s are not identical.", compareDirFiles[j].getAbsolutePath(),
                        files[j].getAbsolutePath()));
                }
            }
        }
        printToCommandConsole(StringUtils.format("All exported workflow run directories from instance %s are identical.", instanceId));
    }

    /**
     * Verifies the content of the last run workflow's log file.
     * 
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param substring the substring or pattern expected to be present or absent in the workflow log file
     */
    @Then("^the workflow log should (not )?contain (the pattern )?\"(.*)\"$")
    public void thenWorkflowLogContains(String negationFlag, String useRegexpMarker, String substring) throws Exception {
        if (lastWorkflowLogDir == null) {
            fail("Test error: No workflow log present yet");
            return; // to satisfy flow check; never reached
        }

        String wfLogFileContent = "";
        Path wfLogFile = lastWorkflowLogDir.resolve("workflow.log");
        if (Files.exists(lastWorkflowLogDir.resolve("workflow.log.tmp")) || Files.isRegularFile(wfLogFile)) {
            int tries = 3;
            while (tries > 0) {
                if (Files.isRegularFile(wfLogFile)) {
                    wfLogFileContent = FileUtils.readFileToString(wfLogFile.toFile());
                    break;
                }
                Thread.sleep(StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS);
                if (tries == 0) {
                    fail(StepDefinitionConstants.FOUND_LOG_OUTPUT_DIRECTORY_LOCATION + lastWorkflowLogDir
                        + "', but workflow.log.tmp did not convert to workflow.log");
                }
            }
        } else {
            fail(StepDefinitionConstants.FOUND_LOG_OUTPUT_DIRECTORY_LOCATION + lastWorkflowLogDir
                + "', but there is neither workflow.log not workflow.log.tmp file inside");
        }

        if (wfLogFileContent.isEmpty()) {
            fail("Test error: Workflow log was stored, but is empty");
        }
        assertPropertyOfTextOutput(lastWorkflowInitiatingInstance, negationFlag, useRegexpMarker, substring, wfLogFileContent,
            "workflow log");
    }

    /**
     * Verifies that a certain workflow state was reached, typically FINISHED.
     * 
     * @param state the state to test for
     */
    @Then("^the workflow should have reached the (\\w+) state$")
    public void thenWorkflowReachedState(String state) throws Exception {
        // delegate
        thenWorkflowLogContainsCaller(false, false, "NEW_STATE:" + state);
    }

    /**
     * Verifies that a certain instance was used as the workflow controller, with an optional node id test.
     * 
     * @param nodeName the node name to test for
     * @param nodeId optionally, the node id to test for
     */
    @Then("^the workflow controller should have been \"([^\"]+)\"(?: using node id \"(\\w+)\")?$")
    public void thenWorkflowController(String nodeName, String nodeId) throws Exception {
        // delegate
        thenWorkflowLogContainsCaller(false, false,
            "Location of workflow controller: \"" + nodeName + StepDefinitionConstants.ESCAPED_DOUBLE_QUOTE);
    }

    /**
     * Verifies that a certain instance was used as the execution location for the given workflow component, with an optional node id test.
     * 
     * @param compName the workflow name of the component to test for
     * @param nodeName the node name to test for
     * @param uplinkFlag name of the uplink instance via which the component is made accessible
     * @param nodeId optionally, the node id to test for
     */
    @Then("^workflow component \"([^\"]+)\" should have been run on \"([^\"]+)\"( via uplink)?(?: using node id \"(\\w+)\")?$")
    public void thenComponentRanOn(String compName, String nodeName, String uplinkFlag, String nodeId) throws Exception {
        if (uplinkFlag != null) {
            nodeName += " \\(via [^)]+\\)";
        }
        String regexp =
            "Location of workflow component \"" + compName + "\" [^:]+: \"" + nodeName + StepDefinitionConstants.ESCAPED_DOUBLE_QUOTE;
        if (nodeId != null) {
            regexp += " \\[" + nodeId + ":0\\]";
        }
        thenWorkflowLogContainsCaller(false, true, regexp);
    }

    /**
     * Verifies that a certain instance was used as the execution location for the given workflow component, with an optional node id test.
     * 
     * @param compName the workflow name of the component to test for
     */
    @Then("^workflow component \"([^\"]+)\" should have been cancelled$")
    public void thenComponentCancelledOn(String compName) throws Exception {
        whenWaitingUntilWorkflowReachedState(lastWorkflowName, lastWorkflowInitiatingInstance.getId(), "cancelled", null);
        thenComponentTerminated(compName);
    }

    /**
     * Verifies that a certain instance was used as the execution location for the given workflow component, with an optional node id test.
     * 
     * @param compName the workflow name of the component to test for
     */
    @Then("^workflow component \"([^\"]+)\" should be terminated$")
    public void thenComponentTerminated(String compName) throws Exception {
        String regexp = "\\[LIFE_CYCLE_EVENT\\] \\[" + compName + "\\] COMPONENT_TERMINATED";
        thenWorkflowLogContainsCaller(false, true, regexp);
    }

    /**
     * Verifies all given instances can see the list of corresponding workflows.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param workflowNames a comma-separated list of workflownames which are to be seen.
     */
    @Then("^(all )?(?:instance[s]? )?(?:\"([^\"]+)\" )?should see(?: workflow[s]?)? \"([^\"]+)\"$")
    public void thenInstancesShouldSeeworkflow(String allFlag, String instanceIds, String workflowNames) {
        performActionOnInstances(
            new AssertWorkflowVisibilityAction(parseCommaSeparatedList(workflowNames)),
            resolveInstanceList(allFlag != null, instanceIds),
            InstanceActionExecutionType.RANDOM);
    }

    /**
     * Verifies all given instances can see the data to the corresponding workflow runs.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param workflowNames a comma-separated list of workflownames which are to be seen.
     */
    @Then("^(all )?(?:instance[s]? )?(?:\"([^\"]+)\" )?should see identical data for(?: workflow[s]?)? \"([^\"]+)\"$")
    public void thenInstancesShouldSeeIdenticalData(String allFlag, String instanceIds, String workflowNames) {
        List<ManagedInstance> instances = resolveInstanceList(allFlag != null, instanceIds);
        if (instances.size() < 2) {
            fail("At least two instances are neccessara to have a meaningful comparison. Less were provided.");
        }
        performActionOnInstances(
            new AssertWorkflowDataAction(instances.remove(0), parseCommaSeparatedList(workflowNames)),
            instances,
            InstanceActionExecutionType.RANDOM);
    }

    private String convertWorkflowPathToName(String workflowPath) {
        return (new File(workflowPath)).getName();
    }

    private String[] startWorkflowOnInstance(final ManagedInstance instance, String workflowName, String placeholderFile)
        throws IOException {
        boolean hasPlaceholder = placeholderFile != null;
        final String instanceId = instance.getId();

        Path originalWfFileLocation = executionContext.getTestScriptLocation().toPath().resolve(WORKFLOWS).resolve(workflowName);
        if (!Files.isRegularFile(originalWfFileLocation)) {
            throw new AssertionError("No workflow file found at expected location " + originalWfFileLocation);
        }
        final File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("bdd-wf");
        Path wfFileLocation = tempDir.toPath().resolve(originalWfFileLocation.getFileName());
        Files.copy(originalWfFileLocation, wfFileLocation);
        String startInfoText = StringUtils.format("Starting workflow %s on instance %s", workflowName, instanceId);

        Path placeholderFileLocation = null;

        if (hasPlaceholder) {
            if (!(new File(placeholderFile).isAbsolute())) {
                Path testLocation = executionContext.getTestScriptLocation().toPath();
                Path subdir = testLocation.resolve(WORKFLOWS).resolve("placeholder_values");
                Path originalPlaceholderFileLocation = subdir.resolve(placeholderFile);
                if (!Files.isRegularFile(originalPlaceholderFileLocation)) {
                    throw new AssertionError("No placeholder file found at expected location " + originalWfFileLocation);
                }
                placeholderFileLocation = tempDir.toPath().resolve(originalPlaceholderFileLocation.getFileName());
                Files.copy(originalPlaceholderFileLocation, placeholderFileLocation);
            } else {
                placeholderFileLocation = Paths.get(placeholderFile);
            }
            startInfoText = StringUtils.format("%s using placeholders from %s", startInfoText, placeholderFile);
        }

        printToCommandConsole(startInfoText);
        log.debug(startInfoText);

        CapturingTextOutReceiver commandOutputReceiver = new CapturingTextOutReceiver();
        try {
            String[] workflowInfo;

            if (hasPlaceholder) {
                // placeholderFileLocation should have a value, as this block is only reached if the initialization block is also reached
                workflowInfo = INSTANCE_MANAGEMENT_SERVICE.startWorkflowOnInstance(instanceId, wfFileLocation, placeholderFileLocation,
                    commandOutputReceiver);
            } else {
                workflowInfo = INSTANCE_MANAGEMENT_SERVICE.startWorkflowOnInstance(instanceId, wfFileLocation, commandOutputReceiver);
            }

            if (workflowInfo == null || workflowInfo.length != StepDefinitionConstants.EXPECTED_WORKFLOW_INFO_LENGTH) {
                fail("Info about started workflow did not contain the expected amount of information.");
            }

            instance.setLastCommandOutput(commandOutputReceiver.getBufferedOutput());
            executionContext.setLastInstanceWithSingleCommandExecution(instance);
            log.debug(StringUtils.format("Started workflow %s on instance %s", workflowName, instanceId));
            return workflowInfo;
        } catch (JSchException | SshParameterException | IOException | InterruptedException e) {
            fail(StringUtils.format("Failed to start workflow %s on instance %s: %s", workflowName, instanceId, e.toString()));
            return null; // dummy command; never reached
        }

    }

    private void thenWorkflowLogContainsCaller(boolean negate, boolean useRegex, String contents) throws Exception {
        String negationFlag = null;
        String regexFlag = null;

        if (negate) {
            negationFlag = StepDefinitionConstants.USE_NEGATION_MARKER;
        }
        if (useRegex) {
            regexFlag = StepDefinitionConstants.USE_REGEXP_MARKER;
        }

        thenWorkflowLogContains(negationFlag, regexFlag, contents);
    }

    private int parseTimeout(String timeoutString) {
        final int timeoutInSecs;
        if (timeoutString == null) {
            timeoutInSecs = StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS;
        } else {
            timeoutInSecs = Integer.parseInt(timeoutString);
        }
        return timeoutInSecs;
    }

    private String addExtension(String file, String extension) {
        if (!file.endsWith(extension)) {
            file += extension;
        }
        return file;
    }

    private String getWorkflowKey(String workflowName, String instanceId) {
        return StringUtils.format("%s_%s", workflowName, instanceId);
    }

}
