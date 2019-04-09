/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.authorization;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;

/**
 * Comparator for authorization groups.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 */
public class AuthorizationGroupViewerComparator extends ViewerComparator {

    /**
     * Sort direction alphabetically descending.
     */
    public static final int DESCENDING = -1;

    /**
     * Sort direction alphabetically ascending.
     */
    public static final int ASCENDING = 1;

    private int direction = ASCENDING;

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        AuthorizationAccessGroup group1 = (AuthorizationAccessGroup) e1;
        AuthorizationAccessGroup group2 = (AuthorizationAccessGroup) e2;
        int returncode = group1.compareToIgnoreCase(group2);
        if (direction == DESCENDING) {
            returncode = -returncode;
        }
        return returncode;
    }
}
