/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.DatabaseComponentHistoryDataItem;
import de.rcenvironment.components.database.common.DatabaseStatementHistoryData;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.gui.datamanagement.browser.spi.CommonHistoryDataItemSubtreeBuilderUtils;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNode;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DMBrowserNodeType;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DefaultHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Database History Data Item Builder.
 *
 * @author Oliver Seebach
 * @author Sascha Zur
 */
public class DatabaseHistoryDataItemSubtreeBuilder extends DefaultHistoryDataItemSubtreeBuilder {

    private static final Image COMPONENT_ICON;

    static {
        String bundleName = "de.rcenvironment.components.database.common";
        String iconName = "database16.png";
        URL url = ComponentUtils.readIconURL(bundleName, iconName);
        if (url != null) {
            COMPONENT_ICON = ImageDescriptor.createFromURL(url).createImage();
        } else {
            COMPONENT_ICON = null;
        }
    }

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { DatabaseComponentConstants.COMPONENT_ID };
    }

    @Override
    public Image getComponentIcon(String historyDataItemIdentifier) {
        return COMPONENT_ICON;
    }

    @Override
    public void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parentNode) {
        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumSerializer serializer = registryAccess.getService(TypedDatumService.class).getSerializer();

        if (historyDataItem instanceof String) {
            DatabaseComponentHistoryDataItem historyData;
            try {
                historyData = DatabaseComponentHistoryDataItem.fromString((String) historyDataItem,
                    serializer, DatabaseComponentConstants.COMPONENT_ID);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }

            CommonHistoryDataItemSubtreeBuilderUtils.buildDefaultHistoryDataItemSubtrees(historyData, parentNode);

            List<DatabaseStatementHistoryData> databaseStatementHistoryData =
                historyData.getDatabaseStatementHistoryDataList();

            if (databaseStatementHistoryData.size() > 0) {
                DMBrowserNode statementsFolderNode =
                    DMBrowserNode.addNewChildNode("SQL Statements", DMBrowserNodeType.SqlFolder, parentNode);
                for (DatabaseStatementHistoryData statement : databaseStatementHistoryData) {

                    DMBrowserNode statementsFolderWithName =
                        DMBrowserNode.addNewChildNode(statement.getStatementName(), DMBrowserNodeType.LogFolder, statementsFolderNode);

                    DMBrowserNode statementPatternNode =
                        DMBrowserNode.addNewLeafNode("Statement pattern: "
                            + StringUtils.abbreviate(statement.getStatementPattern(),
                                CommonHistoryDataItemSubtreeBuilderUtils.MAX_LABEL_LENGTH),
                            DMBrowserNodeType.CommonText,
                            statementsFolderWithName);
                    statementPatternNode.setFileContentAndName(statement.getStatementPattern(), "Statement pattern");
                    DMBrowserNode statementEffectiveNode =
                        DMBrowserNode.addNewLeafNode("Effective statement: "
                            + StringUtils.abbreviate(statement.getStatementEffective(),
                                CommonHistoryDataItemSubtreeBuilderUtils.MAX_LABEL_LENGTH),
                            DMBrowserNodeType.CommonText,
                            statementsFolderWithName);
                    statementEffectiveNode.setFileContentAndName(statement.getStatementEffective(),
                        "Effective statement");

                }
            } else {
                DMBrowserNode noStatementsFolderNode =
                    DMBrowserNode.addNewLeafNode("No statements found.", DMBrowserNodeType.SqlFolder, parentNode);
            }
        }

    }
}
