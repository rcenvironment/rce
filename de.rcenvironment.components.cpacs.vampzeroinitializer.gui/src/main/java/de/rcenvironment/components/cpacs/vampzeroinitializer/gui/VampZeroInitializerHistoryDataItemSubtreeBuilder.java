/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
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
public class VampZeroInitializerHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.cpacs.vampzeroinitializer.common";
        String iconName = "vampzeroInitializer16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { VampZeroInitializerComponentConstants.COMPONENT_ID };
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
                    serializer, VampZeroInitializerComponentConstants.COMPONENT_ID);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildCommonHistoryDataItemSubtrees(historyData, parent);

            if (historyData.getPlainXmlFileReference() != null) {
                DMBrowserNode generatedCpacsNode =
                    DMBrowserNode.addNewChildNode("Generated CPACS", DMBrowserNodeType.InformationText, parent);
                DMBrowserNode plainCpacsFileNode =
                    DMBrowserNode.addNewLeafNode("plainCpacs.xml", DMBrowserNodeType.DMFileResource, generatedCpacsNode);
                plainCpacsFileNode.setAssociatedFilename("plainCpacs.xml");
                plainCpacsFileNode.setDataReferenceId(historyData.getPlainXmlFileReference());
            }
        } else {
            throw new IllegalArgumentException(StringUtils.format("Parsing history data point failed: Expected type %s, but was type %s",
                String.class.getCanonicalName(),
                historyDataItem.getClass().getCanonicalName()));
        }
    }

    @Override
    public Image getComponentIcon(String identifier) {
        return COMPONENT_ICON;
    }

}
