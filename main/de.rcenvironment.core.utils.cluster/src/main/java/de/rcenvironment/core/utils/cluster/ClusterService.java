/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Provides runtime information about a cluster.
 * @author Doreen Seider
 */
public interface ClusterService {
    
    /** Period for continuously fetch cluster job information. */
    int FETCH_INTERVAL = 60000;
    
    /**
     * Fetches information about cluster jobs.
     * @return set of fetch {@link ClusterJobInformation}
     * @throws IOException if connecting to the host failed
     */
    Set<ClusterJobInformation> fetchClusterJobInformation() throws IOException;
    
    /**
     * Cancels cluster jobs.
     * @param jobIds identifiers of jobs to cancel
     * @return standard error of command.
     * @throws IOException if connecting to the host failed
     */
    String cancelClusterJobs(List<String> jobIds) throws IOException;
    
    /**
     * Adds a listener, which gets notified if state of a cluster job changed. The cluster job is identified
     * by the given job identifier.
     * @param jobId identifier of job
     * @param listener {@link ClusterJobStateChangeListener}
     */
    void addClusterJobStateChangeListener(String jobId, ClusterJobStateChangeListener listener);
}
