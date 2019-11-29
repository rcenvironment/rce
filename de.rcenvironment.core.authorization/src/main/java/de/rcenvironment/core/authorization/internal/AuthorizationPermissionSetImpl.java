/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;

/**
 * Default {@link AuthorizationPermissionSet} implementation.
 *
 * @author Robert Mischke
 */
public class AuthorizationPermissionSetImpl implements AuthorizationPermissionSet {

    private static final char SIGNATURE_SEPARATOR_CHAR = ',';

    private static final String LOCAL_ONLY_SIGNATURE = AuthorizationConstants.GROUP_ID_LOCAL;

    private static final String PUBLIC_SIGNATURE = AuthorizationConstants.GROUP_ID_PUBLIC_IN_LOCAL_NETWORK;

    private final List<AuthorizationAccessGroup> groups;

    private final String signature;

    private boolean isPublic;

    /**
     * Convenience constructor to create a "public" (true) or "local only" (false) permission set.
     * 
     * @param isPublic whether this permission set should represent public access; otherwise, it represents local-only access
     */
    public AuthorizationPermissionSetImpl(boolean isPublic) {
        this.isPublic = isPublic;
        if (isPublic) {
            this.signature = PUBLIC_SIGNATURE;
            this.groups = AuthorizationConstants.GROUP_LIST_PUBLIC_IN_LOCAL_NETWORK;
        } else {
            this.signature = LOCAL_ONLY_SIGNATURE;
            this.groups = AuthorizationConstants.GROUP_LIST_LOCAL_ONLY;
        }
    }

    public AuthorizationPermissionSetImpl(AuthorizationAccessGroup[] groupList) {
        this(Arrays.asList(groupList));
    }

    /**
     * Internal constructor; the provided set is copied, not used.
     */
    public AuthorizationPermissionSetImpl(Collection<AuthorizationAccessGroup> groupList) {
        this.groups = new ArrayList<>(groupList);
        Collections.sort(this.groups); // important to make signature deterministic

        // build the group's "signature" based on the ordered list of contained groups
        if (groupList.isEmpty()) {
            this.signature = LOCAL_ONLY_SIGNATURE; // TODO generally improve, and make sure this cannot be abused
        } else {
            StringBuilder buffer = new StringBuilder();
            for (AuthorizationAccessGroup g : this.groups) {
                final String groupId = g.getFullId();
                if (groupId.indexOf(SIGNATURE_SEPARATOR_CHAR) >= 0) {
                    throw new IllegalArgumentException("Group id must not contain the character " + SIGNATURE_SEPARATOR_CHAR);
                }
                if (groupId.equals(LOCAL_ONLY_SIGNATURE)) {
                    throw new IllegalArgumentException("Group id must not be " + LOCAL_ONLY_SIGNATURE);
                }
                if (buffer.length() != 0) {
                    buffer.append(SIGNATURE_SEPARATOR_CHAR);
                }
                buffer.append(groupId);
            }
            this.signature = buffer.toString();
        }
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public Collection<AuthorizationAccessGroup> getAccessGroups() {
        return new ArrayList<>(groups);
    }

    @Override
    public boolean includesAccessGroup(AuthorizationAccessGroup group) {
        return groups.contains(group);
    }

    @Override
    // TODO include in unit test
    public AuthorizationPermissionSet intersectWith(AuthorizationPermissionSet otherPermissionSet) {
        final List<AuthorizationAccessGroup> newGroups = new ArrayList<>(groups);
        newGroups.retainAll(otherPermissionSet.getAccessGroups()); // intersects
        return new AuthorizationPermissionSetImpl(newGroups);
    }

    @Override
    public AuthorizationAccessGroup getArbitraryGroup() {
        if (groups.isEmpty()) {
            throw new IllegalStateException("Requested a group from an empty permission set");
        }
        return groups.get(0); // there is no criterion for picking another one, so simply take the first
    }

    @Override
    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public boolean isLocalOnly() {
        return groups.isEmpty();
    }

    @Override
    public String toString() {
        return signature;
    }

    @Override
    public int hashCode() {
        return signature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthorizationPermissionSetImpl other = (AuthorizationPermissionSetImpl) obj;
        return signature.equals(other.signature);
    }

}
