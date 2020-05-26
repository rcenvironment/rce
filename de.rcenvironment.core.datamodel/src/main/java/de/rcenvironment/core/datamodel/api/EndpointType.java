/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

/**
 * The direction of an endpoint.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public enum EndpointType {
    /** Inputs. */
    INPUT("Input"),
    /** Outputs. */
    OUTPUT("Output");
    
    /** The title. */
    private final String title;

    /**
     * Instantiates a new type.
     * 
     * @param title the title
     */
    EndpointType(final String title) {
        this.title = title;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return title;
    }
}
