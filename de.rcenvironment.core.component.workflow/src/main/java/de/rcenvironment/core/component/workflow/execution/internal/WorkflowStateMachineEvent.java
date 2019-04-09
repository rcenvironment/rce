/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

/**
 * Events the {@link WorkflowStateMachine} can process.
 * 
 * @author Doreen Seider
 */
final class WorkflowStateMachineEvent {

    private final WorkflowStateMachineEventType type;

    private final Throwable throwable;

    private final String errorId;

    private final String errorMessage;

    private final String compExeId;

    protected WorkflowStateMachineEvent(WorkflowStateMachineEventType type) {
        this.type = type;
        this.throwable = null;
        this.errorId = null;
        this.errorMessage = null;
        this.compExeId = null;
    }
    
    protected WorkflowStateMachineEvent(WorkflowStateMachineEventType type, String compExeId) {
        this.type = type;
        this.throwable = null;
        this.errorId = null;
        this.errorMessage = null;
        this.compExeId = compExeId;
    }

    protected WorkflowStateMachineEvent(WorkflowStateMachineEventType type, Throwable throwable) {
        this.type = type;
        this.throwable = throwable;
        this.errorId = null;
        this.errorMessage = null;
        this.compExeId = null;
    }

    protected WorkflowStateMachineEvent(WorkflowStateMachineEventType type, String errorId, String errorMessage, String compExeId) {
        this.type = type;
        this.throwable = null;
        this.errorId = errorId;
        this.errorMessage = errorMessage;
        this.compExeId = compExeId;
    }

    protected  WorkflowStateMachineEventType getType() {
        return type;
    }

    protected  Throwable getThrowable() {
        return throwable;
    }

    protected  String getErrorId() {
        return errorId;
    }

    protected  String getErrorMessage() {
        return errorMessage;
    }

    protected  String getComponentExecutionId() {
        return compExeId;
    }

    @Override
    public String toString() {
        return type.name();
    }

}
