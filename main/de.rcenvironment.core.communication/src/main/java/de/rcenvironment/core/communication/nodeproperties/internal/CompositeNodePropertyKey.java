/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

/**
 * A combined key comprised of a node id and a metadata key.
 * 
 * @author Robert Mischke
 */
public class CompositeNodePropertyKey {

    private final String nodeId;

    private final String dataKey;

    private final String compositeKey;

    public CompositeNodePropertyKey(String nodeId, String dataKey) {
        this.nodeId = nodeId;
        this.dataKey = dataKey;
        this.compositeKey = String.format("%s:%s", nodeId, dataKey);
    }

    public String getNodeIdString() {
        return nodeId;
    }

    public String getDataKey() {
        return dataKey;
    }

    @Override
    public int hashCode() {
        // delegate to compositeKey
        return compositeKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompositeNodePropertyKey)) {
            return false;
        }
        // delegate to compositeKey equality
        return compositeKey.equals(((CompositeNodePropertyKey) obj).compositeKey);
    }

    @Override
    public String toString() {
        return compositeKey;
    }
}
