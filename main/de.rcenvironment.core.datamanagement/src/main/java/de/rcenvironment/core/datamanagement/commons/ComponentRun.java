/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Identifier for a component run.
 * 
 * @author Jan Flink
 */
public class ComponentRun implements Serializable, Comparable<ComponentRun> {

    private static final long serialVersionUID = -366100876644355558L;

    private final Long componentInstanceID;

    private final Long componentRunID;

    private final String nodeID;

    private final Integer runCounter;

    private final Long startTime;

    private final Long endTime;

    private final String historyDataItem;

    private final Boolean referencesDeleted;

    private Set<EndpointData> endpointData;
    
    private final Map<String, String> metaData;

    public ComponentRun(Long componentRunID, Long componentInstanceID, String nodeID, Integer runCounter, Long startTime, Long endtime,
        String historyDataItem, Boolean referencesDeleted, Map<String, String> metaData) {
        this.componentRunID = componentRunID;
        this.componentInstanceID = componentInstanceID;
        this.nodeID = nodeID;
        this.runCounter = runCounter;
        this.startTime = startTime;
        this.endTime = endtime;
        this.historyDataItem = historyDataItem;
        this.referencesDeleted = referencesDeleted;
        this.endpointData = null;
        this.metaData = metaData;
    }

    public ComponentRun(Long componentRunId, String nodeID, Integer runCounter, Long startTime, Long endtime,
        String historyDataItem, Boolean referencesDeleted, Map<String, String> metaData) {
        this.componentRunID = componentRunId;
        this.componentInstanceID = null;
        this.nodeID = nodeID;
        this.runCounter = runCounter;
        this.startTime = startTime;
        this.endTime = endtime;
        this.historyDataItem = historyDataItem;
        this.referencesDeleted = referencesDeleted;
        this.endpointData = null;
        this.metaData = metaData;
    }

    public Long getEndTime() {
        return endTime;
    }

    public String getHistoryDataItem() {
        return historyDataItem;
    }

    public String getNodeID() {
        return nodeID;
    }

    public Integer getRunCounter() {
        return runCounter;
    }
    
    public Map<String, String> getMetaData() {
        return metaData;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getComponentInstanceID() {
        return componentInstanceID;
    }

    public Long getComponentRunID() {
        return componentRunID;
    }

    public Set<EndpointData> getEndpointData() {
        return endpointData;
    }

    public void setEndpointData(Set<EndpointData> endpointData) {
        this.endpointData = endpointData;
    }

    public Boolean isReferencesDeleted() {
        return referencesDeleted;
    }
    
    /**
     * @return the reference to the log file, if available, otherwise <code>null</code>
     */
    public String getLogFile() {
        return metaData.get(PropertiesKeys.COMPONENT_LOG_FILE);
    }
    
    /**
     * @return the reference to the error log file, if available, otherwise <code>null</code>
     */
    public String getErrorLogFile() {
        return metaData.get(PropertiesKeys.COMPONENT_LOG_ERROR_FILE);
    }

    @Override
    public int compareTo(ComponentRun arg0) {
        return getStartTime().compareTo(arg0.getStartTime());
    }
}
