/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.datamanagement.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;

/**
 * It encapsulates the common component data such as inputs and outputs (by inheriting), or exit code and log files.
 * 
 * @author Doreen Seider
 */
public abstract class CommonComponentHistoryDataItem extends DefaultComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";
    
    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;
    
    protected static final String FORMAT_VERSION = "f_v";
    
    protected static final String LOG_FILES = "logs";
    
    protected static final String EXIT_CODE = "exit";

    private static final long serialVersionUID = 3747244536714110690L;

    protected Map<String, String> logs = new HashMap<>();
    
    protected Integer exitCode;
    
    public String getFormatVersion() {
        return CURRENT_FORMAT_VERSION;
    }
    
    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(FORMAT_VERSION, getFormatVersion());
        rootNode.put(LOG_FILES, getLogsAsJsonObjectNode(logs, mapper));
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
        historyDataItem.logs = CommonComponentHistoryDataItem.getLogsFromString(historyData);
        historyDataItem.exitCode = CommonComponentHistoryDataItem.getExitCodeFromString(historyData);
    }
    
    protected static Map<String, String> getLogsFromString(String logRefsString)
        throws IOException {
        
        Map<String, String> logRefsMap = new HashMap<>();
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode tree = mapper.readTree(logRefsString);
            if (tree.get(LOG_FILES) != null) {
                ObjectNode logsObjectNode = (ObjectNode) tree.get(LOG_FILES);
                Iterator<String> logFileNamesIterator = logsObjectNode.getFieldNames();
                while (logFileNamesIterator.hasNext()) {
                    String fileName = logFileNamesIterator.next();
                    logRefsMap.put(fileName, logsObjectNode.get(fileName).getTextValue());
                }
            }
            return logRefsMap;
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }
    
    protected static Integer getExitCodeFromString(String exitCodeString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
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
    
    public Map<String, String> getLogs() {
        return logs;
    }

    /**
     * @param fileName file name of log file
     * @param logFileReferenceId DM file reference of the log file
     */
    public void addLog(String fileName, String logFileReferenceId) {
        logs.put(fileName, logFileReferenceId);
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

