/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

import java.util.Collection;

/**
 * A set of {@link AuthorizationAccessGroup}s that typically defines which groups have access to a given resource. The set may be empty,
 * which typically restricts access to the local instance/node.
 *
 * @author Robert Mischke
 */
public interface AuthorizationPermissionSet {

    /**
     * @return the defining signature of this permission set; two sets that have the same signature are considered "equal". Can also be for
     *         simple output.
     */
    String getSignature();

    /**
     * @return the list of contained groups
     * 
     *         TODO define whether this list should be sorted or not; for now, assume it isn't - misc_ro
     */
    Collection<AuthorizationAccessGroup> getAccessGroups();

    /**
     * @param group a {@link AuthorizationAccessGroup} to test for
     * @return true if the given group (ie, a group object with the same id) is contained in this set
     */
    boolean includesAccessGroup(AuthorizationAccessGroup group);

    /**
     * @param otherPermissionSet the other permission set to intersect with
     * @return the intersection with the other set, ie a set containing only elements present in both
     */
    AuthorizationPermissionSet intersectWith(AuthorizationPermissionSet otherPermissionSet);

    /**
     * Returns any of the contained groups. For use cases where a ressouce can be accessed via any available group, but one must be chosen.
     * No assumptions should be made about the selection process, e.g. it does neither have to be stable nor random.
     * 
     * @return any of the contained groups
     */
    AuthorizationAccessGroup getArbitraryGroup();

    /**
     * @return true if this permission set represents public access; if true, then the list of groups contains exactly the "public" group
     *         element
     */
    boolean isPublic();

    /**
     * @return true if there are no {@link AuthorizationAccessGroup}s in this set, ie whether it represents local-only access.
     */
    boolean isLocalOnly();

}
