/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
        } 
        redo();
    }
    
    /** Restore the old location of the bendpoint. */
    @Override
    public void undo() {
        for (Connection connection : connectionsInModel){
            connection.setBendpoint(index, oldLocation.x, oldLocation.y, false);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute");
    }
    
    @Override
    public void redo() {
        for (Connection connection : connectionsInModel){
            connection.setBendpoint(index, newLocation.x, newLocation.y, false);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute or redo");

    }

}
