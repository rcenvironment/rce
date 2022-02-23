/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.api;

/**
 * Defines a scope of users that are allowed to use/access certain resources within the distributed system.
 * <p>
 * Note that there is a special "public" group that replaces the pre-9.0 "published component" behavior. Combined with custom access groups,
 * this provides more fine-grained component access control.
 *
 * @author Robert Mischke
 */
// TODO this might also simply be called "AuthorizationGroup"; decide which one is better
public interface AuthorizationAccessGroup extends Comparable<AuthorizationAccessGroup> {

    /**
     * @return the user-given name of this group
     */
    String getName();

    /**
     * @return the additional id part of this group that is appended to the display name for near-uniqueness and invalidation on key
     *         material upgrades
     * 
     *         TODO 9.0.0: consider renaming to "suffix"?
     */
    String getIdPart();

    /**
     * @return the full internal id of this group, which is also used for equality checks; currently "name:id"
     */
    String getFullId();

    /**
     * @return the name of this group for end-user messages or lists; currently "name [id]"
     */
    String getDisplayName();

    /**
     * Compares this object with the specified object for order by ignoring cases in the object´s name.
     * 
     * @param o the object to compare
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    int compareToIgnoreCase(AuthorizationAccessGroup o);

}
