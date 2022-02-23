/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.definitions.helper;

/**
 * providing constants for option. keys and default value.s.
 * 
 * @author Marlon Schroeter
 */
public final class ConnectionOptionConstants {

    /**
     * auto retry option.
     */
    public static final String AUTO_RETRY_FLAG = "autoRetry";

    /**
     * auto retry delay multiplier option.
     */
    public static final String AUTO_RETRY_DELAY_MULTIPLIER = "autoRetryDelayMultiplier";

    /**
     * auto retry initial delay option.
     */
    public static final String AUTO_RETRY_INITIAL_DELAY = "autoRetryInitialDelay";

    /**
     * auto retry max delay option.
     */
    public static final String AUTO_RETRY_MAX_DELAY = "autoRetryMaxDelay";

    /**
     * auto start option.
     */
    public static final String AUTO_START_FLAG = "autoStart";
    
    /**
     * client ID option.
     */
    public static final String CLIENT_ID = "clientId";
    
    /**
     * connection name option.
     */
    public static final String CONNECTION_NAME = "connectionName";
    
    /**
     * disbaled option.
     */
    public static final String DISABLED = "disabled";
    /**
     * display name option.
     */
    public static final String DISPLAY_NAME = "displayName";

    /**
     * gateway option.
     */
    public static final String GATEWAY_FLAG = "isGateway";

    /**
     * host option.
     */
    public static final String HOST = "host"; 

    /**
     * password option.
     */
    public static final String PASSWORD = "password"; 

    /**
     * port option.
     */
    public static final String PORT = "port";
    
    /**
     * port option.
     */
    public static final String RELAY = "relay";

    /**
     * server number option.
     */
    public static final String SERVER_NUMBER = "serverNumber";

    /**
     * user name option.
     */
    public static final String USER_NAME = "userName";

    /**
     * user role option.
     */
    public static final String USER_ROLE = "userRole";

    /**
     * default auto retry delay multiplier value.
     */
    public static final float AUTO_RETRY_DELAY_MULTIPLIER_DEFAULT = 1.5f;

    /**
     * default auto retry init delay value.
     */
    public static final int AUTO_RETRY_INIT_DELAY_DEFAULT = 5;

    /**
     * default auto retry max delay value.
     */
    public static final int AUTO_RETRY_MAX_DELAY_DEFAULT = 30;
    
    /**
     * default host value.
     */
    public static final String HOST_DEFAULT = "127.0.0.1";

    /**
     * default port value.
     */
    public static final String USER_NAME_DEFAULT = "userName";
    
    private ConnectionOptionConstants() {
    //empty since this class only provides constants  
    }

}
