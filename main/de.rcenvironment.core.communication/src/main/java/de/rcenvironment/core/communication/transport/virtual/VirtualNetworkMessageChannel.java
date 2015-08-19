/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.virtual;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.connection.internal.ConnectionClosedException;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.RawNetworkResponseHandler;
import de.rcenvironment.core.communication.model.impl.NetworkResponseImpl;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.transport.spi.AbstractMessageChannel;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * The {@link MessageChannel} implementation of {@link VirtualNetworkTransportProvider}.
 * 
 * TODO the internal content serialization/deserialization is obsolete; it is not handled outside of
 * the transports
 * 
 * @author Robert Mischke
 */
public class VirtualNetworkMessageChannel extends AbstractMessageChannel {

    protected final Log log = LogFactory.getLog(getClass());

    private RawMessageChannelEndpointHandler receivingRawEndpointHandler;

    private InitialNodeInformation ownNodeInformation;

    private ThreadPool threadPool = SharedThreadPool.getInstance();

    /**
     * TODO krol_ph: enter comment!
     * 
     */
    public VirtualNetworkMessageChannel(InitialNodeInformation ownNodeInformation,
        RawMessageChannelEndpointHandler receivingRawEndpointHandler,
        ServerContactPoint remoteSCP) {
        this.receivingRawEndpointHandler = receivingRawEndpointHandler;
        this.ownNodeInformation = ownNodeInformation;
        this.associatedSCP = remoteSCP;
        // this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void sendRequest(final NetworkRequest request, final RawNetworkResponseHandler responseHandler, int timeoutMsec) {
        // TODO add local timeout
        // TODO send NetworkResponseHandler connection failure response on invalid destination

        // sanity check to avoid exceptions in async code
        if (request == null || responseHandler == null) {
            throw new NullPointerException();
        }

        Callable<NetworkResponse> task = new Callable<NetworkResponse>() {

            @Override
            @TaskDescription("Virtual connection message sending")
            public NetworkResponse call() throws Exception {
                if (isSimulatingBreakdown()) {
                    responseHandler.onChannelBroken(request, VirtualNetworkMessageChannel.this);
                    throw new ConnectionClosedException("Simulating breakdown of virtual channel " + getChannelId());
                }
                if (associatedSCP.isSimulatingBreakdown()) {
                    responseHandler.onChannelBroken(request, VirtualNetworkMessageChannel.this);
                    throw new ConnectionClosedException(associatedSCP + " is simulating breakdown; failing send attempt on channel "
                        + getChannelId());
                }
                try {
                    return simulateRoundTrip(request, responseHandler);
                } catch (RuntimeException e) {
                    log.warn("Uncaught RuntimeException", e);
                    String nodeId = ownNodeInformation.getNodeId().getIdString();
                    NetworkResponse errorResponse =
                        NetworkResponseFactory.generateResponseForExceptionWhileRouting(request, nodeId, e);
                    responseHandler.onResponseAvailable(errorResponse);
                    // responseHandler.onRequestFailure(request, VirtualNetworkConnection.this, e);
                    // TODO review: keep throwing this exception?
                    throw new CommunicationException("Failed to simulate request-response loop (request id: '" + request.getRequestId()
                        + "')", e);
                }
            }

            private NetworkResponse simulateRoundTrip(final NetworkRequest request, final RawNetworkResponseHandler responseHandler)
                throws SerializationException {

                // clone the associated node identifier
                // TODO is this actually necessary? embed sender UUID in metadata instead?
                final NodeIdentifier virtualSenderId = ownNodeInformation.getNodeId().clone();

                // create a detached clone of the request
                NetworkRequest clonedRequest = NetworkRequestFactory.createDetachedClone(request);

                // invoke the connection service on the "receiving" side and fetch the response
                NetworkResponse generatedResponse = receivingRawEndpointHandler.onRawRequestReceived(clonedRequest, virtualSenderId);
                // create a detached clone of the response
                NetworkResponse clonedResponse = createDetachedClone(generatedResponse);
                responseHandler.onResponseAvailable(clonedResponse);
                return clonedResponse;
            }
        };

        // TODO rework to plain runnable; no Future needed
        threadPool.submit(task);
    }

    @Override
    protected void onClosedOrBroken() {
        log.debug("Closing connection " + this + " (remote=" + getRemoteNodeInformation().getLogDescription() + ", NCP=" + associatedSCP);

        // on a clean shutdown, simulate "goodbye" message
        if (getState() == MessageChannelState.CLOSED) {
            if (isSimulatingBreakdown()) {
                log.debug("Simulating breakdown of virtual channel " + getChannelId() + "; not sending shutdown message");
                return;
            }
            if (associatedSCP.isSimulatingBreakdown()) {
                log.debug(associatedSCP + " is simulating breakdown; not sending shutdown message for channel " + getChannelId());
                return;
            }
            receivingRawEndpointHandler.onInboundChannelClosing(getChannelId());
        }
    }

    private NetworkResponseImpl createDetachedClone(NetworkResponse response) throws SerializationException {
        // clone the received metadata; should be safe as it is a String/String map
        final Map<String, String> clonedResponseMetaData = MessageMetaData.wrap(response.accessRawMetaData()).cloneData();
        // clone content byte array
        byte[] originalContentBytes = response.getContentBytes();
        byte[] detachedContentBytes = null;
        if (originalContentBytes != null) {
            detachedContentBytes = Arrays.copyOf(originalContentBytes, originalContentBytes.length);
        }

        NetworkResponseImpl clonedResponse = new NetworkResponseImpl(detachedContentBytes, clonedResponseMetaData);
        return clonedResponse;
    }

    @Deprecated
    // TODO review: delete? -- misc_ro
    private Serializable createDetachedMessageBody(Serializable originalBody) {
        if (originalBody == null) {
            return null;
        }
        // simulate a remote call by serializing the original message body,
        // then deserializing it again. this is similar to a "clone" call,
        // but provides a stronger test of serializability. -- misc_ro
        Serializable deserializedBody;
        try {
            final byte[] serializedBody = MessageUtils.serializeObject(originalBody);
            deserializedBody = (Serializable) MessageUtils.deserializeObject(serializedBody);
        } catch (SerializationException e) {
            throw new RuntimeException("Failed to create detached copy of message body: " + originalBody, e);
        }
        return deserializedBody;
    }
}
