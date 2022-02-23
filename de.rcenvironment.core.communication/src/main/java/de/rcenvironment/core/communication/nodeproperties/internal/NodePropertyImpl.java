/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Represents a single key-value metadata entry of a specific node, with an added timestamp.
 * 
 * @author Robert Mischke
 */
public class NodePropertyImpl implements NodeProperty {

    private final CompositeNodePropertyKey key;

    private final long sequenceNo;

    private final String value;

    private final InstanceNodeSessionId instanceSessionId; // parsed at creation for centralization

    public NodePropertyImpl(String compactForm, NodeIdentifierService nodeIdentifierService) throws IdentifierException {
        String[] parts = StringUtils.splitAndUnescape(compactForm);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Wrong number of segments: " + compactForm);
        }
        this.key = new CompositeNodePropertyKey(parts[0], parts[1]);
        this.sequenceNo = Long.parseLong(parts[2]);
        this.value = parts[3];
        this.instanceSessionId = nodeIdentifierService.parseInstanceNodeSessionIdString(key.getInstanceNodeSessionIdString());
    }

    public NodePropertyImpl(InstanceNodeSessionId instanceSessionId, String dataKey, long sequenceNo, String value) {
        this.key = new CompositeNodePropertyKey(instanceSessionId.getInstanceNodeSessionIdString(), dataKey);
        this.sequenceNo = sequenceNo;
        this.value = value;
        this.instanceSessionId = instanceSessionId;
    }

    public CompositeNodePropertyKey getCompositeKey() {
        return key;
    }

    @Override
    public InstanceNodeSessionId getInstanceNodeSessionId() {
        return instanceSessionId;
    }

    @Override
    public String getInstanceNodeSessionIdString() {
        return key.getInstanceNodeSessionIdString();
    }

    @Override
    public String getKey() {
        return key.getDataKey();
    }

    @Override
    public String getDistributedUniqueKey() {
        return key.getAsUniqueString();
    }

    @Override
    public long getSequenceNo() {
        return sequenceNo;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * @return the compact form of this entry that can be parsed by the single-arg constructor
     */
    public String toCompactForm() {
        return StringUtils.escapeAndConcat(key.getInstanceNodeSessionIdString(), key.getDataKey(), Long.toString(sequenceNo), value);
    }

    /**
     * IMPORTANT: Two {@link NodePropertyImpl} instances are considered "equal" if their composite key is the same, ie they define the same
     * property of the same node. This is intended to support common set operations, but has the side effect that "equals" cannot be used to
     * check for equal sequence numbers and/or values.
     * 
     * @param obj the other object to compare to
     * @return true if equal (see description)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NodePropertyImpl)) {
            return false;
        }
        NodePropertyImpl other = (NodePropertyImpl) obj;
        return key.equals(other.getCompositeKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        // TODO provisional; improve as necessary
        return toCompactForm();
    }

}
