/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NodeIdentityInformation;
import de.rcenvironment.core.communication.model.NodeInformation;
import de.rcenvironment.core.communication.model.NodeInformationRegistry;

/**
 * Central registry for information gathered about nodes.
 * 
 * @author Robert Mischke
 */
public class NodeInformationRegistryImpl implements NodeInformationRegistry {

    private static final NodeInformationRegistryImpl sharedInstance = new NodeInformationRegistryImpl();

    private Map<String, NodeInformationHolder> idToHolderMap = new HashMap<String, NodeInformationHolder>();

    // TODO this shared map does not properly reflect multi-instance tests; changes required?
    public static NodeInformationRegistryImpl getInstance() {
        return sharedInstance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.communication.model.NodeInformationRegistry#getNodeInformation(java.lang.String)
     */
    @Override
    public NodeInformation getNodeInformation(String id) {
        return getWritableNodeInformation(id);
    }

    /**
     * Provides direct, write-enabled access to {@link NodeInformationHolder}s. Not part of the {@link NodeInformationRegistry} interface as
     * it is intended for bundle-internal use only.
     * 
     * @param id the id of the relevant node
     * @return the writable {@link NodeInformationHolder}
     */
    public NodeInformationHolder getWritableNodeInformation(String id) {
        synchronized (idToHolderMap) {
            NodeInformationHolder holder = idToHolderMap.get(id);
            if (holder == null) {
                holder = new NodeInformationHolder();
                idToHolderMap.put(id, holder);
            }
            return holder;
        }
    }

    /**
     * Updates the associated information for a node from a received or locally-generated {@link InitialNodeInformation} object.
     * 
     * @param remoteNodeInformation the object to update from
     */
    public void updateFrom(InitialNodeInformation remoteNodeInformation) {
        String nodeId = remoteNodeInformation.getNodeId().getIdString();
        NodeInformationHolder writableNodeInformation = getWritableNodeInformation(nodeId);
        writableNodeInformation.setDisplayName(remoteNodeInformation.getDisplayName());
    }

    /**
     * Updates the associated information for a node from a received or locally-generated {@link NodeIdentityInformation} object.
     * 
     * @param identityInformation the object to update from
     */
    public void updateFrom(NodeIdentityInformation identityInformation) {
        String nodeId = identityInformation.getPersistentNodeId();
        NodeInformationHolder writableNodeInformation = getWritableNodeInformation(nodeId);
        writableNodeInformation.setDisplayName(identityInformation.getDisplayName());
    }

}
