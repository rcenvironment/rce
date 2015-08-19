/*
 * Copyright (C) 2006-2015 DLR, Germany
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

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * A single-threaded consumer that listens for initial protocol handshake requests. These requests are the first messages that should be
 * sent by a remote node after it has established a (network level) connection to the local broker.
 * 
 * @author Robert Mischke
 */
public final class InitialInboxConsumer extends AbstractJmsQueueConsumer implements Runnable {

    private final RawMessageChannelEndpointHandler endpointHandler;

    private ServerContactPoint associatedSCP;

    private RemoteInitiatedMessageChannelFactory passiveConnectionFactory;

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    public InitialInboxConsumer(Connection localJmsConnection, RawMessageChannelEndpointHandler endpointHandler,
        ServerContactPoint associatedSCP, RemoteInitiatedMessageChannelFactory passiveConnectionFactory) throws JMSException {
        super(localJmsConnection, JmsProtocolConstants.QUEUE_NAME_INITIAL_BROKER_INBOX);
        this.endpointHandler = endpointHandler;
        this.associatedSCP = associatedSCP;
        this.passiveConnectionFactory = passiveConnectionFactory;
    }

    @Override
    @TaskDescription("Incoming JMS connection listener")
    public void run() {
        super.run();
    }

    @Override
    protected void dispatchMessage(final Message message, final Connection connection) {
        threadPool.execute(new Runnable() {

            @Override
            @TaskDescription("Dispatch initial handshake request")
            public void run() {
                try {
                    Session responseSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        dispatchMessageInternal(message, responseSession, connection);
                    } finally {
                        if (responseSession != null) {
                            responseSession.close();
                        }
                    }
                } catch (JMSException e) {
                    log.error("JMS exception in response session for request from queue " + queueName, e);
                }
            }
        });
    }

    private void dispatchMessageInternal(Message message, Session session, Connection connection) {
        String messageType;
        try {
            messageType = message.getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE);
        } catch (JMSException e) {
            log.warn("Received message with undefined message type");
            return;
        }
        try {
            if (JmsProtocolConstants.MESSAGE_TYPE_INITIAL.equals(messageType)) {
                handleHandshakeRequest(message, session, connection);
            } else {
                log.warn("Received message of unhandled type " + messageType + " from queue " + queueName);
            }
        } catch (JMSException | CommunicationException e) {
            log.warn(StringUtils.format("Error while dispatching message of type %s: %s", messageType, e.toString())); // log without
                                                                                                                       // stacktrace
        }
    }

    private void handleHandshakeRequest(Message message, Session session, Connection connection) throws JMSException,
        CommunicationException {

        JMSHandshakeInformation remoteHandshakeInformation =
            JmsProtocolUtils.parseHandshakeMessage(message, ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION);

        Message jmsResponse;
        if (remoteHandshakeInformation.matchesVersion(ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION)) {
            InitialNodeInformation remoteNodeInformation = remoteHandshakeInformation.getInitialNodeInformation();
            InitialNodeInformation ownNodeInformation = endpointHandler.exchangeNodeInformation(remoteNodeInformation);

            log.debug("Received initial handshake request from " + remoteNodeInformation);
            // create the remote-initiated connection
            // TODO (review: document as general approach?) do so before sending the response
            JmsMessageChannel remoteInitiatedChannel =
                passiveConnectionFactory.createRemoteInitiatedMessageChannel(ownNodeInformation, remoteHandshakeInformation,
                    associatedSCP, connection, session);
            remoteInitiatedChannel.markAsEstablished();
            endpointHandler.onRemoteInitiatedChannelEstablished(remoteInitiatedChannel, associatedSCP);

            // create full response
            JMSHandshakeInformation ownHandshakeInformation = new JMSHandshakeInformation();
            ownHandshakeInformation.setProtocolVersionString(ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION);
            ownHandshakeInformation.setChannelId(remoteInitiatedChannel.getChannelId());
            ownHandshakeInformation.setInitialNodeInformation(ownNodeInformation);
            ownHandshakeInformation.setRemoteInitiatedRequestInboxQueueName(JmsProtocolConstants.QUEUE_NAME_C2B_REQUEST_INBOX);
            log.debug("Remote-initiated connection established, sending handshake response to " + remoteNodeInformation);

            jmsResponse = JmsProtocolUtils.createHandshakeMessage(ownHandshakeInformation, session);
        } else {
            // respond with a reduced handshake response containing only the version information
            JMSHandshakeInformation ownHandshakeInformation = new JMSHandshakeInformation();
            ownHandshakeInformation.setProtocolVersionString(ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION);
            log.debug("Received handshake request with an incompatible version ('" + remoteHandshakeInformation.getProtocolVersionString()
                + "'); sending minimal response");
            jmsResponse = JmsProtocolUtils.createHandshakeMessage(ownHandshakeInformation, session);
        }

        // send response
        MessageProducer responseProducer = session.createProducer(message.getJMSReplyTo());
        JmsProtocolUtils.configureMessageProducer(responseProducer);
        responseProducer.send(jmsResponse);
    }
}
