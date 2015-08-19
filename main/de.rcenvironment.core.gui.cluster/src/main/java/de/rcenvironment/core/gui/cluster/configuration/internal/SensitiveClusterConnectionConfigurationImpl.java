/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;


/**
 * Implementation of {@link SensitiveClusterConnectionConfiguration}.
 *
 * @author Doreen Seider
 */
public class SensitiveClusterConnectionConfigurationImpl implements SensitiveClusterConnectionConfiguration {

    private String password;
    
    private String key;

    /**
     * Should only be used by JSON object mapper.
     */
    @Deprecated
    public SensitiveClusterConnectionConfigurationImpl() {}
    
    public SensitiveClusterConnectionConfigurationImpl(String password) {
        this.password = password;
    }
    
    @Override
    public String getKey() {
        return key;
    }
    
    @Override
    public String getPassword() {
        return password;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
    @Override
    public void setPassword(String password) {
        this.password = password;
    }
    
}
