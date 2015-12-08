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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeConstants;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Script component.
 * 
 * @author Sascha Zur
 */
public class IntegrationHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static Image defaultIconImage;
    
    private final Map<String, Image> componentIconImageCache = new HashMap<>();

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
                    if (node.getTitle().equals(DMBrowserNodeConstants.NODE_NAME_EXECUTION_LOG)) {
                        DMBrowserNode.addNewLeafNode("Working directory: " + historyData.getWorkingDirectory(),
                            DMBrowserNodeType.InformationText, node);
                    }
                }
            }
        }
    }

    @Override
    public Image getComponentIcon(String identifier) {
        if (!componentIconImageCache.containsKey(identifier)) {
            byte[] icon = null;
            ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
            DistributedComponentKnowledgeService componentKnowledgeService = serviceRegistryAccess
                .getService(DistributedComponentKnowledgeService.class);
            Collection<ComponentInstallation> installations = componentKnowledgeService.getCurrentComponentKnowledge()
                .getAllInstallations();
            for (ComponentInstallation installation : installations) {
                if (installation.getInstallationId().startsWith(identifier)) {
                    icon = installation.getComponentRevision().getComponentInterface().getIcon16();
                    break;
                }
            }
            if (icon != null) {
                componentIconImageCache.put(identifier, ImageDescriptor.createFromImage(new Image(Display.getCurrent(),
                    new ByteArrayInputStream(icon))).createImage());
            }
        }
        if (componentIconImageCache.containsKey(identifier)) {
            return componentIconImageCache.get(identifier);            
        } else {
            if (defaultIconImage == null) {
                InputStream inputStream = null;
                try {
                    inputStream = IntegrationHistoryDataItemSubtreeBuilder.class.getResourceAsStream("/resources/icons/tool16.png");
                    defaultIconImage = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), inputStream)).createImage();
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
            return defaultIconImage;
        }
    }
}
