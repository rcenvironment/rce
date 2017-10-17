/*
 * Copyright (C) 2006-2016 DLR, Germany
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
 * An {@link Subject} represents a user. Objects of this type can be used to verify that a
 * caller owns the {@link Permission}s to execute specified operations. This is done by checking the {@link Role}s
 * mapped to this {@link Subject}. {@link Subject} objects will be instantiated by {@link AuthorizationStore}
 * implementations.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public final class Subject extends RBACObject {

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = 6584554867139383429L;

    /**
     * The {@link Role}s associated with this {@link Subject}.
     */
    private final Set<Role> myRoles;

    /**
     * 
     * Creates a new {@link Subject} object with an ID and {@link Role}s.
     * 
     * @param id
     *            The ID of this {@link Subject} (e.g. DN).
     * @param roles
     *            The {@link Role}s associated with this {@link Subject}.
     */
    public Subject(String id, Set<Role> roles) {
        this(id, "", roles);
    }

    /**
     * 
     * Creates a new {@link Subject} object with an ID, a description and {@link Role}s.
     * 
     * @param id
     *            The ID of this {@link Subject} (e.g. DN).
     * @param description
     *            The description of this {@link Subject}.
     * @param roles
     *            The {@link Role}s associated with this {@link Subject}.
     */
    public Subject(String id, String description, Set<Role> roles) {
        super(id, description);
        if (roles == null) {
            myRoles = new HashSet<Role>();
        } else {
            myRoles = new HashSet<Role>(roles);
        }
    }

    /**
     * Checks if this {@link Subject} has a given {@link Role}.
     * 
     * @param role A role.
     * @return True or false.
     */
    public boolean hasRole(Role role) {
        return myRoles.contains(role);
    }

    /**
     * Returns all {@link Role}s of this {@link Subject}.
     * 
     * @return All {@link Role}s of this {@link Subject}.
     */
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(myRoles);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o instanceof Subject) && (super.equals(o))) {
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
        String representation = super.toString() + " - " + myRoles.toString();

        return representation;
    }

}
