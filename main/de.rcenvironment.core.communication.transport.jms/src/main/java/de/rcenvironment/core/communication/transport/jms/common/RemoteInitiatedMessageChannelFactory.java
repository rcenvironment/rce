/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.net.ProtocolException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Factory interface for remote-initiated {@link MessageChannel}s.
 * 
 * @author Robert Mischke
 */
public interface RemoteInitiatedMessageChannelFactory {

    /**
     * Creates a remote-initiated outgoing connection.
     * 
     * @param receivingNodeInformation the node information for the receiver of the original connection (ie, the local node)
     * @param remoteHandshakeInformation the handshake information for the initiator of the original connection (ie, the remote node)
     * @param ownHandshakeInformation the container for the local node's handshake information; used to transport temp queue information
     * @param associatedSCP the {@link ServerContactPoint} the original connection was made to
     * @param localJmsConnection an established JMS connection to the matching JMS broker
     * @param session the session of the incoming request
     * @return the created {@link AbstractJmsMessageChannel}
     * @throws JMSException on JMS errors
     * @throws ProtocolException on errors in the expected handshake information
     */
    JmsMessageChannel createRemoteInitiatedMessageChannel(InitialNodeInformation receivingNodeInformation,
        JMSHandshakeInformation remoteHandshakeInformation, JMSHandshakeInformation ownHandshakeInformation,
        ServerContactPoint associatedSCP, Connection localJmsConnection, Session session) throws JMSException, ProtocolException;

}
