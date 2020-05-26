/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

/**
 * Class to store the Status and installation of an instance.
 * 
 * @author Lukas Rosenbach
 */
public class InstanceStatus{
    
    /**
     * Enum for the Status of an instance.
     */
    public enum InstanceState {
        /**
         * Instance is not running.
         */
        NOTRUNNING,
        /**
         * Instance is starting.
         */
        STARTING,
        /**
         * Instance is running.
         */
        RUNNING;
    }
    
    private String installation;
    private InstanceState instanceState;
    
    public InstanceStatus(String installation, InstanceState instanceState) {
        setInstallation(installation);
        setInstanceState(instanceState);
    }
    
    public synchronized String getInstallation() {
        return installation;
    }
    
    public synchronized InstanceState getInstanceState() {
        return instanceState;
    }
    
    public synchronized void setInstallation(String installation) {
        this.installation = installation;
    }
    
    public synchronized void setInstanceState(InstanceState instanceState) {
        this.instanceState = instanceState;
    }
}
