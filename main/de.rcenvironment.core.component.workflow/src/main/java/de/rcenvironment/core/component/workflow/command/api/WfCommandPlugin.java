/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.command.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.execution.api.WorkflowGraphEdge;
import de.rcenvironment.core.component.execution.api.WorkflowGraphNode;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerification;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationRecorder;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionVerificationResult;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * A {@link CommandPlugin} providing "wf [...]" commands.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class WfCommandPlugin implements CommandPlugin {

    private static final String SELF_TEST_CASES_FILE_ENDING = ".cases";

    private static final String SELF_TEST_CASES_DIR = "cases/";

    private static final String SELF_TEST_WORKFLOW_DIR = "workflows/";

    private static final String SLASH = "/";

    private static final String ESCAPED_BACKSLASH = "\\\\";

    private static final String ASTERISK = "*";

    private static final String DELETE_COMMAND = "delete";

    private static final int PARSING_WORKFLOW_FILE_RETRY_INTERVAL = 2000;

    private static final int MAXIMUM_WORKFLOW_PARSE_RETRIES = 5;

    private static final String BASEDIR_OPTION = "--basedir";

    private static final String STRING_DOT = ".";

    private static final int WORKFLOW_SUFFIX_NUMBER_MODULO = 100;

    private static final String WRONG_STATE_ERROR = "%s workflow not possible in current workflow state: %s";

    private static final String WORKFLOW_ID = "<id>";

    // TODO >5.0.0: crude fix for #10436 - align better with generated workflow name - misc_ro
    private static final AtomicInteger GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER = new AtomicInteger();

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private Log log = LogFactory.getLog(getClass());

    private MetaDataService metaDataService;

    private Bundle bundle;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription("wf", "", false, "short form of \"wf list\""));
        contributions.add(new CommandDescription("wf run",
            "[--dispose <onfinished|never|always>] [--delete <onfinished|never|always>] [--compact-output] "
                + "[-p <JSON placeholder file>] <workflow file>",
            false, "execute a workflow file"));
        contributions.add(new CommandDescription("wf verify",
            "[--dispose <onexpected|never|always>] [--delete <onexpected|never|always>] "
                + "[--pr <parallel runs>] [--sr <sequential runs>] [-p <JSON placeholder file>] "
                + "([--basedir <root directory for all subsequent files>] (<workflow filename>|\"*\")+ )+",
            false, "batch test the specified workflow files"));
        contributions.add(new CommandDescription("wf list", "",
            false, "show workflow list"));
        contributions.add(new CommandDescription("wf details", WORKFLOW_ID,
            false, "show details of a workflow"));
        contributions.add(new CommandDescription("wf pause", WORKFLOW_ID,
            false, "pause a running workflow"));
        contributions.add(new CommandDescription("wf resume", WORKFLOW_ID,
            false, "resume a paused workflow"));
        contributions.add(new CommandDescription("wf cancel", WORKFLOW_ID,
            false, "cancel a running or paused workflow"));
        contributions.add(new CommandDescription("wf delete", WORKFLOW_ID,
            false, "delete and dispose a finished, cancelled or failed workflow"));
        contributions.add(new CommandDescription("wf self-test",
            "[--dispose <onexpected|never|always>] [--delete <onexpected|never|always>] "
                + "[--pr <parallel runs>] [--sr <sequential runs>] [--python <python exe path; default: 'python'>]"
                + " [--cases <comma-seperated list of case files; default: 'core'>]",
            true, "batch test workflow files of the test workflow files bundle"));
        contributions.add(new CommandDescription("wf list-self-test-cases", "",
            true, "list available test cases for wf self-test"));
        contributions.add(new CommandDescription("wf check-self-test-cases", "",
            true, "check if all test workflows are part of at least one test case"));
        contributions.add(new CommandDescription("wf graph", "<workflow file>",
            true, "prints .dot string representation of a workflow (can be used to create graph visualization with Graphviz)"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken("wf");
        String subCmd = context.consumeNextToken();
        if (subCmd == null) {
            // "wf" -> "wf list" by default
            performWfList(context);
        } else {
            if ("run".equals(subCmd)) {
                // "wf run <filename>"
                performWfRun(context);
            } else if ("verify".equals(subCmd)) {
                // "wf verify ..."
                performWfVerify(context);
            } else if ("pause".equals(subCmd)) {
                // "wf pause ..."
                performWfPause(context);
            } else if ("resume".equals(subCmd)) {
                // "wf resume ..."
                performWfResume(context);
            } else if ("cancel".equals(subCmd)) {
                // "wf cancel ..."
                performWfCancel(context);
            } else if ("dispose".equals(subCmd)) {
                // "wf dispose ..."
                performWfDisposeOrDelete(context, subCmd);
            } else if (DELETE_COMMAND.equals(subCmd)) {
                // "wf delete ..."
                performWfDisposeOrDelete(context, subCmd);
            } else if ("list".equals(subCmd)) {
                // "wf list ..."
                performWfList(context);
            } else if ("details".equals(subCmd)) {
                // "wf details ..."
                performWfShowDetails(context);
            } else if ("self-test".equals(subCmd)) {
                performWfSelfTest(context);
            } else if ("list-self-test-cases".equals(subCmd)) {
                performWfListSelfTestCases(context);
            } else if ("check-self-test-cases".equals(subCmd)) {
                performWfCheckSelfTestCases(context);
            } else if ("graph".equals(subCmd)) {
                performWfGraph(context);
            } else {
                throw CommandException.unknownCommand(context);
            }
        }
    }

    private File getWorkflowFile(CommandContext cmdCtx) throws CommandException {
        final String filename = cmdCtx.consumeNextToken();
        // verify: filename is present and the only parameter
        if (filename == null) {
            throw CommandException.syntaxError("Missing filename", cmdCtx);
        }

        final String additionalToken = cmdCtx.consumeNextToken();
        if (additionalToken != null) {
            throw CommandException.syntaxError("Expected end of command, but found another argument: " + additionalToken, cmdCtx);
        }
        final File wfFile;
        try {
            wfFile = WorkflowExecutionUtils.resolveWorkflowOrPlaceholderFileLocation(filename,
                WorkflowExecutionUtils.DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_WORKFLOW_FILE);
        } catch (FileNotFoundException e) {
            throw CommandException.executionError(e.getMessage(), cmdCtx);
        }
        return wfFile;
    }

    /**
     * OSGi DS life-cycle method.
     * 
     * @param context {@link ComponentContext} injected
     */
    public void activate(ComponentContext context) {
        bundle = context.getBundleContext().getBundle();
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindWorkflowExecutionService(HeadlessWorkflowExecutionService newInstance) {
        this.workflowExecutionService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindMetaDataService(MetaDataService newInstance) {
        this.metaDataService = newInstance;
    }

    private void performWfRun(CommandContext cmdCtx) throws CommandException {
        // "wf run [--dispose <...>] [--compact-output] [-p <JSON placeholder file>] <filename>"

        HeadlessWorkflowExecutionService.DisposalBehavior dispose = readOptionalDisposeParameter(cmdCtx);

        HeadlessWorkflowExecutionService.DeletionBehavior delete = readOptionalDeleteParameter(cmdCtx);

        boolean compactId = cmdCtx.consumeNextTokenIfEquals("--compact-output");

        File placeholdersFile = readOptionalPlaceholdersFileParameter(cmdCtx);

        final String filename = cmdCtx.consumeNextToken();
        // verify: filename is present and the only parameter
        if (filename == null) {
            throw CommandException.syntaxError("Missing filename", cmdCtx);
        }

        final String additionalToken = cmdCtx.consumeNextToken();
        if (additionalToken != null) {
            throw CommandException.syntaxError("Expected end of command, but found another argument: " + additionalToken, cmdCtx);
        }
        final File wfFile;
        try {
            wfFile = WorkflowExecutionUtils.resolveWorkflowOrPlaceholderFileLocation(filename,
                WorkflowExecutionUtils.DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_WORKFLOW_FILE);
        } catch (FileNotFoundException e) {
            throw CommandException.executionError(e.getMessage(), cmdCtx);
        }
        if (preValidateWorkflow(cmdCtx, wfFile)) {
            try {
                // TODO specify log directory?
                HeadlessWorkflowExecutionContextBuilder exeContextBuilder =
                    new HeadlessWorkflowExecutionContextBuilder(wfFile, setupLogDirectoryForWfFile(wfFile));
                exeContextBuilder.setPlaceholdersFile(placeholdersFile);
                exeContextBuilder.setTextOutputReceiver(cmdCtx.getOutputReceiver(), compactId);
                exeContextBuilder.setDisposalBehavior(dispose);
                exeContextBuilder.setDeletionBehavior(delete);

                workflowExecutionService.executeWorkflowSync(exeContextBuilder.build());
            } catch (WorkflowExecutionException e) {
                log.error("Exception while executing workflow: " + wfFile.getAbsolutePath(), e);
                throw CommandException.executionError(ComponentUtils.createErrorLogMessage(e), cmdCtx);
            } catch (InvalidFilenameException e) {
                throw CommandException.executionError(ComponentUtils.createErrorLogMessage(e), cmdCtx);
            }
        } else {
            cmdCtx.getOutputReceiver()
                .addOutput(StringUtils.format("'%s' not executed due to validation errors (see log messages above) (full path: %s)",
                    wfFile.getName(), wfFile.getAbsolutePath()));
        }
    }

    /**
     * Note: retry mechanism applied to ensure that all available components and their updaters are registered (both remote and local); it
     * slows down the execution as parsing the workflow file is done at least twice: when pre-validating workflow and for actual execution
     * -> should be improved (https://mantis.sc.dlr.de/view.php?id=14716).
     * 
     * @param context {@link CommandContext} to use
     * @param wfFile workflow to validate
     * @return <code>true</code> if workflow is valid, otherwise <code>false</code>
     */
    private boolean preValidateWorkflow(CommandContext context, File wfFile) {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();
        WorkflowDescription workflowDescription;
        int retries = 0;
        while (true) {
            try {
                workflowDescription = workflowExecutionService
                    .loadWorkflowDescriptionFromFileConsideringUpdates(wfFile,
                        new HeadlessWorkflowDescriptionLoaderCallback(outputReceiver));
            } catch (WorkflowFileException e) {
                log.error("Exception while parsing the workflow file " + wfFile.getAbsolutePath(), e);
                outputReceiver.addOutput(StringUtils.format("Error when parsing '%s': %s (full path: %s)", wfFile.getName(), e.getMessage(),
                    wfFile.getAbsolutePath()));
                return false;
            }
            if (WorkflowExecutionUtils.hasMissingWorkflowNode(workflowDescription.getWorkflowNodes())) {
                if (retries >= MAXIMUM_WORKFLOW_PARSE_RETRIES) {
                    log.debug(StringUtils.format("Maximum number of retries (%d) reached while validating the workflow file '%s'",
                        MAXIMUM_WORKFLOW_PARSE_RETRIES, wfFile.getAbsolutePath()));
                    outputReceiver.addOutput(StringUtils.format("Workflow component(s) of '%s' unknown: %s (full path: %s)",
                        wfFile.getName(), wfFile.getAbsolutePath()));
                    return false;
                }
                retries = waitForWorkflowValidationRetry(retries);
                continue;
            } else {
                outputReceiver.addOutput(
                    StringUtils.format("Validating target instances of '%s'... (full path: %s", wfFile.getName(),
                        wfFile.getAbsolutePath()));
                WorkflowDescriptionValidationResult validationResult =
                    workflowExecutionService.validateWorkflowDescription(workflowDescription);
                if (validationResult.isSucceeded()) {
                    outputReceiver
                        .addOutput(StringUtils.format("Target instance(s) of '%s' valid (full path: %s", wfFile.getName(),
                            wfFile.getAbsolutePath()));
                    break;
                } else {
                    if (retries >= MAXIMUM_WORKFLOW_PARSE_RETRIES) {
                        log.debug(StringUtils.format("Maximum number of retries (%d) reached while validating the workflow file '%s'",
                            MAXIMUM_WORKFLOW_PARSE_RETRIES, wfFile.getAbsolutePath()));
                        outputReceiver.addOutput(StringUtils.format("Some target instance(s) of '%s' unknown: %s (full path: %s)",
                            wfFile.getName(), validationResult.toString(), wfFile.getAbsolutePath()));
                        return false;
                    }
                    retries = waitForWorkflowValidationRetry(retries);
                    continue;
                }
            }
        }
        return true;
    }

    private int waitForWorkflowValidationRetry(int retries) {
        log.debug("Retrying workflow validation in a few seconds");
        try {
            Thread.sleep(PARSING_WORKFLOW_FILE_RETRY_INTERVAL);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for parsing retry", e);
        }
        return ++retries;
    }

    private void performWfVerify(final CommandContext context) throws CommandException {
        HeadlessWorkflowExecutionService.DisposalBehavior dispose = readOptionalDisposeParameter(context);
        HeadlessWorkflowExecutionService.DeletionBehavior delete = readOptionalDeleteParameter(context);

        int parallelRuns = readOptionalParallelRunsParameter(context);
        int sequentialRuns = readOptionalSequentialRunsParameter(context);
        File placeholdersFile = readOptionalPlaceholdersFileParameter(context);
        List<File> wfFiles = parseWfVerifyCommand(context);
        HeadlessWorkflowExecutionVerificationResult wfVerifyResultVerification =
            executeWfVerifySetup(context, wfFiles, placeholdersFile, parallelRuns, sequentialRuns, dispose, delete);
        context.println(wfVerifyResultVerification.getVerificationReport());
    }

    private void performWfPause(final CommandContext context) throws CommandException {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = context.consumeNextToken();
        // verify: executionId is present
        if (executionId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        WorkflowExecutionInformation wfExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wfExecInf != null) {
            // Find the node running this workflow
            LogicalNodeId nodeId = wfExecInf.getWorkflowDescription().getControllerNode();

            try {
                if (wfExecInf.getWorkflowState().equals(WorkflowState.RUNNING)) {
                    workflowExecutionService.pause(executionId, nodeId);
                } else {
                    outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Pausing", wfExecInf.getWorkflowState()));
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to pause workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfResume(final CommandContext context) throws CommandException {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = context.consumeNextToken();
        // verify: executionId is present
        if (executionId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf != null) {
            // Find the node running this workflow
            ResolvableNodeId nodeId = wExecInf.getWorkflowDescription().getControllerNode();

            try {
                if (wExecInf.getWorkflowState().equals(WorkflowState.PAUSED)) {
                    workflowExecutionService.resume(executionId, nodeId);
                } else {
                    outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Resuming", wExecInf.getWorkflowState()));
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to resume workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfCancel(final CommandContext context) throws CommandException {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = context.consumeNextToken();
        // verify: executionId is present
        if (executionId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf != null) {
            // Find the node running this workflow
            ResolvableNodeId nodeId = wExecInf.getWorkflowDescription().getControllerNode();

            try {
                if (wExecInf.getWorkflowState().equals(WorkflowState.RUNNING)
                    || wExecInf.getWorkflowState().equals(WorkflowState.PAUSED)) {
                    workflowExecutionService.cancel(executionId, nodeId);
                } else {
                    outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Canceling", wExecInf.getWorkflowState()));
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to cancel workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfDisposeOrDelete(final CommandContext context, String token) throws CommandException {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = context.consumeNextToken();
        // verify: executionId is present
        if (executionId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf != null) {
            // Find the node running this workflow
            ResolvableNodeId nodeId = wExecInf.getWorkflowDescription().getControllerNode();

            try {
                if (WorkflowConstants.FINAL_WORKFLOW_STATES.contains(wExecInf.getWorkflowState())) {
                    if (token.equals(DELETE_COMMAND)) {
                        metaDataService.deleteWorkflowRun(wExecInf.getWorkflowDataManagementId(), nodeId);
                    }
                    workflowExecutionService.dispose(executionId, nodeId);

                } else {
                    if (token.equals(DELETE_COMMAND)) {
                        outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Deleting", wExecInf.getWorkflowState()));
                    } else {
                        outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Disposing", wExecInf.getWorkflowState()));
                    }
                }
            } catch (ExecutionControllerException | CommunicationException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to dispose workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfList(final CommandContext context) throws CommandException {

        TextOutputReceiver outputReceiver = context.getOutputReceiver();
        outputReceiver.addOutput("Fetching workflows...");
        List<WorkflowExecutionInformation> wfInfos = new ArrayList<>(workflowExecutionService.getWorkflowExecutionInformations(true));
        Collections.sort(wfInfos);
        String output = "";
        int total = 0;
        int running = 0;
        int paused = 0;
        int finished = 0;
        int cancelled = 0;
        int failed = 0;
        int resultsRejected = 0;
        int other = 0;

        for (WorkflowExecutionInformation wfInfo : wfInfos) {
            WorkflowState state = wfInfo.getWorkflowState();
            output += StringUtils.format(" '%s' - %s [%s]\n", wfInfo.getInstanceName(), state, wfInfo.getExecutionIdentifier());
            total++;
            switch (state) {
            case RUNNING:
                running++;
                break;
            case PAUSED:
                paused++;
                break;
            case FINISHED:
                finished++;
                break;
            case CANCELLED:
                cancelled++;
                break;
            case FAILED:
                failed++;
            case RESULTS_REJECTED:
                resultsRejected++;
                break;
            default:
                other++;
            }
        }
        output +=
            StringUtils.format(" -- TOTAL COUNT: %d workflow(s): %d running, %d paused, %d finished,"
                + " %d cancelled, %d failed, %d verification failed, %d other -- ",
                total, running, paused, finished, cancelled, failed, resultsRejected, other);
        outputReceiver.addOutput(output);
    }

    private void performWfShowDetails(final CommandContext context) throws CommandException {

        final String executionId = context.consumeNextToken();
        // verify: executionId is present
        if (executionId == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        TextOutputReceiver outputReceiver = context.getOutputReceiver();
        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd  HH:mm:ss");
        if (wExecInf != null) {
            outputReceiver.addOutput("Name: " + wExecInf.getInstanceName());
            outputReceiver.addOutput("Status: " + wExecInf.getWorkflowState());
            outputReceiver.addOutput("Controller: " + wExecInf.getWorkflowDescription().getControllerNode().getAssociatedDisplayName());
            outputReceiver.addOutput("Start: " + df.format(wExecInf.getStartTime()));
            outputReceiver.addOutput("Started from: " + wExecInf.getNodeIdStartedExecution().getAssociatedDisplayName());
            outputReceiver.addOutput("Additional Information: ");
            String additional = wExecInf.getAdditionalInformationProvidedAtStart();
            if (additional != null) {
                outputReceiver.addOutput(additional);
            }
            // outputReceiver.addOutput("Execution Identifier: " +
            // workflow.getExecutionIdentifier());
            // outputReceiver.addOutput("Node Identifier: " +
            // workflow.getWorkflowDescription().getControllerNode().getIdString());
        }
    }

    private String getSelfTestDirPath() {
        String path = "/src/main/resources/";
        if (bundle.findEntries(path, ASTERISK, false) == null) {
            path = "/";
        }
        return path;
    }

    private void performWfListSelfTestCases(final CommandContext context) throws CommandException {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();
        for (String testCaseFileName : getTestCaseFileNamesWithoutEnding()) {
            try (InputStream caseFileInputStream = getClass().getResourceAsStream(
                getSelfTestDirPath() + SELF_TEST_CASES_DIR + testCaseFileName + SELF_TEST_CASES_FILE_ENDING)) {
                outputReceiver.addOutput(StringUtils.format("%s [%d]", testCaseFileName, IOUtils.readLines(caseFileInputStream).size()));
            } catch (IOException e) {
                throw CommandException.executionError("Failed to read test case file: " + testCaseFileName, context);
            }
        }
    }

    private void performWfCheckSelfTestCases(final CommandContext context) throws CommandException {
        TextOutputReceiver outputReceiver = context.getOutputReceiver();
        List<String> wfFileNamesWithoutEnding = new ArrayList<>();
        @SuppressWarnings("rawtypes") Enumeration selfTestFolderEntries =
            bundle.findEntries(getSelfTestDirPath() + SELF_TEST_WORKFLOW_DIR, ASTERISK + WorkflowConstants.WORKFLOW_FILE_ENDING, true);
        while (selfTestFolderEntries.hasMoreElements()) {
            URL entryURL = (URL) selfTestFolderEntries.nextElement();
            String[] pathParts = entryURL.getPath().split(SLASH);
            wfFileNamesWithoutEnding.add(pathParts[pathParts.length - 1].replace(WorkflowConstants.WORKFLOW_FILE_ENDING, ""));
        }
        @SuppressWarnings("rawtypes") Enumeration selfTestFailureFolderEntries =
            bundle.findEntries(getSelfTestDirPath() + SELF_TEST_WORKFLOW_DIR + HeadlessWorkflowExecutionVerification.FAILURE_DIR_NAME,
                ASTERISK + WorkflowConstants.WORKFLOW_FILE_ENDING, true);
        while (selfTestFailureFolderEntries.hasMoreElements()) {
            URL entryURL = (URL) selfTestFailureFolderEntries.nextElement();
            String[] pathParts = entryURL.getPath().split(SLASH);
            wfFileNamesWithoutEnding.add(pathParts[pathParts.length - 1].replace(WorkflowConstants.WORKFLOW_FILE_ENDING, ""));
        }

        Set<String> wfPartOfTestCase = new HashSet<>();
        for (String testCaseFileName : getTestCaseFileNamesWithoutEnding()) {
            try (InputStream caseFileInputStream = getClass().getResourceAsStream(
                getSelfTestDirPath() + SELF_TEST_CASES_DIR + testCaseFileName + SELF_TEST_CASES_FILE_ENDING)) {
                wfPartOfTestCase.addAll(IOUtils.readLines(caseFileInputStream));
            } catch (IOException e) {
                throw CommandException.executionError("Failed to read test case file: " + testCaseFileName, context);
            }
        }
        wfFileNamesWithoutEnding.removeAll(wfPartOfTestCase);
        if (wfFileNamesWithoutEnding.isEmpty()) {
            outputReceiver.addOutput("Ok: Every workflow file is considered by at least one test case");
        } else {
            outputReceiver.addOutput("Failed: Workflow file(s) are not considered by at least one test case: " + wfFileNamesWithoutEnding);
        }
    }

    private List<String> getTestCaseFileNamesWithoutEnding() {
        List<String> testCaseFileNamesWithoutEnding = new ArrayList<>();
        @SuppressWarnings("rawtypes") final Enumeration selfTestFolderEntries =
            bundle.findEntries(getSelfTestDirPath() + SELF_TEST_CASES_DIR, "*" + SELF_TEST_CASES_FILE_ENDING, false);
        while (selfTestFolderEntries.hasMoreElements()) {
            URL entryURL = (URL) selfTestFolderEntries.nextElement();
            String[] pathParts = entryURL.getPath().split(SLASH);
            testCaseFileNamesWithoutEnding.add(pathParts[pathParts.length - 1].replace(SELF_TEST_CASES_FILE_ENDING, ""));
        }
        return testCaseFileNamesWithoutEnding;
    }

    private void performWfSelfTest(final CommandContext context) throws CommandException {

        HeadlessWorkflowExecutionService.DisposalBehavior dispose = readOptionalDisposeParameter(context);
        HeadlessWorkflowExecutionService.DeletionBehavior delete = readOptionalDeleteParameter(context);

        int parallelRuns = readOptionalParallelRunsParameter(context);
        int sequentialRuns = readOptionalSequentialRunsParameter(context);

        String[] cases = getCasesParameter(context);
        String pythonPath = null;
        TempFileService tempFileService = TempFileServiceAccess.getInstance();

        File tempSelfTestWorkflowDir = null;
        File tempSelfTestWorkflowFailureDir = null;

        File tempPlaceholdersStuffDir = null;
        File placeholdersFile = null;

        try {
            tempSelfTestWorkflowDir = tempFileService.createManagedTempDir();
            tempSelfTestWorkflowFailureDir = new File(tempSelfTestWorkflowDir, HeadlessWorkflowExecutionVerification.FAILURE_DIR_NAME);
            tempPlaceholdersStuffDir = tempFileService.createManagedTempDir();
        } catch (IOException e) {
            String message = "Failed to create temp directory required for self-test workflow execution";
            handleWfSelfTestExecutionError(context, tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir, e, message);
        }

        if (Arrays.asList(cases).contains("with-python")) {
            try {
                pythonPath = getAndCheckPythonPath(context, tempSelfTestWorkflowDir);
            } catch (IOException e) {
                String message = "Failed to check command to be used to invoke Python";
                handleWfSelfTestExecutionError(context, tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir, e, message);
            }
        }

        try {
            copyWorkflowsForSelfTest(tempFileService, tempSelfTestWorkflowDir, tempSelfTestWorkflowFailureDir, cases);
        } catch (IOException e) {
            String message = "Failed to copy workflow files from self-test folder to temp directory: " + e.getMessage();
            handleWfSelfTestExecutionError(context, tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir, e, message);
        }

        try {
            placeholdersFile = generatePlaceholdersRelatedFiles(tempPlaceholdersStuffDir, pythonPath);
        } catch (IOException e) {
            String message = "Failed to create placeholders-related files required for self-test workflow execution";
            handleWfSelfTestExecutionError(context, tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir, e, message);
        }

        // construct synthetic "wf verify" command tokens and delegate
        List<String> newTokens = new ArrayList<>();
        newTokens.add(BASEDIR_OPTION);
        newTokens.add(tempSelfTestWorkflowDir.getAbsolutePath());
        newTokens.add(ASTERISK);
        if (tempSelfTestWorkflowFailureDir.exists()) {
            newTokens.add(BASEDIR_OPTION);
            newTokens.add(tempSelfTestWorkflowFailureDir.getAbsolutePath());
            newTokens.add(ASTERISK);
        }
        CommandContext syntheticContext = new CommandContext(newTokens, context.getOutputReceiver(), context.getInvokerInformation());
        HeadlessWorkflowExecutionVerificationResult wfVerifyResultVerification =
            executeWfVerifySetup(context, parseWfVerifyCommand(syntheticContext), placeholdersFile, parallelRuns, sequentialRuns, dispose,
                delete);

        context.println(wfVerifyResultVerification.getVerificationReport());

        if (!delete.equals(HeadlessWorkflowExecutionService.DeletionBehavior.Never)
            && (wfVerifyResultVerification.isVerified() || delete.equals(HeadlessWorkflowExecutionService.DeletionBehavior.Always))) {
            disposeTempDirsCreatedForSelfTest(tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir);
        } else if (delete.equals(HeadlessWorkflowExecutionService.DeletionBehavior.OnExpected)) {
            for (File file : wfVerifyResultVerification.getWorkflowRelatedFilesToDelete()) {
                try {
                    tempFileService.disposeManagedTempDirOrFile(file);
                } catch (IOException e) {
                    log.error("Failed to delete workflow file after execution: " + file, e);
                }
            }
        }
    }

    private String getAndCheckPythonPath(CommandContext context, File workDir) throws CommandException, IOException {
        String pythonPath = getPythonPathParameter(context);

        LocalApacheCommandLineExecutor executor = new LocalApacheCommandLineExecutor(workDir);
        executor.start(pythonPath + " --version");
        try {
            if (executor.waitForTermination() != 0) {
                throw CommandException.executionError("Command to invoke Python invalid: " + pythonPath, context);
            }
        } catch (InterruptedException e) {
            throw CommandException.executionError("Interupted when checking command to invoke Python: " + e.getMessage(), context);
        }
        try (InputStream stdErrStream = executor.getStderr()) {
            context.getOutputReceiver().addOutput("Using: " + IOUtils.toString(stdErrStream));
        }
        return pythonPath;
    }

    private void handleWfSelfTestExecutionError(final CommandContext context, TempFileService tempFileService,
        File tempSelfTestWorkflowDir,
        File tempPlaceholdersStuffDir, IOException e, String message) throws CommandException {
        log.error(StringUtils.format("%s: %s", message, e.getMessage()));
        disposeTempDirsCreatedForSelfTest(tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir);
        throw CommandException.executionError(message, context);
    }

    private void disposeTempDirsCreatedForSelfTest(TempFileService tempFileService, File tempSelfTestWorkflowDir,
        File tempPlaceholdersStuffDir) {
        if (tempSelfTestWorkflowDir != null) {
            try {
                tempFileService.disposeManagedTempDirOrFile(tempSelfTestWorkflowDir);
            } catch (IOException e) {
                log.error("Failed to dispose temp directory created for self-test workflow execution: " + tempSelfTestWorkflowDir, e);
            }
        }
        if (tempPlaceholdersStuffDir != null) {
            try {
                tempFileService.disposeManagedTempDirOrFile(tempPlaceholdersStuffDir);
            } catch (IOException e) {
                log.error("Failed to dispose temp directory created for self-test workflow execution: " + tempPlaceholdersStuffDir, e);
            }
        }
    }

    private List<String> getWorkflowsForSelfTest(String[] cases) throws IOException {
        List<String> wfs = new ArrayList<>();
        for (String caseName : cases) {
            try (InputStream caseInputStream =
                getClass().getResourceAsStream(getSelfTestDirPath() + SELF_TEST_CASES_DIR + caseName + SELF_TEST_CASES_FILE_ENDING)) {
                if (caseInputStream == null) {
                    throw new IOException("Case unknown: " + caseName);
                } else {
                    wfs.addAll(IOUtils.readLines(caseInputStream));
                }
            }
        }
        return wfs;
    }

    private void copyWorkflowsForSelfTest(TempFileService tempFileService, File tempSelfTestWorkflowDir,
        File tempSelfTestWorkflowFailureDir, String[] cases) throws IOException {

        List<String> wfsForSelfTest = getWorkflowsForSelfTest(cases);

        @SuppressWarnings("rawtypes") final Enumeration selfTestFolderEntries =
            bundle.findEntries(getSelfTestDirPath() + SELF_TEST_WORKFLOW_DIR, ASTERISK, true);
        while (selfTestFolderEntries.hasMoreElements()) {
            final URL entryURL = (URL) selfTestFolderEntries.nextElement();
            final String entryRawPath = entryURL.getPath();
            if (entryRawPath.endsWith(HeadlessWorkflowExecutionVerification.FAILURE_DIR_NAME + SLASH)) {
                tempSelfTestWorkflowFailureDir.mkdir();
            } else if (entryRawPath.endsWith(SLASH)) {
                throw new IOException("Unexpected directory in seft-test directory: " + entryRawPath);
            } else {
                String[] pathParts = entryRawPath.split(SLASH);
                String selfTestFileName = pathParts[pathParts.length - 1];
                validateFileInSelfTestDirectory(selfTestFileName);
                String selfTestFileNameWithoutEnding = selfTestFileName.replaceAll(WorkflowConstants.WORKFLOW_FILE_ENDING, "");
                if (selfTestFileName.endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
                    && !wfsForSelfTest.contains(selfTestFileNameWithoutEnding)) {
                    continue;
                }
                wfsForSelfTest.remove(selfTestFileNameWithoutEnding);
                File targetFile;
                if (pathParts.length > 1
                    && pathParts[pathParts.length - 2].equals(HeadlessWorkflowExecutionVerification.FAILURE_DIR_NAME)) {
                    targetFile = new File(tempSelfTestWorkflowFailureDir, selfTestFileName);
                } else {
                    targetFile = new File(tempSelfTestWorkflowDir, selfTestFileName);
                }
                try (InputStream entryInputStream = entryURL.openStream();
                    FileWriter targetTempFileWriter = new FileWriter(targetFile)) {
                    IOUtils.copy(entryInputStream, targetTempFileWriter, StandardCharsets.UTF_8);
                }
            }
        }
        validateSelfTestData(wfsForSelfTest, tempSelfTestWorkflowFailureDir);
    }

    private void validateFileInSelfTestDirectory(String selfTestFileName) throws IOException {
        if (!selfTestFileName.endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
            && !selfTestFileName.endsWith(HeadlessWorkflowExecutionVerification.FILE_SUFFIX_EXPECTED_LOG)
            && !selfTestFileName.endsWith(HeadlessWorkflowExecutionVerification.FILE_SUFFIX_PROHIBITED_LOG)) {
            throw new IOException("Unexpected file in self-test directory: " + selfTestFileName);
        }

    }

    private void validateSelfTestData(List<String> wfsForSelfTestNotUsed, File tempSelfTestWorkflowFailureDir) throws IOException {
        if (!wfsForSelfTestNotUsed.isEmpty()) {
            throw new IOException("A test case contains workflow file(s) that do(es)n't exist: " + wfsForSelfTestNotUsed);
        }
        validateExpectedLogExistsForWorkflowsExpectedToFail(tempSelfTestWorkflowFailureDir);
    }

    private void validateExpectedLogExistsForWorkflowsExpectedToFail(File tempSelfTestWorkflowFailureDir) throws IOException {
        for (File file : tempSelfTestWorkflowFailureDir.listFiles()) {
            if (file.getName().endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
                && !new File(tempSelfTestWorkflowFailureDir,
                    file.getName().replaceAll(WorkflowConstants.WORKFLOW_FILE_ENDING, "") + ".log.expected").exists()) {
                throw new IOException("File with expected log is missing for workflow expected to fail: " + file);
            }
        }
    }

    private File generatePlaceholdersRelatedFiles(File tempPlaceholdersStuffDir, String pythonPath) throws IOException {

        InputStream placeholdersTemplateInputStream =
            getClass().getResourceAsStream(getSelfTestDirPath() + "placeholders/placeholders_template.json");

        final int maxChars = 1000;
        Random radom = new Random();

        File placeholdersFile = new File(tempPlaceholdersStuffDir, "placeholders.json");

        File testInputFile = new File(tempPlaceholdersStuffDir, "test-input.text");
        FileUtils.write(testInputFile, RandomStringUtils.random(radom.nextInt(maxChars)));

        File testInputDir = new File(tempPlaceholdersStuffDir, "test-input-dir");
        testInputDir.mkdir();
        FileUtils.write(new File(testInputDir, "test-file-1"), RandomStringUtils.random(radom.nextInt(maxChars)));
        FileUtils.write(new File(testInputDir, "test-file-2"), RandomStringUtils.random(radom.nextInt(maxChars)));
        FileUtils.write(new File(testInputDir, "test-file-3"), RandomStringUtils.random(radom.nextInt(maxChars)));

        File testTargetRootDir = new File(tempPlaceholdersStuffDir, "test-target-dir");
        testTargetRootDir.mkdir();

        File testMemFile1 = new File(tempPlaceholdersStuffDir, "test-mem-1");
        File testMemFile2 = new File(tempPlaceholdersStuffDir, "test-mem-2");
        File testMemFile3 = new File(tempPlaceholdersStuffDir, "test-mem-3");

        FileUtils.write(placeholdersFile, StringUtils.format(IOUtils.toString(placeholdersTemplateInputStream),
            pythonPath, testInputFile.getAbsolutePath().replaceAll(ESCAPED_BACKSLASH, SLASH),
            testInputDir.getAbsolutePath().replaceAll(ESCAPED_BACKSLASH, SLASH),
            testTargetRootDir.getAbsolutePath().replaceAll(ESCAPED_BACKSLASH, SLASH),
            testMemFile1.getAbsolutePath().replaceAll(ESCAPED_BACKSLASH, SLASH),
            testMemFile2.getAbsolutePath().replaceAll(ESCAPED_BACKSLASH, SLASH),
            testMemFile3.getAbsolutePath().replaceAll(ESCAPED_BACKSLASH, SLASH)));

        return placeholdersFile;
    }

    private List<File> parseWfVerifyCommand(final CommandContext context) throws CommandException {
        // "wf verify [-pr <parallel runs>] [-sr <sequential runs>] [-p <JSON placeholder file>]
        // [--basedir <dir>]
        // <filename> [<filename> ...]"
        // TODO replace File with a custom class when more parameters are needed - misc_ro
        List<File> wfFiles = new ArrayList<>();

        String lastBaseDirOption = null;
        File lastBaseDir = null;

        String token;
        while ((token = context.consumeNextToken()) != null) {
            if (BASEDIR_OPTION.equals(token)) {
                lastBaseDirOption = context.consumeNextToken();
                if (lastBaseDirOption == null) {
                    throw CommandException.syntaxError("--basedir option specified without a value", context);
                }
                lastBaseDir = new File(lastBaseDirOption);
                // validate
                if (!lastBaseDir.isDirectory()) {
                    throw CommandException.executionError("Specified --basedir is not a valid directory: "
                        + lastBaseDir.getAbsolutePath(), context);
                }
            } else if (ASTERISK.equals(token)) {
                if (lastBaseDir == null) {
                    throw CommandException.executionError("The \"*\" wildcard requires a previous --basedir", context);
                }
                int count = 0;
                for (String filename : lastBaseDir.list()) {
                    if (filename.endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
                        && !filename.endsWith(WorkflowConstants.WORKFLOW_FILE_BACKUP_SUFFIX + WorkflowConstants.WORKFLOW_FILE_ENDING)) {
                        final File wfFile = new File(lastBaseDir, filename);
                        checkWfFileExists(context, wfFile);
                        wfFiles.add(wfFile);
                        count++;
                    }
                }
                context.println("Added " + count + " non-backup workflow file(s) from " + lastBaseDir.getAbsolutePath());
            } else {
                // no option -> filename
                final File wfFile;
                if (lastBaseDir != null) {
                    wfFile = new File(lastBaseDir, token);
                } else {
                    wfFile = new File(token);
                }
                checkWfFileExists(context, wfFile);
                wfFiles.add(wfFile);
            }
        }
        return wfFiles;
    }

    private Integer readOptionalParallelRunsParameter(CommandContext context) throws CommandException {
        return readOptionalRunsParameter(context, "--pr");
    }

    private Integer readOptionalSequentialRunsParameter(CommandContext context) throws CommandException {
        return readOptionalRunsParameter(context, "--sr");
    }

    private Integer readOptionalRunsParameter(CommandContext context, String parameter) throws CommandException {
        int numberOfRuns = 1; // default (parameter is optional)
        if (context.consumeNextTokenIfEquals(parameter)) {
            String number = context.consumeNextToken();
            if (number == null) {
                throw CommandException.syntaxError("Missing number of runs", context);
            }
            try {
                numberOfRuns = Integer.parseInt(number);
            } catch (NumberFormatException e) {
                throw CommandException.executionError(e.getMessage(), context);
            }
        }
        return numberOfRuns;
    }

    private HeadlessWorkflowExecutionService.DisposalBehavior readOptionalDisposeParameter(CommandContext context)
        throws CommandException {
        if (context.consumeNextTokenIfEquals("--dispose")) {
            String dispose = context.consumeNextToken();
            try {
                if (HeadlessWorkflowExecutionService.DisposalBehavior.Always.name().toLowerCase().equals(dispose)) {
                    return HeadlessWorkflowExecutionService.DisposalBehavior.Always;
                } else if (HeadlessWorkflowExecutionService.DisposalBehavior.Never.name().toLowerCase().equals(dispose)) {
                    return HeadlessWorkflowExecutionService.DisposalBehavior.Never;
                } else if ("onfinish".equals(dispose)
                    || HeadlessWorkflowExecutionService.DisposalBehavior.OnExpected.name().toLowerCase().equals(dispose)) {
                    return HeadlessWorkflowExecutionService.DisposalBehavior.OnExpected;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw CommandException.syntaxError("Invalid dispose behavior: " + dispose, context);
            }
        }
        return HeadlessWorkflowExecutionService.DisposalBehavior.OnExpected;
    }

    private String getPythonPathParameter(CommandContext context) throws CommandException {
        if (context.consumeNextTokenIfEquals("--python")) {
            return context.consumeNextToken().replaceAll(ESCAPED_BACKSLASH, SLASH);
        }
        return "python";
    }

    private String[] getCasesParameter(CommandContext context) throws CommandException {
        if (context.consumeNextTokenIfEquals("--cases")) {
            return context.consumeNextToken().split(",");
        }
        return new String[] { "core" };
    }

    private HeadlessWorkflowExecutionService.DeletionBehavior readOptionalDeleteParameter(CommandContext context) throws CommandException {
        if (context.consumeNextTokenIfEquals("--delete")) {
            String delete = context.consumeNextToken();
            try {
                if (HeadlessWorkflowExecutionService.DeletionBehavior.Always.name().toLowerCase().equals(delete)) {
                    return HeadlessWorkflowExecutionService.DeletionBehavior.Always;
                } else if (HeadlessWorkflowExecutionService.DeletionBehavior.Never.name().toLowerCase().equals(delete)) {
                    return HeadlessWorkflowExecutionService.DeletionBehavior.Never;
                } else if ("onfinish".equals(delete)
                    || HeadlessWorkflowExecutionService.DisposalBehavior.OnExpected.name().toLowerCase().equals(delete)) {
                    return HeadlessWorkflowExecutionService.DeletionBehavior.OnExpected;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw CommandException.syntaxError("Invalid delete behavior: " + delete, context);
            }
        }
        return HeadlessWorkflowExecutionService.DeletionBehavior.OnExpected;
    }

    private File readOptionalPlaceholdersFileParameter(CommandContext context) throws CommandException {
        File placeholdersFile = null; // optional
        if (context.consumeNextTokenIfEquals("-p")) {
            String placeholdersFilename = context.consumeNextToken();
            if (placeholdersFilename == null) {
                throw CommandException.syntaxError("Missing placeholder filename", context);
            }
            try {
                placeholdersFile =
                    WorkflowExecutionUtils.resolveWorkflowOrPlaceholderFileLocation(placeholdersFilename,
                        WorkflowExecutionUtils.DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_PLACEHOLDER_FILE);
            } catch (FileNotFoundException e) {
                throw CommandException.executionError(e.getMessage(), context);
            }
        }
        return placeholdersFile;
    }

    private void checkWfFileExists(final CommandContext context, final File wfFile) throws CommandException {
        if (!wfFile.isFile()) {
            throw CommandException.executionError("Specified workflow file does not exist: " + wfFile.getAbsolutePath(), context);
        }
    }

    private HeadlessWorkflowExecutionVerificationResult executeWfVerifySetup(final CommandContext context, List<File> wfFiles,
        File placeholdersFile,
        int parallelRuns, int sequentialRuns, final HeadlessWorkflowExecutionService.DisposalBehavior dispose,
        final HeadlessWorkflowExecutionService.DeletionBehavior delete) throws CommandException {
        Date startTime = new Date();
        if (wfFiles.isEmpty()) {
            throw CommandException.syntaxError("Error: at least one workflow file must be specified", context);
        }
        final HeadlessWorkflowExecutionVerificationRecorder wfVerificationResultRecorder;
        try {
            wfVerificationResultRecorder =
                HeadlessWorkflowExecutionVerification.createAndInitializeInstance(wfFiles, parallelRuns, sequentialRuns);
        } catch (IOException e) {
            throw CommandException.executionError("Failed to initialze expected workflow behavior: " + e.getMessage(), context);
        }
        for (int j = 0; j < sequentialRuns; j++) {
            Set<HeadlessWorkflowExecutionContext> headlessWfExeContexts = new HashSet<>();
            for (int i = 0; i < parallelRuns; i++) {
                for (final File wfFile : wfFiles) {
                    if (preValidateWorkflow(context, wfFile)) {
                        try {
                            // TODO specify log directory?
                            HeadlessWorkflowExecutionContextBuilder exeContextBuilder =
                                new HeadlessWorkflowExecutionContextBuilder(wfFile, setupLogDirectoryForWfFile(wfFile));
                            exeContextBuilder.setPlaceholdersFile(placeholdersFile);
                            exeContextBuilder.setTextOutputReceiver(context.getOutputReceiver());
                            exeContextBuilder.setDisposalBehavior(dispose);
                            exeContextBuilder.setDeletionBehavior(delete);
                            headlessWfExeContexts.add(exeContextBuilder.build());
                        } catch (WorkflowExecutionException | InvalidFilenameException e) {
                            wfVerificationResultRecorder.addWorkflowError(wfFile, e.getMessage());
                        }
                    }
                }
            }
            workflowExecutionService.executeWorkflowsAndVerify(headlessWfExeContexts, wfVerificationResultRecorder);
        }
        wfVerificationResultRecorder.setStartAndEndTime(startTime, new Date());
        return (HeadlessWorkflowExecutionVerificationResult) wfVerificationResultRecorder;
    }

    private File setupLogDirectoryForWfFile(File wfFile) throws WorkflowExecutionException {

        if (!wfFile.isFile()) {
            throw new WorkflowExecutionException("The workflow file \"" + wfFile.getAbsolutePath()
                + "\" does not exist or it cannot be opened");
        }

        File parentDir = wfFile.getParentFile();
        // sanity check
        if (!parentDir.isDirectory()) {
            throw new WorkflowExecutionException("Consistency error: parent directory is not a directory: " + parentDir.getAbsolutePath());
        }

        long millis = new GregorianCalendar().getTimeInMillis();

        String folderName = wfFile.getName();
        if (folderName.contains(STRING_DOT)) {
            folderName = folderName.substring(0, folderName.lastIndexOf(STRING_DOT));
        }

        // make the last two digits sequentially increasing to reduce the likelihood of timestamp collisions
        // TODO >5.0.0: crude fix for #10436 - align better with generated workflow name - misc_ro
        int suffixNumber = GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER.incrementAndGet() % WORKFLOW_SUFFIX_NUMBER_MODULO;
        // TODO don't use SQL timestamp for formatting; also, use StringUtils.format()
        Timestamp ts = new Timestamp(millis);
        folderName = "logs/" + folderName + "_" + ts.toString().replace('.', '-').replace(' ', '_').replace(':', '-') + "_" + suffixNumber;

        File logDir = new File(parentDir, folderName);
        logDir.mkdirs();
        if (!logDir.isDirectory()) {
            throw new WorkflowExecutionException("Failed to create log directory" + logDir.getAbsolutePath());
        }
        return logDir;
    }

    /**
     * Helper function, detects the workflow information for a given executionId.
     * 
     */
    private WorkflowExecutionInformation getWfExecInfFromExecutionId(String executionId, TextOutputReceiver outputReceiver) {

        WorkflowExecutionInformation wExecInf = null;
        Set<WorkflowExecutionInformation> wis = workflowExecutionService.getWorkflowExecutionInformations();
        for (WorkflowExecutionInformation workflow : wis) {
            if (workflow.getExecutionIdentifier().equals(executionId)) {
                wExecInf = workflow;
                break;
            }
        }
        if (wExecInf == null) {
            outputReceiver.addOutput("Workflow with id '" + executionId + "' not found");
        }
        return wExecInf;
    }

    private void performWfGraph(final CommandContext cmdCtx) throws CommandException {
        final File wfFile = getWorkflowFile(cmdCtx);
        WorkflowDescription wfDesc;
        try {
            wfDesc =
                workflowExecutionService.loadWorkflowDescriptionFromFile(wfFile, new WorkflowDescriptionLoaderCallback() {

                    @Override
                    public void onWorkflowFileParsingPartlyFailed(String backupFilename) {
                        cmdCtx.getOutputReceiver()
                            .addOutput("Workflow partly invalid, some parts are removed; backup file: " + backupFilename);
                    }

                    @Override
                    public void onSilentWorkflowFileUpdated(String message) {
                        cmdCtx.getOutputReceiver().addOutput("Workflow updated (silent update): " + message);
                    }

                    @Override
                    public void onNonSilentWorkflowFileUpdated(String message, String backupFilename) {
                        cmdCtx.getOutputReceiver().addOutput("Workflow updated: " + message + "; backup file: " + backupFilename);
                    }

                    @Override
                    public boolean arePartlyParsedWorkflowConsiderValid() {
                        return false;
                    }
                });
        } catch (WorkflowFileException e) {
            throw CommandException.executionError("Failed to load workflow: " + e.getMessage(), cmdCtx);
        }
        if (WorkflowExecutionUtils.hasMissingWorkflowNode(wfDesc.getWorkflowNodes())) {
            throw CommandException.executionError("Workflow has missing components", cmdCtx);
        }
        try {
            WorkflowGraph workflowGraph = createWorkflowGraph(wfDesc);
            cmdCtx.getOutputReceiver().addOutput(workflowGraph.toDotScript());
        } catch (WorkflowExecutionException e) {
            throw CommandException.executionError("Failed to create workflow graph: " + e.getMessage(), cmdCtx);
        }

    }

    // TODO code partly copied from WorkflowStateMachine; didn't refactor the code directly to not change code of workflow execution during
    // final testing for 8.0; code will be cleaned up with: https://mantis.sc.dlr.de/view.php?id=14847
    private WorkflowGraph createWorkflowGraph(WorkflowDescription workflowDescription) throws WorkflowExecutionException {
        Map<String, WorkflowGraphNode> workflowGraphNodes = new HashMap<>();
        Map<String, Set<WorkflowGraphEdge>> workflowGraphEdges = new HashMap<>();
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            Map<String, String> endpointNames = new HashMap<>();
            Set<String> inputIds = new HashSet<>();
            for (EndpointDescription ep : wn.getInputDescriptionsManager().getEndpointDescriptions()) {
                inputIds.add(ep.getIdentifier());
                endpointNames.put(ep.getIdentifier(), ep.getName());
            }
            Set<String> outputIds = new HashSet<>();
            for (EndpointDescription ep : wn.getOutputDescriptionsManager().getEndpointDescriptions()) {
                outputIds.add(ep.getIdentifier());
                endpointNames.put(ep.getIdentifier(), ep.getName());
            }
            String compExeId = wn.getIdentifier();
            boolean isDriverComp = wn.getComponentDescription().getComponentInstallation().getComponentRevision()
                .getComponentInterface().getIsLoopDriver();
            workflowGraphNodes.put(compExeId, new WorkflowGraphNode(compExeId, inputIds, outputIds, endpointNames,
                isDriverComp, false, wn.getName()));
        }
        for (Connection cn : workflowDescription.getConnections()) {
            WorkflowGraphEdge edge = new WorkflowGraphEdge(cn.getSourceNode().getIdentifier(),
                cn.getOutput().getIdentifier(), cn.getOutput().getEndpointDefinition().getEndpointCharacter(),
                cn.getTargetNode().getIdentifier(), cn.getInput().getIdentifier(),
                cn.getInput().getEndpointDefinition().getEndpointCharacter());
            String edgeKey = WorkflowGraph.createEdgeKey(edge);
            if (!workflowGraphEdges.containsKey(edgeKey)) {
                workflowGraphEdges.put(edgeKey, new HashSet<WorkflowGraphEdge>());
            }
            workflowGraphEdges.get(edgeKey).add(edge);
        }
        return new WorkflowGraph(workflowGraphNodes, workflowGraphEdges);
    }

}
