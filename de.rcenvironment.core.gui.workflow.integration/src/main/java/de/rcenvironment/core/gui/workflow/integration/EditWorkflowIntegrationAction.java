/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.integration;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import de.rcenvironment.core.gui.integration.workflowintegration.handlers.EditWorkflowIntegrationHandler;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;

/**
 * Action that allows the user to edit the integration of a workflow directly from its context menu in the workflow editor.
 * 
 * @author Kathrin Schaffert
 */
public class EditWorkflowIntegrationAction extends WorkflowEditorAction {

    @Override
    public void run() {
        try {
            new EditWorkflowIntegrationHandler(workflowNode.getComponentDescription().getName()).execute(new ExecutionEvent());
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass()).error("Opening Workflow Integration Wizard failed", e);
        }
    }
}
