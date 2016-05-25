/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster.internal;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;


/**
 * Implementation of {@link ClusterJobInformation} enhanced with setter methods to modify information by other classes within this bundle.
 * @author Doreen Seider
 */
public class ClusterJobInformationImpl implements ClusterJobInformation {

    /** Constant which is returned, if one of the optional fields are not set. */
    public static final String NO_VALUE_SET = "-";
    
    /** Constant which is returned, if one of the required fields are not set. */
    public static final String VALUE_NA = "n/a";

    private static final String EMPTY_SPACE = "   ";

    private String jobId = VALUE_NA;

    private String user = VALUE_NA;
    
    private String queue = VALUE_NA;
    
    private String jobName = VALUE_NA;

    private ClusterJobState jobState = ClusterJobState.Unknown;
    
    private ClusterJobTimesInformation clusterJobTimesInformation = new ClusterJobTimesInformation();
    
    @Override
    public String getJobId() {
        return jobId;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getQueue() {
        return queue;
    }
    
    @Override
    public String getRemainingTime() {
        return clusterJobTimesInformation.getRemainingTime();
    }
    
    @Override
    public String getStartTime() {
        return clusterJobTimesInformation.getStartTime();
    }
    
    @Override
    public String getQueueTime() {
        return clusterJobTimesInformation.getQueueTime();
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public ClusterJobState getJobState() {
        return jobState;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    public void setQueue(String queue) {
        this.queue = queue;
    }
    
    public void setClusterJobTimesInformation(ClusterJobTimesInformation clusterJobTimesInformation) {
        this.clusterJobTimesInformation = clusterJobTimesInformation;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setJobState(ClusterJobState jobState) {
        this.jobState = jobState;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Cluster job: ");
        builder.append(jobId);
        builder.append(EMPTY_SPACE);
        builder.append(user);
        builder.append(EMPTY_SPACE);
        builder.append(queue);
        builder.append(EMPTY_SPACE);
        builder.append(getRemainingTime());
        builder.append(EMPTY_SPACE);
        builder.append(getStartTime());
        builder.append(EMPTY_SPACE);
        builder.append(getQueueTime());
        builder.append(EMPTY_SPACE);
        builder.append(jobName);
        builder.append(EMPTY_SPACE);
        builder.append(jobState);
        return builder.toString();
    }

}
