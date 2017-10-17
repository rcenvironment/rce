/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.configuration.IPWhitelistConnectionFilter;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;
import de.rcenvironment.core.communication.model.impl.NetworkResponseImpl;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.testutils.AbstractTransportBasedTest;
import de.rcenvironment.core.communication.transport.spi.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.communication.transport.spi.MessageChannelResponseHandler;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * A common base class that defines common tests to verify proper transport operation. Subclasses implement
 * {@link #defineTestConfiguration()} to create a transport-specific test.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractTransportLowLevelTest extends AbstractTransportBasedTest {

    private static final int DEFAULT_REQUEST_TIMEOUT = 1000;

    private static final long WAIT_FOR_INBOUND_CHANNEL_CLOSING_EVENT_MSEC = 1000;

    /**
     * This sets a conservative timeout for all derived tests; individual test may set stricter timeouts. - misc_ro
     */
    @Rule
    public Timeout globalTimeout = new Timeout(DEFAULT_SAFEGUARD_TEST_TIMEOUT);

    /**
     * This test verifies the basic functions of a network transport. It covers {@link ServerContactPoint} startup, connection initiation,
     * initial node information handshake, and the basic request/response loop.
     * 
     * @author Robert Mischke
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void basicTransportOperation() throws Exception {

        int messageRepetitions = 10;

        // create mock server config
        InitialNodeInformationImpl mockServerNodeInformation =
            new InitialNodeInformationImpl(NodeIdentifierTestUtils.createTestInstanceNodeSessionId());
        mockServerNodeInformation.setDisplayName("Mock Server");
        // configure mock endpoint handler
        MessageChannelEndpointHandler serverEndpointHandler = EasyMock.createMock(MessageChannelEndpointHandler.class);
        // configure handshake response
        EasyMock.expect(serverEndpointHandler.exchangeNodeInformation(EasyMock.anyObject(InitialNodeInformation.class))).andReturn(
            mockServerNodeInformation);
        // expect passive connection event (if applicable)
        if (transportProvider.supportsRemoteInitiatedConnections()) {
            serverEndpointHandler.onRemoteInitiatedChannelEstablished(EasyMock.anyObject(MessageChannel.class),
                EasyMock.anyObject(ServerContactPoint.class));
        }
        EasyMock.replay(serverEndpointHandler);
        BrokenMessageChannelListener brokenConnectionListener = EasyMock.createMock(BrokenMessageChannelListener.class);
        EasyMock.replay(brokenConnectionListener);
        // create server contact point
        NetworkContactPoint ncp = contactPointGenerator.createContactPoint();
        IPWhitelistConnectionFilter ipFilter = new IPWhitelistConnectionFilter();
        // allow test connections from IPv4 localhost; adapt if necessary
        ipFilter.configure(Arrays.asList(new String[] { "127.0.0.1" }));
        ServerContactPoint scp =
            new ServerContactPoint(transportProvider, ncp, ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION, serverEndpointHandler,
                ipFilter);
        // start it
        assertFalse(scp.isAcceptingMessages());
        scp.start();
        assertTrue(scp.isAcceptingMessages());

        // create mock client config
        InitialNodeInformationImpl clientNodeInformation =
            new InitialNodeInformationImpl(NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("clientNode"));
        // connect
        // (allows duplex connections, but omits client endpoint handler as it should not be used)
        MessageChannel channel =
            transportProvider.connect(ncp, clientNodeInformation, ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION, true, null,
                brokenConnectionListener);

        assertEquals(MessageChannelState.ESTABLISHED, channel.getState());
        assertTrue(channel.isReadyToUse());

        // verify server side of handshake
        EasyMock.verify(serverEndpointHandler);
        // verify client side of handshake
        assertNotNull(channel.getChannelId());
        assertNotNull(channel.getRemoteNodeInformation());
        assertEquals(mockServerNodeInformation, channel.getRemoteNodeInformation());

        logThreadPoolState("after connecting");

        // define server response behavior
        final String requestString = "Hi world";
        final String responseSuffix = "#response"; // arbitrary
        EasyMock.reset(serverEndpointHandler);
        serverEndpointHandler.onRawRequestReceived(EasyMock.isA(NetworkRequest.class), EasyMock.isA(String.class));

        EasyMock.expectLastCall().andAnswer(new IAnswer<NetworkResponse>() {

            @Override
            public NetworkResponse answer() throws Throwable {
                try {
                    NetworkRequest request = (NetworkRequest) EasyMock.getCurrentArguments()[0];
                    String responseString = request.getDeserializedContent().toString() + responseSuffix;
                    byte[] responseBytes = MessageUtils.serializeSafeObject(responseString);
                    return new NetworkResponseImpl(responseBytes, MessageMetaData.create().getInnerMap());
                } catch (RuntimeException e) {
                    log.warn("RTE in mock", e);
                    return null;
                }
            }
        }).times(messageRepetitions);

        // set up mock client response handler
        MessageChannelResponseHandler responseHandler = EasyMock.createMock(MessageChannelResponseHandler.class);
        // define expected callback
        Capture<NetworkResponse> responseCapture = new Capture<NetworkResponse>(CaptureType.ALL);
        responseHandler.onResponseAvailable(EasyMock.capture(responseCapture));
        EasyMock.expectLastCall().times(messageRepetitions);

        // enter test mode
        EasyMock.replay(serverEndpointHandler, responseHandler);
        // send request(s)
        for (int i = 0; i < messageRepetitions; i++) {
            byte[] contentBytes = MessageUtils.serializeSafeObject(requestString);
            // note: message type is arbitrary, but must be valid
            NetworkRequest request =
                NetworkRequestFactory.createNetworkRequest(contentBytes, ProtocolConstants.VALUE_MESSAGE_TYPE_HEALTH_CHECK,
                    clientNodeInformation.getInstanceNodeSessionId(),
                    mockServerNodeInformation.getInstanceNodeSessionId());
            channel.sendRequest(request, responseHandler, DEFAULT_REQUEST_TIMEOUT);
        }
        // TODO improve; quick&dirty hack to test larger message repetition counts
        Thread.sleep(testConfiguration.getDefaultTrafficWaitTimeout() + messageRepetitions * 10);

        // first, verify that the endpoint handler was called
        EasyMock.verify(serverEndpointHandler);
        // then, verify that the response handler was called
        EasyMock.verify(responseHandler);
        // verify response content
        List<NetworkResponse> responses = responseCapture.getValues();
        assertEquals(messageRepetitions, responses.size());
        for (NetworkResponse response : responses) {
            assertEquals(requestString + responseSuffix, response.getDeserializedContent());
        }

        // close channel and verify that the remote endpoint handler is notified
        EasyMock.reset(serverEndpointHandler);
        serverEndpointHandler.onInboundChannelClosing(channel.getChannelId());
        EasyMock.replay(serverEndpointHandler);
        channel.close();
        Thread.sleep(WAIT_FOR_INBOUND_CHANNEL_CLOSING_EVENT_MSEC);
        EasyMock.verify(serverEndpointHandler);

        scp.shutDown();
        assertFalse(scp.isAcceptingMessages());

        logThreadPoolState("after server shutdown");
    }

    private void logThreadPoolState(String timeDescription) {
        log.debug("Thread pool state " + timeDescription + ":\n"
            + ConcurrencyUtils.getThreadPoolManagement().getFormattedStatistics(true));
    }

}
