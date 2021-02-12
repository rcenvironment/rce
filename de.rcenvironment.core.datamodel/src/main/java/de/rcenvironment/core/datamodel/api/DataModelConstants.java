/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.datamodel.api;

/**
 * Common constants related to the data mode.
 * 
 * @author Doreen Seider
 */
public final class DataModelConstants {

    /**
     * Run counter of init run (no inputs processed, no outputs sent, no actual logic executed). 
     */
    public static final int INIT_RUN = 0;
    
    /**
     * Run counter of tear down run.
     */
    public static final int TEAR_DOWN_RUN = -1;
    
    private DataModelConstants() {}
}
