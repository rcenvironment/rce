/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.api;

/**
 * 
 * The direction of an endpoint action button.
 *
 * @author Marc Stammerjohann
 */
public enum EndpointActionType {
    /** Add. */
    ADD("Add"),
    /** Edit. */
    EDIT("Edit"),
    /** Remove. */
    REMOVE("Remove");
    
    /** The title. */
    private final String title;

    /**
     * Instantiates a new type.
     * 
     * @param title the title
     */
    private EndpointActionType(final String title) {
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
