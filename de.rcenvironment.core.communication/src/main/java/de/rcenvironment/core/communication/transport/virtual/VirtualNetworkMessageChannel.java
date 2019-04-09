/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.virtual;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.connection.internal.ConnectionClosedException;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.impl.NetworkResponseImpl;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.transport.spi.AbstractMessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.communication.transport.spi.MessageChannelResponseHandler;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * The {@link MessageChannel} implementation of {@link VirtualNetworkTransportProvider}.
 * 
 * @author Robert Mischke
 */
public class VirtualNetworkMessageChannel extends AbstractMessageChannel {

    protected final Log log = LogFactory.getLog(getClass());

    private MessageChannelEndpointHandler receivingRawEndpointHandler;

    private InitialNodeInformation ownNodeInformation;

    private AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    public VirtualNetworkMessageChannel(InitialNodeInformation ownNodeInformation,
        String ownProtocolVersion, MessageChannelEndpointHandler receivingRawEndpointHandler, ServerContactPoint remoteSCP)
        throws CommunicationException {
        this.receivingRawEndpointHandler = receivingRawEndpointHandler;
        this.ownNodeInformation = ownNodeInformation;
        this.associatedSCP = remoteSCP;

        // note: this check is performed at a different place than in the JMS channel
        failOnIncompatibleVersions(remoteSCP.getExpectedProtocolVersion(), ownProtocolVersion);
    }

    @Override
    public void sendRequest(final NetworkRequest request, final MessageChannelResponseHandler responseHandler, int timeoutMsec) {
        // TODO add local timeout
        // TODO send NetworkResponseHandler connection failure response on invalid destination

        // sanity check to avoid exceptions in async code
        if (request == null || responseHandler == null) {
            throw new NullPointerException();
        }

        Callable<NetworkResponse> task = new Callable<NetworkResponse>() {

            @Override
            @TaskDescription("Communication Layer: Virtual connection message sending")
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
                    String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log, "Uncaught RuntimeException", e);
                    NetworkResponse errorResponse =
                        NetworkResponseFactory.generateResponseForErrorDuringDelivery(request,
                            ownNodeInformation.getInstanceNodeSessionId(), errorId);
                    responseHandler.onResponseAvailable(errorResponse);
                    // responseHandler.onRequestFailure(request, VirtualNetworkConnection.this, e);
                    // TODO review: keep throwing this exception?
                    throw new CommunicationException("Failed to simulate request-response loop (request id: '" + request.getRequestId()
                        + "')", e);
                }
            }

            private NetworkResponse simulateRoundTrip(final NetworkRequest request, final MessageChannelResponseHandler responseHandler)
                throws SerializationException {

                // as only the string form is passed, this is detached from the sending side
                final String virtualSenderId = ownNodeInformation.getInstanceNodeSessionId().getInstanceNodeSessionIdString();

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

}
