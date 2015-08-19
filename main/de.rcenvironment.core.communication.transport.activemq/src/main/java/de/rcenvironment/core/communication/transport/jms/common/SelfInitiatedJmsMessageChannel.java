/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import javax.jms.TemporaryQueue;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Represents a self-initiated JMS connection, ie a connection that was established from the local node to a remote node.
 * 
 * @author Robert Mischke
 */
public class SelfInitiatedJmsMessageChannel extends AbstractJmsMessageChannel {

    private static final int INITIAL_HANDSHAKE_TIMEOUT_MSEC = 15 * 1000;

    private ConnectionFactory connectionFactory;

    private BrokenMessageChannelListener brokenConnectionListener;

    private TemporaryQueue remoteInitiatedRequestInboxQueue;

    public SelfInitiatedJmsMessageChannel(NodeIdentifier localNodeId, ConnectionFactory connectionFactory,
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
                log.warn("Asynchronous JMS exception in outgoing connection " + getChannelId(), exception);
                // DO NOT automatically assume a broken connection on an async exception; this is covered by "health checking" - misc_ro
                // brokenConnectionListener.onChannelBroken(SelfInitiatedJmsMessageChannel.this);
            }
        });
        connection.start();
    }

    @Override
    protected void onClosedOrBroken() {
        final boolean isActiveShutdown = getState() == MessageChannelState.CLOSED;

        log.debug("Triggering asynchronous JMS disconnect of message channel " + getChannelId());
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("Asynchronous JMS disconnect")
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
            connection.close();
        } catch (JMSException e) {
            log.warn("Exception while closing JMS connection", e);
        }
    }

    HandshakeInformation performInitialHandshake(JMSHandshakeInformation ownHandshakeInformation,
        RawMessageChannelEndpointHandler remoteInitiatedConnectionEndpointHandler) throws JMSException,
        CommunicationException, TimeoutException, IOException {
        Session initialSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        try {
            Queue initialInbox = initialSession.createQueue(JmsProtocolConstants.QUEUE_NAME_INITIAL_BROKER_INBOX);

            remoteInitiatedRequestInboxQueue = initialSession.createTemporaryQueue();
            String remoteInitiatedRequestInboxQueueName = remoteInitiatedRequestInboxQueue.getQueueName();

            ownHandshakeInformation.setRemoteInitiatedRequestInboxQueueName(remoteInitiatedRequestInboxQueueName);
            log.debug("Local (S2C) queue for incoming requests (passing via outgoing connection " + getChannelId() + "): "
                + remoteInitiatedRequestInboxQueueName);

            // create request message
            Message handshakeRequestMessage = JmsProtocolUtils.createHandshakeMessage(ownHandshakeInformation, initialSession);

            // perform handshake
            ObjectMessage handshakeResponseMessage =
                (ObjectMessage) performRequestResponse(initialSession, handshakeRequestMessage, initialInbox,
                    INITIAL_HANDSHAKE_TIMEOUT_MSEC);

            // extract the response, expecting the protocol set in the local HandshakeInformation
            JMSHandshakeInformation remoteHandshakeInformation =
                JmsProtocolUtils.parseHandshakeMessage(handshakeResponseMessage, ownHandshakeInformation.getProtocolVersionString());

            // associate outgoing channel with the id of "mirror" channel
            setAssociatedMirrorChannelId(remoteHandshakeInformation.getChannelId());

            // spawn incoming request listener
            SharedThreadPool.getInstance().execute(
                new RequestInboxConsumer(remoteInitiatedRequestInboxQueueName, connection, remoteInitiatedConnectionEndpointHandler),
                remoteInitiatedRequestInboxQueueName);

            String outgoingQueueName = remoteHandshakeInformation.getRemoteInitiatedRequestInboxQueueName();
            log.debug("Remote (C2S) queue for requests using outgoing connection " + getChannelId() + ": " + outgoingQueueName);
            setOutgoingRequestQueueName(outgoingQueueName);

            return remoteHandshakeInformation;
        } finally {
            initialSession.close();
        }
    }

}
