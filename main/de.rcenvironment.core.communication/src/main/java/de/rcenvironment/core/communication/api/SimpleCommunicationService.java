/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Class serving as an abstraction of the {@link CommunicationService} regarding the OSGi API.
 * 
 * @author Doreen Seider
 * @author Heinrich Wendel
 * @author Robert Mischke
 */
public class SimpleCommunicationService implements PlatformService {

    private CommunicationService communicationService;
    
    private PlatformService platformService;

    public SimpleCommunicationService() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        communicationService = serviceRegistryAccess.getService(CommunicationService.class);
        platformService = serviceRegistryAccess.getService(PlatformService.class);
    }
    
    /**
     * @return the {@link NodeIdentifier}s of all known and currently reachable nodes in the network
     */
    public Set<NodeIdentifier> getReachableNodes() {
        return communicationService.getReachableNodes();
    }

    /**
     * Checks if the specified {@link NodeIdentifier} represent the local RCE platform.
     * 
     * @param nodeId a {@link NodeIdentifier} that should be compared to the local RCE {@link NodeIdentifier}.
     * @return True or false.
     */
    public boolean isLocalNode(NodeIdentifier nodeId) {
        return platformService.isLocalNode(nodeId);
    }

    public NodeIdentifier getLocalNodeId() {
        return platformService.getLocalNodeId();
    }
}
