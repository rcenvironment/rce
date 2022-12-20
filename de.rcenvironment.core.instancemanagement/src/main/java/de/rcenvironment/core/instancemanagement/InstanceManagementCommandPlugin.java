/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.BooleanParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.ListCommandParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.MultiStateParameter;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedBooleanParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.ParsedListParameter;
import de.rcenvironment.core.command.spi.ParsedMultiParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.instancemanagement.internal.InstanceConfigurationException;

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

    private static final int DEFAULT_TIMEOUT = 60;

    private static final String ROOT_COMMAND = "im";

    private static final String ALL_MARKER_TOKEN = "all";

    private static final String IF_MISSING = "if-missing";

    private static final String FORCE_DOWNLOAD = "force-download";

    private static final String FORCE_REINSTALL = "force-reinstall";

    private static final String TIMEOUT = "--timeout";

    private static final String OPTION_START_WITH_GUI = "--gui";

    private static final String COMMAND_ARGUMENTS = "--command-arguments";
    
    private static final String INSTANCES = "instances";
    
    private static final String INSTALLATIONS = "installations";

    private static final String TEMPLATES = "templates";

    private static final Pattern IP_ADDRESS_PATTERN =

        Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static final String TIMEOUT_DESCRIPTION =
        "specifies the maximum length of time this command is allowed to run (in seconds) - default = 60s";
    
    // Parameter
    
    private static final StringParameter MAJOR_VERSION_PARAMETER = new StringParameter(null, "major version",
            "syntax format: [<major version>]/<url version id/part>");
    
    private static final StringParameter INSTALLATION_ID_PARAMETER = new StringParameter(null, "installation id",
            "id for the installation");

    private static final StringParameter INSTANCE_ID_PARAMETER = new StringParameter(null, "instance id", "id for the instance");

    private static final StringParameter COMMAND_ARGUMENTS_PARAMETER = new StringParameter("", "command arguments",
            "arguments for instance commands");
    
    private static final IntegerParameter TIMEOUT_PARAMETER = new IntegerParameter(DEFAULT_TIMEOUT, "timeout",
            "time in seconds until timeout");
    
    private static final BooleanParameter START_WITH_GUI_PARAMETER = new BooleanParameter(false, "start with gui",
            "controls the startWithGui property");

    private static final ListCommandParameter INSTANCE_IDS_PARAMETER =
            new ListCommandParameter(INSTANCE_ID_PARAMETER, INSTANCES, "list of instances to manage");
    
    private static final NamedParameter NAMED_TIMEOUT_PARAMETER = new NamedSingleParameter(TIMEOUT, TIMEOUT_DESCRIPTION, TIMEOUT_PARAMETER);

    private static final NamedParameter NAMED_START_WITH_GUI = new NamedSingleParameter(OPTION_START_WITH_GUI,
            "option to start with gui - standard <false>", START_WITH_GUI_PARAMETER);
    
    private static final NamedParameter NAMED_COMMAND_ARGUMENTS = new NamedSingleParameter(COMMAND_ARGUMENTS,
            "additional command arguments", COMMAND_ARGUMENTS_PARAMETER);

    private static final MultiStateParameter POLICY_PARAMETER =
            new MultiStateParameter("install policy", "specify install policy",
                    IF_MISSING, FORCE_DOWNLOAD, FORCE_REINSTALL);
    
    private static final MultiStateParameter LIST_MODIFIER = new MultiStateParameter(
            "affected items", "specify group of affected items",
            ALL_MARKER_TOKEN, INSTANCES, INSTALLATIONS, TEMPLATES);
    
    private InstanceManagementService instanceManagementService;

    private ThreadLocal<CommandContext> currentContext = new ThreadLocal<>(); // to avoid lots of context passing, but still be thread-safe

    /**
     * OSGi-DS bind method; made public for access from test code.
     * 
     * @param newInstance the new instance to set
     */
    public void bindInstanceManagementService(InstanceManagementService newInstance) {
        this.instanceManagementService = newInstance;
    }
    
    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(ROOT_COMMAND, "Commands for instance-managing",
            "Commands for instance-managing", true,
            new SubCommandDescription("install", "downloads and installs a new RCE installation", this::performInstall,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        POLICY_PARAMETER,
                        MAJOR_VERSION_PARAMETER,
                        INSTALLATION_ID_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_TIMEOUT_PARAMETER
                    }
                ), true
            ),
            new SubCommandDescription("reinstall",
                "stops all instances running the given installation id, downloads and installs the new RCE"
                + "installation, and starts the instances again with the new installation", this::performReinstall,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        POLICY_PARAMETER,
                        MAJOR_VERSION_PARAMETER,
                        INSTALLATION_ID_PARAMETER
                    }
                ), true
            ),
            new SubCommandDescription("configure", "configures the configuration.json file of the specified RCE instance(s)",
                this::performConfigure,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTANCE_IDS_PARAMETER
                    },
                    new CommandFlag[] {
                        InstanceManagementParameters.RESET,
                        InstanceManagementParameters.WIPE,
                        InstanceManagementParameters.DISABLE_SSH_SERVER
                    },
                    new NamedParameter[] {
                        InstanceManagementParameters.SET_RCE_VERSION,
                        InstanceManagementParameters.APPLY_TEMPLATE,
                        InstanceManagementParameters.SET_NAME,
                        InstanceManagementParameters.SET_COMMENT,
                        InstanceManagementParameters.SET_RELAY_OPTION,
                        InstanceManagementParameters.SET_WORKFLOW_HOST_OPTION,
                        InstanceManagementParameters.SET_CUSTOM_NODE_ID,
                        InstanceManagementParameters.SET_TEMPDIR_PATH,
                        InstanceManagementParameters.ADD_SERVER_PORT,
                        InstanceManagementParameters.ADD_CONNECTION,
                        InstanceManagementParameters.REMOVE_CONNECTION,
                        InstanceManagementParameters.CONFIGURE_SSH_SERVER,
                        InstanceManagementParameters.ADD_SSH_ACCOUNT,
                        InstanceManagementParameters.REMOVE_SSH_ACCOUNT,
                        InstanceManagementParameters.SET_IP_FILTER_OPTION,
                        InstanceManagementParameters.ENABLE_IM_SSH_ACCESS,
                        InstanceManagementParameters.SET_REQUEST_TIMEOUT,
                        InstanceManagementParameters.SET_FORWARDING_TIMEOUT,
                        InstanceManagementParameters.ADD_ALLOWED_INBOUND_IP,
                        InstanceManagementParameters.REMOVE_ALLOWED_INBOUND_IP,
                        InstanceManagementParameters.ADD_SSH_CONNECTION,
                        InstanceManagementParameters.REMOVE_SSH_CONNECTION,
                        InstanceManagementParameters.ADD_UPLINK_CONNECTION,
                        InstanceManagementParameters.REMOVE_UPLINK_CONNECTION,
                        InstanceManagementParameters.PUBLISH_COMPONENT,
                        InstanceManagementParameters.UNPUBLISH_COMPONENT,
                        InstanceManagementParameters.SET_BACKGROUNF_MONITORING
                    }
                ), true
            ),
            new SubCommandDescription("start",
                "starts a list of new RCE instances with the desired instance IDs and the desired installation", this::performStart,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTANCE_IDS_PARAMETER,
                        INSTALLATION_ID_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_TIMEOUT_PARAMETER,
                        NAMED_START_WITH_GUI,
                        NAMED_COMMAND_ARGUMENTS
                    }
                ), true
            ),
            new SubCommandDescription("stop", "stops a list of running RCE instances", this::performStop,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTANCE_IDS_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_TIMEOUT_PARAMETER
                    }
                ), true
            ),
            new SubCommandDescription("start-all", "starts all available instances. Uses the given installation",
                this::performStartAll,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTALLATION_ID_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_TIMEOUT_PARAMETER,
                        NAMED_COMMAND_ARGUMENTS
                    }
                ), true
            ),
            new SubCommandDescription("stop-all", "stops all running instances", this::performStopAll,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTALLATION_ID_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_TIMEOUT_PARAMETER
                    }
                ), true
            ),
            new SubCommandDescription("restart",
                "restarts a list of RCE instances with the given instance IDs and the given installation", this::performRestart,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTANCE_IDS_PARAMETER,
                        INSTALLATION_ID_PARAMETER
                    },
                    new NamedParameter[] {
                        NAMED_TIMEOUT_PARAMETER,
                        NAMED_START_WITH_GUI,
                        NAMED_COMMAND_ARGUMENTS
                    }
                ), true
            ),
            new SubCommandDescription("dispose", "disposes the specified instance meaning deletion of the profile directory",
                this::performDispose,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        INSTANCE_IDS_PARAMETER
                    }
                ), true
            ),
            new SubCommandDescription("list", "lists information about instances, installations or templates", this::performList,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        LIST_MODIFIER
                    }
                ), true
            ),
            new SubCommandDescription("info", "shows additional information", this::performInformation, true)
        );
        
        return new MainCommandDescription[] { commands };
    }


    private void performInstall(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        InstallationPolicy policy = InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT;

        String policyParameter = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        
        if (policyParameter != null) {
            if (FORCE_DOWNLOAD.equals(policyParameter)) {
                policy = InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL;
            } else if (FORCE_REINSTALL.equals(policyParameter)) {
                policy = InstallationPolicy.FORCE_REINSTALL;
            } else if (IF_MISSING.equals(policyParameter)) {
                policy = InstallationPolicy.ONLY_INSTALL_IF_NOT_PRESENT;
            }
        }

        String urlQualifier = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(1)).getResult();
        String installationId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(2)).getResult();

        int timeout = ((ParsedIntegerParameter) modifiers.getCommandParameter(TIMEOUT)).getResult() * SECONDS_TO_MILLISECONDS;
        
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

    private void performReinstall(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        InstallationPolicy policy = InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT;

        String policyParameter = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        
        if (policyParameter != null) {
            if (FORCE_DOWNLOAD.equals(policyParameter)) {
                policy = InstallationPolicy.FORCE_NEW_DOWNLOAD_AND_REINSTALL;
            } else if (FORCE_REINSTALL.equals(policyParameter)) {
                policy = InstallationPolicy.FORCE_REINSTALL;
            } else if (IF_MISSING.equals(policyParameter)) {
                policy = InstallationPolicy.ONLY_INSTALL_IF_NOT_PRESENT;
            }
        }

        String urlQualifier = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(1)).getResult();
        String installationId = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(2)).getResult();

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
        
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        ParsedListParameter instanceIdsParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);
        
        if (instanceIdsParameter.getResult().size() > 1) {
            throw CommandException.executionError("Configuring multiple instance ids at once is not implemented yet", context);
        }
        
        List<String> instanceIds = instanceIdsParameter.getResult().stream()
                .map((p) -> ((ParsedStringParameter) p).getResult()).collect(Collectors.toList());

        final InstanceConfigurationOperationSequence changeSequence = instanceManagementService.newConfigurationOperationSequence();
        
        
        parseConfigurationSubCommad(changeSequence, modifiers);
        
        try {
            instanceManagementService.applyInstanceConfigurationOperations(instanceIds.get(0), changeSequence, context.getOutputReceiver());
        } catch (IOException e) {
            throw CommandException.executionError(e.toString(), context);
        } catch (InstanceConfigurationException e) {
            throw CommandException.executionError(e.getMessage(), context);
        }
    }
    
    private void parseConfigurationSubCommad(InstanceConfigurationOperationSequence changeSequence, ParsedCommandModifiers modifiers)
            throws CommandException {
        parseConfigurationSubCommandFlags(changeSequence, modifiers);
        Set<String> named = modifiers.getNamedParameterList();
        
        for (String name : named) {
            switch (name) {
            case InstanceManagementConstants.SET_RCE_VERSION:
                ParsedStringParameter versionParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.setProfileVersion(versionParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE:
                ParsedStringParameter templateNameParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.applyTemplate(templateNameParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_NAME:
                ParsedStringParameter nameParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.setName(nameParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_COMMENT:
                ParsedStringParameter commentParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.setComment(commentParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION:
                ParsedMultiParameter relayMultiParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedBooleanParameter  relayOptionParameter = (ParsedBooleanParameter) relayMultiParameter.getResult()[0];
                changeSequence.setRelayFlag(relayOptionParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_WORKFLOW_HOST_OPTION:
                ParsedMultiParameter workflowMultiParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedBooleanParameter workflowHostOptionParameter = (ParsedBooleanParameter) workflowMultiParameter.getResult()[0];
                changeSequence.setWorkflowHostFlag(workflowHostOptionParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_CUSTOM_NODE_ID:
                ParsedStringParameter customNodeIdParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.setCustomNodeId(customNodeIdParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_TEMPDIR_PATH:
                ParsedStringParameter tempDirParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.setTempDirPath(tempDirParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_SERVER_PORT:
                ParsedMultiParameter addServerParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedStringParameter idParameter = (ParsedStringParameter) addServerParameter.getResult()[0];
                ParsedStringParameter hostParameter = (ParsedStringParameter) addServerParameter.getResult()[1];
                ParsedIntegerParameter portParameter = (ParsedIntegerParameter) addServerParameter.getResult()[2];
                changeSequence.addServerPort(idParameter.getResult(), hostParameter.getResult(), portParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_CONNECTION:
                ParsedMultiParameter addConnectionParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedStringParameter connectionNameParameter = (ParsedStringParameter) addConnectionParameter.getResult()[0];
                ParsedStringParameter connectionHostParameter = (ParsedStringParameter) addConnectionParameter.getResult()[1];
                ParsedIntegerParameter connectionPortParameter = (ParsedIntegerParameter) addConnectionParameter.getResult()[2];
                ParsedBooleanParameter connectionAutoConnectParameter = (ParsedBooleanParameter) addConnectionParameter.getResult()[3];
                changeSequence.addNetworkConnection(connectionNameParameter.getResult(), connectionHostParameter.getResult(),
                        connectionPortParameter.getResult(), connectionAutoConnectParameter.getResult(), 5, 10 * 3, 3 / 2);
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_CONNECTION:
                ParsedStringParameter removeConnectionParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.removeConnection(removeConnectionParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_CONFIGURE_SSH_SERVER:
                ParsedMultiParameter configureSshServerParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedStringParameter configureSshIp = (ParsedStringParameter) configureSshServerParameter.getResult()[0];
                ParsedIntegerParameter configureSshPort = (ParsedIntegerParameter) configureSshServerParameter.getResult()[1];
                changeSequence.enableSshServer(configureSshIp.getResult(), configureSshPort.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_SSH_ACCOUNT:
                ParsedMultiParameter addSshAccountParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedStringParameter addSshAccountUsername = (ParsedStringParameter) addSshAccountParameter.getResult()[0];
                ParsedStringParameter addSshAccountRole = (ParsedStringParameter) addSshAccountParameter.getResult()[1];
                ParsedBooleanParameter addSshAccountEnabled = (ParsedBooleanParameter) addSshAccountParameter.getResult()[2];
                ParsedStringParameter addSshAccountPassword = (ParsedStringParameter) addSshAccountParameter.getResult()[3];
                changeSequence.addSshAccount(addSshAccountUsername.getResult(), addSshAccountRole.getResult(),
                        addSshAccountEnabled.getResult(), addSshAccountPassword.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_ACCOUNT:
                ParsedStringParameter removeSshAccountParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.removeSshAccount(removeSshAccountParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_IP_FILTER_OPTION:
                ParsedMultiParameter setIpFilterOptionParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedBooleanParameter setIpFilterOption = (ParsedBooleanParameter) setIpFilterOptionParameter.getResult()[0];
                changeSequence.setIpFilterEnabled(setIpFilterOption.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS:
                ParsedIntegerParameter enableSshAccessParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(name);
                changeSequence.enableImSshAccessWithDefaultRole(enableSshAccessParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_REQUEST_TIMEOUT:
                ParsedIntegerParameter setRequestTimeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(name);
                changeSequence.setRequestTimeout(setRequestTimeoutParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_FORWARDING_TIMEOUT:
                ParsedIntegerParameter setForwardingTimeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(name);
                changeSequence.setForwardingTimeout(setForwardingTimeoutParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_ALLOWED_INBOUND_IP:
                ParsedStringParameter addAllowedInboundIpParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.addAllowedInboundIP(addAllowedInboundIpParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP:
                ParsedStringParameter removeAllowedInboundIpParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.removeAllowedInboundIP(removeAllowedInboundIpParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_SSH_CONNECTION:
                ParsedMultiParameter addSshConnectionParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedStringParameter addSshId = (ParsedStringParameter) addSshConnectionParameter.getResult()[0];
                ParsedStringParameter addSshDisplayName = (ParsedStringParameter) addSshConnectionParameter.getResult()[1];
                ParsedStringParameter addSshHost = (ParsedStringParameter) addSshConnectionParameter.getResult()[2];
                ParsedIntegerParameter addSshPort = (ParsedIntegerParameter) addSshConnectionParameter.getResult()[3];
                ParsedStringParameter addSshLoginName = (ParsedStringParameter) addSshConnectionParameter.getResult()[4];
                changeSequence.addSshConnection(addSshId.getResult(), addSshDisplayName.getResult(), addSshHost.getResult(),
                        addSshPort.getResult(), addSshLoginName.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_CONNECTION:
                ParsedStringParameter removeSshConnectionParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.removeSshConnection(removeSshConnectionParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_UPLINK_CONNECTION:
                ParsedMultiParameter addUplinkConnectionParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                changeSequence.addUplinkConnection(addUplinkConnectionParameter);
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_UPLINK_CONNECTION:
                ParsedStringParameter removeUplinkConnectionParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.removeUplinkConnection(removeUplinkConnectionParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_PUBLISH_COMPONENT:
                ParsedStringParameter publishComponentParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.publishComponent(publishComponentParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_UNPUBLISH_COMPONENT:
                ParsedStringParameter unpublishComponentParameter = (ParsedStringParameter) modifiers.getCommandParameter(name);
                changeSequence.publishComponent(unpublishComponentParameter.getResult());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_BACKGROUND_MONITORING:
                ParsedMultiParameter setBackgroundMonitoringParameter = (ParsedMultiParameter) modifiers.getCommandParameter(name);
                ParsedStringParameter backgroundMonitorigId = (ParsedStringParameter) setBackgroundMonitoringParameter.getResult()[0];
                ParsedIntegerParameter backgroundMonitoringInterval =
                        (ParsedIntegerParameter) setBackgroundMonitoringParameter.getResult()[1];
                changeSequence.setBackgroundMonitoring(backgroundMonitorigId.getResult(), backgroundMonitoringInterval.getResult());
                break;
            default:
                throw CommandException.syntaxError("Unexpected configuration command " + name, currentContext.get());
            }
        }
    }

    private void parseConfigurationSubCommandFlags(InstanceConfigurationOperationSequence changeSequence, ParsedCommandModifiers modifiers)
            throws CommandException {
        Set<CommandFlag> activeFlags = modifiers.getActiveFlags();
        
        for (CommandFlag flag : activeFlags) {
            switch (flag.getLongFlag()) {
            case InstanceManagementConstants.SUBCOMMAND_WIPE:
                changeSequence.wipeConfiguration();
                break;
            case InstanceManagementConstants.SUBCOMMAND_RESET:
                changeSequence.resetConfiguration();
                break;
            case InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER:
                changeSequence.disableSshServer();
                break;
            default:
                throw CommandException.syntaxError("Unexpected configuration flag " + flag.getLongFlag(), currentContext.get());
            }
        }
    }
    
    private void performStart(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedListParameter instanceIdsParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter installationIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        ParsedIntegerParameter timeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(TIMEOUT);
        ParsedBooleanParameter startWithGUIParameter = (ParsedBooleanParameter) modifiers.getCommandParameter(OPTION_START_WITH_GUI);
        ParsedStringParameter commandArgumentsParameter = (ParsedStringParameter) modifiers.getCommandParameter(COMMAND_ARGUMENTS);
        
        List<String> instanceIds = instanceIdsParameter.getResult().stream()
                .map((p) -> ((ParsedStringParameter) p).getResult()).collect(Collectors.toList());
        
        triggerStartOfInstances(installationIdParameter.getResult(), instanceIds, timeoutParameter.getResult() * SECONDS_TO_MILLISECONDS,
            startWithGUIParameter.getResult(), commandArgumentsParameter.getResult(), context);
    }

    private void performRestart(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedListParameter instanceIdsParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);
        ParsedStringParameter installationIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(1);
        ParsedIntegerParameter timeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(TIMEOUT);
        ParsedStringParameter commandArgumentsParameter = (ParsedStringParameter) modifiers.getCommandParameter(COMMAND_ARGUMENTS);
        ParsedBooleanParameter startWithGUIParameter = (ParsedBooleanParameter) modifiers.getCommandParameter(OPTION_START_WITH_GUI);
        
        List<String> instanceIds = instanceIdsParameter.getResult().stream()
                .map((p) -> ((ParsedStringParameter) p).getResult()).collect(Collectors.toList());
        
        triggerStopOfInstances(instanceIds, timeoutParameter.getResult() * SECONDS_TO_MILLISECONDS, context);
        triggerStartOfInstances(installationIdParameter.getResult(), instanceIds, timeoutParameter.getResult() * SECONDS_TO_MILLISECONDS,
            startWithGUIParameter.getResult(), commandArgumentsParameter.getResult(), context);
    }

    private void performStop(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedListParameter instanceIdsParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);
        ParsedIntegerParameter timeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(TIMEOUT);
        
        List<String> instanceIds = instanceIdsParameter.getResult().stream()
                .map((p) -> ((ParsedStringParameter) p).getResult()).collect(Collectors.toList());
        
        triggerStopOfInstances(instanceIds, timeoutParameter.getResult() * SECONDS_TO_MILLISECONDS, context);
    }

    private void performStopAll(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter installationIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedIntegerParameter timeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(TIMEOUT);
        
        triggerStopOfAllInstances(installationIdParameter.getResult(), timeoutParameter.getResult() * SECONDS_TO_MILLISECONDS, context);
    }

    private void performStartAll(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter installationIdParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        ParsedIntegerParameter timeoutParameter = (ParsedIntegerParameter) modifiers.getCommandParameter(TIMEOUT);
        ParsedStringParameter commandArgumentsParameter = (ParsedStringParameter) modifiers.getCommandParameter(COMMAND_ARGUMENTS);
        
        // TODO use GUI parameter?
        
        triggerStartOfAllInstances(installationIdParameter.getResult(), timeoutParameter.getResult() * SECONDS_TO_MILLISECONDS,
                commandArgumentsParameter.getResult(), context);
    }

    private void performList(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        String scope = ((ParsedStringParameter) modifiers.getPositionalCommandParameter(0)).getResult();
        if ((INSTANCES.equals(scope) || "installations".equals(scope) || "templates".equals(scope) || ALL_MARKER_TOKEN.equals(scope))
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
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedListParameter instanceIdsParameter = (ParsedListParameter) modifiers.getPositionalCommandParameter(0);
        List<String> instanceIds = instanceIdsParameter.getResult().stream()
                .map((p) -> ((ParsedStringParameter) p).getResult()).collect(Collectors.toList());
        
        
        for (String instanceId : instanceIds) {
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
