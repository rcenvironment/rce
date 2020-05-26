/*
 * Copyright 2006-2020 DLR, Germany
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
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " undo");
    }

    @Override
    public void redo() {
        for (Connection connection : connectionsInModel) {
            connection.removeBendpoint(index, false);
        }
        ConnectionUtils.validateConnectionWrapperForEqualBendpointLocations(workflowDescription, referencedwrapper, this.getClass()
            .getSimpleName() + " execute or redo");
    }

}
