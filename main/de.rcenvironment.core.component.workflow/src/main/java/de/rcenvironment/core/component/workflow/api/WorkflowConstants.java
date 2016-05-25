/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.api;

import de.rcenvironment.core.component.api.ComponentConstants;

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
    public static final int CURRENT_WORKFLOW_VERSION_NUMBER = 4;

    /** Initial workflow version number. */
    public static final int INITIAL_WORKFLOW_VERSION_NUMBER = 0;

    /** Constant. */
    public static final String ENCODING_UTF8 = "UTF-8";

    private WorkflowConstants() {}
}
