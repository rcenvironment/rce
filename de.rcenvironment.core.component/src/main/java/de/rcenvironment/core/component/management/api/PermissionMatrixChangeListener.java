/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.api;

import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;

/**
 * A listener to conveniently keep track of changes in the available access groups, the locally registered components (in form of their
 * {@link ComponentAuthorizationSelector}s), or in the assignments of groups to selectors.
 *
 * @author Robert Mischke
 */
public interface PermissionMatrixChangeListener {

    /**
     * Fired on any permission-related change; the flags indicate what has changed.
     * 
     * @param accessGroupsChanged whether access groups have been added or removed from the available set
     * @param componentSelectorsChanged whether local components have been added or removed
     * @param assignmentsChanged whether an assignment of groups to component selectors has been modified
     */
    void onPermissionMatrixChanged(boolean accessGroupsChanged, boolean componentSelectorsChanged, boolean assignmentsChanged);

}
