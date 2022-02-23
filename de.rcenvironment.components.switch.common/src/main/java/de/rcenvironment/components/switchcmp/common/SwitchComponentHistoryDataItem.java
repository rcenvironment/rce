/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.common;

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
 * {@link de.rcenvironment.core.component.datamanagement.api.n} implementation for the Switch component.
 *
 * @author David Scholz
 * @author Doreen Seider
 * @author Kathrin Schaffert
 */
public class SwitchComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    /** Format Version "1" of {@link SwitchComponentHistoryDataItem}. */
    public static final String FORMAT_VERSION_1 = "1";

    /** Format Version "2" of {@link SwitchComponentHistoryDataItem}. */
    public static final String FORMAT_VERSION_2 = "2";

    private static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_2;

    private static final long serialVersionUID = 7371817804674417738L;

    private static final String ACTUAL_CONDITION = "ac";

    private static final String CONDITION_PATTERN = "cp";

    private static final String WRITE_TO_FIRST_CONDITION = "wriOutCb";

    private String actualCondition;

    private String conditionPattern;

    private String writeToFirstCondition;

    private String storedFormatVersion;

    public void setActualCondition(String actualCondition) {
        this.actualCondition = actualCondition;
    }

    public String getActualCondition() {
        return actualCondition;
    }

    public void setConditionPattern(String conditionPattern) {
        this.conditionPattern = conditionPattern;
    }

    public String getConditionPattern() {
        return conditionPattern;
    }

    public void setWriteToFirstCondition(String writeToFirstCondition) {
        this.writeToFirstCondition = writeToFirstCondition;
    }

    public String getWriteToFirstCondition() {
        return writeToFirstCondition;
    }

    public String getStoredFormatVersion() {
        return storedFormatVersion;
    }

    public void setStoredFormatVersion(String storedformatVersion) {
        this.storedFormatVersion = storedformatVersion;
    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
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
        ((ObjectNode) rootNode).put(ACTUAL_CONDITION, actualCondition);
        ((ObjectNode) rootNode).put(CONDITION_PATTERN, conditionPattern);
        ((ObjectNode) rootNode).put(WRITE_TO_FIRST_CONDITION, writeToFirstCondition);
        ((ObjectNode) rootNode).put(FORMAT_VERSION, getFormatVersion());

        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link SwitchComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @return new {@link SwitchComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static SwitchComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer)
        throws IOException {
        SwitchComponentHistoryDataItem historyDataItem = new SwitchComponentHistoryDataItem();
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        readReferenceFromString(historyData, historyDataItem);
        return historyDataItem;
    }

    private static void readReferenceFromString(String historyData, SwitchComponentHistoryDataItem historyDataItem) throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        historyDataItem.actualCondition = ((ObjectNode) rootNode).get(ACTUAL_CONDITION).textValue();
        historyDataItem.conditionPattern = ((ObjectNode) rootNode).get(CONDITION_PATTERN).textValue();
        if (((ObjectNode) rootNode).get(WRITE_TO_FIRST_CONDITION) != null) {
            historyDataItem.writeToFirstCondition = ((ObjectNode) rootNode).get(WRITE_TO_FIRST_CONDITION).textValue();
        }
        historyDataItem.storedFormatVersion = (((ObjectNode) rootNode).get(CommonComponentHistoryDataItem.FORMAT_VERSION).textValue());
    }
}
