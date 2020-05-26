/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A single-threaded consumer that listens for all incoming messages after the initial two-way handshake. These requests contain the actual
 * network communication. Requests are quickly dispatched to the shared thread pool to avoid congestion in this consumer thread.
 * 
 * @author Robert Mischke
 */
public final class RequestInboxConsumer extends AbstractJmsQueueConsumer implements Runnable {

    // TODO >7.0.0: set to a much lower value once relay forwarding is asynchronous as well
    private static final long SLOW_DISPATCH_LOGGING_THRESHOLD_MSEC = 25 * 1000; // 25 sec

    private final MessageChannelEndpointHandler endpointHandler;

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("NetworkRequests");

    public RequestInboxConsumer(String queueName, Connection connection, MessageChannelEndpointHandler endpointHandler)
        throws JMSException {
        super(connection, queueName);
        this.endpointHandler = endpointHandler;
    }

    @Override
    @TaskDescription("JMS Network Transport: Incoming request listener")
    public void run() {
        super.run();
    }

    @Override
    protected void dispatchMessage(final Message message, final Connection jmsConnection) {
        threadPool.execute("JMS Network Transport: Dispatch incoming request", () -> {
            try {
                Session responseSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                try {
                    dispatchMessageInternal(message, responseSession);
                } finally {
                    if (responseSession != null) {
                        responseSession.close();
                    }
                }
            } catch (JMSException e) {
                // do not log stacktrace, as it contains no additional information
                log.error("JMS exception in response session for request from queue " + queueName + ": " + e.toString());
            }
        });
    }

    private void dispatchMessageInternal(Message message, Session session) {
        String messageType;
        try {
            messageType = message.getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE);
        } catch (JMSException e) {
            log.warn("Received message with undefined message type");
            return;
        }
        final NetworkRequest request;
        try {
            if (JmsProtocolConstants.MESSAGE_TYPE_REQUEST.equals(messageType)) {
                request = JmsProtocolUtils.createNetworkRequestFromMessage(message);

                final int requestPayloadSize = request.getContentBytes().length;
                if (verboseRequestLoggingEnabled) {
                    log.debug(StringUtils.format("Received request  %s: type %s, payload length %d", request.getRequestId(),
                        request.getMessageType(), requestPayloadSize));
                }
                if (requestPayloadSize >= NETWORK_PAYLOAD_SIZE_WARNING_THRESHOLD) {
                    log.debug(StringUtils.format("Received large network request %s from %s: type %s, payload length %d",
                        request.getRequestId(), request.accessMetaData().getSender(), request.getMessageType(), requestPayloadSize));
                }

                // on the messaging level, senders and receivers are identified by instance session ids
                String senderIdString = request.accessMetaData().getSenderIdString();
                long startTime = System.currentTimeMillis();
                NetworkResponse response = endpointHandler.onRawRequestReceived(request, senderIdString);
                long durationMsec = System.currentTimeMillis() - startTime;
                // TODO move slow dispatch logging to transport-neutral code - misc_ro, May 2015
                if (durationMsec > SLOW_DISPATCH_LOGGING_THRESHOLD_MSEC) {
                    log.debug(StringUtils.format("Slow dispatch (%,d msec) for incoming request of type %s", durationMsec,
                        request.getMessageType()));
                }
                try {
                    Message jmsResponse = JmsProtocolUtils.createMessageFromNetworkResponse(response, session);
                    final String messageId = message.getJMSMessageID();
                    // sanity check
                    if (messageId == null) {
                        log.error("Unexpected state: null JMS message id");
                        return; // no graceful handling possible
                    }
                    jmsResponse.setJMSCorrelationID(messageId); // set correlation id for non-blocking response handling
                    if (verboseRequestLoggingEnabled) {
                        log.debug(StringUtils.format("Sending response  %s: payload length %d", request.getRequestId(),
                            response.getContentBytes().length));
                    }
                    JmsProtocolUtils.sendWithTransientProducer(session, jmsResponse, message.getJMSReplyTo());
                } catch (JMSException e) {
                    log.debug(StringUtils
                        .format("Error sending JMS response after successful request dispatch; most likely, the remote side "
                            + "has closed the connection after sending the request (request type: %s, exception: %s)",
                            request.getMessageType(), e.toString()));
                }
            } else if (JmsProtocolConstants.MESSAGE_TYPE_CHANNEL_CLOSING.equals(messageType)) {
                String closingChannelId = message.getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_CHANNEL_ID);
                endpointHandler.onInboundChannelClosing(closingChannelId);
            } else {
                log.warn("Received message of unhandled type " + messageType + " from queue " + queueName);
            }
        } catch (JMSException e) {
            log.warn("Error while dispatching message of type " + messageType, e);
        } catch (CommunicationException e) {
            log.warn("Error while dispatching message of type " + messageType, e);
        }
    }
}
