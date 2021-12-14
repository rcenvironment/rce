/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;


class ComponentInstallationMock implements ComponentInstallation {

    private DistributedComponentEntryMock distributedComponentEntryMock;

    ComponentInstallationMock(DistributedComponentEntryMock distributedComponentEntryMock) {
        super();
        this.distributedComponentEntryMock = distributedComponentEntryMock;
    }

    @Override
    public int compareTo(ComponentInstallation o) {
        // no implementation needed for unit testing
        return 0;
    }

    @Override
    public String getNodeId() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ComponentRevision getComponentRevision() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public ComponentInterface getComponentInterface() {
        return new ComponentInterfaceMock(distributedComponentEntryMock);
    }

    @Override
    public String getInstallationId() {
        return distributedComponentEntryMock.getInstallationID();
    }

    @Override
    public Integer getMaximumCountOfParallelInstances() {
        // no implementation needed for unit testing
        return null;
    }

    @Override
    public boolean isMappedComponent() {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    public LogicalNodeId getNodeIdObject() {
        // no implementation needed for unit testing
        return null;
    }

}
