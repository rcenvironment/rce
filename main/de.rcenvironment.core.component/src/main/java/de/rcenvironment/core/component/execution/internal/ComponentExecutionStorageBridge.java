/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.HashMap;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.datamanagement.DataManagementIdMapping;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Bridge class to the data management, holding relevant data management ids.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 * @author Brigitte Boden
 */
public class ComponentExecutionStorageBridge {

    private static MetaDataService metaDataService;

    private final ComponentExecutionRelatedInstances compExeRelatedInstances;

    private final String errorMessageSuffix;

    private final int timestampOffset;

    private Long compExeDmId;

    private EndpointCountMap inputCount = new EndpointCountMap();

    private EndpointCountMap outputCount = new EndpointCountMap();

    @Deprecated
    public ComponentExecutionStorageBridge() {
        compExeRelatedInstances = null;
        this.timestampOffset = 0;
        errorMessageSuffix = null;
    }

    public ComponentExecutionStorageBridge(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        this.compExeRelatedInstances = compExeRelatedInstances;
        this.timestampOffset = compExeRelatedInstances.timestampOffsetToWorkfowNode;
        errorMessageSuffix = StringUtils.format(" of '%s' (%s) (workflow '%s' (%s)) at %s",
            compExeRelatedInstances.compExeCtx.getInstanceName(), compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
            compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
            compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier(),
            compExeRelatedInstances.compExeCtx.getWorkflowNodeId());
    }

    protected synchronized void addComponentExecution(final ComponentExecutionContext compExeCtx, final Integer executionCount)
        throws ComponentExecutionException {
        try {
            compExeDmId = metaDataService.addComponentRun(compExeRelatedInstances.compExeCtx.getInstanceDataManagementId(),
                DataManagementIdMapping.mapLogicalNodeIdToDbString(compExeCtx.getNodeId()),
                executionCount, System.currentTimeMillis() + timestampOffset,
                compExeRelatedInstances.compExeCtx.getDefaultStorageNodeId());
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (CommunicationException | RuntimeException e) {
            throw new ComponentExecutionException("Failed to store component execution" + errorMessageSuffix, e);
        }
    }

    protected synchronized Long addOutput(final String outputName, final String datum) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Adding value for output: " + outputName);
        try {
            return metaDataService.addOutputDatum(compExeDmId,
                compExeRelatedInstances.compExeCtx.getOutputDataManagementIds().get(outputName), datum,
                outputCount.getAndIncrement(outputName), compExeRelatedInstances.compExeCtx.getDefaultStorageNodeId());
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (CommunicationException | RuntimeException e) {
            throw new ComponentExecutionException(StringUtils.format("Failed to store output '%s'", outputName) + errorMessageSuffix, e);
        }
    }

    protected synchronized void addInput(final String inputName, final Long typedDatumId) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Adding value for input: " + inputName);
        if (typedDatumId == null) {
            throw new ComponentExecutionException(StringUtils.format("Failed to store input '%s'", inputName) + errorMessageSuffix + ", "
                + "because given datamanagement id of related output was null. Likely, because saving output failed earlier.");
        } else {
            try {
                metaDataService.addInputDatum(compExeDmId, typedDatumId,
                    compExeRelatedInstances.compExeCtx.getInputDataManagementIds().get(inputName),
                    inputCount.getAndIncrement(inputName), compExeRelatedInstances.compExeCtx.getDefaultStorageNodeId());
                // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
            } catch (CommunicationException | RuntimeException e) {
                throw new ComponentExecutionException(StringUtils.format("Failed to store input '%s'", inputName) + errorMessageSuffix, e);
            }
        }
    }

    protected synchronized void setComponentExecutionFinished(final FinalComponentRunState finalState) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Setting component execution to finish");
        try {
            metaDataService.setComponentRunFinished(compExeDmId, System.currentTimeMillis() + timestampOffset,
                finalState, compExeRelatedInstances.compExeCtx.getDefaultStorageNodeId());
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (CommunicationException | RuntimeException e) {
            throw new ComponentExecutionException("Failed to store component execution" + errorMessageSuffix, e);
        }
        compExeDmId = null;
    }

    protected synchronized void setFinalComponentState(final FinalComponentState finalState) throws ComponentExecutionException {
        try {
            metaDataService.setComponentInstanceFinalState(compExeRelatedInstances.compExeCtx.getInstanceDataManagementId(), finalState,
                compExeRelatedInstances.compExeCtx.getDefaultStorageNodeId());
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (CommunicationException | RuntimeException e) {
            throw new ComponentExecutionException("Failed to store final state" + errorMessageSuffix, e);
        }
    }

    protected synchronized void setOrUpdateHistoryDataItem(final String historyDataItem) throws ComponentExecutionException {
        assertCompExeDmIdNotNull("Adding or updating history data");
        try {
            metaDataService.setOrUpdateHistoryDataItem(compExeDmId, historyDataItem,
                compExeRelatedInstances.compExeCtx.getDefaultStorageNodeId());
            // catch RuntimeException until https://mantis.sc.dlr.de/view.php?id=13865 is solved
        } catch (CommunicationException | RuntimeException e) {
            throw new ComponentExecutionException("Failed to add or update history data" + errorMessageSuffix, e);
        }
    }

    protected synchronized boolean hasUnfinishedComponentExecution() {
        return compExeDmId != null;
    }

    protected synchronized Long getComponentExecutionDataManagementId() {
        return compExeDmId;
    }

    private void assertCompExeDmIdNotNull(String info) throws ComponentExecutionException {
        if (compExeDmId == null) {
            throw new ComponentExecutionException(StringUtils.format("No component run for component '%s' stored in the database; "
                + "request failed: '%s'; note: writing outputs and history data items is only allowed within 'start()' if "
                + "'treatStartAsComponentRun()' returns true and within 'processInputs()' and not allowed at all if component "
                + "was cancelled", compExeRelatedInstances.compExeCtx.getExecutionIdentifier(), info));
        }
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

    protected void bindMetaDataService(MetaDataService newService) {
        ComponentExecutionStorageBridge.metaDataService = newService;
    }
}
