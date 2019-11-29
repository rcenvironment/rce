/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.api;

import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Default implementation of {@link ComponentHistoryDataItem} containing inputs and outputs.
 * 
 * @author Doreen Seider
 */
public class DefaultComponentHistoryDataItem implements ComponentHistoryDataItem {

    /** Serialization key for inputs. */
    public static final String INPUTS = "in";

    /** Serialization key for ouputs. */
    public static final String OUTPUTS = "out";

    /** Serialization key for timestamp. */
    public static final String TIMESTAMP = "ts";

    /** Serialization key for name. */
    public static final String NAME = "n";

    /** Serialization key for value. */
    public static final String VALUE = "v";

    private static final long serialVersionUID = -3420034372755242546L;

    private static final String FORMAT_VERSION_COMMON = "f_vc";

    private static final String FORMAT_VERSION_1 = "1";

    private static final String CURRENT_FORMAT_VERSION = FORMAT_VERSION_1;

    protected Map<String, Deque<EndpointHistoryDataItem>> inputs = Collections.synchronizedMap(new HashMap<String,
        Deque<EndpointHistoryDataItem>>());

    protected Map<String, Deque<EndpointHistoryDataItem>> outputs = Collections.synchronizedMap(new HashMap<String,
        Deque<EndpointHistoryDataItem>>());

    // MetaData for Inputs
    protected Map<String, Map<String, String>> inputMetaData = Collections.synchronizedMap(new HashMap<String, Map<String, String>>());

    // MetaData for Outputs
    protected Map<String, Map<String, String>> outputMetaData = Collections.synchronizedMap(new HashMap<String, Map<String, String>>());

    private String identifier;

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public synchronized String serialize(TypedDatumSerializer serializer) throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(FORMAT_VERSION_COMMON, CURRENT_FORMAT_VERSION);
        rootNode.put(INPUTS, getEndpointsAsJsonObjectNode(inputs, mapper, serializer));
        rootNode.put(OUTPUTS, getEndpointsAsJsonObjectNode(outputs, mapper, serializer));
        return rootNode.toString();
    }

    public Map<String, Deque<EndpointHistoryDataItem>> getInputs() {
        return inputs;
    }

    public Map<String, Deque<EndpointHistoryDataItem>> getOutputs() {
        return outputs;
    }

    /**
     * Get the endpoint meta data for an endpoint.
     * 
     * @param endpointName name of the endpoint.
     * @return map containing the metadata.
     */
    public Map<String, String> getMetaDataForInput(String endpointName) {
        return inputMetaData.get(endpointName);
    }

    /**
     * Get the endpoint meta data for an endpoint.
     * 
     * @param endpointName name of the endpoint.
     * @return map containing the metadata.
     */
    public Map<String, String> getMetaDataForOutput(String endpointName) {
        return outputMetaData.get(endpointName);
    }

    /**
     * @param historyData text representation of {@link ScriptComponentHistoryDataItem}
     * @param serializer {@link TypedDatumSerializer} instance
     * @return new {@link ScriptComponentHistoryDataItem} object
     * @throws IOException on error
     */
    public static DefaultComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer)
        throws IOException {
        DefaultComponentHistoryDataItem historyDataItem = new DefaultComponentHistoryDataItem();
        DefaultComponentHistoryDataItem.initializeDefaultHistoryDataFromString(historyDataItem, historyData, serializer);

        return historyDataItem;
    }

    protected static void initializeDefaultHistoryDataFromString(DefaultComponentHistoryDataItem historyDataItem,
        String historyData, TypedDatumSerializer serializer) throws IOException {
        historyDataItem.inputs = DefaultComponentHistoryDataItem.getInputsFromString(historyData, serializer);
        historyDataItem.outputs = DefaultComponentHistoryDataItem.getOutputsFromString(historyData, serializer);
    }

    private ObjectNode getEndpointsAsJsonObjectNode(Map<String, Deque<EndpointHistoryDataItem>> endpoints, ObjectMapper mapper,
        TypedDatumSerializer serializer) {

        ObjectNode endpointObjectNode = mapper.createObjectNode();

        for (String inputName : endpoints.keySet()) {
            ArrayNode endpointArrayNode = mapper.createArrayNode();
            for (EndpointHistoryDataItem endpointData : endpoints.get(inputName)) {
                ObjectNode endpointDataObjectNode = mapper.createObjectNode();
                endpointDataObjectNode.put(TIMESTAMP, endpointData.getTimestamp());
                endpointDataObjectNode.put(NAME, endpointData.getEndpointName());
                endpointDataObjectNode.put(VALUE, serializer.serialize(endpointData.getValue()));
                endpointArrayNode.add(endpointDataObjectNode);
            }
            endpointObjectNode.put(inputName, endpointArrayNode);
        }

        return endpointObjectNode;
    }

    /**
     * @param inputName name of input to add
     * @param value value of input to add
     */
    public synchronized void addInput(String inputName, TypedDatum value) {
        if (!inputs.containsKey(inputName)) {
            inputs.put(inputName, new LinkedList<EndpointHistoryDataItem>());
        }
        inputs.get(inputName).addLast(new EndpointHistoryDataItem(System.currentTimeMillis(), inputName, value));
    }

    /**
     * @param inputName name of input to add
     * @param metaData the metaData to add
     */
    public synchronized void setInputMetaData(String inputName, Map<String, String> metaData) {
        inputMetaData.put(inputName, metaData);
    }

    /**
     * @param outputName name of output 
     * @param value value of output to add
     */
    public synchronized void addOutput(String outputName, TypedDatum value) {
        if (!outputs.containsKey(outputName)) {
            outputs.put(outputName, new LinkedList<EndpointHistoryDataItem>());
        }
        outputs.get(outputName).addLast(new EndpointHistoryDataItem(System.currentTimeMillis(), outputName, value));
    }
    
    /**
     * @param outputName name of output
     * @param metaData the metaData to add
     */
    public synchronized void setOutputMetaData(String outputName, Map<String, String> metaData) {
        outputMetaData.put(outputName, metaData);
    }

    private static Map<String, Deque<EndpointHistoryDataItem>> getInputsFromString(String endpoints, TypedDatumSerializer serializer)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            JsonNode tree = mapper.readTree(endpoints);
            return getEndpointsFromString((ObjectNode) tree.get(INPUTS), serializer);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    private static Map<String, Deque<EndpointHistoryDataItem>> getOutputsFromString(String endpoints, TypedDatumSerializer serializer)
        throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            JsonNode tree = mapper.readTree(endpoints);
            return getEndpointsFromString((ObjectNode) tree.get(OUTPUTS), serializer);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    private static Map<String, Deque<EndpointHistoryDataItem>> getEndpointsFromString(ObjectNode endpointObjectNode,
        TypedDatumSerializer serializer) {
        Map<String, Deque<EndpointHistoryDataItem>> endpoints = Collections.synchronizedMap(new HashMap<String,
            Deque<EndpointHistoryDataItem>>());

        if (endpointObjectNode != null) {
            Iterator<String> endpointNamesIterator = endpointObjectNode.fieldNames();
            while (endpointNamesIterator.hasNext()) {
                String endpointName = endpointNamesIterator.next();
                endpoints.put(endpointName, new LinkedList<EndpointHistoryDataItem>());
                ArrayNode endpointJsonArray = (ArrayNode) endpointObjectNode.get(endpointName);
                Iterator<JsonNode> endpointDataObjectNodesIterator = endpointJsonArray.elements();
                while (endpointDataObjectNodesIterator.hasNext()) {
                    ObjectNode endpointDataObjectNode = (ObjectNode) endpointDataObjectNodesIterator.next();
                    EndpointHistoryDataItem endpointHistoryData = new EndpointHistoryDataItem(
                        endpointDataObjectNode.get(TIMESTAMP).longValue(),
                        endpointDataObjectNode.get(NAME).textValue(),
                        serializer.deserialize(endpointDataObjectNode.get(VALUE).textValue()));
                    endpoints.get(endpointName).add(endpointHistoryData);
                }
            }
        }

        return endpoints;
    }
}
