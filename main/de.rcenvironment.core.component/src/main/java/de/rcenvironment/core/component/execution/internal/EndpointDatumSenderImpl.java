/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link EndpointDatumSender}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumSenderImpl implements EndpointDatumSender {

    private static final int MAX_SIZE = 500;

    private SharedThreadPool threadPool = SharedThreadPool.getInstance();

    private AsyncOrderedExecutionQueue executionQueue = new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED,
        threadPool);

    private Map<NodeIdentifier, EndpointDatumProcessor> endpointDatumProcessors = new LRUMap<>(MAX_SIZE);

    private BundleContext bundleContext;

    private CommunicationService communicationService;

    protected void activate(BundleContext context) {
        bundleContext = context;
    }

    @Override
    @AllowRemoteAccess
    public void sendEndpointDatumOrderedAsync(final EndpointDatum endpointDatum) {
        NodeIdentifier node;
        if (communicationService.getReachableNodes().contains(endpointDatum.getInputsNodeId())) {
            node = endpointDatum.getInputsNodeId();
        } else {
            node = endpointDatum.getWorkflowNodeId();
        }
        final NodeIdentifier finalNode = node;
        executionQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                if (!endpointDatumProcessors.containsKey(finalNode)) {
                    EndpointDatumProcessor inputProcessor = (EndpointDatumProcessor) communicationService.getService(
                        EndpointDatumProcessor.class, finalNode, bundleContext);
                    endpointDatumProcessors.put(finalNode, inputProcessor);
                }
                endpointDatumProcessors.get(finalNode).onEndpointDatumReceived(EndpointDatumSerializer
                    .serializeEndpointDatum(endpointDatum));
            }
        });
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

}
