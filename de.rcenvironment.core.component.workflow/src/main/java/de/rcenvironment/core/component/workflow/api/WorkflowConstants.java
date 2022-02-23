/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.api;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;

/**
 * Class holding workflow constants.
 * 
 * @author Jens Ruehmkorf
 */
public final class WorkflowConstants {

    /** Key to identify a created workflow instance at the service registry. */
    public static final String WORKFLOW_INSTANCE_ID_KEY = ComponentConstants.COMP_CONTEXT_INSTANCE_ID_KEY;

    /** Notification identifier for notifications sent on state change. */
    public static final String STATE_NOTIFICATION_ID = "rce.component.workflow.state:";

    /** Notification identifier for notifications sent on disposed state. */
    public static final String STATE_DISPOSED_NOTIFICATION_ID = "rce.component.workflow.state.disposed";

    /** Notification identifier for notifications sent on workflow creation. */
    public static final String NEW_WORKFLOW_NOTIFICATION_ID = "rce.component.workflow.new";

    /** Current workflow version number. */
    public static final int CURRENT_WORKFLOW_VERSION_NUMBER = 5;

    /** Initial workflow version number. */
    public static final int INITIAL_WORKFLOW_VERSION_NUMBER = 0;

    /** Constant. */
    public static final String ENCODING_UTF8 = "UTF-8";
    
    /** Constant. */
    public static final String WORKFLOW_FILE_ENDING = ".wf";
    
    /** Constant. */
    public static final String WORKFLOW_FILE_BACKUP_SUFFIX = "_backup";
    
    /** Canceling workflow states. */
    public static final List<WorkflowState> CANCELING_WORKFLOW_STATES = new ArrayList<>();
    
    /** Failed workflow states. */
    public static final List<WorkflowState> FAILED_WORKFLOW_STATES = new ArrayList<>();

    /** Final component states. */
    public static final List<WorkflowState> FINAL_WORKFLOW_STATES = new ArrayList<>();
    
    /** Final component states. */
    public static final List<WorkflowState> FINAL_WORKFLOW_STATES_WITH_DISPOSED = new ArrayList<>();

    static {
        CANCELING_WORKFLOW_STATES.add(WorkflowState.CANCELING);
        CANCELING_WORKFLOW_STATES.add(WorkflowState.CANCELING_AFTER_FAILED);
        CANCELING_WORKFLOW_STATES.add(WorkflowState.CANCELING_AFTER_RESULTS_REJECTED);
        FAILED_WORKFLOW_STATES.add(WorkflowState.FAILED);
        FAILED_WORKFLOW_STATES.add(WorkflowState.RESULTS_REJECTED);
        FINAL_WORKFLOW_STATES.addAll(FAILED_WORKFLOW_STATES);
        FINAL_WORKFLOW_STATES.add(WorkflowState.CANCELLED);
        FINAL_WORKFLOW_STATES.add(WorkflowState.FINISHED);
        FINAL_WORKFLOW_STATES_WITH_DISPOSED.addAll(FINAL_WORKFLOW_STATES);
        FINAL_WORKFLOW_STATES_WITH_DISPOSED.add(WorkflowState.DISPOSED);
    }

    private WorkflowConstants() {}
}
