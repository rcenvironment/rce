/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static de.rcenvironment.core.communication.rpc.internal.MethodCallTestInterface.DEFAULT_RESULT_OR_MESSAGE_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import org.junit.Test;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.api.DisconnectReason;
import de.rcenvironment.core.communication.management.RemoteBenchmarkService;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;
import de.rcenvironment.core.communication.rpc.internal.MethodCallTestInterface;
import de.rcenvironment.core.communication.rpc.internal.MethodCallTestInterfaceImpl;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceGroup;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Base class providing common tests using virtual node instances. "Common" tests are those that do not depend on duplex vs. non-duplex
 * mode. This may also include test that require duplex mode, and are skipped/ignored when the tested transport does not support this.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractCommonVirtualInstanceTest extends AbstractVirtualInstanceTest {

    private static final int DEFAULT_TEST_TIMEOUT = 20000;

    private static final int DIRECT_MESSAGING_TEST_TIMEOUT = 5000;

    private static final int DEFAULT_STATE_CHANGE_TIMEOUT = 5000;

    private static final int WAIT_TIME_BEFORE_ASSUMING_CONNECTION_INITIATED = 500;

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
     * Verifies that a connection is refused if the version strings do not match.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testConnectionRefusedOnVersionMismatch() throws Exception {
        VirtualInstance client = new VirtualInstance("client");
        VirtualInstance server = new VirtualInstance("server");
        client.registerNetworkTransportProvider(transportProvider);
        server.registerNetworkTransportProvider(transportProvider);
        NetworkContactPoint serverNCP = contactPointGenerator.createContactPoint();
        server.addServerConfigurationEntry(serverNCP);
        client.simulateCustomProtocolVersion("wrongClientVersion");
        server.start();
        client.start();

        ConnectionSetup connection = client.connectAsync(serverNCP);
        // wait until the connection should have left the initial DISCONNECTED state
        Thread.sleep(WAIT_TIME_BEFORE_ASSUMING_CONNECTION_INITIATED);

        connection.awaitState(ConnectionSetupState.DISCONNECTED, DEFAULT_STATE_CHANGE_TIMEOUT);
        // note: if the "reason" is null, the above sleep() time may be too short
        assertEquals(DisconnectReason.FAILED_TO_CONNECT, connection.getDisconnectReason());

        client.shutDown();
        server.shutDown();
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

        testTopology.connect(0, 1);
        testTopology.connect(2, 1);
        testTopology.waitUntilReachable(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);
        testTopology.waitUntilReachable(2, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);

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
        // 2 instances using duplex connections
        setupInstances(2, true, true);

        VirtualInstance client1 = testTopology.getInstance(0);

        testTopology.connectAndWait(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);

        MessageChannel client1ToServer = client1.getMessageChannelService().getAllOutgoingChannels().iterator().next();

        // create a request from client1 to client2
        byte[] messageBody = MessageUtils.serializeSafeObject("dummy content");
        String messageType = "rpc"; // only relevant for sanity checks while routing (ie it must be a message type allowed for forwarding)
        NodeIdentifier bogusTargetNodeId = NodeIdentifierFactory.fromNodeId("bogus_node_id");

        NetworkRequest testRequest =
            NetworkRequestFactory.createNetworkRequest(messageBody, messageType, client1.getNodeId(), bogusTargetNodeId);

        // force sending the message from client1 to server, although a client would not usually do this (as it knows no route to the bogus
        // target id); this is expected to fail on the server, as the server does not know a valid route either
        NetworkResponse testResponse =
            client1.getMessageChannelService().sendDirectMessageBlocking(testRequest, client1ToServer, DIRECT_MESSAGING_TEST_TIMEOUT);

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

    /**
     * Tests various aspects of remote service call sending and handling.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testRemoteServiceCalls() throws Exception {
        // 2 instances using duplex connections
        setupInstances(2, true, true);

        VirtualInstance client1 = testTopology.getInstance(0);
        VirtualInstance server = testTopology.getInstance(1);

        // test the behavior on a service call to an unreachable node

        testRPCToUnreachableNode(client1, server);

        testTopology.connectAndWait(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);

        server.injectService(MethodCallTestInterface.class, new MethodCallTestInterfaceImpl());

        testBasicBenchmarkServiceRPC(client1, server);
        testCustomTestInterfaceRPC(client1, server);

        // TODO needed to prevent unnecessary waiting; try to integrate this into the common test setup. (this will probably be easier once
        // "graceful shutdown" behavior is implemented.)
        for (VirtualInstance vi : testTopology.getInstances()) {
            for (MessageChannel channel : vi.getMessageChannelService().getAllOutgoingChannels()) {
                channel.close();
            }
        }

        testTopology.getAsGroup().shutDown();
    }

    private void testRPCToUnreachableNode(VirtualInstance client1, VirtualInstance server) throws RemoteOperationException {
        // this should succeed; the local proxy does not check for reachability
        RemoteBenchmarkService benchmarkServiceProxy =
            client1.getCommunicationService().getRemotableService(RemoteBenchmarkService.class, server.getNodeId());
        try {
            benchmarkServiceProxy.respond("dummy", 1, 1);
            failWithExceptionExpectedMessage();
        } catch (RemoteOperationException e) {
            assertTrue(e.getMessage().contains(ResultCode.NO_ROUTE_TO_DESTINATION_AT_SENDER.toString())); // custom message
            assertTrue(e.getCause() == null); // there should be no stacktraces on the client side
        }

    }

    private void testBasicBenchmarkServiceRPC(VirtualInstance client1, VirtualInstance server) throws RemoteOperationException {
        RemoteBenchmarkService benchmarkServiceProxy =
            client1.getCommunicationService().getRemotableService(RemoteBenchmarkService.class, server.getNodeId());

        // test basic RPC operation using the standard benchmark service
        assertNotNull(benchmarkServiceProxy);
        final int respSize = 99;
        Serializable response = benchmarkServiceProxy.respond("dummy", respSize, 1);
        assertEquals(respSize, ((byte[]) response).length);
    }

    private void testCustomTestInterfaceRPC(VirtualInstance client1, VirtualInstance server) throws RemoteOperationException {
        MethodCallTestInterface remoteProxy =
            client1.getCommunicationService().getRemotableService(MethodCallTestInterface.class, server.getNodeId());

        assertNotNull(remoteProxy);
        assertEquals(DEFAULT_RESULT_OR_MESSAGE_STRING, remoteProxy.getString());
        final int testInput = 5;
        assertEquals(testInput + 1, remoteProxy.add(testInput, 1));

        try {
            remoteProxy.ioExceptionThrower();
            failWithExceptionExpectedMessage();
        } catch (IOException e) {
            assertEquals(DEFAULT_RESULT_OR_MESSAGE_STRING, e.getMessage()); // custom message
            assertTrue(e.getCause() == null); // there should be no stacktraces on the client side
        }

        try {
            remoteProxy.runtimeExceptionThrower();
            failWithExceptionExpectedMessage();
        } catch (RemoteOperationException e) {
            // TODO check message
            assertTrue(e.getCause() == null); // there should be no stacktraces arriving on the client side
        }

        // check that ambiguous methods are refused
        try {
            remoteProxy.ambiguous((Object) "", "");
            failWithExceptionExpectedMessage();
        } catch (RemoteOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("refused"));
            assertTrue(e.getCause() == null); // there should be no stacktraces on the client side
        }
    }

    private void failWithExceptionExpectedMessage() {
        fail("Exception expected");
    }

}
