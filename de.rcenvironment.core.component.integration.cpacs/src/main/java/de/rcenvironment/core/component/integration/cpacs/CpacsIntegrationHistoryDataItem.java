/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.cpacs;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.integration.IntegrationHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link ComponentHistoryDataItem} for integrated CPACS tools.
 * 
 * @author Jan Flink
 */
public class CpacsIntegrationHistoryDataItem extends IntegrationHistoryDataItem {

    private static final String KEY_CPACS_WITH_VARIABLES_FILE_REFERENCE = "cpacsWithVariables";

    private static final String KEY_TOOL_INPUT_FILE_REFERENCE = "toolInput";

    private static final String KEY_TOOL_INPUT_FILENAME = "toolInputFilename";

    private static final String KEY_TOOL_INPUT_WITHOUT_TOOLSPECIFIC_FILE_REFERENCE = "toolInputWithoutToolspecific";

    private static final String KEY_TOOL_OUTPUT_FILE_REFERENCE = "toolOutput";

    private static final String KEY_TOOL_OUTPUT_FILENAME = "toolOutputFilename";

    private static final long serialVersionUID = 7762053749921116711L;

    private String cpacsWithVariablesFileReference;

    private String tIFileReference;

    private String tOFileReference;

    private String toolInputWithoutToolspecificFileReference;

    private String tIFilename;

    private String tOFilename;

    public CpacsIntegrationHistoryDataItem(String identifier) {
        super(identifier);
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
        if (cpacsWithVariablesFileReference != null) {
            ((ObjectNode) rootNode).put(KEY_CPACS_WITH_VARIABLES_FILE_REFERENCE, cpacsWithVariablesFileReference);
        }
        if (tIFileReference != null) {
            ((ObjectNode) rootNode).put(KEY_TOOL_INPUT_FILE_REFERENCE, tIFileReference);
        }
        if (tIFilename != null) {
            ((ObjectNode) rootNode).put(KEY_TOOL_INPUT_FILENAME, tIFilename);
        }
        if (tOFileReference != null) {
            ((ObjectNode) rootNode).put(KEY_TOOL_OUTPUT_FILE_REFERENCE, tOFileReference);
        }
        if (tOFilename != null) {
            ((ObjectNode) rootNode).put(KEY_TOOL_OUTPUT_FILENAME, tOFilename);
        }
        if (toolInputWithoutToolspecificFileReference != null) {
            ((ObjectNode) rootNode).put(KEY_TOOL_INPUT_WITHOUT_TOOLSPECIFIC_FILE_REFERENCE, toolInputWithoutToolspecificFileReference);
        }
        if (workingDirectory != null) {
            ((ObjectNode) rootNode).put(WORKING_DIRECTORY, workingDirectory);
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
    public static CpacsIntegrationHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer, String identifier)
        throws IOException {
        CpacsIntegrationHistoryDataItem historyDataItem = new CpacsIntegrationHistoryDataItem(identifier);
        historyDataItem.workingDirectory = IntegrationHistoryDataItem.fromString(historyData, serializer, identifier).getWorkingDirectory();
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);
        CpacsIntegrationHistoryDataItem.readXMLFileReferencesFromString(historyData, historyDataItem);
        return historyDataItem;
    }

    private static void readXMLFileReferencesFromString(String historyData, CpacsIntegrationHistoryDataItem historyDataItem)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);

        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        if (((ObjectNode) rootNode).get(KEY_CPACS_WITH_VARIABLES_FILE_REFERENCE) != null) {
            historyDataItem.cpacsWithVariablesFileReference =
                ((ObjectNode) rootNode).get(KEY_CPACS_WITH_VARIABLES_FILE_REFERENCE).textValue();
        }
        if (((ObjectNode) rootNode).get(KEY_TOOL_INPUT_FILE_REFERENCE) != null) {
            historyDataItem.tIFileReference = ((ObjectNode) rootNode).get(KEY_TOOL_INPUT_FILE_REFERENCE).textValue();
        }
        if (((ObjectNode) rootNode).get(KEY_TOOL_INPUT_FILENAME) != null) {
            historyDataItem.tIFilename = ((ObjectNode) rootNode).get(KEY_TOOL_INPUT_FILENAME).textValue();
        }
        if (((ObjectNode) rootNode).get(KEY_TOOL_OUTPUT_FILE_REFERENCE) != null) {
            historyDataItem.tOFileReference = ((ObjectNode) rootNode).get(KEY_TOOL_OUTPUT_FILE_REFERENCE).textValue();
        }
        if (((ObjectNode) rootNode).get(KEY_TOOL_OUTPUT_FILENAME) != null) {
            historyDataItem.tOFilename = ((ObjectNode) rootNode).get(KEY_TOOL_OUTPUT_FILENAME).textValue();
        }
        if (((ObjectNode) rootNode).get(KEY_TOOL_INPUT_WITHOUT_TOOLSPECIFIC_FILE_REFERENCE) != null) {
            historyDataItem.toolInputWithoutToolspecificFileReference =
                ((ObjectNode) rootNode).get(KEY_TOOL_INPUT_WITHOUT_TOOLSPECIFIC_FILE_REFERENCE).textValue();
        }
    }

    public String getCpacsWithVariablesFileReference() {
        return cpacsWithVariablesFileReference;
    }

    public void setCpacsWithVariablesFileReference(String xmlWithVariablesFileReference) {
        this.cpacsWithVariablesFileReference = xmlWithVariablesFileReference;
    }

    public String getToolInputFileReference() {
        return tIFileReference;
    }

    /**
     * Sets filename and file reference id of tool input.
     * 
     * @param toolInputFilename The tool input filename.
     * @param toolInputFileReference The tool input file reference id.
     */
    public void setToolInputFile(String toolInputFilename, String toolInputFileReference) {
        this.tIFilename = toolInputFilename;
        this.tIFileReference = toolInputFileReference;
    }

    public String getToolOutputFileReference() {
        return tOFileReference;
    }

    /**
     * Sets filename and file reference id of tool output.
     * 
     * @param toolOutputFilename The tool output filename.
     * @param toolOutputFileReference The tool output file refernce id.
     */
    public void setToolOutputFile(String toolOutputFilename, String toolOutputFileReference) {
        this.tOFilename = toolOutputFilename;
        this.tOFileReference = toolOutputFileReference;
    }

    public void setToolInputWithoutToolspecificFileReference(String toolInputWithoutToolspecificFileReference) {
        this.toolInputWithoutToolspecificFileReference = toolInputWithoutToolspecificFileReference;
    }

    public String getToolInputWithoutToolspecificFileReference() {
        return toolInputWithoutToolspecificFileReference;
    }

    public String getToolInputFilename() {
        return tIFilename;
    }

    public String getToolOutputFilename() {
        return tOFilename;
    }
}
