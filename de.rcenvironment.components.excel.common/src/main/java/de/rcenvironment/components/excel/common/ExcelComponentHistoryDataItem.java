/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

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
 * {@link CommonComponentHistoryDataItem} implementation for the Excel component.
 *
 * @author Doreen Seider
 */
public class ExcelComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";
    
    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;
    
    private static final long serialVersionUID = -2017053187345233310L;

    private static final String EXCEL_FILE_PATH = "e";

    private String excelFilePath;
        
    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    @Override
    public String getIdentifier() {
        return ExcelComponentConstants.COMPONENT_ID;
    }
    
    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException  {
        String commonDataString = super.serialize(serializer);
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(commonDataString);
            
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        ((ObjectNode) rootNode).put(EXCEL_FILE_PATH, excelFilePath);
        return rootNode.toString();
    }
    
    public void setExcelFilePath(String excelFilePath) {
        this.excelFilePath = excelFilePath;
    }
    
    public String getExcelFilePath() {
        return excelFilePath;
    }
    
    /**
     * @param historyData text representation of {@link ExcelComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @return new {@link ExcelComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static ExcelComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer)
        throws IOException {
        ExcelComponentHistoryDataItem historyDataItem = new ExcelComponentHistoryDataItem();
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);
        
        ExcelComponentHistoryDataItem.readExcelFilePathFromString(historyData, historyDataItem);
        
        return historyDataItem;
    }
    
    private static void readExcelFilePathFromString(String historyData, ExcelComponentHistoryDataItem historyDataItem)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
            
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        historyDataItem.excelFilePath = ((ObjectNode) rootNode).get(EXCEL_FILE_PATH).textValue();
    }

}
