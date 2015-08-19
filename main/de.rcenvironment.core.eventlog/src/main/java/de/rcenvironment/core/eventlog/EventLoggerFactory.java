/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.eventlog;

import de.rcenvironment.core.eventlog.internal.EventLogContext;
import de.rcenvironment.core.eventlog.internal.impl.EventLoggerImpl;

/**
 * Static factory for {@link EventLogger} instances.
 * 
 * @author Robert Mischke
 */
public abstract class EventLoggerFactory {

    /**
     * Private constructor; this class is not meant to be instantiated.
     */
    private EventLoggerFactory() {}

    /**
     * Returns a {@link EventLogger} for global, platform-wide events.
     * 
     * This method may or may not return the same instance when called again with the same
     * arguments.
     * 
     * @param clazz the class from which this logger is used
     * @return the logger instance
     */
    public static EventLogger getPlatformLogger(Class<?> clazz) {
        return new EventLoggerImpl(EventLogContext.PLATFORM, clazz, null, null);
    }

    /**
     * Returns a {@link WorkflowEventLogger} for events associated with a workflow.
     * 
     * This method may or may not return the same instance when called again with the same
     * arguments.
     * 
     * @param clazz the class from which this logger is used
     * @param workflowId the id of the relevant workflow
     * @return the logger instance
     */
    public static WorkflowEventLogger getWorkflowLogger(Class<?> clazz, String workflowId) {
        return new EventLoggerImpl(EventLogContext.WORKFLOW, clazz, workflowId, null);
    }

    /**
     * Returns a {@link ComponentEventLogger} for events associated with a workflow component.
     * 
     * This method may or may not return the same instance when called again with the same
     * arguments.
     * 
     * @param clazz the class from which this logger is used
     * @param workflowId the id of the relevant workflow
     * @param componentId the id of the relevant workflow component
     * @return the logger instance
     */
    public static ComponentEventLogger getComponentLogger(Class<?> clazz, String workflowId, String componentId) {
        return new EventLoggerImpl(EventLogContext.COMPONENT, clazz, workflowId, componentId);
    }
}
