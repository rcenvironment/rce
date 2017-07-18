/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editpolicies.BendpointEditPolicy;
import org.eclipse.gef.handles.BendpointCreationHandle;
import org.eclipse.gef.handles.BendpointMoveHandle;
import org.eclipse.gef.requests.BendpointRequest;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;
import de.rcenvironment.core.gui.workflow.parts.CustomConnectionBendpointTracker;

/**
 * Policy to allow for adding bendpoints in connections.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionBendpointEditPolicy extends BendpointEditPolicy {

    private static final Log LOGGER = LogFactory.getLog(ConnectionBendpointEditPolicy.class);

    @Override
    protected List createSelectionHandles() {

        List listOfHandles = super.createSelectionHandles();

        // Custom bendpoint tracker required to enable snap to grid for bendpoints
        for (Object selectionHandle : listOfHandles) {
            if (selectionHandle instanceof BendpointCreationHandle){
                BendpointCreationHandle bendpointCreationHandle = (BendpointCreationHandle) selectionHandle;
                int index = bendpointCreationHandle.getIndex();
                CustomConnectionBendpointTracker tracker = new CustomConnectionBendpointTracker((ConnectionEditPart) getHost(), index);
                tracker.setType(RequestConstants.REQ_CREATE_BENDPOINT);
                bendpointCreationHandle.setDragTracker(tracker);
            } else if (selectionHandle instanceof BendpointMoveHandle){
                BendpointMoveHandle bendpointMoveHandle = (BendpointMoveHandle) selectionHandle;
                int index = bendpointMoveHandle.getIndex();
                CustomConnectionBendpointTracker tracker = new CustomConnectionBendpointTracker((ConnectionEditPart) getHost(), index);
                tracker.setType(RequestConstants.REQ_MOVE_BENDPOINT);
                bendpointMoveHandle.setDragTracker(tracker);
            }
        }

        return listOfHandles;
    }

    @Override
    protected Command getCreateBendpointCommand(BendpointRequest request) {      
        EditPart parent = request.getSource().getParent();
        if (parent == null) {
            return null;
        }
        Object model = ((ScalableFreeformRootEditPart) parent).getContents().getModel();
        if (model instanceof WorkflowDescription) {
            List<Connection> connections =
                getMatchingConnectionModelsForConnectionWrapper((ConnectionWrapper) request.getSource().getModel());
            List<Connection> connectionsInverse =
                getMatchingInverseConnectionModelsForConnectionWrapper((ConnectionWrapper) request.getSource().getModel());
            BendpointCreateCommand command = new BendpointCreateCommand();
            Point translatedPoint = getTranslatedPointByViewersOffset(request);
            command.setConnectionsInModel(connections);
            command.setConnectionsInModelInverse(connectionsInverse);
            command.setReferencedwrapper((ConnectionWrapper) request.getSource().getModel());
            command.setWorkflowDescription((WorkflowDescription) model);
            command.setNewLocation(translatedPoint);
            command.setIndex(request.getIndex());
            return command;
        } else {
            return null;
        }

    }

    @Override
    protected Command getDeleteBendpointCommand(BendpointRequest request) {
        EditPart parent = request.getSource().getParent();
        if (parent == null) {
            return null;
        }
        Object model = ((ScalableFreeformRootEditPart) parent).getContents().getModel();
        if (model instanceof WorkflowDescription) {
            List<Connection> connections =
                getMatchingConnectionModelsForConnectionWrapper((ConnectionWrapper) request.getSource().getModel());
            List<Connection> connectionsInverse =
                getMatchingInverseConnectionModelsForConnectionWrapper((ConnectionWrapper) request.getSource().getModel());
            BendpointDeleteCommand command = new BendpointDeleteCommand();
            command.setConnectionsInModel(connections);
            command.setConnectionsInModelInverse(connectionsInverse);
            command.setReferencedwrapper((ConnectionWrapper) request.getSource().getModel());
            command.setWorkflowDescription((WorkflowDescription) model);
            command.setIndex(request.getIndex());
            return command;
        } else {
            return null;
        }
    }




    
    @Override
    protected Command getMoveBendpointCommand(BendpointRequest request) {
        EditPart parent = request.getSource().getParent();
        if (parent == null) {
            return null;
        }
        Object model = ((ScalableFreeformRootEditPart) parent).getContents().getModel();
        if (model instanceof WorkflowDescription) {
            List<Connection> connections =
                getMatchingConnectionModelsForConnectionWrapper((ConnectionWrapper) request.getSource().getModel());
            List<Connection> connectionsInverse =
                getMatchingInverseConnectionModelsForConnectionWrapper((ConnectionWrapper) request.getSource().getModel());
            BendpointMoveCommand command = new BendpointMoveCommand();
            Point translatedPoint = getTranslatedPointByViewersOffset(request);
            command.setConnectionsInModel(connections);
            command.setConnectionsInModelInverse(connectionsInverse);
            command.setReferencedwrapper((ConnectionWrapper) request.getSource().getModel());
            command.setWorkflowDescription((WorkflowDescription) model);
            command.setNewLocation(translatedPoint);
            command.setIndex(request.getIndex());
            return command;
        } else {
            return null;
        }
    }

    // Returns the connection within a connection wrapper that lead from connection wrappers source to target
    private List<Connection> getMatchingConnectionModelsForConnectionWrapper(ConnectionWrapper connectionWrapper) {
        List<Connection> connections = new ArrayList<>();
        Object parent = getHost().getParent().getViewer().getContents().getModel();
        WorkflowDescription description = null;
        if (parent instanceof WorkflowDescription) {
            description = (WorkflowDescription) parent;
            for (Connection connectionInModel : description.getConnections()) {
                if (connectionWrapper.getSource().getIdentifier().equals(connectionInModel.getSourceNode().getIdentifier())
                    && connectionWrapper.getTarget().getIdentifier().equals(connectionInModel.getTargetNode().getIdentifier())) {
                    connections.add(connectionInModel);
                }
            }
        } else {
            LOGGER.debug("Model's type is not WorkflowDescription, but " + parent.getClass());
        }
        return connections;
    }

    // Returns the connection within a connection wrapper that lead from connection wrappers target to source (thus called inverse)
    private List<Connection> getMatchingInverseConnectionModelsForConnectionWrapper(ConnectionWrapper connectionWrapper) {
        List<Connection> connections = new ArrayList<>();
        Object parent = getHost().getParent().getViewer().getContents().getModel();
        WorkflowDescription description = null;
        if (parent instanceof WorkflowDescription) {
            description = (WorkflowDescription) parent;
            for (Connection connectionInModel : description.getConnections()) {
                if (connectionWrapper.getSource().getIdentifier().equals(connectionInModel.getTargetNode().getIdentifier())
                    && connectionWrapper.getTarget().getIdentifier().equals(connectionInModel.getSourceNode().getIdentifier())) {
                    connections.add(connectionInModel);
                }
            }
        } else {
            LOGGER.debug("Model's type is not WorkflowDescription, but " + parent.getClass());
        }
        return connections;
    }

    // Return the location for a request translated by the offset of the viewer caused by scrollbars
    private Point getTranslatedPointByViewersOffset(BendpointRequest request) {
        Point offsetPoint = ((FigureCanvas) ((ScalableFreeformRootEditPart) request.getSource().getParent())
            .getContents().getViewer().getControl()).getViewport().getViewLocation();
        double zoomLevel = ((ScalableFreeformRootEditPart) request.getSource().getParent()).getZoomManager().getZoom();
        int coordinateWithOffsetAndZoomX = (int) ((request.getLocation().x + offsetPoint.x) / zoomLevel);
        int coordinateWithOffsetAndZoomY = (int) ((request.getLocation().y + offsetPoint.y) / zoomLevel);
        return new Point(coordinateWithOffsetAndZoomX, coordinateWithOffsetAndZoomY);
    }
}
