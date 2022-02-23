/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
