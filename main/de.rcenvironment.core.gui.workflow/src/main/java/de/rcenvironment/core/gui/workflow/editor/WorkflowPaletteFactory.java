/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Factory for creating a new palette with all running components.
 * 
 * @author Heinrich Wendel
 * 
 */
public class WorkflowPaletteFactory {

    /**
     * Returns a new (up to date) palette by consuming the ComponentRegistry.
     * 
     * @param componentInstallations {@link List} of {@link ComponentInstallation}s
     * @return A palette.
     */
    public PaletteRoot createPalette(List<ComponentInstallation> componentInstallations) {
        PaletteRoot palette = new PaletteRoot();
        createToolsGroup(palette);
        createComponentsGroup(palette, componentInstallations);
        return palette;
    }

    private void createComponentsGroup(PaletteRoot palette, List<ComponentInstallation> componentInstallations) {

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        PlatformService platformService = serviceRegistryAccess.getService(PlatformService.class);
        NodeIdentifier localNode = platformService.getLocalNodeId();

        Map<String, List<PaletteEntry>> groupedComponents = new HashMap<String, List<PaletteEntry>>();
        componentInstallations = ComponentUtils.eliminateComponentInterfaceDuplicates(componentInstallations, localNode);
        Collections.sort(componentInstallations);

        for (ComponentInstallation ci : componentInstallations) {
            ComponentInterface componentInterface = ci.getComponentRevision().getComponentInterface();
            // prepare the icon of the component
            ImageDescriptor image = null;
            byte[] icon = componentInterface.getIcon16();
            if (icon != null) {
                image = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), new ByteArrayInputStream(icon)));
            } else {
                image = Activator.getInstance().getImageRegistry().getDescriptor(Activator.IMAGE_RCE_ICON_16);
            }
            String name = componentInterface.getDisplayName();
            ToolIntegrationContextRegistry toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
            if (componentInterface.getVersion() != null
                && toolIntegrationRegistry.hasId(componentInterface.getIdentifier())) {
                name = name + String.format(WorkflowEditor.COMPONENTNAMES_WITH_VERSION, componentInterface.getVersion());
            }
            // create the palette entry
            CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(name, name,
                new WorkflowNodeFactory(ci), image, image);

            if (!groupedComponents.containsKey(componentInterface.getGroupName())) {
                groupedComponents.put(componentInterface.getGroupName(), new ArrayList<PaletteEntry>());
            }
            groupedComponents.get(componentInterface.getGroupName()).add(component);
        }
        List<String> specialGroups = new ArrayList<String>();
        List<String> standardGroups = new ArrayList<String>();

        // separate components into special ones (e.g. deprecated) to appear in the end and normal
        // ones
        for (String group : groupedComponents.keySet()) {
            if (group.startsWith("_")) {
                specialGroups.add(group);
            } else {
                standardGroups.add(group);
            }
        }

        // sort and add them separately
        Collections.sort(specialGroups, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(standardGroups, String.CASE_INSENSITIVE_ORDER);

        for (String standardGroup : standardGroups) {
            PaletteDrawer componentsDrawer = new PaletteDrawer(standardGroup);
            componentsDrawer.addAll(groupedComponents.get(standardGroup));
            if (standardGroup.equals(ComponentConstants.COMPONENT_GROUP_TEST)) {
                componentsDrawer.setInitialState(PaletteDrawer.INITIAL_STATE_CLOSED);
            }
            palette.add(componentsDrawer);
        }

        for (String specialGroup : specialGroups) {
            PaletteDrawer componentsDrawer = new PaletteDrawer(specialGroup);
            componentsDrawer.addAll(groupedComponents.get(specialGroup));
            if (specialGroup.equals(ComponentConstants.COMPONENT_GROUP_TEST)) {
                componentsDrawer.setInitialState(PaletteDrawer.INITIAL_STATE_CLOSED);
            }
            palette.add(componentsDrawer);
        }

    }

    private void createToolsGroup(PaletteRoot palette) {
        PaletteGroup toolsGroup = new PaletteGroup(Messages.tools);
        List<PaletteEntry> entries = new ArrayList<PaletteEntry>();

        // Add a selection tool to the group
        ToolEntry tool = new SelectionToolEntry();
        tool.setLabel(Messages.select);
        palette.setDefaultEntry(tool);
        entries.add(tool);

        // Add (solid-line) connection tool
        tool = new ConnectionCreationToolEntry(
            Messages.connection,
            Messages.newConnection,
            new SimpleFactory(null),
            ImageDescriptor.createFromURL(WorkflowPaletteFactory.class.getResource("/resources/icons/connection16.gif")), //$NON-NLS-1$ 
            ImageDescriptor.createFromURL(WorkflowPaletteFactory.class.getResource("/resources/icons/connection24.gif"))); //$NON-NLS-1$
        tool.setLabel(tool.getLabel());
        entries.add(tool);
       
        tool = new CombinedTemplateCreationEntry(WorkflowLabel.PALETTE_ENTRY_NAME,
            Messages.label, new LabelFactory(), null, null);
        tool.setDescription(Messages.labelDescription);
        tool.setLargeIcon(ImageDescriptor.createFromURL(WorkflowPaletteFactory.class.getResource("/resources/icons/label_24.png")));
        tool.setSmallIcon(ImageDescriptor.createFromURL(WorkflowPaletteFactory.class.getResource("/resources/icons/label_16.png")));
        entries.add(tool);
        toolsGroup.addAll(entries);
        palette.add(toolsGroup);
    }

}
