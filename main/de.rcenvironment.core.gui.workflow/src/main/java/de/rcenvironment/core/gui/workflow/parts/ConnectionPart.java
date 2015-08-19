/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.draw2d.ConnectionEndpointLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;

import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionDeletionPolicy;

/**
 * EditPart representing all connections between two workflow nodes.
 *
 * @author Heinrich Wendel
 * @author Oliver Seebach
 */
public class ConnectionPart extends AbstractConnectionEditPart {

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
        if (wrapper.getSource() != wrapper.getTarget()) {
            UpdatingConnectionEndpointLocator relationshipLocator = new UpdatingConnectionEndpointLocator(connection, true);
            relationshipLabel = new Label(String.valueOf(((ConnectionWrapper) getModel()).getNumberOfConnections()));
            // hide label per default
            relationshipLabel.setVisible(false);
            connection.add(relationshipLabel, relationshipLocator);
        }

        return connection;
    }

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.CONNECTION_ROLE, new ConnectionDeletionPolicy());
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

    /**
     * ConnectionEndpointLocator that updates the label's location.
     * 
     * @author Oliver Seebach
     */
    class UpdatingConnectionEndpointLocator extends ConnectionEndpointLocator {

        private PolylineConnection connection = null;

        public UpdatingConnectionEndpointLocator(PolylineConnection c, boolean isEnd) {
            super(c, isEnd);
            this.connection = c;
        }

        @Override
        public void relocate(IFigure figure) {
            // place label in the middle between start and end point
            double distance = Math.abs(connection.getEnd().getDistance(connection.getStart()));
            this.setUDistance((int) (distance / 2));
            super.relocate(figure);
        }

    }
}
