/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.common;

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
 * 
 * {@link CommonComponentHistoryDataItem} implementation for the Evaluation Memory component.
 *
 * @author Doreen Seider
 */
public class EvaluationMemoryComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    private static final long serialVersionUID = -5443884702114523764L;

    private static final String FORMAT_VERSION_1 = "1";

    private static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final String MEMORY_FILE_PATH = "mfp";
    
    private static final String MEMORY_FILE_REFERENCE = "mfr";

    private String memoryFilePath;
    
    private String memoryFileReference;

    private String identifier;

    public EvaluationMemoryComponentHistoryDataItem(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    public void setMemoryFilePath(String memoryFilePath) {
        this.memoryFilePath = memoryFilePath;
    }

    public String getMemoryFilePath() {
        return memoryFilePath;
    }
    
    public void setMemoryFileReference(String memoryFileReference) {
        this.memoryFileReference = memoryFileReference;
    }

    public String getMemoryFileReference() {
        return memoryFileReference;
    }

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        String data = super.serialize(serializer);
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;

        try {
            rootNode = mapper.readTree(data);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        ((ObjectNode) rootNode).put(MEMORY_FILE_PATH, memoryFilePath);
        ((ObjectNode) rootNode).put(MEMORY_FILE_REFERENCE, memoryFileReference);

        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link EvaluationMemoryComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @param identifier represents component id
     * @return new {@link EvaluationMemoryComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static EvaluationMemoryComponentHistoryDataItem fromString(String historyData, 
        TypedDatumSerializer serializer, String identifier)
        throws IOException {
        EvaluationMemoryComponentHistoryDataItem historyDataItem = new EvaluationMemoryComponentHistoryDataItem(identifier);
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        readReferenceFromString(historyData, historyDataItem);
        return historyDataItem;
    }

    private static void readReferenceFromString(String historyData, EvaluationMemoryComponentHistoryDataItem historyDataItem) 
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        historyDataItem.memoryFilePath = ((ObjectNode) rootNode).get(MEMORY_FILE_PATH).textValue();
        historyDataItem.memoryFileReference = ((ObjectNode) rootNode).get(MEMORY_FILE_REFERENCE).textValue();

    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

}
