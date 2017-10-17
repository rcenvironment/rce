/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.instancemanagement;


/**
 * A class containing constants for instance management.
 *
 * @author Brigitte Boden
 */
public final class InstanceManagementConstants {
    

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
    public static final String IM_MASTER_ROLE_ALLOWED_COMMANDS = "(version.*)|(cn.*)|(components.*)|(net.*)|restart|"
        + "shutdown|stop|stats|(tasks.*)";
    
    /**
     * Constant.
     */
    public static final String IM_MASTER_PASSPHRASE_KEY = "im_master_passphrase";
    /**
     * Constant.
     */
    public static final String LOCALHOST = "127.0.0.1";
    

    private InstanceManagementConstants(){}
}
