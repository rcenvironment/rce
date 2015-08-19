/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.datamanagement.api;

import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;

/**
 * {@link ComponentHistoryDataItem} containing the common part, the workflow engine provides and the custom part, which provides the
 * components.
 * 
 * @author Doreen Seider
 */
public class MergedComponentHistoryDateItem implements ComponentHistoryDataItem {

    private static final long serialVersionUID = 2052516404322602047L;

    private final String identifier;
    
    private final String serializedMergedComponentHistoryDateItem;
    
    public MergedComponentHistoryDateItem(ComponentHistoryDataItem first, ComponentHistoryDataItem second,
        TypedDatumSerializer typedDatumSerializer)
        throws JsonProcessingException, IOException {
        identifier = first.getIdentifier();
        serializedMergedComponentHistoryDateItem = mergeTwoJsonNodes(getStringAsJsonNode(first.serialize(typedDatumSerializer)),
            getStringAsJsonNode(second.serialize(typedDatumSerializer))).toString();
    }
    
    private JsonNode getStringAsJsonNode(String serializedJson) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(serializedJson);
    }
    
    private JsonNode mergeTwoJsonNodes(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.getFieldNames();
        while (fieldNames.hasNext()) {

            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            if (jsonNode != null && jsonNode.isObject()) {
                mergeTwoJsonNodes(jsonNode, updateNode.get(fieldName));
            } else {
                if (mainNode instanceof ObjectNode) {
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) mainNode).put(fieldName, value);
                }
            }
        }
        return mainNode;
    }
    
    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        return serializedMergedComponentHistoryDateItem;
    }

}
