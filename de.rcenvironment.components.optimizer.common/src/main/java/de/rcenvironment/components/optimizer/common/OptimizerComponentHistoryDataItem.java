/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

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
 * {@link de.rcenvironment.core.component.datamanagement.api.n} implementation for the Optimizer
 * component.
 * 
 * @author Sascha Zur
 */
public class OptimizerComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";

    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final long serialVersionUID = -2017053187345233310L;

    private static final String INPUT_FILE_REFERENCE = "InputFile";

    private static final String RESTART_FILE_REFERENCE = "RestartFile";

    private static final String RESULT_FILE_REFERENCE = "ResultFile";

    private String inputFileReference;

    private String restartFileReference;

    private String resultFileReference;

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    @Override
    public String getIdentifier() {
        return OptimizerComponentConstants.COMPONENT_IDS[0];
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
        ((ObjectNode) rootNode).put(INPUT_FILE_REFERENCE, inputFileReference);
        ((ObjectNode) rootNode).put(RESTART_FILE_REFERENCE, restartFileReference);
        ((ObjectNode) rootNode).put(RESULT_FILE_REFERENCE, resultFileReference);
        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link OptimizerComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @return new {@link OptimizerComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static OptimizerComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer)
        throws IOException {
        OptimizerComponentHistoryDataItem historyDataItem = new OptimizerComponentHistoryDataItem();
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        historyDataItem.inputFileReference = OptimizerComponentHistoryDataItem.readFileReferenceFromString(INPUT_FILE_REFERENCE,
            historyData, historyDataItem);
        historyDataItem.restartFileReference = OptimizerComponentHistoryDataItem.readFileReferenceFromString(RESTART_FILE_REFERENCE,
            historyData, historyDataItem);
        historyDataItem.resultFileReference = OptimizerComponentHistoryDataItem.readFileReferenceFromString(RESULT_FILE_REFERENCE,
            historyData, historyDataItem);
        return historyDataItem;
    }

    private static String readFileReferenceFromString(String referenceName, String historyData,
        OptimizerComponentHistoryDataItem historyDataItem)
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
        resultFileReference = fileReference;
    }

    public String getResultFileReference() {
        return resultFileReference;
    }

    public String getRestartFileReference() {
        return restartFileReference;
    }

    public void setRestartFileReference(String restartFileReference) {
        this.restartFileReference = restartFileReference;
    }

    public void setInputFileReference(String inputFileReference) {
        this.inputFileReference = inputFileReference;
    }

    public String getInputFileReference() {
        return inputFileReference;
    }
}
