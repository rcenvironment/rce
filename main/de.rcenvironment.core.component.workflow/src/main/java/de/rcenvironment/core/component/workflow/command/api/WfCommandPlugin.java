/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.command.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * A {@link CommandPlugin} providing "wf [...]" commands.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class WfCommandPlugin implements CommandPlugin {

    private static final int PARSING_WORKFLOW_FILE_RETRY_INTERVAL = 2000;

    private static final int MAXIMUM_WORKFLOW_PARSE_RETRIES = 5;

    private static final String[] SELF_TEST_RELATIVE_WF_FOLDER_PATHS =
        new String[] {
            "de.rcenvironment.core.component.workflow.tests/src/test/resources/workflows_automated_without_placeholders",
            "de.rcenvironment.core.component.workflow.tests/src/test/resources/workflows_automated_with_placeholders",
            // note: this path is not intended as these files' permanent place; move to non-gui fragment - misc_ro
            "de.rcenvironment.core.gui.wizards.exampleproject/templates/workflows_examples"
        };

    private static final String BASEDIR_OPTION = "--basedir";

    private static final String STRING_DOT = ".";

    private static final int WORKFLOW_SUFFIX_NUMBER_MODULO = 100;

    // TODO >5.0.0: crude fix for #10436 - align better with generated workflow name - misc_ro
    private static final AtomicInteger GLOBAL_WORKFLOW_SUFFIX_SEQUENCE_COUNTER = new AtomicInteger();

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private AtomicInteger sequenceNumberGenerator = new AtomicInteger();

    private Log log = LogFactory.getLog(getClass());

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription("wf run", "[--dispose <onfinished|never|always>] [-p <JSON placeholder file>]"
            + " <workflow file>", false, "execute a workflow file"));
        contributions.add(new CommandDescription("wf verify",
            "[--dispose <onfinished|never|always>] [--pr <parallel runs>] [--sr <sequential runs>] [-p <JSON placeholder file>] "
            + "([--basedir <root directory for all subsequent files>] (<workflow filename>|\"*\")+ )+",
            true, "batch test the specified workflow files"));
        contributions.add(new CommandDescription("wf self-test", "-p <JSON placeholder file> <root path containing RCE core projects>",
            true, "batch test all default workflow files"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken("wf");
        String subCmd = context.consumeNextToken();
        if ("run".equals(subCmd)) {
            // "wf run <filename>"
            performWfRun(context);
        } else if ("verify".equals(subCmd)) {
            // "wf verify ..."
            performWfVerify(context);
        } else if ("self-test".equals(subCmd)) {
            performWfSelfTest(context);
        } else {
            throw CommandException.unknownCommand(context);
        }
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {}

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindWorkflowExecutionService(HeadlessWorkflowExecutionService newInstance) {
        this.workflowExecutionService = newInstance;
    }

    private void performWfRun(CommandContext cmdCtx) throws CommandException {
        // "wf run [--dispose] [-p <JSON placeholder file>] <filename>"

        HeadlessWorkflowExecutionService.Dispose dispose = readOptionalDisposeParameter(cmdCtx);
        File placeholdersFile = readOptionalPlaceholdersFileParameter(cmdCtx);
        
        final String filename = cmdCtx.consumeNextToken();
        // verify: filename is present and the only parameter
        if (filename == null) {
            throw CommandException.syntaxError("Missing filename", cmdCtx);
        }

        if (cmdCtx.consumeNextToken() != null) {
            throw CommandException.wrongNumberOfParameters(cmdCtx);
        }
        final File wfFile;
        try {
            wfFile = WorkflowExecutionUtils.resolveWorkflowOrPlaceholderFileLocation(filename,
                WorkflowExecutionUtils.DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_WORKFLOW_FILE);
        } catch (FileNotFoundException e) {
            throw CommandException.executionError(e.getMessage(), cmdCtx);
        }

        // introduced to allow retries in distributed setup, if not all required connections are established when --batch is executed
        // It slows down the execution as parsing the workflow file is done twice now. Should be improved. -seid_do
        validateWorkflow(cmdCtx, wfFile);
        
        try {
            // TODO specify log directory?
            workflowExecutionService.executeWorkflow(wfFile, placeholdersFile,
                setupLogDirectoryForWfFile(wfFile), cmdCtx.getOutputReceiver(), null, dispose);
        } catch (WorkflowExecutionException e) {
            log.error("Exception while executing workflow from file: " + wfFile.getAbsolutePath(), e);
            throw CommandException.executionError(e.getMessage(), cmdCtx);
        }
    }
    
    private void validateWorkflow(CommandContext context, File wfFile) throws CommandException {
        int retries = 0;
        while (true) {
            try {
                if (workflowExecutionService.isWorkflowDescriptionValid(workflowExecutionService.parseWorkflowFile(wfFile,
                    context.getOutputReceiver()))) {
                    break;
                } else {
                    if (retries >= MAXIMUM_WORKFLOW_PARSE_RETRIES) {
                        log.debug(String.format("Maximum number of retries (%d) reached while validating the workflow file '%s'",
                            MAXIMUM_WORKFLOW_PARSE_RETRIES, wfFile.getAbsolutePath()));
                        throw CommandException.executionError(
                            String.format("Workflow file '%s' is not valid. See log above for more details.", wfFile.getAbsolutePath()),
                            context);
                    }
                    log.debug("Retrying workflow validation in a few seconds.");
                    try {
                        Thread.sleep(PARSING_WORKFLOW_FILE_RETRY_INTERVAL);
                    } catch (InterruptedException e1) {
                        log.error("Waiting for parsing retry failed", e1);
                        throw CommandException.executionError(e1.getMessage(), context);
                    }
                    retries++;
                }
            } catch (WorkflowExecutionException e) {
                log.error("Exception while parsing the workflow file " + wfFile.getAbsolutePath(), e);
                throw CommandException.executionError(e.getMessage(), context);
            }            
        }
    }

    private void performWfVerify(final CommandContext context) throws CommandException {
        HeadlessWorkflowExecutionService.Dispose dispose = readOptionalDisposeParameter(context);
        int parallelRuns = readOptionalParallelRunsParameter(context);
        int sequentialRuns = readOptionalSequentialRunsParameter(context);
        File placeholdersFile = readOptionalPlaceholdersFileParameter(context);
        List<File> wfFiles = parseWfVerifyCommand(context);
        executeWfVerifySetup(context, wfFiles, placeholdersFile, parallelRuns, sequentialRuns, dispose);
    }

    private void performWfSelfTest(final CommandContext context) throws CommandException {
        // reuse code, although the parameter is not actually optional
        HeadlessWorkflowExecutionService.Dispose dispose = readOptionalDisposeParameter(context);
        File placeholdersFile = readOptionalPlaceholdersFileParameter(context);
        if (placeholdersFile == null) {
            throw CommandException.executionError("Placeholder file (\"-p <filename>\") must be specified for self-test", context);
        }
        String baseDirPath = context.consumeNextToken();
        if (baseDirPath == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        File baseDir = new File(baseDirPath);
        if (!baseDir.isDirectory()) {
            throw CommandException.executionError("Base directory does not exist: " + baseDir.getAbsolutePath(), context);
        }
        // construct synthetic "wf verify" command tokens and delegate
        List<String> newTokens = new ArrayList<>();
        for (String testFilesPath : SELF_TEST_RELATIVE_WF_FOLDER_PATHS) {
            File absolutePath = new File(baseDir, testFilesPath);
            newTokens.add(BASEDIR_OPTION);
            newTokens.add(absolutePath.getAbsolutePath());
            newTokens.add("*");
        }
        CommandContext syntheticContext = new CommandContext(newTokens, context.getOutputReceiver(), context.getInvokerInformation());
        List<File> wfFiles = parseWfVerifyCommand(syntheticContext);
        executeWfVerifySetup(context, wfFiles, placeholdersFile, dispose);
    }

    private List<File> parseWfVerifyCommand(final CommandContext context) throws CommandException {
        // "wf verify [-pr <parallel runs>] [-sr <sequential runs>] [-p <JSON placeholder file>] [--basedir <dir>] 
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
            } else if ("*".equals(token)) {
                if (lastBaseDir == null) {
                    throw CommandException.executionError("The \"*\" wildcard requires a previous --basedir", context);
                }
                int count = 0;
                for (String filename : lastBaseDir.list()) {
                    if (filename.endsWith(".wf") && !filename.endsWith("_backup.wf")) {
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

    private HeadlessWorkflowExecutionService.Dispose readOptionalDisposeParameter(CommandContext context) throws CommandException {
        if (context.consumeNextTokenIfEquals("--dispose")) {
            String dispose = context.consumeNextToken();
            try {
                if (HeadlessWorkflowExecutionService.Dispose.Always.name().toLowerCase().equals(dispose)) {
                    return HeadlessWorkflowExecutionService.Dispose.Always;
                } else if (HeadlessWorkflowExecutionService.Dispose.Never.name().toLowerCase().equals(dispose)) {
                    return HeadlessWorkflowExecutionService.Dispose.Never;
                } else if (HeadlessWorkflowExecutionService.Dispose.OnFinished.name().toLowerCase().equals(dispose)) {
                    return HeadlessWorkflowExecutionService.Dispose.OnFinished;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw CommandException.syntaxError("Invalid dispose behavior: " + dispose, context);
            }
        }
        return HeadlessWorkflowExecutionService.Dispose.OnFinished;
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

    private void executeWfVerifySetup(final CommandContext context, List<File> wfFiles, final File placeholdersFile,
        HeadlessWorkflowExecutionService.Dispose dispose) throws CommandException {
        executeWfVerifySetup(context, wfFiles, placeholdersFile, 1, 1, dispose);
    }   
    
    private void executeWfVerifySetup(final CommandContext context, List<File> wfFiles, final File placeholdersFile, int parallelRuns,
        int sequentialRuns, final HeadlessWorkflowExecutionService.Dispose dispose) throws CommandException {
        if (wfFiles.isEmpty()) {
            throw CommandException.syntaxError("Error: at least one workflow file must be specified", context);
        }
        final WfVerifyResult wfVerifyResult = new WfVerifyResult(wfFiles.size() * parallelRuns * sequentialRuns);
        for (int j = 0; j < sequentialRuns; j++) {
            CallablesGroup<Void> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Void.class);
            for (int i = 0; i < parallelRuns; i++) {
                for (final File wfFile : wfFiles) {
                    // attach a task id to help with debugging, e.g. for identifying the thread of a stalled workflow - misc_ro
                    String taskId = String.format("wf-verify-%s-%s", sequenceNumberGenerator.incrementAndGet(), wfFile.getName());
                    callablesGroup.add(new Callable<Void>() {
        
                        @Override
                        @TaskDescription("Single 'wf verify' workflow execution")
                        public Void call() {
                            try {
                                // TODO specify log directory?
                                FinalWorkflowState finalState = workflowExecutionService.executeWorkflow(wfFile, placeholdersFile,
                                    setupLogDirectoryForWfFile(wfFile), context.getOutputReceiver(), null, dispose);
                                wfVerifyResult.addFinalState(finalState);
                            } catch (WorkflowExecutionException e) {
                                context.println("Exception while executing workflow " + wfFile + ": " + e.toString());
                                log.warn("Exception while executing workflow " + wfFile + " triggered by 'wf verify' command", e);
                                wfVerifyResult.addError();
                            }
                            return null;
                        }
                    }, taskId);
                }
            }
            callablesGroup.executeParallel(new AsyncExceptionListener() {
    
                @Override
                public void onAsyncException(Exception e) {
                    context.println("Async error while executing workflow(s): " + e.toString());
                    log.warn("Async error while executing workflow(s) for 'wf verify' command", e);
                }
            });
        }
        context.println(String.format("Workflow verification results (%s):", wfVerifyResult.asString()));
    }

    /**
     * Counts overall result of wf verify execution.
     * 
     * @author Doreen Seider
     */
    private class WfVerifyResult {

        private final int runsSubmitted;

        private AtomicInteger totalRuns = new AtomicInteger(0);

        private AtomicInteger finished = new AtomicInteger(0);

        private AtomicInteger failed = new AtomicInteger(0);

        private AtomicInteger canceled = new AtomicInteger(0);

        private AtomicInteger error = new AtomicInteger(0);

        public WfVerifyResult(int runsSubmitted) {
            this.runsSubmitted = runsSubmitted;
        }

        public void addError() {
            error.incrementAndGet();
            totalRuns.incrementAndGet();
        }

        public void addFinalState(FinalWorkflowState finalState) {
            switch (finalState) {
            case FINISHED:
                finished.incrementAndGet();
                break;
            case CANCELLED:
                canceled.incrementAndGet();
                break;
            case FAILED:
                failed.incrementAndGet();
                break;
            default:
                break;
            }
            totalRuns.incrementAndGet();
        }

        private String asString() {
            return String.format("Executions: %d/%d -> Finished: %d,  Cancelled: %d, Failed: %d, Error: %d",
                totalRuns.get(), runsSubmitted, finished.get(), canceled.get(), failed.get(), error.get());
        }
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
        // TODO don't use SQL timestamp for formatting; also, use String.format()
        Timestamp ts = new Timestamp(millis);
        folderName = "logs/" + folderName + "_" + ts.toString().replace('.', '-').replace(' ', '_').replace(':', '-') + "_" + suffixNumber;

        File logDir = new File(parentDir, folderName);
        logDir.mkdirs();
        if (!logDir.isDirectory()) {
            throw new WorkflowExecutionException("Failed to create log directory" + logDir.getAbsolutePath());
        }
        return logDir;
    }
}
