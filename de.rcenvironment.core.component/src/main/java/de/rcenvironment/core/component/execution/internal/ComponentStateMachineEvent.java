/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.execution.api.ComponentState;

/**
 * Events the {@link ComponentStateMachine} can process.
 * 
 * @author Doreen Seider
 */
final class ComponentStateMachineEvent {

    private final ComponentStateMachineEventType type;

    private Throwable throwable;

    private String errorId;

    private ComponentState newComponentState;

    protected  ComponentStateMachineEvent(ComponentStateMachineEventType type) {
        this.type = type;
    }

    protected  ComponentStateMachineEvent(ComponentStateMachineEventType type, Throwable t) {
        this(type);
        this.throwable = t;
    }

    protected  ComponentStateMachineEvent(ComponentStateMachineEventType type, ComponentState newComponentState) {
        this(type);
        this.newComponentState = newComponentState;
    }

    protected  ComponentStateMachineEvent(ComponentStateMachineEventType type, ComponentState newComponentState, Throwable t) {
        this(type, t);
        this.newComponentState = newComponentState;
    }

    protected  ComponentStateMachineEvent(ComponentStateMachineEventType type, String errorId) {
        this(type);
        this.errorId = errorId;
    }

    protected  ComponentStateMachineEvent(ComponentStateMachineEventType tearedDown, ComponentState failed, String errorId) {
        this(tearedDown, failed);
        this.errorId = errorId;
    }

    protected  ComponentStateMachineEventType getType() {
        return type;
    }

    protected  Throwable getThrowable() {
        return throwable;
    }

    protected  String getErrorId() {
        return errorId;
    }

    protected  ComponentState getNewComponentState() {
        return newComponentState;
    }

    @Override
    public String toString() {
        return type.name();
    }

}
