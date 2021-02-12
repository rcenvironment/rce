/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;


/**
 * Command to move a WorkflowNode to a new location on the screen.
 *
 * @author Heinrich Wendel
 * @author Oliver Seebach
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
    
    private List<Connection> relatedConnections;
    
    private int deltaX;
    
    private int deltaY;
    
    
    /**
     * Constructor.
     * 
     * @param node The workflow node.
     * @param req The request.
     * @param newBounds The rectangle with the new location.
     * @param relatedConnections The affected connections.
     */
    public WorkflowNodeMoveCommand(WorkflowNode node, ChangeBoundsRequest req, Rectangle newBounds, List<Connection> relatedConnections) {
        this.node = node;
        this.request = req;
        this.newLocation = newBounds.getLocation();
        this.relatedConnections = relatedConnections;
    }
    
    @Override
    public boolean canExecute() {
        Object type = request.getType();
        return (RequestConstants.REQ_MOVE.equals(type) || RequestConstants.REQ_MOVE_CHILDREN.equals(type));
    }
    

    @Override
    public void execute() {
        oldLocation = new Point(node.getX(), node.getY());
        deltaX = (newLocation.x - oldLocation.x);
        deltaY = (newLocation.y - oldLocation.y);
        redo();
    }

    @Override
    public void redo() {
        node.setLocation(newLocation.x, newLocation.y);
        // Shift by half the delta because both nodes will contribute to the shift
        for (Connection connection : relatedConnections){
            connection.setBendpoints(ConnectionUtils.translateBendpointListByOffset(connection.getBendpoints(), (deltaX/2), (deltaY/2)));
        }
    }

    @Override
    public void undo() {
        node.setLocation(oldLocation.x, oldLocation.y);
        // Shift by half the delta because both nodes will contribute to the shift
        for (Connection connection : relatedConnections){
            connection.setBendpoints(ConnectionUtils.translateBendpointListByOffset(connection.getBendpoints(), -(deltaX/2), -(deltaY/2)));
        }
    }
}
