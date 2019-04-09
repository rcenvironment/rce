/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal.impl;

import java.io.Serializable;

import de.rcenvironment.core.eventlog.internal.EventLogContext;
import de.rcenvironment.core.eventlog.internal.EventLogMessageType;

/**
 * Internal representation of a log event entry to simplify dispatch.
 * 
 * @author Robert Mischke
 * 
 */
public class EventLogMessage implements Serializable {

    private static final long serialVersionUID = 7438234889863602761L;

    private EventLogContext context;

    private EventLogMessageType messageType;

    private String sourceId;

    private String workflowId;

    private String componentId;

    private Throwable detailInformation;

    private boolean localized;

    private String message;

    private Object[] parameters;

    // Note: package-local object for now; make methods public if needed externally

    protected EventLogContext getContext() {
        return context;
    }

    protected void setContext(EventLogContext context) {
        this.context = context;
    }

    protected EventLogMessageType getMessageType() {
        return messageType;
    }

    protected void setMessageType(EventLogMessageType messageType) {
        this.messageType = messageType;
    }

    protected String getSourceId() {
        return sourceId;
    }

    protected void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    protected String getWorkflowId() {
        return workflowId;
    }

    protected void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    protected String getComponentId() {
        return componentId;
    }

    protected void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    protected Throwable getDetailInformation() {
        return detailInformation;
    }

    protected void setDetailInformation(Throwable detailInformation) {
        this.detailInformation = detailInformation;
    }

    protected boolean isLocalized() {
        return localized;
    }

    protected void setLocalized(boolean localized) {
        this.localized = localized;
    }

    protected String getMessage() {
        return message;
    }

    protected void setMessage(String message) {
        this.message = message;
    }

    protected Object[] getParameters() {
        return parameters;
    }

    protected void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
