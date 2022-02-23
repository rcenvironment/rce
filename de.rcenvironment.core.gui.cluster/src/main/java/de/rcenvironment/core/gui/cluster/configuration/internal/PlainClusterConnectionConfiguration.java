/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.rcenvironment.core.utils.cluster.ClusterQueuingSystem;


/**
 * Describes plain text part of cluster connection configuration.
 *
 * @author Doreen Seider
 */
@JsonDeserialize(as = PlainClusterConnectionConfigurationImpl.class)
public interface PlainClusterConnectionConfiguration {

    /**
     * @return remote cluster queuing system
     */
    ClusterQueuingSystem getClusterQueuingSystem();
    
    /**
     * @return path to cluster queuing system commands. Will be empty, if they are known by the environment established via ssh). Key is
     *         command, value is path.
     */
    Map<String, String> getPathToClusterQueuingSystemCommands();
    
    /**
     * @return host
     */
    String getHost();

    /**
     * @return port
     */
    int getPort();

    /**
     * @return user name
     */
    String getUsername();

    /**
     * @return configuration name
     */
    String getConfigurationName();
    
}
