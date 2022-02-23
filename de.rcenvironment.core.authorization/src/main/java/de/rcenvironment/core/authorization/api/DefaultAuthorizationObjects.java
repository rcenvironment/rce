/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

/**
 * Provides access to special groups and permission sets, especially "local" and "public".
 *
 * @author Robert Mischke
 */
public interface DefaultAuthorizationObjects {

    /**
     * @return the special access group that allows access from all nodes/instances in the local network (ie, connected by standard
     *         connections)
     */
    AuthorizationAccessGroup accessGroupPublicInLocalNetwork();

    /**
     * @return the permission set to only allow access from the local node/instance
     */
    AuthorizationPermissionSet permissionSetLocalOnly();

    /**
     * @return the permission set to allow access from all nodes/instances in the local network (ie, connected by standard connections); it
     *         contains one element, which is the "public" group (see
     *         {@link AuthorizationService#accessGroupPublicInLocalNetwork()})
     */
    AuthorizationPermissionSet permissionSetPublicInLocalNetwork();

}
