/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.model;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NetworkGraphWithProperties;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.gui.communication.views.NetworkView;
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;

/**
 * The complete model that the {@link NetworkView} is filled from.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class NetworkViewModel {

    /**
     * The reachable network graph, with no attached properties.
     */
    public NetworkGraph networkGraph;

    /**
     * The latest {@link DistributedComponentKnowledge} object.
     */
    public DistributedComponentKnowledge componentKnowledge;

    /**
     * The collection of connection setups.
     */
    public Collection<ConnectionSetup> connectionSetups;

    /**
     * The collected node's property maps; the inner maps must be immutable.
     */
    public Map<InstanceNodeSessionId, Map<String, String>> nodeProperties;

    /**
     * The merged {@link NetworkGraphWithProperties}, constructed from {@link #networkGraph} and {@link #nodeProperties}.
     */
    public NetworkGraphWithProperties networkGraphWithProperties;

    /**
     * The map of {@link FullSystemAndProcessDataSnapshot}.
     */
    public Map<InstanceNodeSessionId, FullSystemAndProcessDataSnapshot> monitoringDataModelMap;

    public NetworkViewModel(NetworkGraph networkGraph, DistributedComponentKnowledge componentKnowledge,
        Collection<ConnectionSetup> connectionSetups, Map<InstanceNodeSessionId, Map<String, String>> nodeProperties,
        Map<InstanceNodeSessionId, FullSystemAndProcessDataSnapshot> monitoringDataModelMap) {
        this.networkGraph = networkGraph;
        this.nodeProperties = nodeProperties;
        this.componentKnowledge = componentKnowledge;
        this.connectionSetups = connectionSetups;
        this.monitoringDataModelMap = monitoringDataModelMap;
        updateGraphWithProperties();
    }

    public NetworkGraph getNetworkGraphWithProperties() {
        return networkGraphWithProperties;
    }

    public DistributedComponentKnowledge getComponentKnowledge() {
        return componentKnowledge;
    }

    public Collection<ConnectionSetup> getConnectionSetups() {
        return connectionSetups;
    }

    public Map<InstanceNodeSessionId, Map<String, String>> getNodeProperties() {
        return nodeProperties;
    }
    
    public Map<InstanceNodeSessionId, FullSystemAndProcessDataSnapshot> getMonitoringDataModelMap() {
        return monitoringDataModelMap;
    }

    /**
     * Updates the merged {@link NetworkGraphWithProperties} from {@link #networkGraph} and {@link #nodeProperties}.
     */
    public void updateGraphWithProperties() {
        if (networkGraph != null) {
            networkGraphWithProperties = networkGraph.attachNodeProperties(nodeProperties);
        } else {
            networkGraphWithProperties = null;
        }
    }

}
