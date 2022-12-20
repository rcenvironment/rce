/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import de.rcenvironment.core.command.spi.BooleanParameter;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.NamedMultiParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.StringParameter;

public final class InstanceManagementParameters {
    
    // Parameters
    
    /**
     * 
     */
    public static final StringParameter ID_PARAMETER = new StringParameter(null, "id", "id parameter");

    /**
     * 
     */
    public static final StringParameter HOST_PARAMETER = new StringParameter(null, "host", "host parameter");
    
    /**
     * 
     */
    public static final StringParameter VERSION_PARAMETER = new StringParameter(null, "version", "version parameter");
    
    /**
     * 
     */
    public static final StringParameter TEMPLATE_PARAMETER = new StringParameter(null, "template name", "template name parameter");
    
    /**
     * 
     */
    public static final StringParameter NAME_PARAMETER = new StringParameter(null, "name", "name parameter");
    
    /**
     * 
     */
    public static final StringParameter COMMENT_PARAMETER = new StringParameter(null, "comment", "comment parameter");
    
    /**
     * 
     */
    public static final StringParameter NODE_ID_PARAMETER = new StringParameter(null, "node id", "node id parameter");
    
    /**
     * 
     */
    public static final StringParameter TEMPDIR_PATH_PARAMETER = new StringParameter(null, "tempdir", "tempdir parameter");
    
    /**
     * 
     */
    public static final StringParameter IP_PARAMETER = new StringParameter(null, "ip", "ip parameter");
    
    /**
     * 
     */
    public static final StringParameter USERNAME_PARAMETER = new StringParameter(null, "username", "username parameter");
    
    /**
     * 
     */
    public static final StringParameter ROLE_PARAMETER = new StringParameter(null, "role", "role parameter");
    
    /**
     * 
     */
    public static final StringParameter PASSWORD_PARAMETER = new StringParameter(null, "password", "password parameter");
    
    /**
     * 
     */
    public static final StringParameter DISPLAYNAME_PARAMETER = new StringParameter(null, "display name", "display name parameter");
    
    /**
     * 
     */
    public static final StringParameter HOSTNAME_PARAMETER = new StringParameter(null, "hostname", "hostname parameter");
    
    /**
     * 
     */
    public static final StringParameter LOGINNAME_PARAMETER = new StringParameter(null, "login name", "login name parameter");
    
    /**
     * 
     */
    public static final StringParameter CLIENT_ID_PARAMETER = new StringParameter(null, "client id", "client id parameter");
    
    /**
     * 
     */
    public static final StringParameter PASSWORD_KEYWORD = new StringParameter(null, "passwrod keyword", "password keyword");
    
    /**
     * 
     */
    public static final IntegerParameter PORT_PARAMETER = new IntegerParameter(null, "port", "port for the connection");
    
    /**
     * 
     */
    public static final IntegerParameter REQUEST_TIMEOUT_PARAMETER = new IntegerParameter(null, "r timeout", "request timeout in seconds");
    
    /**
     * 
     */
    public static final IntegerParameter FORWARDING_TIMEOUT_PARAMETER = new IntegerParameter(null, "f timeout",
            "forwarding timout in seconds");
    
    /**
     * 
     */
    public static final IntegerParameter INTERVAL_PARAMETER = new IntegerParameter(null, "interval", "interval in seconds");
    
    /**
     * 
     */
    public static final BooleanParameter AUTO_CONNECT_PARAMETER = new BooleanParameter(null, "auto-connect",
            "controls the isRelay property");

    /**
     * 
     */
    public static final BooleanParameter RELAY_OPTION_PARAMETER = new BooleanParameter(false, "is relay", "controls the isRelay property");
    
    /**
     * 
     */
    public static final BooleanParameter WORKFLOW_HOST_OPTION_PARAMETER = new BooleanParameter(null, "is worflow host",
            "controls the isWorflowHost property");
    
    /**
     * 
     */
    public static final BooleanParameter ENABLE_SSH_ACCOUNT_PARAMETER = new BooleanParameter(null, "enable ssh-account",
            "controls the ssh-account enabled property");
    
    /**
     * 
     */
    public static final BooleanParameter IP_FILTER_OPTION_PARAMETER = new BooleanParameter(true, "has ip-filter",
            "controls the hasIp-filter property");

    /**
     * 
     */
    public static final BooleanParameter IS_GATEWAY_PARAMETER = new BooleanParameter(null, "is gateway",
            "controls the isGateway property");
    
    /**
     * 
     */
    public static final BooleanParameter CONNECT_ON_STARTUP_PARAMETER = new BooleanParameter(true, "connect on startup",
            "controls the connectOnStartup property");
    
    /**
     * 
     */
    public static final BooleanParameter AUTO_RETRY_PARAMETER = new BooleanParameter(null, "auto-retry",
            "controls the auto-retry property");
    
    // CommandFlags
    
    /**
     * 
     */
    public static final CommandFlag RESET = new CommandFlag(
            InstanceManagementConstants.SUBCOMMAND_RESET, InstanceManagementConstants.SUBCOMMAND_RESET,
            "resets the instance to an empty configuration");
    
    /**
     * 
     */
    public static final CommandFlag WIPE = new CommandFlag(
            InstanceManagementConstants.SUBCOMMAND_WIPE, InstanceManagementConstants.SUBCOMMAND_WIPE, "wipes the instance");
    
    /**
     * 
     */
    public static final CommandFlag DISABLE_SSH_SERVER = new CommandFlag(
            InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER, InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER,
            "disables the ssh server");
    
    // NamedSingleParameters
    
    /**
     * 
     */
    public static final NamedSingleParameter REMOVE_CONNECTION = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_REMOVE_CONNECTION, "removes a connection", false, ID_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_RCE_VERSION = new NamedSingleParameter(
            InstanceManagementConstants.SET_RCE_VERSION, "sets the rce version of the instances. (Does not work on existing instances.)",
            false, VERSION_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter APPLY_TEMPLATE = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE, "applies (i.e. copies) the given template as the new configuration",
            false, TEMPLATE_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_NAME = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_NAME, "sets the name of the instance", false, NAME_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_COMMENT = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_COMMENT, "sets a general comment", false, COMMENT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_CUSTOM_NODE_ID = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_CUSTOM_NODE_ID, "adds an override value for the node's network id;"
                    + " use with caution!", false, NODE_ID_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_TEMPDIR_PATH = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_TEMPDIR_PATH, "sets the root path for RCE's temporary files directory",
            false, TEMPDIR_PATH_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter ENABLE_IM_SSH_ACCESS = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS, "enables and configures SSH forwarding of RCE console "
            + "commands by the IM \"master\" instance", false, PORT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter REMOVE_SSH_ACCOUNT = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_ACCOUNT, "removes an SSH account", false, USERNAME_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_REQUEST_TIMEOUT = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_REQUEST_TIMEOUT, "sets the request timeout in msec", false,
            REQUEST_TIMEOUT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter SET_FORWARDING_TIMEOUT = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_FORWARDING_TIMEOUT, "sets the forwarding timeout in msec",
            false, FORWARDING_TIMEOUT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter ADD_ALLOWED_INBOUND_IP = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_ADD_ALLOWED_INBOUND_IP, "adds/allows an inbound IP address to the filter",
            false, IP_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter REMOVE_ALLOWED_INBOUND_IP = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP, "removes/disallows an inbound IP address from the filter",
            false, IP_PARAMETER);
    /**
     * 
     */
    public static final NamedSingleParameter REMOVE_SSH_CONNECTION = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_CONNECTION, "removes a ssh connection", false, NAME_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter REMOVE_UPLINK_CONNECTION = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_REMOVE_UPLINK_CONNECTION, "removes an uplink connection", false, ID_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter PUBLISH_COMPONENT = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_PUBLISH_COMPONENT, "publishes a new component", false, NAME_PARAMETER);
    
    /**
     * 
     */
    public static final NamedSingleParameter UNPUBLISH_COMPONENT = new NamedSingleParameter(
            InstanceManagementConstants.SUBCOMMAND_UNPUBLISH_COMPONENT, "unpublishes a component", false, NAME_PARAMETER);
    
    // NamedMultiParameters
    
    /**
     * 
     */
    public static final NamedMultiParameter ADD_CONNECTION = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_ADD_CONNECTION, "adds new connection to the given ip/hostname and port,"
            + " and whether it should auto-connect", false, 4, ID_PARAMETER, HOST_PARAMETER, PORT_PARAMETER,
            AUTO_CONNECT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter ADD_SERVER_PORT = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_ADD_SERVER_PORT, "adds a new server port and sets the ip and port number"
            + " to bind to", false, 3, ID_PARAMETER, HOST_PARAMETER, PORT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter SET_RELAY_OPTION = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION, "sets or clears the relay flag", false, 0, RELAY_OPTION_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter SET_WORKFLOW_HOST_OPTION = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_WORKFLOW_HOST_OPTION, "sets or clears the workflow host flag", false, 0,
            WORKFLOW_HOST_OPTION_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter CONFIGURE_SSH_SERVER = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_CONFIGURE_SSH_SERVER, "enables the ssh server and sets the ip and port to bind to",
            false, 2, IP_PARAMETER, PORT_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter ADD_SSH_ACCOUNT = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_ADD_SSH_ACCOUNT, "adds an SSH account", false, 4, USERNAME_PARAMETER, ROLE_PARAMETER,
            ENABLE_SSH_ACCOUNT_PARAMETER, PASSWORD_PARAMETER);
    /**
     * 
     */
    public static final NamedMultiParameter SET_IP_FILTER_OPTION = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_IP_FILTER_OPTION, "enables or disables the ip filter; default: true", false, 0,
            IP_FILTER_OPTION_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter ADD_SSH_CONNECTION = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_ADD_SSH_CONNECTION, "adds a new ssh connection", false, 5, NAME_PARAMETER,
            DISPLAYNAME_PARAMETER, HOST_PARAMETER, PORT_PARAMETER, LOGINNAME_PARAMETER);
    
    /**
     * 
     */
    public static final NamedMultiParameter ADD_UPLINK_CONNECTION = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_ADD_UPLINK_CONNECTION, "adds a new uplink connection", false, 10, IP_PARAMETER,
            HOSTNAME_PARAMETER, PORT_PARAMETER, CLIENT_ID_PARAMETER, IS_GATEWAY_PARAMETER, CONNECT_ON_STARTUP_PARAMETER,
            AUTO_RETRY_PARAMETER, USERNAME_PARAMETER, PASSWORD_KEYWORD, PASSWORD_PARAMETER);
    /**
     * 
     */
    public static final NamedMultiParameter SET_BACKGROUNF_MONITORING = new NamedMultiParameter(
            InstanceManagementConstants.SUBCOMMAND_SET_BACKGROUND_MONITORING, "enables background monitoring with the given interval"
            + " (in seconds)", false, 2, ID_PARAMETER, INTERVAL_PARAMETER);
    
    private InstanceManagementParameters() {
    }
    
}
