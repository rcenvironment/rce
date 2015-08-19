/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import de.rcenvironment.core.component.datamanagement.api.DefaultComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Parametric study component.
 * 
 * @author Doreen Seider
 */
public abstract class DefaultHistoryDataItemSubtreeBuilder implements ComponentHistoryDataItemSubtreeBuilder {

    @Override
    public Serializable deserializeHistoryDataItem(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (Serializable) ois.readObject();
    }

    @Override
    public void buildInitialHistoryDataItemSubtree(Serializable historyDataItem, DMBrowserNode parentNode) {

        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        TypedDatumSerializer serializer = registryAccess.getService(TypedDatumService.class).getSerializer();

        if (historyDataItem instanceof String) {
            DefaultComponentHistoryDataItem historyData;
            try {
                historyData = DefaultComponentHistoryDataItem.fromString((String) historyDataItem,
                    serializer);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            CommonHistoryDataItemSubtreeBuilderUtils.buildDefaultHistoryDataItemSubtrees(historyData, parentNode);

        } else {
            String exceptionInformationText = "";
            if (historyDataItem != null) {
                exceptionInformationText = String.format("Parsing history data point failed: Expected type %s, but was of type %s",
                    String.class.getCanonicalName(),
                    historyDataItem.getClass().getCanonicalName());
            } else {
                exceptionInformationText = String.format("Parsing history data point failed: Expected type %s, actual type not available.",
                    String.class.getCanonicalName());
            }
            throw new IllegalArgumentException(exceptionInformationText);
        }
    }

}
