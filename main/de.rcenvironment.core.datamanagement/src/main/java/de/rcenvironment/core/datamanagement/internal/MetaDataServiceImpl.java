/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation class of {@link MetaDataService}. Delegates all methods to {@link MetaDataBackendService}.
 * 
 * @author Jan Flink
 */
public class MetaDataServiceImpl implements MetaDataService {

    private MetaDataBackendService metaDataBackendService;

    @Override
    public Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId, String workflowDataManagementNodeId, Long starttime) {
        return metaDataBackendService.addWorkflowRun(workflowTitle, workflowControllerNodeId, workflowDataManagementNodeId, starttime);
    }

    @Override
    public Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances) {
        return metaDataBackendService.addComponentInstances(workflowRunId, componentInstances);
    }

    @Override
    public Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances) {
        return metaDataBackendService.addEndpointInstances(componentInstanceId, endpointInstances);
    }

    @Override
    @AllowRemoteAccess
    public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime) {
        return metaDataBackendService.addComponentRun(componentInstanceId, nodeId, count, starttime);
    }

    @Override
    @AllowRemoteAccess
    public void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count) {
        metaDataBackendService.addInputDatum(componentRunId, typedDatumId, endpointInstanceId, count);
    }

    @Override
    @AllowRemoteAccess
    public Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count) {
        return metaDataBackendService.addOutputDatum(componentRunId, endpointInstanceId, datum, count);
    }

    @Override
    public void addWorkflowRunProperties(Long workflowRunId, Map<String, String> properties) {
        metaDataBackendService.addWorkflowRunProperties(workflowRunId, properties);
    }

    @Override
    public void addComponentInstanceProperties(Long componentInstanceId, Map<String, String> properties) {
        metaDataBackendService.addComponentInstanceProperties(componentInstanceId, properties);
    }

    @Override
    @AllowRemoteAccess
    public void addComponentRunProperties(Long componentRunId, Map<String, String> properties) {
        metaDataBackendService.addComponentRunProperties(componentRunId, properties);
    }

    @Override
    @AllowRemoteAccess
    public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem) {
        metaDataBackendService.setOrUpdateHistoryDataItem(componentRunId, historyDataItem);
    }

    @Override
    public void setWorkflowRunFinished(Long workflowRunId, Long endtime, FinalWorkflowState finalState) {
        metaDataBackendService.setWorkflowRunFinished(workflowRunId, endtime, finalState);
    }

    @Override
    @AllowRemoteAccess
    public void setComponentRunFinished(Long componentRunId, Long endtime) {
        metaDataBackendService.setComponentRunFinished(componentRunId, endtime);
    }

    @Override
    @AllowRemoteAccess
    public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState) {
        metaDataBackendService.setComponentInstanceFinalState(componentInstanceId, finalState);
    }

    @Override
    @AllowRemoteAccess
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions() {
        return metaDataBackendService.getWorkflowRunDescriptions();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRun getWorkflowRun(Long workflowRunId) {
        return metaDataBackendService.getWorkflowRun(workflowRunId);
    }

    @Override
    public Collection<ComponentRun> getComponentRuns(Long componentInstanceId) {
        return metaDataBackendService.getComponentRuns(componentInstanceId);
    }

    @Override
    public Collection<EndpointData> getInputData(Long componentRunId) {
        return metaDataBackendService.getInputData(componentRunId);
    }

    @Override
    public Collection<EndpointData> getOutputData(Long componentRunId) {
        return metaDataBackendService.getOutputData(componentRunId);
    }

    @Override
    public Map<String, String> getWorkflowRunProperties(Long workflowRunId) {
        return metaDataBackendService.getWorkflowRunProperties(workflowRunId);
    }

    @Override
    public Map<String, String> getComponentRunProperties(Long componentRunId) {
        return metaDataBackendService.getComponentRunProperties(componentRunId);
    }

    @Override
    public void addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime) {
        metaDataBackendService.addTimelineInterval(workflowRunId, intervalType, starttime);
    }

    @Override
    public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId) {
        return metaDataBackendService.addTimelineInterval(workflowRunId, intervalType, starttime, relatedComponentId);
    }

    @Override
    public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime) {
        metaDataBackendService.setTimelineIntervalFinished(timelineIntervalId, endtime);
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRunTimline getWorkflowTimeline(Long workflowRunId) {
        return metaDataBackendService.getWorkflowTimeline(workflowRunId);
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRun(Long workflowRunId) {
        return metaDataBackendService.deleteWorkflowRun(workflowRunId);
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRunFiles(Long workflowRunId) {
        return metaDataBackendService.deleteWorkflowRunFiles(workflowRunId);
    }

    protected void bindMetaDataBackendService(MetaDataBackendService newMetaDataBackendService) {
        metaDataBackendService = newMetaDataBackendService;
    }

}
