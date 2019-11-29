/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;

/**
 * Internal authorization constants. Note that these fields are only intended for direct usage within the authorization system; external
 * code should fetch these objects via the {@link DefaultAuthorizationObjects} interface.
 *
 * @author Robert Mischke
 */
public final class AuthorizationConstants {

    /**
     * The group id for local-only access. Note that this is only for reserving this id, and for displaying purposes; there is never an
     * actual group object with that id.
     */
    public static final String GROUP_ID_LOCAL = "local";

    /**
     * The group id for public access to a component within the local network. Unlike the "local" group, this is used by an actual
     * {@link AuthorizationAccessGroup}.
     */
    public static final String GROUP_ID_PUBLIC_IN_LOCAL_NETWORK = "public";

    /**
     * The predefined group object for "public" acess.
     */
    public static final AuthorizationAccessGroup GROUP_OBJECT_PUBLIC_IN_LOCAL_NETWORK;

    /**
     * An empty list of access groups; represents local-only access.
     */
    public static final List<AuthorizationAccessGroup> GROUP_LIST_LOCAL_ONLY;

    /**
     * A list containing the "public" access group.
     */
    public static final List<AuthorizationAccessGroup> GROUP_LIST_PUBLIC_IN_LOCAL_NETWORK;

    /**
     * A permission set representing local-only access; it contains {@link #GROUP_LIST_LOCAL_ONLY}.
     */
    public static final AuthorizationPermissionSet PERMISSION_SET_LOCAL_ONLY;

    /**
     * A permission set representing "public" access; it contains {@link #GROUP_LIST_PUBLIC_IN_LOCAL_NETWORK}.
     */
    public static final AuthorizationPermissionSet PERMISSION_SET_PUBLIC_IN_LOCAL_NETWORK;

    static {
        GROUP_OBJECT_PUBLIC_IN_LOCAL_NETWORK =
            new AuthorizationAccessGroupImpl(AuthorizationConstants.GROUP_ID_PUBLIC_IN_LOCAL_NETWORK, null,
                AuthorizationConstants.GROUP_ID_PUBLIC_IN_LOCAL_NETWORK, "Public Access", 1); // 1 = sort bias

        GROUP_LIST_LOCAL_ONLY =
            Collections.unmodifiableList(new ArrayList<AuthorizationAccessGroup>(0));

        List<AuthorizationAccessGroup> tempList = new ArrayList<AuthorizationAccessGroup>();
        tempList.add(GROUP_OBJECT_PUBLIC_IN_LOCAL_NETWORK);
        GROUP_LIST_PUBLIC_IN_LOCAL_NETWORK = Collections.unmodifiableList(tempList);

        PERMISSION_SET_LOCAL_ONLY = new AuthorizationPermissionSetImpl(false);
        PERMISSION_SET_PUBLIC_IN_LOCAL_NETWORK = new AuthorizationPermissionSetImpl(true);
    }

    private AuthorizationConstants() {}
}
