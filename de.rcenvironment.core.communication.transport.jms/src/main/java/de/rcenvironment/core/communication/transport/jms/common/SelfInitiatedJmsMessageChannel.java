/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.transport.spi.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Represents a self-initiated JMS connection, ie a connection that was established from the local node to a remote node.
 * 
 * @author Robert Mischke
 */
public class SelfInitiatedJmsMessageChannel extends AbstractJmsMessageChannel {

    private static final int INITIAL_HANDSHAKE_TIMEOUT_MSEC = 15 * 1000;

    private ConnectionFactory connectionFactory;

    private BrokenMessageChannelListener brokenConnectionListener;

    // private TemporaryQueue remoteInitiatedRequestInboxQueue;

    public SelfInitiatedJmsMessageChannel(InstanceNodeSessionId localNodeId, ConnectionFactory connectionFactory,
        BrokenMessageChannelListener brokenConnectionListener) {
        super(localNodeId);
        this.connectionFactory = connectionFactory;
        this.brokenConnectionListener = brokenConnectionListener;
    }

    void connectToJmsBroker() throws JMSException {
        connection = connectionFactory.createConnection();
        connection.setExceptionListener(new ExceptionListener() {

            @Override
            public void onException(JMSException exception) {
                log.warn(StringUtils.format("Asynchronous JMS exception in outgoing connection %s: %s ", getChannelId(),
                    exception.toString()));
                // DO NOT automatically assume a broken connection on an async exception; this is covered by "health checking" - misc_ro
                // brokenConnectionListener.onChannelBroken(SelfInitiatedJmsMessageChannel.this);
            }
        });
        connection.start();
    }

    @Override
    protected void onClosedOrBroken() {
        log.debug("Closing self-initiated channel " + getChannelId());
        super.onClosedOrBroken();

        final boolean isActiveShutdown = getState() == MessageChannelState.CLOSED;

        log.debug("Triggering asynchronous JMS disconnect of message channel " + getChannelId());
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("JMS Network Transport: Asynchronous disconnect")
            public void run() {
                tearDownJmsConnection(isActiveShutdown);
            }
        }, getChannelId());

    }

    private void tearDownJmsConnection(boolean isActiveShutdown) {
        // on a clean shutdown, send "goodbye" message and wait
        if (isActiveShutdown) {
            sendShutdownMessageToRemoteRequestInbox();
            try {
                Thread.sleep(JmsProtocolConstants.WAIT_AFTER_SENDING_SHUTDOWN_MESSAGE_MSEC);
            } catch (InterruptedException e1) {
                // log and proceed; still try to close the connection
                log.warn("Interrupted between sending the shutdown notice and closing the JMS connection");
            }
        }

        try {
            // note: this should automatically clean up (discard) the remoteInitiatedRequestInboxQueue
            if (connection != null) {
                connection.close();
            } else {
                log.debug("No JMS connection for channel " + getChannelId() + " when asked to tear it down");
            }
        } catch (JMSException e) {
            log.debug("Exception while closing JMS connection", e);
        }
    }

    HandshakeInformation performInitialHandshake(JMSHandshakeInformation ownHandshakeInformation,
        MessageChannelEndpointHandler remoteInitiatedConnectionEndpointHandler) throws JMSException,
        CommunicationException, TimeoutException, IOException {
        Session initialSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        try {
            Queue initialInbox = initialSession.createQueue(JmsProtocolConstants.QUEUE_NAME_INITIAL_BROKER_INBOX);

            // remoteInitiatedRequestInboxQueue = initialSession.createTemporaryQueue();
            // String remoteInitiatedRequestInboxQueueName = remoteInitiatedRequestInboxQueue.getQueueName();

            RequestResponseQueuesManager tempQueueManager = new RequestResponseQueuesManager();
            String clientQueueInfo = tempQueueManager.initClientSide(initialSession);

            ownHandshakeInformation.setTemporaryQueueInformation(clientQueueInfo);

            // create request message
            Message handshakeRequestMessage = JmsProtocolUtils.createHandshakeMessage(ownHandshakeInformation, initialSession);

            // perform handshake
            ObjectMessage handshakeResponseMessage =
                (ObjectMessage) performBlockingJmsRequestResponse(initialSession, handshakeRequestMessage, initialInbox,
                    INITIAL_HANDSHAKE_TIMEOUT_MSEC);

            // extract the response, expecting the protocol set in the local HandshakeInformation
            JMSHandshakeInformation remoteHandshakeInformation =
                JmsProtocolUtils.parseHandshakeMessage(handshakeResponseMessage, ownHandshakeInformation.getProtocolVersionString());

            failOnIncompatibleVersions(remoteHandshakeInformation.getProtocolVersionString(),
                ownHandshakeInformation.getProtocolVersionString());

            tempQueueManager.finishClientSide(remoteHandshakeInformation.getTemporaryQueueInformation());

            // associate outgoing channel with the id of "mirror" channel
            setAssociatedMirrorChannelId(remoteHandshakeInformation.getChannelId());

            // spawn incoming request listener
            // note: this listener is not part of the message channel, so it must be closed explicitly
            // TODO clarify ownership
            ConcurrencyUtils.getAsyncTaskService().execute(
                new RequestInboxConsumer(tempQueueManager.getB2CRequestQueue(), connection, remoteInitiatedConnectionEndpointHandler),
                StringUtils.format("B2C Request Inbox Consumer for channel %s @ %s", remoteHandshakeInformation.getChannelId(),
                    tempQueueManager.getB2CRequestQueue()));

            String outgoingRequestQueueName = tempQueueManager.getC2BRequestQueue();
            String incomingResponseQueueName = tempQueueManager.getC2BResponseQueue();
            setupNonBlockingRequestSending(outgoingRequestQueueName, incomingResponseQueueName);

            return remoteHandshakeInformation;
        } finally {
            initialSession.close();
        }
    }

}
