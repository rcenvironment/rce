/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editpolicies.SelectionHandlesEditPolicy;
import org.eclipse.gef.handles.MoveHandle;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;

/**
 * Read-only EditPart representing a {@link WorkflowLabel}.
 * @author Martin Misiak
 */
public class ReadOnlyWorkflowLabelPart extends WorkflowLabelPart {

    @Override
    protected void createEditPolicies() {
        
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionHandlesEditPolicy() {
            
            @Override
            protected List<MoveHandle> createSelectionHandles() {
                List<MoveHandle> list = new ArrayList<>();
                list.add(new MoveHandle((GraphicalEditPart) getHost()));
                return list;
            }     
        }); 
    }

}
