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
import javax.jms.Session;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.MessageChannel;

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
     * @param associatedSCP the {@link ServerContactPoint} the original connection was made to
     * @param localJmsConnection an established JMS connection to the matching JMS broker
     * @param session the session of the incoming request
     * @return the created {@link AbstractJmsMessageChannel}
     * @throws JMSException on JMS errors
     */
    JmsMessageChannel createRemoteInitiatedMessageChannel(InitialNodeInformation receivingNodeInformation,
        JMSHandshakeInformation remoteHandshakeInformation, ServerContactPoint associatedSCP,
        Connection localJmsConnection, Session session) throws JMSException;

}
