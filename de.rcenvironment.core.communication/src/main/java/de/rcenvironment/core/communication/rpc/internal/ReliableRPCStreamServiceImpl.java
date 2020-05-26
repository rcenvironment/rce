/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.api.RemotableReliableRPCStreamService;
import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextHolder;

/**
 * Default {@link ReliableRPCStreamService} implementation that also provides the remotable methods of
 * {@link RemotableReliableRPCStreamService}.
 *
 * @author Robert Mischke
 */
@Component
public class ReliableRPCStreamServiceImpl implements RemotableReliableRPCStreamService, ReliableRPCStreamService {

    private MessageRoutingService routingService;

    private RemoteServiceCallHandlerService serviceCallHandlerService;

    /**
     * Generator for the stream ids generated at this instance, ie for streams in which the local node is the receiving end.
     */
    private final AtomicInteger streamIdGenerator = new AtomicInteger(0);

    /**
     * A map of stream receivers, with stream ids as map keys. These receivers are used to handle streams in which the local node is the
     * receiving end.
     */
    private final Map<String, ReliableRPCStreamReceiver> streamReceivers = new HashMap<>();

    private final Map<ReliableRPCStreamHandle, ReliableRPCStreamSender> streamSenders = new HashMap<>();

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("RemoteServiceCalls");

    private final Log log = LogFactory.getLog(getClass());

    @Override
    @AllowRemoteAccess
    public String createReliableRPCStream() throws RemoteOperationException {
        String streamId = Integer.toString(streamIdGenerator.incrementAndGet());
        synchronized (streamReceivers) {
            ReliableRPCStreamReceiver replaced =
                streamReceivers.put(streamId, new ReliableRPCStreamReceiver(streamId, serviceCallHandlerService));
            if (replaced != null) {
                throw new IllegalStateException(); // consistency error; should never happen
            }
        }
        log.debug(
            "Created rRPC stream " + streamId + " initiated by "
                + ThreadContextHolder.getCurrentContextAspect(ServiceCallContext.class).getCallingNode());
        return streamId;
    }

    @Override
    @AllowRemoteAccess
    public void disposeReliableRPCStream(String streamId) throws RemoteOperationException {
        // TODO trivial implementation for conceptual testing; probably needs more cleanup/shutdown steps
        synchronized (streamReceivers) {
            ReliableRPCStreamReceiver removed = streamReceivers.remove(streamId);
            if (removed == null) {
                throw new RemoteOperationException("Requested to dispose an rRPC stream that does not exist; streamId = " + streamId);
            }
        }
    }

    @Override
    public ReliableRPCStreamHandle createLocalSetupForRemoteStreamId(LogicalNodeSessionId resolvedTargetNodeId, String streamId) {
        final ReliableRPCStreamHandleImpl streamHandle = new ReliableRPCStreamHandleImpl(resolvedTargetNodeId, streamId);
        synchronized (streamSenders) {
            final ReliableRPCStreamSender replaced =
                streamSenders.put(streamHandle, new ReliableRPCStreamSender(streamHandle, routingService));
            if (replaced != null) {
                throw new IllegalStateException("There already was a stream sender registered for rRPC stream handle " + streamId);
            }
        }
        return streamHandle;
    }

    @Override
    public ServiceCallResult performRequest(ServiceCallRequest serviceCallRequest) throws SerializationException {
        final String streamId = serviceCallRequest.getReliableRPCStreamId();
        final ReliableRPCStreamSender streamSender;
        synchronized (streamSenders) {
            streamSender = streamSenders.get(serviceCallRequest.getSenderSideReliableRPCStreamHandle());
            if (streamSender == null) {
                // TODO (p1) prototype code - handle this properly instead
                throw new RuntimeException(
                    "No active sender for rRPC stream " + streamId + " - it may have been closed before this RPC was initiated");
            }
        }
        return streamSender.performRequest(serviceCallRequest);
    }

    @Override
    public ServiceCallResult handleIncomingRequest(ServiceCallRequest serviceCallRequest) throws InternalMessagingException {
        final String streamId = serviceCallRequest.getReliableRPCStreamId();
        ReliableRPCStreamReceiver streamReceiver;
        synchronized (streamReceivers) {
            streamReceiver = streamReceivers.get(streamId);
            if (streamReceiver == null) {
                // TODO (p1) check if this exception handling is appropriate
                throw new InternalMessagingException(
                    "No active receiver for rRPC stream " + streamId + " - it may have been closed before this RPC was received", null);
            }
        }
        return streamReceiver.handle(serviceCallRequest);
    }

    /**
     * Injects the {@link MessageRoutingService} to use for performing actual remote request attempts. Called by OSGi-DS and unit tests.
     * 
     * @param newInstance the service implementation
     */
    @Reference
    public void bindMessageRoutingService(MessageRoutingService newInstance) {
        this.routingService = newInstance;
    }

    /**
     * Injects the {@link RemoteServiceCallHandlerService} to use for dispatching incoming remote request attempts. Called by OSGi-DS and
     * unit tests.
     * 
     * Note that this service reference must be dynamic to prevent a startup cycle. The reference is also not actually "optional", but it is
     * not required for activation. It must have been set before the first use of {@link #createReliableRPCStream()}, though.
     * 
     * @param newInstance the service implementation
     */
    @Reference
    public void bindServiceCallHandlerService(RemoteServiceCallHandlerService newInstance) {
        this.serviceCallHandlerService = newInstance;
    }

}
