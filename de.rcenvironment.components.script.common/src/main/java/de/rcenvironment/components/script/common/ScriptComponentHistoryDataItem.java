/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.script.common;

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
 * {@link de.rcenvironment.core.component.datamanagement.api} implementation for the Script component.
 *
 * @author Doreen Seider
 */
public class ScriptComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";
    
    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;
    
    private static final long serialVersionUID = -2017053187345233310L;

    private static final String SCRIPT_FILE_REFERENCE = "s";

    private String scriptFileReference;
        
    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    @Override
    public String getIdentifier() {
        return ScriptComponentConstants.COMPONENT_ID;
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
        ((ObjectNode) rootNode).put(SCRIPT_FILE_REFERENCE, scriptFileReference);
        return rootNode.toString();
    }
    
    public void setScriptFileReference(String scriptFileReference) {
        this.scriptFileReference = scriptFileReference;
    }
    
    public String getScriptFileReference() {
        return scriptFileReference;
    }
    
    /**
     * @param historyData text representation of {@link ScriptComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @return new {@link ScriptComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static ScriptComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer)
        throws IOException {
        ScriptComponentHistoryDataItem historyDataItem = new ScriptComponentHistoryDataItem();
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);
        
        ScriptComponentHistoryDataItem.readScriptFileReferenceFromString(historyData, historyDataItem);
        
        return historyDataItem;
    }
    
    private static void readScriptFileReferenceFromString(String historyData, ScriptComponentHistoryDataItem historyDataItem)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
            
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        if (((ObjectNode) rootNode).has(SCRIPT_FILE_REFERENCE)) {
            historyDataItem.scriptFileReference = ((ObjectNode) rootNode).get(SCRIPT_FILE_REFERENCE).textValue();
        }
    }

}
