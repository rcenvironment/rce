/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog;

/**
 * A logger for workflow events. Provides additional support for state change events.
 * 
 * @author Robert Mischke
 * 
 */
public interface WorkflowEventLogger extends EventLogger {

    /**
     * Report the state change of a workflow or a component. The content of the state object is not
     * specified by the event log API and must be understood by the final event receivers.
     * 
     * @param oldState an object representing the previous state; may be null
     * @param newState an object representing the new state; must not be null
     */
    void stateChanged(Object oldState, Object newState);
}
