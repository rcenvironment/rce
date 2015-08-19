/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;

/**
 * Command that handles bendpoint movement.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointMoveCommand extends AbstractBendpointCommand {
        
    
    /** Move the bendpoint to the new location. */
    public void execute() {
        if (!connectionsInModel.isEmpty()) {
            oldLocation = new Location(connectionsInModel.get(0).getBendpoints().get(index).x, 
                connectionsInModel.get(0).getBendpoints().get(index).y);
        } else if (!connectionsInModelInverse.isEmpty()) {
            int adaptedLocationIndex = (connectionsInModelInverse.get(0).getBendpoints().size() - index - 1);
            oldLocation = new Location(connectionsInModelInverse.get(0).getBendpoints().get(adaptedLocationIndex).x, 
                connectionsInModelInverse.get(0).getBendpoints().get(adaptedLocationIndex).y);
        }
        
        redo();
    }
    
    /** Restore the old location of the bendpoint. */
    @Override
    public void undo() {
        for (Connection connection : connectionsInModel){
            connection.setBendpoint(index, oldLocation.x, oldLocation.y, false);
        }
        for (Connection connection : connectionsInModelInverse){
            connection.setBendpoint(index, oldLocation.x, oldLocation.y, true);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute");
    }
    
    @Override
    public void redo() {
        for (Connection connection : connectionsInModel){
            connection.setBendpoint(index, newLocation.x, newLocation.y, false);
        }
        for (Connection connection : connectionsInModelInverse){
            connection.setBendpoint(index, newLocation.x, newLocation.y, true);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute or redo");

    }

}
