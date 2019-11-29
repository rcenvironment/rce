/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;

/**
 * Default {@link AuthorizationAccessGroup} implementation.
 *
 * @author Robert Mischke
 */
public class AuthorizationAccessGroupImpl implements AuthorizationAccessGroup {

    private String name;

    private final String idPart;

    private final String fullId; // currently "name:idPart"

    private final String displayName; // currently "name [idPart]"

    private final int sortOrderingCategory; // for actively sorting certain groups to the top or bottom of standard groups

    /**
     * Standard constructor for local or remote group creation.
     * 
     * @param name the user-given name, e.g. "MyGroup"
     * @param idPart a randomly-generated id suffix to distinguish between different groups with the same display name
     * @param fullId the complete id used for equality checks and network information passing; currently consists of the display name, a
     *        separator, and the id part
     * @param the display name to show in user-facing messages and UIs
     */
    public AuthorizationAccessGroupImpl(String name, String idPart, String fullId, String displayName) {
        this.name = name;
        this.idPart = idPart;
        this.fullId = fullId;
        this.displayName = displayName;
        this.sortOrderingCategory = 0;
    }

    /**
     * Constructor for special groups, allowing to specify all fields individually.
     * 
     * @param name the user-given name, e.g. "MyGroup"
     * @param idPart a randomly-generated id suffix to distinguish between different groups with the same display name
     * @param fullId the complete id used for equality checks and network information passing; currently consists of the display name, a
     *        separator, and the id part
     * @param the display name to show in user-facing messages and UIs
     * @param sortOrderingCategory the sort ordering to apply between groups before sorting alphabetically; default is 0
     */
    public AuthorizationAccessGroupImpl(String name, String idPart, String fullId, String displayName, int sortOrderingCategory) {
        this.name = name;
        this.idPart = idPart;
        this.fullId = fullId;
        this.displayName = displayName;
        this.sortOrderingCategory = sortOrderingCategory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullId() {
        return fullId;
    }

    @Override
    public String getIdPart() {
        return idPart;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return fullId; // use "name:idPart" for log output
    }

    @Override
    public int compareTo(AuthorizationAccessGroup o) {
        AuthorizationAccessGroupImpl other = (AuthorizationAccessGroupImpl) o;
        int categoryBias = Integer.compare(this.sortOrderingCategory, other.sortOrderingCategory);
        if (categoryBias != 0) {
            return categoryBias;
        }
        return displayName.compareTo(o.getDisplayName());
    }

    @Override
    public int compareToIgnoreCase(AuthorizationAccessGroup o) {
        AuthorizationAccessGroupImpl other = (AuthorizationAccessGroupImpl) o;
        int categoryBias = Integer.compare(this.sortOrderingCategory, other.sortOrderingCategory);
        if (categoryBias != 0) {
            return categoryBias;
        }
        return displayName.compareToIgnoreCase(o.getDisplayName());
    }

    @Override
    public int hashCode() {
        return fullId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthorizationAccessGroupImpl other = (AuthorizationAccessGroupImpl) obj;
        return this.fullId.equals(other.fullId);
    }

}
