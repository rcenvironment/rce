/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.gui.workflow.editor.documentation.ToolIntegrationDocumentationGUIHelper;

/**
 * {@link WorkflowEditorAction} used to download and save the tool's documentation if available.
 * 
 * @author Brigitte Boden
 */
public class ExportDocumentationWorkflowEditorAction extends WorkflowEditorAction {

    @Override
    public void run() {
        String identifier = workflowNode.getComponentDescription().getComponentInterface().getIdentifierAndVersion();
        ToolIntegrationDocumentationGUIHelper.getInstance().showComponentDocumentation(identifier, true);
    }

}
