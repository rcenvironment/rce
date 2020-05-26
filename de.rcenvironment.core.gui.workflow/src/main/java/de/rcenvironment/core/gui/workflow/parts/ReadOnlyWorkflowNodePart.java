/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editpolicies.SelectionHandlesEditPolicy;
import org.eclipse.gef.handles.MoveHandle;

import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.handlers.OvalBorderMoveHandle;




/**
 * Read-only EditPart representing a {@link WorkflowNode}.
 * 
 * @author Martin Misiak
 */
public class ReadOnlyWorkflowNodePart extends WorkflowNodePart {

    private static final Log LOGGER = LogFactory.getLog(ReadOnlyWorkflowNodePart.class);

    public ReadOnlyWorkflowNodePart() {}
   
    @Override
    protected void createEditPolicies() {
        
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionHandlesEditPolicy() {

            @Override
            protected List<MoveHandle> createSelectionHandles() {
                List<MoveHandle> list = new ArrayList<>();
                GraphicalEditPart child = (GraphicalEditPart) getHost();
            
                if (child instanceof WorkflowNodePart
                    && ((WorkflowNode) ((WorkflowNodePart) child).getModel()).getComponentDescription().getComponentInstallation()
                        .getComponentInterface().getShape() == ComponentShape.CIRCLE) {
                    list.add(new OvalBorderMoveHandle(child));
                } else {
                    list.add(new MoveHandle(child));
                }
                return list;
            }
        
        });
        
    }
    
    
}
