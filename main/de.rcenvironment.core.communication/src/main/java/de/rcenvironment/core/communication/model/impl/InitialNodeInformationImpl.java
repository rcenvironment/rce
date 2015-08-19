/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.impl;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.model.InitialNodeInformation;

/**
 * Default {@link InitialNodeInformation} implementation.
 * 
 * @author Robert Mischke
 */
public class InitialNodeInformationImpl implements InitialNodeInformation {

    // TODO made Serializable for rapid prototyping; change for production
    private static final long serialVersionUID = 6729868652469869965L;

    private String nodeIdString;

    private String displayName;

    private boolean isWorkflowHost;

    private String softwareVersion;

    private String protocolVersion;

    private transient NodeIdentifier wrappedNodeId;

    /**
     * Default constructor for bean-style construction.
     */
    public InitialNodeInformationImpl() {
        // NOP
    }

    /**
     * Convenience constructor.
     * 
     * @param nodeId
     */
    public InitialNodeInformationImpl(NodeIdentifier nodeId) {
        this.nodeIdString = nodeId.getIdString();
    }

    public InitialNodeInformationImpl(String id) {
        this.nodeIdString = id;
    }

    @Override
    public String getNodeIdString() {
        return nodeIdString;
    }

    // setter for bean-style construction
    public void setNodeId(String nodeId) {
        this.nodeIdString = nodeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean getIsWorkflowHost() {
        return isWorkflowHost;
    }

    public void setIsWorkflowHost(boolean isWorkflowHost) {
        this.isWorkflowHost = isWorkflowHost;
    }

    @Override
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    @Override
    public String getNativeProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public synchronized NodeIdentifier getNodeId() {
        // create the wrapped object on-the-fly to support bean-style construction
        if (wrappedNodeId == null) {
            wrappedNodeId = NodeIdentifierFactory.fromNodeId(nodeIdString);
        }
        return wrappedNodeId;
    }

    @Override
    public String getLogDescription() {
        String name = displayName;
        if (displayName == null) {
            displayName = "<unnamed>";
        }
        return String.format("%s [%s]", name, nodeIdString);
    }

    // NOTE: only intended for use in unit tests; not for production use!
    private String getInternalFingerprint() {
        return String.format("%s#%s#%s#%s", nodeIdString, displayName, softwareVersion, protocolVersion);
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%s/%s", nodeIdString, displayName, softwareVersion, protocolVersion);
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
