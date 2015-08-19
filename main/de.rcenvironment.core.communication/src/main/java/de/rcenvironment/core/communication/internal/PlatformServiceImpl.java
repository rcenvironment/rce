/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.internal.NodeInformationRegistryImpl;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Implementation of {@link PlatformService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class PlatformServiceImpl implements PlatformService {

    private InitialNodeInformation localInitialNodeInformation;

    private NodeIdentifier localNodeIdentifier;

    private NodeConfigurationService nodeConfigurationService;

    // private final Log log = LogFactory.getLog(getClass());

    /**
     * Constructor; called by OSGi-DS and integration tests.
     */
    public void activate() {
        localInitialNodeInformation = nodeConfigurationService.getInitialNodeInformation();
        localNodeIdentifier = localInitialNodeInformation.getNodeId();

        // register own name for own node id for proper log output
        // TODO refactor/move elsewhere? - misc_ro
        NodeInformationRegistryImpl.getInstance().updateFrom(localInitialNodeInformation);
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindNodeConfigurationService(NodeConfigurationService newInstance) {
        nodeConfigurationService = newInstance;
    }

    @Override
    @AllowRemoteAccess
    public NodeIdentifier getLocalNodeId() {
        return (NodeIdentifier) localNodeIdentifier;
    }

    @Override
    public boolean isLocalNode(NodeIdentifier identifier) {
        Assertions.isDefined(identifier, "NodeIdentifier must not be null.");
        return localInitialNodeInformation.getNodeIdString().equals(identifier.getIdString());
    }

}
