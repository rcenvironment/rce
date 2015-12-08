/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.TimelineInterval;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Interface for the meta data service support.
 * 
 * @author Jan Flink
 */
@RemotableService
public interface RemotableMetaDataService {

    /**
     * Adds a new {@link WorkflowRun} with the given initial parameters to the database.
     * 
     * @param workflowTitle The title of the workflow.
     * @param workflowControllerNodeId The workflow controller node identifier.
     * @param workflowDataManagementNodeId The workflow data management node identifier.
     * @param starttime The startime of the workflow run.
     * @return The database identifier of the {@link WorkflowRun}.
     * @throws RemoteOperationException standard remote operation exception
     */
    Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId, String workflowDataManagementNodeId, Long starttime)
        throws RemoteOperationException;

    /**
     * Adds a reference to the workflow file to a {@link WorkflowRun}.
     * 
     * @param workflowRunId The Id of the workflow to modify
     * @param wfFileReference Reference to the workflow file.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addWorkflowFileToWorkflowRun(Long workflowRunId, String wfFileReference) throws RemoteOperationException;

    /**
     * Adds {@link ComponentInstance}s to a {@link WorkflowRun}.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param componentInstances The collection of {@link ComponentInstance}s.
     * @return The map holding the relation between components execution identifier (String) and the database identifier (Long) of the
     *         {@link ComponentInstance}s.
     * @throws RemoteOperationException standard remote operation exception
     */
    Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances)
        throws RemoteOperationException;

    /**
     * Adds {@link EndpointInstance}s to a {@link ComponentInstance}.
     * 
     * @param componentInstanceId The identifier of the {@link ComponentInstance}.
     * @param endpointInstances The collection of {@link EndpointInstance}s.
     * @return The map holding the relation between endpoint execution identifier (String) and the database identifier (Long) of the
     *         {@link ComponentInstance}s.
     * @throws RemoteOperationException standard remote operation exception
     */
    Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances)
        throws RemoteOperationException;

    /**
     * Adds a {@link ComponentRun} with the given initial parameters to the database.
     * 
     * @param componentInstanceId The identifier of the related {@link ComponentInstance}.
     * @param nodeId The identifier of the node the component runs on.
     * @param count The current run count.
     * @param starttime The start time of the {@link ComponentRun}.
     * @return The component run database identifier.
     * @throws RemoteOperationException standard remote operation exception
     */
    Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime) throws RemoteOperationException;

    /**
     * Links a {@link TypedDatum} as input datum to an {@link EndpointInstance} of a {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param typedDatumId The identifier of the {@link TypedDatum}.
     * @param endpointInstanceId The identifier of the {@link EndpointInstance}.
     * @param count The counter representing the order of incoming values.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count) throws RemoteOperationException;

    /**
     * Adds an {@link TypedDatum} as output datum to an {@link EndpointInstance} of a {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param endpointInstanceId The identifier of the {@link EndpointInstance}.
     * @param datum The serialized {@link TypedDatum} to add.
     * @param count The counter representing the order of outgoing values.
     * @return The database identifier of the {@link TypedDatum}.
     * @throws RemoteOperationException standard remote operation exception
     */
    Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count) throws RemoteOperationException;

    /**
     * Adds properties to a {@link WorkflowRun}. Properties a represented in a key-value map.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param properties The key-value map.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addWorkflowRunProperties(Long workflowRunId, Map<String, String> properties) throws RemoteOperationException;

    /**
     * Adds properties to a {@link ComponentInstance}. Properties a represented in a key-value map.
     * 
     * @param componentInstanceId The identifier of the {@link ComponentInstance}.
     * @param properties The key-value map.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addComponentInstanceProperties(Long componentInstanceId, Map<String, String> properties) throws RemoteOperationException;

    /**
     * Adds properties to a {@link ComponentInstance}. Properties a represented in a key-value map.
     * 
     * @param endpointInstanceId The identifier of the {@link ComponentInstance}.
     * @param properties The key-value map.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addEndpointInstanceProperties(Long endpointInstanceId, Map<String, String> properties) throws RemoteOperationException;

    /**
     * Adds properties to a {@link ComponentRun}. Properties a represented in a key-value map.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param properties The key-value map.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addComponentRunProperties(Long componentRunId, Map<String, String> properties) throws RemoteOperationException;

    /**
     * Sets or updates the string representation of a history data item of an {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param historyDataItem The string representation of the history data item.
     * @throws RemoteOperationException standard remote operation exception
     */
    void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem) throws RemoteOperationException;

    /**
     * Sets the final state and the end time of a {@link WorkflowRun}.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param endtime The end time.
     * @param finalState The final {@link FinalWorkflowState}.
     * @throws RemoteOperationException standard remote operation exception
     */
    void setWorkflowRunFinished(Long workflowRunId, Long endtime, FinalWorkflowState finalState) throws RemoteOperationException;

    /**
     * Sets the final state and the end time of a {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param endtime The end time.
     * @throws RemoteOperationException standard remote operation exception
     */
    void setComponentRunFinished(Long componentRunId, Long endtime) throws RemoteOperationException;

    /**
     * Sets the final state a {@link ComponentInstance}.
     * 
     * @param componentInstanceId The identifier of the {@link ComponentInstance}.
     * @param finalState The final {@link FinalComponentState}.
     * @throws RemoteOperationException standard remote operation exception
     */
    void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState) throws RemoteOperationException;

    /**
     * Gets a collection of all {@link WorkflowRun}s in the database ordered by timestamp descending.
     * 
     * @return A collection of {@link WorkflowRunDescription}s.
     * @throws RemoteOperationException standard remote operation exception
     */
    // TODO to make use of the ordered result, this should return List instead of Set - misc_ro
    Set<WorkflowRunDescription> getWorkflowRunDescriptions() throws RemoteOperationException;

    /**
     * Gets the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @return The {@link WorkflowRun}.
     * @throws RemoteOperationException standard remote operation exception
     */
    WorkflowRun getWorkflowRun(Long workflowRunId) throws RemoteOperationException;

    /**
     * Gets a collection of all {@link ComponentRun}s related to the {@link ComponentInstance} with the given identifier.
     * 
     * @param componentInstanceId The identifier of the {@link ComponentInstance}.
     * @return A collection of {@link ComponentRun}s.
     * @throws RemoteOperationException standard remote operation exception
     */
    Collection<ComponentRun> getComponentRuns(Long componentInstanceId) throws RemoteOperationException;

    /**
     * Gets a collection of all {@link EndpointData} items representing the input data of a {@link ComponentRun} with the given identifier.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @return The collection of {@link EndpointData} items.
     * @throws RemoteOperationException standard remote operation exception
     */
    Collection<EndpointData> getInputData(Long componentRunId) throws RemoteOperationException;

    /**
     * Gets a collection of all {@link EndpointData} items representing the output data of a {@link ComponentRun} with the given identifier.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @return The collection of {@link EndpointData} items.
     * @throws RemoteOperationException standard remote operation exception
     */
    Collection<EndpointData> getOutputData(Long componentRunId) throws RemoteOperationException;

    /**
     * Gets a map of key value entries representing properties of a {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @return The key-value map.
     * @throws RemoteOperationException standard remote operation exception
     */
    Map<String, String> getWorkflowRunProperties(Long workflowRunId) throws RemoteOperationException;

    /**
     * Gets a map of key value entries representing properties of a {@link ComponentRun} with the given identifier.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @return The key-value map.
     * @throws RemoteOperationException standard remote operation exception
     */
    Map<String, String> getComponentRunProperties(Long componentRunId) throws RemoteOperationException;

    /**
     * Adds a {@link TimelineInterval} to the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param intervalType The type of the interval.
     * @param starttime The start time of the interval.
     * @throws RemoteOperationException standard remote operation exception
     */
    void addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime) throws RemoteOperationException;

    /**
     * Adds a {@link TimelineInterval} to the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param intervalType The type of the interval.
     * @param starttime The start time of the interval.
     * @param relatedComponentId The database id of the related component.
     * @return The database identifier of the {@link TimelineInterval}.
     * @throws RemoteOperationException standard remote operation exception
     */
    Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId)
        throws RemoteOperationException;

    /**
     * Sets the endtime of the {@link TimelineInterval} with the given identifier.
     * 
     * @param timelineIntervalId The identifier of the {@link TimelineInterval}.
     * @param endtime The end time of the interval.
     * @throws RemoteOperationException standard remote operation exception
     */
    void setTimelineIntervalFinished(Long timelineIntervalId, long endtime) throws RemoteOperationException;

    /**
     * Gets the {@link WorkflowRunTimline} related to the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @return The {@link WorkflowRunTimline}.
     * @throws RemoteOperationException standard remote operation exception
     */
    WorkflowRunTimline getWorkflowTimeline(Long workflowRunId) throws RemoteOperationException;

    /**
     * Deletes a {@link WorkflowRun} and all related content in the database and the blob store.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun} to be deleted.
     * @return True if successfully deleted, false otherwise.
     * @throws RemoteOperationException standard remote operation exception
     */
    Boolean deleteWorkflowRun(Long workflowRunId) throws RemoteOperationException;

    /**
     * Deletes all {@link DataReference}s of a {@link WorkflowRun} in the database and the blob store.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun} to be deleted.
     * @return True if successfully deleted, false otherwise.
     * @throws RemoteOperationException standard remote operation exception
     */
    Boolean deleteWorkflowRunFiles(Long workflowRunId) throws RemoteOperationException;

    /**
     * Gets the {@link DataReference} for the given uuid from the meta data backend.
     * 
     * @param dataReferenceKey The key of the {@link DataReference} to return.
     * @return The {@link DataReference}.
     * @throws RemoteOperationException standard remote operation exception
     */
    DataReference getDataReference(String dataReferenceKey) throws RemoteOperationException;

}
