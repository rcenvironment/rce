/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link ComponentHistoryDataItem} implementation for all integrated components.
 * 
 * @author Sascha Zur
 */
public class IntegrationHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String WORKING_DIRECTORY = "workingDirectory";

    protected static final String FORMAT_VERSION_1 = "1";

    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final long serialVersionUID = 266418465982554055L;

    protected String workingDirectory;

    private String identifier;

    public IntegrationHistoryDataItem(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        String commonDataString = super.serialize(serializer);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(commonDataString);
            ((ObjectNode) rootNode).put(WORKING_DIRECTORY, workingDirectory);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link IntegrationHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @param identifier for the item
     * @return new {@link IntegrationHistoryDataItem} object
     * @throws IOException on error
     */
    public static IntegrationHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer, String identifier)
        throws IOException {
        IntegrationHistoryDataItem historyDataItem = new IntegrationHistoryDataItem(identifier);
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> tree = mapper.readValue(historyData, new HashMap<String, Object>().getClass());
            if (tree.get(WORKING_DIRECTORY) != null) {
                historyDataItem.setWorkingDirectory((String) tree.get(WORKING_DIRECTORY));
            }

        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        return historyDataItem;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setWorkingDirectory(String absolutePath) {
        this.workingDirectory = absolutePath;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

}
