/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.instancemanagement.internal.InstanceConfigurationException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * A {@link CommandPlugin} that provides instance management ("im") commands.
 * 
 * @author Robert Mischke
 * @author David Scholz
 * @author Brigitte Boden
 * @author Lukas Rosenbach
 */
public class InstanceManagementCommandPlugin implements CommandPlugin {

    private static final int SECONDS_TO_MILLISECONDS = 1000;

    private static final int DEFAULT_TIMEOUT = 60000;

    private static final String ROOT_COMMAND = "im";

    private static final String ZERO_STRING = "0";

    private static final String COMMA_STRING = ",";

    private static final String ALL_MARKER_TOKEN = "all";

    private static final String IF_MISSING = "--if-missing";

    private static final String FORCE_DOWNLOAD = "--force-download";

    private static final String FORCE_REINSTALL = "--force-reinstall";

    private static final String TIMEOUT = "--timeout";

    private static final String OPTION_START_WITH_GUI = "--gui";

    private static final String COMMAND_ARGUMENTS = "--command-arguments";

    private static final Pattern IP_ADDRESS_PATTERN =

        Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static final String TIMEOUT_DESCRIPTION =
        "--timeout - specifies the maximum length of time this command is allowed to run (in seconds) - default = 60s";

    private InstanceManagementService instanceManagementService;

    private ThreadLocal<CommandContext> currentContext = new ThreadLocal<>(); // to avoid lots of context passing, but still be thread-safe

    /**
     * Encapsulates parsing of common command parameters. Note that this is just a collection of previously scattered and duplicated code;
     * it could certainly be reworked to be more elegant.
     *
     * @author Robert Mischke (extracted/refactored from outer class; various authors)
     */
    private static final class ParameterParser {

        private final CommandContext context;

        private final String installationId;

        private final long timeout;

        private final boolean startWithGUI;

        private final List<String> instanceIds;

        private final String commandArguments;

        /**
         * Constructor.
         * 
         * @param context the {@link CommandContext}
         * @param expectListOfInstanceIds whether a comma-separated list of instance ids (profiles) is expected
         * @param expectInstallationId whether an installation id is expected; pass null to specify that an installation id is optional
         * @throws CommandException on syntax errors
         */
        private ParameterParser(CommandContext context, boolean expectListOfInstanceIds, Boolean expectInstallationId)
            throws CommandException {
            this.context = context;
            List<String> remainingTokens = context.consumeRemainingTokens();

            timeout = determineTimeout(remainingTokens);
            startWithGUI = determineStartWithGUI(remainingTokens);
            commandArguments = determineCommandArguments(remainingTokens);

            if (expectInstallationId != Boolean.FALSE) {
                // note: considering "optional" as "not required" here
                installationId = determineInstallationId(remainingTokens, expectInstallationId == Boolean.TRUE);
            } else {
                installationId = null;
            }

            if (expectListOfInstanceIds) {
                instanceIds = parseTokensToInstanceIdList(remainingTokens);
            } else {
                instanceIds = Collections.unmodifiableList(new ArrayList<String>());
            }

        }

        public long getTimeout() throws CommandException {
            return timeout;
        }

        public boolean getStartWithGUI() throws CommandException {
            return startWithGUI;
        }

        public List<String> getInstanceIds() throws CommandException {
            return instanceIds;
        }

        public String getInstallationId() {
            return installationId;
        }

        public String getCommandArguments() {
            return commandArguments;
        }

        private long determineTimeout(List<String> remainingTokens) throws CommandException {
            long result = 0;
            if (remainingTokens.contains(TIMEOUT)) {
                int index = remainingTokens.indexOf(TIMEOUT);
                remainingTokens.remove(index);
                try {
                    String t = remainingTokens.remove(index);
                    if (org.apache.commons.lang3.StringUtils.isNumeric(t)) {
                        result = Long.parseLong(t);
                    } else {
                        throw CommandException.executionError("No timeout specified", context);
                    }
                } catch (IndexOutOfBoundsException e) {
                    throw CommandException.executionError("No timeout specified", context);
                }
            }

            return result;
        }

        private boolean determineStartWithGUI(List<String> remainingTokens) {
            int index = remainingTokens.indexOf(OPTION_START_WITH_GUI);
            if (index >= 0) {
                remainingTokens.remove(index);
                return true;
            } else {
                return false;
            }
        }

        private String determineInstallationId(List<String> remainingTokens, boolean required) throws CommandException {
            if (!remainingTokens.isEmpty()) {
                final String lastToken = remainingTokens.remove(remainingTokens.size() - 1); // done this way to work for all List types
                if (lastToken.isEmpty()) { // sanity check
                    throw CommandException.executionError("Installation ID can not be empty", context);
                }
                return lastToken;
            } else {
                if (required) {
                    throw new IllegalStateException("No remaining token to use as installation id");
                } else {
                    return null;
                }
            }
        }

        private List<String> parseTokensToInstanceIdList(List<String> remainingTokens) throws CommandException {
            // TODO this could use a comment on the idea behind this...
            List<String> instanceIdList = new LinkedList<>();
            final String c = COMMA_STRING;
            for (String token : remainingTokens) {
                if (token.contains(c)) {
                    if (token.length() == 1) {
                        continue;
                    }
                    if (token.startsWith(c)) {
                        instanceIdList.add(token.replaceFirst(COMMA_STRING, ""));
                    } else {
                        instanceIdList.addAll(Arrays.asList(token.split(c)));
                    }
                } else {
                    instanceIdList.add(token);
                }
            }
            if (instanceIdList.isEmpty()) {
                throw CommandException.syntaxError("Expected at least one instance id", context);
            }
            return Collections.unmodifiableList(instanceIdList);
        }

        private String determineCommandArguments(List<String> remainingTokens) {
            int index = remainingTokens.indexOf(COMMAND_ARGUMENTS);
            if (index >= 0 && index + 1 < remainingTokens.size()) {
                String arguments = remainingTokens.get(index + 1);
                remainingTokens.remove(index + 1);
                remainingTokens.remove(index);
                return arguments;
            } else {
                return null;
            }
        }

    }

    /**
     * OSGi-DS bind method; made public for access from test code.
     * 
     * @param newInstance the new instance to set
     */
    public void bindInstanceManagementService(InstanceManagementService newInstance) {
        this.instanceManagementService = newInstance;
    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();

        contributions
            .add(new CommandDescription(ROOT_COMMAND + " install",
                "[<--if-missing|--force-download|--force-reinstall>] [<mayor version>]/<url version id/part> <installation id>",
                true,
                "downloads and installs a new RCE installation",
                "--if-missing - download and install if and only if an installation with the same version is not present",
                "--force-download - forces the download and reinstallation of the installation files even if they are present in the "
                    + "current cache",
                "--force-reinstall - forces reinstallation even if the same version is already installed",
                TIMEOUT_DESCRIPTION));

        contributions
            .add(new CommandDescription(ROOT_COMMAND + " reinstall",
                "[<--force-download|--force-reinstall>] [<mayor version>]/<url version id/part> <installation id>",
                true,
                "stops all instances running the given installation id, downloads and installs the new RCE installation, and starts the "
                    + "instances again with the new installation",
                "--force-download - forces the download and reinstallation of the installation files even if they are present in the "
                    + "current cache",
                "--force-reinstall - forces reinstallation even if the same version is already installed",
                TIMEOUT_DESCRIPTION));

        contributions
            .add(new CommandDescription(
                ROOT_COMMAND + " configure",
                "<instance id(s)> <command> [<parameters>] [<command> [<parameters>]] [...]",
                true,
                "configures the configuration.json file of the specified RCE instance(s)",

                "Available commands:",
                InstanceManagementConstants.SET_RCE_VERSION
                    + " <version> - sets the rce version of the instances. (Does not work on existing instances.)",
                InstanceManagementConstants.SUBCOMMAND_RESET + " - resets the instance to an empty configuration",
                InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE
                    + " <template id> - applies (i.e. copies) the given template as the new configuration",
                InstanceManagementConstants.SUBCOMMAND_SET_NAME + " <name> - sets the name of the instance",
                InstanceManagementConstants.SUBCOMMAND_SET_COMMENT + " <comment> - sets a general comment",
                InstanceManagementConstants.SUBCOMMAND_SET_WORKFLOW_HOST_OPTION + " [<true/false>] - sets or clears the workflow host flag",
                InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION + " [<true/false>] - sets or clears the relay flag",
                InstanceManagementConstants.SUBCOMMAND_SET_CUSTOM_NODE_ID
                    + " <node id> - adds an override value for the node's network id; use with caution!",
                InstanceManagementConstants.SUBCOMMAND_SET_TEMPDIR_PATH
                    + " <path> - sets the root path for RCE's temporary files directory",
                InstanceManagementConstants.SUBCOMMAND_ADD_SERVER_PORT
                    + " <id> <ip> <port> - adds a new server port and sets the ip and port number to bind to",
                // TODO restore reconnect parameters
                InstanceManagementConstants.SUBCOMMAND_ADD_CONNECTION + " <id> <host> <port> <true/false> - adds new connection "
                    + "to the given ip/hostname and port, and whether it should auto-connect",
                InstanceManagementConstants.SUBCOMMAND_REMOVE_CONNECTION + " <id> removes a connection",
                InstanceManagementConstants.SUBCOMMAND_SET_IP_FILTER_OPTION
                    + " [<true/false>] - enables or disables the ip filter; default: true",
                InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS + " <port> - enables and configures SSH forwarding of "
                    + "RCE console commands by the IM \"master\" instance",
                InstanceManagementConstants.SUBCOMMAND_CONFIGURE_SSH_SERVER
                    + " <ip> <port> - enables the ssh server and sets the ip and port to bind to",
                InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER + " - disables the ssh server",
                InstanceManagementConstants.SUBCOMMAND_ADD_SSH_ACCOUNT
                    + " <username> <role> <enabled: true/false> password - adds an SSH account",
                InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_ACCOUNT + " <username> - removes an SSH account",
                InstanceManagementConstants.SUBCOMMAND_SET_REQUEST_TIMEOUT + " - sets the request timeout in msec",
                InstanceManagementConstants.SUBCOMMAND_SET_FORWARDING_TIMEOUT + " - sets the forwarding timeout in msec",
                InstanceManagementConstants.SUBCOMMAND_ADD_ALLOWED_INBOUND_IP + " <ip> - adds/allows an inbound IP address to the filter",
                InstanceManagementConstants.SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP
                    + " <ip> - removes/disallows an inbound IP address from the filter",
                InstanceManagementConstants.SUBCOMMAND_ADD_SSH_CONNECTION
                    + " <name> <displayName> <host> <port> <loginName> - adds a new ssh connection",
                InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_CONNECTION
                    + " <name> - removes a ssh connection",
                InstanceManagementConstants.SUBCOMMAND_ADD_UPLINK_CONNECTION
                    + " <id> <hostname> <port> <clientid> <gateway> <connectOnStartup> <autoRetry> <user_name> "
                    + "password <password>",
                InstanceManagementConstants.SUBCOMMAND_REMOVE_UPLINK_CONNECTION
                    + " <id> - removes an uplink connection",
                InstanceManagementConstants.SUBCOMMAND_PUBLISH_COMPONENT + " <name> - publishes a new component",
                InstanceManagementConstants.SUBCOMMAND_UNPUBLISH_COMPONENT + " <name> - unpublishes a component",
                InstanceManagementConstants.SUBCOMMAND_SET_BACKGROUND_MONITORING
                    + " <id> <interval> - Enables background monitoring with the given interval (in seconds)",
                InstanceManagementConstants.SUBCOMMAND_WIPE
                    + " - wipes the instance, i.e. recursively deletes everything in the profile folder"
            ));

        contributions.add(new CommandDescription(ROOT_COMMAND + " start",
            "[--timeout <value>] [--command-arguments <arguments>] <instance id1, instance id2, ...> <installation id>",
            true,
            "starts a list of new RCE instances with the desired instance IDs and the desired installation; "
                + "use \"" + InstanceManagementService.MASTER_INSTANCE_SYMBOLIC_INSTALLATION_ID
                + "\" to use the current \"master\" installation",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " stop",
            "[--timeout <value>] <instance id1, instance id2, ...>",
            true,
            "stops a list of running RCE instances",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " start all",
            "[--timeout <value>] [--command-arguments <arguments>] <installation id>",
            true,
            "starts all available instances. Uses the given installation; "
                + "use \"" + InstanceManagementService.MASTER_INSTANCE_SYMBOLIC_INSTALLATION_ID
                + "\" to use the current \"master\" installation",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " stop all",
            "[--timeout <value>] [<installation id>]",
            true,
            "stops all running instances",
            "<installation id> - optional parameter if one want to stop all running instances of a specific installation",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " restart",
            "[--timeout <value>] [--command-arguments <arguments>] <instance id1, instance id2, ...> <installation id>",
            true,
            "restarts a list of RCE instances with the given instance IDs and the given installation"));

        contributions.add(new CommandDescription(ROOT_COMMAND + " dispose",
            "<instance id>",
            true,
            "disposes the specified instance meaning deletion of the profile directory"));

        contributions.add(new CommandDescription(ROOT_COMMAND + " list",
            "[<instances|installations|templates>]",
            true,
            "lists information about instances, installations or templates"));

        contributions.add(new CommandDescription(ROOT_COMMAND + " info",
            "",
            true,
            "shows additional information"));

        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(ROOT_COMMAND);
        String subCommand = context.consumeNextToken();
        if (subCommand == null) {
            throw CommandException.unknownCommand(context);
        }
        if (!instanceManagementService.isInstanceManagementStarted()) {
            throw CommandException.executionError(
                "Cannot execute instance management command. " + instanceManagementService.getReasonInstanceManagementNotStarted(),
                context);
        }

        switch (subCommand) {
        case "install":
            performInstall(context);
            break;
        case "reinstall":
            performReinstall(context);
            break;
        case "configure":
            performConfigure(context);
            break;
        case "start":
            if (context.consumeNextTokenIfEquals(ALL_MARKER_TOKEN)) {
                performStartAll(context);
            } else {
                performStart(context);
            }
            break;
        case "stop":
            if (context.consumeNextTokenIfEquals(ALL_MARKER_TOKEN)) {
                performStopAll(context);
            } else {
                performStop(context);
            }
            break;
        case "restart":
            performRestart(context);
            break;
        case "list":
            performList(context);
            break;
        case "dispose":
            performDispose(context);
            break;
        case "info":
            performInformation(context);
            break;
        case "execute-on":
            performExecuteOn(context);
            break;
        default:
            throw CommandException.syntaxError("Unknown sub-command", context);
        }

        context.getOutputReceiver().addOutput("Done.");
    }

    private void performExecuteOn(CommandContext context) throws CommandException {
        String instanceId = context.consumeNextToken();
        String command = context.consumeNextToken();
        if (instanceId == null || command == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        try {
            instanceManagementService.executeCommandOnInstance(instanceId, command, context.getOutputReceiver());
        } catch (JSchException | SshParameterException | IOException | InterruptedException e) {
            throw CommandException.executionError(
                "Could not execute command " + command + " on instance " + instanceId + ": " + e.getMessage(), context);
        }
    }

    private void performInstall(CommandContext context) throws CommandException {

        int timeout = DEFAULT_TIMEOUT;
        InstallationPolicy policy = InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT;

        if (context.consumeNextTokenIfEquals(TIMEOUT)) {
            timeout = getTimeoutValueFromContext(context);
        }
        if (context.consumeNextTokenIfEquals(FORCE_DOWNLOAD)) {
            policy = InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL;
        } else if (context.consumeNextTokenIfEquals(FORCE_REINSTALL)) {
            policy = InstallationPolicy.FORCE_REINSTALL;
        } else if (context.consumeNextTokenIfEquals(IF_MISSING)) {
            policy = InstallationPolicy.ONLY_INSTALL_IF_NOT_PRESENT;
        }
        if (context.consumeNextTokenIfEquals(TIMEOUT)) {
            timeout = getTimeoutValueFromContext(context);
        }

        String urlQualifier = context.consumeNextToken();
        String installationId = context.consumeNextToken();
        if (urlQualifier == null || installationId == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            switch (policy) {
            case FORCE_NEW_DOWNLOAD_AND_REINSTALL:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL, context.getOutputReceiver(), timeout);
                break;
            case FORCE_REINSTALL:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.FORCE_REINSTALL, context.getOutputReceiver(), timeout);
                break;
            case IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver(), timeout);
                break;
            case ONLY_INSTALL_IF_NOT_PRESENT:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.ONLY_INSTALL_IF_NOT_PRESENT, context.getOutputReceiver(), timeout);
                break;
            default:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver(), timeout);
                break;
            }
        } catch (IOException e) {
            throw CommandException.executionError("Error during installation setup process: " + e.getMessage(), context);
        }
    }

    private int getTimeoutValueFromContext(CommandContext context) throws CommandException {
        try {
            return Integer.parseInt(context.consumeNextToken()) * SECONDS_TO_MILLISECONDS;
        } catch (NumberFormatException e) {
            throw CommandException.executionError("Timeout value is not a number.", context);
        }
    }

    private void performReinstall(CommandContext context) throws CommandException {
        InstallationPolicy policy = InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT;
        if (context.consumeNextTokenIfEquals(FORCE_DOWNLOAD)) {
            policy = InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL;
        } else if (context.consumeNextTokenIfEquals(FORCE_REINSTALL)) {
            policy = InstallationPolicy.FORCE_REINSTALL;
        }
        String urlQualifier = context.consumeNextToken();
        String installationId = context.consumeNextToken();
        if (urlQualifier == null || installationId == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            switch (policy) {
            case FORCE_NEW_DOWNLOAD_AND_REINSTALL:
                instanceManagementService.reinstallFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL, context.getOutputReceiver(), 0);
                break;
            case FORCE_REINSTALL:
                instanceManagementService.reinstallFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.FORCE_REINSTALL, context.getOutputReceiver(), 0);
                break;
            case IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT:
                instanceManagementService.reinstallFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver(), 0);
                break;
            default:
                instanceManagementService.reinstallFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver(), 0);
                break;
            }
        } catch (IOException e) {
            throw CommandException.executionError("Error during installation setup process: " + e.getMessage(), context);
        }
    }

    private boolean validateIpAddress(final String ip) {
        Matcher matcher = IP_ADDRESS_PATTERN.matcher(ip);
        return matcher.matches();
    }

    private void performConfigure(CommandContext context) throws CommandException {
        currentContext.set(context); // TODO use for other commands as well?
        String instanceIds = context.consumeNextToken();
        if (instanceIds == null) {
            throw CommandException.syntaxError("Expected an instance id", context);
        }
        if (instanceIds.contains(COMMA_STRING)) {
            throw CommandException.executionError("Configuring multiple instance ids at once is not implemented yet", context);
        }

        // validate start of first sub-command
        String firstToken = context.peekNextToken();
        if (firstToken == null) {
            throw CommandException.syntaxError("At least one command must be provided after the instance id(s)", context);
        }
        if (!isASubCommandToken(firstToken)) {
            throw CommandException.syntaxError("Expected a command after the instance id(s), but found another value: " + firstToken,
                context);
        }

        final InstanceConfigurationOperationSequence changeSequence = instanceManagementService.newConfigurationOperationSequence();
        while (context.hasRemainingTokens()) {
            String commandToken = context.consumeNextToken();
            if (!isASubCommandToken(commandToken)) {
                throw CommandException.syntaxError("Internal consistency error: received an unexpected non-command token ", context);
            }
            List<String> parameters = new ArrayList<>();
            while (isAParameterToken(context.peekNextToken())) {
                parameters.add(context.consumeNextToken());
            }
            parseConfigurationSubCommand(changeSequence, commandToken, parameters);
        }

        try {
            instanceManagementService.applyInstanceConfigurationOperations(instanceIds, changeSequence, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        } catch (InstanceConfigurationException e) {
            throw CommandException.executionError(e.getMessage(), context);
        }
    }

    private void parseConfigurationSubCommand(InstanceConfigurationOperationSequence changeSequence, String token, List<String> parameters)
        throws CommandException {
        switch (token) {
        case InstanceManagementConstants.SET_RCE_VERSION:
            assertParameterCount(parameters, 1, token);
            String version = parameters.get(0);
            changeSequence.setProfileVersion(version);
            break;
        case InstanceManagementConstants.SUBCOMMAND_RESET:
            assertParameterCount(parameters, 0, token);
            changeSequence.resetConfiguration();
            break;
        case InstanceManagementConstants.SUBCOMMAND_WIPE:
            assertParameterCount(parameters, 0, token);
            changeSequence.wipeConfiguration();
            break;
        case InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE:
            assertParameterCount(parameters, 1, token);
            final String templateName = parameters.get(0);
            changeSequence.applyTemplate(templateName);
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_NAME:
            assertParameterCount(parameters, 1, token);
            final String name = parameters.get(0);
            changeSequence.setName(name);
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_COMMENT:
            assertParameterCount(parameters, 1, token);
            final String comment = parameters.get(0);
            changeSequence.setComment(comment);
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION:
            assertParameterCount(parameters, 0, 1, token);
            changeSequence.setRelayFlag(parseSingleBooleanParameter(parameters, false));
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_WORKFLOW_HOST_OPTION:
            assertParameterCount(parameters, 0, 1, token);
            changeSequence.setWorkflowHostFlag(parseSingleBooleanParameter(parameters, false)); // TODO merge derived configuration keys?
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_CUSTOM_NODE_ID:
            assertParameterCount(parameters, 1, token);
            changeSequence.setCustomNodeId(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_TEMPDIR_PATH:
            assertParameterCount(parameters, 1, token);
            final String tempPath = parameters.get(0);
            changeSequence.setTempDirPath(tempPath);
            break;
        case InstanceManagementConstants.SUBCOMMAND_ADD_SERVER_PORT:
            assertParameterCount(parameters, 3, token); // id, host, port
            String serverPortName = parameters.get(0);
            String serverPortIp = parameters.get(1);
            int serverPortNumber = Integer.parseInt(parameters.get(2));
            changeSequence.addServerPort(serverPortName, serverPortIp, serverPortNumber);
            break;
        case InstanceManagementConstants.SUBCOMMAND_ADD_CONNECTION:
            assertParameterCount(parameters, 4, token); // id, host, port, autoconnect
            parameters.add("5"); // TODO dummy values for now; note that the initial delay cannot be less than 5 seconds (due to validation)
            parameters.add("30");
            parameters.add("1.5");
            changeSequence.addNetworkConnectionFromStringParameters(parameters);
            break;
        case InstanceManagementConstants.SUBCOMMAND_REMOVE_CONNECTION:
            assertParameterCount(parameters, 1, token);
            changeSequence.removeConnection(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_CONFIGURE_SSH_SERVER:
            assertParameterCount(parameters, 2, token);
            configureSshServer(changeSequence, parameters);
            break;
        case InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER:
            assertParameterCount(parameters, 0, token);
            changeSequence.disableSshServer();
            break;
        case InstanceManagementConstants.SUBCOMMAND_ADD_SSH_ACCOUNT:
            assertParameterCount(parameters, 4, token);
            changeSequence.addSshAccountFromStringParameters(parameters);
            break;
        case InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_ACCOUNT:
            assertParameterCount(parameters, 1, token);
            changeSequence.removeSshAccount(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_IP_FILTER_OPTION:
            assertParameterCount(parameters, 0, 1, token);
            final boolean ipFilterState = parseSingleBooleanParameter(parameters, true);
            changeSequence.setIpFilterEnabled(ipFilterState);
            break;
        case InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS:
            assertParameterCount(parameters, 1, token);
            enableImSshAccess(changeSequence, parameters);
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_REQUEST_TIMEOUT:
            assertParameterCount(parameters, 1, token);
            if (!org.apache.commons.lang3.StringUtils.isNumeric(parameters.get(0))) {
                throw CommandException.syntaxError("Unexpected parameter type. Timeout must be a numeric value.", currentContext.get());
            }
            long timeout = Long.parseLong(parameters.get(0));
            changeSequence.setRequestTimeout(timeout);
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_FORWARDING_TIMEOUT:
            assertParameterCount(parameters, 1, token);
            if (!org.apache.commons.lang3.StringUtils.isNumeric(parameters.get(0))) {
                throw CommandException.syntaxError("Unexpected parameter type. Timeout must be a numeric value.", currentContext.get());
            }
            long fTimeout = Long.parseLong(parameters.get(0));
            changeSequence.setForwardingTimeout(fTimeout);
            break;
        case InstanceManagementConstants.SUBCOMMAND_ADD_ALLOWED_INBOUND_IP:
            assertParameterCount(parameters, 1, token);
            changeSequence.addAllowedInboundIP(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP:
            assertParameterCount(parameters, 1, token);
            changeSequence.removeAllowedInboundIP(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_ADD_SSH_CONNECTION:
            assertParameterCount(parameters, 5, token); // id, displayName, host, port, loginName
            changeSequence.addSshConnectionFromStringParameters(parameters);
            break;
        case InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_CONNECTION:
            assertParameterCount(parameters, 1, token);
            changeSequence.removeSshConnection(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_ADD_UPLINK_CONNECTION:
            assertParameterCount(parameters, 10, token);
            changeSequence.addUplinkConnectionFromStringParameters(parameters);
            break;
        case InstanceManagementConstants.SUBCOMMAND_REMOVE_UPLINK_CONNECTION:
            assertParameterCount(parameters, 1, token);
            changeSequence.removeUplinkConnection(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_PUBLISH_COMPONENT:
            assertParameterCount(parameters, 1, token);
            changeSequence.publishComponent(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_UNPUBLISH_COMPONENT:
            assertParameterCount(parameters, 1, token);
            changeSequence.unpublishComponent(parameters.get(0));
            break;
        case InstanceManagementConstants.SUBCOMMAND_SET_BACKGROUND_MONITORING:
            assertParameterCount(parameters, 2, token);
            if (!org.apache.commons.lang3.StringUtils.isNumeric(parameters.get(1))) {
                throw CommandException.syntaxError("Unexpected parameter type. Interval must be a numeric value.", currentContext.get());
            }
            changeSequence.setBackgroundMonitoring(parameters.get(0), Integer.parseInt(parameters.get(1)));
            break;
        default:
            throw CommandException.syntaxError("Unexpected configuration command " + token, currentContext.get());
        }
    }

    private void enableImSshAccess(InstanceConfigurationOperationSequence changeSequence, List<String> parameters)
        throws CommandException {
        if (!org.apache.commons.lang3.StringUtils.isNumeric(parameters.get(0))) {
            throw CommandException.syntaxError("Unexpected parameter type. Port must be a numeric value.", currentContext.get());
        }
        final int accessPort = Integer.parseInt(parameters.get(0));
        changeSequence.enableImSshAccessWithDefaultRole(accessPort);
    }

    private void configureSshServer(InstanceConfigurationOperationSequence changeSequence, List<String> parameters)
        throws CommandException {
        final String ipAddress = parameters.get(0);
        if (!validateIpAddress(ipAddress)) {
            throw CommandException.syntaxError(ipAddress + " is not a valid IP address.", currentContext.get());
        }

        if (!org.apache.commons.lang3.StringUtils.isNumeric(parameters.get(1))) {
            throw CommandException.syntaxError("The SSH port must be a numeric value.", currentContext.get());
        }
        final int sshServerPort = Integer.parseInt(parameters.get(1)); // note: may still fail with overflow

        changeSequence.enableSshServer(ipAddress, sshServerPort);
    }

    private boolean parseSingleBooleanParameter(List<String> parameters, boolean defaultValue) throws CommandException {
        if (parameters.isEmpty()) {
            return defaultValue;
        } else if (parameters.get(0).equals("true")) {
            return true;
        } else if (parameters.get(0).equals("false")) {
            return false;
        } else {
            throw CommandException
                .syntaxError("Invalid parameter (expected 'true' or 'false'): " + parameters.get(0), currentContext.get());
        }
    }

    private void assertParameterCount(List<String> parameters, int min, String commandToken)
        throws CommandException {
        assertParameterCount(parameters, min, min, commandToken);
    }

    private void assertParameterCount(List<String> parameters, int min, int max, String commandToken) throws CommandException {
        int actual = parameters.size();
        if (actual < min || max < actual) {
            throw CommandException.syntaxError(StringUtils.format(
                "Wrong number of parameters for the %s command: expected between %d and %d parameters, but found %d: %s", commandToken,
                min, max, actual, Arrays.toString(parameters.toArray())), currentContext.get());
        }
    }

    private boolean isASubCommandToken(String token) {
        return token != null && token.startsWith("--");
    }

    private boolean isAParameterToken(String token) {
        return token != null && !token.startsWith("--");
    }

    private void performStart(CommandContext context) throws CommandException {
        final ParameterParser parameters = new ParameterParser(context, true, true);
        String commandArguments;
        if (parameters.getCommandArguments() == null) {
            commandArguments = "";
        } else {
            commandArguments = parameters.getCommandArguments();
        }
        triggerStartOfInstances(parameters.getInstallationId(), parameters.getInstanceIds(), parameters.getTimeout(),
            parameters.getStartWithGUI(), commandArguments, context);
    }

    private void performRestart(CommandContext context) throws CommandException {
        final ParameterParser parameters = new ParameterParser(context, true, true);
        triggerStopOfInstances(parameters.getInstanceIds(), parameters.getTimeout(), context);
        triggerStartOfInstances(parameters.getInstallationId(), parameters.getInstanceIds(), parameters.getTimeout(),
            parameters.getStartWithGUI(), parameters.getCommandArguments(), context);
    }

    private void performStop(CommandContext context) throws CommandException {
        final ParameterParser parameters = new ParameterParser(context, true, false);
        triggerStopOfInstances(parameters.getInstanceIds(), parameters.getTimeout(), context);
    }

    private void performStopAll(CommandContext context) throws CommandException {
        final ParameterParser parameters = new ParameterParser(context, false, null);
        String installationId = parameters.getInstallationId(); // optional; may be null
        if (installationId == null) {
            installationId = ""; // TODO backwards compatibility; change service code to use null instead?
        }
        triggerStopOfAllInstances(installationId, parameters.getTimeout(), context);
    }

    private void performStartAll(CommandContext context) throws CommandException {
        final ParameterParser parameters = new ParameterParser(context, false, true);
        // TODO use GUI parameter?
        String commandArguments;
        if (parameters.getCommandArguments() == null) {
            commandArguments = "";
        } else {
            commandArguments = parameters.getCommandArguments();
        }
        triggerStartOfAllInstances(parameters.getInstallationId(), parameters.getTimeout(), commandArguments, context);
    }

    private void performList(CommandContext context) throws CommandException {
        String scope = context.consumeNextToken();
        if (scope == null) {
            // list ALL
            scope = ALL_MARKER_TOKEN;
        }
        if (("instances".equals(scope) || "installations".equals(scope) || "templates".equals(scope) || ALL_MARKER_TOKEN.equals(scope))
            && !context.hasRemainingTokens()) {
            try {
                instanceManagementService.listInstanceManagementInformation(scope, context.getOutputReceiver());
            } catch (IOException e) {
                throw CommandException.executionError(e.toString(), context);
            }
        } else {
            throw CommandException.syntaxError("Unknown parameter", context);
        }
    }

    private void performDispose(CommandContext context) throws CommandException {
        final ParameterParser parameters = new ParameterParser(context, true, false);
        for (String instanceId : parameters.getInstanceIds()) {
            try {
                instanceManagementService.disposeInstance(instanceId, context.getOutputReceiver());
            } catch (IOException e) {
                throw CommandException.executionError(e.toString(), context);
            }
            context.getOutputReceiver().addOutput("Instance '" + instanceId + "' disposed");
        }
    }

    private void performInformation(CommandContext context) throws CommandException {
        if (context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        instanceManagementService.showInstanceManagementInformation(context.getOutputReceiver());
    }

    private void triggerStartOfInstances(final String installationId, final List<String> instanceIdList, final long timeout,
        final boolean startWithGui, final String commandArguments, CommandContext context) throws CommandException {
        try {
            instanceManagementService.startInstance(installationId, instanceIdList, context.getOutputReceiver(),
                timeout, startWithGui, commandArguments);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    // TODO add GUI parameter?
    private void triggerStartOfAllInstances(final String installationId, final long timeout, final String commandArguments,
        CommandContext context)
        throws CommandException {
        try {
            instanceManagementService.startAllInstances(installationId, context.getOutputReceiver(), timeout, commandArguments);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void triggerStopOfInstances(final List<String> instanceIdList, final long timeout, CommandContext context)
        throws CommandException {
        try {
            instanceManagementService.stopInstance(instanceIdList, context.getOutputReceiver(), timeout);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void triggerStopOfAllInstances(String installationId, final long timeout, CommandContext context) throws CommandException {
        try {
            instanceManagementService.stopAllInstances(installationId, context.getOutputReceiver(), timeout);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

}
