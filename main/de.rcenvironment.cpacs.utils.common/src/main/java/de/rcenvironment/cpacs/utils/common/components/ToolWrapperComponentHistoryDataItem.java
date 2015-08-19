/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.utils.common.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link CommonComponentHistoryDataItem} implementation for tool wrapper components.
 * 
 * @author Sascha Zur
 */
public class ToolWrapperComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";

    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final String DYNAMIC_OUTPUTS = "dynamicOutputs";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String TOOL_OUTPUT_FILE = "toolOutputFile";

    private static final String TOOL_INPUT_FILE = "toolInputFile";

    private static final String OUTGOING_DIRECTORY = "outgoingDirectory";

    private static final String INCOMING_DIRECTORY = "incomingDirectory";

    private static final String CPACS_VARIABLE_IN_FILE = "cpacsVariableInFile";

    private static final String CPACS_OUT_FILE = "cpacsOutFile";

    private static final String CPACS_IN_FILE = "cpacsInFile";

    private static final long serialVersionUID = 2710680638841835660L;

    private String cpacsInFileReference;

    private String cpacsOutFileReference;

    private String cpacsVariableInFileReference;

    private Map<String, String> incomingDirectoryReferences;

    private Map<String, String> outgoingDirectoryReference;

    private Map<String, String> dynamicInputs;

    private Map<String, String> dynamicOutputs;

    private String toolInFileReference;

    private String toolOutFileReference;

    private String identifier;

    public ToolWrapperComponentHistoryDataItem(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
    }

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        String commonDataString = super.serialize(serializer);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(commonDataString);

        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        if (cpacsInFileReference != null) {
            ((ObjectNode) rootNode).put(CPACS_IN_FILE, cpacsInFileReference);
        }
        if (cpacsOutFileReference != null) {
            ((ObjectNode) rootNode).put(CPACS_OUT_FILE, cpacsOutFileReference);
        }
        if (cpacsVariableInFileReference != null) {
            ((ObjectNode) rootNode).put(CPACS_VARIABLE_IN_FILE, cpacsVariableInFileReference);
        }
        if (incomingDirectoryReferences != null) {
            ((ObjectNode) rootNode).put(INCOMING_DIRECTORY, mapper.writeValueAsString(incomingDirectoryReferences));
        }
        if (outgoingDirectoryReference != null) {
            ((ObjectNode) rootNode).put(OUTGOING_DIRECTORY, mapper.writeValueAsString(outgoingDirectoryReference));
        }
        if (incomingDirectoryReferences != null) {
            ((ObjectNode) rootNode).put(DYNAMIC_INPUTS, mapper.writeValueAsString(dynamicInputs));
        }
        if (outgoingDirectoryReference != null) {
            ((ObjectNode) rootNode).put(DYNAMIC_OUTPUTS, mapper.writeValueAsString(dynamicOutputs));
        }
        if (toolInFileReference != null) {
            ((ObjectNode) rootNode).put(TOOL_INPUT_FILE, toolInFileReference);
        }
        if (toolOutFileReference != null) {
            ((ObjectNode) rootNode).put(TOOL_OUTPUT_FILE, toolOutFileReference);
        }
        return rootNode.toString();
    }

    /**
     * @param historyData text representation of {@link ScriptComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @param identifier identifier representing the component
     * @return new {@link ScriptComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static ToolWrapperComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer, String identifier)
        throws IOException {
        ToolWrapperComponentHistoryDataItem historyDataItem = new ToolWrapperComponentHistoryDataItem(identifier);
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        ToolWrapperComponentHistoryDataItem.readReferences(historyData, historyDataItem);

        return historyDataItem;
    }

    @SuppressWarnings("unchecked")
    private static void readReferences(String historyData, ToolWrapperComponentHistoryDataItem historyDataItem)
        throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);

        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        if (((ObjectNode) rootNode).get(CPACS_IN_FILE) != null) {
            historyDataItem.cpacsInFileReference = ((ObjectNode) rootNode).get(CPACS_IN_FILE).getTextValue();
        }
        if (((ObjectNode) rootNode).get(CPACS_OUT_FILE) != null) {
            historyDataItem.cpacsOutFileReference = ((ObjectNode) rootNode).get(CPACS_OUT_FILE).getTextValue();
        }
        if (((ObjectNode) rootNode).get(CPACS_VARIABLE_IN_FILE) != null) {
            historyDataItem.cpacsVariableInFileReference = ((ObjectNode) rootNode).get(CPACS_VARIABLE_IN_FILE).getTextValue();
        }
        if (((ObjectNode) rootNode).get(INCOMING_DIRECTORY) != null) {
            historyDataItem.incomingDirectoryReferences =
                mapper.readValue(((ObjectNode) rootNode).get(INCOMING_DIRECTORY).getTextValue(), new HashMap<String, String>().getClass());
        }
        if (((ObjectNode) rootNode).get(OUTGOING_DIRECTORY) != null) {
            historyDataItem.outgoingDirectoryReference =
                mapper.readValue(((ObjectNode) rootNode).get(OUTGOING_DIRECTORY).getTextValue(), new HashMap<String, String>().getClass());
        }
        if (((ObjectNode) rootNode).get(DYNAMIC_INPUTS) != null) {
            historyDataItem.dynamicInputs =
                mapper.readValue(((ObjectNode) rootNode).get(DYNAMIC_INPUTS).getTextValue(), new HashMap<String, String>().getClass());
        }
        if (((ObjectNode) rootNode).get(DYNAMIC_OUTPUTS) != null) {
            historyDataItem.dynamicOutputs =
                mapper.readValue(((ObjectNode) rootNode).get(DYNAMIC_OUTPUTS).getTextValue(), new HashMap<String, String>().getClass());
        }
        if (((ObjectNode) rootNode).get(TOOL_INPUT_FILE) != null) {
            historyDataItem.toolInFileReference = ((ObjectNode) rootNode).get(TOOL_INPUT_FILE).getTextValue();
        }
        if (((ObjectNode) rootNode).get(TOOL_OUTPUT_FILE) != null) {
            historyDataItem.toolOutFileReference = ((ObjectNode) rootNode).get(TOOL_OUTPUT_FILE).getTextValue();
        }
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCpacsInFileReference() {
        return cpacsInFileReference;
    }

    public void setCpacsInFileReference(String cpacsInFileReference) {
        this.cpacsInFileReference = cpacsInFileReference;
    }

    public String getCpacsOutFileReference() {
        return cpacsOutFileReference;
    }

    public void setCpacsOutFileReference(String cpacsOutFileReference) {
        this.cpacsOutFileReference = cpacsOutFileReference;
    }

    public String getCpacsVariableInFileReference() {
        return cpacsVariableInFileReference;
    }

    public void setCpacsVariableInFileReference(String cpacsVariableInFileReference) {
        this.cpacsVariableInFileReference = cpacsVariableInFileReference;
    }

    public Map<String, String> getIncomingDirectoryReferences() {
        return incomingDirectoryReferences;
    }

    public void setIncomingDirectoryReference(Map<String, String> incomingDirectoryReference) {
        this.incomingDirectoryReferences = incomingDirectoryReference;
    }

    public Map<String, String> getOutgoingDirectoryReferences() {
        return outgoingDirectoryReference;
    }

    public void setOutgoingDirectoryReference(Map<String, String> outgoingDirectoryReference) {
        this.outgoingDirectoryReference = outgoingDirectoryReference;
    }

    public String getToolInFileReference() {
        return toolInFileReference;
    }

    public void setToolInFileReference(String toolInFileReference) {
        this.toolInFileReference = toolInFileReference;
    }

    public String getToolOutFileReference() {
        return toolOutFileReference;
    }

    public void setToolOutFileReference(String toolOutFileReference) {
        this.toolOutFileReference = toolOutFileReference;
    }

    public Map<String, String> getDynamicInputs() {
        return dynamicInputs;
    }

    public void setDynamicInputs(Map<String, String> dynamicInputs) {
        this.dynamicInputs = dynamicInputs;
    }

    public Map<String, String> getDynamicOutputs() {
        return dynamicOutputs;
    }

    public void setDynamicOutputs(Map<String, String> dynamicOutput) {
        this.dynamicOutputs = dynamicOutput;
    }
}
