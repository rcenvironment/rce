/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link EndpointDatumProcessor}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumProcessorImpl implements EndpointDatumProcessor {

    private static final int CACHE_SIZE = 20;

    private SharedThreadPool threadPool = SharedThreadPool.getInstance();

    private AsyncOrderedExecutionQueue executionQueue = new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED,
        threadPool);

    private Map<String, WeakReference<ComponentExecutionController>> compExeCtrls = new LRUMap<>(CACHE_SIZE);
    
    private Map<NodeIdentifier, WeakReference<EndpointDatumProcessor>> endpointDatumProcessors = new LRUMap<>(CACHE_SIZE);

    private BundleContext bundleContext;

    private CommunicationService communicationService;
    
    private PlatformService platformService;
    
    private EndpointDatumSerializer endpointDatumSerializer;

    protected void activate(BundleContext context) {
        bundleContext = context;
    }

    @Override
    @AllowRemoteAccess
    public void onEndpointDatumReceived(String serializedEndpointDatum) {
        final EndpointDatum endpointDatum = endpointDatumSerializer.deserializeEndpointDatum(serializedEndpointDatum);
        final String executionId = endpointDatum.getInputComponentExecutionIdentifier();
        final NodeIdentifier node = endpointDatum.getInputsNodeId();
        executionQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                if (platformService.isLocalNode(node)) {
                    processEndpointDatum(executionId, endpointDatum);
                } else {
                    forwardEndpointDatum(node, endpointDatum);
                }
            }
        });

    }
    
    private void forwardEndpointDatum(NodeIdentifier node, EndpointDatum endpointDatum) {
        EndpointDatumProcessor processor = null;
        synchronized (endpointDatumProcessors) {
            if (endpointDatumProcessors.containsKey(node)) {
                processor = endpointDatumProcessors.get(node).get();
            }
            if (processor == null) {
                processor = (EndpointDatumProcessor) communicationService.getService(
                    EndpointDatumProcessor.class, node, bundleContext);
                endpointDatumProcessors.put(node, new WeakReference<EndpointDatumProcessor>(processor));
            }
        }
        processor.onEndpointDatumReceived(endpointDatumSerializer.serializeEndpointDatum(endpointDatum));
    }
    
    private void processEndpointDatum(String executionId, EndpointDatum endpointDatum) {
        ComponentExecutionController ctrl = null;
        synchronized (compExeCtrls) {
            if (compExeCtrls.containsKey(executionId)) {
                ctrl = compExeCtrls.get(executionId).get();
            }
            if (ctrl == null) {
                Map<String, String> searchProperties = new HashMap<>();
                searchProperties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, executionId);
                try {
                    ctrl = (ComponentExecutionController) communicationService.getService(
                        ComponentExecutionController.class, searchProperties, null, bundleContext);
                    compExeCtrls.put(executionId, new WeakReference<ComponentExecutionController>(ctrl));
                } catch (IllegalStateException e) {
                    Log log = LogFactory.getLog(getClass());
                    log.warn(StringUtils.format("Endpoint datum '%s' not processed, because component controller (%s) is not available",
                        endpointDatum.toString(), executionId));
                    log.debug("Failed to get ComponentExecutionController (OSGi service)", e);
                    return;
                }
            }
        }
        ctrl.onEndpointDatumReceived(endpointDatum);
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }
    
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }
    
    protected void bindEndpointDatumSerializer(EndpointDatumSerializer newService) {
        endpointDatumSerializer = newService;
    }

}
