/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.api;

import java.util.Map;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;

/**
 * A listener for component authorization changes that is guaranteed to be called *immediately* on authorization change methods. In other
 * words, a listener of this type is guaranteed to receive the most up-to-date authorization information available. It is also guaranteed to
 * receive an initial call to {@link #initialize() first, followed by a non-overlapping sequence of {@link #onComponentPermissionsChanged()}
 * calls.
 *
 * @author Robert Mischke
 */
public interface ComponentAuthorizationHighPriorityListener {

    /**
     * Synchronizes this listener with the current permission settings.
     * 
     * @param currentPermissions a map containing all current permissions
     */
    void initializeComponentPermissions(Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> currentPermissions);

    /**
     * Reports an updated or deleted permission setting. Note that removal of all remote permissions (ie "local only") may be represented
     * both by a null permission set, or an empty one.
     * 
     * @param selector the selector key that specifies which component(s) are affected
     * @param permissionSet the new permission set, or null if a permission was deleted (which implies "local only" access)
     */
    void onComponentPermissionChanged(ComponentAuthorizationSelector selector, AuthorizationPermissionSet permissionSet);
}
