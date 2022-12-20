/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleModelSnapshot;

/**
 * Default implementation of {@link ConsoleModelSnapshot}.
 * 
 * @author Robert Mischke
 * @author Kathrin Schaffert (#17869: changed storage of data in newly added workflowComponentsMap)
 */
class ConsoleModelSnapshotImpl implements ConsoleModelSnapshot {

    private Collection<ConsoleRow> filteredRows;

    private int sequenceId;

    private Map<String, Collection<String>> workflowComponentsMap;

    private boolean workflowListChanged = false;

    @Override
    public int getSequenceId() {
        return sequenceId;
    }

    @Override
    public boolean hasFilteredRowListChanged() {
        return filteredRows != null;
    }

    @Override
    public Collection<ConsoleRow> getFilteredRows() {
        return filteredRows;
    }

    @Override
    public boolean isWorkflowListChanged() {
        return workflowListChanged;
    }

    public void setWorkflowListChanged(boolean workflowListChanged) {
        this.workflowListChanged = workflowListChanged;
    }

    @Override
    public Collection<String> getWorkflowList() {
        List<String> list = workflowComponentsMap.keySet().stream().collect(Collectors.toList());
        Collections.sort(list);
        return list;
    }

    @Override
    public boolean hasComponentListChanged() {
        return workflowComponentsMap != null;
    }

    @Override
    public Map<String, Collection<String>> getWorkflowComponentsMap() {
        return workflowComponentsMap;
    }

    /**
     * @param filteredRows The filteredRows to set.
     */
    void setFilteredRows(Collection<ConsoleRow> filteredRows) {
        this.filteredRows = filteredRows;
    }

    /**
     * @param sequenceId The sequenceId to set.
     */
    void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    void setWorkflowComponentsMap(Map<String, Collection<String>> map) {
        this.workflowComponentsMap = map;
    }

}
