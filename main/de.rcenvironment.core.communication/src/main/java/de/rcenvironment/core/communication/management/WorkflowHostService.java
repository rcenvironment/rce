/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * A service that keeps track of reachable "workflow host" nodes, and publishes the local node's "workflow host" state.
 * 
 * @author Robert Mischke
 */
// FIXME temporary package location to simplify migration; move to "de.rcenvironment.core.component.workflow.api"
// when complete - misc_ro
public interface WorkflowHostService {

/** 
     * Returns all reachable nodes (see {@link CommunicationService#getReachableNodes(boolean)) 
     * that have declared themselves to be a "workflow host", ie those that declare themselves 
     * available to act as a workflow controller. 
     * 
     * @return the set of all reachable workflow host nodes
     */
    Set<NodeIdentifier> getWorkflowHostNodes();

    /**
     * Convenience method that returns the result of {@link #getWorkflowHostNodes()}, plus the id of the local node if it is not already
     * present.
     * 
     * @return the merged set of all reachable workflow host nodes and the local node
     */
    Set<NodeIdentifier> getWorkflowHostNodesAndSelf();

}
