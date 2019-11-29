/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.gui.dm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.doe.common.DOEComponentHistoryDataItem;
import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the DOE component.
 * 
 * @author Sascha Zur
 */
public class DOEHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.doe.common";
        String iconName = "doe16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { DOEConstants.COMPONENT_ID };
    }

    @Override
    public Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (Serializable) ois.readObject();
    }

    @Override
    public void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parentNode) {

        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumSerializer serializer = registryAccess.getService(TypedDatumService.class).getSerializer();

        if (historyDataItem instanceof String) {
            DOEComponentHistoryDataItem historyData;
            try {
                historyData = DOEComponentHistoryDataItem.fromString((String) historyDataItem,
                    serializer);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildCommonHistoryDataItemSubtrees(historyData, parentNode);
            if (historyData.getTableFileReference() != null) {
                DMBrowserNode inputFileNode =
                    DMBrowserNode.addNewLeafNode("table.csv", DMBrowserNodeType.DMFileResource, parentNode);
                inputFileNode.setAssociatedFilename("table.csv");
                inputFileNode.setDataReferenceId(historyData.getTableFileReference());
            }
            if (historyData.getResultFileReference() != null) {
                DMBrowserNode inputFileNode =
                    DMBrowserNode.addNewLeafNode("result.csv", DMBrowserNodeType.DMFileResource, parentNode);
                inputFileNode.setAssociatedFilename("result.csv");
                inputFileNode.setDataReferenceId(historyData.getResultFileReference());
            }
        } else {
            String exceptionInformationText = "";
            if (historyDataItem != null) {
                exceptionInformationText = StringUtils.format("Parsing history data point failed: Expected type %s, but was of type %s",
                    String.class.getCanonicalName(),
                    historyDataItem.getClass().getCanonicalName());
            } else {
                exceptionInformationText =
                    StringUtils.format("Parsing history data point failed: Expected type %s, actual type not available.",
                        String.class.getCanonicalName());
            }
            throw new IllegalArgumentException(exceptionInformationText);
        }
    }

    @Override
    public Image getComponentIcon(String identifier) {
        return COMPONENT_ICON;
    }

}
