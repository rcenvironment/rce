/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import java.util.List;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Provides configuration parameters for a benchmark subtask. A subtask is a set of one or more
 * target nodes that should be benchmarked with the same parameters.
 * 
 * @author Robert Mischke
 */
public interface BenchmarkSubtask {

    /**
     * @return the list of target nodes to contact
     */
    List<NodeIdentifier> getTargetNodes();

    /**
     * @return the number of messages to send to each target node
     */
    int getNumMessages();

    /**
     * @return the size of each request payload, in bytes
     */
    int getRequestSize();

    /**
     * @return the size of each generated response payload, in bytes
     */
    int getResponseSize();

    /**
     * @return the delay to wait before generating the benchmark response, in msec
     */
    int getResponseDelay();

    /**
     * @return the number of parallel sender threads for each target node
     */
    int getThreadsPerTarget();

}
