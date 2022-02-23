/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;

/**
 * Objects implementing this interface can be added as listener for cluster job state changes
 * at the {@link ClusterService}.
 * @author Doreen Seider
 */
public interface ClusterJobStateChangeListener {

    /**
     * Called on cluster job state changes.
     * 
     * @param state new cluster job state
     * @return <code>true</code> if object wants to get notified furthermore, otherwise
     *         <code>false</code> (e.g. if desired job state was achieved).
     */
    boolean onClusterJobStateChanged(ClusterJobState state);
}
