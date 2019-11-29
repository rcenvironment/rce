/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link WorkflowExecutionStatsService}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionStatsServiceImpl implements WorkflowExecutionStatsService {

    private static final int THOUSAND = 1000;
    
    private static final String CAT_WF_EXEC = "Workflow Execution: Workflow runs by number of components";

    private static final String CAT_WF_EXEC_DURATION = "Workflow Execution: Duration by number of components [sec]";

    private static final String CAT_WF_EXEC_DISTR = "Workflow Execution: Components distribution per workflow run";

    private static final String CAT_WF_EXEC_FINAL_STATES = "Workflow Execution: Final states";

    private static final String KEY_00_05_COMPS = "00..05 components";

    private static final String KEY_06_10_COMPS = "06..10 components";

    private static final String KEY_11_20_COMPS = "11..20 components";

    private static final String KEY_21_30_COMPS = "21..30 components";

    private static final String KEY_31_50_COMPS = "31..50 components";

    private static final String KEY_MORE_THAN_50_COMPS = ">50 components";

    private static final String KEY_TOTAL = "[total]";

    private static final String KEY_LOCAL_COMPS = "local components only";

    private static final String KEY_DISTR_COMPS = "components distributed";

    private static final int FIFTY = 50;

    private static final int THIRTY = 30;

    private static final int TWENTY = 20;

    private PlatformService platformService;

    private Map<String, Long> wfExeStartTimestamps = Collections.synchronizedMap(new HashMap<String, Long>());

    @Override
    public void addStatsAtWorkflowStart(WorkflowExecutionContext wfExeCtx) {
        if (wfExeStartTimestamps.containsKey(wfExeCtx.getExecutionIdentifier())) {
            LogFactory.getLog(getClass()).error(StringUtils.format("Stats about duration for component '%s' (%s) not recorded properly: "
                + "last time (start record but no termination record existed)",
                wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier()));
        }
        wfExeStartTimestamps.put(wfExeCtx.getExecutionIdentifier(), Long.valueOf(System.currentTimeMillis()));
        StatsCounter.count(CAT_WF_EXEC, KEY_TOTAL);
        int compCount = wfExeCtx.getWorkflowDescription().getWorkflowNodes().size();
        if (compCount <= 5) {
            StatsCounter.count(CAT_WF_EXEC, KEY_00_05_COMPS);
        } else if (compCount <= 10) {
            StatsCounter.count(CAT_WF_EXEC, KEY_06_10_COMPS);
        } else if (compCount <= TWENTY) {
            StatsCounter.count(CAT_WF_EXEC, KEY_11_20_COMPS);
        } else if (compCount <= THIRTY) {
            StatsCounter.count(CAT_WF_EXEC, KEY_21_30_COMPS);
        } else if (compCount <= FIFTY) {
            StatsCounter.count(CAT_WF_EXEC, KEY_31_50_COMPS);
        } else {
            StatsCounter.count(CAT_WF_EXEC, KEY_MORE_THAN_50_COMPS);
        }

        boolean localOnly = true;
        for (WorkflowNode wn : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
            if (!platformService.matchesLocalInstance(wn.getComponentDescription().getNode())) {
                localOnly = false;
                break;
            }
        }
        // suggestion: tracking the actual number of involved instances/nodes may be interesting - misc_ro
        if (localOnly) {
            StatsCounter.count(CAT_WF_EXEC_DISTR, KEY_LOCAL_COMPS);
        } else {
            StatsCounter.count(CAT_WF_EXEC_DISTR, KEY_DISTR_COMPS);
        }

    }

    @Override
    public void addStatsAtWorkflowTermination(WorkflowExecutionContext wfExeCtx, WorkflowState finalWorkflowState) {
        if (wfExeStartTimestamps.containsKey(wfExeCtx.getExecutionIdentifier())) {
            String statsKey;
            int compCount = wfExeCtx.getWorkflowDescription().getWorkflowNodes().size();
            if (compCount <= 5) {
                statsKey = KEY_00_05_COMPS;
            } else if (compCount <= 10) {
                statsKey = KEY_06_10_COMPS;
            } else if (compCount <= TWENTY) {
                statsKey = KEY_11_20_COMPS;
            } else if (compCount <= THIRTY) {
                statsKey = KEY_21_30_COMPS;
            } else if (compCount <= FIFTY) {
                statsKey = KEY_31_50_COMPS;
            } else {
                statsKey = KEY_MORE_THAN_50_COMPS;
            }
            StatsCounter.registerValue(CAT_WF_EXEC_DURATION, statsKey, Math.abs((System.currentTimeMillis()
                - wfExeStartTimestamps.get(wfExeCtx.getExecutionIdentifier())) / THOUSAND));
            wfExeStartTimestamps.remove(wfExeCtx.getExecutionIdentifier());
        } else {
            LogFactory.getLog(getClass()).error(StringUtils.format("Failed to add stats about duration for workflow '%s' (%s)",
                wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier()));
        }
        StatsCounter.count(CAT_WF_EXEC_FINAL_STATES, finalWorkflowState.getDisplayName());
    }

    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

}
