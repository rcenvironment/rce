/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.datamanagement.RemotableMetaDataService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation class of {@link RemotableMetaDataService}. Delegates all methods to {@link MetaDataBackendService}.
 * 
 * @author Jan Flink
 */
public class RemotableMetaDataServiceImpl implements RemotableMetaDataService {

    private MetaDataBackendService metaDataBackendService;

    @Override
    public Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId, String workflowDataManagementNodeId, Long starttime)
        throws RemoteOperationException {
        return metaDataBackendService.addWorkflowRun(workflowTitle, workflowControllerNodeId, workflowDataManagementNodeId, starttime);
    }

    @Override
    public Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances)
        throws RemoteOperationException {
        return metaDataBackendService.addComponentInstances(workflowRunId, componentInstances);
    }

    @Override
    public Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances)
        throws RemoteOperationException {
        return metaDataBackendService.addEndpointInstances(componentInstanceId, endpointInstances);
    }

    @Override
    @AllowRemoteAccess
    public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime) throws RemoteOperationException {
        return metaDataBackendService.addComponentRun(componentInstanceId, nodeId, count, starttime);
    }

    @Override
    @AllowRemoteAccess
    public void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count)
        throws RemoteOperationException {
        metaDataBackendService.addInputDatum(componentRunId, typedDatumId, endpointInstanceId, count);
    }

    @Override
    @AllowRemoteAccess
    public Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count) throws RemoteOperationException {
        return metaDataBackendService.addOutputDatum(componentRunId, endpointInstanceId, datum, count);
    }

    @Override
    public void addWorkflowRunProperties(Long workflowRunId, Map<String, String> properties) throws RemoteOperationException {
        metaDataBackendService.addWorkflowRunProperties(workflowRunId, properties);
    }

    @Override
    public void addComponentInstanceProperties(Long componentInstanceId, Map<String, String> properties) throws RemoteOperationException {
        metaDataBackendService.addComponentInstanceProperties(componentInstanceId, properties);
    }

    @Override
    public void addEndpointInstanceProperties(Long endpointInstanceId, Map<String, String> properties) throws RemoteOperationException {
        metaDataBackendService.addEndpointInstanceProperties(endpointInstanceId, properties);
    }

    @Override
    @AllowRemoteAccess
    public void addComponentRunProperties(Long componentRunId, Map<String, String> properties) throws RemoteOperationException {
        metaDataBackendService.addComponentRunProperties(componentRunId, properties);
    }

    @Override
    @AllowRemoteAccess
    public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem) throws RemoteOperationException {
        metaDataBackendService.setOrUpdateHistoryDataItem(componentRunId, historyDataItem);
    }

    @Override
    public void setWorkflowRunFinished(Long workflowRunId, Long endtime, FinalWorkflowState finalState) throws RemoteOperationException {
        metaDataBackendService.setWorkflowRunFinished(workflowRunId, endtime, finalState);
    }

    @Override
    @AllowRemoteAccess
    public void setComponentRunFinished(Long componentRunId, Long endtime, FinalComponentRunState finalState)
        throws RemoteOperationException {
        metaDataBackendService.setComponentRunFinished(componentRunId, endtime, finalState);
    }

    @Override
    @AllowRemoteAccess
    public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState) throws RemoteOperationException {
        metaDataBackendService.setComponentInstanceFinalState(componentInstanceId, finalState);
    }

    @Override
    @AllowRemoteAccess
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions() throws RemoteOperationException {
        return metaDataBackendService.getWorkflowRunDescriptions();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRun getWorkflowRun(Long workflowRunId) throws RemoteOperationException {
        return metaDataBackendService.getWorkflowRun(workflowRunId);
    }

    @Override
    public Collection<ComponentRun> getComponentRuns(Long componentInstanceId) throws RemoteOperationException {
        return metaDataBackendService.getComponentRuns(componentInstanceId);
    }

    @Override
    public Collection<EndpointData> getInputData(Long componentRunId) throws RemoteOperationException {
        return metaDataBackendService.getInputData(componentRunId);
    }

    @Override
    public Collection<EndpointData> getOutputData(Long componentRunId) throws RemoteOperationException {
        return metaDataBackendService.getOutputData(componentRunId);
    }

    @Override
    public Map<String, String> getWorkflowRunProperties(Long workflowRunId) throws RemoteOperationException {
        return metaDataBackendService.getWorkflowRunProperties(workflowRunId);
    }

    @Override
    public Map<String, String> getComponentRunProperties(Long componentRunId) throws RemoteOperationException {
        return metaDataBackendService.getComponentRunProperties(componentRunId);
    }

    @Override
    public void addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime) throws RemoteOperationException {
        metaDataBackendService.addTimelineInterval(workflowRunId, intervalType, starttime);
    }

    @Override
    public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId)
        throws RemoteOperationException {
        return metaDataBackendService.addTimelineInterval(workflowRunId, intervalType, starttime, relatedComponentId);
    }

    @Override
    public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime) throws RemoteOperationException {
        metaDataBackendService.setTimelineIntervalFinished(timelineIntervalId, endtime);
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRunTimline getWorkflowTimeline(Long workflowRunId) throws RemoteOperationException {
        return metaDataBackendService.getWorkflowTimeline(workflowRunId);
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRun(Long workflowRunId) throws RemoteOperationException {
        return metaDataBackendService.deleteWorkflowRun(workflowRunId);
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRunFiles(Long workflowRunId) throws RemoteOperationException {
        return metaDataBackendService.deleteWorkflowRunFiles(workflowRunId);
    }

    @Override
    @AllowRemoteAccess
    public DataReference getDataReference(String dataReferenceKey) throws RemoteOperationException {
        return metaDataBackendService.getDataReference(dataReferenceKey);
    }

    protected void bindMetaDataBackendService(MetaDataBackendService newMetaDataBackendService) {
        metaDataBackendService = newMetaDataBackendService;
    }

    @Override
    public void addWorkflowFileToWorkflowRun(Long workflowRunId, String wfFileReference) throws RemoteOperationException {
        metaDataBackendService.addWorkflowFileToWorkflowRun(workflowRunId, wfFileReference);
    }

}
