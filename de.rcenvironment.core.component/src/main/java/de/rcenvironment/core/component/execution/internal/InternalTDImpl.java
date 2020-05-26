/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.api.WorkflowGraphPath;
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

    private WorkflowGraphPath hopsToTraverse = null;

    private String payload = null;

    public InternalTDImpl(InternalTDType type) {
        this(type, UUID.randomUUID().toString());
    }

    public InternalTDImpl(InternalTDType type, String identifier) {
        this.identifier = identifier;
        this.type = type;
    }

    public InternalTDImpl(InternalTDType type, WorkflowGraphPath hopsToTraverse) {
        this(type, UUID.randomUUID().toString(), hopsToTraverse);
    }

    public InternalTDImpl(InternalTDType type, String identifier, WorkflowGraphPath hopsToTraverse) {
        this(type, identifier);
        this.hopsToTraverse = hopsToTraverse;
    }

    public InternalTDImpl(InternalTDType type, WorkflowGraphPath hopsToTraverse, String payload) {
        this(type, UUID.randomUUID().toString(), hopsToTraverse, payload);
    }

    public InternalTDImpl(InternalTDType type, String identifier, WorkflowGraphPath hopsToTraverse, String payload) {
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

    public WorkflowGraphPath getHopsToTraverse() {
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
        WorkflowGraphPath hops = getHopsToTraverse();
        if (hops != null) {
            for (WorkflowGraphHop hop : getHopsToTraverse()) {
                ArrayNode hopArrayNode = mapper.createArrayNode();
                hopArrayNode.add(hop.getHopExecutionIdentifier().toString());
                hopArrayNode.add(hop.getHopOuputName());
                hopArrayNode.add(hop.getTargetExecutionIdentifier().toString());
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
    // why is this called "fromString" and not deserialize to match the counter part? --seid_do
    public static InternalTDImpl fromString(String value) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode rootNode;
        try {
            rootNode = (ObjectNode) mapper.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        String typeStr = rootNode.get(SERIALIZE_KEY_TYPE).textValue();
        if (!isValidType(typeStr)) {
            return null;
        }
        InternalTDType type = InternalTDImpl.InternalTDType.valueOf(typeStr);
        String identifier = rootNode.get(SERIALIZE_KEY_IDENTIFIER).textValue();
        WorkflowGraphPath hops = null;
        if (rootNode.has(SERIALIZE_KEY_HOPS)) {
            ArrayNode hopsArrayNode = (ArrayNode) rootNode.get(SERIALIZE_KEY_HOPS);
            hops = WorkflowGraphPath.createEmpty();
            Iterator<JsonNode> hopsElements = hopsArrayNode.elements();
            while (hopsElements.hasNext()) {
                ArrayNode hopArrayNode = (ArrayNode) hopsElements.next();
                hops.append(
                    new WorkflowGraphHop(new ComponentExecutionIdentifier(hopArrayNode.get(0).asText()), hopArrayNode.get(1).asText(),
                        new ComponentExecutionIdentifier(hopArrayNode.get(2).asText()), hopArrayNode.get(3).asText()));
            }
        }
        String pyld = null;
        if (rootNode.has(SERIALIZE_KEY_PAYLOAD)) {
            pyld = rootNode.get(SERIALIZE_KEY_PAYLOAD).textValue();
        }
        return new InternalTDImpl(type, identifier, hops, pyld);
    }

    private static boolean isValidType(String typeStr) {
        for (int i = 0; i < InternalTDType.values().length; i++) {
            if (InternalTDType.values()[i].toString().equals(typeStr)) {
                return true;
            }
        }
        return false;
    }
}
