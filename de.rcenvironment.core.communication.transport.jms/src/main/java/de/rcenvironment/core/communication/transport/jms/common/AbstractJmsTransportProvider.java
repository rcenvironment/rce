/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import java.io.IOException;
import java.net.ProtocolException;
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
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.spi.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.transport.spi.HandshakeInformation;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

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

    protected final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    protected final JmsArtifactFactory artifactFactory;

    /**
     * Common JMS implementation of the {@link RemoteInitiatedMessageChannelFactory} interface.
     * 
     * @author Robert Mischke
     */
    public class RemoteInitiatedMessageChannelFactoryImpl implements RemoteInitiatedMessageChannelFactory {

        @Override
        public JmsMessageChannel createRemoteInitiatedMessageChannel(InitialNodeInformation receivingNodeInformation,
            JMSHandshakeInformation remoteHandshakeInformation, JMSHandshakeInformation ownHandshakeInformation,
            ServerContactPoint associatedSCP, Connection localJmsConnection,
            Session session) throws JMSException, ProtocolException {

            InitialNodeInformation remoteNodeInformation = remoteHandshakeInformation.getInitialNodeInformation();
            String connectionId = connectionIdFactory.generateId(false);

            JmsMessageChannel remoteInitiatedConnection =
                new RemoteInitiatedJmsMessageChannel(receivingNodeInformation.getInstanceNodeSessionId(), localJmsConnection,
                    associatedSCP);
            remoteInitiatedConnection.setRemoteNodeInformation(remoteNodeInformation);
            remoteInitiatedConnection.setAssociatedMirrorChannelId(remoteHandshakeInformation.getChannelId());
            // FIXME add proper token
            remoteInitiatedConnection.setShutdownSecurityToken("passive." + remoteNodeInformation.getInstanceNodeSessionIdString());
            remoteInitiatedConnection.setChannelId(connectionId);
            remoteInitiatedConnection.setInitiatedByRemote(true);

            // initialize the temporary queues created by the server
            RequestResponseQueuesManager tempQueueManager = new RequestResponseQueuesManager();
            String serverQueueInfo =
                tempQueueManager.initServerSide(session, remoteHandshakeInformation.getTemporaryQueueInformation());
            ownHandshakeInformation.setTemporaryQueueInformation(serverQueueInfo);

            // note: these are not part of the remote-initiated channel, but of the server port
            // TODO move out of this method? (note: replaced with single shared inbox consumer for now)
            // String incomingRequestQueueName = tempQueueManager.getC2BRequestQueue();
            // threadPool.execute(
            // new RequestInboxConsumer(incomingRequestQueueName, localJmsConnection, associatedSCP.getEndpointHandler()),
            // StringUtils.format("C2B Request Inbox Consumer for channel %s @ %s", remoteHandshakeInformation.getChannelId(),
            // incomingRequestQueueName));

            String outgoingRequestQueueName = tempQueueManager.getB2CRequestQueue();
            String incomingResponseQueueName = tempQueueManager.getB2CResponseQueue();
            remoteInitiatedConnection.setupNonBlockingRequestSending(outgoingRequestQueueName, incomingResponseQueueName);

            return remoteInitiatedConnection;
        }
    }

    public AbstractJmsTransportProvider(MessageChannelIdFactory connectionIdFactory, JmsArtifactFactory artifactFactory) {
        this.connectionIdFactory = connectionIdFactory;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public MessageChannel connect(NetworkContactPoint ncp, InitialNodeInformation ownNodeInformation, String ownProtocolVersion,
        boolean allowInverseConnection, MessageChannelEndpointHandler inverseConnectionEndpointHandler,
        BrokenMessageChannelListener brokenConnectionListener) throws CommunicationException {
        SelfInitiatedJmsMessageChannel newChannel = null;
        try {
            try {
                ConnectionFactory connectionFactory = artifactFactory.createConnectionFactory(ncp);
                InstanceNodeSessionId localNodeId = ownNodeInformation.getInstanceNodeSessionId();
                newChannel = new SelfInitiatedJmsMessageChannel(localNodeId, connectionFactory, brokenConnectionListener);
                newChannel.setChannelId(connectionIdFactory.generateId(true));
                newChannel.connectToJmsBroker();

                log.debug("Connected to JMS broker; sending initial handshake with identity '" + localNodeId + "'");

                JMSHandshakeInformation ownHandshakeInformation = new JMSHandshakeInformation();
                ownHandshakeInformation.setProtocolVersionString(ownProtocolVersion);
                ownHandshakeInformation.setInitialNodeInformation(ownNodeInformation);
                ownHandshakeInformation.setChannelId(newChannel.getChannelId());

                // this throws a CommunicationException in case of protocol version mismatch
                HandshakeInformation remoteHandshakeInformation =
                    newChannel.performInitialHandshake(ownHandshakeInformation, inverseConnectionEndpointHandler);

                InitialNodeInformation remoteNodeInformation = remoteHandshakeInformation.getInitialNodeInformation();
                newChannel.setRemoteNodeInformation(remoteNodeInformation);

                log.debug("Successfully performed JMS handshake with remote node " + remoteNodeInformation.getLogDescription());
                // basic check against duplicate node ids; does not guard against non-neighbor nodes
                // with same id
                if (remoteNodeInformation.getInstanceNodeSessionId().isSameInstanceNodeAs(localNodeId)) {
                    throw new CommunicationException("Invalid setup: Remote and local node share the same instance node id: "
                        + localNodeId.getInstanceNodeIdString());
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
        } catch (CommunicationException e) {
            if (newChannel != null) {
                newChannel.onClosedOrBroken();
            }
            throw e;
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
        synchronized (serverEndpoints) {
            serverEndpoints.put(scp, broker);
        }
    }

    @Override
    public void stopServer(ServerContactPoint scp) {
        final JmsBroker broker;
        synchronized (serverEndpoints) {
            broker = serverEndpoints.get(scp);
        }
        broker.stop();
    }
}
