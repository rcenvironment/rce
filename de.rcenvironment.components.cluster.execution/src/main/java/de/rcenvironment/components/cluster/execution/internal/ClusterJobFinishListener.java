/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.cluster.execution.internal;

import java.util.concurrent.BlockingQueue;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.utils.cluster.ClusterJobInformation.ClusterJobState;
import de.rcenvironment.core.utils.cluster.ClusterJobStateChangeListener;

/**
 * Listens for cluster job states (Completed and Unknown) to finish component run.
 * 
 * @author Doreen Seider
 */
public class ClusterJobFinishListener implements ClusterJobStateChangeListener {

    private final BlockingQueue<String> synchronousQueue;
    
    public ClusterJobFinishListener(BlockingQueue<String> synchronousQueue) {
        this.synchronousQueue = synchronousQueue;
    }
    
    @Override
    public boolean onClusterJobStateChanged(ClusterJobState state) {
        boolean continueListening = true;
        if (state == null) {
            try {
                synchronousQueue.put(ClusterComponentConstants.CLUSTER_FETCHING_FAILED);
            } catch (InterruptedException e) {
                throw new RuntimeException("Waiting for job become completed failed", e);
            }
            continueListening = false;
        } else if (state.equals(ClusterJobState.Completed) || state.equals(ClusterJobState.Unknown)) {
            try {
                synchronousQueue.put(state.name());
            } catch (InterruptedException e) {
                throw new RuntimeException("Waiting for job become completed failed", e);
            }
            continueListening = false;
        }
        return continueListening;
    }

}
