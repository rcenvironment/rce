/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

/**
 * A class containing constants for instance management.
 *
 * @author Brigitte Boden
 * @author David Scholz (moved subcommand string from {@link InstanceManagementCommandPlugin}).
 * @author Lukas Rosenbach
 */
public final class InstanceManagementConstants {

    /**
     * Adds a new connection.
     */
    public static final String SUBCOMMAND_ADD_CONNECTION = "--add-connection";

    /**
     * Adds a new connection.
     */
    public static final String SUBCOMMAND_REMOVE_CONNECTION = "--remove-connection";

    /**
     * Adds the server port.
     */
    public static final String SUBCOMMAND_ADD_SERVER_PORT = "--add-server-port";

    /**
     * Resets the config.
     */
    public static final String SUBCOMMAND_RESET = "--reset";
    
    /**
     * Wipes the config.
     */
    public static final String SUBCOMMAND_WIPE = "--wipe";
    
    /**
     * Set profile version.
     */
    public static final String SET_RCE_VERSION = "--set-rce-version";

    /**
     * Applies a template.
     */
    public static final String SUBCOMMAND_APPLY_TEMPLATE = "--apply-template";

    /**
     * Set the instance name in the config.
     */
    public static final String SUBCOMMAND_SET_NAME = "--set-name";

    /**
     * Sets a comment in the config.
     */
    public static final String SUBCOMMAND_SET_COMMENT = "--set-comment";

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_RELAY_OPTION = "--set-relay-option";

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_WORKFLOW_HOST_OPTION = "--set-workflow-host-option";

    /**
     * Sets a node id override value; typically used for automated testing.
     */
    public static final String SUBCOMMAND_SET_CUSTOM_NODE_ID = "--set-custom-node-id";

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_TEMPDIR_PATH = "--set-tempdir-path";

    /**
     * 
     */
    public static final String SUBCOMMAND_ENABLE_IM_SSH_ACCESS = "--enable-im-ssh-access";

    /**
     * 
     */
    public static final String SUBCOMMAND_CONFIGURE_SSH_SERVER = "--configure-ssh-server";

    /**
     * 
     */
    public static final String SUBCOMMAND_DISABLE_SSH_SERVER = "--disable-ssh-server";
    
    /**
     * 
     */
    public static final String SUBCOMMAND_ADD_SSH_ACCOUNT = "--add-ssh-account";
    
    /**
     * 
     */
    public static final String SUBCOMMAND_REMOVE_SSH_ACCOUNT = "--remove-ssh-account";
    

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_IP_FILTER_OPTION = "--set-ip-filter-option";

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_REQUEST_TIMEOUT = "--set-request-timeout";

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_FORWARDING_TIMEOUT = "--set-forwarding-timeout";

    /**
     * 
     */
    public static final String SUBCOMMAND_ADD_ALLOWED_INBOUND_IP = "--add-allowed-inbound-ip";

    /**
     * 
     */
    public static final String SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP = "--remove-allowed-inbound-ip";

    /**
     * 
     */
    public static final String SUBCOMMAND_ADD_SSH_CONNECTION = "--add-ssh-connection";

    /**
     * 
     */
    public static final String SUBCOMMAND_REMOVE_SSH_CONNECTION = "--remove-ssh-connection";
    
    /**
     * 
     */
    public static final String SUBCOMMAND_ADD_UPLINK_CONNECTION = "--add-uplink-connection";

    /**
     * 
     */
    public static final String SUBCOMMAND_REMOVE_UPLINK_CONNECTION = "--remove-uplink-connection";


    /**
     * 
     */
    public static final String SUBCOMMAND_PUBLISH_COMPONENT = "--publish-component";

    /**
     * 
     */
    public static final String SUBCOMMAND_UNPUBLISH_COMPONENT = "--unpublish-component";

    /**
     * 
     */
    public static final String SUBCOMMAND_SET_BACKGROUND_MONITORING = "--set-background-monitoring";

    /**
     * Constant.
     */
    public static final String IM_MASTER_USER_NAME = "im_master";

    /**
     * Constant.
     */
    public static final String IM_MASTER_ROLE = "instance_management_delegate_user";

    /**
     * Constant.
     */
    public static final String IM_MASTER_PASSPHRASE_KEY = "im_master_passphrase";

    /**
     * Constant.
     */
    public static final String LOCALHOST = "127.0.0.1";
    
    /**
     * Constant.
     */
    public static final String AUTHORIZATION_SEGMENT = "authorization";

    private InstanceManagementConstants() {}
}
