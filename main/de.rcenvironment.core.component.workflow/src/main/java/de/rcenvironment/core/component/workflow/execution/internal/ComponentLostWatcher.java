/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Checks in periodically if heartbeat of workflow-related components were received and announce any lost.
 * 
 * @author Doreen Seider
 */
public class ComponentLostWatcher implements Runnable {

    /**
     * After 140 seconds without heartbeat from component, the workflow will fail.
     */
    public static final int DEFAULT_MAX_HEA7RTBEAT_INTERVAL_MSEC = 140 * 1000;
    
    protected static int maxHeartbeatIntervalMsec = DEFAULT_MAX_HEA7RTBEAT_INTERVAL_MSEC;

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionControllerImpl.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(ComponentLostWatcher.class);

    private final Map<String, Long> componentHeartbeatTimestamps = Collections.synchronizedMap(new HashMap<String, Long>());

    private final ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier;

    private final String logMessage;

    public ComponentLostWatcher(ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier,
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
        componentHeartbeatTimestamps.put(compExecutionId, System.currentTimeMillis());
    }

    @Override
    @TaskDescription("Check heartbeats of components")
    public void run() {
        if (VERBOSE_LOGGING) {
            LOG.debug(logMessage);
        }
        long currentTimestamp = System.currentTimeMillis();
        Set<String> compExeIdsLost = new HashSet<>();
        synchronized (componentHeartbeatTimestamps) {
            for (String compExeId : componentHeartbeatTimestamps.keySet()) {
                if (currentTimestamp - componentHeartbeatTimestamps.get(compExeId) > maxHeartbeatIntervalMsec
                    && !compStatesEntirelyChangedVerifier.isComponentInFinalState(compExeId)) {
                    compExeIdsLost.add(compExeId);
                }
            }
        }
        if (!compExeIdsLost.isEmpty()) {
            compStatesEntirelyChangedVerifier.announceLostComponents(compExeIdsLost);
        }
    }
}
