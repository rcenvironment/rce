/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Factory responsible for creating the EditParts.
 * 
 * @author Heinrich Wendel
 */
public class WorkflowEditorEditPartFactory implements EditPartFactory {

    @Override
    public EditPart createEditPart(EditPart context, Object model) {
        EditPart part = null;
        if (model instanceof WorkflowDescription) {
            part = new WorkflowPart();
        } else if (model instanceof WorkflowNode) {
            part = new WorkflowNodePart();
        } else if (model instanceof ConnectionWrapper) {
            part = new ConnectionPart();
        } else if (model instanceof WorkflowLabel) {
            part = new WorkflowLabelPart();
        }
        if (part != null) {
            part.setModel(model);
        }
        return part;
    }

}
