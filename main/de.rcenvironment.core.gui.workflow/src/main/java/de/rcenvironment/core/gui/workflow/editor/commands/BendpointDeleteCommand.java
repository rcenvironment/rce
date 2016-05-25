/*
 * Copyright (C) 2006-2016 DLR, Germany
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
 * Command that handles bendpoint deletion.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointDeleteCommand extends AbstractBendpointCommand {

    /**
     * Remove the bendpoint from the connection.
     */
    @Override
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

    /**
     * Reinsert the bendpoint in the connection.
     */
    @Override
    public void undo() {
        for (Connection connection : connectionsInModel) {
            connection.addBendpoint(index, oldLocation.x, oldLocation.y, false);
        }
        for (Connection connection : connectionsInModelInverse) {
            connection.addBendpoint(index, oldLocation.x, oldLocation.y, true);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " undo");
    }

    @Override
    public void redo() {
        for (Connection connection : connectionsInModel) {
            connection.removeBendpoint(index, false);
        }
        for (Connection connection : connectionsInModelInverse) {
            connection.removeBendpoint(index, true);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, this.getClass()
            .getSimpleName() + " execute or redo");
    }

}
