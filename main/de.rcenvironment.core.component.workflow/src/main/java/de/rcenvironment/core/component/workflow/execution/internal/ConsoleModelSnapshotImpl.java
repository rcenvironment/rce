/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleModelSnapshot;

/**
 * Default implementation of {@link ConsoleModelSnapshot}.
 * 
 * @author Robert Mischke
 */
class ConsoleModelSnapshotImpl implements ConsoleModelSnapshot {

    private Collection<ConsoleRow> filteredRows;

    private int sequenceId;

    private Collection<String> componentList;

    private Collection<String> workflowList;

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
    public boolean hasWorkflowListChanged() {
        return workflowList != null;
    }

    @Override
    public Collection<String> getWorkflowList() {
        return workflowList;
    }

    @Override
    public boolean hasComponentListChanged() {
        return componentList != null;
    }

    @Override
    public Collection<String> getComponentList() {
        return componentList;
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

    void setWorkflowList(Collection<String> workflowList) {
        this.workflowList = workflowList;
    }

    void setComponentList(Collection<String> componentList) {
        this.componentList = componentList;
    }

}
