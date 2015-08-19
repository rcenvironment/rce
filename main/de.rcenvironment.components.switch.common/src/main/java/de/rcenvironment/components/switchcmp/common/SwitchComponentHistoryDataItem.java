/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.common;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * {@link de.rcenvironment.core.component.datamanagement.api.n} implementation for the Switch component.
 *
 * @author David Scholz
 * @author Doreen Seider
 */
public class SwitchComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";

    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final long serialVersionUID = 7371817804674417738L;

    private static final String ACTUAL_CONDITION = "ac";
    
    private static final String CONDITION_PATTERN = "cp";

    private String actualCondition;
    
    private String conditionPattern;

    private String identifier;

    public SwitchComponentHistoryDataItem(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

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

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        String data = super.serialize(serializer);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;

        try {
            rootNode = mapper.readTree(data);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        ((ObjectNode) rootNode).put(ACTUAL_CONDITION, actualCondition);
        ((ObjectNode) rootNode).put(CONDITION_PATTERN, conditionPattern);

        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link SwitchComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @param identifier represents component id
     * @return new {@link SwitchComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static SwitchComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer, String identifier)
        throws IOException {
        SwitchComponentHistoryDataItem historyDataItem = new SwitchComponentHistoryDataItem(identifier);
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        readReferenceFromString(historyData, historyDataItem);
        return historyDataItem;
    }

    private static void readReferenceFromString(String historyData, SwitchComponentHistoryDataItem historyDataItem) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        historyDataItem.actualCondition = ((ObjectNode) rootNode).get(ACTUAL_CONDITION).getTextValue();
        historyDataItem.conditionPattern = ((ObjectNode) rootNode).get(CONDITION_PATTERN).getTextValue();

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
