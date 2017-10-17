/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization;

import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;

/**
 * Generic interface that provides access to an {@link AuthorizationStore} and retrieves
 * authorization information which belongs to a specified {@link Subject}, e.g. a user.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public interface AuthorizationStore {

    /**
     * Key used for a service property.
     */
    String STORE = "store";

    /**
     * 
     * Initializes the underlying {@link AuthorizationStore}, e.g. create a connection to an LDAP
     * directory service or open an XML file.
     * 
     * @throws AuthorizationStoreException if accessing the {@link AuthorizationStore} fails.
     */
    void initialize() throws AuthorizationStoreException;

    /**
     * 
     * Looks up and retrieves the privileges of a specific user from the underlying
     * {@link AuthorizationStore}.
     * 
     * @param subjectID The ID of the {@link Subject}, e.g. the DN.
     * @return a {@link Subject}, e.g. representing a user.
     */
    Subject lookupSubject(String subjectID);

    /**
     * 
     * Looks up and retrieves the privileges of a specific user from the underlying
     * {@link AuthorizationStore}.
     * 
     * @param roleID The ID of the {@link Role}.
     * @return a {@link Subject}, e.g. representing a user.
     */
    Role lookupRole(String roleID);

    /**
     * 
     * Looks up and retrieves the privileges of a specific user from the underlying
     * {@link AuthorizationStore}.
     * 
     * @param permissionID The ID of the {@link Permission}.
     * @return a {@link Subject}, e.g. representing a user.
     */
    Permission lookupPermission(String permissionID);

}
