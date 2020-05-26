/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.virtual.testutils;

import java.util.concurrent.atomic.AtomicInteger;

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.NetworkContactPointImpl;
import de.rcenvironment.core.communication.testutils.NetworkContactPointGenerator;
import de.rcenvironment.core.communication.transport.virtual.VirtualNetworkTransportProvider;

/**
 * A {@link NetworkContactPointGenerator} matching the {@link VirtualNetworkTransportProvider}.
 * Generates virtual {@link NetworkContactPoint}s with a given host name and sequential port
 * numbers.
 * 
 * @author Robert Mischke
 */
public class VirtualNetworkContactPointGenerator implements NetworkContactPointGenerator {

    // keep port numbers unique within the classloader context
    private static AtomicInteger nextPort = new AtomicInteger(1);

    private String hostName;

    /**
     * Constructor.
     * 
     * @param hostName the virtual host name to use for all generated {@link NetworkContactPoint}s.
     */
    public VirtualNetworkContactPointGenerator(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public NetworkContactPoint createContactPoint() {
        return new NetworkContactPointImpl(hostName, nextPort.getAndIncrement(), VirtualNetworkTransportProvider.TRANSPORT_ID);
    }

}
