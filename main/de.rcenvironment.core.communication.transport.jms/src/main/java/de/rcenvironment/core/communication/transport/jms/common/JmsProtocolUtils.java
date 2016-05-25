/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.net.ProtocolException;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Utility class providing the mapping between RCE entities and JMS messages, plus related message-related settings.
 * 
 * @author Robert Mischke
 */
public final class JmsProtocolUtils {

    private JmsProtocolUtils() {
        // prevent instantiation
    }

    /**
     * Creates a new JMS {@link Message} with the provided {@link InitialNodeInformation} as the handshake content, and a message type field
     * of {@link JmsProtocolConstants#MESSAGE_TYPE_INITIAL}.
     * 
     * @param handshakeInformation the node information to use as body
     * @param session the JMS session to use
     * @return the created JMS {@link Message}
     * @throws JMSException on JMS errors
     */
    public static Message createHandshakeMessage(JMSHandshakeInformation handshakeInformation, Session session) throws JMSException {
        ObjectMessage initialMessage = session.createObjectMessage();
        initialMessage.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE, JmsProtocolConstants.MESSAGE_TYPE_INITIAL);
        initialMessage.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_PROTOCOL_VERSION,
            handshakeInformation.getProtocolVersionString());
        initialMessage.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_CHANNEL_ID, handshakeInformation.getChannelId());
        initialMessage.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_REMOTE_INITIATED_REQUEST_INBOX,
            handshakeInformation.getTemporaryQueueInformation());

        InitialNodeInformation initialNodeInformation = handshakeInformation.getInitialNodeInformation();
        if (initialNodeInformation != null) {
            // TODO replace by JSON data?
            byte[] handshakeBytes = MessageUtils.serializeSafeObject(initialNodeInformation);
            initialMessage.setObject(handshakeBytes);
        }
        return initialMessage;
    }

    /**
     * Extracts the {@link HandshakeInformation} from a received initial JMS handshake response.
     * 
     * @param message the received message
     * @param expectedProtocolVersion the protocol version string that the response must match to continue parsing; made a parameter to
     *        allow unit testing
     * @return the extracted {@link HandshakeInformation}
     * @throws JMSException on JMS errors
     * @throws ProtocolException on a protocol version mismatch, or deserialization errors
     */
    public static JMSHandshakeInformation parseHandshakeMessage(Message message, String expectedProtocolVersion) throws JMSException,
        ProtocolException {
        JMSHandshakeInformation result = new JMSHandshakeInformation();

        result.setProtocolVersionString(message.getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_PROTOCOL_VERSION));
        result.setTemporaryQueueInformation(message
            .getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_REMOTE_INITIATED_REQUEST_INBOX));

        if (!result.matchesVersion(expectedProtocolVersion)) {
            // on an incompatible version, do not continue parsing; return result containing only
            // the version information
            return result;
        }

        result.setChannelId(message.getStringProperty(JmsProtocolConstants.MESSAGE_FIELD_CHANNEL_ID));

        byte[] handshakeRequestBytes = (byte[]) ((ObjectMessage) message).getObject();
        if (handshakeRequestBytes == null || handshakeRequestBytes.length == 0) {
            throw new ProtocolException("Received handshake request without payload");
        }
        try {
            result.setInitialNodeInformation(MessageUtils.deserializeObject(handshakeRequestBytes, InitialNodeInformationImpl.class));
        } catch (SerializationException e) {
            throw new ProtocolException("Failed to deserialize initial node information from handshake message: " + e.toString());
        }
        return result;
    }

    /**
     * Creates a JMS message from a given {@link NetworkRequest}.
     * 
     * @param request the request to transform
     * @param session the JMS session to use
     * @return the equivalent JMS message
     * @throws JMSException on JMS errors
     */
    public static Message createMessageFromNetworkRequest(final NetworkRequest request, Session session) throws JMSException {
        Map<String, String> metadata = request.accessRawMetaData();
        ObjectMessage jmsRequest = session.createObjectMessage();
        jmsRequest.setObject(request.getContentBytes());
        jmsRequest.setObjectProperty(JmsProtocolConstants.MESSAGE_FIELD_METADATA, metadata);
        jmsRequest.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE, JmsProtocolConstants.MESSAGE_TYPE_REQUEST);
        return jmsRequest;
    }

    /**
     * Restores a {@link NetworkRequest} from its JMS message form.
     * 
     * @param jmsRequest the JMS message
     * @return the reconstructed {@link NetworkRequest}
     * @throws JMSException on JMS errors
     * @throws CommunicationException on message format errors
     */
    public static NetworkRequest createNetworkRequestFromMessage(Message jmsRequest) throws JMSException, CommunicationException {
        byte[] content = (byte[]) ((ObjectMessage) jmsRequest).getObject();
        if (content.length == 0) {
            throw new CommunicationException("Received message with zero-length payload");
        }
        @SuppressWarnings("unchecked") Map<String, String> requestMetadata = (Map<String, String>) jmsRequest
            .getObjectProperty(JmsProtocolConstants.MESSAGE_FIELD_METADATA);
        NetworkRequest originalRequest = NetworkRequestFactory.reconstructNetworkRequest(content, requestMetadata);
        return originalRequest;
    }

    /**
     * Creates a JMS message from a given {@link NetworkResponse}.
     * 
     * @param response the response to transform
     * @param session the JMS session to use
     * @return the equivalent JMS message
     * @throws JMSException on JMS errors
     */
    public static Message createMessageFromNetworkResponse(NetworkResponse response, Session session) throws JMSException {
        ObjectMessage jmsResponse = session.createObjectMessage();
        jmsResponse.setObject(response.getContentBytes());
        jmsResponse.setIntProperty(JmsProtocolConstants.MESSAGE_FIELD_RESULT_CODE, response.getResultCode().getCode());
        // TODO add metadata?
        return jmsResponse;
    }

    /**
     * Restores a {@link NetworkResponse} from its JMS message form.
     * 
     * @param jmsResponse the JMS message
     * @param request the {@link NetworkRequest} this response is associated with
     * @return the reconstructed {@link NetworkResponse}
     * @throws JMSException on JMS errors
     */
    public static NetworkResponse createNetworkResponseFromMessage(Message jmsResponse, final NetworkRequest request) throws JMSException {
        byte[] content = (byte[]) ((ObjectMessage) jmsResponse).getObject();
        if (jmsResponse.propertyExists(JmsProtocolConstants.MESSAGE_FIELD_RESULT_CODE)) {
            int resultCode = jmsResponse.getIntProperty(JmsProtocolConstants.MESSAGE_FIELD_RESULT_CODE);
            return NetworkResponseFactory.generateResponseWithResultCode(request, content, resultCode);
        } else {
            // backwards compatibility for remote 6.0.x - 6.2.x RCE nodes; can be dropped in 7.0.0
            return NetworkResponseFactory.generateSuccessResponse(request, content);
        }
    }

    /**
     * Creates a JMS message to send to a JMS queue to terminate one {@link AbstractJmsQueueConsumer} listening on this queue ("poison pill"
     * pattern).
     * 
     * @param session the JMS session to use
     * @param channelId the channel id to send with the message
     * @param securityToken the shared-secret security token to prevent unauthorized shutdown
     * @return the shutdown message
     * @throws JMSException on JMS errors
     */
    public static Message createChannelShutdownMessage(Session session, String channelId, String securityToken) throws JMSException {
        TextMessage poisonPill = session.createTextMessage();
        poisonPill.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE,
            JmsProtocolConstants.MESSAGE_TYPE_CHANNEL_CLOSING);
        poisonPill.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_CHANNEL_ID,
            channelId);
        poisonPill.setText(securityToken);
        return poisonPill;
    }

    /**
     * Creates a JMS message to send to a JMS queue to terminate one {@link AbstractJmsQueueConsumer} listening on this queue ("poison pill"
     * pattern).
     * 
     * @param session the JMS session to use
     * @param securityToken the shared-secret security token to prevent unauthorized queue shutdown
     * @return the shutdown message
     * @throws JMSException on JMS errors
     */
    public static Message createQueueShutdownMessage(Session session, String securityToken) throws JMSException {
        TextMessage poisonPill = session.createTextMessage();
        poisonPill.setStringProperty(JmsProtocolConstants.MESSAGE_FIELD_MESSAGE_TYPE, JmsProtocolConstants.MESSAGE_TYPE_QUEUE_SHUTDOWN);
        poisonPill.setText(securityToken);
        return poisonPill;
    }

    /**
     * Applies common settings (like message timeouts etc.) to a JMS {@link MessageProducer}. Should be invoked on any
     * {@link MessageProducer} before it is used for sending messages.
     * 
     * @param producer the producer to configure
     * @throws JMSException on JMS errors
     */
    public static void configureMessageProducer(MessageProducer producer) throws JMSException {
        // set the maximum time that messages from this producer are preserved
        producer.setTimeToLive(ProtocolConstants.JMS_MESSAGES_TTL_MSEC);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    }

    /**
     * Wraps the common JMS pattern where a temporary {@link MessageProducer} is created from a session, used to send a single
     * {@link MessageEOFException}, and then disposed.
     * 
     * @param session the session to create the producer from
     * @param message the message to send
     * @param destination the JMS destination
     * @throws JMSException on JMS errors
     */
    public static void sendWithTransientProducer(Session session, Message message, Destination destination) throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        try {
            JmsProtocolUtils.configureMessageProducer(producer);
            producer.send(message);
        } finally {
            producer.close();
        }
    }
}
