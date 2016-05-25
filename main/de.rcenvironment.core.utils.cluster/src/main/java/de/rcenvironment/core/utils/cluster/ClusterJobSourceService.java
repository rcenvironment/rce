/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster;

import java.io.Serializable;
import java.util.Map;

/**
 * Manages information about job submission sources.
 * @author Doreen Seider
 */
public interface ClusterJobSourceService extends Serializable {

    /**
     * Adds source information about job.
     * 
     * @param system target queuing system.
     * @param host target host
     * @param port target server
     * @param jobId identifier of job
     * @param submissionSource {@link String} representation of submission source
     */
    void addSourceInformation(ClusterQueuingSystem system, String host, int port, String jobId, String submissionSource);
    
    /**
     * Removes source information for given job id.
     * @param system target queuing system.
     * @param host target host
     * @param port target server
     * @param jobId identifier of job
     */
    void removeSourceInformation(ClusterQueuingSystem system, String host, int port, String jobId);
    
    /**
     * @param system target queuing system.
     * @param host target host
     * @param port target server
     * @return all stored source information.
     */
    // Integer instead of int because of remote call
    Map<String, String> getSourceInformation(ClusterQueuingSystem system, String host, Integer port);
    
}
