/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;


class DistributedComponentEntryMock implements DistributedComponentEntry {

    private String displayName;

    private String nodeID;

    private DistributedComponentEntryType type;

    private String installationID;

    private String groupName;

    DistributedComponentEntryMock(String displayName, String nodeID, DistributedComponentEntryType type, String installationID,
        String groupName) {
        super();
        this.displayName = displayName;
        this.nodeID = nodeID;
        this.type = type;
        this.installationID = installationID;
        this.groupName = groupName;
    }
    
    @Override
    public DistributedComponentEntryType getType() {
        return type;
    }

    @Override
    public boolean isAccessible() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public ComponentInstallation getComponentInstallation() {
        return new ComponentInstallationMock(this);
    }

    @Override
    public ComponentRevision getComponentRevision() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ComponentInterface getComponentInterface() {
        return new ComponentInterfaceMock(this);
    }

    @Override
    public String getPublicationData() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public String getNodeId() {
        return nodeID;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public AuthorizationPermissionSet getDeclaredPermissionSet() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public AuthorizationPermissionSet getMatchingPermissionSet() {
        // no implementation needed for unit testing
        return null;
    }

    public String getInstallationID() {
        return installationID;
    }

    public String getGroupName() {
        return groupName;
    }

}
