/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link de.rcenvironment.core.component.datamanagement.api.n} implementation for the DOE
 * component.
 * 
 * @author Sascha Zur
 */
public class DOEComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";

    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final long serialVersionUID = -2017053187345233310L;

    private static final String TABLE_FILE_REFERENCE = "TableFile";

    private static final String RESULT_FILE_REFERENCE = "ResultFile";

    private String tableFileReference;

    private String resultFileReference;

    public DOEComponentHistoryDataItem() {

    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    @Override
    public String getIdentifier() {
        return DOEConstants.COMPONENT_ID;
    }

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        String commonDataString = super.serialize(serializer);
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(commonDataString);

        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        ((ObjectNode) rootNode).put(TABLE_FILE_REFERENCE, tableFileReference);

        ((ObjectNode) rootNode).put(RESULT_FILE_REFERENCE, resultFileReference);
        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link DOEComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @return new {@link DOEComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static DOEComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer)
        throws IOException {
        DOEComponentHistoryDataItem historyDataItem = new DOEComponentHistoryDataItem();
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        historyDataItem.tableFileReference = DOEComponentHistoryDataItem.readFileReferenceFromString(TABLE_FILE_REFERENCE,
            historyData, historyDataItem);
        historyDataItem.resultFileReference = DOEComponentHistoryDataItem.readFileReferenceFromString(RESULT_FILE_REFERENCE,
            historyData, historyDataItem);
        return historyDataItem;
    }

    private static String readFileReferenceFromString(String referenceName, String historyData,
        DOEComponentHistoryDataItem historyDataItem)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        if (((ObjectNode) rootNode).has(referenceName)) {
            return ((ObjectNode) rootNode).get(referenceName).textValue();
        }
        return null;
    }

    public void setResultFileReference(String fileReference) {
        this.resultFileReference = fileReference;
    }

    public String getResultFileReference() {
        return resultFileReference;
    }

    public void setTableFileReference(String inputFileReference) {
        this.tableFileReference = inputFileReference;
    }

    public String getTableFileReference() {
        return tableFileReference;
    }
}
