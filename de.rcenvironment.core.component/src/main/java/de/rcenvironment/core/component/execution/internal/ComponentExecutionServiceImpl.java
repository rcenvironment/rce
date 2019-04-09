/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link ComponentExecutionService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ComponentExecutionServiceImpl implements ComponentExecutionService {

    // Note that these default limits are currently very conservative; once the workflow graph payload size has been reduced,
    // these could be increased again (see Mantis #16249). -- misc_ro, Oct 2018

    private static final int RATE_LIMITING_DEFAULT_MAX_CONCURRENT_REQUESTS_PER_NODE = 1;

    private static final int RATE_LIMITING_DEFAULT_MAX_GLOBAL_CONCURRENT_REQUESTS = 2;

    // override properties
    private static final String RATE_LIMITING_SYSTEM_PROPERTY_MAX_CONCURRENT_REQUESTS_PER_NODE = "rce.maxComponentInitRequestsPerNode";

    private static final String RATE_LIMITING_SYSTEM_PROPERTY_MAX_GLOBAL_CONCURRENT_REQUESTS = "rce.maxGlobalComponentInitRequests";

    private static final long RATE_LIMITING_RETRY_INTERVAL_MSEC = 250; // local check only, so this can be fairly low to avoid extra latency

    /**
     * Implements rate limiting of concurrent component requests to the same target node to prevent traffic spikes on workflow start. See
     * https://mantis.sc.dlr.de/view.php?id=16250 for context.
     *
     * @author Robert Mischke
     */
    protected class ComponentInitializationRateLimiter {

        private final Log log;

        private int globalPendingRequestCount = 0;

        private final int maxConcurrentRequestsPerNode;

        private final int maxGlobalConcurrentRequests;

        // whenever a counter is reduced to zero, it is removed to avoid memory leaks
        private final Map<LogicalNodeId, Integer> requestCountPerNode = new HashMap<>();

        public ComponentInitializationRateLimiter() {
            log = LogFactory.getLog(getClass()); // ensure that this is set first, even if fields are changed later

            maxConcurrentRequestsPerNode = parsePotentialOverrideProperty(RATE_LIMITING_SYSTEM_PROPERTY_MAX_CONCURRENT_REQUESTS_PER_NODE,
                RATE_LIMITING_DEFAULT_MAX_CONCURRENT_REQUESTS_PER_NODE);

            maxGlobalConcurrentRequests = parsePotentialOverrideProperty(RATE_LIMITING_SYSTEM_PROPERTY_MAX_GLOBAL_CONCURRENT_REQUESTS,
                RATE_LIMITING_DEFAULT_MAX_GLOBAL_CONCURRENT_REQUESTS);

            log.debug("Component initialization limits set to " + maxConcurrentRequestsPerNode + " concurrent requests per node and "
                + maxGlobalConcurrentRequests + " concurrent global requests");
        }

        synchronized boolean acquirePermission(LogicalNodeId nodeId) {

            // do not perform any rate limiting for local components, and also don't count them towards the global limit
            if (platformService.matchesLocalInstance(nodeId)) {
                return true;
            }

            if (globalPendingRequestCount >= maxGlobalConcurrentRequests) {
                return false;
            }

            final Integer currentWrapper = requestCountPerNode.get(nodeId);
            final int oldValue;
            if (currentWrapper != null) {
                oldValue = currentWrapper.intValue();
            } else {
                oldValue = 0;
            }

            if (oldValue >= maxConcurrentRequestsPerNode) {
                // limit reached
                return false;
            }

            // below limit, so increase the counters and return success
            requestCountPerNode.put(nodeId, Integer.valueOf(oldValue + 1));
            globalPendingRequestCount++;
            return true;
        }

        synchronized void releasePermission(LogicalNodeId nodeId) {

            // local components were not registered, so do not lower any counters for them either
            if (platformService.matchesLocalInstance(nodeId)) {
                return;
            }

            final Integer removedValue = requestCountPerNode.remove(nodeId);
            if (removedValue == null) {
                throw new IllegalStateException("Per-node request counter reduced below zero");
            }
            final int oldValue = removedValue.intValue();
            if (oldValue < 1) {
                // see field remark
                throw new IllegalStateException("Request counter map contained a stored value below 1");
            }
            // if the value was 1, it should stay removed, so there is nothing more to do
            if (oldValue > 1) {
                requestCountPerNode.put(nodeId, Integer.valueOf(oldValue - 1));
            }
            // always lower the global request counter
            if (--globalPendingRequestCount < 0) {
                throw new IllegalStateException("Global request counter reduced below zero");
            }
        }

        private int parsePotentialOverrideProperty(String propertyKey, int defaultValue) {
            int finalValue;
            final String sysPropertyValue = System.getProperty(propertyKey);
            if (sysPropertyValue != null && !sysPropertyValue.isEmpty()) {
                try {
                    finalValue = Integer.parseInt(sysPropertyValue);
                } catch (NumberFormatException e) {
                    log.error("Invalid value for " + propertyKey, e);
                    finalValue = defaultValue;
                }
            } else {
                finalValue = defaultValue;
            }
            return finalValue;
        }
    }

    private CommunicationService communicationService;

    private ComponentExecutionControllerService cmpExeCtrlService;

    private PlatformService platformService;

    private final ComponentInitializationRateLimiter rateLimiter = new ComponentInitializationRateLimiter();

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public String init(ComponentExecutionContext executionContext, String authToken, Long referenceTimestamp)
        throws ComponentExecutionException {
        final LogicalNodeId remoteNodeId = executionContext.getNodeId();

        boolean wasDelayedAtLeastOnce = false;
        while (!rateLimiter.acquirePermission(remoteNodeId)) {
            if (!wasDelayedAtLeastOnce) {
                log.debug(StringUtils.format("Delaying initialization of component %s on node %s for rate limiting",
                    executionContext.getExecutionIdentifier(), remoteNodeId));
                wasDelayedAtLeastOnce = true;
            }
            try {
                Thread.sleep(RATE_LIMITING_RETRY_INTERVAL_MSEC);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ComponentExecutionException("Interrupted while waiting for rate limiting permission: " + e.toString());
            }
        }
        if (wasDelayedAtLeastOnce) {
            log.debug(StringUtils.format("Performing initialization of component %s on node %s after waiting for rate limiting",
                executionContext.getExecutionIdentifier(), remoteNodeId));
        } else {
            log.debug(StringUtils.format("Performing immediate initialization of component %s on node %s (no rate limiting required)",
                executionContext.getExecutionIdentifier(), remoteNodeId));
        }

        try {
            try {
                if (authToken == null) {
                    // not allowed since 9.0.0; every component execution requires an auth token now
                    throw new ComponentExecutionException(
                        "Received a 'null' authorization token for component execution " + executionContext.getExecutionIdentifier());
                }
                return getExecutionControllerService(remoteNodeId).createExecutionController(executionContext, authToken,
                    referenceTimestamp);
            } catch (RemoteOperationException e) {
                throw new ComponentExecutionException(
                    "Error initiating component " + executionContext.getExecutionIdentifier() + " for execution", e);
            }
        } finally {
            rateLimiter.releasePermission(remoteNodeId);
        }
    }

    @Override
    public void pause(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPause(executionId);
    }

    @Override
    public void resume(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performResume(executionId);
    }

    @Override
    public void cancel(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performCancel(executionId);
    }

    @Override
    public void dispose(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performDispose(executionId);
    }

    @Override
    public void prepare(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPrepare(executionId);
    }

    @Override
    public void start(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performStart(executionId);
    }

    @Override
    public ComponentState getComponentState(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(node).getComponentState(executionId);
    }

    @Override
    public ComponentExecutionInformation getComponentExecutionInformation(final String verificationToken) throws RemoteOperationException {

        final AtomicReference<RemoteOperationException> remoteOperationExceptionRef = new AtomicReference<RemoteOperationException>(null);

        CallablesGroup<ComponentExecutionInformation> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(ComponentExecutionInformation.class);
        for (LogicalNodeId logicalNodeId : communicationService.getReachableLogicalNodes()) {
            final LogicalNodeId nodeId = logicalNodeId;
            callablesGroup.add(new Callable<ComponentExecutionInformation>() {

                @TaskDescription("Fetching component information")
                @Override
                public ComponentExecutionInformation call() throws Exception {
                    ComponentExecutionInformation compExeInfo =
                        getExecutionControllerService(nodeId).getComponentExecutionInformation(verificationToken);
                    if (compExeInfo != null) {
                        return compExeInfo;
                    }
                    return null;
                }
            });
            List<ComponentExecutionInformation> compExeInfos = callablesGroup.executeParallel(new AsyncExceptionListener() {

                @Override
                public void onAsyncException(Exception e) {
                    if (e instanceof RemoteOperationException && remoteOperationExceptionRef.get() == null) {
                        remoteOperationExceptionRef.set((RemoteOperationException) e);
                    }
                    LogFactory.getLog(ComponentExecutionServiceImpl.class)
                        .error("Error in asychronous request when retrieving component execution information for a verification key", e);
                }
            });

            if (remoteOperationExceptionRef.get() != null) {
                throw remoteOperationExceptionRef.get();
            }

            for (ComponentExecutionInformation compExeInfo : compExeInfos) {
                if (compExeInfo != null) {
                    return compExeInfo;
                }
            }
        }
        return null;
    }

    @Override
    public boolean verifyResults(String executionId, ResolvableNodeId node, String verificationToken, boolean verified)
        throws ExecutionControllerException, RemoteOperationException {
        return getExecutionControllerService(node).performVerifyResults(executionId, verificationToken, verified);
    }

    @Override
    public Set<ComponentExecutionInformation> getLocalComponentExecutionInformations() {
        return new HashSet<ComponentExecutionInformation>(cmpExeCtrlService.getComponentExecutionInformations());
    }

    private RemotableComponentExecutionControllerService getExecutionControllerService(NetworkDestination destination) {
        // TODO is this explicit check for the local node necessary?
        if (destination instanceof ResolvableNodeId && platformService.matchesLocalInstance((ResolvableNodeId) destination)) {
            return cmpExeCtrlService;
        } else {
            // fetching the service proxy on each call, assuming that it will be cached centrally if necessary
            return communicationService.getRemotableService(RemotableComponentExecutionControllerService.class, destination);
        }
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    protected void bindComponentExecutionControllerService(ComponentExecutionControllerService newService) {
        cmpExeCtrlService = newService;
    }

}
