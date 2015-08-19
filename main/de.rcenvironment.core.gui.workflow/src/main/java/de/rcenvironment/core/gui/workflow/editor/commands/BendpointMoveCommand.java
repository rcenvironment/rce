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

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;

/**
 * Command that handles bendpoint movement.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointMoveCommand extends Command {
    
    /** Old location of the moved bendpoint. */
    private Location oldLocation;

    /** New location of the moved bendpoint. */
    private Location newLocation;

    /** Index of the bendpoint in the link's bendpoint list. */
    private int index;

    private List<Connection> connectionsInModel = new ArrayList<>();
    
    private List<Connection> connectionsInModelInverse = new ArrayList<>();

    private ConnectionWrapper referencedwrapper;
    
    private WorkflowDescription workflowDescription;
    
    private int oldIndex = 0;
    
    
    /** Move the bendpoint to the new location. */
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
    
    /** Restore the old location of the bendpoint. */
    @Override
    public void undo() {
        for (Connection connection : connectionsInModel){
            connection.setBendpoint(oldIndex, oldLocation.x, oldLocation.y, false);
        }
        for (Connection connection : connectionsInModelInverse){
            connection.setBendpoint(oldIndex, oldLocation.x, oldLocation.y, true);
        }
        ConnectionUtils.validateConnectionWrapperBySameBendpointCount(workflowDescription, referencedwrapper, 
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
        ConnectionUtils.validateConnectionWrapperBySameBendpointCount(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " execute or redo");

    }

    /**
     * Set the index where the bendpoint is located in the bendpoint list.
     * 
     * @param index the index where the bendpoint is located.
     */
    public void setIndex(final int index) {
        this.index = new Integer(index);
    }

    /**
     * Set the new location of the bendpoint.
     * 
     * @param newLocationPoint the new location of the bendpoint.
     */
    public void setLocation(final Point newLocationPoint) {
        this.newLocation = new Location(newLocationPoint.x, newLocationPoint.y);
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
