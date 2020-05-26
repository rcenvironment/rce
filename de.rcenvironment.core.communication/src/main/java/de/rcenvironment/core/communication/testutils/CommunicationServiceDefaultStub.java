/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.Set;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.ResolvableNodeId;

/**
 * Common test/mock implementations of {@link CommunicationService}. These can be used directly, or can as superclasses for custom mock
 * classes.
 * 
 * Custom mock implementations of {@link CommunicationService} should use these as superclasses whenever possible to avoid code duplication,
 * and to shield the mock classes from irrelevant API changes.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (adapted to new conventions)
 */
public class CommunicationServiceDefaultStub implements CommunicationService {

    @Override
    public Set<InstanceNodeSessionId> getReachableInstanceNodes() {
        return null;
    }

    @Override
    public Set<LogicalNodeId> getReachableLogicalNodes() {
        return null;
    }

    @Override
    public <T> T getRemotableService(Class<T> iface, NetworkDestination nodeId) {
        return null;
    }

    @Override
    public ReliableRPCStreamHandle createReliableRPCStream(ResolvableNodeId targetNodeId) {
        return null;
    }

    @Override
    public void closeReliableRPCStream(ReliableRPCStreamHandle session) {}

    @Override
    public String getFormattedNetworkInformation(String type) {
        return null;
    }

}
