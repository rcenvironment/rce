/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
        for (Connection connection : connectionsInModelInverse){
            connection.removeBendpoint(index, true);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " undo");
    }

    @Override
    public void redo() {
        for (Connection connection : connectionsInModel){
            connection.addBendpoint(index, newLocation.x, newLocation.y, false);
        }
        for (Connection connection : connectionsInModelInverse){
            connection.addBendpoint(index, newLocation.x, newLocation.y, true);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute or redo");
    }
    
    @Override
    public boolean canExecute() {

        if (!connectionsInModel.isEmpty()) {
            if (connectionsInModel.get(0).getSourceNode().getIdentifier().
                equals(connectionsInModel.get(0).getTargetNode().getIdentifier())) {
                return false;
            }
        } else if (!connectionsInModelInverse.isEmpty()) {
            if (connectionsInModelInverse.get(0).getSourceNode().getIdentifier()
                .equals(connectionsInModelInverse.get(0).getTargetNode().getIdentifier())) {
                return false;
            }
        }
        return true;
    }
  
    
}
