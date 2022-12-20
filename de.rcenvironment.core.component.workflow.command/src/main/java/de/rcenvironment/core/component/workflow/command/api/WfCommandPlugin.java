/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.command.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.AbstractParsedCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandParser;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.FileParameter;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.ListCommandParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.MultiStateParameter;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedFileParameter;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.ParsedListParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationResults;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationService;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamodel.api.DataModelConstants;
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
 * @author Kathrin Schaffert
 * @author Alexander Weinert
 */
@Component
public class WfCommandPlugin implements CommandPlugin {

	private static final String WF_DETAILS_OUTPUT_TEXT = "        Execution Count: %s,  "
            + "Average Time: %s msec, Max Time: %s msec, Total Time: %s msec";

    private static final String SELF_TEST_CASES_FILE_ENDING = ".cases";

    private static final String SELF_TEST_CASES_DIR = "cases/";

    private static final String SELF_TEST_WORKFLOW_DIR = "workflows/";

    private static final String SLASH = "/";

    private static final String ESCAPED_BACKSLASH = "\\\\";

    private static final String ASTERISK = "*";

    private static final String DELETE_COMMAND = "delete";

    private static final String DISPOSE_COMMAND = "dispose";

    private static final int PARSING_WORKFLOW_FILE_RETRY_INTERVAL = 2000;

    private static final int MAXIMUM_WORKFLOW_PARSE_RETRIES = 5;

    private static final String BASEDIR_OPTION = "--basedir";
    
    private static final String INCLUDEDIRS_OPTION = "--includedirs";

    private static final String STRING_DOT = ".";

    private static final int WORKFLOW_SUFFIX_NUMBER_MODULO = 100;

    private static final String WRONG_STATE_ERROR = "%s workflow not possible in current workflow state: %s";

    private static final String WORKFLOW_ID = "<id>";

    private static final String WF = "wf";
    
    private static final String DELETE = "--delete";
    
    private static final String DISPOSE = "--dispose";
    
    private static final String ONFINISHED = "onfinished";
    
    private static final String PR = "--pr";
    
    private static final String SR = "--sr";
    
    private static final String JSON_FILE = "-p";
    
    private static final String PYTHON = "--python";
    
    private static final String CASES = "--cases";

// Parameters
    
    private static final FileParameter WORKFLOW_FILE_PARAMETER = new FileParameter("workflow file", "path to the workflow file");
    
    private static final StringParameter WORKFLOW_FILE_BASEDIR_PARAMETER = new StringParameter("workflow file", "path to the workflow file", null);
    
    private static final FileParameter JSON_FILE_PARAMETER = new FileParameter("JSON placholder file",
            "path to the JSON placeholder file");

    private static final FileParameter BASEDIR_PARAMETER = new FileParameter("base directory", "path to base directory");
    
    private static final FileParameter INCLUDEDIR_PARAMETER = new FileParameter("include directory", "path to directory to include workflow files");
    
    private static final FileParameter PYTHON_FILE_PARAMETER = new FileParameter("python exe path", "path to python exe");
    
    private static final IntegerParameter PR_PARAMETER = new IntegerParameter(1, "parallel runs", "number of parallel runs");

    private static final IntegerParameter SR_PARAMETER = new IntegerParameter(1, "sequential runs", "number of sequential runs");

    private static final StringParameter ID_PARAMETER = new StringParameter(null, "id", "id of the workflow");
    
    private static final StringParameter CASES_PARAMETER = new StringParameter("", "cases", "parameter for the cases");
    
    private static final ListCommandParameter WORKFLOW_LIST_PARAMETER =
            new ListCommandParameter(WORKFLOW_FILE_PARAMETER, "workflows", "list of workflow files");
    
    private static final ListCommandParameter WORKFLOW_LIST_BASEDIR_PARAMETER =
    		new ListCommandParameter(WORKFLOW_FILE_BASEDIR_PARAMETER, "workflows", "list of workflow files");
    
    private static final ListCommandParameter CASES_LIST_PARAMETER =
            new ListCommandParameter(CASES_PARAMETER, "cases", "cases");
    
    private static final ListCommandParameter INCLUDEDIR_LIST_PARAMETER =
            new ListCommandParameter(INCLUDEDIR_PARAMETER, "include directory", "path to directory to include workflow files");
    
    private static final MultiStateParameter DISPOSE_DELETE_PARAMETR =
            new MultiStateParameter("dispose/delete", "", ONFINISHED,
                    "never", "always");
    
    private static final NamedParameter NAMED_JSON_FILE_PARAMETER = new NamedSingleParameter(
            JSON_FILE, "JSON placeholder file", JSON_FILE_PARAMETER);
    
    private static final NamedParameter NAMED_BASEDIR_PARAMETER = new NamedSingleParameter(
            BASEDIR_OPTION, "optional base directory", BASEDIR_PARAMETER);
    
    private static final NamedParameter NAMED_INCLUDEDIR_LIST_PARAMETER = new NamedSingleParameter(
            INCLUDEDIRS_OPTION, "optional include directorys", INCLUDEDIR_LIST_PARAMETER);
    
    private static final NamedParameter NAMED_PYTHON_PARAMETER = new NamedSingleParameter(
            PYTHON, "path to python.exe", PYTHON_FILE_PARAMETER);
    
    private static final NamedParameter DISPOSE_PARAMETER = new NamedSingleParameter(
            DISPOSE, "dispose behaviour", DISPOSE_DELETE_PARAMETR);
    
    private static final NamedParameter DELETE_PARAMETER = new NamedSingleParameter(
            DELETE, "deletion behaviour", DISPOSE_DELETE_PARAMETR);
    
    private static final NamedParameter NAMED_PR_PARAMETER = new NamedSingleParameter(
            PR, "number of parallel runs", PR_PARAMETER);
    
    private static final NamedParameter NAMED_SR_PARAMETER = new NamedSingleParameter(
            SR, "number of sequential runs", SR_PARAMETER);
    
    private static final NamedParameter NAMED_CASES_PARAMETER = new NamedSingleParameter(
            CASES, "parameter for the cases", CASES_LIST_PARAMETER);
    
    /** Name of directory with workflows that are expected to fail. */
    private static final String FAILURE_DIR_NAME = "failure";

    // TODO >5.0.0: crude fix for #10436 - align better with generated workflow name - misc_ro
    private static final AtomicInteger GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER = new AtomicInteger();

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private WorkflowExecutionDisplayService workflowExecutionDisplayService;

    private WorkflowVerificationService workflowVerificationService;

    private MetaDataService metaDataService;

    private Bundle bundle;

    private final Log log = LogFactory.getLog(getClass());
    
    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(WF, "manage workflows",
            "alias for \"wf list\"", this::performWfList,
            new SubCommandDescription("run", "starts a workflow from the given file and waits for its completion",
            	this::performWfRun,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        WORKFLOW_FILE_PARAMETER
                    },
                    new CommandFlag[] {
                        new CommandFlag("-c", "--compact-output")
                    },
                    new NamedParameter[] {
                        DELETE_PARAMETER,
                        DISPOSE_PARAMETER,
                        NAMED_JSON_FILE_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("start", "starts a workflow from the given file and returns its workflow id if validation passed",
            	this::performWfStart,
            	new CommandModifierInfo(
            		new AbstractCommandParameter[] {
            			WORKFLOW_FILE_PARAMETER
            		},
            		new CommandFlag[] {
            			new CommandFlag("-c", "--compact-output")
            		},
            		new NamedParameter[] {
            			DELETE_PARAMETER,
            			DISPOSE_PARAMETER,
            			NAMED_JSON_FILE_PARAMETER
            		}
            	)
            ),
            new SubCommandDescription("verify", "batch test the specified workflow files", this::performWfVerify,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        WORKFLOW_LIST_BASEDIR_PARAMETER
                    },
                    new NamedParameter[] {
                        DELETE_PARAMETER,
                        DISPOSE_PARAMETER,
                        NAMED_PR_PARAMETER,
                        NAMED_SR_PARAMETER,
                        NAMED_JSON_FILE_PARAMETER,
                        NAMED_BASEDIR_PARAMETER,
                        NAMED_INCLUDEDIR_LIST_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("list", "show workflow list", this::performWfList),
            new SubCommandDescription("details", "show details of a workflow", this::performWfShowDetails,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("open", "open a runtime viewer of a workflow. Requires GUI.", this::performWfOpen,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("pause", "pause a running workflow", this::performWfPause,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("resume", "resume a paused workflow", this::performWfResume,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("cancel", "cancel a running or paused workflow", this::performWfCancel,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription(DELETE_COMMAND, "delete and dispose a finished, cancelled or failed workflow",
                this::performWfDelete,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                 )
            ),
            new SubCommandDescription(DISPOSE_COMMAND, "disposes a finished, cancelled or failed workflow",
                this::performWfDispose,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        ID_PARAMETER
                    }
                )
            ),
            new SubCommandDescription("self-test", "batch test workflow files of the test workflow files bundle",
                this::performWfSelfTest,
                new CommandModifierInfo(
                    new NamedParameter[] {
                        DISPOSE_PARAMETER,
                        DELETE_PARAMETER,
                        NAMED_PR_PARAMETER,
                        NAMED_SR_PARAMETER,
                        NAMED_PYTHON_PARAMETER,
                        NAMED_CASES_PARAMETER
                    }
                ),
                true
            ),
            new SubCommandDescription("list-self-test-cases", "list available test cases for wf self-test",
                this::performWfListSelfTestCases, true),
            new SubCommandDescription("check-self-test-cases",
                "check if all test workflows are part of at least one test case",
                this::performWfCheckSelfTestCases, true),
            new SubCommandDescription("graph",
                "prints .dot string representation of a workflow (can be used to create graph visualization with Graphviz)",
                this::performWfGraph,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        WORKFLOW_FILE_PARAMETER
                    }
                ),
                true
            )
        );
        return new MainCommandDescription[] { commands };
    }

    /**
     * OSGi DS life-cycle method.
     * 
     * @param context {@link ComponentContext} injected
     */
    @Activate
    public void activate(ComponentContext context) {
        bundle = context.getBundleContext().getBundle();
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindWorkflowExecutionService(HeadlessWorkflowExecutionService newInstance) {
        this.workflowExecutionService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    public void bindWorkflowExecutionDisplayService(WorkflowExecutionDisplayService newInstance) {
        this.workflowExecutionDisplayService = newInstance;
    }

    /**
     * OSGi-DS unbind method.
     * 
     * @param oldInstance the old service instance
     */
    public void unbindWorkflowExecutionDisplayService(WorkflowExecutionDisplayService oldInstance) {
        this.workflowExecutionDisplayService = null;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindMetaDataService(MetaDataService newInstance) {
        this.metaDataService = newInstance;
    }

    @Reference
    public void bindWorkflowVerificationService(WorkflowVerificationService service) {
        this.workflowVerificationService = service;
    }
    
    private void performWfRun(CommandContext cmdCtx) throws CommandException {
    	performWfRunOrStart(cmdCtx, true);
    }

    private void performWfStart(CommandContext cmdCtx) throws CommandException {
    	performWfRunOrStart(cmdCtx, false);
    }

    private void performWfRunOrStart(CommandContext cmdCtx, boolean waitForTermination) throws CommandException {
        // "wf run [--dispose <...>] [--compact-output] [-p <JSON placeholder file>] <filename>"

    	ParsedCommandModifiers modifiers = cmdCtx.getParsedModifiers();
        
        ParsedStringParameter disposeParameter = (ParsedStringParameter) modifiers.getCommandParameter(DISPOSE);
        ParsedStringParameter deleteParameter = (ParsedStringParameter) modifiers.getCommandParameter(DELETE);
        ParsedFileParameter placeholdersParameter = (ParsedFileParameter) modifiers.getCommandParameter(JSON_FILE);
        ParsedFileParameter fileParameter = (ParsedFileParameter) modifiers.getPositionalCommandParameter(0);
        boolean hasCompactFlag = modifiers.hasCommandFlag("-c");
        
        HeadlessWorkflowExecutionService.DisposalBehavior dispose = toDisposal(disposeParameter.getResult(), cmdCtx);

        HeadlessWorkflowExecutionService.DeletionBehavior delete = toDeletion(deleteParameter.getResult(), cmdCtx);

        if (workflowVerificationService.preValidateWorkflow(cmdCtx.getOutputReceiver(), fileParameter.getResult(), true)) { // true = always print
                                                                                                         // pre-verification output
            try {
                // TODO specify log directory?
                HeadlessWorkflowExecutionContextBuilder exeContextBuilder =
                    new HeadlessWorkflowExecutionContextBuilder(fileParameter.getResult()).setLogDirectory(setupLogDirectoryForWfFile(fileParameter.getResult()));
                exeContextBuilder.setPlaceholdersFile(placeholdersParameter.getResult());
                exeContextBuilder.setTextOutputReceiver(cmdCtx.getOutputReceiver(), hasCompactFlag);
                exeContextBuilder.setDisposalBehavior(dispose);
                exeContextBuilder.setDeletionBehavior(delete);

                if (waitForTermination) {
                    // spawn and wait for termination
                    workflowExecutionService.executeWorkflow(exeContextBuilder.buildExtended());
                } else {
                    // spawn only
                    WorkflowExecutionInformation execInfo =
                        workflowExecutionService.startHeadlessWorkflowExecution(exeContextBuilder.buildExtended());
                    // print workflow id; only reached if no validation errors occurred
                    cmdCtx.println("Workflow Id: " + execInfo.getWorkflowExecutionHandle().getIdentifier());
                }
            } catch (WorkflowExecutionException | IOException e) {
                log.error("Exception while executing workflow: " + fileParameter.getResult().getAbsolutePath(), e);
                throw CommandException.executionError(ComponentUtils.createErrorLogMessage(e), cmdCtx);
            } catch (InvalidFilenameException e) {
                throw CommandException.executionError(ComponentUtils.createErrorLogMessage(e), cmdCtx);
            }
        } else {
            cmdCtx.getOutputReceiver()
                .addOutput(StringUtils.format("'%s' not executed due to validation errors (see log file for details) (full path: %s)",
                		fileParameter.getResult().getName(), fileParameter.getResult().getAbsolutePath()));
        }
    }
    
    private HeadlessWorkflowExecutionService.DisposalBehavior toDisposal(String token, CommandContext context) throws CommandException {
        
        if (HeadlessWorkflowExecutionService.DisposalBehavior.Always.name().equalsIgnoreCase(token)) {
            return HeadlessWorkflowExecutionService.DisposalBehavior.Always;
        } else if (HeadlessWorkflowExecutionService.DisposalBehavior.Never.name().equalsIgnoreCase(token)) {
            return HeadlessWorkflowExecutionService.DisposalBehavior.Never;
        } else if (ONFINISHED.equalsIgnoreCase(token)) {
            return HeadlessWorkflowExecutionService.DisposalBehavior.OnExpected;
        }
        
        throw CommandException.syntaxError("Invalid disposal behavior: " + token, context);
        
    }
    
    private HeadlessWorkflowExecutionService.DeletionBehavior toDeletion(String token, CommandContext context) throws CommandException {
        
        if (HeadlessWorkflowExecutionService.DeletionBehavior.Always.name().equalsIgnoreCase(token)) {
            return HeadlessWorkflowExecutionService.DeletionBehavior.Always;
        } else if (HeadlessWorkflowExecutionService.DeletionBehavior.Never.name().equalsIgnoreCase(token)) {
            return HeadlessWorkflowExecutionService.DeletionBehavior.Never;
        } else if (ONFINISHED.equalsIgnoreCase(token)) {
            return HeadlessWorkflowExecutionService.DeletionBehavior.OnExpected;
        }
        
        throw CommandException.syntaxError("Invalid delete behavior: " + token, context);
        
    }
    
    private void performWfVerify(final CommandContext context) throws CommandException {
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
    	
    	ParsedListParameter workflowListParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);
    	ParsedFileParameter basedirParameter = (ParsedFileParameter) modifiers.getCommandParameter(BASEDIR_OPTION);
    	ParsedListParameter includedirsListParameter = (ParsedListParameter) modifiers.getCommandParameter(INCLUDEDIRS_OPTION);
        ParsedStringParameter disposeParameter = (ParsedStringParameter) modifiers.getCommandParameter(DISPOSE);
    	ParsedStringParameter deleteParameter = (ParsedStringParameter) modifiers.getCommandParameter(DELETE);
    	ParsedIntegerParameter parallelRunsParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(PR);
    	ParsedIntegerParameter sequentialRunsParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(SR);
    	ParsedFileParameter placeholderFileParameter = (ParsedFileParameter) modifiers.getCommandParameter(JSON_FILE);
        
        HeadlessWorkflowExecutionService.DisposalBehavior dispose = toDisposal(disposeParameter.getResult(), context);
        HeadlessWorkflowExecutionService.DeletionBehavior delete = toDeletion(deleteParameter.getResult(), context);

        List<String> workflowList = workflowListParameter.getResult().stream()
                .map(parameter -> ((ParsedStringParameter) parameter).getResult()).collect(Collectors.toList());
        List<File> includedirsList = includedirsListParameter.getResult().stream()
                .map(parameter -> ((ParsedFileParameter) parameter).getResult()).collect(Collectors.toList());
        
        List<File> wfFiles = collectFiles(context, workflowList, basedirParameter.getResult(), includedirsList);
        
        final Map<Boolean, List<File>> partitionedWfFiles = wfFiles.stream()
                .collect(Collectors.partitioningBy(file -> !file.getParentFile().getName().equals(FAILURE_DIR_NAME)));
        
        final File rootFolder;
        if (!partitionedWfFiles.get(true).isEmpty()) {
            rootFolder = partitionedWfFiles.get(true).get(0).getParentFile();
        } else {
            rootFolder = partitionedWfFiles.get(false).get(0).getParentFile().getParentFile();
        }

        try {
            WorkflowVerificationResults wfVerifyResultVerification = workflowVerificationService.getVerificationBuilder()
                .outputReceiver(context.getOutputReceiver())
                .workflowRootFile(rootFolder)
                .addWorkflowsExpectedToSucceed(partitionedWfFiles.get(true))
                .addWorkflowsExpectedToFail(partitionedWfFiles.get(false))
                .placeholdersFile(placeholderFileParameter.getResult())
                .logFileFactory(this::setupLogDirectoryForWfFile)
                .numberOfParallelRuns(parallelRunsParameter.getResult())
                .numberOfSequentialRuns(sequentialRunsParameter.getResult())
                .disposalBehavior(dispose)
                .deletionBehavior(delete)
                .verify();
            context.println(wfVerifyResultVerification.getVerificationReport());
        } catch (IOException e) {
            throw CommandException.executionError("Failed to initialze expected workflow behavior: " + e.getMessage(), context);
        }
    }
    
//    private List<File> collectFiles(CommandContext context, ParsedListParameter workflowList,
//            ParsedFileParameter basedir, ParsedListParameter includeList) throws CommandException {
//        
//        if (workflowList.getResult().isEmpty() && includeList.getResult().isEmpty()) {
//            throw CommandException.executionError("at least one file has to be specified", context);
//        }
//        
//        if (basedir.getResult() != null && !basedir.getResult().isDirectory()) {
//            throw CommandException.syntaxError("basedir is not a directory", context);
//        }
//        
//        List<File> wfFiles = new ArrayList<>();
//        
//        if (basedir.getResult() == null) {
//            wfFiles.addAll(workflowList.getResult().stream()
//                    .map(parameter -> new File(((ParsedStringParameter) parameter).getResult())).collect(Collectors.toList()));
//        } else {
//            wfFiles = workflowList.getResult().stream()
//                    .map(parameter -> new File(basedir.getResult(), ((ParsedStringParameter) parameter).getResult()))
//                    .collect(Collectors.toList());
//        }
//        
//        for (AbstractParsedCommandParameter includeDir : includeList.getResult()) {
//            File dir = ((ParsedFileParameter) includeDir).getResult();
//            
//            if (dir.isDirectory()) {
//                for (String filename : dir.list()) {
//                    if (filename.endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
//                        && !filename.endsWith(WorkflowConstants.WORKFLOW_FILE_BACKUP_SUFFIX + WorkflowConstants.WORKFLOW_FILE_ENDING)) {
//                        final File wfFile = new File(dir, filename);
//                        checkWfFileExists(context, wfFile);
//                        wfFiles.add(wfFile);
//                    }
//                }
//            } else {
//                throw CommandException.syntaxError("includedir " + dir + " is not a directory", context);
//            }
//        }
//        
//        return wfFiles;
//    }

    private List<File> collectFiles(CommandContext context, List<String> workflowList,
            File basedir, List<File> includeList) throws CommandException {
        
        List<File> wfFiles = new ArrayList<>();
        
        if (basedir == null) {
            wfFiles.addAll(workflowList.stream()
                    .map(File::new).collect(Collectors.toList()));
        } else {
            wfFiles = workflowList.stream()
                    .map(filename -> new File(basedir, filename))
                    .collect(Collectors.toList());
        }
        
        for (File includeDir : includeList) {
            if (includeDir.isDirectory()) {
                for (String filename : includeDir.list()) {
                    if (filename.endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
                        && !filename.endsWith(WorkflowConstants.WORKFLOW_FILE_BACKUP_SUFFIX + WorkflowConstants.WORKFLOW_FILE_ENDING)) {
                        final File wfFile = new File(includeDir, filename);
                        checkWfFileExists(context, wfFile);
                        wfFiles.add(wfFile);
                    }
                }
            } else {
                throw CommandException.syntaxError("includedir " + includeDir + " is not a directory", context);
            }
        }
        
        return wfFiles;
    }
    
    private void performWfPause(final CommandContext context) {
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        WorkflowExecutionInformation wfExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wfExecInf != null) {
            // Find the node running this workflow
            try {
                if (wfExecInf.getWorkflowState().equals(WorkflowState.RUNNING)) {
                    workflowExecutionService.pause(wfExecInf.getWorkflowExecutionHandle());
                } else {
                    outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Pausing", wfExecInf.getWorkflowState()));
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to pause workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfResume(final CommandContext context) {
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf != null) {
            try {
                if (wExecInf.getWorkflowState().equals(WorkflowState.PAUSED)) {
                    workflowExecutionService.resume(wExecInf.getWorkflowExecutionHandle());
                } else {
                    outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Resuming", wExecInf.getWorkflowState()));
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to resume workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfCancel(final CommandContext context) {
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
        TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf != null) {
            try {
                if (wExecInf.getWorkflowState().equals(WorkflowState.RUNNING)
                    || wExecInf.getWorkflowState().equals(WorkflowState.PAUSED)) {
                    workflowExecutionService.cancel(wExecInf.getWorkflowExecutionHandle());
                } else {
                    outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Canceling", wExecInf.getWorkflowState()));
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to cancel workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }
    
    private void performWfDispose(final CommandContext context) {
    	performWfDisposeOrDelete(context, true);
    }
    
    private void performWfDelete(final CommandContext context) {
    	performWfDisposeOrDelete(context, false);
    }
    
    private void performWfDisposeOrDelete(final CommandContext context, boolean delete) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
    	TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf != null) {
            try {
                if (WorkflowConstants.FINAL_WORKFLOW_STATES.contains(wExecInf.getWorkflowState())) {
                    if (!delete) {
                        workflowExecutionService.deleteFromDataManagement(wExecInf.getWorkflowExecutionHandle());
                    }
                    workflowExecutionService.dispose(wExecInf.getWorkflowExecutionHandle());

                } else {
                    if (delete) {
                        outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Deleting", wExecInf.getWorkflowState()));
                    } else {
                        outputReceiver.addOutput(StringUtils.format(WRONG_STATE_ERROR, "Disposing", wExecInf.getWorkflowState()));
                    }
                }
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.error(StringUtils.format("Failed to dispose workflow '%s'; cause: %s", executionId, e.toString()));
            }
        }
    }

    private void performWfList(final CommandContext context) {

        TextOutputReceiver outputReceiver = context.getOutputReceiver();
        outputReceiver.addOutput("Fetching workflows...");
        List<WorkflowExecutionInformation> wfInfos = new ArrayList<>(workflowExecutionService.getWorkflowExecutionInformations(true));
        Collections.sort(wfInfos);
        StringBuilder outputBuilder = new StringBuilder();
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
            outputBuilder.append(StringUtils.format(" '%s' - %s [%s]\n", wfInfo.getInstanceName(), state, wfInfo.getExecutionIdentifier()));
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
        outputBuilder.append(
            StringUtils.format(" -- TOTAL COUNT: %d workflow(s): %d running, %d paused, %d finished,"
                + " %d cancelled, %d failed, %d verification failed, %d other -- ",
                total, running, paused, finished, cancelled, failed, resultsRejected, other));
        outputReceiver.addOutput(outputBuilder.toString());
    }

    private void performWfShowDetails(final CommandContext context) throws CommandException {
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
    	
        final String executionId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        final TextOutputReceiver outputReceiver = context.getOutputReceiver();
        final WorkflowExecutionInformation wExecInf = getWfExecInfFromExecutionId(executionId, outputReceiver);

        if (wExecInf == null) {
            return;
            // TODO use command exception?
        }

        final Long wfDataManagementId = wExecInf.getWorkflowDataManagementId();
        LogicalNodeId wfControllerNodeId = wExecInf.getNodeId();

        final List<ComponentExecutionInformation> compExecInfos =
            new ArrayList<>(wExecInf.getComponentExecutionInformations());
        Collections.sort(compExecInfos, (p1, p2) -> p1.getInstanceName().compareTo(p2.getInstanceName()));

        final WorkflowRun workflowRunData;
        try {
            workflowRunData = metaDataService.getWorkflowRun(wfDataManagementId, wfControllerNodeId);
        } catch (CommunicationException e) {
            throw CommandException.executionError(
                StringUtils.format("Failed to fetch run data of workflow #%s from %s", wfDataManagementId, wfControllerNodeId), context);
        }

        if (workflowRunData == null) {
            throw CommandException.executionError(
                StringUtils.format("No run data found for workflow #%s from %s. Maybe data have been already deleted.", wfDataManagementId,
                    wfControllerNodeId),
                context);
        }

        final Map<ComponentInstance, Set<ComponentRun>> componentRunsByComponentInstance = workflowRunData.getComponentRuns();

        final SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd  HH:mm:ss");
        if (wExecInf != null) {
            outputReceiver.addOutput("Name: " + wExecInf.getInstanceName());
            outputReceiver.addOutput("Status: " + wExecInf.getWorkflowState());
            outputReceiver.addOutput("Controller: " + wExecInf.getWorkflowDescription().getControllerNode().getAssociatedDisplayName());
            outputReceiver.addOutput("Start: " + df.format(wExecInf.getStartTime()));
            outputReceiver.addOutput("Started from: " + wExecInf.getNodeIdStartedExecution().getAssociatedDisplayName());
            String additional = wExecInf.getAdditionalInformationProvidedAtStart();
            if (additional != null) {
                outputReceiver.addOutput("Additional Information: ");
                outputReceiver.addOutput(additional);
            }
            // outputReceiver.addOutput("Execution Identifier: " +
            // workflow.getExecutionIdentifier());

            outputReceiver.addOutput("Components: ");

            for (ComponentExecutionInformation compExecInfo : compExecInfos) {
                Entry<ComponentInstance, Set<ComponentRun>> entry = getComponentRuns(compExecInfo, componentRunsByComponentInstance);
                if (entry == null) {
                    outputReceiver.addOutput(StringUtils.format(
                        "    %s: No run yet. Execution Count: 0", compExecInfo.getInstanceName()));
                    continue;
                }

                final ComponentInstance componentInstance = entry.getKey();
                final SortedSet<ComponentRun> componentRuns = new TreeSet<>(entry.getValue());

                // if there is an init run, this run will be ignored for calculation
                if (componentRuns.first().getRunCounter() == DataModelConstants.INIT_RUN) {
                    componentRuns.remove(componentRuns.first());
                }
                // if there is a tear down run, this run will be ignored for calculation
                if (componentRuns.last().getRunCounter() == DataModelConstants.TEAR_DOWN_RUN) {
                    componentRuns.remove(componentRuns.last());
                }

                if (componentRuns.isEmpty()) {
                    outputReceiver.addOutput(StringUtils.format(
                        "    %s: No run yet. Execution Count: 0", compExecInfo.getInstanceName()));
                    continue;
                }

                long sum = 0;
                long max = 0;
                for (ComponentRun cRun : componentRuns) {
                    if (componentRuns.last().getEndTime() == null) {
                        continue;
                    }
                    long diff = cRun.getEndTime() - cRun.getStartTime();
                    sum += diff;
                    if (diff > max) {
                        max = diff;
                    }
                }
                int average = (int) (sum / componentRuns.size());

                final String componentInstanceName = componentInstance.getComponentInstanceName();

                if (componentRuns.last().getEndTime() != null) {
                    final String endTime = df.format(componentRuns.last().getEndTime());
                    final String message;

                    if (componentInstance.getFinalState() == null) {
                        message = StringUtils.format(
                            "    %s: Waiting for Input since %s", componentInstanceName,
                            endTime);
                    } else {
                        message = StringUtils.format(
                            "    %s: %s at %s", componentInstanceName, componentInstance.getFinalState(),
                            endTime);
                    }
                    outputReceiver.addOutput(message);
                } else {
                    outputReceiver.addOutput(StringUtils.format(
                        "    %s: started at %s", componentInstanceName,
                        df.format(componentRuns.first().getStartTime())));
                    outputReceiver.addOutput(StringUtils.format(
                        "        Current Execution started at %s",
                        df.format(componentRuns.last().getStartTime())));
                }
                outputReceiver.addOutput(StringUtils.format(
                    WF_DETAILS_OUTPUT_TEXT, componentRuns.last().getRunCounter(), average, max, sum));
            }
        }
    }

    /**
     * 
     * @param compExecInfo must not be null
     * @param componentRunsByComponentInstance must not be null
     * @return Set of Component Runs for the given Component or null if component has not run
     */
    private Entry<ComponentInstance, Set<ComponentRun>> getComponentRuns(ComponentExecutionInformation compExecInfo,
        Map<ComponentInstance, Set<ComponentRun>> componentRunsByComponentInstance) {
        for (Entry<ComponentInstance, Set<ComponentRun>> entry : componentRunsByComponentInstance.entrySet()) {
            final ComponentInstance componentInstance = entry.getKey();
            if (componentInstance.getComponentInstanceName().equals(compExecInfo.getInstanceName())) {
                // entry.getValue() is never null and never empty due to invariant on workflowRunData.getComponentRuns()
                return entry;
            }
        }
        return null;
    }

    private void performWfOpen(CommandContext context) {
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
        final TextOutputReceiver outputReceiver = context.getOutputReceiver();

        final String executionId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        // Defensive copy in line with best practice for OSGI
        final WorkflowExecutionDisplayService displayService = workflowExecutionDisplayService;

        if (displayService == null || !displayService.hasGui()) {
            outputReceiver.addOutput("Could not display workflow execution, as no GUI is present.");
            return;
        }

        final WorkflowExecutionInformation wfExecInfo = getWfExecInfFromExecutionId(executionId, outputReceiver);
        if (wfExecInfo == null) {
            // An error has already been logged to the outputReceiver by getWfExecInfFromExecutionId in this case, hence we can simply
            // return
            return;
        }

        displayService.displayWorkflowExecution(wfExecInfo);
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
            bundle.findEntries(getSelfTestDirPath() + SELF_TEST_WORKFLOW_DIR + FAILURE_DIR_NAME,
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
    	ParsedCommandModifiers modifiers = context.getParsedModifiers();
    	
    	ParsedStringParameter disposeParameter = (ParsedStringParameter) modifiers.getCommandParameter(DISPOSE);
    	ParsedStringParameter deleteParameter = (ParsedStringParameter) modifiers.getCommandParameter(DELETE);
    	ParsedIntegerParameter parallelRunsParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(PR);
    	ParsedIntegerParameter sequentialRunsParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(SR);
    	ParsedListParameter casesParameter = (ParsedListParameter) modifiers.getCommandParameter(CASES);
    	
        HeadlessWorkflowExecutionService.DisposalBehavior dispose = toDisposal(disposeParameter.getResult(), context);
        HeadlessWorkflowExecutionService.DeletionBehavior delete = toDeletion(deleteParameter.getResult(), context);

        int parallelRuns = parallelRunsParameter.getResult();
        int sequentialRuns = sequentialRunsParameter.getResult();

        List<String> cases = casesParameter.getResult()
        		.stream().map(parameter -> ((ParsedStringParameter) parameter).getResult()).collect(Collectors.toList());
        
        if (cases.isEmpty()) {
            cases = Collections.singletonList("core");
        }
        
        String pythonPath = null;
        TempFileService tempFileService = TempFileServiceAccess.getInstance();

        File tempSelfTestWorkflowDir = null;
        File tempSelfTestWorkflowFailureDir = null;

        File tempPlaceholdersStuffDir = null;
        File placeholdersFile = null;
        
        try {
            tempSelfTestWorkflowDir = tempFileService.createManagedTempDir();
            tempSelfTestWorkflowFailureDir = new File(tempSelfTestWorkflowDir, FAILURE_DIR_NAME);
            tempPlaceholdersStuffDir = tempFileService.createManagedTempDir();
        } catch (IOException e) {
            String message = "Failed to create temp directory required for self-test workflow execution";
            handleWfSelfTestExecutionError(context, tempFileService, tempSelfTestWorkflowDir, tempPlaceholdersStuffDir, e, message);
        }

        if (cases.contains("with-python")) {
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

        List<File> includedirs = new ArrayList<>();
        includedirs.add(tempSelfTestWorkflowDir);
        if (tempSelfTestWorkflowFailureDir.exists()) {
            includedirs.add(tempSelfTestWorkflowFailureDir);
        }
        
        final List<File> workflowFiles = collectFiles(context, Collections.emptyList(), null, includedirs);
        if (workflowFiles.isEmpty()) {
            throw CommandException.syntaxError("at least one workflow file must be specified", context);
        }

        final Map<Boolean, List<File>> partitionedWfFiles = workflowFiles.stream()
            .collect(Collectors.partitioningBy(file -> !file.getParentFile().getName().equals(FAILURE_DIR_NAME)));
        final File rootFolder;
        if (!partitionedWfFiles.get(true).isEmpty()) {
            rootFolder = partitionedWfFiles.get(true).get(0).getParentFile();
        } else {
            rootFolder = partitionedWfFiles.get(false).get(0).getParentFile().getParentFile();
        }

        try {
            WorkflowVerificationResults wfVerifyResultVerification = workflowVerificationService.getVerificationBuilder()
                .outputReceiver(context.getOutputReceiver())
                .workflowRootFile(rootFolder)
                .addWorkflowsExpectedToSucceed(partitionedWfFiles.get(true))
                .addWorkflowsExpectedToFail(partitionedWfFiles.get(false))
                .placeholdersFile(placeholdersFile)
                .logFileFactory(this::setupLogDirectoryForWfFile)
                .numberOfParallelRuns(parallelRuns)
                .numberOfSequentialRuns(sequentialRuns)
                .disposalBehavior(dispose)
                .deletionBehavior(delete)
                .verify();
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
        } catch (IOException e) {
            throw CommandException.executionError("Failed to initialze expected workflow behavior: " + e.getMessage(), context);
        }
    }

    private String getAndCheckPythonPath(CommandContext context, File workDir) throws CommandException, IOException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
    	ParsedFileParameter pythonPathParameter = (ParsedFileParameter) modifiers.getCommandParameter(PYTHON);
        String pythonPath = null;
    	
        if (pythonPathParameter.getResult() != null) {
            pythonPath = pythonPathParameter.getResult().getAbsolutePath();
        } else {
            pythonPath = "python";
        }

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
        log.error(message, e);
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

    private List<String> getWorkflowsForSelfTest(List<String> cases) throws IOException {
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
        File tempSelfTestWorkflowFailureDir, List<String> cases) throws IOException {

        // always create the target "failure" sub-directory in advance; it was created on demand in previous releases, but Equinox platform
        // enumeration order changes made this unreliable (issue #16090)
        tempSelfTestWorkflowFailureDir.mkdir();

        List<String> wfsForSelfTest = getWorkflowsForSelfTest(cases);
        @SuppressWarnings("rawtypes") final Enumeration selfTestFolderEntries =
            bundle.findEntries(getSelfTestDirPath() + SELF_TEST_WORKFLOW_DIR, ASTERISK, true);
        while (selfTestFolderEntries.hasMoreElements()) {
            final URL entryURL = (URL) selfTestFolderEntries.nextElement();
            final String entryRawPath = entryURL.getPath();
            if (entryRawPath.endsWith(SLASH)) {
                // accept the "failure" test case sub-directory, but abort on any other directory
                if (!entryRawPath.endsWith(FAILURE_DIR_NAME + SLASH)) {
                    throw new IOException("Unexpected directory in self-test directory: " + entryRawPath);
                }
            } else {
                String[] pathParts = entryRawPath.split(SLASH);
                String selfTestFileName = pathParts[pathParts.length - 1];
                validateFileInSelfTestDirectory(selfTestFileName);
                String selfTestFileNameWithoutEnding = selfTestFileName.replace(WorkflowConstants.WORKFLOW_FILE_ENDING, "");
                // TODO this will never match on .log.expected files, so depending on the other code, those are always or never copied
                if (selfTestFileName.endsWith(WorkflowConstants.WORKFLOW_FILE_ENDING)
                    && !wfsForSelfTest.contains(selfTestFileNameWithoutEnding)) {
                    continue;
                }
                wfsForSelfTest.remove(selfTestFileNameWithoutEnding);
                File targetFile;
                if (pathParts.length > 1
                    && pathParts[pathParts.length - 2].equals(FAILURE_DIR_NAME)) {
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
            && !selfTestFileName.endsWith(".log.expected")
            && !selfTestFileName.endsWith(".log.prohibited")) {
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

    private void checkWfFileExists(final CommandContext context, final File wfFile) throws CommandException {
        if (!wfFile.isFile()) {
            throw CommandException.executionError("Specified workflow file does not exist: " + wfFile.getAbsolutePath(), context);
        }
    }

    private File setupLogDirectoryForWfFile(File wfFile) throws IOException {

        if (!wfFile.isFile()) {
            throw new IOException("The workflow file \"" + wfFile.getAbsolutePath()
                + "\" does not exist or it cannot be opened");
        }

        File parentDir = wfFile.getParentFile();
        // sanity check
        if (!parentDir.isDirectory()) {
            throw new IOException("Consistency error: parent directory is not a directory: " + parentDir.getAbsolutePath());
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
            throw new IOException("Failed to create log directory" + logDir.getAbsolutePath());
        }
        return logDir;
    }

    /**
     * Helper function, detects the workflow information for a given executionId. If no workflow information exists for the given
     * executionId, an error is logged to the given outputReceiver and null is returned.
     * 
     * @return The WorkflowExecutionInformation for the given executionId, or null, if none exists.
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
        ParsedCommandModifiers modifiers = cmdCtx.getParsedModifiers();
    	
    	final File wfFile = ((ParsedFileParameter) modifiers.getPositionalCommandParameter(0)).getResult();
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
            if (workflowGraph != null) {
                cmdCtx.getOutputReceiver().addOutput(workflowGraph.toDotScript());
            } else {
                throw CommandException.executionError(
                    "The wf graph command is not implemented yet. See " + getClass().getTypeName() + " for more informations.",
                    cmdCtx);
            }
        } catch (WorkflowExecutionException e) {
            throw CommandException.executionError("Failed to create workflow graph: " + e.getMessage(), cmdCtx);
        }

    }

    // TODO code partly copied from WorkflowStateMachine; didn't refactor the code directly to not change code of workflow execution during
    // final testing for 8.0; code will be cleaned up with: https://mantis.sc.dlr.de/view.php?id=14847
    private WorkflowGraph createWorkflowGraph(WorkflowDescription workflowDescription) throws WorkflowExecutionException {
        return null; // TODO

        // Map<String, WorkflowGraphNode> workflowGraphNodes = new HashMap<>();
        // Map<String, Set<WorkflowGraphEdge>> workflowGraphEdges = new HashMap<>();
        // for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
        // Map<String, String> endpointNames = new HashMap<>();
        // Set<String> inputIds = new HashSet<>();
        // for (EndpointDescription ep : wn.getInputDescriptionsManager().getEndpointDescriptions()) {
        // inputIds.add(ep.getIdentifier());
        // endpointNames.put(ep.getIdentifier(), ep.getName());
        // }
        // Set<String> outputIds = new HashSet<>();
        // for (EndpointDescription ep : wn.getOutputDescriptionsManager().getEndpointDescriptions()) {
        // outputIds.add(ep.getIdentifier());
        // endpointNames.put(ep.getIdentifier(), ep.getName());
        // }
        // String compExeId = wn.getIdentifier();
        // boolean isDriverComp = wn.getComponentDescription().getComponentInstallation().getComponentInterface().getIsLoopDriver();
        // workflowGraphNodes.put(compExeId, new WorkflowGraphNode(compExeId, inputIds, outputIds, endpointNames,
        // isDriverComp, false, wn.getName()));
        // }
        // for (Connection cn : workflowDescription.getConnections()) {
        // WorkflowGraphEdge edge = new WorkflowGraphEdge(cn.getSourceNode().getIdentifier(),
        // cn.getOutput().getIdentifier(), cn.getOutput().getEndpointDefinition().getEndpointCharacter(),
        // cn.getTargetNode().getIdentifier(), cn.getInput().getIdentifier(),
        // cn.getInput().getEndpointDefinition().getEndpointCharacter());
        // String edgeKey = WorkflowGraph.createEdgeKey(edge);
        // if (!workflowGraphEdges.containsKey(edgeKey)) {
        // workflowGraphEdges.put(edgeKey, new HashSet<WorkflowGraphEdge>());
        // }
        // workflowGraphEdges.get(edgeKey).add(edge);
        // }
        // return new WorkflowGraph(workflowGraphNodes, workflowGraphEdges);
    }

}
