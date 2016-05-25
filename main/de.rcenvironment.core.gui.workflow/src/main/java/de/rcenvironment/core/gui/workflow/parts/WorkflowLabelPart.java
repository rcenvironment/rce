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

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel.AlignmentType;
import de.rcenvironment.core.gui.workflow.editor.properties.LabelPropertySource;

/**
 * GUI element of the {@link WorkflowLabel}.
 * 
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author Doreen Seider
 */
public class WorkflowLabelPart extends AbstractGraphicalEditPart implements PropertyChangeListener, NodeEditPart {

    private static final String SEPARATOR = " ";
    
    private Font customFont = null;

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
        TransparentLabel labelFigure = new TransparentLabel("", (WorkflowLabel) getModel());
        setupLabel((WorkflowLabel) getModel(), labelFigure);
        return labelFigure;
    }
    
    private void setupLabel(WorkflowLabel label, Label labelFigure) {
        String labelText = label.getText();
        labelFigure.setOpaque(true);
        labelFigure.setTextAlignment(label.getAlignmentType().getTextAlignment());
        if (label.getAlignmentType().equals(AlignmentType.TOPLEFT) || label.getAlignmentType().equals(AlignmentType.CENTERLEFT)
            || label.getAlignmentType().equals(AlignmentType.BOTTOMLEFT)) {
            labelText = SEPARATOR + labelText;
        } else if (label.getAlignmentType().equals(AlignmentType.TOPRIGHT) || label.getAlignmentType().equals(AlignmentType.CENTERRIGHT)
            || label.getAlignmentType().equals(AlignmentType.BOTTOMRIGHT)) {
            labelText = labelText + SEPARATOR;
        }
        labelFigure.setText(labelText);
        if (label.getTextSize() != WorkflowLabel.DEFAULT_FONT_SIZE
            && labelFigure.getFont() != null && labelFigure.getFont().getFontData().length > 0) {
            FontData[] fD = labelFigure.getFont().getFontData();
            fD[0].setHeight(label.getTextSize());
            if (customFont != null) {
                customFont.dispose();
            }
            customFont = new Font(Display.getDefault(), fD[0]);
            labelFigure.setFont(customFont);
        }
        labelFigure.setLabelAlignment(label.getAlignmentType().getLabelAlignment());
        int[] colorForeground = label.getColorText();
        labelFigure.setForegroundColor(new Color(null, colorForeground[0], colorForeground[1], colorForeground[2]));
        int[] colorBackground = label.getColorBackground();
        labelFigure.setBackgroundColor(new Color(null, colorBackground[0], colorBackground[1], colorBackground[2]));
        if (label.hasBorder()) {
            labelFigure.setBorder(new LineBorder(1));            
        } else {
            labelFigure.setBorder(new AbstractFlowBorder() {
    
                @Override
                public boolean isOpaque() {
                    return false;
                }
                
            });
        }
        
    }
    
    @Override
    protected void refreshVisuals() {
        WorkflowLabel label = (WorkflowLabel) getModel();
        Label labelFigure = (Label) getFigure();
        setupLabel(label, labelFigure);
        Point loc = new Point(label.getX(), label.getY());
        labelFigure.setLocation(loc);
        ((GraphicalEditPart) getParent()).setLayoutConstraint(this, labelFigure, 
            new Rectangle(loc, new Dimension(label.getWidth(), label.getHeight())));
        figure.repaint();
    }

    /**
     * A label that can be transparent.
     * 
     * @author Sascha Zur
     */
    protected class TransparentLabel extends Label {

        private static final int ROUNDED_RECTANGLE_SIZE = 10;

        private final WorkflowLabel label;

        public TransparentLabel(String text, WorkflowLabel label) {
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
        if (customFont != null) {
            customFont.dispose();
        }

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
