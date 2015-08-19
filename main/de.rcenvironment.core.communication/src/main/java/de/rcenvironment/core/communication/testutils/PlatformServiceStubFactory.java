/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;

/**
 * A factory for common {@link PlatformService} stub implementations to avoid code duplication.
 * 
 * @author Robert Mischke
 */
// TODO check: deprecated? - misc_ro
public abstract class PlatformServiceStubFactory {

    /**
     * An implementation that wraps a host/number NodeIdentifier.
     */
    private static class HostAndNumberStub extends PlatformServiceDefaultStub {

        private NodeIdentifier nodeId;

        private Set<NodeIdentifier> remotePlatforms;

        public HostAndNumberStub(String host, int platformNumber, Collection<NodeIdentifier> remotePlatforms) {
            this.nodeId = NodeIdentifierFactory.fromHostAndNumber(host, platformNumber);
            this.remotePlatforms = new HashSet<NodeIdentifier>(remotePlatforms);
        }

        @Override
        public NodeIdentifier getLocalNodeId() {
            return nodeId;
        }

        @Override
        public boolean isLocalNode(NodeIdentifier testedNodeIdentifier) {
            // perform simple check here; improve if necessary
            return nodeId.equals(testedNodeIdentifier);
        }

    }

    private PlatformServiceStubFactory() {}

    /**
     * @return an implementation with a custom host, instance number, and remote platforms
     * 
     * @param host the host name for the local platform
     * @param platformNumber the platform number for the local platform
     * @param remotePlatforms the set of known platforms to emulate
     */
    @Deprecated
    public static PlatformService createHostAndNumberStub(String host, int platformNumber,
        Collection<NodeIdentifier> remotePlatforms) {
        return new HostAndNumberStub(host, platformNumber, remotePlatforms);
    }

}
