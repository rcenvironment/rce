/*
 * Copyright (C) 2006-2014 DLR, Germany
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

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.AbstractFlowBorder;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.gui.workflow.editor.properties.LabelPropertySource;

/**
 * GUI element of the {@link WorkflowLabel}.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabelPart extends AbstractGraphicalEditPart implements PropertyChangeListener, NodeEditPart {

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(Request request) {
        return new ChopboxAnchor(getFigure());
    }

    @Override
    public void performRequest(Request req) {
        if (req.getType().equals(RequestConstants.REQ_OPEN)) {
            try {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.ui.views.PropertySheet");
            } catch (PartInitException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    protected void createEditPolicies() {
        installEditPolicy("Snap Feedback", new SnapFeedbackPolicy());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (WorkflowLabel.PROPERTY_CHANGE.equals(prop)) {
            refreshVisuals();
        }
    }

    @Override
    protected IFigure createFigure() {
        final String labelText = ((WorkflowLabel) getModel()).getText();
        Label newLabel = new TranparentLabel("", (WorkflowLabel) getModel());
        newLabel.setText(labelText);
        newLabel.setOpaque(true);
        int[] colorForeground = ((WorkflowLabel) getModel()).getColorText();
        newLabel.setForegroundColor(new Color(null, colorForeground[0], colorForeground[1], colorForeground[2]));
        int[] colorBackground = ((WorkflowLabel) getModel()).getColorBackground();
        newLabel.setBackgroundColor(new Color(null, colorBackground[0], colorBackground[1], colorBackground[2]));
        newLabel.setBorder(new AbstractFlowBorder() {

            @Override
            public boolean isOpaque() {
                return false;
            }
        });
        return newLabel;
    }

    @Override
    protected void refreshVisuals() {
        Point loc = new Point(((WorkflowLabel) getModel()).getX(), ((WorkflowLabel) getModel()).getY());
        final Label labelFigure = (Label) getFigure();
        labelFigure.setLocation(loc);
        labelFigure.setOpaque(true);
        labelFigure.setText(((WorkflowLabel) getModel()).getText());
        int[] colorForeground = ((WorkflowLabel) getModel()).getColorText();
        labelFigure.setForegroundColor(new Color(null, colorForeground[0], colorForeground[1], colorForeground[2]));
        int[] colorBackground = ((WorkflowLabel) getModel()).getColorBackground();
        labelFigure.setBackgroundColor(new Color(null, colorBackground[0], colorBackground[1], colorBackground[2]));
        ((GraphicalEditPart) getParent())
            .setLayoutConstraint(this, labelFigure, new Rectangle(loc, ((WorkflowLabel) getModel()).getSize()));

    }

    /**
     * A label that can be transparent.
     * 
     * @author Sascha Zur
     */
    private class TranparentLabel extends Label {

        private static final int ROUNDED_RECTANGLE_SIZE = 10;

        private final WorkflowLabel label;

        public TranparentLabel(String text, WorkflowLabel label) {
            super(text);
            this.label = label;
        }

        @Override
        protected void paintFigure(Graphics graphics) {
            int oldAlpha = graphics.getAlpha();
            int[] color = ((WorkflowLabel) getModel()).getColorText();
            graphics.setForegroundColor(new Color(null, color[0], color[1], color[2]));
            int[] colorBackground = ((WorkflowLabel) getModel()).getColorBackground();
            graphics.setBackgroundColor(new Color(null, colorBackground[0], colorBackground[1], colorBackground[2]));
            graphics.setAlpha(label.getAlpha());
            graphics.fillRoundRectangle(getBounds(), ROUNDED_RECTANGLE_SIZE, ROUNDED_RECTANGLE_SIZE);
            graphics.setAlpha(oldAlpha);
            graphics.drawText(getText(), getTextBounds().x, getTextBounds().y);
        }

    }

    @Override
    public void activate() {
        super.activate();
        ((PropertiesChangeSupport) getModel()).addPropertyChangeListener(this);

    }

    @Override
    public void deactivate() {
        super.deactivate();
        ((PropertiesChangeSupport) getModel()).removePropertyChangeListener(this);

    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySource.class) {
            return new LabelPropertySource(getViewer().getEditDomain().getCommandStack(), (WorkflowLabel) getModel());
        }
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
        }
        return super.getAdapter(type);
    }

}
