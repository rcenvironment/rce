/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.Set;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
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
    public <T> T getRemotableService(Class<T> iface, ResolvableNodeId nodeId) {
        return null;
    }

    @Override
    public String getFormattedNetworkInformation(String type) {
        return null;
    }

}
