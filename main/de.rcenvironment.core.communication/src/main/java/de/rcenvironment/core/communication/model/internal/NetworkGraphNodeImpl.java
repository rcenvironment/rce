/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.model.internal;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * A disconnected representation of a network node, in the sense that changes to the actual, live network state will not automatically
 * affect instances of this class. It is intended to provide stable "snapshot" representations of the actual network state.
 * 
 * @author Robert Mischke
 */
public final class NetworkGraphNodeImpl implements NetworkGraphNode {

    private final NodeIdentifier nodeId;

    private final Map<String, String> nodeProperties;

    private volatile boolean isLocalNode = false; // volatile to ensure thread visibility

    // private Map<String, String> nodeProperties;

    public NetworkGraphNodeImpl(NodeIdentifier nodeId, Map<String, String> nodeProperties) {
        if (nodeId == null) {
            throw new NullPointerException(String.format("%s / %s", nodeId, nodeProperties));
        }
        this.nodeId = nodeId;
        this.nodeProperties = nodeProperties;
    }

    public NetworkGraphNodeImpl(NodeIdentifier nodeId) {
        this.nodeId = nodeId;
        // implicit property map for unit test
        this.nodeProperties = new HashMap<String, String>();
        nodeProperties.put("displayName", "<" + nodeId.getIdString() + ">");
    }

    @Override
    public NodeIdentifier getNodeId() {
        return nodeId;
    }

    @Override
    public String getDisplayName() {
        String result = null;
        if (nodeProperties != null) {
            nodeProperties.get("displayName"); // TODO use constant
        }
        if (result == null) {
            result = "<unknown>";
        }
        return result;
    }

    @Override
    public boolean isLocalNode() {
        return isLocalNode;
    }

    public void setIsLocalNode(boolean isLocalNode) {
        this.isLocalNode = isLocalNode;
    }

    // public Map<String, String> getNodeProperties() {
    // return nodeProperties;
    // }
    //
    // public void setNodeProperties(Map<String, String> nodeProperties) {
    // this.nodeProperties = nodeProperties;
    // }

    @Override
    public String toString() {
        return String.format("%s ('%s')", nodeId.getIdString(), getDisplayName());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkGraphNodeImpl)) {
            return false;
        }
        return nodeId.equals(((NetworkGraphNodeImpl) obj).nodeId);
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode();
    }
}
