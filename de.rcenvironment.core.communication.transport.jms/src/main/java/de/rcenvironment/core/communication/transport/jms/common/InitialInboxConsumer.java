/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.net.ProtocolException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A single-threaded consumer that listens for initial protocol handshake requests. These requests are the first messages that should be
 * sent by a remote node after it has established a (network level) connection to the local broker.
 * 
 * @author Robert Mischke
 */
public final class InitialInboxConsumer extends AbstractJmsQueueConsumer implements Runnable {

    private final MessageChannelEndpointHandler endpointHandler;

    private ServerContactPoint associatedSCP;

    private RemoteInitiatedMessageChannelFactory passiveConnectionFactory;

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private final String expectedProtocolVersion;

    public InitialInboxConsumer(Connection localJmsConnection, ServerContactPoint scp,
        RemoteInitiatedMessageChannelFactory passiveConnectionFactory) throws JMSException {
        super(localJmsConnection, JmsProtocolConstants.QUEUE_NAME_INITIAL_BROKER_INBOX);
        this.associatedSCP = scp;
        this.endpointHandler = scp.getEndpointHandler();
        this.expectedProtocolVersion = scp.getExpectedProtocolVersion();
        this.passiveConnectionFactory = passiveConnectionFactory;
    }

    @Override
    @TaskDescription("JMS Network Transport: Incoming connection listener")
    public void run() {
        super.run();
    }

    @Override
    protected void dispatchMessage(final Message message, final Connection connection) {
        threadPool.execute("JMS Network Transport: Dispatch initial handshake request", () -> {

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
        } catch (JMSException | ProtocolException e) {
            log.warn(StringUtils.format("Error while dispatching message of type %s: %s", messageType, e.toString())); // log without
                                                                                                                       // stacktrace
        }
    }

    private void handleHandshakeRequest(Message message, Session session, Connection connection) throws JMSException,
        ProtocolException {

        JMSHandshakeInformation remoteHandshakeInformation =
            JmsProtocolUtils.parseHandshakeMessage(message, expectedProtocolVersion);

        Message jmsResponse;
        if (remoteHandshakeInformation.matchesVersion(expectedProtocolVersion)) {
            InitialNodeInformation remoteNodeInformation = remoteHandshakeInformation.getInitialNodeInformation();
            InitialNodeInformation ownNodeInformation = endpointHandler.exchangeNodeInformation(remoteNodeInformation);

            // create handshake response so the connection factory can set its temporary queue information
            JMSHandshakeInformation ownHandshakeInformation = new JMSHandshakeInformation();

            log.debug("Received initial handshake request from " + remoteNodeInformation);
            // create the remote-initiated connection
            // TODO (review: document as general approach?) do so before sending the response
            JmsMessageChannel remoteInitiatedChannel =
                passiveConnectionFactory.createRemoteInitiatedMessageChannel(ownNodeInformation, remoteHandshakeInformation,
                    ownHandshakeInformation, associatedSCP, connection, session);
            remoteInitiatedChannel.markAsEstablished();
            endpointHandler.onRemoteInitiatedChannelEstablished(remoteInitiatedChannel, associatedSCP);

            // create full response
            ownHandshakeInformation.setProtocolVersionString(expectedProtocolVersion);
            ownHandshakeInformation.setChannelId(remoteInitiatedChannel.getChannelId());
            ownHandshakeInformation.setInitialNodeInformation(ownNodeInformation);
            // ownHandshakeInformation.setClientGeneratedTemporaryQueueNames(JmsProtocolConstants.QUEUE_NAME_C2B_REQUEST_INBOX);
            log.debug("Remote-initiated connection established, sending handshake response to " + remoteNodeInformation);

            jmsResponse = JmsProtocolUtils.createHandshakeMessage(ownHandshakeInformation, session);
        } else {
            // respond with a reduced handshake response containing only the version information
            JMSHandshakeInformation ownHandshakeInformation = new JMSHandshakeInformation();
            ownHandshakeInformation.setProtocolVersionString(expectedProtocolVersion);
            log.debug("Received handshake request with an incompatible version ('" + remoteHandshakeInformation.getProtocolVersionString()
                + "'); sending minimal response");
            jmsResponse = JmsProtocolUtils.createHandshakeMessage(ownHandshakeInformation, session);
        }

        // send response
        JmsProtocolUtils.sendWithTransientProducer(session, jmsResponse, message.getJMSReplyTo());
    }
}
