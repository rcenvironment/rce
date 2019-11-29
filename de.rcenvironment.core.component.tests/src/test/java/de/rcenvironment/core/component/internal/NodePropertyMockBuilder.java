/*
 * Copyright (C) 2006-2019 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.internal;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;

/**
 * Builder class for mocks of {@link NodeProperty}. Only to be used in unit testing. Using this class makes code more readable that mocking
 * all methods individually.
 *
 * @author Alexander Weinert
 */
public class NodePropertyMockBuilder {
    private final NodeProperty nodeProperty = EasyMock.createNiceMock(NodeProperty.class);
    
    /**
     * @param id The ID to be returned by {@link NodeProperty#getInstanceNodeSessionId()}
     * @return This object for daisy-chaining.
     */
    public NodePropertyMockBuilder instanceNodeSessionId(InstanceNodeSessionId id) {
        EasyMock.expect(nodeProperty.getInstanceNodeSessionId()).andStubReturn(id);
        return this;
    }

    /**
     * @param id The ID to be returned by {@link NodeProperty#getInstanceNodeSessionIdString()}
     * @return This object for daisy-chaining.
     */
    public NodePropertyMockBuilder instanceNodeSessionIdString(String id) {
        EasyMock.expect(nodeProperty.getInstanceNodeSessionIdString()).andStubReturn(id);
        return this;
    }

    /**
     * @param key The key to be returned by {@link NodeProperty#getDistributedUniqueKey()}
     * @return This object for daisy-chaining.
     */
    public NodePropertyMockBuilder distributedUniqueKey(String key) {
        EasyMock.expect(nodeProperty.getDistributedUniqueKey()).andStubReturn(key);
        return this;
    }

    /**
     * @param key The key to be returned by {@link NodeProperty#getKey()}
     * @return This object for daisy-chaining.
     */
    public NodePropertyMockBuilder key(String key) {
        EasyMock.expect(nodeProperty.getKey()).andStubReturn(key);
        return this;
    }

    /**
     * @param value The value to be returned by {@link NodeProperty#getValue()}
     * @return This object for daisy-chaining.
     */
    public NodePropertyMockBuilder value(String value) {
        EasyMock.expect(nodeProperty.getValue()).andStubReturn(value);
        return this;
    }

    /**
     * @param sequenceNo The sequence number to be returned by {@link NodeProperty#getSequenceNo()}
     * @return This object for daisy-chaining.
     */
    public NodePropertyMockBuilder sequenceNo(long sequenceNo) {
        EasyMock.expect(nodeProperty.getSequenceNo()).andStubReturn(sequenceNo);
        return this;
    }
    
    /**
     * Calls to methods whose return value was not previously configured return the default value of 0 or null.
     * 
     * @return A node property as configured by previous calls to methods of this class.
     */
    public NodeProperty build() {
        EasyMock.replay(this.nodeProperty);
        return this.nodeProperty;
    }
}
