/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.activemq.testutils;

import java.util.concurrent.atomic.AtomicInteger;

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.impl.NetworkContactPointImpl;
import de.rcenvironment.core.communication.testutils.NetworkContactPointGenerator;
import de.rcenvironment.core.communication.transport.jms.activemq.impl.ActiveMQTransportProvider;

/**
 * A generator for {@link NetworkContactPoint}s to be used in ActiveMQ transport tests. These
 * {@link NetworkContactPoint}s represent TCP ports on the local machine.
 * 
 * @author Robert Mischke
 * 
 */
public class ActiveMQNetworkContactPointGenerator implements NetworkContactPointGenerator {

    // TODO optimistic, trivial approach: use ports from 20000 upward
    private static final AtomicInteger NEXT_PORT_GENERATOR = new AtomicInteger(20000);

    private String host;

    public ActiveMQNetworkContactPointGenerator(String host) {
        this.host = host;
    }

    @Override
    public NetworkContactPoint createContactPoint() {
        return new NetworkContactPointImpl(host, NEXT_PORT_GENERATOR.getAndIncrement(), ActiveMQTransportProvider.TRANSPORT_ID);
    }

}
