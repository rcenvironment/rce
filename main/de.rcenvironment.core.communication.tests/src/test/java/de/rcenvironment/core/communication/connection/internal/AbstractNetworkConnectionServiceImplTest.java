/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.connection.internal;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.easymock.EasyMock;
import org.junit.Before;

import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;

/**
 * Base class for {@link MessageChannelServiceImpl} unit tests providing common tests that can be
 * performed with various network transports.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractNetworkConnectionServiceImplTest {

    protected InitialNodeInformation node1Information;

    protected NetworkContactPoint node1ContactPoint;

    protected MessageChannelServiceImpl node1Service;

    protected InitialNodeInformation node2Information;

    protected NetworkContactPoint node2ContactPoint;

    protected MessageChannelServiceImpl node2Service;

    /**
     * Common test setup.
     */
    @Before
    public void setUp() {
        node1Information = new InitialNodeInformationImpl("node1.testId");
        node1Service = new MessageChannelServiceImpl();
        node1Service.setNodeInformation(node1Information);
        node2Information = new InitialNodeInformationImpl("node2.testId");
        node2Service = new MessageChannelServiceImpl();
        node2Service.setNodeInformation(node2Information);
    }

    /**
     * Tests two nodes connecting to each other with duplex disabled. Each node should receive a
     * single {@link MessageChannel} to the other node.
     * 
     * @throws Exception on unexpected test errors
     */
    protected void commonTestActiveConnectionsNoDuplex() throws Exception {
        defineNetworkSetup();

        node1Service.startServer(node1ContactPoint);
        node2Service.startServer(node2ContactPoint);

        // TODO test return values more precisely
        MessageChannelLifecycleListener connectionListener = EasyMock.createMock(MessageChannelLifecycleListener.class);
        // TODO improve test of sequential behaviour
        connectionListener.onOutgoingChannelEstablished(EasyMock.anyObject(MessageChannel.class));
        EasyMock.replay(connectionListener);
        node1Service.addChannelLifecycleListener(connectionListener);
        node2Service.addChannelLifecycleListener(connectionListener);

        MessageChannel node1SelfConnection = node1Service.connect(node1ContactPoint, false).get();
        node1Service.registerNewOutgoingChannel(node1SelfConnection);
        assertEquals(node1Information.getNodeId(), node1SelfConnection.getRemoteNodeInformation().getNodeId());

        EasyMock.verify(connectionListener);
        EasyMock.reset(connectionListener);

        connectionListener.onOutgoingChannelEstablished(EasyMock.anyObject(MessageChannel.class));
        EasyMock.replay(connectionListener);

        MessageChannel node1To2Connection = node1Service.connect(node2ContactPoint, false).get();
        node1Service.registerNewOutgoingChannel(node1To2Connection);
        assertEquals(node2Information.getNodeId(), node1To2Connection.getRemoteNodeInformation().getNodeId());

        EasyMock.verify(connectionListener);
    }

    /**
     * Tests two nodes, with one connecting to the other with duplex enabled. Each node should
     * receive a single {@link MessageChannel} to the other node.
     * 
     * Note that the test behaviour expects the tested transport to support passive connections;
     * future tests may need to adapt this.
     * 
     * @throws Exception on unexpected test errors
     */
    protected void commonTestSingleDuplexConnection() throws CommunicationException, InterruptedException, ExecutionException {
        defineNetworkSetup();

        node1Service.startServer(node1ContactPoint);
        node2Service.startServer(node2ContactPoint);

        MessageChannelLifecycleListener connectionListener = EasyMock.createMock(MessageChannelLifecycleListener.class);
        connectionListener.onOutgoingChannelEstablished(EasyMock.anyObject(MessageChannel.class));
        EasyMock.replay(connectionListener);

        node2Service.addChannelLifecycleListener(connectionListener);

        MessageChannel node1To2Connection = node1Service.connect(node2ContactPoint, true).get();
        node1Service.registerNewOutgoingChannel(node1To2Connection);
        assertEquals(node2Information.getNodeId(), node1To2Connection.getRemoteNodeInformation().getNodeId());

        EasyMock.verify(connectionListener);
    }

    protected abstract void defineNetworkSetup();
}
