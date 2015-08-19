/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Roles are authorization related objects. They contain permissions to describe which operations
 * can be done by a {@link Subject} if it owns this {@link Role}. {@link Role} objects will be
 * instantiated by {@link AuthorizationStore} implementations.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public final class Role extends RBACObject {

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -4911570862906136144L;

    /**
     * The {@link Permission}s that are associated with this {@link Role}.
     */
    private final Set<Permission> myPermissions;

    /**
     * 
     * Creates a new {@link Role} with an ID and a set of {@link Permission}s.
     * 
     * @param id
     *            The ID of the {@link Role}.
     * @param permissions
     *            A set of {@link Permission}s for this {@link Role}.
     */
    public Role(String id, Set<Permission> permissions) {
        this(id, "", permissions);
    }

    /**
     * 
     * Creates a new {@link Role} with an ID, a description and a set of {@link Permission}s.
     * 
     * @param id
     *            The ID of the {@link Role}.
     * @param description
     *            The description of the {@link Role}.
     * @param permissions
     *            A collection of {@link Permission}s for this {@link Role}.
     */
    public Role(String id, String description, Set<Permission> permissions) {
        super(id, description);

        if (permissions == null) {
            myPermissions = new HashSet<Permission>();
        } else {
            myPermissions = permissions;
        }
    }

    /**
     * Returns the {@link Permission}s of this {@link Role}.
     * 
     * @return The {@link Permission}s of this {@link Role}.
     */
    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(myPermissions);
    }

    /**
     * Checks whether this {@link Role} has a given {@link Permission}.
     * 
     * @param permission The {@link Permission} to check.
     * @return True or false.
     */
    public boolean hasPermission(Permission permission) {
        return myPermissions.contains(permission);
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o instanceof Role) && (super.equals(o))) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        String representation = super.toString() + " - " + myPermissions.toString();

        return representation;
    }

}
