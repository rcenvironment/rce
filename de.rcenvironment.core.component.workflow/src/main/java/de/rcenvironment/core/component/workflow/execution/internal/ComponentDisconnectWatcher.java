/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Checks periodically if heartbeat messages of workflow-related components were received and react on timeouts. Until RCE 8, this detected
 * timeouts and always treated them as permanent failures. Since RCE 9 provides robustness against network disruptions, timeouts are
 * reported as temporary disconnects first, and only treated as permanent failure after a very long timeout.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (RCE 9 changes)
 */
public class ComponentDisconnectWatcher implements Runnable {

    /**
     * After a short time without heartbeats from a component, the component will be marked as "disconnected" (purely as state information
     * for users).
     */
    public static final long DEFAULT_DISCONNECT_REPORTING_INTERVAL_MSEC = TimeUnit.SECONDS.toMillis(90);

    /**
     * After a long time without heartbeats from a component, the workflow will fail.
     */
    // greatly increased when rRPC streams were introduced;
    // idea behind 3 days timeout = "workflow running over weekend, notice and reconnect a node on monday morning"
    public static final long DEFAULT_FAILURE_INTERVAL_MSEC = TimeUnit.DAYS.toMillis(3);

    /**
     * The default interval between two invocations of this check.
     */
    public static final long DEFAULT_TEST_INTERVAL_MSEC = 10000; // runs on cached local data, so it is fairly cheap

    protected static long maxDisconnectReportingIntervalMsec = DEFAULT_DISCONNECT_REPORTING_INTERVAL_MSEC;

    protected static long maxFailureIntervalMsec = DEFAULT_FAILURE_INTERVAL_MSEC;

    private static final Log LOG = LogFactory.getLog(ComponentDisconnectWatcher.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    private final Map<String, Long> componentHeartbeatTimestamps = new HashMap<String, Long>();

    private final Set<String> componentsMarkedAsDisconnected = new HashSet<>();

    private final ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier;

    private final String logMessage;

    public ComponentDisconnectWatcher(ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier,
        WorkflowExecutionContext wfExeCtx) {
        this.compStatesEntirelyChangedVerifier = compStatesEntirelyChangedVerifier;
        logMessage = StringUtils.format("Checking component heartbeats for workflow '%s' (%s)",
            wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier());
    }

    /**
     * Announce a new component heartbeat.
     * 
     * @param compExecutionId execution identifier of component the heartbeat belongs to
     */
    public void announceComponentHeartbeat(String compExecutionId) {
        synchronized (componentHeartbeatTimestamps) {
            componentHeartbeatTimestamps.put(compExecutionId, System.currentTimeMillis());
            if (componentsMarkedAsDisconnected.contains(compExecutionId)) {
                LOG.info("Component " + compExecutionId + " has become reachable again; resuming workflow");
                componentsMarkedAsDisconnected.remove(compExecutionId);
            }
        }
    }

    // Peridically check for heartbeat messages from components (old task description)
    @Override
    public void run() {
        if (VERBOSE_LOGGING) {
            LOG.debug(logMessage);
        }
        long currentTimestamp = System.currentTimeMillis();
        Set<String> compExeIdsLost = new HashSet<>();
        synchronized (componentHeartbeatTimestamps) {
            for (String compExeId : componentHeartbeatTimestamps.keySet()) {
                final long lastHeartbeatAge = currentTimestamp - componentHeartbeatTimestamps.get(compExeId);
                if (lastHeartbeatAge > maxFailureIntervalMsec
                    && !compStatesEntirelyChangedVerifier.isComponentInFinalState(compExeId)) {
                    compExeIdsLost.add(compExeId);
                } else if (lastHeartbeatAge > maxDisconnectReportingIntervalMsec
                    && !compStatesEntirelyChangedVerifier.isComponentInFinalState(compExeId)
                    && !componentsMarkedAsDisconnected.contains(compExeId)) {
                    LOG.info("Component " + compExeId
                        + " has become disconnected or overloaded; the workflow will resume if and when it reconnects");
                    componentsMarkedAsDisconnected.add(compExeId);
                }
            }
        }
        if (!compExeIdsLost.isEmpty()) {
            compStatesEntirelyChangedVerifier.announceLostComponents(compExeIdsLost);
        }
    }
}
