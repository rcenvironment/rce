/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * Describes sensitive part of cluster connection configuration.
 *
 * @author Doreen Seider
 */
@JsonDeserialize(as = SensitiveClusterConnectionConfigurationImpl.class)
public interface SensitiveClusterConnectionConfiguration {

    /**
     * @return representing key
     */
    String getKey();
    
    /**
     * @return password
     */
    String getPassword();
    
    /**
     * @param password password to set
     */
    void setPassword(String password);
}
