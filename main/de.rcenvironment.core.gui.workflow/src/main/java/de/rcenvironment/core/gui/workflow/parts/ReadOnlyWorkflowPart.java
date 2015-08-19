/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;


/**
 * Readonly root edit part holding a WorkflowDescription.
 *
 * @author Heinrich Wendel
 */
public class ReadOnlyWorkflowPart extends WorkflowPart {

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new RootComponentEditPolicy());
        installEditPolicy(EditPolicy.LAYOUT_ROLE, new XYLayoutEditPolicy() {
            @Override
            protected Command createChangeConstraintCommand(EditPart arg0, Object arg1) {
                return null;
            }

            @Override
            protected Command getCreateCommand(CreateRequest arg0) {
                return null;
            }
        });
    }
    
}
