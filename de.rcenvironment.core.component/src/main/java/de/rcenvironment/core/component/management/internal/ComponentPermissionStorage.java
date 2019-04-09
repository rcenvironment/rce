/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import java.util.Map;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Persists and restores assignments between {@link ComponentAuthorizationSelector}s and {@link AuthorizationAccessGroup}s.
 *
 * @author Robert Mischke
 */
public interface ComponentPermissionStorage {

    /**
     * Adds the given assignment to the persistent storage.
     * 
     * @param selector the component's selector
     * @param permissions the set of groups to allow access to components matching the given selector
     * @throws OperationFailureException on errors while persisting the entry
     */
    void persistAssignment(ComponentAuthorizationSelector selector, AuthorizationPermissionSet permissions)
        throws OperationFailureException;

    /**
     * Restores all persisted assignments.
     * 
     * @return the restored assignments
     */
    Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> restorePersistedAssignments();

}
