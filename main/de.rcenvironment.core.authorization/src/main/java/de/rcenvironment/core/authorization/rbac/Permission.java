/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.rbac;

/**
 * A permission is an operation a subject is allowed to perform. Permission s are associated with
 * {@link Role}s. Permission objects will be instantiated by {@link AuthorizationStore}
 * implementations.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public final class Permission extends RBACObject {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7743680394826833516L;

    /**
     * 
     * Creates a new permission object.
     * 
     * @param id The ID of this permission.
     */
    public Permission(String id) {
        super(id);
    }

    /**
     * 
     * Creates a new permission object.
     * 
     * @param id The ID of this permission.
     * @param description The description of this permission.
     */
    public Permission(String id, String description) {
        super(id, description);
    }

}
