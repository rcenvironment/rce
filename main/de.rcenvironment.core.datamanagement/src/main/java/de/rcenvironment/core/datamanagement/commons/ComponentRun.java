/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;

/**
 * Identifier for a component run.
 * 
 * @author Jan Flink
 * @author Brigitte Boden
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class ComponentRun implements Serializable, Comparable<ComponentRun> {

    private static final long serialVersionUID = -366100876644355558L;

    private final Long componentInstanceID;

    private final Long componentRunID;

    private final FinalComponentRunState finalState;

    private final String logicalNodeId;

    // as this object is created from a string for on the server side anyway, only parse this id on demand on the client side
    private transient LogicalNodeId logicalNodeIdObject;

    private final Integer runCounter;

    private final Long startTime;

    private final Long endTime;

    private final String historyDataItem;

    private final Boolean referencesDeleted;

    private Set<EndpointData> endpointData;

    private final Map<String, String> metaData;

    public ComponentRun(Long componentRunID, Long componentInstanceID, String logicalNodeId, Integer runCounter, Long startTime,
        Long endtime, String historyDataItem, Boolean referencesDeleted, Map<String, String> metaData, FinalComponentRunState finalState) {
        this.componentRunID = componentRunID;
        this.componentInstanceID = componentInstanceID;
        this.logicalNodeId = logicalNodeId;
        this.runCounter = runCounter;
        this.startTime = startTime;
        this.endTime = endtime;
        this.historyDataItem = historyDataItem;
        this.referencesDeleted = referencesDeleted;
        this.endpointData = null;
        this.metaData = metaData;
        this.finalState = finalState;
    }

    public ComponentRun(Long componentRunId, String logicalNodeId, Integer runCounter, Long startTime, Long endtime,
        String historyDataItem, Boolean referencesDeleted, Map<String, String> metaData, FinalComponentRunState finalState) {
        this.componentRunID = componentRunId;
        this.componentInstanceID = null;
        this.logicalNodeId = logicalNodeId;
        this.runCounter = runCounter;
        this.startTime = startTime;
        this.endTime = endtime;
        this.historyDataItem = historyDataItem;
        this.referencesDeleted = referencesDeleted;
        this.endpointData = null;
        this.metaData = metaData;
        this.finalState = finalState;
    }

    public Long getEndTime() {
        return endTime;
    }

    public String getHistoryDataItem() {
        return historyDataItem;
    }

    public String getLogicalNodeIdString() {
        return logicalNodeId;
    }

    /**
     * @return returns the {@link LogicalNodeId} representation of the stored instance or logical node id representing the execution
     *         location; this should be used in place of {@link #getLogicalNodeIdString()} for type safety
     */
    public synchronized LogicalNodeId getLogicalNodeId() {
        if (logicalNodeIdObject == null && logicalNodeId != null) { // also return null in case the string id is ever null
            try {
                logicalNodeIdObject = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeId(logicalNodeId);
            } catch (IdentifierException e) {
                throw new RuntimeException("Failed to parse component run location string (expected an instance id or logical node id)", e);
            }
        }
        return logicalNodeIdObject;
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

    public FinalComponentRunState getFinalState() {
        return finalState;
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
