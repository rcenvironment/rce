/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.util.Set;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * A service that keeps track of reachable "workflow host" nodes, and publishes the local node's "workflow host" state.
 * 
 * @author Robert Mischke
 */
// FIXME temporary package location to simplify migration; move to "de.rcenvironment.core.component.workflow.api"
// when complete - misc_ro
public interface WorkflowHostService {

    // TODO (p3) >=8.0.0 improve JavaDoc for method variants (if both are kept)

    /**
     * Returns all reachable nodes (see {@link CommunicationService#getReachableNodes(boolean)) that have declared themselves to be a
     * "workflow host", ie those that declare themselves available to act as a workflow controller.
     * 
     * @return the set of all reachable workflow host nodes
     */
    Set<InstanceNodeSessionId> getWorkflowHostNodes();

    /**
     * Returns all reachable nodes (see {@link CommunicationService#getReachableNodes(boolean)) that have declared themselves to be a
     * "workflow host", ie those that declare themselves available to act as a workflow controller.
     * 
     * @return the set of all reachable workflow host nodes
     */
    Set<LogicalNodeId> getLogicalWorkflowHostNodes();

    /**
     * Convenience method that returns the result of {@link #getWorkflowHostNodes()}, plus the id of the local node if it is not already
     * present.
     * 
     * @return the merged set of all reachable workflow host nodes and the local node
     */
    Set<InstanceNodeSessionId> getWorkflowHostNodesAndSelf();

    /**
     * Convenience method that returns the result of {@link #getWorkflowHostNodes()}, plus the id of the local node if it is not already
     * present.
     * 
     * @return the merged set of all reachable workflow host nodes and the local node
     */
    Set<LogicalNodeId> getLogicalWorkflowHostNodesAndSelf();

}
