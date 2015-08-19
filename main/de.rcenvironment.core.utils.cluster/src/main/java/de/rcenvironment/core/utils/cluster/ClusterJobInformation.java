/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.cluster;

/**
 * Holds information about a cluster job.
 * 
 * @author Doreen Seider
 */
public interface ClusterJobInformation {

    /** Default port for ssh connections. */
    int DEFAULT_SSH_PORT = 22;
    
    /**
     * Possible cluster job states.
     * @author Doreen Seider
     */
    enum ClusterJobState {
        Completed,
        Exiting,
        Held,
        Queued,
        Running,
        Moved,
        Waiting,
        Suspended,
        DepWait,
        Unsched,
        Unknown
    }
    
    /**
     * @return id of the job
     */
    String getJobId();

    /**
     * @return user running job
     */
    String getUser();
    
    /**
     * @return queue job belongs to
     */
    String getQueue();
    
    /**
     * @return remaining time
     */
    String getRemainingTime();

    /**
     * @return start time
     */
    String getStartTime();

    /**
     * @return queue time
     */
    String getQueueTime();
    
    /**
     * @return name of the job
     */
    String getJobName();

    /**
     * @return state of the job
     */
    ClusterJobState getJobState();
    
}
