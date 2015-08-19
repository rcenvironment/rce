/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;

/**
 * Command that handles bendpoint deletion.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointDeleteCommand extends Command {

    /** Index where the bendpoint is located in the link's bendpoin list. */
    private int index;

    /** Point in the diagram where the bendpoint is located. */
    private Location oldLocation;

    private int oldIndex = 0;

    private List<Connection> connectionsInModel = new ArrayList<>();

    private List<Connection> connectionsInModelInverse = new ArrayList<>();

    private ConnectionWrapper referencedwrapper;

    private WorkflowDescription workflowDescription;


    /**
     * Remove the bendpoint from the link.
     */
    @Override
    public void execute() {

        oldIndex = new Integer(index);
        if (!connectionsInModel.isEmpty()) {
            oldLocation = new Location(connectionsInModel.get(0).getBendpoints().get(oldIndex).x, 
                connectionsInModel.get(0).getBendpoints().get(oldIndex).y);
        } else if (!connectionsInModelInverse.isEmpty()) {
            int adaptedLocationIndex = (connectionsInModelInverse.get(0).getBendpoints().size() - oldIndex - 1);
            oldLocation = new Location(connectionsInModelInverse.get(0).getBendpoints().get(adaptedLocationIndex).x, 
                connectionsInModelInverse.get(0).getBendpoints().get(adaptedLocationIndex).y);
        }
        redo();
    }

    /**
     * Reinsert the bendpoint in the link.
     */
    @Override
    public void undo() {
        for (Connection connection : connectionsInModel) {
            connection.addBendpoint(oldIndex, oldLocation.x, oldLocation.y, false);
        }
        for (Connection connection : connectionsInModelInverse) {
            connection.addBendpoint(oldIndex, oldLocation.x, oldLocation.y, true);
        }
        ConnectionUtils.validateConnectionWrapperBySameBendpointCount(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " undo");
    }

    @Override
    public void redo() {
        for (Connection connection : connectionsInModel) {
            connection.removeBendpoint(oldIndex, false);
        }
        for (Connection connection : connectionsInModelInverse) {
            connection.removeBendpoint(oldIndex, true);
        }
        ConnectionUtils.validateConnectionWrapperBySameBendpointCount(workflowDescription, referencedwrapper, this.getClass()
            .getSimpleName() + " execute or redo");
    }

    /**
     * Set the index of the bendpoint that should be removed.
     * 
     * @param index the index of the bendpoint to remove.
     */
    public void setIndex(final int index) {
        this.index = new Integer(index);
    }

    public List<Connection> getConnectionsInModel() {
        return connectionsInModel;
    }

    public void setConnectionsInModel(List<Connection> connectionsInModel) {
        this.connectionsInModel = connectionsInModel;
    }

    public void setConnectionsInModelInverse(List<Connection> connectionsInModelInverse) {
        this.connectionsInModelInverse = connectionsInModelInverse;
    }

    public void setReferencedModel(ConnectionWrapper referencedModel) {
        this.referencedwrapper = referencedModel;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }
}
