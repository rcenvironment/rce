/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import java.util.concurrent.Future;

import org.junit.Test;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceGroup;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Base class providing common tests using virtual node instances. "Common" tests are those that do not depend on duplex vs. non-duplex
 * mode. This may also include test that require duplex mode, and are skipped/ignored when the tested transport does not support this.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractCommonVirtualInstanceTest extends AbstractVirtualInstanceTest {

    private static final int DEFAULT_TEST_TIMEOUT = 10000;

    /**
     * Test with two clients connecting to a single server. The server instance is started before and shut down after the clients.
     * 
     * @throws Exception on unexpected test exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testBasicClientServer() throws Exception {

        // TODO old test; could be improved by using new test utilities

        VirtualInstance client1 = new VirtualInstance("Client1Id", "Client1");
        VirtualInstance client2 = new VirtualInstance("Client2Id", "Client2");
        VirtualInstance server = new VirtualInstance("ServerId", "Server");

        VirtualInstanceGroup allInstances = new VirtualInstanceGroup(server, client1, client2);
        VirtualInstanceGroup clients = new VirtualInstanceGroup(client1, client2);

        allInstances.registerNetworkTransportProvider(transportProvider);
        addGlobalTrafficListener(allInstances);

        NetworkContactPoint serverContactPoint = contactPointGenerator.createContactPoint();
        server.addServerConfigurationEntry(serverContactPoint);

        server.start();

        // TODO validate server network knowledge, internal state etc.

        prepareWaitForNextMessage();
        // configure & start clients
        clients.addInitialNetworkPeer(serverContactPoint);
        clients.start();
        // wait for network traffic to end
        // FIXME check: this succeeds on its own, but fails when run together with other tests
        waitForNextMessage();
        waitForNetworkSilence();

        // Systemx.out.println(NetworkFormatter.summary(client1.getTopologyMap()));
        // Systemx.out.println(NetworkFormatter.summary(client2.getTopologyMap()));
        // Systemx.out.println(NetworkFormatter.summary(server.getTopologyMap()));

        // TODO validate server/client network knowledge, internal state etc.

        prepareWaitForNextMessage();
        // stop clients
        clients.shutDown();
        // wait for network traffic to end
        waitForNextMessage();
        waitForNetworkSilence();

        // TODO validate server network knowledge, internal state etc.

        allInstances.shutDown();
    }

    /**
     * Tests details of channel lifecycle and operation.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testChannelHandling() throws Exception {
        // 3 instances using duplex connections
        setupInstances(3, true, true);

        VirtualInstance client1 = testTopology.getInstance(0);
        VirtualInstance server = testTopology.getInstance(1);
        VirtualInstance client2 = testTopology.getInstance(2);

        prepareWaitForNextMessage();
        testTopology.connect(0, 1, false);
        testTopology.connect(2, 1, false);
        waitForNextMessage();
        waitForNetworkSilence();

        Set<MessageChannel> client1Outgoing = client1.getMessageChannelService().getAllOutgoingChannels();
        Set<MessageChannel> client2Outgoing = client2.getMessageChannelService().getAllOutgoingChannels();
        Set<MessageChannel> serverOutgoing = server.getMessageChannelService().getAllOutgoingChannels();

        assertEquals(1, client1Outgoing.size());
        assertEquals(1, client2Outgoing.size());
        assertEquals(2, serverOutgoing.size());

        MessageChannel channel1toS = client1Outgoing.iterator().next();
        MessageChannel channel2toS = client2Outgoing.iterator().next();

        // identify/assign outgoing channels of server
        MessageChannel channelSto1 = null;
        MessageChannel channelSto2 = null;
        for (MessageChannel channel : serverOutgoing) {
            NodeIdentifier remoteNodeId = channel.getRemoteNodeInformation().getNodeId();
            if (remoteNodeId.equals(client1.getNodeId())) {
                channelSto1 = channel;
            } else if (remoteNodeId.equals(client2.getNodeId())) {
                channelSto2 = channel;
            } else {
                fail();
            }
        }
        assertNotNull(channelSto1);
        assertNotNull(channelSto2);

        // ids assigned?
        assertNotNull(channel1toS.getChannelId());
        assertNotNull(channel2toS.getChannelId());
        assertNotNull(channelSto1.getChannelId());
        assertNotNull(channelSto2.getChannelId());

        // correct "mirror" ids associated?
        assertEquals(channelSto1.getChannelId(), channel1toS.getAssociatedMirrorChannelId());
        assertEquals(channelSto2.getChannelId(), channel2toS.getAssociatedMirrorChannelId());
        assertEquals(channel1toS.getChannelId(), channelSto1.getAssociatedMirrorChannelId());
        assertEquals(channel2toS.getChannelId(), channelSto2.getAssociatedMirrorChannelId());

        // check initial channel states
        for (VirtualInstance vi : testTopology.getInstances()) {
            for (MessageChannel channel : vi.getMessageChannelService().getAllOutgoingChannels()) {
                assertEquals(MessageChannelState.ESTABLISHED, channel.getState());
                assertTrue(channel.isReadyToUse());
            }
        }

        // close a client-to-server connection
        channel1toS.close();
        // may or may not produce network traffic, so wait
        Thread.sleep(testConfiguration.getDefaultNetworkSilenceWait());
        waitForNetworkSilence();

        assertEquals(MessageChannelState.CLOSED, channel1toS.getState());
        assertEquals(MessageChannelState.CLOSED, channelSto1.getState());
        assertEquals(MessageChannelState.ESTABLISHED, channel2toS.getState());
        assertEquals(MessageChannelState.ESTABLISHED, channelSto2.getState());

        // close a server-to-client connection
        channelSto2.close();
        // may or may not produce network traffic, so wait
        Thread.sleep(testConfiguration.getDefaultNetworkSilenceWait());
        waitForNetworkSilence();

        assertEquals(MessageChannelState.CLOSED, channel2toS.getState());
        assertEquals(MessageChannelState.CLOSED, channelSto2.getState());

        testTopology.getAsGroup().shutDown();
    }

    /**
     * Tests that result codes are transported back to the client, and properly mapped back into the {@link NetworkResponse} instances.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testResultCodeTransferFromRemoteNodes() throws Exception {
        // 3 instances using duplex connections
        setupInstances(2, true, true);

        VirtualInstance client1 = testTopology.getInstance(0);

        prepareWaitForNextMessage();
        testTopology.connect(0, 1, false);
        waitForNextMessage();
        waitForNetworkSilence();

        MessageChannel client1ToServer = client1.getMessageChannelService().getAllOutgoingChannels().iterator().next();

        // create a request from client1 to client2
        byte[] messageBody = MessageUtils.serializeSafeObject("dummy content");
        String messageType = "rpc"; // only relevant for sanity checks while routing (ie it must be a message type allowed for forwarding)
        NodeIdentifier bogusTargetNodeId = NodeIdentifierFactory.fromNodeId("bogus_node_id");

        NetworkRequest testRequest =
            NetworkRequestFactory.createNetworkRequest(messageBody, messageType, client1.getNodeId(), bogusTargetNodeId);

        // force sending the message from client1 to server, although a client would not usually do this (as it knows no route to the bogus
        // target id); this is expected to fail on the server, as the server does not know a valid route either
        Future<NetworkResponse> resultFuture = client1.getMessageChannelService().sendRequest(testRequest, client1ToServer);
        NetworkResponse testResponse = resultFuture.get();

        assertEquals(ResultCode.NO_ROUTE_TO_DESTINATION_WHILE_FORWARDING, testResponse.getResultCode());

        // TODO needed to prevent unnecessary waiting; try to integrate this into the common test setup. (this will probably be easier once
        // "graceful shutdown" behavior is implemented.)
        for (VirtualInstance vi : testTopology.getInstances()) {
            for (MessageChannel channel : vi.getMessageChannelService().getAllOutgoingChannels()) {
                channel.close();
            }
        }

        testTopology.getAsGroup().shutDown();
    }
}
