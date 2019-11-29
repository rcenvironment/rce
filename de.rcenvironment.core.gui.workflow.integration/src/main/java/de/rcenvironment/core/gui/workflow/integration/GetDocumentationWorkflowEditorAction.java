/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
        String identifier = workflowNode.getComponentDescription().getComponentInterface().getIdentifierAndVersion();
        ToolIntegrationDocumentationGUIHelper.getInstance().showComponentDocumentation(identifier, false);
    }

}
