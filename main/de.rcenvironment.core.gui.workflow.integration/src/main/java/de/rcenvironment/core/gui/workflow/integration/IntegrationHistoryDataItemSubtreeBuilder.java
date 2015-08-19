/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.IntegrationHistoryDataItem;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Script component.
 * 
 * @author Sascha Zur
 */
public class IntegrationHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static Map<String, byte[]> componentIcons = Collections.synchronizedMap(new HashMap<String, byte[]>());

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { ToolIntegrationConstants.STANDARD_COMPONENT_ID_PREFIX.replace(".", "\\.") + ".*" };
    }

    @Override
    public Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (Serializable) ois.readObject();
    }

    @Override
    public void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parent) {

        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumSerializer serializer = registryAccess.getService(TypedDatumService.class).getSerializer();

        if (historyDataItem instanceof String) {
            IntegrationHistoryDataItem historyData;
            try {
                historyData = IntegrationHistoryDataItem.fromString((String) historyDataItem, serializer, "");
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildCommonHistoryDataItemSubtrees(historyData, parent);
            if (historyData.getWorkingDirectory() != null) {
                for (DMBrowserNode node : parent.getChildren()) {
                    if (node.toString().equals(CommonHistoryDataItemSubtreeBuilderUtils.EXECUTION_LOG_FOLDER_NODE_TITLE)) {
                        DMBrowserNode.addNewLeafNode("Working directory: " + historyData.getWorkingDirectory(),
                            DMBrowserNodeType.InformationText,
                            node);
                    }
                }
            }
        }
    }

    @Override
    public Image getComponentIcon(String identifier) {
        if (!componentIcons.containsKey(identifier)) {
            ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
            DistributedComponentKnowledgeService componentKnowledgeService =
                serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
            Collection<ComponentInstallation> installations =
                componentKnowledgeService.getCurrentComponentKnowledge().getAllInstallations();
            for (ComponentInstallation installation : installations) {
                if (installation.getInstallationId().startsWith(identifier)) {
                    synchronized (componentIcons) {
                        componentIcons.put(identifier, installation.getComponentRevision().getComponentInterface().getIcon16());
                    }
                }
            }
        }
        byte[] icon = null;
        Image image = null;
        synchronized (componentIcons) {
            icon = componentIcons.get(identifier);
            if (icon != null) {
                image = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), new ByteArrayInputStream(icon))).createImage();
            } else {
                InputStream inputStream =
                    IntegrationHistoryDataItemSubtreeBuilder.class.getResourceAsStream("/resources/icons/tool16.png");
                image = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), inputStream)).createImage();
            }
        }
        return image;
    }
}
