/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Bridge class to the data management, holding relevant data management ids.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionStorageBridge {

    private static final Log LOG = LogFactory.getLog(ComponentExecutionStorageBridge.class);
    
    private final String compExeId;
    
    private final String errorMessageSuffix;

    private final MetaDataService metaDataService;

    private final NodeIdentifier storageNodeId;

    private final int timestampOffset;

    private final Long compInstanceDmId;

    private final Map<String, Long> inputDmIds;

    private final Map<String, Long> outputDmIds;

    private Long compExeDmId;

    private EndpointCountMap inputCount = new EndpointCountMap();

    private EndpointCountMap outputCount = new EndpointCountMap();

    public ComponentExecutionStorageBridge(MetaDataService metaDataService, ComponentExecutionContext compExeCtx,
        int timestampOffset) {
        this.compExeId = compExeCtx.getExecutionIdentifier();
        this.metaDataService = metaDataService;
        this.storageNodeId = compExeCtx.getDefaultStorageNodeId();
        this.compInstanceDmId = compExeCtx.getInstanceDataManagementId();
        this.timestampOffset = timestampOffset;
        this.inputDmIds = compExeCtx.getInputDataManagementIds();
        this.outputDmIds = compExeCtx.getOutputDataManagementIds();
        errorMessageSuffix = StringUtils.format(" of '%s' (%s) (workflow '%s' (%s)) at %s",
            compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
            compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier(),
            compExeCtx.getWorkflowNodeId());
    }

    protected synchronized void addComponentExecution(final ComponentExecutionContext compExeCtx, final Integer executionCount)
        throws ComponentExecutionException {
        new MetaDataServiceCaller("Failed to store component execution" + errorMessageSuffix) {
            @Override
            protected Object callback() throws CommunicationException {
                compExeDmId = metaDataService.addComponentRun(compInstanceDmId, compExeCtx.getNodeId().getIdString(),
                    executionCount, System.currentTimeMillis() + timestampOffset, storageNodeId);
                return null;
            }
        }.callbackWithRetries();
    }

    protected synchronized Long addOutput(final String outputName, final String datum) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Adding value for output: " + outputName);
        return (Long) new MetaDataServiceCaller(StringUtils.format("Failed to store output '%s'", outputName) + errorMessageSuffix) {
            @Override
            protected Object callback() throws CommunicationException {
                return metaDataService.addOutputDatum(compExeDmId, outputDmIds.get(outputName), datum,
                    outputCount.getAndIncrement(outputName), storageNodeId);
            }
        }.callbackWithRetries();
    }

    protected synchronized void addInput(final String inputName, final Long typedDatumId) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Adding value for input: " + inputName);
        if (typedDatumId == null) {
            throw new ComponentExecutionException(StringUtils.format("Failed to store input '%s'", inputName) + errorMessageSuffix + ", "
                + "because given datamanagement id of related ouput was null. Likely, because saving output failed earlier.");
        } else {
            new MetaDataServiceCaller(StringUtils.format("Failed to store input '%s'", inputName) + errorMessageSuffix) {
                @Override
                protected Object callback() throws CommunicationException {
                    metaDataService.addInputDatum(compExeDmId, typedDatumId, inputDmIds.get(inputName),
                        inputCount.getAndIncrement(inputName), storageNodeId);
                    return null;
                }
            }.callbackWithRetries();
        }
    }

    protected synchronized void setComponentExecutionFinished() throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Setting component execution to finish");
        new MetaDataServiceCaller("Failed to store component execution" + errorMessageSuffix) {
            @Override
            protected Object callback() throws CommunicationException {
                metaDataService.setComponentRunFinished(compExeDmId, System.currentTimeMillis() + timestampOffset, storageNodeId);
                return null;
            }
        }.callbackWithRetries();
        compExeDmId = null;
    }

    protected synchronized void setFinalComponentState(final FinalComponentState finalState) throws ComponentExecutionException {
        new MetaDataServiceCaller("Failed to store final state" + errorMessageSuffix) {
            @Override
            protected Object callback() throws CommunicationException {
                metaDataService.setComponentInstanceFinalState(compInstanceDmId, finalState, storageNodeId);
                return null;
            }
        }.callbackWithRetries();
    }

    protected synchronized void setOrUpdateHistoryDataItem(final String historyDataItem) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Adding or updating history data");
        new MetaDataServiceCaller("Failed to add or update history data" + errorMessageSuffix) {
            @Override
            protected Object callback() throws CommunicationException {
                metaDataService.setOrUpdateHistoryDataItem(compExeDmId, historyDataItem, storageNodeId);
                return null;
            }
        }.callbackWithRetries();
    }
    
    protected synchronized boolean hasUnfinishedComponentExecution() {
        return compExeDmId != null;
    }

    protected synchronized Long getComponentExecutionDataManagementId() {
        return compExeDmId;
    }

    private void assertCompExeDmIdNotNull(String info) throws ComponentExecutionException {
        if (compExeDmId == null) {
            throw new ComponentExecutionException(StringUtils.format("There is no related component run for component %s"
                + " in the database stored; ignored: '%s'", compExeId, info));
        }
    }

    /**
     * Executes callbacks to the workflow controller by doing a certain amount of retries in
     * case of failure.
     * 
     * @author Doreen Seider
     */
    private abstract class MetaDataServiceCaller {
        
        private final String exceptionMessage;
        
        protected MetaDataServiceCaller(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
        }

        protected Object callbackWithRetries() throws ComponentExecutionException {
            
            // retrying disabled as long as methods called are not robust against multiple calls

//            int failureCount = 0;
//            while (true) {
            try {
                Object result = callback();
//                    ComponentExecutionUtils.logCallbackSuccessAfterFailure(LOG, "Storing data" + errorMessageSuffix, failureCount);
                return result;
            } catch (CommunicationException e) {
//                    if (++failureCount < ComponentExecutionUtils.MAX_RETRIES) {
//                        ComponentExecutionUtils.waitForRetryAfterCallbackFailure(LOG, failureCount, 
//                            "Failed to store data" + errorMessageSuffix, e.toString());
//                    } else {
//                        ComponentExecutionUtils.logCallbackFailureAfterRetriesExceeded(LOG, 
//                            "Failed to store data" + errorMessageSuffix, e);
                throw new ComponentExecutionException(exceptionMessage, e);
//                    }
//                }
            }
        }
        
        protected abstract Object callback() throws CommunicationException;
    }
    
    /**
     * {@link HashMap} that has default values and adds support for incrementing its values when returning them.
     * 
     * @author Doreen Seider
     */
    private class EndpointCountMap extends HashMap<String, Integer> {

        private static final long serialVersionUID = 6170727124152514043L;

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
