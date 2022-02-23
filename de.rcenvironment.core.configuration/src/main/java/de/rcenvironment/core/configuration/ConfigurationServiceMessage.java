/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration;


/**
 * An error which occurred in a {@link ConfigurationService}.
 *
 * @author Christian Weiss
 */
public class ConfigurationServiceMessage {
    
    private final String message;
    
    public ConfigurationServiceMessage(final String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    

}
