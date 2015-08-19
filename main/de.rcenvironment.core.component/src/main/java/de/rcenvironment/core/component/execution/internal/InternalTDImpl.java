/*
 * Copyright (C) 2006-2015 DLR, Germany
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

/**
 * Internal value sent to ouputs to control the workflow.
 * 
 * @author Doreen Seider
 */
public class InternalTDImpl implements InternalTD {

    private static final String SERIALIZE_KEY_TYPE = "t";
    
    private static final String SERIALIZE_KEY_IDENTIFIER = "i";
    
    private static final String SERIALIZE_KEY_HOPS = "h";
    
    /**
     * Type of possible purposes an {@link InternalTDImpl} can have.
     * 
     * @author Doreen Seider
     */
    public enum InternalTDType {
        
        /** Used to detect if a workflow is finished. */
        WorkflowFinish,
        
        /** Used to reset nested loops. */
        NestedLoopReset;
    }
    
    private final String identifier;
    
    private final InternalTDType type;
    
    private Queue<WorkflowGraphHop> resetCycleHops = null;
    
    public InternalTDImpl(InternalTDType type) {
        this(type, UUID.randomUUID().toString());
    }
    
    public InternalTDImpl(InternalTDType type, String identifier) {
        this.identifier = identifier;
        this.type = type;
    }
    
    public InternalTDImpl(InternalTDType type, Queue<WorkflowGraphHop> resetCycleHops) {
        this(type, UUID.randomUUID().toString(), resetCycleHops);
    }
    
    public InternalTDImpl(InternalTDType type, String identifier, Queue<WorkflowGraphHop> resetCycleHops) {
        this(type, identifier);
        this.resetCycleHops = resetCycleHops;
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
        return resetCycleHops;
    }
    
    @Override
    public String toString() {
        return "Internal: " + getType().name();
    }
    
    /**
     * @return serialized {@link InternalTDImpl}
     */
    public String serialize() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(SERIALIZE_KEY_TYPE, getType().name());
        rootNode.put(SERIALIZE_KEY_IDENTIFIER, getIdentifier());
        ArrayNode hopsArrayNode = mapper.createArrayNode();
        Queue<WorkflowGraphHop> graphHops = getHopsToTraverse();
        if (graphHops != null) {
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
        return rootNode.toString();
    }
    
    /**
     * Creates {@link InternalTDImpl} instance out of a serialized string.
     * 
     * @param value serialized string
     * @return {@link InternalTDImpl} instance
     */
    public static InternalTDImpl fromString(String value) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode;
        try {
            rootNode = (ObjectNode) mapper.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        InternalTDType type = InternalTDImpl.InternalTDType.valueOf(rootNode.get(SERIALIZE_KEY_TYPE).getTextValue());
        String identifier = rootNode.get(SERIALIZE_KEY_IDENTIFIER).getTextValue();
        Queue<WorkflowGraphHop> graphHops = null;
        ArrayNode hopsArrayNode = (ArrayNode) rootNode.get(SERIALIZE_KEY_HOPS);
        if (hopsArrayNode != null) {
            graphHops = new LinkedList<>();
            Iterator<JsonNode> hopsElements = hopsArrayNode.getElements();
            while (hopsElements.hasNext()) {
                ArrayNode hopArrayNode = (ArrayNode) hopsElements.next();
                graphHops.add(new WorkflowGraphHop(hopArrayNode.get(0).asText(), hopArrayNode.get(1).asText(),
                    hopArrayNode.get(2).asText(), hopArrayNode.get(3).asText()));
            }
        }
        return new InternalTDImpl(type, identifier, graphHops);
    }

}
