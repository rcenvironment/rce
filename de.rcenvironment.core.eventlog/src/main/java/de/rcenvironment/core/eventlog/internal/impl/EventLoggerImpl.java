/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal.impl;

import de.rcenvironment.core.eventlog.ComponentEventLogger;
import de.rcenvironment.core.eventlog.EventLogger;
import de.rcenvironment.core.eventlog.WorkflowEventLogger;
import de.rcenvironment.core.eventlog.internal.EventLogContext;
import de.rcenvironment.core.eventlog.internal.EventLogMessageType;
import de.rcenvironment.core.eventlog.internal.EventLogService;

/**
 * Internal implementation of {@link EventLogger} and its provided subinterfaces.
 * 
 * @author Robert Mischke
 * 
 */
public class EventLoggerImpl implements EventLogger, WorkflowEventLogger, ComponentEventLogger {

    /**
     * The shared reference to the current {@link EventLogService}; initialized with a placeholder
     * implementation.
     */
    private static EventLogService eventLogService = new EventLogServiceForwardToACLImpl();

    private EventLogContext context;

    private String sourceId;

    private String workflowId;

    private String componentId;

    public EventLoggerImpl(EventLogContext context, Class<?> clazz, String workflowId, String componentId) {
        this.context = context;
        this.sourceId = clazz.getName();
        this.workflowId = workflowId;
        this.componentId = componentId;
    }

    @Override
    public void error(boolean localized, String message, Object... parameters) {
        // delegate with a "null" detail information
        error(null, localized, message, parameters);
    }

    @Override
    public void error(Throwable detailInformation, boolean localized, String message, Object... parameters) {
        createMessage(EventLogMessageType.ERROR, detailInformation, localized, message, parameters);
    }

    @Override
    public void warn(boolean localized, String message, Object... parameters) {
        // delegate with a "null" detail information
        warn(null, localized, message, parameters);
    }

    @Override
    public void warn(Throwable detailInformation, boolean localized, String message, Object... parameters) {
        createMessage(EventLogMessageType.WARNING, detailInformation, localized, message, parameters);
    }

    @Override
    public void info(boolean localized, String message, Object... parameters) {
        createMessage(EventLogMessageType.INFO, null, localized, message, parameters);
    }

    @Override
    public void debug(String message, Object... parameters) {
        // delegate with a "null" detail information
        debug(null, message, parameters);
    }

    @Override
    public void debug(Throwable detailInformation, String message, Object... parameters) {
        createMessage(EventLogMessageType.DEBUG_DEFAULT, detailInformation, false, message, parameters);
    }

    @Override
    public void debugVerbose(String message, Object... parameters) {
        // delegate with a "null" detail information
        debugVerbose(null, message, parameters);
    }

    @Override
    public void debugVerbose(Throwable detailInformation, String message, Object... parameters) {
        createMessage(EventLogMessageType.DEBUG_VERBOSE, detailInformation, false, message, parameters);
    }

    @Override
    public boolean isDebugVerboseEnabled() {
        // TODO add runtime flag/switch
        return true;
    }

    @Override
    public void stateChanged(Object oldState, Object newState) {
    }

    @Override
    public void stdout(String line) {
        createMessage(EventLogMessageType.STDOUT, null, false, line);
    }

    @Override
    public void stderr(String line) {
        createMessage(EventLogMessageType.STDERR, null, false, line);
    }

    private void createMessage(EventLogMessageType messageType, Throwable detailInformation, boolean localized, String message,
        Object... parameters) {
        EventLogMessage messageObject = new EventLogMessage();
        // call-independent parameters
        messageObject.setContext(context);
        messageObject.setSourceId(sourceId);
        messageObject.setWorkflowId(workflowId);
        messageObject.setComponentId(componentId);
        // call-specific parameters
        messageObject.setMessageType(messageType);
        messageObject.setDetailInformation(detailInformation);
        messageObject.setLocalized(localized);
        messageObject.setMessage(message);
        messageObject.setParameters(parameters);
        // send to dispatching service
        eventLogService.dispatchMessage(messageObject);
    }
}
