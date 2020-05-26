/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link ComponentExecutionStatsService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor tweaks)
 */
public class ComponentExecutionStatsServiceImpl implements ComponentExecutionStatsService {
    
    private static final int THOUSAND = 1000;

    private static final String CAT_COMP_EXEC = "Workflow Component Execution: Component runs";

    private static final String CAT_COMP_EXEC_USAGE_PER_WF = "Workflow Component Execution: Usage count by workflows";

    private static final String CAT_COMP_EXEC_DURATION = "Workflow Component Execution: Duration [sec]";

    private static final String CAT_COMP_EXEC_CTRL = "Workflow Component Execution: Location of workflow controller";

    private static final String CAT_COMP_EXEC_FINAL_STATES = "Workflow Component Execution: Final states";

    // private static final String KEY_TOTAL = "[total]";

    private static final String KEY_LOCAL_WF_CTRL = "[local]";

    private PlatformService platformService;

    private Map<String, Long> compExeStartTimestamps = Collections.synchronizedMap(new HashMap<String, Long>());

    @Override
    public void addStatsAtComponentStart(ComponentExecutionContext compExeCtx) {
        // StatsCounter.count(CAT_COMP_EXEC_USAGE_PER_WF, KEY_TOTAL);
        StatsCounter.count(CAT_COMP_EXEC_USAGE_PER_WF, compExeCtx.getComponentDescription().getName());
        if (platformService.matchesLocalInstance(compExeCtx.getWorkflowNodeId())) {
            StatsCounter.count(CAT_COMP_EXEC_CTRL, KEY_LOCAL_WF_CTRL);
        } else {
            // note: there should be no statistics categories that can grow indefinitely; leaving in for now - misc_ro
            // suggestion: isn't the *initiator* of the workflow even more interesting?
            StatsCounter.count(CAT_COMP_EXEC_CTRL, compExeCtx.getWorkflowNodeId().toString());
            // StatsCounter.count(CAT_COMP_EXEC_CTRL, KEY_REMOTE_WF_CTRL);
        }
    }

    @Override
    public void addStatsAtComponentRunStart(ComponentExecutionContext compExeCtx) {
        if (compExeStartTimestamps.containsKey(compExeCtx.getExecutionIdentifier())) {
            LogFactory.getLog(getClass()).error(StringUtils.format("Stats about duration for component '%s' (%s) not recorded properly: "
                + "last time (start record but no termination record existed)",
                compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier()));
        }
        compExeStartTimestamps.put(compExeCtx.getExecutionIdentifier(), Long.valueOf(System.currentTimeMillis()));
        // StatsCounter.count(CAT_COMP_EXEC, KEY_TOTAL);
        StatsCounter.count(CAT_COMP_EXEC, compExeCtx.getComponentDescription().getName());
    }

    @Override
    public void addStatsAtComponentRunTermination(ComponentExecutionContext compExeCtx) {
        if (compExeStartTimestamps.containsKey(compExeCtx.getExecutionIdentifier())) {
            StatsCounter.registerValue(CAT_COMP_EXEC_DURATION, compExeCtx.getComponentDescription().getName(),
                Math.abs((System.currentTimeMillis()
                    - compExeStartTimestamps.get(compExeCtx.getExecutionIdentifier())) / THOUSAND));
            compExeStartTimestamps.remove(compExeCtx.getExecutionIdentifier());
        } else {
            LogFactory.getLog(getClass()).error(StringUtils.format("Failed to add stats about duration for component '%s' (%s)",
                compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier()));
        }
    }

    @Override
    public void addStatsAtComponentTermination(ComponentExecutionContext compExeCtx, ComponentState finalCompState) {
        StatsCounter.count(CAT_COMP_EXEC_FINAL_STATES, finalCompState.getDisplayName());
    }

    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }
}
