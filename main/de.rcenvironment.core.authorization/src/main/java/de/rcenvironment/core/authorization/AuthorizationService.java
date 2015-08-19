/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization;

import java.util.Set;

import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;

/**
 * Interface of the authorization service. It provides methods to query permissions, roles and
 * subjects. The underlying authorization concept is FlatRBAC.
 * 
 * @author Doreen Seider
 */
public interface AuthorizationService {

    /**
     * 
     * Checks if a {@link Subject} given by its ID has a specified {@link Permission}.
     * 
     * @param subjectID The ID of the {@link Subject}.
     * @param permission The {@link Permission} to check.
     * @return true if the {@link Subject} has the {@link Permission}, else false.
     */
    boolean hasPermission(String subjectID, Permission permission);

    /**
     * 
     * Returns all {@link Permission} objects of a {@link Subject}.
     * 
     * @param subjectID The ID of the {@link Subject}.
     * @return the {@link Permission}s.
     */
    Set<Permission> getPermissions(String subjectID);

    /**
     * 
     * Checks if a {@link Subject} owns a specified {@link Role}.
     * 
     * @param subjectID The ID of the {@link Subject}.
     * @param role The {@link Role} to check.
     * @return true if the {@link Subject} owns the {@link Role}, else false.
     */
    boolean hasRole(String subjectID, Role role);

    /**
     * 
     * Returns all {@link Role} objects of a {@link Subject}.
     * 
     * @param subjectID The ID of the {@link Subject}.
     * @return the {@link Role}s.
     */
    Set<Role> getRoles(String subjectID);

    /**
     * 
     * Returns a {@link Subject}.
     * 
     * @param subjectID The ID of the {@link Subject}.
     * @return the {@link Subject}.
     */
    Subject getSubject(String subjectID);

    /**
     * 
     * Returns a {@link Role}.
     * 
     * @param roleID The ID of the {@link Role}.
     * @return the {@link Role}.
     */
    Role getRole(String roleID);

    /**
     * 
     * Returns a {@link Permission}.
     * 
     * @param permissionID The ID of the {@link Permission}.
     * @return the {@link Subject}.
     * 
     */
    Permission getPermission(String permissionID);
}
