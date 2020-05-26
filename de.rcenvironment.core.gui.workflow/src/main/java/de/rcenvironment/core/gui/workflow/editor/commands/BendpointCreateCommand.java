/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;

/**
 * Command that handles bendpoint creation.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointCreateCommand extends AbstractBendpointCommand {
    
    
    @Override
    public void execute() {
        redo();
    }

    @Override
    public void undo() {
        for (Connection connection : connectionsInModel){
            connection.removeBendpoint(index, false);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " undo");
    }

    @Override
    public void redo() {
        for (Connection connection : connectionsInModel){
            connection.addBendpoint(index, newLocation.x, newLocation.y, false);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute or redo");
    }
    
    @Override
    public boolean canExecute() {

        if (!connectionsInModel.isEmpty()) {
            if (connectionsInModel.get(0).getSourceNode().getIdentifierAsObject().
                equals(connectionsInModel.get(0).getTargetNode().getIdentifierAsObject())) {
                return false;
            }
        } 
        return true;
    }
  
    
}
