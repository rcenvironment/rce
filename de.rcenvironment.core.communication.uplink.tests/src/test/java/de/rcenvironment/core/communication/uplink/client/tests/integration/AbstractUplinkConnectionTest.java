/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.tests.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.internal.ClientSideUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.CommonUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkConnectionLowLevelEventHandler;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * A common base class for Uplink integration tests. Subclasses can instantiate this together with their transport-specific setup code.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractUplinkConnectionTest {

    private static final int TEST_TIMEOUT = 10000;

    private static final Charset ENCODING_CHARSET = Charsets.UTF_8;

    // must be set up and torn down by subclasses before/after each test
    protected UplinkConnection uplinkConnection;

    private CommonUplinkLowLevelProtocolWrapper protocolWrapper;

    private AtomicBoolean asyncErrorOccurred = new AtomicBoolean();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Basic communication test. TODO review test scope and possible merging
     * 
     * @throws Exception on unexpected failure
     */
    @Test(timeout = TEST_TIMEOUT)
    public void basicUplinkConnection() throws Exception {

        CountDownLatch responseReceivedCDL = new CountDownLatch(1);
        CountDownLatch sessionCompleteCDL = new CountDownLatch(1);

        protocolWrapper = new ClientSideUplinkLowLevelProtocolWrapper(uplinkConnection, new UplinkConnectionLowLevelEventHandler() {

            @Override
            public void provideOrProcessHandshakeData(Map<String, String> incomingData, Map<String, String> outgoingData) {
                if (incomingData == null) {
                    assertNotNull(outgoingData);
                    // generate initial data
                    outgoingData.put("clientTestData", "dummyVal");
                    outgoingData.put(UplinkProtocolConstants.HANDSHAKE_KEY_HIGH_LEVEL_PROTOCOL_VERSION,
                        UplinkProtocolConstants.HIGH_LEVEL_PROTOCOL_VERSION);
                } else {
                    assertNull(outgoingData);
                    // TODO parse/check response
                }
            }

            @Override
            public void onHandshakeComplete() {
                ConcurrencyUtils.getAsyncTaskService().execute("Exchange test messages",
                    AbstractUplinkConnectionTest.this::exchangeTestMessages);
            }

            // TODO review these method stubs in comparison to actual client/server side code
            @Override
            public void onRegularGoodbyeMessage() {
                protocolWrapper.closeOutgoingMessageStream();
            }

            @Override
            public void onErrorGoodbyeMessage(UplinkProtocolErrorType errorType, String errorMessage) {
                log.error("Received protocol-level error message of type " + errorType + ": " + errorMessage);
                registerAsyncTestError();
                protocolWrapper.closeOutgoingMessageStream();
            }

            @Override
            public void onNonProtocolError(IOException exception) {
                log.error("Non-protocol connection error: " + exception.toString());
                registerAsyncTestError();
                protocolWrapper.closeOutgoingMessageStream();
            }

            @Override
            public void onMessageBlock(long channelId, MessageBlock message) {
                log.info("Received server message block: " + new String(message.getData(), ENCODING_CHARSET));
                responseReceivedCDL.countDown();
            }
        });

        ConcurrencyUtils.getAsyncTaskService().execute("Uplink test client: handle session", () -> {
            try {
                protocolWrapper.runSession();
            } catch (IOException e) {
                log.error("Error runnning test client session", e);
            }
            sessionCompleteCDL.countDown();
        });

        log.info("Waiting for response message");
        boolean receivedResponse = responseReceivedCDL.await(1, TimeUnit.SECONDS);
        protocolWrapper.closeOutgoingMessageStream();
        sessionCompleteCDL.await();

        // note: this would be a good use case for a multi-assertion
        assertFalse("There was at least one asynchronous error; check the log output for details", asyncErrorOccurred.get());
        assertTrue("Timed out waiting for uplink server response", receivedResponse);
    }

    protected void exchangeTestMessages() {
        try {
            log.info("Sending test message");
            final byte[] msgBytes = "clientMessage".getBytes(ENCODING_CHARSET);
            protocolWrapper.sendMessageBlock(UplinkProtocolConstants.DEFAULT_CHANNEL_ID, MessageType.PING.getCode(), msgBytes);
        } catch (IOException e) {
            log.error("Test error", e);
            registerAsyncTestError();
        }
    }

    private void registerAsyncTestError() {
        asyncErrorOccurred.set(true);
    }

}
