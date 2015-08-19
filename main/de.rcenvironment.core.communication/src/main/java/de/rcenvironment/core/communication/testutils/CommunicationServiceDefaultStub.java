/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;

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
    public Set<NodeIdentifier> getReachableNodes() {
        return null;
    }

    @Override
    public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
        throws IllegalStateException {
        return null;
    }

    @Override
    public Object getService(Class<?> iface, Map<String, String> properties, NodeIdentifier nodeId,
        BundleContext bundleContext) throws IllegalStateException {
        return null;
    }

    @Override
    public void addRuntimeNetworkPeer(String contactPointDefinition) throws CommunicationException {}

    @Override
    public String getFormattedNetworkInformation(String type) {
        return null;
    }

}
