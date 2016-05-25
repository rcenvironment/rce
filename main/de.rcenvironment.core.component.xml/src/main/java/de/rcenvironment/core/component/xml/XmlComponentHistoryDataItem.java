/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.xml;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link CommonComponentHistoryDataItem} implementation for XML components.
 * 
 * @author Jan Flink
 */
public class XmlComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    protected static final String FORMAT_VERSION_1 = "1";

    protected static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    private static final long serialVersionUID = -6610340731496850793L;

    private static final String PLAIN_XML_FILE_REFERENCE = "plainXML";

    private static final String XML_WITH_VARIABLES_FILE_REFERENCE = "xmlWithVariables";

    private String xmlWithVariablesFileReference;

    private String plainXMLFileReference;

    private String identifier;

    public XmlComponentHistoryDataItem(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getFormatVersion() {
        return StringUtils.escapeAndConcat(super.getFormatVersion(), CURRENT_FORMAT_VERSION);
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
        if (xmlWithVariablesFileReference != null) {
            ((ObjectNode) rootNode).put(XML_WITH_VARIABLES_FILE_REFERENCE, xmlWithVariablesFileReference);
        }
        if (plainXMLFileReference != null) {
            ((ObjectNode) rootNode).put(PLAIN_XML_FILE_REFERENCE, plainXMLFileReference);
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
    public static XmlComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer, String identifier)
        throws IOException {
        XmlComponentHistoryDataItem historyDataItem = new XmlComponentHistoryDataItem(identifier);
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        XmlComponentHistoryDataItem.readXMLFileReferencesFromString(historyData, historyDataItem);

        return historyDataItem;
    }

    private static void readXMLFileReferencesFromString(String historyData, XmlComponentHistoryDataItem historyDataItem)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);

        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        if (((ObjectNode) rootNode).get(XML_WITH_VARIABLES_FILE_REFERENCE) != null) {
            historyDataItem.xmlWithVariablesFileReference = ((ObjectNode) rootNode).get(XML_WITH_VARIABLES_FILE_REFERENCE).getTextValue();
        }
        if (((ObjectNode) rootNode).get(PLAIN_XML_FILE_REFERENCE) != null) {
            historyDataItem.plainXMLFileReference = ((ObjectNode) rootNode).get(PLAIN_XML_FILE_REFERENCE).getTextValue();
        }
    }

    public String getXmlWithVariablesFileReference() {
        return xmlWithVariablesFileReference;
    }

    public void setXmlWithVariablesFileReference(String xmlWithVariablesFileReference) {
        this.xmlWithVariablesFileReference = xmlWithVariablesFileReference;
    }

    public void setPlainXMLFileReference(String plainXMLFileReference) {
        this.plainXMLFileReference = plainXMLFileReference;
    }

    public String getPlainXmlFileReference() {
        return plainXMLFileReference;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
