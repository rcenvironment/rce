/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.xml.XmlComponentHistoryDataItem;
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
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the XML Merger component.
 * 
 * @author Jan Flink
 * @author Sascha Zur
 */
public class XmlMergerHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.xml.merger.common";
        String iconName = "xmlMerger16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { XmlMergerComponentConstants.COMPONENT_ID };
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
            XmlComponentHistoryDataItem historyData;
            try {
                historyData = XmlComponentHistoryDataItem.fromString((String) historyDataItem,
                    serializer, XmlMergerComponentConstants.COMPONENT_ID);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildCommonHistoryDataItemSubtrees(historyData, parent);

            if (historyData.getXmlWithVariablesFileReference() != null) {
                DMBrowserNode mappingWithVariablesNode =
                    DMBrowserNode.addNewChildNode("Mapping with variables", DMBrowserNodeType.InformationText, parent);
                DMBrowserNode xmlWithVariablesFileNode =
                    DMBrowserNode.addNewLeafNode("xmlWithVariables.xml", DMBrowserNodeType.DMFileResource, mappingWithVariablesNode);
                xmlWithVariablesFileNode.setAssociatedFilename("xmlWithVariables.xml");
                xmlWithVariablesFileNode.setDataReferenceId(historyData.getXmlWithVariablesFileReference());
            }
        } else {
            throw new IllegalArgumentException(StringUtils.format("Parsing history data point failed: Expected type %s, but was type %s",
                String.class.getCanonicalName(), historyDataItem.getClass().getCanonicalName()));
        }
    }

    @Override
    public Image getComponentIcon(String identifier) {
        return COMPONENT_ICON;
    }

}
