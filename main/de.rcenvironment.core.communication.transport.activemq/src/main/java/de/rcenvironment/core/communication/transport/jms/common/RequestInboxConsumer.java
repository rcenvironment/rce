/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * A single-threaded consumer that listens for all incoming messages after the initial two-way handshake. These requests contain the actual
 * network communication. Requests are quickly dispatched to the shared thread pool to avoid congestion in this consumer thread.
 * 
 * @author Robert Mischke
 */
public final class RequestInboxConsumer extends
    AbstractJmsQueueConsumer implements Runnable {

    private static final long SLOW_DISPATCH_LOGGING_THRESHOLD_NANOS = 2 * 1000000000L;

    private static final float NANOS_PER_SECOND = 1000000000f;

    private final RawMessageChannelEndpointHandler endpointHandler;

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    public RequestInboxConsumer(String queueName, Connection connection, RawMessageChannelEndpointHandler endpointHandler)
        throws JMSException {
        super(connection, queueName);
        this.endpointHandler = endpointHandler;
    }

    @Override
    @TaskDescription("Incoming JMS request listener")
    public void run() {
        super.run();
    }

    @Override
    protected void dispatchMessage(final Message message, final Connection jmsConnection) {
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("Dispatch incoming request")
            public void run() {
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
        NetworkRequest request;
        try {
            if (JmsProtocolConstants.MESSAGE_TYPE_REQUEST.equals(messageType)) {
                request = JmsProtocolUtils.createNetworkRequestFromMessage(message);
                NodeIdentifier senderId = NodeIdentifierFactory.fromNodeId("FIXME");
                long startTime = System.nanoTime();
                NetworkResponse response = endpointHandler.onRawRequestReceived(request, senderId);
                long duration = System.nanoTime() - startTime;
                // TODO extract to constant
                if (duration > SLOW_DISPATCH_LOGGING_THRESHOLD_NANOS) {
                    log.debug("Slow dispatch (" + (duration / NANOS_PER_SECOND) + " sec) for message type " + messageType + ", "
                        + request.accessRawMetaData());
                }
                try {
                    Message jmsResponse = JmsProtocolUtils.createMessageFromNetworkResponse(response, session);
                    MessageProducer responseProducer = session.createProducer(message.getJMSReplyTo());
                    JmsProtocolUtils.configureMessageProducer(responseProducer);
                    responseProducer.send(jmsResponse);
                } catch (JMSException e) {
                    log.warn(String
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
