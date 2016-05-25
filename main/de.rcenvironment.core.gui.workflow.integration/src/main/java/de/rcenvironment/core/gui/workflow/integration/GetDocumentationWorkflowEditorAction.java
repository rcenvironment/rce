/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.gui.workflow.editor.documentation.ToolIntegrationDocumentationGUIHelper;

/**
 * {@link WorkflowEditorAction} used to open the tool's documentation if available.
 * 
 * @author Sascha Zur
 */
public class GetDocumentationWorkflowEditorAction extends WorkflowEditorAction {

    @Override
    public void run() {
        String identifier = workflowNode.getComponentDescription().getComponentInterface().getIdentifier();
        ToolIntegrationDocumentationGUIHelper.getInstance().showComponentDocumentation(identifier);
    }

}
