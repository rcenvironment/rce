/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

/**
 * Available event types for the {@link ComponentStateMachine}.
 * 
 * @author Doreen Seider
 */
enum ComponentStateMachineEventType {
    
    // requests
    PREPARE_REQUESTED,
    START_REQUESTED,
    PROCESSING_INPUT_DATUMS_REQUESTED,
    CANCEL_REQUESTED,
    DISPOSE_REQUESTED,
    PAUSE_REQUESTED,
    RESUME_REQUESTED,
    RESET_REQUESTED,
    IDLE_REQUESTED,

    // successful attempts
    PREPARATION_SUCCESSFUL,
    START_SUCCESSFUL,
    RESET_SUCCESSFUL,
    PROCESSING_INPUTS_SUCCESSFUL,
    CANCEL_ATTEMPT_SUCCESSFUL,
    DISPOSE_ATTEMPT_SUCCESSFUL,

    // failed attempts
    PREPARATION_FAILED,
    START_FAILED,
    RESET_FAILED,
    PROCESSING_INPUTS_FAILED,
    SCHEDULING_FAILED,
    CANCEL_ATTEMPT_FAILED,
    PAUSE_ATTEMPT_FAILED,
    WF_CRTL_CALLBACK_FAILED,

    NEW_SCHEDULING_STATE,
    RUNNING,
    FINISHED,
    TEARED_DOWN
}
