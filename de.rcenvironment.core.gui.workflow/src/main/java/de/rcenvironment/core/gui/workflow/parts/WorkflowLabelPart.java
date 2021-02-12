/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.ParagraphTextLayout;
import org.eclipse.draw2d.text.TextFlow;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
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
 * @author Marc Stammerjohann
 * @author Doreen Seider
 * @author Jascha Riedel
 */
public class WorkflowLabelPart extends AbstractGraphicalEditPart implements PropertyChangeListener, NodeEditPart {

    private static final int MINUS_ONE = -1;

    private static final int TWELVE = 12;
    
    private Font headerFont = null;
    
    private Font mainFont = null;
    
    private Color headerTextColor = null;
    
    private Color mainTextColor = null;
    
    private Color labelColor = null;
    
    private TransparentLabel containerLabel;
    
    private IFigure textContainer;
    
    private FlowPage headerFlowPage;
    
    private FlowPage mainFlowPage;

    private int currentHeaderTextSize = MINUS_ONE;

    private int currentMainTextSize = MINUS_ONE;
    
    
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
        containerLabel = new TransparentLabel((WorkflowLabel) getModel());
        GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 10;
        gridLayout.numColumns = 1;
        containerLabel.setLayoutManager(gridLayout);
        
        textContainer = new Figure();
        textContainer.setLayoutManager(new GridLayout(1, false) {

            @Override
            public void layout(IFigure container) {
                super.layout(container);
                int availableWidth = container.getClientArea().width - horizontalSpacing * (numColumns - 1)
                    - marginWidth * 2;
                if (container.getClientArea().width + TWELVE + 1 >= containerLabel.getClientArea().width) {
                    Dimension d = mainFlowPage.getPreferredSize(SWT.DEFAULT, SWT.DEFAULT);
                    if (d.width < textContainer.getClientArea().width - horizontalSpacing * (numColumns - 1)
                        - marginWidth * 2) {
                        ((GridData) getConstraint(mainFlowPage)).widthHint = SWT.DEFAULT;
                    } else {
                        ((GridData) getConstraint(mainFlowPage)).widthHint = availableWidth;
                    }
                    if (textContainer.getChildren().contains(headerFlowPage)) {
                        Dimension dd = headerFlowPage.getPreferredSize(SWT.DEFAULT, SWT.DEFAULT);
                        if (dd.width < textContainer.getClientArea().width - horizontalSpacing * (numColumns - 1)
                            - marginWidth * 2) {
                            ((GridData) getConstraint(headerFlowPage)).widthHint = SWT.DEFAULT;
                        } else {
                            ((GridData) getConstraint(headerFlowPage)).widthHint = availableWidth;
                        }
                    }
                } else {
                    ((GridData) getConstraint(mainFlowPage)).widthHint = SWT.DEFAULT;
                    ((GridData) getConstraint(headerFlowPage)).widthHint = SWT.DEFAULT;
                }
            }
            
        });
        GridData textContainerGD = new GridData();
        textContainerGD.grabExcessHorizontalSpace = true;
        textContainerGD.grabExcessVerticalSpace = true;
        textContainerGD.horizontalAlignment = GridData.CENTER;
        textContainerGD.verticalAlignment = GridData.CENTER;
        containerLabel.add(textContainer, textContainerGD);

        mainFlowPage = new FlowPage();
        TextFlow mainText = new TextFlow();
        mainText.setLayoutManager(new ParagraphTextLayout(mainText, ParagraphTextLayout.WORD_WRAP_SOFT));
        mainFlowPage.add(mainText);

        headerFlowPage = new FlowPage();
        TextFlow headerText = new TextFlow();
        headerText.setLayoutManager(new ParagraphTextLayout(headerText, ParagraphTextLayout.WORD_WRAP_SOFT));
        headerFlowPage.add(headerText);
       
        GridData gridDataHeader = new GridData();
        GridData gridDataMain = new GridData();
        gridDataMain.grabExcessHorizontalSpace = false;
        gridDataMain.grabExcessVerticalSpace = false;
        gridDataMain.horizontalAlignment = SWT.FILL;
        textContainer.add(headerFlowPage, gridDataHeader, 0);
        textContainer.add(mainFlowPage, gridDataMain, 1);
        
        refreshSettings((WorkflowLabel) getModel());
        return containerLabel;
    }
    
    @Override
    protected void refreshVisuals() {
        WorkflowLabel label = (WorkflowLabel) getModel();
        Point loc = new Point(label.getX(), label.getY());
        containerLabel.setLocation(loc);
        ((GraphicalEditPart) getParent()).setLayoutConstraint(this, containerLabel, 
            new Rectangle(loc, new Dimension(label.getWidth(), label.getHeight())));
        refreshSettings(label);
        figure.repaint();
        // This is a dirty fix to layout the flowpages correctly. 
        refreshSettings(label);
        figure.repaint();
        refreshSettings(label);
        figure.repaint();
    }
    
    protected void refreshSettings(WorkflowLabel label) {
        refreshContainerLabel(label);
        textContainer.revalidate();
        refreshMainText(label, (TextFlow) mainFlowPage.getChildren().get(0));
        refreshHeaderText(label, (TextFlow) headerFlowPage.getChildren().get(0));
        refreshPositionOfText(label);
        textAlignHeaderPage(label);
        textAlignMainPage(label);
        textContainer.getUpdateManager().performUpdate();
    }

    protected void refreshContainerLabel(WorkflowLabel label) {
        if (labelColor == null) {
            labelColor = new Color(null, label.getColorBackground()[0], label.getColorBackground()[1], label.getColorBackground()[2]);
        } else {
            if ((labelColor.getRed() != label.getColorBackground()[0])
                || (labelColor.getGreen() != label.getColorBackground()[1])
                || (labelColor.getBlue() != label.getColorBackground()[2])) {
                labelColor.dispose();
                labelColor = new Color(null, label.getColorBackground()[0], label.getColorBackground()[1], label.getColorBackground()[2]);
            }
        }
        containerLabel.setBackgroundColor(labelColor);
        if (label.hasBorder()) {
            containerLabel.setBorder(new LineBorder());
        } else {
            containerLabel.setBorder(null);
        }
    }
    
    private void refreshPositionOfText(WorkflowLabel label) {
        GridData textContainerGD = (GridData) containerLabel.getLayoutManager().getConstraint(textContainer);
        // Position of text in Label
        switch (label.getLabelPosition()) {
        case TOPLEFT:
            textContainerGD.verticalAlignment = GridData.BEGINNING;
            textContainerGD.horizontalAlignment = GridData.BEGINNING;
            break;
        case TOPCENTER:
            textContainerGD.verticalAlignment = GridData.BEGINNING;
            textContainerGD.horizontalAlignment = GridData.CENTER;
            break;
        case TOPRIGHT:
            textContainerGD.verticalAlignment = GridData.BEGINNING;
            textContainerGD.horizontalAlignment = GridData.END;
            break;
        case CENTERLEFT:
            textContainerGD.verticalAlignment = GridData.CENTER;
            textContainerGD.horizontalAlignment = GridData.BEGINNING;
            break;
        case CENTER:
            textContainerGD.verticalAlignment = GridData.CENTER;
            textContainerGD.horizontalAlignment = GridData.CENTER;
            break;
        case CENTERRIGHT:
            textContainerGD.verticalAlignment = GridData.CENTER;
            textContainerGD.horizontalAlignment = GridData.END;
            break;
        case BOTTOMLEFT:
            textContainerGD.verticalAlignment = GridData.END;
            textContainerGD.horizontalAlignment = GridData.BEGINNING;
            break;
        case BOTTOMCENTER:
            textContainerGD.verticalAlignment = GridData.END;
            textContainerGD.horizontalAlignment = GridData.CENTER;
            break;
        case BOTTOMRIGHT:
            textContainerGD.verticalAlignment = GridData.END;
            textContainerGD.horizontalAlignment = GridData.END;
            break;
        default:
            textContainerGD.verticalAlignment = GridData.BEGINNING;
            textContainerGD.horizontalAlignment = GridData.BEGINNING;
            break;
        }
    }
    
    private void textAlignHeaderPage(WorkflowLabel label) {
        switch (label.getHeaderAlignmentType()) {
        case LEFT:
            headerFlowPage.setHorizontalAligment(PositionConstants.LEFT);
            ((GridData) textContainer.getLayoutManager().getConstraint(headerFlowPage)).horizontalAlignment = GridData.BEGINNING;
            break;
        case CENTER:
            headerFlowPage.setHorizontalAligment(PositionConstants.CENTER);
            ((GridData) textContainer.getLayoutManager().getConstraint(headerFlowPage)).horizontalAlignment = GridData.CENTER;
            break;
        case RIGHT:
            headerFlowPage.setHorizontalAligment(PositionConstants.RIGHT);
            ((GridData) textContainer.getLayoutManager().getConstraint(headerFlowPage)).horizontalAlignment = GridData.END;
            break;
        default:
            headerFlowPage.setHorizontalAligment(PositionConstants.LEFT);
            ((GridData) textContainer.getLayoutManager().getConstraint(headerFlowPage)).horizontalAlignment = GridData.BEGINNING;
            break;
        }
    }
    
    private void textAlignMainPage(WorkflowLabel label) {
        switch (label.getTextAlignmentType()) {
        case LEFT:
            mainFlowPage.setHorizontalAligment(PositionConstants.LEFT);
            break;
        case CENTER:
            mainFlowPage.setHorizontalAligment(PositionConstants.CENTER);
            break;
        case RIGHT:
            mainFlowPage.setHorizontalAligment(PositionConstants.RIGHT);
            break;
        default:
            mainFlowPage.setHorizontalAligment(PositionConstants.LEFT);
            break;
        }
    }
    
    
    protected  void refreshHeaderText(WorkflowLabel label, TextFlow textFlow) {
        if (label.getHeaderText().isEmpty() 
            && textContainer.getChildren().contains(headerFlowPage)) {
            textContainer.remove(headerFlowPage);
            return;
        } else if (label.getHeaderText().isEmpty()) {
            return;
        } else {
            if (!textContainer.getChildren().contains(headerFlowPage)) {
                textContainer.add(headerFlowPage, 0);
            }
        }
        textFlow.setText(label.getHeaderText());
        if (label.getHeaderTextSize() != currentHeaderTextSize
            && textFlow.getFont() != null && textFlow.getFont().getFontData().length > 0) {
            FontData[] fD = textFlow.getFont().getFontData();
            int size = label.getHeaderTextSize();
            fD[0].setHeight(size);
            currentHeaderTextSize = size;
            if (headerFont != null) {
                headerFont.dispose();
            }
            headerFont = new Font(Display.getDefault(), fD[0]);
            textFlow.setFont(headerFont);
        }
        if (headerTextColor == null) {
            headerTextColor = new Color(null, label.getColorHeader()[0], label.getColorHeader()[1], label.getColorHeader()[2]);
        } else {
            if ((headerTextColor.getRed() != label.getColorHeader()[0])
                || (headerTextColor.getGreen() != label.getColorHeader()[1])
                || (headerTextColor.getBlue() != label.getColorHeader()[2])) {
                headerTextColor.dispose();
                headerTextColor = new Color(null, label.getColorHeader()[0], label.getColorHeader()[1], label.getColorHeader()[2]);
            }
        }
        textFlow.setForegroundColor(headerTextColor);
    }
    
    protected void refreshMainText(WorkflowLabel label, TextFlow textFlow) {
        textFlow.setText(label.getText());
        if (label.getTextSize() != currentMainTextSize
            && textFlow.getFont() != null && textFlow.getFont().getFontData().length > 0) {
            FontData[] fD = textFlow.getFont().getFontData();
            int size = label.getTextSize();
            fD[0].setHeight(size);
            currentMainTextSize = size;
            if (mainFont != null) {
                mainFont.dispose();
            }
            mainFont = new Font(Display.getDefault(), fD[0]);
            textFlow.setFont(mainFont);
        }
        if (mainTextColor == null) {
            mainTextColor = new Color(null, label.getColorText()[0], label.getColorText()[1], label.getColorText()[2]);
        } else {
            if ((mainTextColor.getRed() != label.getColorText()[0])
                || (mainTextColor.getGreen() != label.getColorText()[1])
                || (mainTextColor.getBlue() != label.getColorText()[2])) {
                mainTextColor.dispose();
                mainTextColor = new Color(null, label.getColorText()[0], label.getColorText()[1], label.getColorText()[2]);
            }
        }
        textFlow.setForegroundColor(mainTextColor);
        textFlow.revalidate();
    }
    

    /**
     * A label that can be transparent.
     * 
     * @author Sascha Zur
     */
    protected class TransparentLabel extends Label {

        private static final int ROUNDED_RECTANGLE_SIZE = 10;

        private final WorkflowLabel label;

        public TransparentLabel(WorkflowLabel label) {
            super();
            this.label = label;
        }

        @Override
        protected void paintFigure(Graphics graphics) {
            int oldAlpha = graphics.getAlpha();
            int[] colorBackground = ((WorkflowLabel) getModel()).getColorBackground();
            //FIXME: resource leak: The color object is never disposed!
            graphics.setBackgroundColor(new Color(null, colorBackground[0], colorBackground[1], colorBackground[2]));
            graphics.setAlpha(label.getAlpha());
            graphics.fillRoundRectangle(getBounds(), ROUNDED_RECTANGLE_SIZE, ROUNDED_RECTANGLE_SIZE);
            graphics.setAlpha(oldAlpha);
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
        if (mainFont != null) {
            mainFont.dispose();
        }
        if (headerFont != null) {
            headerFont.dispose();
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
