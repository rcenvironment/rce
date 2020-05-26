/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.model.internal;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A disconnected representation of a network node, in the sense that changes to the actual, live network state will not automatically
 * affect instances of this class. It is intended to provide stable "snapshot" representations of the actual network state.
 * 
 * @author Robert Mischke
 */
public final class NetworkGraphNodeImpl implements NetworkGraphNode {

    private final InstanceNodeSessionId nodeId;

    private final Map<String, String> nodeProperties;

    private volatile boolean isLocalNode = false; // volatile to ensure thread visibility

    // private Map<String, String> nodeProperties;

    public NetworkGraphNodeImpl(InstanceNodeSessionId nodeId, Map<String, String> nodeProperties) {
        if (nodeId == null) {
            throw new NullPointerException(StringUtils.format("%s / %s", nodeId, nodeProperties));
        }
        this.nodeId = nodeId;
        this.nodeProperties = nodeProperties;
    }

    public NetworkGraphNodeImpl(InstanceNodeSessionId nodeId) {
        this.nodeId = nodeId;
        // implicit property map for unit test
        this.nodeProperties = new HashMap<String, String>();
        nodeProperties.put("displayName", "<" + nodeId.getInstanceNodeSessionIdString() + ">");
    }

    @Override
    public InstanceNodeSessionId getNodeId() {
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
        return StringUtils.format("%s ('%s')", nodeId.getInstanceNodeSessionIdString(), getDisplayName());
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
