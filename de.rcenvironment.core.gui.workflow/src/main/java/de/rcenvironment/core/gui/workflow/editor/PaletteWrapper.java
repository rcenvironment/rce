/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.jface.resource.ImageDescriptor;

class PaletteWrapper {

    private final PaletteRoot paletteRoot;
    
    private PaletteViewer paletteViewer;

    private ConnectionCreationToolEntry connectionCreationToolEntry = null;

    PaletteWrapper(PaletteRoot paletteRoot) {
        this.paletteRoot = paletteRoot;
    }

    public void addComponentToGroup(CombinedTemplateCreationEntry component, String groupLabel) {
        for (Object group : paletteRoot.getChildren()) {
            if (!(group instanceof PaletteDrawer)) {
                continue;
            }
            final PaletteDrawer drawer = (PaletteDrawer) group;

            if (!(drawer.getLabel().equals(groupLabel))) {
                continue;
            }

            drawer.add(getIndexForComponentToAdd(drawer, component.getLabel()), component);
        }
    }

    private int getIndexForComponentToAdd(PaletteDrawer group, String componentLabel) {
        int index = 0;
        for (Object child : group.getChildren()) {
            if (!(child instanceof PaletteEntry)) {
                continue;
            }

            final PaletteEntry paletteEntry = (PaletteEntry) child;

            if (paletteEntry.getLabel().compareToIgnoreCase(componentLabel) > 0) {
                return index;
            }

            index++;
        }
        return index;
    }

    public PaletteDrawer createPaletteDrawer(String groupLabel) {
        final PaletteDrawer group = new PaletteDrawer(groupLabel);
        group.setDescription(groupLabel);
        group.setInitialState(PaletteDrawer.INITIAL_STATE_CLOSED);
        paletteRoot.add(getIndexForGroupToAdd(groupLabel), group);
        return group;
    }

    private int getIndexForGroupToAdd(String groupLabel) {
        int index = 0;

        // if group starts with underscore, append it to the end.
        if (groupLabel.startsWith("_")) {
            return paletteRoot.getChildren().size();
        }

        for (Object group : paletteRoot.getChildren()) {
            if (!(group instanceof PaletteDrawer)) {
                continue;
            }

            final PaletteDrawer paletteDrawer = (PaletteDrawer) group;
            if (paletteDrawer.getLabel().compareToIgnoreCase(groupLabel) > 0) {
                return index;
            }

            index++;
        }
        return index;
    }

    public List<String> getExistingPaletteEntries() {
        List<String> paletteEntries = new ArrayList<>();
        for (Object child : paletteRoot.getChildren()) {
            if (!(child instanceof PaletteDrawer)) { 
                continue;
            }

            final PaletteDrawer paletteDrawer = (PaletteDrawer) child;
            for (Object innerChild : paletteDrawer.getChildren()) {
                if (!(innerChild instanceof PaletteEntry)) {
                    continue;
                }

                paletteEntries.add(((PaletteEntry) innerChild).getLabel());
            }
        }
        return paletteEntries;
    }

    public List<String> getExistingPaletteGroups() {
        List<String> paletteGroups = new ArrayList<>();
        for (Object child : paletteRoot.getChildren()) {
            if (child instanceof PaletteDrawer) {
                paletteGroups.add(((PaletteDrawer) child).getLabel());
            }
        }
        return paletteGroups;
    }

    public Map<PaletteDrawer, List<PaletteEntry>> getComponentsByName(List<String> componentNames) {
        Map<PaletteDrawer, List<PaletteEntry>> componentsToRemove = new HashMap<>();

        for (Object group : paletteRoot.getChildren()) {
            if (!(group instanceof PaletteDrawer)) {
                continue;
            }

            final PaletteDrawer paletteDrawer = (PaletteDrawer) group;
            for (Object component : paletteDrawer.getChildren()) {
                if (!(component instanceof PaletteEntry)) {
                    continue;
                }

                final PaletteEntry paletteEntry = (PaletteEntry) component;
                if (componentNames.contains(paletteEntry.getLabel())) {
                    continue;
                }

                componentsToRemove
                    .computeIfAbsent(paletteDrawer, ignored -> new ArrayList<>())
                    .add((PaletteEntry) component);
            }
        }
        return componentsToRemove;
    }

    public void removeGroupIfEmpty(PaletteDrawer group) {
        if (group.getChildren().isEmpty()) {
            paletteRoot.remove(group);
        }
    }

    public void updateComponentIcon(Supplier<ImageDescriptor> getImageDescriptor, String name) {
        for (Object group : paletteRoot.getChildren()) {
            if (!(group instanceof PaletteDrawer)) {
                continue;
            }
            for (Object component : ((PaletteDrawer) group).getChildren()) {
                if (!(component instanceof PaletteEntry && ((PaletteEntry) component).getLabel().equals(name))) {
                    continue;
                }

                final ImageDescriptor descriptor = getImageDescriptor.get();
                ((PaletteEntry) component).setLargeIcon(descriptor);
                ((PaletteEntry) component).setSmallIcon(descriptor);
            }
        }
    }

    public PaletteViewer getPaletteViewer() {
        return this.paletteViewer;
    }

    public void setPaletteViewer(PaletteViewer paletteViewerParam) {
        this.paletteViewer = paletteViewerParam;
    }
    
    public void setActiveTool(ToolEntry entry) {
        this.paletteViewer.setActiveTool(entry);
    }

    public void switchToDefaultTool() {
        this.setActiveTool(this.paletteRoot.getDefaultEntry());
    }


    public ConnectionCreationToolEntry getConnectionCreationToolEntry() {
        for (Object paletteGroupObject : this.paletteViewer.getPaletteRoot().getChildren()) {
            if (!(paletteGroupObject instanceof PaletteGroup)) {
                continue;
            }

            final PaletteGroup paletteGroup = (PaletteGroup) paletteGroupObject;
            for (Object paletteEntryObject : paletteGroup.getChildren()) {
                if (!(paletteEntryObject instanceof ConnectionCreationToolEntry)) {
                    continue;
                }

                return (ConnectionCreationToolEntry) paletteEntryObject;
            }
        }
        throw new IllegalStateException("Could not find ConnectionCreationToolEntry in Palette");
    }

    // Switch activate tool in palette to the Draw Connection tool
    public void switchToConnectionTool() {
        if (this.connectionCreationToolEntry == null) {
            this.connectionCreationToolEntry = this.getConnectionCreationToolEntry();
        }

        this.setActiveTool(connectionCreationToolEntry);
    }
}
