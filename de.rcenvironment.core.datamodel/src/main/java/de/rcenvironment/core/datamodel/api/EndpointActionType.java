/*
 * Copyright (C) 2006-2016 DLR, Germany
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
    ADD("Add", "Add..."),
    /** Edit. */
    EDIT("Edit", "Edit..."),
    /** Remove. */
    REMOVE("Remove", "Remove");
    
    /** The title. */
    private final String title;

    private String buttonText;

    /**
     * Instantiates a new type.
     * 
     * @param title the title
     */
    EndpointActionType(final String title, final String buttonText) {
        this.title = title;
        this.buttonText = buttonText;
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

    public String getButtonText() {
        return buttonText;
    }

}
