/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.cluster;

import java.util.Map;


/**
 * Creates, caches, and retrieves {@link ClusterService}s.
 * 
 * @author Doreen Seider
 */
public interface ClusterServiceManager {

    /**
     * Returns {@link ClusterService} which connects to the host via SSH. If no one was created for given system, host, port, and user a new
     * one will be created, otherwise the existing cached one will be returned.
     * 
     * @param system target queuing system.
     * @param pathsToQueuingSystemCommands paths to the queuing system commands on the server (optional; only needed if they are not known
     *        by the environment established via ssh); key is command, value is path
     * @param host target host
     * @param port target server
     * @param sshAuthUser given SSH user
     * @param sshAuthPhrase given SSH password
     * @return {@link ClusterService}
     */
    ClusterService retrieveSshBasedClusterService(ClusterQueuingSystem system, Map<String, String> pathsToQueuingSystemCommands,
        String host, int port, String sshAuthUser, String sshAuthPhrase);
    
}
