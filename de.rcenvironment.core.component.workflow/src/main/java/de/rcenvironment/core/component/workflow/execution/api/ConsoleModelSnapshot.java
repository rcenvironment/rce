/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Collection;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.internal.ConsoleRowModelServiceImpl;

/**
 * Represents a (possibly filtered) state of the ConsoleModel at a given time. Used to communicate
 * an immutable model state to the view in a single call.
 * 
 * @author Robert Mischke
 */
public interface ConsoleModelSnapshot {

    /**
     * Gets the sequence id of the {@link ConsoleRowModelServiceImpl} this snapshot was created at. Used for
     * efficient polling.
     * @return Returns the sequenceId.
     */
    int getSequenceId();

    /**
     * @return true if the filtered console row list has changed
     */
    boolean hasFilteredRowListChanged();

    /**
     * Get the current list of filtered {@link ConsoleRow}s.
     * @return the contained {@link ConsoleRow}s; only set if {@link #hasFilteredRowListChanged()}
     *         is true; otherwise null.
     */
    Collection<ConsoleRow> getFilteredRows();

    /**
     * @return true if the workflow list has potentially changed
     */
    boolean hasWorkflowListChanged();

    /**
     * @return the current workflow list; for performance, only set if #hasWorkflowListChanged is
     *         true; otherwise null
     */
    Collection<String> getWorkflowList();

    /**
     * @return true if the component list has potentially changed
     */
    boolean hasComponentListChanged();

    /**
     * @return the current component list; for performance, only set if
     *         {@link #hasComponentListChanged()} is true; otherwise null
     */
    Collection<String> getComponentList();

}
