/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.execution.api.ComponentControllerRoutingMap;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.EndpointDatumDispatchService;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.RemotableEndpointDatumDispatcher;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;

/**
 * Implementation of {@link RemotableEndpointDatumDispatcher}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
@Component
public class EndpointDatumDispatcherImpl implements EndpointDatumDispatchService, RemotableEndpointDatumDispatcher {

    private static final String FAILED_TO_SEND_ENDPOINT_DATUM = "Failed to send endpoint datum %s";

    private static final Log LOG = LogFactory.getLog(EndpointDatumDispatcherImpl.class);

    private static final int CACHE_SIZE = 20;

    private AsyncOrderedExecutionQueue executionQueue = ConcurrencyUtils.getFactory().createAsyncOrderedExecutionQueue(
        AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private Map<String, WeakReference<ComponentExecutionController>> compExeCtrls = new LRUMap<>(CACHE_SIZE);

    private Map<String, ComponentControllerRoutingMap> componentControllerForwardingMaps = Collections.synchronizedMap(new HashMap<>());

    private BundleContext bundleContext;

    private CommunicationService communicationService;

    private LocalExecutionControllerUtilsService exeCtrlUtilsService;

    private PlatformService platformService;

    private EndpointDatumSerializer endpointDatumSerializer;

    @Activate
    protected void activate(BundleContext context) {
        bundleContext = context;
    }

    @Override
    public void dispatchEndpointDatum(final EndpointDatum endpointDatum) {
        final String executionId = endpointDatum.getInputsComponentExecutionIdentifier();
        executionQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                if (platformService.matchesLocalInstance(endpointDatum.getDestinationNodeId())) {
                    // datum has arrived at its location -> process it
                    processEndpointDatum(executionId, endpointDatum);
                } else {
                    forwardEndpointDatum(endpointDatum);
                }
            }

        });
    }

    @Override
    @AllowRemoteAccess
    public void dispatchEndpointDatum(String serializedEndpointDatum) {
        dispatchEndpointDatum(endpointDatumSerializer.deserializeEndpointDatum(serializedEndpointDatum));
    }

    @Override
    public void registerComponentControllerForwardingMap(String workflowExecutionId, ComponentControllerRoutingMap destinationMap) {
        componentControllerForwardingMaps.put(workflowExecutionId, destinationMap);
        LOG.debug("Registered component endpoint forwarding map for workflow " + workflowExecutionId);
    }

    @Override
    public void unregisterComponentControllerForwardingMap(String workflowExecutionId) {
        componentControllerForwardingMaps.remove(workflowExecutionId);
        LOG.debug("Unregistered component endpoint forwarding map for workflow " + workflowExecutionId);
    }

    protected void forwardEndpointDatum(EndpointDatum endpointDatum) {

        try {
            // does the endpoint have a prepared network destination?
            final NetworkDestination networkDestination;
            if (endpointDatum.getNetworkDestination() != null) {
                // if yes, then we are on a component controller node; simply use the provided destination
                networkDestination = endpointDatum.getNetworkDestination();
            } else {
                // otherwise, we are on the associated workflow controller and are forwarding the datum
                final String workflowExecutionIdentifier = endpointDatum.getWorkflowExecutionIdentifier();
                final String componentExecutionIdentifier = endpointDatum.getInputsComponentExecutionIdentifier();
                ComponentControllerRoutingMap routingMap =
                    componentControllerForwardingMaps.get(workflowExecutionIdentifier);
                if (routingMap == null) {
                    throw new RemoteOperationException(
                        "A endpoint datum forwarding for target component " + componentExecutionIdentifier
                            + " was requested, but there is no routing information available for workflow " + workflowExecutionIdentifier);
                }
                networkDestination = routingMap.getNetworkDestinationForComponentController(componentExecutionIdentifier);
                if (networkDestination == null) {
                    throw new RemoteOperationException(
                        "Found routing information for workflow " + workflowExecutionIdentifier
                            + ", but it did not contain a route for component " + componentExecutionIdentifier);
                }
            }

            // fetching the service proxy on each call, assuming that it will be cached centrally if necessary
            final RemotableEndpointDatumDispatcher dispatcher =
                communicationService.getRemotableService(RemotableEndpointDatumDispatcher.class, networkDestination);

            dispatcher.dispatchEndpointDatum(endpointDatumSerializer.serializeEndpointDatum(endpointDatum));
            // ComponentExecutionUtils.logCallbackSuccessAfterFailure(LOG, StringUtils.format("Sending endpoint datum %s",
            // endpointDatum), failureCount);
            // break;
        } catch (RemoteOperationException e) {
            // if (++failureCount < ComponentExecutionUtils.MAX_RETRIES) {
            // ComponentExecutionUtils.waitForRetryAfterCallbackFailure(LOG, failureCount, StringUtils.format(
            // FAILED_TO_SEND_ENDPOINT_DATUM, endpointDatum), e.toString());
            // } else {
            // ComponentExecutionUtils.logCallbackFailureAfterRetriesExceeded(LOG, StringUtils.format(FAILED_TO_SEND_ENDPOINT_DATUM,
            // endpointDatum), e);
            callbackComponentExecutionController(endpointDatum, e);
            // break;
            // }
        }

    }

    protected void callbackComponentExecutionController(EndpointDatum endpointDatum, RemoteOperationException e) {
        if (platformService.matchesLocalInstance(endpointDatum.getOutputsNodeId())) {
            callbackComponentExecutionControllerLocally(endpointDatum, e);
        } else {
            callbackComponentExecutionControllerRemotely(endpointDatum, e);
        }
    }

    private void callbackComponentExecutionControllerLocally(EndpointDatum endpointDatum, RemoteOperationException e) {
        String executionId = endpointDatum.getOutputsComponentExecutionIdentifier();
        ComponentExecutionController compExeCtrl = null;
        try {
            compExeCtrl = getComponentExecutionController(executionId);
        } catch (ExecutionControllerException e1) {
            LOG.warn(StringUtils.format("Failed to announce that sending endpoint datum '%s'; failed cause: %s",
                endpointDatum.toString(), e1.toString()));
            return;
        }
        compExeCtrl.onSendingEndointDatumFailed(endpointDatum, e);
    }

    private void callbackComponentExecutionControllerRemotely(EndpointDatum endpointDatum, RemoteOperationException e) {
        String outputCompExeId = endpointDatum.getOutputsComponentExecutionIdentifier();
        RemotableComponentExecutionControllerService compExeCtrlService;
        compExeCtrlService = communicationService.getRemotableService(RemotableComponentExecutionControllerService.class,
            endpointDatum.getOutputsNodeId());
        try {
            compExeCtrlService.onSendingEndointDatumFailed(outputCompExeId, endpointDatumSerializer.serializeEndpointDatum(endpointDatum),
                e);
        } catch (ExecutionControllerException | RemoteOperationException e1) {
            LOG.warn(StringUtils.format("Failed to announce that sending endpoint datum '%s' failed; cause: %s",
                endpointDatum, e1.toString()));
        }
    }

    protected void processEndpointDatum(String executionId, EndpointDatum endpointDatum) {
        ComponentExecutionController compExeCtrl = null;
        try {
            compExeCtrl = getComponentExecutionController(executionId);
        } catch (ExecutionControllerException e) {
            LOG.warn(StringUtils.format("Endpoint datum '%s' not processed; cause: %s",
                endpointDatum.toString(), e.toString()));
            return;
        }
        compExeCtrl.onEndpointDatumReceived(endpointDatum);
    }

    private ComponentExecutionController getComponentExecutionController(String executionId) throws ExecutionControllerException {
        ComponentExecutionController compExeCtrl = null;
        synchronized (compExeCtrls) {
            if (compExeCtrls.containsKey(executionId)) {
                compExeCtrl = compExeCtrls.get(executionId).get();
            }
            if (compExeCtrl == null) {
                compExeCtrl = exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext);
                compExeCtrls.put(executionId, new WeakReference<ComponentExecutionController>(compExeCtrl));
            }
        }
        return compExeCtrl;
    }

    @Reference
    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    @Reference
    protected void bindLocalExecutionControllerUtilsService(LocalExecutionControllerUtilsService newService) {
        exeCtrlUtilsService = newService;
    }

    @Reference
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    @Reference
    protected void bindEndpointDatumSerializer(EndpointDatumSerializer newService) {
        endpointDatumSerializer = newService;
    }

}
