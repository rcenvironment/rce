/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static de.rcenvironment.core.communication.rpc.internal.MethodCallTestInterface.DEFAULT_RESULT_OR_MESSAGE_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import org.junit.Test;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.api.ServiceCallContextUtils;
import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierContextHolder;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
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
import de.rcenvironment.core.communication.transport.virtual.testutils.VirtualTopology;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Base class providing common tests using virtual node instances. "Common" tests are those that do not depend on duplex vs. non-duplex
 * mode. This may also include test that require duplex mode, and are skipped/ignored when the tested transport does not support this.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractCommonVirtualInstanceTest extends AbstractVirtualInstanceTest {

    /**
     * {@link DummyTestService} implementation that captures the {@link ServiceCallContext} of the last inbound method invocation.
     * 
     * @author Robert Mischke
     */
    public static final class ServiceCallContextCatcherImpl implements DummyTestService {

        private volatile ServiceCallContext lastServiceCallContext;

        @Override
        @AllowRemoteAccess
        public void dummyCall() {
            lastServiceCallContext = ServiceCallContextUtils.getCurrentServiceCallContext();
        }

        public ServiceCallContext getLastServiceCallContext() {
            return lastServiceCallContext;
        }

        /**
         * Clears the last captured {@link ServiceCallContext} to prevent test artifacts.
         */
        public void reset() {
            lastServiceCallContext = null;
        }
    }

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

        VirtualInstance client1 = new VirtualInstance("Client1");
        VirtualInstance client2 = new VirtualInstance("Client2");
        VirtualInstance server = new VirtualInstance("Server");

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
            InstanceNodeSessionId remoteNodeId = channel.getRemoteNodeInformation().getInstanceNodeSessionId();
            if (remoteNodeId.equals(client1.getInstanceNodeSessionId())) {
                channelSto1 = channel;
            } else if (remoteNodeId.equals(client2.getInstanceNodeSessionId())) {
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
        InstanceNodeSessionId bogusTargetNodeId = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("bogus_node_id");

        NetworkRequest testRequest =
            NetworkRequestFactory.createNetworkRequest(messageBody, messageType, client1.getInstanceNodeSessionId(), bogusTargetNodeId);

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
     * Tests various aspects of connecting to a network with the same instance node id, but changing session parts, simulating node
     * restarts.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testSessionIdHandling() throws Exception {

        VirtualInstance server = new VirtualInstance("s", true);
        NetworkContactPoint serverNCP = contactPointGenerator.createContactPoint();
        server.addServerConfigurationEntry(serverNCP);

        VirtualInstance client1 = new VirtualInstance("c1");
        VirtualInstance client2a = new VirtualInstance("c2a");
        VirtualInstance client2b = new VirtualInstance(client2a.getPersistentInstanceNodeId().getInstanceNodeIdString(), "c2b", true);
        VirtualInstance client2c = new VirtualInstance(client2a.getPersistentInstanceNodeId().getInstanceNodeIdString(), "c2c", true);

        testTopology = new VirtualTopology(client1, client2a, server, client2b, client2c);

        testTopology.getAsGroup().registerNetworkTransportProvider(transportProvider);
        testTopology.getAsGroup().start();

        assertTrue(client2a.getPersistentInstanceNodeId().equals(client2b.getPersistentInstanceNodeId()));
        assertFalse(client2a.getInstanceNodeSessionId().equals(client2b.getInstanceNodeSessionId()));
        assertTrue(client2a.getPersistentInstanceNodeId().equals(client2c.getPersistentInstanceNodeId()));
        assertFalse(client2a.getInstanceNodeSessionId().equals(client2c.getInstanceNodeSessionId()));
        assertFalse(client2b.getInstanceNodeSessionId().equals(client2c.getInstanceNodeSessionId()));
        log.debug(client2a.getInstanceNodeSessionId());
        log.debug(client2b.getInstanceNodeSessionId());
        log.debug(client2c.getInstanceNodeSessionId());

        testTopology.connect(0, 2);
        testTopology.connect(1, 2);

        client1.waitUntilContainsInReachableNodes(client2a.getInstanceNodeSessionId(), DEFAULT_NODE_REACHABILITY_TIMEOUT);
        client2a.waitUntilContainsInReachableNodes(client1.getInstanceNodeSessionId(), DEFAULT_NODE_REACHABILITY_TIMEOUT);

        assertTrue(client1.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertTrue(client1.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        assertTrue(client2a.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertTrue(client2a.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(client2b.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertFalse(client2b.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(client2b.containsInReachableNodes(client2a.getInstanceNodeSessionId()));

        // test the regular case first: disconnect the original client2a
        client2a.shutDown();
        // now connect client2b
        testTopology.connect(3, 2);
        client2b.waitUntilContainsInReachableNodes(client1.getInstanceNodeSessionId(), DEFAULT_NODE_REACHABILITY_TIMEOUT);
        client1.waitUntilContainsInReachableNodes(client2b.getInstanceNodeSessionId(), DEFAULT_NODE_REACHABILITY_TIMEOUT);

        assertTrue(client1.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertFalse(client1.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        assertTrue(client1.containsInReachableNodes(client2b.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(client2b.getInstanceNodeSessionId()));
        assertTrue(client2b.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertTrue(client2b.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(client2b.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        assertTrue(server.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(server.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        assertTrue(server.containsInReachableNodes(client2b.getInstanceNodeSessionId()));

        // now test the irregular case the connected client2b is *not* unregistered from the network
        client2b.simulateCrash();
        // connect client2c
        testTopology.connect(4, 2);
        client2c.waitUntilContainsInReachableNodes(client1.getInstanceNodeSessionId(), DEFAULT_NODE_REACHABILITY_TIMEOUT);
        client1.waitUntilContainsInReachableNodes(client2c.getInstanceNodeSessionId(), DEFAULT_NODE_REACHABILITY_TIMEOUT);

        // note: whether the commented-out lines should be true depends on the pending decision on how to handle
        // id collisions in the network - let both ids, or the most recent id, or none of them be reachable? - misc_ro, 8.0.0
        assertTrue(client1.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertFalse(client1.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        // assertTrue(client1.containsInReachableNodes(client2b.getInstanceNodeSessionId()));
        assertTrue(client1.containsInReachableNodes(client2c.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(client2b.getInstanceNodeSessionId()));
        assertFalse(client2a.containsInReachableNodes(client2c.getInstanceNodeSessionId()));
        assertTrue(client2c.containsInReachableNodes(server.getInstanceNodeSessionId()));
        assertTrue(client2c.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(client2c.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        // assertFalse(client2c.containsInReachableNodes(client2b.getInstanceNodeSessionId()));
        assertTrue(server.containsInReachableNodes(client1.getInstanceNodeSessionId()));
        assertFalse(server.containsInReachableNodes(client2a.getInstanceNodeSessionId()));
        // assertTrue(server.containsInReachableNodes(client2b.getInstanceNodeSessionId()));
        assertTrue(server.containsInReachableNodes(client2c.getInstanceNodeSessionId()));

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

    /**
     * Tests that a {@link ServiceCallContext} is available both on local and remote calls to {@link RemotableService}s.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testServiceCallContextAvailability() throws Exception {
        // 2 instances using duplex connections
        setupInstances(2, true, true);

        VirtualInstance client = testTopology.getInstance(0);
        VirtualInstance server = testTopology.getInstance(1);

        ServiceCallContextCatcherImpl clientServiceCallCatcher = new ServiceCallContextCatcherImpl();
        client.injectService(DummyTestService.class, clientServiceCallCatcher);
        ServiceCallContextCatcherImpl serverServiceCallCatcher = new ServiceCallContextCatcherImpl();
        server.injectService(DummyTestService.class, serverServiceCallCatcher);

        testTopology.connectAndWait(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);

        // C->C (local)
        client.getCommunicationService().getRemotableService(DummyTestService.class, client.getInstanceNodeSessionId()).dummyCall();
        assertNull(serverServiceCallCatcher.lastServiceCallContext);
        assertEquals(client.getInstanceNodeSessionId(), clientServiceCallCatcher.getLastServiceCallContext().getCallingNode()
            .convertToInstanceNodeSessionId());
        assertEquals(client.getInstanceNodeSessionId(), clientServiceCallCatcher.getLastServiceCallContext().getReceivingNode()
            .convertToInstanceNodeSessionId());
        clientServiceCallCatcher.reset();

        // C->S
        client.getCommunicationService().getRemotableService(DummyTestService.class, server.getInstanceNodeSessionId()).dummyCall();
        assertNull(clientServiceCallCatcher.lastServiceCallContext);
        assertEquals(client.getInstanceNodeSessionId(), serverServiceCallCatcher.getLastServiceCallContext().getCallingNode()
            .convertToInstanceNodeSessionId());
        assertEquals(server.getInstanceNodeSessionId(), serverServiceCallCatcher.getLastServiceCallContext().getReceivingNode()
            .convertToInstanceNodeSessionId());
        serverServiceCallCatcher.reset();

        // S->S (local)
        server.getCommunicationService().getRemotableService(DummyTestService.class, server.getInstanceNodeSessionId()).dummyCall();
        assertNull(clientServiceCallCatcher.lastServiceCallContext);
        assertEquals(server.getInstanceNodeSessionId(), serverServiceCallCatcher.getLastServiceCallContext().getCallingNode()
            .convertToInstanceNodeSessionId());
        assertEquals(server.getInstanceNodeSessionId(), serverServiceCallCatcher.getLastServiceCallContext().getReceivingNode()
            .convertToInstanceNodeSessionId());
        serverServiceCallCatcher.reset();

        // S->C
        server.getCommunicationService().getRemotableService(DummyTestService.class, client.getInstanceNodeSessionId()).dummyCall();
        assertNull(serverServiceCallCatcher.lastServiceCallContext);
        assertEquals(server.getInstanceNodeSessionId(), clientServiceCallCatcher.getLastServiceCallContext().getCallingNode()
            .convertToInstanceNodeSessionId());
        assertEquals(client.getInstanceNodeSessionId(), clientServiceCallCatcher.getLastServiceCallContext().getReceivingNode()
            .convertToInstanceNodeSessionId());
        clientServiceCallCatcher.reset();

        testTopology.getAsGroup().shutDown();
    }

    private void testRPCToUnreachableNode(VirtualInstance caller, VirtualInstance receiver) throws RemoteOperationException,
        IdentifierException {
        // use id handling context of client1
        NodeIdentifierService callerNodeIdService = caller.getService(NodeIdentifierService.class);
        assertNotNull(callerNodeIdService);
        NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(callerNodeIdService);

        log.debug("Performing test RPC from " + caller + " to unreachable target node " + receiver);

        // detach from the sender's id object by serializing and reconstructing it at the sender
        InstanceNodeSessionId detachedReceiverInstanceNodeSessionId =
            callerNodeIdService.parseInstanceNodeSessionIdString(receiver.getInstanceNodeSessionIdString());

        // this should succeed; the local proxy does not check for reachability
        RemoteBenchmarkService benchmarkServiceProxy =
            caller.getCommunicationService().getRemotableService(RemoteBenchmarkService.class, detachedReceiverInstanceNodeSessionId);
        try {
            benchmarkServiceProxy.respond("dummy", 1, 1);
            failWithExceptionExpectedMessage();
        } catch (RemoteOperationException e) {
            assertTrue(e.getMessage().contains(ResultCode.NO_ROUTE_TO_DESTINATION_AT_SENDER.toString())); // custom message
            assertTrue(e.getCause() == null); // there should be no stacktraces on the client side
        }
        NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(null);
    }

    private void testBasicBenchmarkServiceRPC(VirtualInstance client1, VirtualInstance server) throws RemoteOperationException {
        RemoteBenchmarkService benchmarkServiceProxy =
            client1.getCommunicationService().getRemotableService(RemoteBenchmarkService.class, server.getInstanceNodeSessionId());

        // test basic RPC operation using the standard benchmark service
        assertNotNull(benchmarkServiceProxy);
        final int respSize = 99;
        Serializable response = benchmarkServiceProxy.respond("dummy", respSize, 1);
        assertEquals(respSize, ((byte[]) response).length);
    }

    private void testCustomTestInterfaceRPC(VirtualInstance client1, VirtualInstance server) throws RemoteOperationException {
        MethodCallTestInterface remoteProxy =
            client1.getCommunicationService().getRemotableService(MethodCallTestInterface.class, server.getInstanceNodeSessionId());

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
