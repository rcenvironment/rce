/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentHistoryDataItem;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * 
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for Evaluation Memory component.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class EvaluationMemoryHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.evaluationmemory.common";
        String iconName = "evaluationMemory16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { EvaluationMemoryComponentConstants.COMPONENT_ID };
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
            EvaluationMemoryComponentHistoryDataItem historyData;
            try {
                historyData = EvaluationMemoryComponentHistoryDataItem.fromString((String) historyDataItem,
                    serializer, EvaluationMemoryComponentConstants.COMPONENT_ID);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildDefaultHistoryDataItemSubtrees(historyData, parentNode);

            if (historyData.getMemoryFilePath() != null) {
                DMBrowserNode.addNewLeafNode("Memory file path: " + historyData.getMemoryFilePath(),
                    DMBrowserNodeType.CommonText, parentNode);
            }

            if (historyData.getMemoryFileReference() != null) {
                String fileName = new File(historyData.getMemoryFilePath()).getName();
                DMBrowserNode scriptFileNode = DMBrowserNode.addNewLeafNode("Memory file: " + fileName, DMBrowserNodeType.DMFileResource,
                    parentNode);
                scriptFileNode.setAssociatedFilename(fileName);
                scriptFileNode.setDataReferenceId(historyData.getMemoryFileReference());
            }

        } else {
            String exceptionInformationText = "";
            if (historyDataItem != null) {
                exceptionInformationText =
                    de.rcenvironment.core.utils.common.StringUtils.format(
                        "Parsing history data point failed: Expected type %s, but was of type %s",
                        String.class.getCanonicalName(), historyDataItem.getClass().getCanonicalName());
            } else {
                exceptionInformationText =
                    de.rcenvironment.core.utils.common.StringUtils.format(
                        "Parsing history data point failed: Expected type %s, actual type not available.",
                        String.class.getCanonicalName());
            }
            throw new IllegalArgumentException(exceptionInformationText);
        }

    }

    @Override
    public Image getComponentIcon(String historyDataItemIdentifier) {
        return COMPONENT_ICON;
    }

}
