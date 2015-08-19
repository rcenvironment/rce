/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;

/**
 * Reports changes to the set of reachable nodes that have declared themselves to be "workflow hosts".
 * 
 * @author Robert Mischke
 */
// FIXME temporary package location to simplify migration; move to "de.rcenvironment.core.component.workflow.spi"
// when complete - misc_ro
public interface WorkflowHostSetListener {

    /**
     * Reports a change to the set of reachable "workflow host" nodes. As a convenience, the difference to the previous state is provided as
     * well.
     * 
     * Typically, new listeners will receive a custom {@link #onReachableWorkflowHostsChanged()} callback on subscription that allows them
     * to easily initialize to the current observed state. From the listener's perspective, the call will contain parameters as if the last
     * observed set of nodes was empty, and all current nodes just became available.
     * 
     * Note that this listener interface is intentionally similar to {@link NetworkTopologyChangeListener}, as their behaviour is almost the
     * same, except that the sets reported by this listener have filtering applied to them.
     * 
     * @param reachableWfHosts the new set of reachable workflow host nodes
     * @param addedWfHosts workflow host nodes that have been added by this change
     * @param removedWfHosts workflow host nodes that have been removed by this change
     */
    void onReachableWorkflowHostsChanged(Set<NodeIdentifier> reachableWfHosts, Set<NodeIdentifier> addedWfHosts,
        Set<NodeIdentifier> removedWfHosts);
}
