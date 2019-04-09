/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementUtil;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.history.HistoryMetaDataKeys;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;

/**
 * Implements history-related methods.
 * 
 * TODO merge with {@link ComponentDataManagementService}?
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class WorkflowHistoryServiceImpl {

    private static final MetaData METADATA_CLASS_NAME = new MetaData(
        HistoryMetaDataKeys.HISTORY_HISTORY_DATA_ITEM_IDENTIFIER, true, true);

    private static final MetaData METADATA_USER_INFO_TEXT = new MetaData(
        HistoryMetaDataKeys.HISTORY_USER_INFO_TEXT, true, true);

    private static final MetaData METADATA_HISTORY_TIMESTAMP = new MetaData(
        HistoryMetaDataKeys.HISTORY_TIMESTAMP, true, true);

    private TypedDatumSerializer typedDatumSerializer;

    /**
     * FileDataService for storing/loading resources to the data management.
     */
    private FileDataService fileDataService;

    /**
     * Default constructor that tries to acquire all data management services automatically.
     * 
     * @param user the certificate to acquire the data management services with
     */
    public WorkflowHistoryServiceImpl(TypedDatumSerializer typedDatumSerializer, FileDataService fileDataService) {
        this.typedDatumSerializer = typedDatumSerializer;
        this.fileDataService = fileDataService;
    }

    /**
     * Creates a history data point.
     * 
     * TODO better doc; merge with {@link ComponentDataManagementService}?
     * 
     * @param historyData the history data to store
     * @param userInfoText the user information text to associate; usually used as display title
     * @param componentContext the {@link ComponentContext} to read metadata from
     * @param timestamp timestamp of the history data creation
     * @throws IOException on a data management or I/O error
     */
    public void addHistoryDataPoint(Serializable historyData, String userInfoText, ComponentContext componentContext,
        long timestamp) throws IOException {

        MetaDataSet mds = new MetaDataSet();

        ComponentDataManagementUtil.setComponentMetaData(mds, componentContext);

        setHistoryMetaData(mds, historyData, userInfoText, timestamp);

        InputStream historyDataInputStream = getHistoryDataPointAsInputStreamInputStream(historyData);
        // create reference
        try {
            fileDataService.newReferenceFromStream(historyDataInputStream, mds, componentContext.getStorageNetworkDestination());
        } catch (InterruptedException | CommunicationException e) {
            // reduce exception types
            throw new IOException(e);
        }
    }

    private static void setHistoryMetaData(MetaDataSet mds, Serializable historyData, String userInfoText, long timestamp) {
        if (historyData instanceof ComponentHistoryDataItem) {
            mds.setValue(METADATA_CLASS_NAME, ((ComponentHistoryDataItem) historyData).getIdentifier());
        } else {
            mds.setValue(METADATA_CLASS_NAME, historyData.getClass().getCanonicalName());
        }
        mds.setValue(METADATA_USER_INFO_TEXT, userInfoText);
        mds.setValue(METADATA_HISTORY_TIMESTAMP, Long.toString(timestamp));
    }

    private InputStream getHistoryDataPointAsInputStreamInputStream(Serializable historyData) throws IOException {
        if (historyData instanceof ComponentHistoryDataItem) {
            historyData = ((ComponentHistoryDataItem) historyData).serialize(typedDatumSerializer);
        }
        // convert Serializable -> InputStream;
        // as all streams are memory-only, cleanup is left to GC
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baOutputStream);
        oos.writeObject(historyData);
        return new ByteArrayInputStream(baOutputStream.toByteArray());
    }
}
