/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.RawNetworkResponseHandler;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.transport.spi.AbstractMessageChannel;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * The abstract superclass for both self-initiated and remote-initiated JMS connections.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractJmsMessageChannel extends AbstractMessageChannel implements JmsMessageChannel {

    protected final ThreadPool threadPool = SharedThreadPool.getInstance();

    protected Connection connection;

    protected final Log log = LogFactory.getLog(getClass());

    protected NodeIdentifier localNodeId;

    private String outgoingRequestQueueName;

    private String shutdownSecurityToken;

    /**
     * @param transportContext
     */
    public AbstractJmsMessageChannel(NodeIdentifier localNodeId) {
        this.localNodeId = localNodeId;
    }

    @Override
    public void sendRequest(final NetworkRequest request, final RawNetworkResponseHandler responseHandler, final int timeoutMsec) {

        // Note: this is a very basic approach; optimize? -- misc_ro
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("JMS request/response")
            public void run() {
                // check if channel was closed or marked as broken in the meantime
                if (!isReadyToUse()) {
                    NetworkResponse response = NetworkResponseFactory.generateResponseForChannelClosedOrBroken(request);
                    responseHandler.onResponseAvailable(response);
                    return;
                }

                try {
                    // IMPORTANT: although this is not stated in the JMS JavaDoc, this ActiveMQ call can block the thread! - misc_ro
                    final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        final Queue destinationQueue = session.createQueue(outgoingRequestQueueName);
                        // construct message
                        Message jmsRequest = JmsProtocolUtils.createMessageFromNetworkRequest(request, session);
                        Message jmsResponse = performRequestResponse(session, jmsRequest, destinationQueue, timeoutMsec);
                        NetworkResponse response = JmsProtocolUtils.createNetworkResponseFromMessage(jmsResponse, request);
                        responseHandler.onResponseAvailable(response);
                    } finally {
                        session.close();
                    }
                } catch (TimeoutException e) {
                    // do not print the irrelevant stacktrace for this exception; only use message
                    log.warn(String.format("Timeout while waiting for response to request '%s' of type '%s': %s", request.getRequestId(),
                        request.getMessageType(), e.getMessage()));
                    NetworkResponse response =
                        NetworkResponseFactory.generateResponseForTimeoutWaitingForResponse(request, localNodeId.getIdString(), e);
                    responseHandler.onResponseAvailable(response);
                } catch (JMSException e) {
                    // TODO detect broken connections
                    responseHandler.onChannelBroken(request, AbstractJmsMessageChannel.this);
                    log.warn(String.format("Error sending JMS message via channel %s; channel will be marked as broken (exception: %s) ",
                        getChannelId(), e.toString()));
                    // convert to IOException, as transport-specific exceptions fail to deserialize
                    IOException safeException = new IOException(e.getMessage() + " (converted from " + e.getClass().getName() + ")");
                    NetworkResponse response =
                        NetworkResponseFactory.generateResponseForExceptionWhileRouting(request, localNodeId.getIdString(), safeException);
                    responseHandler.onResponseAvailable(response);
                }
            }

        }, request.getRequestId());
    }

    protected void sendShutdownMessageToRemoteRequestInbox() {
        try {
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                final Queue destinationQueue = session.createQueue(outgoingRequestQueueName);
                Message shutdownMessage = JmsProtocolUtils.createChannelShutdownMessage(session, getChannelId(), shutdownSecurityToken);
                session.createProducer(destinationQueue).send(shutdownMessage);
            } finally {
                session.close();
            }
        } catch (JMSException e) {
            log.debug("Failed to send shutdown message while closing channel " + getChannelId(), e);
        }
    }

    /**
     * Sends a shutdown message to the broker-to-client (B2C) queue at the JMS broker. This allows the client-side queue listener to
     * terminate cleanly.
     * 
     * @throws JMSException on JMS errors
     */
    protected void asyncSendShutdownMessageToB2CJmsQueue() throws JMSException {
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("Send shutdown signal to Client-to-Broker JMS queue")
            public void run() {
                Session session;
                try {
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        final Queue destinationQueue = session.createQueue(outgoingRequestQueueName);
                        Message shutdownMessage = JmsProtocolUtils.createQueueShutdownMessage(session, shutdownSecurityToken);
                        session.createProducer(destinationQueue).send(shutdownMessage);
                    } finally {
                        session.close();
                    }
                } catch (JMSException e) {
                    String message = e.toString();
                    // ignore exceptions that just report that the temporary queue is already gone,
                    // which is normal on client connection breakdown - misc_ro
                    if (!message.contains("")) {
                        log.warn(String.format("Exception on sending shutdown signal to Client-to-Broker JMS queue %s: %s",
                            outgoingRequestQueueName, message));
                    }
                }
            }
        });
    }

    @Override
    public String getOutgoingRequestQueueName() {
        return outgoingRequestQueueName;
    }

    @Override
    public void setOutgoingRequestQueueName(String destinationQueueName) {
        this.outgoingRequestQueueName = destinationQueueName;
    }

    @Override
    public void setShutdownSecurityToken(String shutdownSecurityToken) {
        this.shutdownSecurityToken = shutdownSecurityToken;
    }

    protected String getShutdownSecurityToken() {
        return shutdownSecurityToken;
    }

    protected final Message performRequestResponse(final Session session, Message message,
        final Queue destinationQueue, int timeoutMsec) throws JMSException, TimeoutException {
        final TemporaryQueue tempResponseQueue = session.createTemporaryQueue();
        try {
            message.setJMSReplyTo(tempResponseQueue);
            // send
            sendRequest(session, message, destinationQueue);
            // receive
            return receiveResponse(session, timeoutMsec, tempResponseQueue);
        } finally { // close temporary queue
            try {
                tempResponseQueue.delete();
            } catch (JMSException e) {
                // only log compact exception
                log.debug(String.format(
                    "Exception on deleting a temporary response queue for channel %s (%s - %s): %s",
                    getChannelId(), tempResponseQueue.getQueueName(), getState(), e.toString()));
            }
        }
    }

    private void sendRequest(final Session session, Message message, final Queue destinationQueue) throws JMSException {
        MessageProducer producer = session.createProducer(destinationQueue);
        JmsProtocolUtils.configureMessageProducer(producer);
        producer.send(message);
    }

    private Message receiveResponse(final Session session, int timeoutMsec, final TemporaryQueue tempResponseQueue) throws JMSException,
        TimeoutException {
        MessageConsumer consumer = session.createConsumer(tempResponseQueue);
        try {
            Message response;
            response = consumer.receive(timeoutMsec);
            if (response != null) {
                return response;
            } else {
                // null return value indicates timeout
                MessageChannelState currentState = getState();
                if (currentState == MessageChannelState.CLOSED || currentState == MessageChannelState.MARKED_AS_BROKEN) {
                    throw new TimeoutException(String.format(
                        "Received JMS exception while waiting for a response from message channel %s (on queue %s), "
                            + "which is already %s", getChannelId(), tempResponseQueue.getQueueName(), currentState));
                } else {
                    throw new TimeoutException(String.format(
                        "Timeout (%d ms) exceeded while waiting for a response from message channel %s (on queue %s), "
                            + "which is in state %s", timeoutMsec, getChannelId(), tempResponseQueue.getQueueName(), currentState));
                }
            }
        } finally {
            // close the consumer as it blocks the temporary queue from deletion otherwise
            consumer.close();
        }
    }

}
