/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration;

import java.util.EventObject;


/**
 * A error event in a {@link ConfigurationService}.
 *
 * @author Christian Weiss
 */
public class ConfigurationServiceMessageEvent extends EventObject {

    private static final long serialVersionUID = -1545958485394261322L;
    
    private final ConfigurationServiceMessage error;

    /**
     * The constructor.
     * 
     * @param source the {@link ConfigurationService} the error occurred in
     */
    public ConfigurationServiceMessageEvent(final ConfigurationService source, final ConfigurationServiceMessage error) {
        super(source);
        if (error == null) {
            throw new IllegalArgumentException("No message provided.");
        }
        this.error = error;
    }
    
    /**
     * {@inheritDoc}
     *
     * @see java.util.EventObject#getSource()
     */
    @Override
    public ConfigurationService getSource() {
        return (ConfigurationService) super.getSource();
    }

    /**
     * Returns the error.
     * 
     * @return the error
     */
    public ConfigurationServiceMessage getError() {
        return error;
    }

}
