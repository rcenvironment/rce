/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;


/**
 * Abstract super class for commands regarding bendpoints.
 *
 * @author Oliver Seebach
 */
public abstract class AbstractBendpointCommand extends Command {

    /** Index where the bendpoint is located in the connection's bendpoint list. */
    protected int index;
    
    /** Old location of the bendpoint. */
    protected Location oldLocation;

    /** New location of the bendpoint. */
    protected Location newLocation;
    
    /** The list of affected connectios from source to target. */
    protected List<Connection> connectionsInModel = new ArrayList<>();
    
    /** The list of affected connectios from target to source. */
    protected List<Connection> connectionsInModelInverse = new ArrayList<>();
    
    /** The respective workflow description. */
    protected WorkflowDescription workflowDescription;
    
    /** The referenced connection wrapper. */
    protected ConnectionWrapper referencedwrapper;

    
    public void setIndex(int index) {
        this.index = new Integer(index);
    }

    
    public void setNewLocation(Point newLocation) {
        this.newLocation = new Location(newLocation.x, newLocation.y);
    }

    
    public void setConnectionsInModel(List<Connection> connectionsInModel) {
        this.connectionsInModel = connectionsInModel;
    }

    
    public void setConnectionsInModelInverse(List<Connection> connectionsInModelInverse) {
        this.connectionsInModelInverse = connectionsInModelInverse;
    }

    
    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    
    public void setReferencedwrapper(ConnectionWrapper referencedwrapper) {
        this.referencedwrapper = referencedwrapper;
    }
    
}
