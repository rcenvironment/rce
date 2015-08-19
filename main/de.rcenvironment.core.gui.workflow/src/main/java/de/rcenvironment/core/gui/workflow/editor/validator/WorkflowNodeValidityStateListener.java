/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.EventListener;


/**
 * Listener to react on changes in the validity of a {@link WorkflowNodeValidator}.
 * 
 * @author Christian Weiss
 */
public interface WorkflowNodeValidityStateListener extends EventListener {

    /**
     * Handle the change of the validity status determined by a {@link WorkflowNodeValidator}.
     * 
     * @param event the event with detailed information
     */
    void handleWorkflowNodeValidityStateEvent(WorkflowNodeValidityStateEvent event);

}
