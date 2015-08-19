/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster;

/**
 * Base class for container classes ,which hold information about a specified cluster queuing request, e.g. a job submission.
 *
 * @author Doreen Seider
 */
public interface ClusterQueuingSystemRequestContext {

    /**
     * Create a command string to execute.
     * @return a command string to execute
     */
    String toCommandString();
}
