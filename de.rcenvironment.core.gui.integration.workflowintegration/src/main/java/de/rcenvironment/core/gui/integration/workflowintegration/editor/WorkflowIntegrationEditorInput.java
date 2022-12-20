/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor;

import java.util.Optional;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.integration.workflowintegration.WorkflowIntegrationController;

/**
 * IEditorInput for the {@link WorkflowIntegrationEditor}. This editor input is used with a given {@link WorkflowIntegrationController} or
 * {@link WorkflowDescription}.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class WorkflowIntegrationEditorInput implements IEditorInput {

    private WorkflowIntegrationController workflowIntegrationController;


    public WorkflowIntegrationEditorInput(WorkflowIntegrationController workflowIntegrationController) {
        super();
        this.workflowIntegrationController = workflowIntegrationController;
    }

    public WorkflowIntegrationEditorInput(WorkflowDescription workflowDescription) {
        this(new WorkflowIntegrationController());
        this.workflowIntegrationController.setWorkflowDescription(workflowDescription);
    }

    public WorkflowIntegrationController getWorkflowIntegrationController() {
        return workflowIntegrationController;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> type) {
        if (type == WorkflowIntegrationController.class) {
            return (T) workflowIntegrationController;
        }
        if (type == WorkflowDescription.class) {
            return (T) workflowIntegrationController.getWorkflowDescription();
        }

        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.getMissingImageDescriptor();
    }

    @Override
    public String getName() {
        return workflowIntegrationController.getEditorTitle();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WorkflowIntegrationEditorInput) {
            return ((WorkflowIntegrationEditorInput) o).getName().equals(this.getName());
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((workflowIntegrationController.getWorkflowDescription().getName() == null) ? 0
            : workflowIntegrationController.getWorkflowDescription().getName().hashCode());
        return result;
    }

    public Optional<String> validate() {
        return WorkflowIntegrationEditorInputValidator.validate(this);
    }
}
