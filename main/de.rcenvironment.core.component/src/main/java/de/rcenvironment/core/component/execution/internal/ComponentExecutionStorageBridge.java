/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.datamanagement.DistributedMetaDataService;
import de.rcenvironment.core.datamodel.api.FinalComponentState;

/**
 * Bridge class to the data management, holding relevant data management ids.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionStorageBridge {

    private final String errorMessageSuffix;
    
    private final DistributedMetaDataService metaDataService;
    
    private final NodeIdentifier storageNodeId;

    private final int timestampOffset;
    
    private final Long compInstanceDmId;
    
    private final Map<String, Long> inputDmIds;
    
    private final Map<String, Long> outputDmIds;
    
    private Long compExeDmId;
    
    private EndpointCountMap inputCount = new EndpointCountMap();
    
    private EndpointCountMap outputCount = new EndpointCountMap();
    
    public ComponentExecutionStorageBridge(DistributedMetaDataService metaDataService, ComponentExecutionContext compExeCtx,
        int timestampOffset) {
        this.metaDataService = metaDataService;
        this.storageNodeId = compExeCtx.getDefaultStorageNodeId();
        this.compInstanceDmId = compExeCtx.getInstanceDataManagementId();
        this.timestampOffset = timestampOffset;
        this.inputDmIds = compExeCtx.getInputDataManagementIds();
        this.outputDmIds = compExeCtx.getOutputDataManagementIds();
        errorMessageSuffix = String.format(" of component '%s' (%s) of workflow '%s' (%s)",
            compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
            compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier());
    }
    
    protected synchronized void addComponentExecution(ComponentExecutionContext compExeCtx, Integer executionCount)
        throws ComponentExecutionException {
        try {
            compExeDmId = metaDataService.addComponentRun(compInstanceDmId, compExeCtx.getNodeId().getIdString(),
                executionCount, System.currentTimeMillis() + timestampOffset, storageNodeId);
        } catch (CommunicationException e) {
            throw new ComponentExecutionException("Failed to store component execution" + errorMessageSuffix, e);
        }
    }
    
    protected synchronized Long addOutput(String outputName, String datum) throws ComponentExecutionException {
        assertCompExeDmIdNotNull();
        try {
            return metaDataService.addOutputDatum(compExeDmId, outputDmIds.get(outputName), datum,
                outputCount.getAndIncrement(outputName), storageNodeId);
        } catch (CommunicationException e) {
            throw new ComponentExecutionException(String.format("Failed to store output '%s'", outputName) + errorMessageSuffix, e);
        }
    }
    
    protected synchronized void addInput(String inputName, Long typedDatumId) throws ComponentExecutionException {
        assertCompExeDmIdNotNull();
        if (typedDatumId == null) {
            throw new ComponentExecutionException(String.format("Failed to store input '%s'", inputName) + errorMessageSuffix + ", "
                + "because given datamanagement id of related ouput was null. Likely, because saving output failed earlier.");
        } else {
            try {
                metaDataService.addInputDatum(compExeDmId, typedDatumId, inputDmIds.get(inputName),
                    inputCount.getAndIncrement(inputName), storageNodeId);
            } catch (CommunicationException e) {
                throw new ComponentExecutionException(String.format("Failed to store input '%s'", inputName) + errorMessageSuffix, e);
            }
        }
    }
    
    protected synchronized void setComponentExecutionFinished() throws ComponentExecutionException {
        try {
            metaDataService.setComponentRunFinished(compExeDmId, Long.valueOf(System.currentTimeMillis() + timestampOffset), storageNodeId);
        } catch (CommunicationException e) {
            throw new ComponentExecutionException("Failed to store component execution" + errorMessageSuffix, e);
        }
        compExeDmId = null;
    }
    
    protected synchronized void setFinalComponentState(FinalComponentState finalState) throws ComponentExecutionException {
        try {
            metaDataService.setComponentInstanceFinalState(compInstanceDmId, finalState, storageNodeId);
        } catch (CommunicationException e) {
            throw new ComponentExecutionException("Failed to store final state" + errorMessageSuffix, e);
        }
    }
    
    protected synchronized void setOrUpdateHistoryDataItem(String historyDataItem) throws ComponentExecutionException {
        assertCompExeDmIdNotNull();
        try {
            metaDataService.setOrUpdateHistoryDataItem(compExeDmId, historyDataItem, storageNodeId);
        } catch (CommunicationException e) {
            throw new ComponentExecutionException("Failed to store history data" + errorMessageSuffix, e);
        }
    }
    
    protected synchronized Long getComponentExecutionDataManagementId() {
        return compExeDmId;
    }
    
    private void assertCompExeDmIdNotNull() throws ComponentExecutionException {
        if (compExeDmId == null) {
            throw new ComponentExecutionException("There is no related component run in the database stored. Request will be ignored.");
        }
    }
    
    /**
     * {@link HashMap} which has default values and adds support for incrementing its values when getting them.
     * 
     * @author Doreen Seider
     */
    private class EndpointCountMap extends HashMap<String, Integer> {
        
        private static final long serialVersionUID = 1L;

        @Override
        public Integer get(Object key) {
            if (!containsKey(key)) {
                put((String) key, new Integer(0));
            }
            return super.get(key);
        }
        
        public Integer getAndIncrement(String endpointName) {
            Integer count = get(endpointName);
            put(endpointName, count + 1);
            return count;
        }
    }
}
