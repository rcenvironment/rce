/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.executor;

/**
 * Constants for executor components.
 * @author Doreen Seider
 */
public final class SshExecutorConstants {

    /** Configuration key constant. */
    public static final String CONFIG_KEY_HOST = "host";

    /** Configuration key constant. */
    public static final String CONFIG_KEY_PORT = "port";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_AUTH_USER = "authUser";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_AUTH_PHRASE = "authPhrase";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_SANDBOXROOT = "sandboxRoot";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_DELETESANDBOX = "deleteSandbox";
    
    /** Configuration key constant. */
    public static final String CONFIG_KEY_SCRIPT = "script";

    private SshExecutorConstants() {}
    
}
