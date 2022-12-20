/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.workflowintegration.editor;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;

import de.rcenvironment.core.gui.integration.workflowintegration.WorkflowIntegrationController;

/**
 * IEditorInput for the {@link WorkflowIntegrationEditor}. This editor input is used with a given workflow file.
 *
 * @author Jan Flink
 */
public class WorkflowFileEditorInput extends WorkflowIntegrationEditorInput {

    public WorkflowFileEditorInput(IFile workflowFile) throws FileNotFoundException {
        super(new WorkflowIntegrationController());
        getWorkflowIntegrationController().setWorkflowDescriptionFromFile(workflowFile.getLocation().toFile());
    }
}
