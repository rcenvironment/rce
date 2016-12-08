/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;


/**
 * Read-only root edit part holding a {@link WorkflowDescription}.
 * 
 * @author Martin Misiak
 */
public class ReadOnlyWorkflowPart extends WorkflowPart {

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new RootComponentEditPolicy());
    }
        
}
