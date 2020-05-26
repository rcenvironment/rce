/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
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

    private InstanceNodeSessionId localInstanceSessionId;

    private LogicalNodeSessionId localDefaultLogicalNodeSessionId;

    private NodeConfigurationService nodeConfigurationService;

    private InstanceNodeId localInstanceId;

    private LogicalNodeId localDefaultLogicalNodeId;

    // private final Log log = LogFactory.getLog(getClass());

    /**
     * Initialization; called by OSGi-DS and integration tests.
     */
    public void activate() {
        localInitialNodeInformation = nodeConfigurationService.getInitialNodeInformation();

        // perform all conversions once as they may be fetched frequently
        localInstanceSessionId = localInitialNodeInformation.getInstanceNodeSessionId();
        localInstanceId = localInstanceSessionId.convertToInstanceNodeId();
        localDefaultLogicalNodeId = localInstanceSessionId.convertToDefaultLogicalNodeId();
        localDefaultLogicalNodeSessionId = localInstanceSessionId.convertToDefaultLogicalNodeSessionId();

        // register a preliminary name for proper log output (instead of <unknown> or similar); this is replaced with the
        // actual display name when the local node receives the callback for its own node properties (as from any other node)
        final NodeIdentifierService nodeIdentifierService = nodeConfigurationService.getNodeIdentifierService();
        nodeIdentifierService.associateDisplayName(localInstanceSessionId, "<local instance>");
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
    public InstanceNodeId getLocalInstanceNodeId() {
        return localInstanceId;
    }

    @Override
    @AllowRemoteAccess
    public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
        return localInstanceSessionId;
    }

    @Override
    public LogicalNodeId getLocalDefaultLogicalNodeId() {
        return localDefaultLogicalNodeId;
    }

    @Override
    public LogicalNodeSessionId getLocalDefaultLogicalNodeSessionId() {
        return localDefaultLogicalNodeSessionId;
    }

    @Override
    public boolean matchesLocalInstance(ResolvableNodeId identifier) {
        Assertions.isDefined(identifier, "NodeIdentifier must not be null.");
        // TODO >=8.0: review whether this is the best possible implementation considering possible InstanceId collisions - misc_ro
        return localInitialNodeInformation.getInstanceNodeSessionId().isSameInstanceNodeAs(identifier);
    }

}
