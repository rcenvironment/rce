/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

/**
 * Database management action types.
 *
 * @author Oliver Seebach
 */
public enum DatabaseManagementActionType {
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
    DatabaseManagementActionType(final String title) {
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
