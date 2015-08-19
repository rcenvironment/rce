/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.view.internal;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;
import de.rcenvironment.core.utils.cluster.ClusterService;


/**
 * Model holding job information entries and connected cluster items.
 *
 * @author Doreen Seider
 */
public class ClusterJobInformationModel {
    
    protected static final String NOT_CONNECTED = Messages.notConnectedSelection;

    private static ClusterJobInformationModel instance = null;
    
    private Map<String, Set<ClusterJobInformation>> clusterJobInformationMap = new HashMap<String, Set<ClusterJobInformation>>();
    
    private Map<String, ClusterConnectionInformation> connectionInformationMap = new HashMap<String, ClusterConnectionInformation>();
    
    private Map<String, ClusterService> connectedClustersMap = new HashMap<String, ClusterService>();
    
    private String selectedConnectedConfigurationName = NOT_CONNECTED;
    
    protected static synchronized ClusterJobInformationModel getInstance() {
        if (null == instance) {
            instance = new ClusterJobInformationModel();
        }
        return instance;
    }
    
    protected synchronized Set<ClusterJobInformation> getClusterJobInformation() {
        if (selectedConnectedConfigurationName == null) {
            throw new IllegalArgumentException("no cluster set as selected");
        } else if (selectedConnectedConfigurationName.equals(NOT_CONNECTED)) {
            return new HashSet<ClusterJobInformation>();
        } else {
            return clusterJobInformationMap.get(selectedConnectedConfigurationName);            
        }
    }
    
    protected synchronized void setSelectedConnectedConfigurationName(String configurationName) {
        this.selectedConnectedConfigurationName = configurationName;
    }
    
    protected synchronized void addClusterConnectionInformation(String configurationName,
        ClusterConnectionInformation connectionInformation) {
        connectionInformationMap.put(configurationName, connectionInformation);
    }
    
    protected synchronized ClusterConnectionInformation getClusterConnectionInformation(String configurationName) {
        if (!connectionInformationMap.containsKey(configurationName)) {
            throw new IllegalArgumentException("no cluster connection information available for: " + configurationName);
        }
        return connectionInformationMap.get(configurationName);
    }
    
    protected synchronized String[] getConnectedConfigurationNames() {
        if (connectedClustersMap.isEmpty()) {
            return new String[] { NOT_CONNECTED };
        } else {
            return connectedClustersMap.keySet().toArray(new String[connectedClustersMap.size()]);            
        }
    }
        
    protected synchronized void getUpdateFromCluster() throws IOException {
        if (selectedConnectedConfigurationName == null) {
            throw new IllegalArgumentException("no cluster set as selected");
        } else if (!selectedConnectedConfigurationName.equals(NOT_CONNECTED)) {
            clusterJobInformationMap.put(selectedConnectedConfigurationName,
                connectedClustersMap.get(selectedConnectedConfigurationName).fetchClusterJobInformation());        
            connectionInformationMap.get(selectedConnectedConfigurationName).setLastUpdate(new Date());
        }
    }
    
    protected synchronized void addConnectedCluster(String configurationName,
        ClusterService jobInformationService) {
        connectedClustersMap.put(configurationName, jobInformationService);
    }
    
    protected synchronized void removeConnectedCluster(String configurationName) {
        connectedClustersMap.remove(configurationName);
        connectionInformationMap.remove(configurationName);
    }
    
    // TODO not really part of the model. should be extracted in extra class. for now added it here
    // to keep it simple -seid_do
    protected ClusterService getClusterInformationService() {
        return connectedClustersMap.get(selectedConnectedConfigurationName);
    }
}
