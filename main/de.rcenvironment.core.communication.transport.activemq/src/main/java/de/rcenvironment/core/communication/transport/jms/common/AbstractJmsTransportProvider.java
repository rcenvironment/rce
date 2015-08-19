/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelIdFactory;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * Abstract base class for JMS transport providers. This base class provides the aspects of a JMS transport provider that are independent of
 * the JMS implementation used.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractJmsTransportProvider implements NetworkTransportProvider {

    protected final Map<ServerContactPoint, JmsBroker> serverEndpoints =
        new HashMap<ServerContactPoint, JmsBroker>();

    protected final MessageChannelIdFactory connectionIdFactory;

    protected final Log log = LogFactory.getLog(getClass());

    protected final ThreadPool threadPool = SharedThreadPool.getInstance();

    protected final JmsArtifactFactory artifactFactory;

    /**
     * Common JMS implementation of the {@link RemoteInitiatedMessageChannelFactory} interface.
     * 
     * @author Robert Mischke
     */
    public class RemoteInitiatedMessageChannelFactoryImpl implements RemoteInitiatedMessageChannelFactory {

        @Override
        public JmsMessageChannel createRemoteInitiatedMessageChannel(InitialNodeInformation receivingNodeInformation,
            JMSHandshakeInformation remoteHandshakeInformation, ServerContactPoint associatedSCP, Connection localJmsConnection,
            Session session) throws JMSException {

            InitialNodeInformation remoteNodeInformation = remoteHandshakeInformation.getInitialNodeInformation();
            String connectionId = connectionIdFactory.generateId(false);
            JmsMessageChannel remoteInitiatedConnection =
                new RemoteInitiatedJmsMessageChannel(receivingNodeInformation.getNodeId(), localJmsConnection, associatedSCP);
            remoteInitiatedConnection.setRemoteNodeInformation(remoteNodeInformation);
            remoteInitiatedConnection.setAssociatedMirrorChannelId(remoteHandshakeInformation.getChannelId());
            // FIXME add proper token
            remoteInitiatedConnection.setShutdownSecurityToken("passive." + remoteNodeInformation.getNodeIdString());
            remoteInitiatedConnection.setChannelId(connectionId);
            remoteInitiatedConnection.setInitiatedByRemote(true);
            String destinationQueueName = remoteHandshakeInformation.getRemoteInitiatedRequestInboxQueueName();
            log.debug("Setting destination queue for remote-initiated message channel to " + destinationQueueName);
            remoteInitiatedConnection.setOutgoingRequestQueueName(destinationQueueName);
            return remoteInitiatedConnection;
        }

    }

    public AbstractJmsTransportProvider(MessageChannelIdFactory connectionIdFactory, JmsArtifactFactory artifactFactory) {
        this.connectionIdFactory = connectionIdFactory;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public MessageChannel connect(NetworkContactPoint ncp, InitialNodeInformation ownNodeInformation, boolean allowInverseConnection,
        RawMessageChannelEndpointHandler inverseConnectionEndpointHandler, BrokenMessageChannelListener brokenConnectionListener)
        throws CommunicationException {
        try {
            ConnectionFactory connectionFactory = artifactFactory.createConnectionFactory(ncp);
            NodeIdentifier localNodeId = ownNodeInformation.getNodeId();
            SelfInitiatedJmsMessageChannel newChannel =
                new SelfInitiatedJmsMessageChannel(localNodeId, connectionFactory, brokenConnectionListener);
            newChannel.setChannelId(connectionIdFactory.generateId(true));
            newChannel.connectToJmsBroker();

            log.debug("Connected to JMS broker; sending initial handshake with identity '" + localNodeId + "'");

            JMSHandshakeInformation ownHandshakeInformation = new JMSHandshakeInformation();
            ownHandshakeInformation.setProtocolVersionString(ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION);
            ownHandshakeInformation.setInitialNodeInformation(ownNodeInformation);
            ownHandshakeInformation.setChannelId(newChannel.getChannelId());

            HandshakeInformation remoteHandshakeInformation =
                newChannel.performInitialHandshake(ownHandshakeInformation, inverseConnectionEndpointHandler);

            JmsProtocolUtils.failOnIncompatibleVersions(remoteHandshakeInformation.getProtocolVersionString(),
                ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION);

            InitialNodeInformation remoteNodeInformation = remoteHandshakeInformation.getInitialNodeInformation();
            newChannel.setRemoteNodeInformation(remoteNodeInformation);

            log.debug("Successfully performed JMS handshake with remote node " + remoteNodeInformation.getLogDescription());
            // basic check against duplicate node ids; does not guard against non-neighbor nodes
            // with same id
            if (remoteNodeInformation.getNodeIdString().equals(localNodeId.getIdString())) {
                throw new CommunicationException("Invalid setup: Remote and local node share the same node id: "
                    + localNodeId.getIdString());
            }
            newChannel.markAsEstablished();
            return newChannel;
        } catch (IOException e) {
            throw new CommunicationException("Failed to initiate JMS connection", e);
        } catch (RuntimeException e) {
            throw new CommunicationException("Failed to establish JMS connection", e);
        } catch (JMSException e) {
            throw new CommunicationException("Failed to establish JMS connection. Reason: " + e.toString()); // compress stacktrace
        } catch (TimeoutException e) {
            throw new CommunicationException("Timeout while establishing JMS connection", e);
        }
    }

    @Override
    public boolean supportsRemoteInitiatedConnections() {
        return true;
    }

    @Override
    public void startServer(ServerContactPoint scp) throws CommunicationException {
        JmsBroker broker = artifactFactory.createBroker(scp, new RemoteInitiatedMessageChannelFactoryImpl());
        // CHECKSTYLE:DISABLE (IllegalCatch) - ActiveMQ method declares "throws Exception"
        try {
            broker.start();
        } catch (Exception e) {
            throw new CommunicationException("Failed to start JMS broker for SCP " + scp, e);
        }
        // CHECKSTYLE:ENABLE (IllegalCatch)
        serverEndpoints.put(scp, broker);
    }

    @Override
    public void stopServer(ServerContactPoint scp) {
        JmsBroker broker = serverEndpoints.get(scp);
        // FIXME this can be null on test shutdown; why? - misc_ro
        broker.stop();
    }
}
