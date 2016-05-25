/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.commons;


/**
 * Contains common keys for the properties tables.
 *
 * @author Brigitte Boden
 */
public final class PropertiesKeys {

    /**
     * Key for the additional information in workflow run properties table.
     */
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    
    /**
     * Key for the aggregated error log in workflow run properties table.
     */
    public static final String ERROR_LOG_FILE = "wfErrorLogFile";
    
    /**
     * Key for the component log in component run properties table.
     */
    public static final String COMPONENT_LOG_FILE = "compLogFile";

    /**
     * Key for the component error log in component run properties table.
     */
    public static final String COMPONENT_LOG_ERROR_FILE = "compErrorLogFile";

    private PropertiesKeys(){}
}
