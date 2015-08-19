/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * Command to move a WorkflowNode to a new location on the screen.
 *
 * @author Heinrich Wendel
 */
public class WorkflowNodeMoveCommand extends Command {

    /** The workflow node. **/
    private WorkflowNode node;
    
    /** the request. **/
    private ChangeBoundsRequest request;
    
    /** the new location. **/
    private Point newLocation;
    
    /** saved old location. **/
    private Point oldLocation;

    /**
     * Constructor.
     * 
     * @param node The workflow node.
     * @param req The request.
     * @param newBounds The rectangle with the new location.
     */
    public WorkflowNodeMoveCommand(WorkflowNode node, ChangeBoundsRequest req, Rectangle newBounds) {
        this.node = node;
        this.request = req;
        this.newLocation = newBounds.getLocation();
    }
    
    @Override
    public boolean canExecute() {
        Object type = request.getType();
        return (RequestConstants.REQ_MOVE.equals(type) || RequestConstants.REQ_MOVE_CHILDREN.equals(type));
    }
    

    @Override
    public void execute() {
        oldLocation = new Point(node.getX(), node.getY());
        redo();
    }

    @Override
    public void redo() {
        node.setLocation(newLocation.x, newLocation.y);
    }

    @Override
    public void undo() {
        node.setLocation(oldLocation.x, oldLocation.y);
    }
}
