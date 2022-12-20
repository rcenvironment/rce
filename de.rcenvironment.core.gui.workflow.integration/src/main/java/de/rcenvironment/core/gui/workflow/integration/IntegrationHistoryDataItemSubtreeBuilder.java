/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.IntegrationHistoryDataItem;
import de.rcenvironment.core.component.model.api.ComponentImageContainerService;
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

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { ComponentConstants.COMMON_INTEGRATED_COMPONENT_ID_PREFIX.replace(".", "\\.") + ".*" };
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
        return ServiceRegistry.createAccessFor(this).getService(ComponentImageContainerService.class).getComponentImageContainer(identifier)
            .getComponentIcon16();
    }
}
