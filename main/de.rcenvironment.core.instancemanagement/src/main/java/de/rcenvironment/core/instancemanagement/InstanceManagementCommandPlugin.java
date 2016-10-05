/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.ConfigurationFlag;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationChangeEntry;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationChangeSequence;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationSshConnection;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * A {@link CommandPlugin} that provides instance management ("im") commands.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class InstanceManagementCommandPlugin implements CommandPlugin {

    private static final String SUBCOMMAND_ADD_CONNECTION = "--add-connection";

    private static final String SUBCOMMAND_ADD_SERVER_PORT = "--add-server-port";

    private static final String ROOT_COMMAND = "im";

    private static final String SUBCOMMAND_RESET = "--reset";

    private static final String SUBCOMMAND_APPLY_TEMPLATE = "--apply-template";

    private static final String SUBCOMMAND_SET_NAME = "--set-name";

    private static final String SUBCOMMAND_SET_COMMENT = "--set-comment";

    private static final String SUBCOMMAND_SET_RELAY_OPTION = "--set-relay-option";

    private static final String SUBCOMMAND_SET_WORKFLOW_HOST_OPTION = "--set-workflow-host-option";

    private static final String SUBCOMMAND_SET_TEMPDIR_PATH = "--set-tempdir-path";

    private static final String SUBCOMMAND_ENABLE_IM_SSH_ACCESS = "--enable-im-ssh-access";

    private static final String SUBCOMMAND_CONFIGURE_SSH_SERVER = "--configure-ssh-server";

    private static final String SUBCOMMAND_DISABLE_SSH_SERVER = "--disable-ssh-server";

    private static final String SUBCOMMAND_SET_IP_FILTER_ENABLED = "--set-ip-filter-enabled";

    private static final String ZERO_STRING = "0";

    private static final String COMMA_STRING = ",";

    private static final String ALL_MARKER_TOKEN = "all";

    private static final String IF_MISSING = "--if-missing";

    private static final String FORCE_DOWNLOAD = "--force-download";

    private static final String FORCE_REINSTALL = "--force-reinstall";

    private static final String TIMEOUT = "--timeout";

    private static final String TIMEOUT_DESCRIPTION =
        "--timeout - specifies the maximum length of time this command is allowed to run (in seconds) - default = 60s";

    private InstanceManagementService instanceManagementService;

    private ThreadLocal<CommandContext> currentContext = new ThreadLocal<>(); // to avoid lots of context passing, but still be thread-safe

    protected void bindInstanceManagementService(InstanceManagementService newInstance) {
        this.instanceManagementService = newInstance;
    }

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();

        contributions
            .add(new CommandDescription(ROOT_COMMAND + " install",
                "[--if-missing] [--force-download] [--force-reinstall] [--timeout] <url version id/part> <installation id>",
                true,
                "downloads and installs a new RCE installation",
                "--if-missing - download and install if and only if an installation with the same version is not present",
                "--force-download - forces the download of the installation files even if they are present in the current cache",
                "--force-reinstall - forces reinstallation even if the same version is already installed",
                TIMEOUT_DESCRIPTION));

        contributions
            .add(new CommandDescription(ROOT_COMMAND + " reinstall",
                "[--force-download] [--force-reinstall] [--timeout] <url version id/part> <installation id>",
                true,
                "stops all instances running the given installation id, downloads and installs the new RCE installation, and starts the "
                    + "instances again with the new installation",
                "--force-download - forces the download of the installation files even if they are present in the current cache",
                "--force-reinstall - forces reinstallation even if the same version is already installed",
                TIMEOUT_DESCRIPTION));

        contributions
            .add(new CommandDescription(ROOT_COMMAND + " configure",
                "<instance id(s)> <command> [<parameters>] [<command> [<parameters>]] [...]",
                true,
                "configures the configuration.json file of the specified RCE instance(s)",

                "Available commands:",
                SUBCOMMAND_RESET + " - resets the instance to an empty configuration",
                SUBCOMMAND_APPLY_TEMPLATE + " <template id> - applies (i.e. copies) the given template as the new configuration",

                SUBCOMMAND_SET_NAME + " <name> - sets the name of the instance",
                SUBCOMMAND_SET_COMMENT + " <comment> - sets a general comment",
                SUBCOMMAND_SET_WORKFLOW_HOST_OPTION + " [<true/false>] - sets or clears the workflow host flag",
                SUBCOMMAND_SET_RELAY_OPTION + " [<true/false>] - sets or clears the relay flag",
                SUBCOMMAND_SET_TEMPDIR_PATH + " <path> - sets the root path for RCE's temporary files directory",

                SUBCOMMAND_ADD_SERVER_PORT + " <id> <ip> <port> - adds a new server port and sets the ip and port number to bind to",
                // TODO restore reconnect parameters
                SUBCOMMAND_ADD_CONNECTION + " <id> <host> <port> <true/false> - adds new connection "
                    + "to the given ip/hostname and port, and whether it should auto-connect",

                SUBCOMMAND_SET_IP_FILTER_ENABLED + " [<true/false>] - enables or disables the ip filter; default: true",
                SUBCOMMAND_ENABLE_IM_SSH_ACCESS + " <port> - enables and configures SSH forwarding of "
                    + "RCE console commands by the IM \"master\" instance",

                SUBCOMMAND_CONFIGURE_SSH_SERVER + " <ip> <port> - enables the ssh server and sets the ip and port to bind to",
                SUBCOMMAND_DISABLE_SSH_SERVER + " - disables the ssh server"

            // from here: new or reworked commands, but handling not adapted/implemented yet

            ));

        // from here: not checked or migrated yet

        // "--set-request-timeout <timeout> - sets the request timeout in msec",
        // "--set-forwarding-timeout <timeout> - sets the forwarding timeout in msec",
        // + "<autoRetryInitialDelay> <autoRetryMaximumDelay> <atuoRetryDelayMultiplier> - adds new connection",
        // "--remove-connection <name> - removes a connection",
        // "--remove-server-port <name> - removes the desired server port",
        // "--add-allowed-inbound-ip <ip> - adds/allows an inbound IP address to the filter",
        // "--remove-allowed-inbound-ip <ip> - removes/disallows an inbound IP address from the filter",
        // "--add-ssh-connection <name> <displayName> <host> <port> <loginName> - adds a new ssh connection",
        // "--remove-ssh-connection <name> - removes a ssh connection",
        // "--publish-component <name> - publishes a new component",
        // "--unpublish-component <name> - unpublishes a component",
        // "--set-ssh-server-ip <ip> - sets the ssh server ip adress",
        // "--set-ssh-server-port <port> - sets the ssh server port"));

        contributions.add(new CommandDescription(ROOT_COMMAND + " start",
            "[--timeout] <instance id1, instance id2, ...> <installation id>",
            true,
            "starts a list of new RCE instances with the desired instance IDs and the desired installation",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " stop",
            "[--timeout] <instance id1, instance id2, ...>",
            true,
            "stops a list of running RCE instances",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " start all",
            "[--timeout] <installation id>",
            true,
            "starts all available instances with the desired installation",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " stop all",
            "[--timeout] <installation id>",
            true,
            "stops all running instances",
            "<installation id> - optional parameter if one want to stop all running instances of a specific installation",
            TIMEOUT_DESCRIPTION));

        contributions.add(new CommandDescription(ROOT_COMMAND + " restart",
            " <instance id1, instance id2, ...> <installation id>",
            true,
            "restarts a list of RCE instances with the given instance IDs and the given installation"));

        contributions.add(new CommandDescription(ROOT_COMMAND + " dispose",
            "<instance id>",
            true,
            "disposes the specified instance meaning deletion of the profile directory"));

        contributions.add(new CommandDescription(ROOT_COMMAND + " list",
            "[--instances] [--installations] [--templates]",
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
        InstallationPolicy policy = InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT;
        if (context.consumeNextTokenIfEquals(FORCE_DOWNLOAD)) {
            policy = InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL;
        } else if (context.consumeNextTokenIfEquals(FORCE_REINSTALL)) {
            policy = InstallationPolicy.FORCE_REINSTALL;
        } else if (context.consumeNextTokenIfEquals(IF_MISSING)) {
            policy = InstallationPolicy.ONLY_INSTALL_IF_NOT_PRESENT;
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
                    InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL, context.getOutputReceiver(), 0);
                break;
            case FORCE_REINSTALL:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.FORCE_REINSTALL, context.getOutputReceiver(), 0);
                break;
            case IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver(), 0);
                break;
            case ONLY_INSTALL_IF_NOT_PRESENT:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.ONLY_INSTALL_IF_NOT_PRESENT, context.getOutputReceiver(), 0);
                break;
            default:
                instanceManagementService.setupInstallationFromUrlQualifier(installationId, urlQualifier,
                    InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, context.getOutputReceiver(), 0);
                break;
            }
        } catch (IOException e) {
            throw CommandException.executionError("Error during installation setup process: " + e.getMessage(), context);
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

    private void performConfigure(CommandContext context) throws CommandException {
        currentContext.set(context); // TODO use for other commands as well?
        String instanceIds = context.consumeNextToken();
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

        ConfigurationChangeSequence changeSequence = new ConfigurationChangeSequence();
        while (context.hasRemainingTokens()) {
            String commandToken = context.consumeNextToken();
            if (!isASubCommandToken(commandToken)) {
                throw CommandException.syntaxError("Internal consistency error: received an unexpected non-command token ", context);
            }
            List<String> parameters = new ArrayList<>();
            while (isAParameterToken(context.peekNextToken())) {
                parameters.add(context.consumeNextToken());
            }
            handleSubCommand(changeSequence, commandToken, parameters);
        }

        try {
            instanceManagementService.configureInstance(instanceIds, changeSequence, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void handleSubCommand(ConfigurationChangeSequence changeSequence, String token, List<String> parameters)
        throws CommandException {
        switch (token) {
        case SUBCOMMAND_RESET:
            assertParameterCount(parameters, 0, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.RESET_CONFIGURATION, Object.class,
                null));
            break;
        case SUBCOMMAND_APPLY_TEMPLATE:
            assertParameterCount(parameters, 1, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.APPLY_TEMPLATE, String.class,
                parameters.get(0)));
            break;
        case SUBCOMMAND_SET_NAME:
            assertParameterCount(parameters, 1, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_NAME, String.class, parameters.get(0)));
            break;
        case SUBCOMMAND_SET_COMMENT:
            assertParameterCount(parameters, 1, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_COMMENT, String.class, parameters.get(0)));
            break;
        case SUBCOMMAND_SET_RELAY_OPTION:
            assertParameterCount(parameters, 0, 1, token);
            if (parseSingleBooleanParameter(parameters, true)) { // TODO merge derived configuration keys?
                changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_RELAY, Boolean.class, true));
            } else {
                changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.DISABLE_RELAY, Boolean.class, false));
            }
            break;
        case SUBCOMMAND_SET_WORKFLOW_HOST_OPTION:
            assertParameterCount(parameters, 0, 1, token);
            if (parseSingleBooleanParameter(parameters, true)) { // TODO merge derived configuration keys?
                changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_WORKFLOWHOST, Boolean.class, true));
            } else {
                changeSequence
                    .append(new ConfigurationChangeEntry(ConfigurationFlag.DISABLE_WORKFLOWHOST, Boolean.class, false));
            }
            break;
        case SUBCOMMAND_SET_TEMPDIR_PATH:
            assertParameterCount(parameters, 1, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.TEMP_DIR, String.class, parameters.get(0)));
            break;
        case SUBCOMMAND_ADD_SERVER_PORT:
            assertParameterCount(parameters, 3, token); // id, host, port
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_SERVER_PORT, Object.class, parameters));
            break;
        case SUBCOMMAND_ADD_CONNECTION:
            assertParameterCount(parameters, 4, token); // id, host, port, autoconnect
            parameters.add(ZERO_STRING); // TODO dummy values for now
            parameters.add(ZERO_STRING);
            parameters.add(ZERO_STRING);
            addConnectionToConfig(parameters, changeSequence);
            break;
        case SUBCOMMAND_CONFIGURE_SSH_SERVER:
            assertParameterCount(parameters, 2, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_SSH_SERVER, Boolean.class,
                true));
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_SSH_SERVER_IP, String.class,
                parameters.get(0)));
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_SSH_SERVER_PORT, Integer.class,
                Integer.parseInt(parameters.get(1))));
            break;
        case SUBCOMMAND_DISABLE_SSH_SERVER:
            assertParameterCount(parameters, 0, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.DISABLE_SSH_SERVER, Boolean.class,
                false));
            break;
        case SUBCOMMAND_SET_IP_FILTER_ENABLED:
            assertParameterCount(parameters, 0, 1, token);
            if (parseSingleBooleanParameter(parameters, true)) { // TODO merge derived configuration keys?
                changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_IP_FILTER, Boolean.class, true));
            } else {
                changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.DISABLE_IP_FILTER, Boolean.class, false));
            }
            break;
        case SUBCOMMAND_ENABLE_IM_SSH_ACCESS:
            assertParameterCount(parameters, 1, token);
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_IM_SSH_ACCESS, Integer.class,
                Integer.parseInt(parameters.get(0))));
            break;
        // case SUBCOMMAND_:
        // assertParameterCount(parameters, 1, token);
        // break;
        default:
            throw CommandException.syntaxError("Unexpected configuration command " + token, currentContext.get());
        }

        // TODO unmigrated code below - for reference only
        if (true) {
            return;
        }
        int index = 0; // only to ensure compilation during migration
        List<String> tokens = new ArrayList<>(); // only to ensure compilation during migration

        if (token.equals("")) {
            index = 1; // dummy entry
        } else if (token.equals(ConfigurationFlag.ENABLE_DEP_INPUT_TAB.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_DEP_INPUT_TAB, Boolean.class, true));
        } else if (token.equals(ConfigurationFlag.DISABLE_DEP_INPUT_TAB.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.DISABLE_DEP_INPUT_TAB, Boolean.class,
                false));
        } else if (token.equals(ConfigurationFlag.REQUEST_TIMEOUT.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.REQUEST_TIMEOUT, Long.class,
                Long.parseLong(tokens.get(index + 1))));
        } else if (token.equals(ConfigurationFlag.FORWARDING_TIMEOUT.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.FORWARDING_TIMEOUT, Long.class,
                Long.parseLong(tokens.get(index + 1))));
        } else if (token.equals(ConfigurationFlag.REMOVE_CONNECTION.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.REMOVE_CONNECTION, String.class,
                tokens.get(index + 1)));
        } else if (token.equals(ConfigurationFlag.REMOVE_SERVER_PORT.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.REMOVE_SERVER_PORT, String.class,
                tokens.get(index + 1)));
        } else if (token.equals(ConfigurationFlag.ADD_ALLOWED_IP.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_ALLOWED_IP, String.class,
                parameters.get(0)));
        } else if (token.equals(ConfigurationFlag.REMOVE_ALLOWED_IP.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.REMOVE_ALLOWED_IP, String.class,
                parameters.get(0)));
        } else if (token.equals(ConfigurationFlag.ADD_SSH_CONNECTION.getFlag())) {
            addSshConnectionToConfig(tokens, changeSequence, index);
            for (int i = 5; i < 1; i--) {
                tokens.remove(index + i);
            }
            index = 0;
        } else if (token.equals(ConfigurationFlag.REMOVE_SSH_CONNECTION.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.REMOVE_SSH_CONNECTION, String.class,
                parameters.get(0)));
        } else if (token.equals(ConfigurationFlag.PUBLISH_COMPONENT.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.PUBLISH_COMPONENT, String.class,
                parameters.get(0)));
        } else if (token.equals(ConfigurationFlag.UNPUBLISH_COMPONENT.getFlag())) {
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.UNPUBLISH_COMPONENT, String.class,
                parameters.get(0)));
        } else if (token.equals(ConfigurationFlag.SET_BACKGROUND_MONITORING.getFlag())) {
            Map<String, Integer> map = new HashMap<String, Integer>();
            map.put(parameters.get(0), Integer.parseInt(parameters.get(1)));
            changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_BACKGROUND_MONITORING, Object.class,
                map));
            tokens.remove(index + 1);
            tokens.remove(index + 2);
            index = 0;
        } else {
            throw CommandException.syntaxError("Unexpected token: " + token, currentContext.get());
        }
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

    private void addSshConnectionToConfig(List<String> tokens, ConfigurationChangeSequence changeSequence, int index) {
        ConfigurationSshConnection connection =
            new ConfigurationSshConnection(tokens.get(index + 1), tokens.get(index + 2), tokens.get(index + 3),
                Integer.parseInt(tokens
                    .get(index + 4)),
                tokens.get(index + 5));
        changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_SSH_CONNECTION,
            ConfigurationSshConnection.class, connection));
    }

    private void addConnectionToConfig(List<String> parameters, ConfigurationChangeSequence changeSequence) {
        de.rcenvironment.core.instancemanagement.internal.ConfigurationConnection configConnection =
            new de.rcenvironment.core.instancemanagement.internal.ConfigurationConnection(parameters.get(0),
                parameters.get(1), Integer.parseInt(parameters.get(2)), Boolean.parseBoolean(parameters.get(3)),
                Long.parseLong(parameters.get(4)), Long.parseLong(parameters.get(5)), Integer.parseInt(parameters.get(6)));
        changeSequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_CONNECTION,
            de.rcenvironment.core.instancemanagement.internal.ConfigurationConnection.class, configConnection));
    }

    private List<String> getInstanceIdList(List<String> tokens) {
        List<String> instanceIdList = new LinkedList<>();
        final String c = COMMA_STRING;
        for (String token : tokens) {
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
        return instanceIdList;
    }

    private void performStart(CommandContext context) throws CommandException {
        List<String> instanceIdList = new LinkedList<>();
        List<String> tokens = new LinkedList<>(context.consumeRemainingTokens());
        long timeout = getTimeoutIfSpecified(tokens, context);
        boolean startWithGui = getStartWithGUIIfSpecified(tokens, context);
        String installationId = ((LinkedList<String>) tokens).removeLast();
        instanceIdList = getInstanceIdList(tokens);
        if (instanceIdList.isEmpty() || installationId == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.startInstance(installationId, instanceIdList, context.getOutputReceiver(), timeout, startWithGui);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performRestart(CommandContext context) throws CommandException {
        List<String> instanceIdList = new LinkedList<>();
        List<String> tokens = new LinkedList<>(context.consumeRemainingTokens());
        long timeout = getTimeoutIfSpecified(tokens, context);
        boolean startWithGui = getStartWithGUIIfSpecified(tokens, context);
        String installationId = ((LinkedList<String>) tokens).removeLast();
        instanceIdList = getInstanceIdList(tokens);
        if (instanceIdList.isEmpty() || installationId == null || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.stopInstance(new LinkedList<String>(instanceIdList), context.getOutputReceiver(), timeout);
            instanceManagementService.startInstance(installationId, new LinkedList<String>(instanceIdList), context.getOutputReceiver(),
                timeout, startWithGui);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performStop(CommandContext context) throws CommandException {
        List<String> tokens = new LinkedList<>(context.consumeRemainingTokens());
        List<String> instanceIdList = new LinkedList<>();
        long timeout = getTimeoutIfSpecified(tokens, context);
        instanceIdList = getInstanceIdList(tokens);
        if (instanceIdList.isEmpty() || context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        try {
            instanceManagementService.stopInstance(instanceIdList, context.getOutputReceiver(), timeout);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private void performStopAll(CommandContext context) throws CommandException {
        List<String> tokens = new LinkedList<>(context.consumeRemainingTokens());
        long timeout = getTimeoutIfSpecified(tokens, context);
        if (tokens.size() > 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String installationId = "";
        if (!tokens.isEmpty()) {
            installationId = ((LinkedList<String>) tokens).removeLast();
        }

        try {
            instanceManagementService.stopAllInstances(installationId, context.getOutputReceiver(), timeout);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
    }

    private long getTimeoutIfSpecified(final List<String> tokens, CommandContext context) throws CommandException {
        long timeout = 0;
        if (tokens.contains(TIMEOUT)) {
            int index = tokens.indexOf(TIMEOUT);
            tokens.remove(index);
            try {
                String t = tokens.remove(index);
                if (org.apache.commons.lang3.StringUtils.isNumeric(t)) {
                    timeout = Long.parseLong(t);
                } else {
                    throw CommandException.executionError("No timeout specified", context);
                }
            } catch (IndexOutOfBoundsException e) {
                throw CommandException.executionError("No timeout specified", context);
            }
        }

        return timeout;
    }

    private boolean getStartWithGUIIfSpecified(final List<String> tokens, CommandContext context) throws CommandException {
        long timeout = 0;
        if (tokens.contains("--gui")) {
            int index = tokens.indexOf("--gui");
            tokens.remove(index);
            return true;
        }

        return false;
    }

    private void performStartAll(CommandContext context) throws CommandException {
        List<String> tokens = new LinkedList<>(context.consumeRemainingTokens());
        long timeout = getTimeoutIfSpecified(tokens, context);
        if (tokens.isEmpty()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String installationId = ((LinkedList<String>) tokens).removeLast();

        try {
            instanceManagementService.startAllInstances(installationId, context.getOutputReceiver(), timeout);
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        }
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
        List<String> tokens = new LinkedList<>(context.consumeRemainingTokens());
        List<String> instanceIds = getInstanceIdList(tokens);
        if (instanceIds.isEmpty()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        for (String instanceId : instanceIds) {
            try {
                instanceManagementService.disposeInstance(instanceId, context.getOutputReceiver());
            } catch (IOException e) {
                throw CommandException.executionError(e.toString(), context);
            }
            context.getOutputReceiver().addOutput("Instance with id: " + instanceId + " disposed");
            context.getOutputReceiver().addOutput("Done.");
        }
    }

    private void performInformation(CommandContext context) throws CommandException {
        if (context.hasRemainingTokens()) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        instanceManagementService.showInstanceManagementInformation(context.getOutputReceiver());
    }

}
