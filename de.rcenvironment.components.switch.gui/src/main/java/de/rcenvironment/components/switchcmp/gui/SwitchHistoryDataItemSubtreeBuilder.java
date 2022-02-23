/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.common.SwitchComponentHistoryDataItem;
import de.rcenvironment.components.switchcmp.common.SwitchCondition;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * 
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for this component.
 *
 * @author David Scholz
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Kathrin Schaffert
 */
public class SwitchHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    private static final String EXCEPTION_MESSAGE_READING = "Unexpected Exception occured, while reading JSON content String.";

    static {
        String bundleName = "de.rcenvironment.components.switch.common";
        String iconName = "switch_16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { SwitchComponentConstants.COMPONENT_ID };
    }

    @Override
    public Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (Serializable) ois.readObject();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parentNode) {
        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumSerializer serializer = registryAccess.getService(TypedDatumService.class).getSerializer();

        if (historyDataItem instanceof String) {
            SwitchComponentHistoryDataItem historyData;
            try {
                historyData = SwitchComponentHistoryDataItem.fromString((String) historyDataItem,
                    serializer);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }

            String storedFormatVersion = historyData.getStoredFormatVersion();
            String storedFV = storedFormatVersion.split(":")[1];
            String currentFV = historyData.getFormatVersion().split(":")[1];
            
            CommonHistoryDataItemSubtreeBuilderUtils.buildDefaultHistoryDataItemSubtrees(historyData, parentNode);

            if (storedFormatVersion.equals(historyData.getFormatVersion())
                && currentFV.equals(SwitchComponentHistoryDataItem.FORMAT_VERSION_2)) {
                DMBrowserNode conditionsNode = DMBrowserNode.addNewChildNode("Conditions", DMBrowserNodeType.Custom, parentNode);
                conditionsNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.QUESTION_MARK_16));

                DMBrowserNode writeToFirstConditionNode = DMBrowserNode.addNewLeafNode(
                    Messages.writeOutputLabel + ": " + StringUtils.abbreviate(historyData.getWriteToFirstCondition(),
                        CommonHistoryDataItemSubtreeBuilderUtils.MAX_LABEL_LENGTH),
                    DMBrowserNodeType.InformationText, conditionsNode);
                writeToFirstConditionNode.setFileContentAndName(historyData.getWriteToFirstCondition(), Messages.writeOutputLabel);

                ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
                Map<String, String> actualConditions = new HashMap<>();
                try {
                    actualConditions = (HashMap<String, String>) mapper.readValue(historyData.getActualCondition(), Map.class);
                } catch (IOException e) {
                    throw new RuntimeException(EXCEPTION_MESSAGE_READING, e); // should never happen
                }

                ArrayList<SwitchCondition> templateConditions =
                    (ArrayList<SwitchCondition>) SwitchCondition.getSwitchConditionList(historyData.getConditionPattern());

                for (Entry<String, String> entry : actualConditions.entrySet()) {
                    int key = Integer.parseInt(entry.getKey());
                    String scriptTemplate = templateConditions.get(key - 1).getConditionScript();
                    DMBrowserNode conditionNode =
                        DMBrowserNode.addNewChildNode("Condition " + key + ": " + scriptTemplate,
                        DMBrowserNodeType.Custom, conditionsNode);
                    conditionNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.QUESTION_MARK_16));
                    DMBrowserNode actualConditionnode = DMBrowserNode.addNewLeafNode(
                        "Actual: " + StringUtils.abbreviate(entry.getValue(),
                            CommonHistoryDataItemSubtreeBuilderUtils.MAX_LABEL_LENGTH),
                        DMBrowserNodeType.CommonText, conditionNode);
                    actualConditionnode.setFileContentAndName(entry.getValue(), "Actual Condition " + key);
                }
            } else if (storedFV.equals(SwitchComponentHistoryDataItem.FORMAT_VERSION_1)) {
                DMBrowserNode conditionNode = DMBrowserNode.addNewChildNode("Condition", DMBrowserNodeType.Custom, parentNode);
                conditionNode.setIcon(ImageManager.getInstance().getSharedImage(StandardImages.QUESTION_MARK_16));
                DMBrowserNode actualConditionnode = DMBrowserNode.addNewLeafNode(
                    "Actual: " + StringUtils.abbreviate(historyData.getActualCondition(),
                        CommonHistoryDataItemSubtreeBuilderUtils.MAX_LABEL_LENGTH),
                    DMBrowserNodeType.CommonText, conditionNode);
                actualConditionnode.setFileContentAndName(historyData.getActualCondition(), "Actual condition");
                DMBrowserNode templateConditionnode = DMBrowserNode.addNewLeafNode(
                    "Pattern: " + StringUtils.abbreviate(historyData.getConditionPattern(),
                        CommonHistoryDataItemSubtreeBuilderUtils.MAX_LABEL_LENGTH),
                    DMBrowserNodeType.CommonText, conditionNode);
                templateConditionnode.setFileContentAndName(historyData.getConditionPattern(), "Condition pattern");
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
