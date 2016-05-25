/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.rbac;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Describes the common behavior of objects of the RBAC concept, e.g. a subject, role, permission.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public abstract class RBACObject implements Serializable {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7537369104628038291L;

    /**
     * Constant.
     */
    private static final String ASSERTIONS_PARAMETER_NULL = "The parameter \"%s\" must not be null.";

    /**
     * The ID of the RBAC object.
     */
    private final String myID;;

    /**
     * The name of the RBAC object.
     */
    private final String myDescription;

    /**
     * 
     * Creates a new RBAC object with an ID.
     * 
     * @param id The ID of the authorization object.
     */
    public RBACObject(String id) {
        this(id, "");
    }

    /**
     * 
     * Creates a new RBAC object with an ID and a description.
     * 
     * @param id The ID of the authorization object.
     * @param description The description of the authorization object.
     */
    public RBACObject(String id, String description) {

        Assertions.isDefined(id, StringUtils.format(ASSERTIONS_PARAMETER_NULL, "id"));

        myID = id;
        if (description == null) {
            myDescription = "";
        } else {
            myDescription = description;
        }
    }

    /**
     * Getter for Id.
     * 
     * @return The Id.
     */
    public String getID() {
        return myID;
    }

    /**
     * Getter for description.
     * 
     * @return The description.
     */
    public String getDescription() {
        return myDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof RBACObject)) {
            return false;
        } else {
            return myID.equals(((RBACObject) o).getID());
        }
    }

    @Override
    public int hashCode() {
        return myID.hashCode();
    }

    @Override
    public String toString() {
        return myID;
    }

}
