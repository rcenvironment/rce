/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.routing.InstanceRestartAndPresenceService;
import de.rcenvironment.core.communication.routing.InstanceSessionNetworkStatus;
import de.rcenvironment.core.communication.routing.InstanceSessionNetworkStatus.State;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Periodically checks whether any nodes/instances hosting at least one component have been restarted since the start of the workflow. If
 * this happens, all components on these nodes are considered permanently "lost", causing the workflow to fail cleanly.
 *
 * @author Robert Mischke
 */
public class NodeRestartWatcher implements Runnable {

    /**
     * The default interval between two invocations of this check.
     */
    public static final long DEFAULT_TEST_INTERVAL_MSEC = 10000; // the test works on cached local data, so it is fairly cheap

    private final Map<InstanceNodeSessionId, List<String>> instanceSessionIdsAndHostedComponentExecIds = new HashMap<>();

    private final ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier;

    private final InstanceRestartAndPresenceService restartAndPresenceService;

    private final InstanceNodeSessionId localInstanceSessionId;

    private final Log log = LogFactory.getLog(getClass());

    private LiveNetworkIdResolutionService idResolutionService;

    public NodeRestartWatcher(ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier,
        WorkflowExecutionContext wfExeCtx, ServiceRegistryAccess serviceRegistryAccess) {
        this.compStatesEntirelyChangedVerifier = compStatesEntirelyChangedVerifier;
        this.idResolutionService = serviceRegistryAccess.getService(LiveNetworkIdResolutionService.class);
        this.localInstanceSessionId = serviceRegistryAccess.getService(PlatformService.class).getLocalInstanceNodeSessionId();
        this.restartAndPresenceService = serviceRegistryAccess.getService(InstanceRestartAndPresenceService.class);
    }

    /**
     * Registers instantiated {@link ComponentExecutionContext}s in this watcher.
     * 
     * @param compExecContexts the {@link ComponentExecutionContext}s to register
     * @throws WorkflowExecutionException if the initial reachability check fails
     */
    public synchronized void initialize(Collection<ComponentExecutionContext> compExecContexts) throws WorkflowExecutionException {
        for (final ComponentExecutionContext compExecContext : compExecContexts) {
            LogicalNodeSessionId resolvedLNId;
            try {
                resolvedLNId = idResolutionService.resolveToLogicalNodeSessionId(compExecContext.getNodeId());
            } catch (IdentifierException e) {
                // no matching node in the current network
                throw new WorkflowExecutionException(
                    "No matching node for location of component " + compExecContext.getExecutionIdentifier() + ", which is node "
                        + compExecContext.getNodeId() + " - it has probably become unreachable since the workflow was initiated");
            }

            final InstanceNodeSessionId componentHostInstanceSessionId = resolvedLNId.convertToInstanceNodeSessionId();
            if (localInstanceSessionId.isSameInstanceNodeSessionAs(componentHostInstanceSessionId)) {
                continue; // ignore the local node; no need for checking it
            }
            List<String> compExecIdList =
                instanceSessionIdsAndHostedComponentExecIds.computeIfAbsent(componentHostInstanceSessionId, key -> new ArrayList<>());
            compExecIdList.add(compExecContext.getExecutionIdentifier());
        }

        // test internal consistency by checking all nodes with the same API used later
        for (Map.Entry<InstanceNodeSessionId, List<String>> entry : instanceSessionIdsAndHostedComponentExecIds.entrySet()) {
            final InstanceNodeSessionId instanceSessionId = entry.getKey();
            final State networkState = restartAndPresenceService.queryInstanceSessionNetworkStatus(instanceSessionId).getState();
            if (networkState == InstanceSessionNetworkStatus.State.PRESENT_WITH_DIFFERENT_SESSION
                || networkState == InstanceSessionNetworkStatus.State.ID_COLLISION) {
                throw new WorkflowExecutionException(
                    "The instance session " + instanceSessionId
                        + " is unreachable right after selecting it as the location for one or more workflow components "
                        + "- it has probably become unreachable moments ago");
            } else {
                log.debug("Verified initial instance session reachability of " + instanceSessionId + " hosting components "
                    + Arrays.toString(entry.getValue().toArray()));
            }
        }
    }

    // Periodically check for restarts of nodes running workflow components (old task description)
    @Override
    public synchronized void run() {
        Set<String> lostComponentExecIds = null;
        for (Map.Entry<InstanceNodeSessionId, List<String>> entry : instanceSessionIdsAndHostedComponentExecIds.entrySet()) {
            final InstanceNodeSessionId instanceSessionId = entry.getKey();
            final State networkState = restartAndPresenceService.queryInstanceSessionNetworkStatus(instanceSessionId).getState();
            if (networkState == InstanceSessionNetworkStatus.State.PRESENT_WITH_DIFFERENT_SESSION
                || networkState == InstanceSessionNetworkStatus.State.ID_COLLISION) {
                final String reasonText;
                if (networkState == InstanceSessionNetworkStatus.State.PRESENT_WITH_DIFFERENT_SESSION) {
                    reasonText = " has been restarted, which means that these component runs cannot be recovered";
                } else {
                    reasonText = " is affected by a network id collision, "
                        + "which could result in undefined behavior and is therefore not allowed";
                }
                // TODO component names would be more user friendly here, but they are not immediately available
                log.warn("The instance " + instanceSessionId + " running the component(s) "
                    + Arrays.toString(entry.getValue().toArray()) + reasonText + "; the workflow will be aborted");
                if (lostComponentExecIds == null) {
                    lostComponentExecIds = new HashSet<>();
                }
                lostComponentExecIds.addAll(entry.getValue());
            }
        }
        if (lostComponentExecIds != null) {
            if (lostComponentExecIds.isEmpty()) {
                throw new IllegalStateException("Internal consistency error: created a list of lost nodes but it is empty");
            }
            compStatesEntirelyChangedVerifier.announceLostComponents(lostComponentExecIds);
        }
    }

}
