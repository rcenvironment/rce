/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.impl;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link InitialNodeInformation} implementation.
 * 
 * @author Robert Mischke
 */
public class InitialNodeInformationImpl implements InitialNodeInformation {

    // note: this object is only deserialized *after* protocol version checking, so the serialization is safe
    private static final long serialVersionUID = 6729868652469869965L;

    private transient InstanceNodeSessionId instanceSessionIdObject;

    private String instanceSessionId;

    private String displayName;

    /**
     * Default constructor for bean-style construction.
     */
    public InitialNodeInformationImpl() {
        // NOP
    }

    /**
     * Convenience constructor.
     * 
     * @param localInstanceSessionId
     */
    public InitialNodeInformationImpl(InstanceNodeSessionId localInstanceSessionId) {
        this.instanceSessionIdObject = localInstanceSessionId;
        this.instanceSessionId = localInstanceSessionId.getInstanceNodeSessionIdString();
    }

    public InitialNodeInformationImpl(String id) {
        this.instanceSessionId = id;
    }

    @Override
    public String getInstanceNodeSessionIdString() {
        return instanceSessionId;
    }

    // setter for bean-style construction
    public void setNodeId(String nodeId) {
        this.instanceSessionId = nodeId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public synchronized InstanceNodeSessionId getInstanceNodeSessionId() {
        // create the wrapped object on-the-fly to support bean-style construction
        if (instanceSessionIdObject == null) {
            instanceSessionIdObject = NodeIdentifierUtils.parseInstanceNodeSessionIdStringWithExceptionWrapping(instanceSessionId);
        }
        return instanceSessionIdObject;
    }

    @Override
    public String getLogDescription() {
        String name = displayName;
        if (displayName == null) {
            displayName = "<unnamed>";
        }
        return StringUtils.format("%s [%s]", name, instanceSessionId);
    }

    // NOTE: only intended for use in unit tests; not for production use!
    private String getInternalFingerprint() {
        return StringUtils.format("%s#%s", instanceSessionId, displayName);
    }

    @Override
    public String toString() {
        return StringUtils.format("%s/%s", instanceSessionId, displayName);
    }

    @Override
    // NOTE: only intended for unit tests; not for production use!
    public boolean equals(Object obj) {
        if (!(obj instanceof InitialNodeInformationImpl)) {
            return false;
        }
        return getInternalFingerprint().equals(((InitialNodeInformationImpl) obj).getInternalFingerprint());
    }

    @Override
    // NOTE: only intended for unit tests; not for production use!
    public int hashCode() {
        return getInternalFingerprint().hashCode();
    }

}
