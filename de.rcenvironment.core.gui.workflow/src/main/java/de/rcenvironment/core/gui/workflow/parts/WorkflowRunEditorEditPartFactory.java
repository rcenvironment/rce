/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Factory responsible for creating the EditParts.
 * 
 * @author Heinrich Wendel
 * @author Martin Misiak
 */
public class WorkflowRunEditorEditPartFactory implements EditPartFactory {

    @Override
    public EditPart createEditPart(EditPart context, Object model) {
        EditPart part = null;
        if (model instanceof WorkflowDescription) {
            part = new ReadOnlyWorkflowPart();
        } else if (model instanceof WorkflowNode) {
            part = new WorkflowRunNodePart();
        } else if (model instanceof ConnectionWrapper) {
            part = new ReadOnlyConnectionPart();
        } else if (model instanceof WorkflowExecutionInformation) {
            part = new WorkflowExecutionInformationPart();
        } else if (model instanceof WorkflowLabel) {
            part = new ReadOnlyWorkflowLabelPart();
        }
        if (part != null) {
            part.setModel(model);
        }
        return part;
    }

}
