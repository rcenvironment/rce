/*
 * Copyright (C) 2006-2015 DLR, Germany
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

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Bridge class to the data management, holding relevant data management ids.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionStorageBridge {

    private final MetaDataService metaDataService;
    
    private final String errorMessageSuffix;

    private Long workflowDmId;

    private Map<String, Long> compInstDmIds;

    private Map<String, Map<String, Long>> inputDmIds = new HashMap<>();

    private Map<String, Map<String, Long>> outputDmIds = new HashMap<>();
    
    private Map<String, Long> intervalTypeDmIds = Collections.synchronizedMap(new HashMap<String, Long>());
    
    public WorkflowExecutionStorageBridge(WorkflowExecutionContext wfExeCtx, MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
        errorMessageSuffix = StringUtils.format(" of workflow '%s' (%s)", wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier());
    }

    protected Long addWorkflowExecution(WorkflowExecutionContext wfExeCtx) throws WorkflowExecutionException {
        try {
            workflowDmId = metaDataService.addWorkflowRun(wfExeCtx.getInstanceName(), wfExeCtx.getNodeId().getIdString(),
                wfExeCtx.getDefaultStorageNodeId().getIdString(), System.currentTimeMillis());
        } catch (RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store workflow execution" + errorMessageSuffix, e);
        }
        Map<String, Set<EndpointInstance>> compInputInstances = new HashMap<>();
        Map<String, Set<EndpointInstance>> compOutputInstances = new HashMap<>();
        Set<ComponentInstance> componentInstances = new HashSet<>();
        for (WorkflowNode wn : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
            Set<EndpointInstance> endpointInstances = new HashSet<>();
            String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wn.getIdentifier());
            componentInstances.add(new ComponentInstance(compExeId, wn.getComponentDescription().getIdentifier(), wn.getName(), null));
            for (EndpointDescription ep : wn.getComponentDescription().getInputDescriptionsManager().getEndpointDescriptions()) {
                endpointInstances.add(new EndpointInstance(ep.getName(), EndpointType.INPUT));
            }
            compInputInstances.put(compExeId, endpointInstances);
            endpointInstances = new HashSet<>();
            for (EndpointDescription ep : wn.getComponentDescription().getOutputDescriptionsManager().getEndpointDescriptions()) {
                endpointInstances.add(new EndpointInstance(ep.getName(), EndpointType.OUTPUT));
            }
            compOutputInstances.put(compExeId, endpointInstances);
        }
        try {
            compInstDmIds = metaDataService.addComponentInstances(workflowDmId, componentInstances);
        } catch (RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store component instances" + errorMessageSuffix, e);
        }
        for (String dmId : compInputInstances.keySet()) {
            try {
                inputDmIds.put(dmId, metaDataService.addEndpointInstances(compInstDmIds.get(dmId),
                    compInputInstances.get(dmId)));
            } catch (RuntimeException e) {
                throw new WorkflowExecutionException("Failed to store component input instances" + errorMessageSuffix, e);
            }
        }
        for (String compExeId : compOutputInstances.keySet()) {
            try {
                outputDmIds.put(compExeId, metaDataService.addEndpointInstances(compInstDmIds.get(compExeId),
                    compOutputInstances.get(compExeId)));
            } catch (RuntimeException e) {
                throw new WorkflowExecutionException("Failed to store component output instances" + errorMessageSuffix, e);
            }
        }
        return workflowDmId;
    }
    
    protected void setWorkflowExecutionFinished(FinalWorkflowState finalState) throws WorkflowExecutionException {
        try {
            metaDataService.setWorkflowRunFinished(workflowDmId, System.currentTimeMillis(), finalState);
        } catch (RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store final state" + errorMessageSuffix, e);
        }
        if (!intervalTypeDmIds.isEmpty()) {
            LogFactory.getLog(WorkflowExecutionStorageBridge.class).warn("Timeline interval ids left, "
                + "which were not used for setting timeline interval to finished: " + intervalTypeDmIds);
        }
    }
    
    protected void addComponentTimelineInterval(TimelineIntervalType intervalType, long startTime, String compRunDmId)
        throws WorkflowExecutionException {
        synchronized (intervalTypeDmIds) {
            if (intervalTypeDmIds.containsKey(createTimelineIntervalMapKey(intervalType, compRunDmId))) {
                throw new WorkflowExecutionException("Timeline interval already written within this component run: " + intervalTypeDmIds);
            }
        }
        Long intervalTypeDmId;
        try {
            intervalTypeDmId = metaDataService.addTimelineInterval(workflowDmId, intervalType, startTime, Long.valueOf(compRunDmId));
        } catch (RuntimeException e) {
            throw new WorkflowExecutionException("Failed to store start of timeline interval" + errorMessageSuffix, e);
        }
        synchronized (intervalTypeDmIds) {
            intervalTypeDmIds.put(createTimelineIntervalMapKey(intervalType, compRunDmId), intervalTypeDmId);
        }
    }
    
    protected void setComponentTimelineIntervalFinished(TimelineIntervalType intervalType, long endTime, String compRunDmId)
        throws WorkflowExecutionException {
        synchronized (intervalTypeDmIds) {
            Long dmId = intervalTypeDmIds.remove(createTimelineIntervalMapKey(intervalType, compRunDmId));
            if (dmId != null) {
                try {
                    metaDataService.setTimelineIntervalFinished(dmId, endTime);
                } catch (RuntimeException e) {
                    throw new WorkflowExecutionException("Failed to store end of timeline interval" + errorMessageSuffix, e);
                }
            } else {
                throw new WorkflowExecutionException(StringUtils.format("Failed to store end of timeline interval '%s' for component '%s'"
                    + " as no valid dm id exists", intervalType.name(), compRunDmId));
            }
        }
    }
    
    private String createTimelineIntervalMapKey(TimelineIntervalType intervalType, String compRunDmId) {
        return StringUtils.escapeAndConcat(compRunDmId, intervalType.name());
    }

    protected Long getWorkflowInstanceDataManamagementId() {
        return workflowDmId;
    }
    
    protected Long getComponentInstanceDataManamagementId(String compExecutionIdentifier) {
        return compInstDmIds.get(compExecutionIdentifier);
    }

    protected Map<String, Long> getInputInstanceDataManamagementIds(String compExecutionIdentifier) {
        return inputDmIds.get(compExecutionIdentifier);
    }

    protected Map<String, Long> getOutputInstanceDataManamagementIds(String compExecutionIdentifier) {
        return outputDmIds.get(compExecutionIdentifier);
    }

}
