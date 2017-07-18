/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.transport.jms.common.NonBlockingResponseInboxConsumer.JmsResponseCallback;
import de.rcenvironment.core.communication.transport.spi.AbstractMessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelResponseHandler;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * The abstract superclass for both self-initiated and remote-initiated JMS connections.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractJmsMessageChannel extends AbstractMessageChannel implements JmsMessageChannel {

    protected final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    protected Connection connection;

    protected InstanceNodeSessionId localNodeId;

    protected final Log log = LogFactory.getLog(getClass());

    private String outgoingRequestQueueName;

    private String shutdownSecurityToken;

    private String sharedResponseQueueName;

    private RequestSender requestSender;

    private NonBlockingResponseInboxConsumer responseInboxConsumer;

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("NetworkRequests");

    /**
     * A {@link Runnable} that holds a single queue for outgoing {@link NetworkRequest}, and sends them sequentially.
     * 
     * @author Robert Mischke
     */
    private final class RequestSender implements Runnable {

        private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        private Session jmsSession;

        private Queue jmsDestinationQueue;

        private volatile boolean cancelled = false;

        RequestSender(String queueName, Connection connection) {}

        @Override
        @TaskDescription("JMS Network Transport: Message channel request sender")
        public void run() {
            try {
                try {
                    // IMPORTANT: although this is not stated in the JMS JavaDoc, this ActiveMQ call can block the thread! - misc_ro
                    jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    jmsDestinationQueue = jmsSession.createQueue(outgoingRequestQueueName);
                } catch (JMSException e) {
                    log.error("Error creating JMS session or destination for request sender loop", e);
                    return;
                }
                runDispatchLoop();
            } finally {
                try {
                    if (jmsSession != null) {
                        jmsSession.close();
                    }
                } catch (JMSException e) {
                    log.error("Error closing JMS session after running request sender loop", e);
                    return;
                }
            }
        }

        private void runDispatchLoop() {
            while (true) {
                Runnable nextTask;
                try {
                    nextTask = queue.take();
                } catch (InterruptedException e) {
                    log.warn("Request sender interrupted; shutting down");
                    return;
                }
                if (cancelled) {
                    log.debug("Clean request sender shutdown");
                    return;
                }
                nextTask.run(); // important: run in same single thread, not dispatched to thread pool
            }
        }

        void enqueue(final NetworkRequest request, final MessageChannelResponseHandler responseHandler, final int timeoutMsec) {
            final long startTime = System.currentTimeMillis();
            queue.add(new Runnable() {

                private static final int SIZE_CATEGORY_DIVISOR = 100 * 1024;

                @Override
                public void run() {
                    sendNonBlockingRequest(jmsSession, jmsDestinationQueue, request, responseHandler, timeoutMsec);

                    // store transit time statistics; intended to check if JMS producer stalling occurs
                    final int rangeValue = request.getContentBytes().length / SIZE_CATEGORY_DIVISOR;
                    final String categoryString = StringUtils.format("Size range: %1$s00..%1$s99 kiB", rangeValue);
                    StatsCounter.registerValue("Messaging: Outgoing request queue transit time", categoryString,
                        System.currentTimeMillis() - startTime);
                }
            });
        }

        public void shutdown() {
            // discard all enqueued messages when closing the channel; that the shutdown information signal is sent outside of this queue
            // (note: there is no point in trying go get an exact number here, as new messages can be enqueued at any time)
            int numDiscarded = queue.size();
            queue.clear();

            cancelled = true;
            enqueue(null, null, 0); // ensure that the dispatcher wakes up to "see" the shutdown flag
            if (numDiscarded != 0) {
                log.debug(StringUtils.format("Discarded %d pending requests for %s as channel %s is shutting down", numDiscarded,
                    getRemoteNodeInformation().getInstanceNodeSessionId(), getChannelId()));
            }
        }
    }

    /**
     * @param transportContext
     */
    public AbstractJmsMessageChannel(InstanceNodeSessionId localNodeId) {
        this.localNodeId = localNodeId;
    }

    @Override
    public void sendRequest(final NetworkRequest request, final MessageChannelResponseHandler responseHandler, final int timeoutMsec) {
        requestSender.enqueue(request, responseHandler, timeoutMsec); // new approach
        // spawnBlockingRequestResponseTask(request, responseHandler, timeoutMsec); // old approach
    }

    protected void sendShutdownMessageToRemoteRequestInbox() {
        try {
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                final Queue destinationQueue = session.createQueue(outgoingRequestQueueName);
                Message shutdownMessage = JmsProtocolUtils.createChannelShutdownMessage(session, getChannelId(), shutdownSecurityToken);
                session.createProducer(destinationQueue).send(shutdownMessage); // disposed in finally block (as part of the session)
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
            @TaskDescription("JMS Network Transport: Send shutdown signal to Client-to-Broker queue")
            public void run() {
                Session session;
                try {
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        final Queue destinationQueue = session.createQueue(outgoingRequestQueueName);
                        Message shutdownMessage = JmsProtocolUtils.createQueueShutdownMessage(session, shutdownSecurityToken);
                        session.createProducer(destinationQueue).send(shutdownMessage); // disposed in finally block (part of the session)
                    } finally {
                        session.close();
                    }
                } catch (JMSException e) {
                    String message = e.toString();
                    // ignore exceptions that just report that the temporary queue is already gone,
                    // which is normal on client connection breakdown - misc_ro
                    if (!message.contains("")) {
                        log.warn(StringUtils.format("Exception on sending shutdown signal to Client-to-Broker JMS queue %s: %s",
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
    public void setupNonBlockingRequestSending(String outgoingRequestQueue, String incomingResponseQueue) throws JMSException {

        // set up request sending
        log.debug(StringUtils.format("Setting outgoing request queue for channel %s to %s", getChannelId(), outgoingRequestQueue));
        this.outgoingRequestQueueName = outgoingRequestQueue;
        startRequestSender(StringUtils.format("Request Sender for channel %s @ %s", getChannelId(), outgoingRequestQueue));

        // set up response handling
        log.debug(StringUtils.format("Setting incoming response queue for channel %s to %s", getChannelId(), incomingResponseQueue));
        this.sharedResponseQueueName = incomingResponseQueue;
        startResponseConsumer(StringUtils.format("Response Inbox Consumer for channel %s @ %s", getChannelId(), incomingResponseQueue));
    }

    @Override
    protected void onClosedOrBroken() {
        if (requestSender != null) {
            requestSender.shutdown();
        }
        try {
            if (responseInboxConsumer != null) {
                responseInboxConsumer.triggerShutDown();
            }
        } catch (JMSException e) {
            log.warn("Error while shutting down response consumer for channel " + getChannelId(), e);
        }
    }

    private void startRequestSender(String taskName) throws JMSException {
        requestSender = new RequestSender(outgoingRequestQueueName, connection);
        threadPool.execute(requestSender, taskName);
    }

    private void startResponseConsumer(String taskName) throws JMSException {
        responseInboxConsumer = new NonBlockingResponseInboxConsumer(sharedResponseQueueName, connection);
        threadPool.execute(responseInboxConsumer, taskName);
    }

    @Override
    public void setShutdownSecurityToken(String shutdownSecurityToken) {
        this.shutdownSecurityToken = shutdownSecurityToken;
    }

    protected String getShutdownSecurityToken() {
        return shutdownSecurityToken;
    }

    private void spawnBlockingRequestResponseTask(final NetworkRequest request, final MessageChannelResponseHandler responseHandler,
        final int timeoutMsec) {
        // note: old approach
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("JMS Network Transport: blocking request/response")
            public void run() {
                // check if channel was closed or marked as broken in the meantime
                if (!isReadyToUse()) {
                    NetworkResponse response =
                        NetworkResponseFactory.generateResponseForCloseOrBrokenChannelDuringRequestDelivery(request, localNodeId, null);
                    responseHandler.onResponseAvailable(response);
                    return;
                }

                performBlockingRequestResponse(request, responseHandler, timeoutMsec);
            }

        }, request.getRequestId());
    }

    private void performBlockingRequestResponse(final NetworkRequest request, final MessageChannelResponseHandler responseHandler,
        final int timeoutMsec) {
        try {
            // IMPORTANT: although this is not stated in the JMS JavaDoc, this ActiveMQ call can block the thread! - misc_ro
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                final Queue destinationQueue = session.createQueue(outgoingRequestQueueName);
                // construct message
                Message jmsRequest = JmsProtocolUtils.createMessageFromNetworkRequest(request, session);
                Message jmsResponse = performBlockingJmsRequestResponse(session, jmsRequest, destinationQueue, timeoutMsec);
                NetworkResponse response = JmsProtocolUtils.createNetworkResponseFromMessage(jmsResponse, request);
                responseHandler.onResponseAvailable(response);
            } finally {
                session.close();
            }
        } catch (TimeoutException e) {
            // do not print the irrelevant stacktrace for this exception; only use message
            log.debug(StringUtils.format("Timeout while waiting for response to request '%s' of type '%s': %s",
                request.getRequestId(), request.getMessageType(), e.getMessage()));
            NetworkResponse response =
                NetworkResponseFactory.generateResponseForTimeoutWaitingForResponse(request, localNodeId);
            responseHandler.onResponseAvailable(response);
        } catch (JMSException e) {
            // TODO detect broken connections
            responseHandler.onChannelBroken(request, AbstractJmsMessageChannel.this);
            String errorId = LogUtils.logErrorAndAssignUniqueMarker(
                log, StringUtils.format("Error sending JMS message via channel %s; channel will be marked as broken (exception: %s) ",
                    getChannelId(), e.toString()));
            NetworkResponse response = NetworkResponseFactory.generateResponseForErrorDuringDelivery(request, localNodeId, errorId);
            responseHandler.onResponseAvailable(response);
        }
    }

    protected final Message performBlockingJmsRequestResponse(final Session session, Message message,
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
                log.debug(StringUtils.format(
                    "Exception on deleting a temporary response queue for channel %s (%s - %s): %s",
                    getChannelId(), tempResponseQueue.getQueueName(), getState(), e.toString()));
            }
        }
    }

    private void sendNonBlockingRequestInTempSession(final NetworkRequest request, final MessageChannelResponseHandler responseHandler,
        final int timeoutMsec) {

        Session session = null;
        Queue destinationQueue = null;
        try {
            // IMPORTANT: although this is not stated in the JMS JavaDoc, this ActiveMQ call can block the thread! - misc_ro
            try {
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                destinationQueue = session.createQueue(outgoingRequestQueueName);
            } catch (JMSException e) {
                log.error("Error creating JMS session or destination for message sending", e);
                return;
            }
            sendNonBlockingRequest(session, destinationQueue, request, responseHandler, timeoutMsec);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (JMSException e) {
                log.error("Error closing JMS session after message sending", e);
                return;
            }
        }
    }

    private void sendNonBlockingRequest(final Session session, final Queue destinationQueue, final NetworkRequest request,
        final MessageChannelResponseHandler responseHandler, final int timeoutMsec) {
        try {
            if (verboseRequestLoggingEnabled) {
                log.debug(StringUtils.format("Sending request   %s: type %s, payload length %d", request.getRequestId(),
                    request.getMessageType(), request.getContentBytes().length));
            }
            // construct message
            Message jmsRequest = JmsProtocolUtils.createMessageFromNetworkRequest(request, session);
            final Queue replyToQueue = session.createQueue(sharedResponseQueueName);
            jmsRequest.setJMSReplyTo(replyToQueue);
            // send
            sendRequest(session, jmsRequest, destinationQueue);
            String messageId = jmsRequest.getJMSMessageID();

            responseInboxConsumer.registerResponseListener(messageId, new JmsResponseCallback() {

                @Override
                public void onResponseReceived(Message jmsResponse) {
                    NetworkResponse response;
                    try {
                        response = JmsProtocolUtils.createNetworkResponseFromMessage(jmsResponse, request);
                        if (verboseRequestLoggingEnabled) {
                            log.debug(StringUtils.format("Received response %s: payload length %d", response.getRequestId(),
                                response.getContentBytes().length));
                        }
                        responseHandler.onResponseAvailable(response);
                    } catch (JMSException e) {
                        // check: log full stacktrace here, or compress it?
                        String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log,
                            "JMS exception while parsing response message", e);
                        response = NetworkResponseFactory.generateResponseForErrorDuringDelivery(request, localNodeId, errorId);
                        responseHandler.onResponseAvailable(response);
                    }
                }

                @Override
                public void onTimeoutReached() {
                    log.debug(StringUtils.format("Timeout reached while waiting for response to request '%s' of type '%s'",
                        request.getRequestId(), request.getMessageType()));
                    NetworkResponse response =
                        NetworkResponseFactory.generateResponseForTimeoutWaitingForResponse(request, localNodeId);
                    responseHandler.onResponseAvailable(response);
                }

                @Override
                public void onChannelClosed() {
                    log.debug(StringUtils.format("Message channel closed while waiting for response to request '%s' of type '%s'",
                        request.getRequestId(), request.getMessageType()));
                    NetworkResponse response =
                        NetworkResponseFactory.generateResponseForChannelCloseWhileWaitingForResponse(request, localNodeId, null);
                    responseHandler.onResponseAvailable(response);
                }

            }, timeoutMsec);
        } catch (JMSException e) {
            responseHandler.onChannelBroken(request, AbstractJmsMessageChannel.this);
            String errorId = LogUtils.logErrorAndAssignUniqueMarker(log, StringUtils.format(
                "Error sending JMS message via channel %s; channel will be marked as broken (exception: %s) ", getChannelId(),
                e.toString()));
            NetworkResponse response = NetworkResponseFactory.generateResponseForErrorDuringDelivery(request, localNodeId, errorId);
            responseHandler.onResponseAvailable(response);
        }
    }

    private void sendRequest(final Session session, Message message, final Queue destinationQueue) throws JMSException {
        JmsProtocolUtils.sendWithTransientProducer(session, message, destinationQueue);
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
                    throw new TimeoutException(StringUtils.format(
                        "Received JMS exception while waiting for a response from message channel %s (on queue %s), "
                            + "which is already %s", getChannelId(), tempResponseQueue.getQueueName(), currentState));
                } else {
                    throw new TimeoutException(StringUtils.format(
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
