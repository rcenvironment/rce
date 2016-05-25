/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.server.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.embedded.ssh.api.ScpContext;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.remoteaccess.common.RemoteAccessConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils.SubstitutionContext;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.incubator.IdGenerator;

/**
 * A {@link CommandPlugin} providing "ra/ra-admin [...]" commands.
 * 
 * @author Robert Mischke
 */
public class RemoteAccessCommandPlugin implements CommandPlugin {

    private static final String WORKFLOW_STATE_CHANGE_CONSOLEROW_PREFIX = ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name() + ":";

    private static final String RA_COMMAND = "ra";

    private static final String RA_ADMIN_COMMAND = "ra-admin";

    private static final String SUBCOMMAND_PROTOCOL_VERSION = "protocol-version";

    private static final String SUBCOMMAND_LIST_TOOLS = "list-tools";

    private static final String SUBCOMMAND_LIST_WORKFLOWS = "list-wfs";

    private static final String SUBCOMMAND_INIT = "init";

    private static final String OPTION_COMPACT_SHORT_FORM = "-c";

    private static final String OPTION_COMPACT_LONG_FORM = "--compact";

    private static final String SUBCOMMAND_RUN_TOOL = "run-tool";

    private static final String SUBCOMMAND_RUN_WF = "run-wf";

    private static final String OPTION_STREAMING_OUTPUT_SHORT_FORM = "-o";

    private static final String OPTION_STREAMING_OUTPUT_LONG_FORM = "--show-output";

    private static final Object SUBCOMMAND_DISPOSE = "dispose";

    private static final Object SUBCOMMAND_ADMIN_PUBLISH_WF = "publish-wf";

    private static final String OPTION_PLACEHOLDERS_FILE = "-p";

    private static final Object SUBCOMMAND_ADMIN_UNPUBLISH_WF = "unpublish-wf";

    private static final Object SUBCOMMAND_ADMIN_LIST_WFS = "list-wfs";

    private RemoteAccessService remoteAccessService;

    private ScpContextManager scpContextManager;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Template for the shared code parts between the "run-tool" and "run-wf" commands.
     * 
     * @author Robert Mischke
     */
    private abstract class WorkflowRun {

        private final CommandContext context;

        private WorkflowRun(CommandContext context) {
            this.context = context;
        }

        public void execute() throws CommandException {
            SshAccount account = getAndValidateSshAccount(context);
            String usedCommandVariant = context.getOriginalTokens().get(0); // e.g. "ra" or "ra-admin"
            final String sessionToken = context.consumeNextToken();

            String virtualScpRootPath = getVirtualScpRootPath(usedCommandVariant, sessionToken);

            // TODO fetch directly by session id instead? - misc_ro
            ScpContext scpContext = scpContextManager.getMatchingScpContext(account.getLoginName(), virtualScpRootPath);
            if (scpContext == null) {
                throw CommandException.executionError(StringUtils.format(
                    "No permission to access session %s (or not a valid session token)", sessionToken), context);
            }

            boolean optionStreamingOutput = context.consumeNextTokenIfEquals(OPTION_STREAMING_OUTPUT_SHORT_FORM)
                || context.consumeNextTokenIfEquals(OPTION_STREAMING_OUTPUT_LONG_FORM);

            readCustomParameters();

            List<String> parameterParts = context.consumeRemainingTokens();
            String parameters = org.apache.commons.lang3.StringUtils.join(parameterParts, " ");
            log.debug("Read parameter string: " + parameters);
            if (!validateToolOrWorkflowParameterString(parameters)) {
                throw CommandException.executionError(StringUtils.format(
                    "The parameter string contains at least one forbidden character. "
                        + "More information is available in the RCE instance's log files.", sessionToken), context);
            }

            log.debug("Executing 'run' command in the context of temporary account " + account.getLoginName()
                + ", with a local SCP directory of " + scpContext.getLocalRootPath());

            File inputFilesPath = new File(scpContext.getLocalRootPath(), "input");
            File outputFilesPath = new File(scpContext.getLocalRootPath(), "output");

            try {
                if (!inputFilesPath.isDirectory()) {
                    throw CommandException.executionError("No \"input\" directory found; aborting tool run", context);
                }
                SingleConsoleRowsProcessor optionalStreamingOutputProcessor = null;
                if (optionStreamingOutput) {
                    optionalStreamingOutputProcessor = new StreamingOutputConsoleRowAdapter(context, sessionToken);
                }
                FinalWorkflowState finalState =
                    invokeWorkflow(parameters, inputFilesPath, outputFilesPath, optionalStreamingOutputProcessor);
                if (!outputFilesPath.isDirectory()) {
                    context.println("WARNING: no \"output\" directory found after tool execution; creating an empty one");
                    outputFilesPath.mkdirs();
                    if (!outputFilesPath.isDirectory()) {
                        context.println("WARNING: \"output\" directory still does not exist after attempting to create it");
                    }
                }
                // TODO merge common code with "wf" command plugin?
                if (finalState != FinalWorkflowState.FINISHED) {
                    throw CommandException.executionError("The workflow finished in state " + finalState + " (instead of "
                        + FinalWorkflowState.FINISHED + "); check the log file for more details", context);
                }
            } catch (IOException | WorkflowExecutionException e) {
                // TODO review: error handling sufficient? - misc_ro
                log.error("Error running remote access workflow", e);
                throw CommandException.executionError("An error occurred during remote workflow execution: " + e.toString(), context);
            }
        }

        protected abstract void readCustomParameters() throws CommandException;

        protected abstract FinalWorkflowState invokeWorkflow(String parameters, File inputFilesPath, File outputFilesPath,
            SingleConsoleRowsProcessor optionalStreamingOutputProcessor) throws IOException, WorkflowExecutionException;

        private boolean validateToolOrWorkflowParameterString(String parameterString) {
            // only accept strings that are safe in all known contexts
            for (SubstitutionContext substitutionContext : SubstitutionContext.values()) {
                if (!StringSubstitutionSecurityUtils.isSafeForSubstitutionInsideDoubleQuotes(parameterString, substitutionContext)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Processes workflow {@link ConsoleRow}s and generates appropriate streaming output lines on the context's {@link TextOutputReceiver} .
     * 
     * @author Robert Mischke
     */
    private static final class StreamingOutputConsoleRowAdapter implements SingleConsoleRowsProcessor {

        private final String sessionToken;

        private final CommandContext context;

        private StreamingOutputConsoleRowAdapter(CommandContext context, String sessionToken) {
            this.sessionToken = sessionToken;
            this.context = context;
        }

        @Override
        public void onConsoleRow(ConsoleRow consoleRow) {
            ConsoleRow.Type type = consoleRow.getType();
            String payload = consoleRow.getPayload();
            // TODO add session token to output when needed
            switch (type) {
            case TOOL_OUT:
                context.println(StringUtils.format("[%s] StdOut: %s", sessionToken, payload));
                break;
            case TOOL_ERROR:
                context.println(StringUtils.format("[%s] StdErr: %s", sessionToken, payload));
                break;
            case LIFE_CYCLE_EVENT:
                if (payload.startsWith(WORKFLOW_STATE_CHANGE_CONSOLEROW_PREFIX)) {
                    String stateString = payload.substring(WORKFLOW_STATE_CHANGE_CONSOLEROW_PREFIX.length());
                    // suppress DISPOSING and DISPOSED states for output stability, as it is random whether they will happen in time or not
                    if (!stateString.startsWith("DISPOS")) {
                        context.println(StringUtils.format("[%s] State: %s", sessionToken, stateString));
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {

    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        // ra protocol-version
        contributions.add(new CommandDescription(RA_COMMAND + " " + SUBCOMMAND_PROTOCOL_VERSION, "", true,
            "prints the protocol version of this interface"));
        // ra list-tools
        contributions.add(new CommandDescription(RA_COMMAND + " " + SUBCOMMAND_LIST_TOOLS, "[-f/--format {csv|token-list}]", true,
            "lists all available tool ids and versions for the \"" + SUBCOMMAND_RUN_TOOL + "\" command",
            "-f/--format: specifies the output format; allowed values are \"csv\" (default) and \"token-stream\""));
        // ra list-wfs
        contributions.add(new CommandDescription(RA_COMMAND + " " + SUBCOMMAND_LIST_WORKFLOWS, "", true,
            "lists all available workflow ids and versions for the \"" + SUBCOMMAND_LIST_WORKFLOWS + "\" command"));
        // ra init
        contributions.add(new CommandDescription(RA_COMMAND + " init", "[-c/--compact]", true,
            "initializes a remote access session, and returns a session token",
            "-c/--compact: only print the session token, omitting all extra information; useful for scripting"));
        // ra run-tool
        contributions.add(new CommandDescription(RA_COMMAND + " " + SUBCOMMAND_RUN_TOOL,
            "<session token> [-o/--show-output] [-n <tool node id>] <tool id> <tool version> [<parameters>]", true,
            "invokes a tool by its id and version; ",
            "-o/--show-output: print tool output and execution state while the command is running",
            "-n: specify the node id (*) of the RCE instance to run the rool on; can be omitted if only one instance provides this tool",
            "    (*) the third value of the \"list-tools\" output",
            "All parameters after <tool version> are passed to the tool as a single parameter string."));
        // ra run-wf
        contributions.add(new CommandDescription(RA_COMMAND + " " + SUBCOMMAND_RUN_WF,
            "<session token> [-o/--show-output] <workflow id> <workflow version> [<parameters>]", true,
            "invokes a published workflow by its id; ",
            "-o/--show-output: print tool output and execution state while the command is running",
            "All parameters after <tool version> are passed to the workflow as a single parameter string."));
        // ra dispose
        contributions.add(new CommandDescription(RA_COMMAND + " dispose", "<session token>", true,
            "releases resources used by a remote access session [not implemented yet]"));
        // ra-admin publish-wf
        contributions
            .add(new CommandDescription(
                RA_ADMIN_COMMAND + " publish-wf",
                "[-t] [-p <JSON placeholder file>] <workflow file> <id>",
                false,
                "publishes a workflow file for remote execution via \""
                    + RA_COMMAND + " " + SUBCOMMAND_RUN_WF + "\" using <id>.",
                "-t (temporary/transient): if set, the workflow is automatically unpublished when the RCE instance is shut down",
                "-p: adds a placeholder file for the given workflow; see the \"wf run\" command's documentation for details.",
                "This operation verifies that the workflow contains the required standard elements before publishing.",
                "Note that a snapshot of the workflow file (and optionally, the given placeholder file) is taken before publishing; ",
                "subsequent changes of the workflow file do NOT affect the published workflow."));
        // ra-admin unpublish-wf
        contributions.add(new CommandDescription(RA_ADMIN_COMMAND + " unpublish-wf", "<id>",
            false, "unpublishes (removes) the workflow file with id <id> from remote execution."));
        // ra-admin list-wfs
        contributions.add(new CommandDescription(RA_ADMIN_COMMAND + " list-wfs", "",
            false, "lists the ids of all published workflows."));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        try {
            String rootCommand = context.consumeNextToken();
            if (RA_COMMAND.equals(rootCommand)) {
                handleStandardCommand(context);
            } else if (RA_ADMIN_COMMAND.equals(rootCommand)) {
                handleAdminCommand(context);
            } else {
                throw CommandException.unknownCommand(context);
            }
        } catch (IOException e) {
            log.warn("I/O error during Remote Access command execution", e);
            throw CommandException.executionError("An I/O error occurred during command execution. "
                + "Please check the log file for details.", context);
        }
    }

    private void handleStandardCommand(CommandContext context) throws IOException, CommandException {
        String subCommand = context.consumeNextToken();
        if (SUBCOMMAND_PROTOCOL_VERSION.equals(subCommand)) {
            performProtocolVersion(context);
        } else if (SUBCOMMAND_LIST_TOOLS.equals(subCommand)) {
            performListTools(context);
        } else if (SUBCOMMAND_LIST_WORKFLOWS.equals(subCommand)) {
            performListWfs(context);
        } else if (SUBCOMMAND_INIT.equals(subCommand)) {
            performInit(context);
        } else if (SUBCOMMAND_RUN_TOOL.equals(subCommand)) {
            performRunTool(context);
        } else if (SUBCOMMAND_RUN_WF.equals(subCommand)) {
            performRunWf(context);
        } else if (SUBCOMMAND_DISPOSE.equals(subCommand)) {
            performDispose(context);
        } else {
            throw CommandException.unknownCommand(context);
        }
    }

    private void handleAdminCommand(CommandContext context) throws IOException, CommandException {
        String subCommand = context.consumeNextToken();
        if (SUBCOMMAND_ADMIN_PUBLISH_WF.equals(subCommand)) {
            performAdminPublishWf(context);
        } else if (SUBCOMMAND_ADMIN_UNPUBLISH_WF.equals(subCommand)) {
            performAdminUnpublishWf(context);
        } else if (SUBCOMMAND_ADMIN_LIST_WFS.equals(subCommand)) {
            performAdminListWfs(context);
        } else {
            throw CommandException.unknownCommand(context);
        }
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindScpContextManager(ScpContextManager newInstance) {
        this.scpContextManager = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindRemoteAccessService(RemoteAccessService newInstance) {
        this.remoteAccessService = newInstance;
    }

    private void performProtocolVersion(CommandContext context) {
        context.println(RemoteAccessConstants.PROTOCOL_VERSION);
    }

    private void performListTools(CommandContext context) throws CommandException {
        String format = "csv";
        if (context.consumeNextTokenIfEquals("-f") || context.consumeNextTokenIfEquals("--format")) {
            format = context.consumeNextToken();
        }
        try {
            remoteAccessService.printListOfAvailableTools(context.getOutputReceiver(), format);
        } catch (IllegalArgumentException e) {
            throw CommandException.syntaxError(e.getMessage(), context);
        }
    }

    private void performListWfs(CommandContext context) throws CommandException {
        // TODO add override and csv output if useful
        String format = "token-stream";
        try {
            remoteAccessService.printListOfAvailableWorkflows(context.getOutputReceiver(), format);
        } catch (IllegalArgumentException e) {
            throw CommandException.syntaxError(e.getMessage(), context);
        }
    }

    private void performInit(CommandContext context) throws IOException, CommandException {
        SshAccount account = getAndValidateSshAccount(context);

        String token = context.consumeNextToken();
        boolean useCompactOutput = OPTION_COMPACT_LONG_FORM.equals(token) || OPTION_COMPACT_SHORT_FORM.equals(token);

        String sessionToken = IdGenerator.randomUUIDWithoutDashes().substring(0, 8); // TODO improve
        String usedCommandVariant = context.getOriginalTokens().get(0); // e.g. "ra" or "ra-admin"
        String virtualScpRootPath = getVirtualScpRootPath(usedCommandVariant, sessionToken);

        ScpContext scpContext = scpContextManager.createScpContext(account.getLoginName(), virtualScpRootPath);
        createScpContextSubdir("input", scpContext, context);
        createScpContextSubdir("output", scpContext, context);

        if (useCompactOutput) {
            // session token only for simple parsing
            context.println(sessionToken);
        } else {
            context.println(StringUtils.format("Session token: %s", sessionToken));
            context.println(StringUtils.format("Input (upload) SCP path: %sinput/", virtualScpRootPath));
            context.println(StringUtils.format(
                "Execution command: \"%s %s %s <tool id> <tool version> [<parameters>]\"", usedCommandVariant, SUBCOMMAND_RUN_TOOL,
                sessionToken));
            context.println(StringUtils.format("Output (download) SCP path: %soutput/",
                virtualScpRootPath));
        }
    }

    private void performRunTool(final CommandContext context) throws CommandException {

        new WorkflowRun(context) {

            private String toolId;

            private String toolVersion;

            private String nodeId;

            @Override
            protected void readCustomParameters() throws CommandException {

                if (context.consumeNextTokenIfEquals("-n")) {
                    nodeId = context.consumeNextToken();
                    if (nodeId == null) {
                        throw CommandException.syntaxError("Error: missing node id after -n", context);
                    }
                }

                toolId = context.consumeNextToken();
                validateIdOrVersion(toolId, "tool id", context);

                toolVersion = context.consumeNextToken();
                validateIdOrVersion(toolId, "tool version", context);

                log.debug(StringUtils.format("Command run-tool: Parsed tool id '%s', version '%s'", toolId, toolVersion));

                try {
                    nodeId = remoteAccessService.validateToolParametersAndGetFinalNodeId(toolId, toolVersion, nodeId);
                } catch (WorkflowExecutionException e) {
                    throw CommandException.executionError("Invalid tool parameters: " + e.getMessage(), context);
                }
            }

            @Override
            protected FinalWorkflowState invokeWorkflow(String parameters, File inputFilesPath, File outputFilesPath,
                SingleConsoleRowsProcessor optionalStreamingOutputProcessor) throws IOException, WorkflowExecutionException {
                return remoteAccessService.runSingleToolWorkflow(toolId, toolVersion, nodeId, parameters, inputFilesPath,
                    outputFilesPath, optionalStreamingOutputProcessor);
            }
        }.execute();
    }

    private void performRunWf(final CommandContext context) throws CommandException {
        new WorkflowRun(context) {

            private String workflowId;

            private String workflowVersion;

            @Override
            protected void readCustomParameters() throws CommandException {
                workflowId = context.consumeNextToken();
                validateIdOrVersion(workflowId, "workflow id", context);

                workflowVersion = context.consumeNextToken();
                validateIdOrVersion(workflowVersion, "workflow version", context);

                log.debug(StringUtils.format("Command run-wf: Parsed workflow id '%s', version '%s'", workflowId, workflowVersion));
            }

            @Override
            protected FinalWorkflowState invokeWorkflow(String parameters, File inputFilesPath, File outputFilesPath,
                SingleConsoleRowsProcessor optionalStreamingOutputProcessor) throws IOException, WorkflowExecutionException {
                // note: workflowVersion is ignored so far; it was added for future proofing only
                return remoteAccessService.runPublishedWorkflowTemplate(workflowId, parameters, inputFilesPath, outputFilesPath,
                    optionalStreamingOutputProcessor);
            }

        }.execute();
    }

    private void performDispose(CommandContext context) throws CommandException {
        getAndValidateSshAccount(context);
        // TODO >5.0.0: actually dispose ScpContext - misc_ro
    }

    private void performAdminPublishWf(CommandContext context) throws CommandException {
        // ra-admin publish-wf [-t] [-p <JSON placeholder file>] <workflow file> <id>

        // note: the -t (transient) option is the inverse of the boolean value set here (persistent);
        // the default behavior is "persistent" since persistence was added in 6.2.0
        boolean makePersistent = !context.consumeNextTokenIfEquals("-t");

        File placeholdersFile = null; // optional
        if (context.consumeNextTokenIfEquals(OPTION_PLACEHOLDERS_FILE)) {
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

        final String filename = context.consumeNextToken();
        validateParameterNotNull(filename, "filename", context);

        final String publishId = context.consumeNextToken();
        validateIdOrVersion(publishId, "workflow publish id", context);

        // TODO also add validation for version number when added

        if (context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        File wfFile;
        try {
            wfFile =
                WorkflowExecutionUtils.resolveWorkflowOrPlaceholderFileLocation(filename,
                    WorkflowExecutionUtils.DEFAULT_ERROR_MESSAGE_TEMPLATE_CANNOT_READ_WORKFLOW_FILE);
        } catch (FileNotFoundException e) {
            throw CommandException.executionError(e.getMessage(), context);
        }
        try {
            remoteAccessService.checkAndPublishWorkflowFile(wfFile, placeholdersFile, publishId, context.getOutputReceiver(),
                makePersistent);
        } catch (WorkflowExecutionException e) {
            throw CommandException.executionError(e.getMessage(), context);
        } catch (RuntimeException e) {
            log.error("Error checking/publishing workflow file", e);
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performAdminUnpublishWf(CommandContext context) throws CommandException {
        final String publishId = context.consumeNextToken();
        // note: intentionally only validating for presence to allow removal of now-forbidden ids
        validateIdOrVersion(publishId, "workflow publish id", context);

        if (context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            remoteAccessService.unpublishWorkflowForId(publishId, context.getOutputReceiver());
        } catch (WorkflowExecutionException e) {
            throw CommandException.executionError(e.getMessage(), context);
        }
    }

    private void performAdminListWfs(CommandContext context) throws CommandException {
        remoteAccessService.printSummaryOfPublishedWorkflows(context.getOutputReceiver());
    }

    private void createScpContextSubdir(String name, ScpContext scpContext, CommandContext commandContext) throws CommandException {
        File dir = new File(scpContext.getLocalRootPath(), name);
        if (!dir.mkdir()) {
            throw CommandException.executionError("Internal problem: failed to create " + name + " directory", commandContext);
        }
    }

    private String getVirtualScpRootPath(String commandVariant, String sessionToken) {
        return StringUtils.format("/%s/%s/", commandVariant, sessionToken);
    }

    private SshAccount getAndValidateSshAccount(CommandContext context) throws CommandException {
        Object invoker = context.getInvokerInformation();
        if (!(invoker instanceof SshAccount)) {
            throw CommandException.executionError("This command is only usable from an SSH account as it requires an SCP context", context);
        }
        return (SshAccount) invoker;
    }

    protected void validateParameterNotNull(String input, String description, CommandContext context) throws CommandException {
        if (input == null) {
            throw CommandException.syntaxError("Error: missing " + description, context);
        }
    }

    /**
     * Includes {@link #validateParameterNotNull(String, String, CommandContext)}.
     */
    private void validateIdOrVersion(String input, String description, CommandContext context) throws CommandException {
        validateParameterNotNull(input, description, context);
        String errorMsg = StringUtils.checkAgainstCommonInputRules(input);
        if (errorMsg != null) {
            throw CommandException.syntaxError(StringUtils.format("Invalid %s: %s", description, errorMsg), context);
        }
    }
}
