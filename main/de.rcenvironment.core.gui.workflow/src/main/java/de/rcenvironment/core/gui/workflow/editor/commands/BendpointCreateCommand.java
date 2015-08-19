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
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;

/**
 * Command that handles bendpoint creation.
 * 
 * @author Oliver Seebach
 *
 */
public class BendpointCreateCommand extends Command {
    
    /** Index on which the new bendpoint is added. */
    private int index;

    /** Location of new bendpoint. */
    private Point location;

    private List<Connection> connectionsInModel = new ArrayList<>();
    
    private List<Connection> connectionsInModelInverse = new ArrayList<>();
    
    private ConnectionWrapper referencedwrapper;
    
    private WorkflowDescription workflowDescription;
    
    private int oldIndex = 0;
       
    
    
    
    @Override
    public void execute() {
        oldIndex = index;
        
        redo();
    }

    @Override
    public void undo() {
        for (Connection connection : connectionsInModel){
            connection.removeBendpoint(oldIndex, false);
        }
        for (Connection connection : connectionsInModelInverse){
            connection.removeBendpoint(oldIndex, true);
        }
        ConnectionUtils.validateConnectionWrapperBySameBendpointCount(workflowDescription, referencedwrapper, 
            this.getClass().getSimpleName() + " undo");
    }

    @Override
    public void redo() {
        for (Connection connection : connectionsInModel){
            connection.addBendpoint(index, location.x, location.y, false);
        }
        for (Connection connection : connectionsInModelInverse){
            connection.addBendpoint(index, location.x, location.y, true);
        }
        ConnectionUtils.validateConnectionWrapperBySameBendpointCount(workflowDescription, referencedwrapper, 
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



    /**
     * Set the index on which the bendpoint is added.
     * 
     * @param index Index on which the bendpoint should be added.
     */
    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Set the location where the new bendpoint is added.
     * 
     * @param location point in the diagram where the new bendpoint is added.
     */
    public void setLocation(final Point location) {
        this.location = location;
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
