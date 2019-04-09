/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

/**
 * Available event types for the {@link WorkflowStateMachine}.
 * 
 * @author Doreen Seider
 */
enum WorkflowStateMachineEventType {
    
    // requests
    START_REQUESTED,
    PAUSE_REQUESTED,
    RESUME_REQUESTED,
    CANCEL_REQUESTED,
    CANCEL_AFTER_FAILED_REQUESTED,
    CANCEL_AFTER_COMPONENT_LOST_REQUESTED,
    CANCEL_AFTER_RESULTS_REJECTED_REQUESTED,
    DISPOSE_REQUESTED,
    // successful attempts
    PREPARE_ATTEMPT_SUCCESSFUL,
    START_ATTEMPT_SUCCESSFUL,
    PAUSE_ATTEMPT_SUCCESSFUL,
    RESUME_ATTEMPT_SUCCESSFUL,
    CANCEL_ATTEMPT_SUCCESSFUL,
    DISPOSE_ATTEMPT_SUCCESSFUL,
    // failed attempts
    PREPARE_ATTEMPT_FAILED,
    START_ATTEMPT_FAILED,
    PAUSE_ATTEMPT_FAILED,
    RESUME_ATTEMPT_FAILED,
    CANCEL_ATTEMPT_FAILED,
    DISPOSE_ATTEMPT_FAILED,
    VERIFICATION_ATTEMPT_FAILED,
    FINISH_ATTEMPT_FAILED,
    PROCESS_COMPONENT_TIMELINE_EVENTS_FAILED,
    COMPONENT_HEARTBEAT_LOST,
    // finished
    ON_COMPONENTS_FINISHED,
    FINISHED
}
