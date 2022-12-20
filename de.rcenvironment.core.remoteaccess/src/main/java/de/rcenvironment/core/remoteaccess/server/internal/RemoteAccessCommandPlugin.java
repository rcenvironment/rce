/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.server.internal;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.FileParameter;
import de.rcenvironment.core.command.spi.ListCommandParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.NamedMultiParameter;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedFileParameter;
import de.rcenvironment.core.command.spi.ParsedListParameter;
import de.rcenvironment.core.command.spi.ParsedMultiParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.embedded.ssh.api.ScpContext;
import de.rcenvironment.core.embedded.ssh.api.ScpContextManager;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.remoteaccess.common.RemoteAccessConstants;
import de.rcenvironment.core.utils.common.CommonIdRules;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils.SubstitutionContext;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * A {@link CommandPlugin} providing "ra/ra-admin [...]" commands.
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
@Component
public class RemoteAccessCommandPlugin implements CommandPlugin {

    private static final int SEC_TO_MSEC = 1000;

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

    private static final String SUBCOMMAND_CANCEL = "cancel";

    private static final String OPTION_STREAMING_OUTPUT_SHORT_FORM = "-o";

    private static final String OPTION_STREAMING_OUTPUT_LONG_FORM = "--show-output";

    private static final String SUBCOMMAND_DISPOSE = "dispose";

    private static final String SUBCOMMAND_TOOL_DETAILS = "describe-tool";

    private static final String SUBCOMMAND_WF_DETAILS = "describe-wf";

    private static final String SUBCOMMAND_TOOL_DOCUMENTATION_LIST = "get-doc-list";

    private static final String SUBCOMMAND_DOWNLOAD_DOCUMENTATION = "get-tool-doc";

    private static final String SUBCOMMAND_ADMIN_PUBLISH_WF = "publish-wf";

    private static final String OPTION_PLACEHOLDERS_FILE = "-p";

    private static final String OPTION_GROUP_NAME = "-g";

    private static final String SUBCOMMAND_ADMIN_UNPUBLISH_WF = "unpublish-wf";

    private static final String SUBCOMMAND_ADMIN_LIST_WFS = "list-wfs";

    private static final String INPUT = "input";

    private static final String OUTPUT = "output";

    private static final String FORMAT = "-f";

    private static final String CSV = "csv";

    private static final String TOKEN_STREAM = "token-stream";

    private static final String TEMPLATE = "--template";
    
    private static final String T = "-t";
    
    private static final String K = "-k";

    private static final String TOOLNODEID_PARAM = "-n";
    
    private static final String WITH_LOAD_DATA = "--with-load-data";
    
    private static final String UNCOMPRESSED = "-u";
    
    private static final String SIMPLE = "--simple";
    
    private static final String DYN_INPUTS = "--dynInputs";
    
    private static final String DYN_OUTPUTS = "--dynOutputs";
    
    private static final String NON_REQ_INPUTS = "--nonReqInputs";
    
    // Modifiers

    private static final CommandFlag OUTPUT_FLAG = new CommandFlag(
            OPTION_STREAMING_OUTPUT_SHORT_FORM, OPTION_STREAMING_OUTPUT_LONG_FORM, "shows output");
    
    private static final CommandFlag TEMPLATE_FLAG = new CommandFlag(T, TEMPLATE, "make template");
    
    private static final CommandFlag TEMPORARY_FLAG = new CommandFlag(T, "--temporary", "automatically unpublish when RCE shuts down");
    
    private static final CommandFlag KEEP_DATA_FALG = new CommandFlag(K, "--keep-data", "workflow data will not be deleted");
    
    private static final CommandFlag COMPACT_FLAG = new CommandFlag(OPTION_COMPACT_SHORT_FORM, OPTION_COMPACT_LONG_FORM, "compact output");
    
    private static final CommandFlag UNCOMPRESSED_FLAG = new CommandFlag(UNCOMPRESSED, "uncompressed upload");
    
    private static final CommandFlag SIMPLE_FLAG = new CommandFlag(SIMPLE, "simple description format");
    
    private static final StringParameter SESSION_TOKEN = new StringParameter(null, "session token", "token of the session");
    
    private static final StringParameter TOOL_ID = new StringParameter(null, "tool id", "id of the tool");
    
    private static final StringParameter TOOL_VERSION = new StringParameter(null, "tool version", "version of the tool");
    
    private static final StringParameter WORKFLOW_ID = new StringParameter(null, "workflow id", "id of the workflow");
    
    private static final StringParameter WORKFLOW_VERSION = new StringParameter(null, "workflow version", "version of the workflow");
    
    private static final StringParameter TOOL_NODE = new StringParameter(null, "tool node id", "id of the tool node");
    
    private static final StringParameter PARAMETER = new StringParameter(null, "parameter", "additional parameter");
    
    private static final FileParameter WORKFLOW_FILE = new FileParameter("workflow file", "the file of the workflow");
    
    private static final StringParameter GROUP_NAME = new StringParameter("", "group name", "name of the group");
    
    private static final FileParameter PLACEHOLER_FILE = new FileParameter("JSON placeholder file", "JSON file as a placeholder");
    
    private static final StringParameter FORMAT_CSV_PARAMETER = new StringParameter(CSV, "format", "output format");

    private static final StringParameter FORMAT_TOKEN_STREAM_PARAMETER = new StringParameter(TOKEN_STREAM, "format", "output format");
    
    private static final StringParameter TIME_SPAN_PARAMETER = new StringParameter(null ,
            "time span", "time span");

    private static final StringParameter TIME_LIMIT_PARAMETER = new StringParameter(null ,
        "time limit", "time limit");
    
    private static final StringParameter DYN_INPUTS_PARAMETER = new StringParameter(null, "dyn inputs", "dynamic inputs");
    
    private static final StringParameter DYN_OUTPUTS_PARAMETER = new StringParameter(null, "dyn outputs", "dynamic outputs");
    
    private static final StringParameter NON_REQ_INPUTS_PARAMETER = new StringParameter(null, "non req inputs", "non required inputs");
    
    private static final StringParameter HASH_VALUE_PARAMETER = new StringParameter(null, "hash value", "hash value");
    
    private static final NamedSingleParameter TOOL_NODE_ID = new NamedSingleParameter(TOOLNODEID_PARAM, "set tool node id", TOOL_NODE);
    
    private static final NamedSingleParameter NAMED_GROUP_NAME = new NamedSingleParameter(OPTION_GROUP_NAME, "set group name", GROUP_NAME);
    
    private static final NamedSingleParameter NAMED_PLACEHOLDER_FILE = new NamedSingleParameter(
            OPTION_PLACEHOLDERS_FILE, "JSON placeholder file", PLACEHOLER_FILE);
    
    private static final NamedSingleParameter PARAMETERS = new NamedSingleParameter(OPTION_PLACEHOLDERS_FILE, "additional parameters",
            new ListCommandParameter(PARAMETER, "parameters", "list of parameters"));
    
    private static final NamedSingleParameter NAMED_FORMAT_CSV_PARAMETER = new NamedSingleParameter(FORMAT,
            "output format", FORMAT_CSV_PARAMETER);

    private static final NamedSingleParameter NAMED_FORMAT_TOKEN_STREAM_PARAMETER = new NamedSingleParameter(FORMAT,
            "output format", FORMAT_TOKEN_STREAM_PARAMETER);
    
    private static final NamedMultiParameter NAMED_WITH_LOAD_DATA_PARAMETER = new NamedMultiParameter(WITH_LOAD_DATA,
            "load data", false, 2, TIME_SPAN_PARAMETER, TIME_LIMIT_PARAMETER);
    
    private static final NamedSingleParameter NAMED_DYN_INPUTS_PARAMETER = new NamedSingleParameter(DYN_INPUTS,
            "dynamic inputs", DYN_INPUTS_PARAMETER);
    
    private static final NamedSingleParameter NAMED_DYN_OUTPUTS_PARAMETER = new NamedSingleParameter(DYN_OUTPUTS,
            "dynamic outputs", DYN_OUTPUTS_PARAMETER);
    
    private static final NamedSingleParameter NAMED_NON_REQ_INPUTS_PARAMETER = new NamedSingleParameter(NON_REQ_INPUTS,
            "non required inputs", NON_REQ_INPUTS_PARAMETER);
    
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
            ParsedCommandModifiers modifiers = context.getParsedModifiers();
            
            ParsedStringParameter sessionTokenParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
            final String sessionToken = sessionTokenParameter.getResult();
            boolean optionStreamingOutput = modifiers.hasCommandFlag(OPTION_STREAMING_OUTPUT_SHORT_FORM);
            
            SshAccount account = getAndValidateSshAccount(context);
            String usedCommandVariant = context.getOriginalTokens().get(0); // e.g. "ra" or "ra-admin"
            
            String virtualScpRootPath = getVirtualScpRootPath(usedCommandVariant, sessionToken);

            // TODO fetch directly by session id instead? - misc_ro
            ScpContext scpContext = scpContextManager.getMatchingScpContext(account.getLoginName(), virtualScpRootPath);
            if (scpContext == null) {
                throw CommandException.executionError(StringUtils.format(
                    "No permission to access session %s (or not a valid session token)", sessionToken), context);
            }

            readCustomParameters();

            /*
             * List<String> parameterParts = context.consumeRemainingTokens(); String
             * parameters = org.apache.commons.lang3.StringUtils.join(parameterParts, " ");
             * log.debug("Read parameter string: " + parameters); if
             * (!validateToolOrWorkflowParameterString(parameters)) { throw
             * CommandException.executionError(StringUtils.format(
             * "The parameter string contains at least one forbidden character. " +
             * "More information is available in the RCE instance's log files.",
             * sessionToken), context); }
             */

            log.debug("Executing 'run' command in the context of temporary account " + account.getLoginName()
                    + ", with a local SCP directory of " + scpContext.getLocalRootPath());

            File inputFilesPath = new File(scpContext.getLocalRootPath(), INPUT);
            File outputFilesPath = new File(scpContext.getLocalRootPath(), OUTPUT);

            try {
                if (!inputFilesPath.isDirectory()) {
                    throw CommandException.executionError("No \"input\" directory found; aborting tool run", context);
                }
                SingleConsoleRowsProcessor optionalStreamingOutputProcessor = null;
                if (optionStreamingOutput) {
                    optionalStreamingOutputProcessor = new StreamingOutputConsoleRowAdapter(context, sessionToken);
                }
                FinalWorkflowState finalState = invokeWorkflow(sessionToken, inputFilesPath, outputFilesPath,
                        optionalStreamingOutputProcessor);
                if (!outputFilesPath.isDirectory()) {
                    context.println(
                            "WARNING: no \"output\" directory found after tool execution; creating an empty one");
                    outputFilesPath.mkdirs();
                    if (!outputFilesPath.isDirectory()) {
                        context.println(
                                "WARNING: \"output\" directory still does not exist after attempting to create it");
                    }
                }
                // TODO merge common code with "wf" command plugin?
                if (finalState != FinalWorkflowState.FINISHED) {
                    throw CommandException
                            .executionError(
                                    "The workflow finished in state " + finalState + " (instead of "
                                            + FinalWorkflowState.FINISHED + "); check the log file for more details",
                                    context);
                }
            } catch (IOException | WorkflowExecutionException e) {
                // TODO review: error handling sufficient? - misc_ro
                log.error("Error running remote access workflow", e);
                throw CommandException
                        .executionError("An error occurred during remote workflow execution: " + e.toString(), context);
            }
        }

        protected abstract void readCustomParameters() throws CommandException;

        protected abstract FinalWorkflowState invokeWorkflow(String sessionToken, File inputFilesPath,
                File outputFilesPath, SingleConsoleRowsProcessor optionalStreamingOutputProcessor)
                throws IOException, WorkflowExecutionException;

        private boolean validateToolOrWorkflowParameterString(String parameterString) {
            // only accept strings that are safe in all known contexts
            for (SubstitutionContext substitutionContext : SubstitutionContext.values()) {
                if (!StringSubstitutionSecurityUtils.isSafeForSubstitutionInsideDoubleQuotes(parameterString,
                        substitutionContext)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Processes workflow {@link ConsoleRow}s and generates appropriate streaming
     * output lines on the context's {@link TextOutputReceiver} .
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
            case COMPONENT_ERROR:
            case COMPONENT_WARN:
                context.println(StringUtils.format("[%s] StdErr: %s", sessionToken, payload));
                break;
            case LIFE_CYCLE_EVENT:
                if (payload.startsWith(WORKFLOW_STATE_CHANGE_CONSOLEROW_PREFIX)) {
                    String stateString = payload.substring(WORKFLOW_STATE_CHANGE_CONSOLEROW_PREFIX.length());
                    // suppress DISPOSING and DISPOSED states for output stability, as it is random
                    // whether they will happen in time or not
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
    
    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription userCommands = new MainCommandDescription(RA_COMMAND, "Commands for remote-access",
            "Commands for remote-access", true,
            new SubCommandDescription(SUBCOMMAND_PROTOCOL_VERSION,
                "prints the protocol version of this interface", this::performProtocolVersion, true),
            new SubCommandDescription(SUBCOMMAND_LIST_TOOLS, // How does this work?
                "lists all available tool ids and versions for the \"" + SUBCOMMAND_RUN_TOOL + "\"command",
                this::performListTools, new CommandModifierInfo(
                    new NamedParameter[] {
                        NAMED_FORMAT_CSV_PARAMETER,
                        NAMED_WITH_LOAD_DATA_PARAMETER
                    }
                ), true),
            new SubCommandDescription(SUBCOMMAND_LIST_WORKFLOWS,
                "lists all available workflow ids and versions for the \"" + SUBCOMMAND_LIST_WORKFLOWS
                + "\"command",
                this::performListWfs, new CommandModifierInfo(
                    new NamedParameter[] {
                        NAMED_FORMAT_TOKEN_STREAM_PARAMETER
                    }
                ), true),
            new SubCommandDescription(SUBCOMMAND_INIT,
                "initializes a remote access session, and returns a session token",
                ((context) -> {
                try {
                    performInit(context);
                } catch (IOException e) {
                    log.warn("I/O error during Remote Access command execution", e);
                    throw CommandException.executionError("An I/O error occurred during command execution. "
                        + "Please check the log file for details.", context);
                }
            }),
                new CommandModifierInfo(
                    new CommandFlag[] {
                        COMPACT_FLAG
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_RUN_TOOL, "invokes a tool by its id and version", //Needs work
                this::performRunTool,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        SESSION_TOKEN,
                        TOOL_ID,
                        TOOL_VERSION
                    },
                    new CommandFlag[] {
                        OUTPUT_FLAG,
                        UNCOMPRESSED_FLAG,
                        SIMPLE_FLAG
                    },
                    new NamedParameter[] {
                        TOOL_NODE_ID,
                        PARAMETERS,
                        NAMED_DYN_INPUTS_PARAMETER,
                        NAMED_DYN_OUTPUTS_PARAMETER,
                        NAMED_NON_REQ_INPUTS_PARAMETER
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_RUN_WF, "invokes a published workflow by its id", //Needs work
                this::performRunWf,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        SESSION_TOKEN,
                        WORKFLOW_ID,
                        WORKFLOW_VERSION
                    },
                    new CommandFlag[] {
                        OUTPUT_FLAG
                    },
                    new NamedParameter[] {
                        PARAMETERS
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_DISPOSE,
                "releases resources used by a remote access session", this::performDispose,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        SESSION_TOKEN
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_TOOL_DETAILS,
                "prints names and data types of the tool's or workflow's intputs and outputs",
                this::performGetToolDetails,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        TOOL_ID,
                        TOOL_VERSION
                    },
                    new CommandFlag[] {
                        TEMPLATE_FLAG
                    },
                    new NamedParameter[] {
                        TOOL_NODE_ID
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_WF_DETAILS,
                "prints names and data types of the tool's or workflow's intputs and outputs",
                this::performGetWfDetails,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        WORKFLOW_ID,
                        WORKFLOW_VERSION
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_CANCEL, "cancels a session",
                this::performCancel,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        SESSION_TOKEN
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_TOOL_DOCUMENTATION_LIST, "get tool documentation list",
                this::performGetToolDocList,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        TOOL_ID,
                        TOOL_VERSION
                    }
                ), true
            ),
            new SubCommandDescription(SUBCOMMAND_DOWNLOAD_DOCUMENTATION, "download documentation",
                this::performDownloadDocumentation,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        SESSION_TOKEN,
                        TOOL_ID,
                        TOOL_VERSION,
                        TOOL_NODE,
                        HASH_VALUE_PARAMETER
                    }
                ), true
            )
        );
        
        final MainCommandDescription adminCommands = new MainCommandDescription(RA_ADMIN_COMMAND, "administrate remote access",
            "administrate remote access",
            new SubCommandDescription(SUBCOMMAND_ADMIN_PUBLISH_WF,
                "publishes a workflow file for remote execution via \"" + RA_COMMAND + " " + SUBCOMMAND_RUN_WF
                + "\" using <id>.", this::performAdminPublishWf,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        WORKFLOW_FILE,
                        WORKFLOW_ID
                    },
                    new CommandFlag[] {
                        KEEP_DATA_FALG,
                        TEMPORARY_FLAG
                    },
                    new NamedParameter[] {
                        NAMED_GROUP_NAME,
                        NAMED_PLACEHOLDER_FILE
                    }
                )
            ),
            new SubCommandDescription(SUBCOMMAND_ADMIN_UNPUBLISH_WF,
                "unpublishes (removes) the workflow file with id <id> from remote execution.",
                this::performAdminUnpublishWf,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        WORKFLOW_ID
                    }
                )
            ),
            new SubCommandDescription((String) SUBCOMMAND_ADMIN_LIST_WFS, "lists the ids of all published workflows.",
                this::performAdminListWfs));
        
        return new MainCommandDescription[] { userCommands, adminCommands };
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    @Reference
    public void bindScpContextManager(ScpContextManager newInstance) {
        this.scpContextManager = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    @Reference
    public void bindRemoteAccessService(RemoteAccessService newInstance) {
        this.remoteAccessService = newInstance;
    }

    private void performProtocolVersion(CommandContext context) {
        context.println(RemoteAccessConstants.PROTOCOL_VERSION_STRING);
    }

    private void performListTools(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter formatParameter = (ParsedStringParameter) modifiers.getCommandParameter(FORMAT);
        ParsedMultiParameter withLoadDataParameter = (ParsedMultiParameter) modifiers.getCommandParameter(WITH_LOAD_DATA);
        
        
        try {
            // load data parameters
            if (withLoadDataParameter != null) {
                ParsedStringParameter timeSpanParameter = (ParsedStringParameter) withLoadDataParameter.getResult()[0];
                ParsedStringParameter timeLimitParameter = (ParsedStringParameter) withLoadDataParameter.getResult()[1];
                
                Integer timeSpan = Integer.valueOf(timeSpanParameter.getResult());
                Integer timeLimit = Integer.valueOf(timeLimitParameter.getResult());
                remoteAccessService.printListOfAvailableTools(context.getOutputReceiver(), formatParameter.getResult(), true,
                        timeSpan * SEC_TO_MSEC, timeLimit);
            } else {
                remoteAccessService.printListOfAvailableTools(context.getOutputReceiver(), formatParameter.getResult(), false, 0, 0);
            }
        } catch (IllegalArgumentException | InterruptedException | ExecutionException | TimeoutException e) {
            throw CommandException.syntaxError(e.toString(), context);
        }

    }

    private void performListWfs(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter formatParameter = (ParsedStringParameter) modifiers.getCommandParameter(FORMAT);
        
        try {
            remoteAccessService.printListOfAvailableWorkflows(context.getOutputReceiver(), formatParameter.getResult());
        } catch (IllegalArgumentException e) {
            throw CommandException.syntaxError(e.getMessage(), context);
        }
    }

    private void performInit(CommandContext context) throws IOException, CommandException {
        SshAccount account = getAndValidateSshAccount(context);

        boolean useCompactOutput = context.getParsedModifiers().hasCommandFlag(OPTION_COMPACT_SHORT_FORM);

        String sessionToken = IdGenerator.fastRandomHexString(8);
        String usedCommandVariant = context.getOriginalTokens().get(0); // e.g. "ra" or "ra-admin"
        String virtualScpRootPath = getVirtualScpRootPath(usedCommandVariant, sessionToken);

        ScpContext scpContext = scpContextManager.createScpContext(account.getLoginName(), virtualScpRootPath);
        createScpContextSubdir(INPUT, scpContext, context);
        createScpContextSubdir(OUTPUT, scpContext, context);

        if (useCompactOutput) {
            // session token only for simple parsing
            context.println(sessionToken);
        } else {
            context.println(StringUtils.format("Session token: %s", sessionToken));
            context.println(StringUtils.format("Input (upload) SCP path: %sinput/", virtualScpRootPath));
            context.println(
                    StringUtils.format("Execution command: \"%s %s %s <tool id> <tool version> [<parameters>]\"",
                            usedCommandVariant, SUBCOMMAND_RUN_TOOL, sessionToken));
            context.println(StringUtils.format("Output (download) SCP path: %soutput/", virtualScpRootPath));
        }
    }

    private void performRunTool(final CommandContext context) throws CommandException {

        new WorkflowRun(context) {

            private String toolId;

            private String toolVersion;

            private String nodeId;

            private String dynInputs;

            private String dynOutputs;

            private String nonReqInputs;

            private boolean uncompressedUpload = false;

            private boolean simpleDescriptionFormat = false;

            @Override
            protected void readCustomParameters() throws CommandException {
                ParsedCommandModifiers modifiers = context.getParsedModifiers();
                
                ParsedStringParameter toolIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
                ParsedStringParameter toolVersionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(2);
                ParsedStringParameter toolNodeIdParameter = (ParsedStringParameter) modifiers.getCommandParameter(TOOLNODEID_PARAM);
                ParsedListParameter parametersParameter = (ParsedListParameter) modifiers.getCommandParameter(OPTION_PLACEHOLDERS_FILE);
                ParsedStringParameter dynInputsParameter = (ParsedStringParameter) modifiers.getCommandParameter(DYN_INPUTS);
                ParsedStringParameter dynOutputsParameter = (ParsedStringParameter) modifiers.getCommandParameter(DYN_OUTPUTS);
                ParsedStringParameter nonReqInputsParameter = (ParsedStringParameter) modifiers.getCommandParameter(NON_REQ_INPUTS);
                
                nodeId = toolNodeIdParameter.getResult();
                toolId = toolIdParameter.getResult();
                validateIdString(toolId, "tool id", context);
                toolVersion = toolVersionParameter.getResult();
                validateVersionString(toolVersion, "tool version", context);
                dynInputs = dynInputsParameter.getResult();
                dynOutputs = dynOutputsParameter.getResult();
                nonReqInputs = nonReqInputsParameter.getResult();
                uncompressedUpload = modifiers.hasCommandFlag("-u");
                simpleDescriptionFormat = modifiers.hasCommandFlag("-simple");
                
                log.debug(
                        StringUtils.format("Command run-tool: Parsed tool id '%s', version '%s'", toolId, toolVersion));

                try {
                    nodeId = remoteAccessService.validateToolParametersAndGetFinalNodeId(toolId, toolVersion, nodeId);
                } catch (WorkflowExecutionException e) {
                    throw CommandException.executionError("Invalid tool parameters: " + e.getMessage(), context);
                }
            }

            @Override
            protected FinalWorkflowState invokeWorkflow(String sessionToken, File inputFilesPath, File outputFilesPath,
                    SingleConsoleRowsProcessor optionalStreamingOutputProcessor)
                    throws IOException, WorkflowExecutionException {
                return remoteAccessService.runSingleToolWorkflow(new RemoteComponentExecutionParameter(toolId,
                        toolVersion, nodeId, sessionToken, inputFilesPath, outputFilesPath, dynInputs, dynOutputs,
                        nonReqInputs, uncompressedUpload, simpleDescriptionFormat), optionalStreamingOutputProcessor);
            }
        }.execute();
    }

    private void performRunWf(final CommandContext context) throws CommandException {
        new WorkflowRun(context) {
            
            private String workflowId;

            private String workflowVersion;

            private boolean uncompressedUpload = false;

            private boolean simpleDescriptionFormat = false;

            @Override
            protected void readCustomParameters() throws CommandException {
                ParsedCommandModifiers modifiers = context.getParsedModifiers();
                
                ParsedStringParameter workflowIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
                ParsedStringParameter workflowVersionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(2);

                workflowId = workflowIdParameter.getResult();
                workflowVersion = workflowVersionParameter.getResult();
                validateVersionString(workflowVersion, "workflow version", context);
                
                uncompressedUpload = modifiers.hasCommandFlag(UNCOMPRESSED);
                simpleDescriptionFormat = modifiers.hasCommandFlag(SIMPLE);

                log.debug(StringUtils.format("Command run-wf: Parsed workflow id '%s', version '%s'", workflowId,
                        workflowVersion));
            }

            @Override
            protected FinalWorkflowState invokeWorkflow(String sessionToken, File inputFilesPath, File outputFilesPath,
                    SingleConsoleRowsProcessor optionalStreamingOutputProcessor)
                    throws IOException, WorkflowExecutionException {
                // note: workflowVersion is ignored so far; it was added for future proofing
                // only
                return remoteAccessService.runPublishedWorkflowTemplate(workflowId, sessionToken, inputFilesPath,
                        outputFilesPath, optionalStreamingOutputProcessor, uncompressedUpload, simpleDescriptionFormat);
            }

        }.execute();
    }

    private void performDispose(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter sessionTokenParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        final String sessionToken = sessionTokenParameter.getResult();
        
        SshAccount account = getAndValidateSshAccount(context);
        String usedCommandVariant = context.getOriginalTokens().get(0); // e.g. "ra" or "ra-admin"

        String virtualScpRootPath = getVirtualScpRootPath(usedCommandVariant, sessionToken);
        ScpContext scpContext = scpContextManager.getMatchingScpContext(account.getLoginName(), virtualScpRootPath);
        try {
            scpContextManager.disposeScpContext(scpContext);
        } catch (IOException e) {
            throw CommandException.executionError(e.getMessage(), context);
        }
    }

    private void performGetToolDetails(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter toolIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter toolVersionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        ParsedStringParameter nodeIdParameter = (ParsedStringParameter) modifiers.getCommandParameter(TOOLNODEID_PARAM);

        final String toolId = toolIdParameter.getResult();
        final String toolVersion = toolVersionParameter.getResult();
        validateIdString(toolId, "tool id", context);
        validateVersionString(toolVersion, "tool version", context);
        String nodeId = nodeIdParameter.getResult();
        boolean template = modifiers.hasCommandFlag(TEMPLATE);

        try {
            nodeId = remoteAccessService.validateToolParametersAndGetFinalNodeId(toolId, toolVersion, nodeId);
        } catch (WorkflowExecutionException e) {
            throw CommandException.executionError("Invalid tool parameters: " + e.getMessage(), context);
        }

        remoteAccessService.printToolDetails(context.getOutputReceiver(), toolId, toolVersion, nodeId, template);
    }

    private void performGetWfDetails(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter wfIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter wfVersionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        
        final String wfId = wfIdParameter.getResult();
        final String wfVersion = wfVersionParameter.getResult();
        boolean template = modifiers.hasCommandFlag(TEMPLATE);

        validateIdString(wfId, "wf id", context);
        validateVersionString(wfVersion, "wf version", context);

        remoteAccessService.printWfDetails(context.getOutputReceiver(), wfId, template);
    }

    private void performCancel(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter sessionTokenParamter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        
        String sessionToken = sessionTokenParamter.getResult();
        remoteAccessService.cancelToolOrWorkflow(sessionToken);
    }

    private void performGetToolDocList(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter toolIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter toolVersionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        
        String toolId = toolIdParameter.getResult();
        String toolVersion = toolVersionParameter.getResult();
        remoteAccessService.getToolDocumentationList(context.getOutputReceiver(), StringUtils.format("%s/%s", toolId, toolVersion));
    }

    private void performDownloadDocumentation(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter sessionTokenParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter toolIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        ParsedStringParameter toolVersionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(2);
        ParsedStringParameter nodeIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(3);
        ParsedStringParameter hashValueParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(4);
        
        SshAccount account = getAndValidateSshAccount(context);
        String sessionToken = sessionTokenParameter.getResult();
        String toolId = toolIdParameter.getResult();
        String nodeId = nodeIdParameter.getResult();
        String toolVersion = toolVersionParameter.getResult();
        String hashValue = hashValueParameter.getResult();

        String usedCommandVariant = context.getOriginalTokens().get(0);
        String virtualScpRootPath = getVirtualScpRootPath(usedCommandVariant, sessionToken);

        // Get SCP context and output file path
        ScpContext scpContext = scpContextManager.getMatchingScpContext(account.getLoginName(), virtualScpRootPath);
        if (scpContext == null) {
            throw CommandException.executionError(StringUtils.format(
                    "No permission to access session %s (or not a valid session token)", sessionToken), context);
        }

        File outputFilesPath = new File(scpContext.getLocalRootPath(), OUTPUT);

        remoteAccessService.getToolDocumentation(context.getOutputReceiver(),  StringUtils.format("%s/%s", toolId, toolVersion), nodeId, hashValue, outputFilesPath);
    }

    private void performAdminPublishWf(CommandContext context) throws CommandException {
        // ra-admin publish-wf [-g group name] [-t] [-p <JSON placeholder file>]
        // <workflow file> <id>
        ParsedCommandModifiers parameters = context.getParsedModifiers();
        
        ParsedFileParameter wfFileParameter = (ParsedFileParameter) parameters.getPositionalCommandParameter(0);
        ParsedStringParameter publishIdParameter = (ParsedStringParameter) parameters.getPositionalCommandParameter(1);
        ParsedStringParameter groupNameParameter = (ParsedStringParameter) parameters.getCommandParameter(OPTION_GROUP_NAME);
        ParsedFileParameter placeholderFileParameter = (ParsedFileParameter) parameters.getCommandParameter(OPTION_PLACEHOLDERS_FILE);
        boolean neverDeleteExecutionData = parameters.hasCommandFlag(K);
        boolean makePersistent = !parameters.hasCommandFlag(T);
        // note: the -t (transient) option is the inverse of the boolean value set here
        // (persistent);
        // the default behavior is "persistent" since persistence was added in 6.2.0
        
        // TODO also add validation for version number when added
        
        try {
            remoteAccessService.checkAndPublishWorkflowFile(wfFileParameter.getResult(),
                    placeholderFileParameter.getResult(), publishIdParameter.getResult(),
                    groupNameParameter.getResult(), context.getOutputReceiver(),
                    makePersistent, neverDeleteExecutionData);
        } catch (WorkflowExecutionException e) {
            throw CommandException.executionError(e.getMessage(), context);
        } catch (RuntimeException e) {
            log.error("Error checking/publishing workflow file", e);
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performAdminUnpublishWf(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter publishIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        final String publishId = publishIdParameter.getResult();
        
        // note: intentionally only validating for presence to allow removal of
        // now-forbidden ids
        validateIdString(publishId, "workflow publish id", context);

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

    private void createScpContextSubdir(String name, ScpContext scpContext, CommandContext commandContext)
            throws CommandException {
        File dir = new File(scpContext.getLocalRootPath(), name);
        if (!dir.mkdir()) {
            throw CommandException.executionError("Internal problem: failed to create " + name + " directory",
                    commandContext);
        }
    }

    private String getVirtualScpRootPath(String commandVariant, String sessionToken) {
        return StringUtils.format("/%s/%s/", commandVariant, sessionToken);
    }

    private SshAccount getAndValidateSshAccount(CommandContext context) throws CommandException {
        Object invoker = context.getInvokerInformation();
        if (!(invoker instanceof SshAccount)) {
            throw CommandException.executionError(
                    "This command is only usable from an SSH account as it requires an SCP context", context);
        }
        return (SshAccount) invoker;
    }

    protected void validateParameterNotNull(String input, String description, CommandContext context)
            throws CommandException {
        if (input == null) {
            throw CommandException.syntaxError("Error: missing " + description, context);
        }
    }

    /**
     * Includes {@link #validateParameterNotNull(String, String, CommandContext)}.
     */
    private void validateIdString(String input, String description, CommandContext context) throws CommandException {
        validateParameterNotNull(input, description, context);
        Optional<String> errorMsg = CommonIdRules.validateCommonIdRules(input);
        if (errorMsg.isPresent()) {
            throw CommandException.syntaxError(StringUtils.format("Invalid %s: %s", description, errorMsg), context);
        }
    }

    /**
     * Includes {@link #validateParameterNotNull(String, String, CommandContext)}.
     */
    private void validateVersionString(String input, String description, CommandContext context)
            throws CommandException {
        validateParameterNotNull(input, description, context);
        Optional<String> errorMsg = CommonIdRules.validateCommonVersionStringRules(input);
        if (errorMsg.isPresent()) {
            throw CommandException.syntaxError(StringUtils.format("Invalid %s: %s", description, errorMsg), context);
        }
    }
}
