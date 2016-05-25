/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionBendpointEditPolicy;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionDeletionPolicy;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentPropertySource;

/**
 * EditPart representing all connections between two workflow nodes.
 *
 * @author Heinrich Wendel
 * @author Oliver Seebach
 */
public class ConnectionPart extends AbstractConnectionEditPart implements PropertyChangeListener {

    private Label relationshipLabel;
    
    @Override
    public IFigure createFigure() {

        Polyline figure = (Polyline) super.createFigure();
        PolylineConnection connection = new PolylineConnection();
        connection.setPoints(figure.getPoints());
        ConnectionWrapper wrapper = (ConnectionWrapper) getModel();

        if (wrapper.getSourceArrow()) {
            connection.setSourceDecoration(new PolylineDecoration());
        }

        if (wrapper.getTargetArrow()) {
            connection.setTargetDecoration(new PolylineDecoration());
        }
        
        // skip self connections
        if (!wrapper.getSource().getIdentifier().equals(wrapper.getTarget().getIdentifier())) {
            ConnectionLocator connectionLocator = new ConnectionLocator(connection, ConnectionLocator.MIDDLE);
            
            connectionLocator.setRelativePosition(PositionConstants.NSEW);
            connectionLocator.setGap(5);
            relationshipLabel = new Label(String.valueOf(((ConnectionWrapper) getModel()).getNumberOfConnections()));
            // hide label per default
            relationshipLabel.setVisible(false);
            connection.add(relationshipLabel, connectionLocator);
        }
        
        return connection;
    }

    @Override
    public void activate() {
        super.activate();
        Object model = (((ScalableFreeformRootEditPart) getParent()).getContents()).getModel();
        WorkflowDescription workflowDescription = getWorkflowDescriptionFromModelObject(model);
        for (Connection connection : workflowDescription.getConnections()) {
            connection.addPropertyChangeListener(this);
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        Object model = (((ScalableFreeformRootEditPart) getParent()).getContents()).getModel();
        WorkflowDescription workflowDescription = getWorkflowDescriptionFromModelObject(model);
        for (Connection connection : workflowDescription.getConnections()) {
            connection.removePropertyChangeListener(this);
        }
    }

    @Override
    protected void refreshVisuals() {
        EditPart parent = getParent();
        if (parent != null) {
            EditPartViewer viewer = parent.getViewer();
            EditPart contents = viewer.getContents();
            Object modelObject = contents.getModel();
            WorkflowDescription workflowDescription = getWorkflowDescriptionFromModelObject(modelObject);
            ConnectionWrapper connectionWrapper = (ConnectionWrapper) getModel();

            for (Connection connection : workflowDescription.getConnections()) {
                if ((connectionWrapper.getSource().getIdentifier().equals(connection.getSourceNode().getIdentifier())
                    && connectionWrapper.getTarget().getIdentifier().equals(connection.getTargetNode().getIdentifier()))
                    || (connectionWrapper.getSource().getIdentifier().equals(connection.getTargetNode().getIdentifier())
                    && connectionWrapper.getTarget().getIdentifier().equals(connection.getSourceNode().getIdentifier()))) {
                    addBendpointsFromModelToGraphics(connection);
                    return;
                }
            }
        }
    }
    
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        // Enable Snap to grid/geometry in wf editor for bendpoints..
        if (type == SnapToHelper.class) {
            List<SnapToHelper> helpers = new ArrayList<SnapToHelper>();
            if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED))) {
                helpers.add(new SnapToGeometry(this));
            }
            if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED))) {
                helpers.add(new SnapToGrid(this));
            }
            if (helpers.size() == 0) {
                return null;
            } else {
                return new CompoundSnapToHelper(helpers.toArray(new SnapToHelper[0]));
            }
        } else if (type == IPropertySource.class && getModel() instanceof WorkflowNode) {
            return new ComponentPropertySource(getViewer().getEditDomain().getCommandStack(), (WorkflowNode) getModel());
        }
        return super.getAdapter(type);
    }

    private void addBendpointsFromModelToGraphics(Connection matchingConnection) {
        org.eclipse.draw2d.Connection connection = getConnectionFigure();
        List<AbsoluteBendpoint> figureConstraint = new ArrayList<AbsoluteBendpoint>();
        for (Location location : matchingConnection.getBendpoints()) {
            figureConstraint.add(new AbsoluteBendpoint(new Point(location.x, location.y)));
        }
        connection.setRoutingConstraint(figureConstraint);
    }

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.CONNECTION_ROLE, new ConnectionDeletionPolicy());
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE, new ConnectionBendpointEditPolicy());
    }

    /**
     * Hides connection label.
     */
    public void hideLabel() {
        if (relationshipLabel != null) {
            relationshipLabel.setVisible(false);
        }
    }

    /**
     * Show connection label.
     */
    public void showLabel() {
        if (relationshipLabel != null) {
            relationshipLabel.setVisible(true);
        }
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Connection.PROPERTY_BENDPOINT)) {
            refreshVisuals();
        }
    }
    
    private WorkflowDescription getWorkflowDescriptionFromModelObject(Object modelObject) {
        WorkflowDescription workflowDescription = null;
        if (modelObject instanceof WorkflowDescription) {
            workflowDescription = (WorkflowDescription) modelObject;
        } else if (modelObject instanceof WorkflowExecutionInformationImpl) {
            workflowDescription = ((WorkflowExecutionInformationImpl) modelObject).getWorkflowDescription();
        }
        return workflowDescription;
    }
    
    
}
