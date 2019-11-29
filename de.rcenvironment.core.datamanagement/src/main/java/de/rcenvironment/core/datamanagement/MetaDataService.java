/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement;

import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.TimelineInterval;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Distributed service of the {@link RemotableMetaDataService}.
 * 
 * @author Doreen Seider
 * @author Jan Flink
 */
public interface MetaDataService {

    /**
     * Adds a {@link ComponentRun} with the given initial parameters to the database.
     * 
     * @param componentInstanceId The identifier of the related component instance.
     * @param nodeId The identifier of the node the component runs on.
     * @param count The current run count.
     * @param starttime The startime of the {@link ComponentRun}.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node.
     * @return The component run database identifier.
     * @throws CommunicationException in case of communication error
     */
    Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Links a {@link TypedDatum} as input datum to an {@link EndpointInstance} of a {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param typedDatumId The identifier of the {@link TypedDatum}.
     * @param endpointInstanceId The identifier of the {@link EndpointInstance}.
     * @param count The counter representing the order of incoming values.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node.
     * @throws CommunicationException in case of communication error
     */
    void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Adds an {@link TypedDatum} as output datum to an {@link EndpointInstance} of a {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param endpointInstanceId The identifier of the {@link EndpointInstance}.
     * @param datum The serialized {@link TypedDatum} to add.
     * @param count The counter representing the order of outgoing values.
     * @return The database identifier of the {@link TypedDatum}.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node.
     * @throws CommunicationException in case of communication error
     */
    Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Adds properties to a {@link ComponentRun}. Properties a represented in a key-value map.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param properties The key-value map.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node.
     * @throws CommunicationException in case of communication error
     */
    void addComponentRunProperties(Long componentRunId, Map<String, String> properties,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Adds a {@link TimelineInterval} to the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param intervalType The type of the interval.
     * @param starttime The start time of the interval.
     * @param relatedComponentId The database id of the related component.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node.
     * @return The database identifier of the {@link TimelineInterval}.
     * @throws CommunicationException in case of communication error
     */
    Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Sets the endtime of the {@link TimelineInterval} with the given identifier.
     * 
     * @param timelineIntervalId The identifier of the {@link TimelineInterval}.
     * @param endtime The end time of the interval.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node.
     * @throws CommunicationException in case of communication error
     */
    void setTimelineIntervalFinished(Long timelineIntervalId, long endtime,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Sets or updates the string representation of a history data item of an {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param historyDataItem The string representation of the history data item.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node
     * @throws CommunicationException in case of communication error
     */
    void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Sets the final state and the end time of a {@link ComponentRun}.
     * 
     * @param componentRunId The identifier of the {@link ComponentRun}.
     * @param endtime The end time.
     * @param finalState the final state of the run.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node
     * @throws CommunicationException in case of communication error
     */
    void setComponentRunFinished(Long componentRunId, Long endtime, FinalComponentRunState finalState,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Sets the final state a {@link ComponentInstance}.
     * 
     * @param componentInstanceId The identifier of the {@link ComponentInstance}.
     * @param finalState The final {@link FinalComponentState}.
     * @param storageNodeId {@link NetworkDestination} of the workflow storage node
     * @throws CommunicationException in case of communication error
     */
    void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState,
        NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Gets a collection of all {@link WorkflowRunDescription}s in the database.
     * 
     * @return A collection of {@link WorkflowRunDescription}s.
     * @throws CommunicationException in case of communication error
     */
    Set<WorkflowRunDescription> getWorkflowRunDescriptions() throws CommunicationException;

    /**
     * Gets the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRunDescription}.
     * @param storageNodeId any node id referring to the workflow storage node; if necessary, it will be attempted to resolve this to a more
     *        specific id
     * @return The {@link WorkflowRunDescription}.
     * @throws CommunicationException in case of communication error
     */
    WorkflowRun getWorkflowRun(Long workflowRunId, NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Gets the {@link WorkflowRunTimline} related to the {@link WorkflowRun} with the given identifier.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun}.
     * @param storageNodeId any node id referring to the workflow storage node; if necessary, it will be attempted to resolve this to a more
     *        specific id
     * @return The {@link WorkflowRunTimline}.
     * @throws CommunicationException in case of communication error
     */
    WorkflowRunTimline getWorkflowTimeline(Long workflowRunId, NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Deletes a {@link WorkflowRun} and all related content in the database and the blob store.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun} to be deleted.
     * @param storageNodeId any node id referring to the workflow storage node; if necessary, it will be attempted to resolve this to a more
     *        specific id
     * @return True if successfully deleted, false otherwise.
     * @throws CommunicationException in case of communication error
     */
    Boolean deleteWorkflowRun(Long workflowRunId, NetworkDestination storageNodeId) throws CommunicationException;

    /**
     * Deletes all {@link DataReference}s of a {@link WorkflowRun} in the database and the blob store.
     * 
     * @param workflowRunId The identifier of the {@link WorkflowRun} to be deleted.
     * @param storageNodeId any node id referring to the workflow storage node; if necessary, it will be attempted to resolve this to a more
     *        specific id
     * @return True if successfully deleted, false otherwise.
     * @throws CommunicationException in case of communication error
     */
    Boolean deleteWorkflowRunFiles(Long workflowRunId, NetworkDestination storageNodeId) throws CommunicationException;
}
