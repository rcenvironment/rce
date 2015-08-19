/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.connection.internal;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.testutils.NetworkContactPointGenerator;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.communication.transport.virtual.VirtualNetworkTransportProvider;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;

/**
 * Unit test for {@link MessageChannelServiceImpl} using the
 * {@link VirtualNetworkTransportProvider}.
 * 
 * @author Robert Mischke
 */
@Ignore("obsolete test container")
// FIXME review: either rework/merge into AbstractTransportLowLevelTest or discard
public class VirtualTransportNetworkConnectionServiceImplTest extends AbstractNetworkConnectionServiceImplTest {

    /**
     * Tests actively connecting instances with passive/inverse connections disabled.
     * 
     * @see AbstractNetworkConnectionServiceImplTest#commonTestActiveConnectionsNoDuplex()
     * 
     * @throws Exception on unintended test exceptions
     */
    @Test
    public void activeConnectionNoDuplex() throws Exception {
        commonTestActiveConnectionsNoDuplex();
    }

    /**
     * Tests an A->B setup with passive/inverse connections enabled.
     * 
     * @see AbstractNetworkConnectionServiceImplTest#commonTestSingleDuplexConnection()
     * 
     * @throws Exception on unintended test exceptions
     */
    @Test
    public void singleDuplexConnection() throws Exception {
        commonTestSingleDuplexConnection();
    }

    @Override
    protected void defineNetworkSetup() {
        VirtualTransportTestConfiguration testConfiguration = new VirtualTransportTestConfiguration(true);
        NetworkTransportProvider transportProvider = testConfiguration.getTransportProvider();
        NetworkContactPointGenerator contactPointGenerator = testConfiguration.getContactPointGenerator();

        node1Service.addNetworkTransportProvider(transportProvider);
        node2Service.addNetworkTransportProvider(transportProvider);

        node1Service.activate();
        node2Service.activate();

        node2ContactPoint = contactPointGenerator.createContactPoint();
        node1ContactPoint = contactPointGenerator.createContactPoint();

        Assert.assertFalse(node1ContactPoint.equals(node2ContactPoint));
    }
}
