/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.datamanagement.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * It encapsulates the common component data such as inputs and outputs (by inheriting), or exit code and log files.
 * 
 * @author Doreen Seider
 */
public abstract class CommonComponentHistoryDataItem extends DefaultComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";
    
    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;
    
    protected static final String FORMAT_VERSION = "f_v";
    
    protected static final String EXIT_CODE = "exit";

    private static final long serialVersionUID = 3747244536714110690L;

    protected Map<String, String> logs = new HashMap<>();
    
    protected Integer exitCode;
    
    public String getFormatVersion() {
        return CURRENT_FORMAT_VERSION;
    }
    
    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(FORMAT_VERSION, getFormatVersion());
        if (exitCode != null) {
            rootNode.put(EXIT_CODE, exitCode);            
        }
        
        return rootNode.toString();
    }
    
    protected ObjectNode getLogsAsJsonObjectNode(Map<String, String> logRefs, ObjectMapper mapper) {
        
        ObjectNode logsObjectNode = mapper.createObjectNode();
        
        for (String logFileName : logRefs.keySet()) {
            logsObjectNode.put(logFileName, logRefs.get(logFileName));
        }
        
        return logsObjectNode;
    }

    protected static void initializeCommonHistoryDataFromString(CommonComponentHistoryDataItem historyDataItem,
        String historyData, TypedDatumSerializer serializer) throws IOException {
        DefaultComponentHistoryDataItem.initializeDefaultHistoryDataFromString(historyDataItem, historyData, serializer);
        historyDataItem.exitCode = CommonComponentHistoryDataItem.getExitCodeFromString(historyData);
    }
    
    protected static Integer getExitCodeFromString(String exitCodeString) throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            JsonNode tree = mapper.readTree(exitCodeString);
            if (tree.get(EXIT_CODE) != null) {
                return tree.get(EXIT_CODE).asInt();
            }
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        return null;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    /**
     * @return exit code or <code>null</code> if no exit code was set
     */
    public Integer getExitCode() {
        return exitCode;
    }

}

