/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

/**
 * Constants used during execution of workflows or components.
 * 
 * @author Doreen Seider
 */
public final class ExecutionConstants {

    /**
     * Key to identify a created component/workflow controller instance at the service registry.
     */
    public static final String EXECUTION_ID_OSGI_PROP_KEY = "rce.component.execution.id";

    /** Private Constructor. */
    private ExecutionConstants() {}
}
