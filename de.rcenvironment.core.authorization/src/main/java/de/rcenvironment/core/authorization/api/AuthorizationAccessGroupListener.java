/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

import java.util.List;

/**
 * Listener interface to get informed of changes in the available set of {@link AuthorizationAccessGroup}s.
 *
 * @author Robert Mischke
 */
public interface AuthorizationAccessGroupListener {

    /**
     * Triggered on any change in the available set of {@link AuthorizationAccessGroup}s; the list is sent along for convenience and
     * consistency. Note that the provided set includes the "public" group, as this is the more typical use case.
     * 
     * @param accessGroups the updated list of available {@link AuthorizationAccessGroup}s
     */
    void onAvailableAuthorizationAccessGroupsChanged(List<AuthorizationAccessGroup> accessGroups);
}
