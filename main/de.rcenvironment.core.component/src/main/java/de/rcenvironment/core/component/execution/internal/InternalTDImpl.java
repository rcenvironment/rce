/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.types.api.InternalTD;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Internal value sent to ouputs to control the workflow.
 * 
 * @author Doreen Seider
 */
public class InternalTDImpl implements InternalTD {

    private static final String SERIALIZE_KEY_TYPE = "t";
    
    private static final String SERIALIZE_KEY_IDENTIFIER = "i";
    
    private static final String SERIALIZE_KEY_HOPS = "h";
    
    private static final String SERIALIZE_KEY_PAYLOAD = "p";
    
    /**
     * Type of possible purposes an {@link InternalTDImpl} can have.
     * 
     * @author Doreen Seider
     */
    public enum InternalTDType {
        
        /** Used to detect if a workflow is finished. */
        WorkflowFinish,
        
        /** Used to reset nested loops. */
        NestedLoopReset,
        
        /** Used to announce failure to loop driver. */
        FailureInLoop;
    }
    
    private final String identifier;
    
    private final InternalTDType type;
    
    private Queue<WorkflowGraphHop> hopsToTraverse = null;
    
    private String payload = null;
    
    public InternalTDImpl(InternalTDType type) {
        this(type, UUID.randomUUID().toString());
    }
    
    public InternalTDImpl(InternalTDType type, String identifier) {
        this.identifier = identifier;
        this.type = type;
    }
    
    public InternalTDImpl(InternalTDType type, Queue<WorkflowGraphHop> hopsToTraverse) {
        this(type, UUID.randomUUID().toString(), hopsToTraverse);
    }
    
    public InternalTDImpl(InternalTDType type, String identifier, Queue<WorkflowGraphHop> hopsToTraverse) {
        this(type, identifier);
        this.hopsToTraverse = hopsToTraverse;
    }
    
    public InternalTDImpl(InternalTDType type, Queue<WorkflowGraphHop> hopsToTraverse, String payload) {
        this(type, UUID.randomUUID().toString(), hopsToTraverse, payload);
    }
    
    public InternalTDImpl(InternalTDType type, String identifier, Queue<WorkflowGraphHop> hopsToTraverse, String payload) {
        this(type, identifier);
        this.hopsToTraverse = hopsToTraverse;
        this.payload = payload;
    }

    @Override
    public DataType getDataType() {
        return DataType.Internal;
    }
    
    public String getIdentifier() {
        return identifier;
    }

    public InternalTDType getType() {
        return type;
    }
    
    public Queue<WorkflowGraphHop> getHopsToTraverse() {
        return hopsToTraverse;
    }
    
    public String getPayload() {
        return payload;
    }
    
    @Override
    public String toString() {
        return "Internal: " + getType().name();
    }
    
    /**
     * @return serialized {@link InternalTDImpl}
     */
    public String serialize() {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(SERIALIZE_KEY_TYPE, getType().name());
        rootNode.put(SERIALIZE_KEY_IDENTIFIER, getIdentifier());
        ArrayNode hopsArrayNode = mapper.createArrayNode();
        Queue<WorkflowGraphHop> hops = getHopsToTraverse();
        if (hops != null) {
            for (WorkflowGraphHop hop : getHopsToTraverse()) {
                ArrayNode hopArrayNode = mapper.createArrayNode();
                hopArrayNode.add(hop.getHopExecutionIdentifier());
                hopArrayNode.add(hop.getHopOuputName());
                hopArrayNode.add(hop.getTargetExecutionIdentifier());
                hopArrayNode.add(hop.getTargetInputName());
                hopsArrayNode.add(hopArrayNode);
            }
            rootNode.put(SERIALIZE_KEY_HOPS, hopsArrayNode);
        }
        String pyld = getPayload();
        if (pyld != null) {
            rootNode.put(SERIALIZE_KEY_PAYLOAD, pyld);
        }
        return rootNode.toString();
    }
    
    /**
     * Creates {@link InternalTDImpl} instance out of a serialized string.
     * 
     * @param value serialized string
     * @return {@link InternalTDImpl} instance
     */
    public static InternalTDImpl fromString(String value) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode rootNode;
        try {
            rootNode = (ObjectNode) mapper.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        InternalTDType type = InternalTDImpl.InternalTDType.valueOf(rootNode.get(SERIALIZE_KEY_TYPE).getTextValue());
        String identifier = rootNode.get(SERIALIZE_KEY_IDENTIFIER).getTextValue();
        Queue<WorkflowGraphHop> hops = null;
        if (rootNode.has(SERIALIZE_KEY_HOPS)) {
            ArrayNode hopsArrayNode = (ArrayNode) rootNode.get(SERIALIZE_KEY_HOPS);
            hops = new LinkedList<>();
            Iterator<JsonNode> hopsElements = hopsArrayNode.getElements();
            while (hopsElements.hasNext()) {
                ArrayNode hopArrayNode = (ArrayNode) hopsElements.next();
                hops.add(new WorkflowGraphHop(hopArrayNode.get(0).asText(), hopArrayNode.get(1).asText(),
                    hopArrayNode.get(2).asText(), hopArrayNode.get(3).asText()));
            }
        }
        String pyld = null;
        if (rootNode.has(SERIALIZE_KEY_PAYLOAD)) {
            pyld = rootNode.get(SERIALIZE_KEY_PAYLOAD).getTextValue();
        }
        return new InternalTDImpl(type, identifier, hops, pyld);
    }

}
