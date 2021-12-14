/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.palette;

import java.util.Set;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.RootNode;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

class PaletteViewContentProviderMock extends PaletteViewContentProvider {

    PaletteViewContentProviderMock(Set<DistributedComponentEntry> currentToolInstallations) {
        super(null);
        setRootNode(new RootNode(this));
        setCurrentToolInstallations(currentToolInstallations);
        setAssignment(new ToolGroupAssignmentMock(this));

    }

    @Override
    protected ServiceRegistryPublisherAccess getServiceRegistryPublisherAccess() {
        return EasyMock.createNiceMock(ServiceRegistryPublisherAccess.class);
    }

    @Override
    protected LogicalNodeId getLocalNodeId() {
        return null;
    }

    @Override
    protected Set<DistributedComponentEntry> getCurrentToolInstallations(
        DistributedComponentKnowledgeService distributedComponentKnowledgeService) {
        return getCurrentToolInstallations();
    }

    @Override
    public PaletteTreeNode[] getSuitableGroups(AccessibleComponentNode[] nodes, boolean hideOwnGroup) {
        // no implementation needed for unit testing
        return null;
    }


    @Override
    public boolean containsAnyToolNodes(PaletteTreeNode node, boolean exludeOfflineTools) {
        // no implementation needed for unit testing
        return false;
    }

    @Override
    protected void registerChangeListeners() {
    }

    public boolean getExpandedState(PaletteTreeNode node) {
        // no implementation needed for unit testing
        return false;
    }

    public void setExpandedState(PaletteTreeNode node, boolean expanded) {
        // no implementation needed for unit testing
    }

    @Override
    public void refreshPaletteTreeViewer(PaletteTreeNode parent) {
        // do nothing
    }

}
