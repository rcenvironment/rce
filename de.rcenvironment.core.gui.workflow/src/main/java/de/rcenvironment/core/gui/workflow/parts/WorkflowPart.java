/*
 * Copyright 2006-2022 DLR, Germany
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.handles.MoveHandle;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.WorkflowNodeLabelConnectionHelper;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowLabelMoveCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeMoveCommand;
import de.rcenvironment.core.gui.workflow.editor.handlers.OvalBorderMoveHandle;

/**
 * Part holding a {@link WorkflowDescription}.
 * 
 * @author Heinrich Wendel
 * @author Sascha Zur
 * @author Oliver Seebach
 * @author Jascha Riedel (sortWorkflowLabels)
 * 
 */
public class WorkflowPart extends AbstractGraphicalEditPart implements PropertyChangeListener, IAdaptable {

    private final List<ConnectionWrapper> connections = new ArrayList<ConnectionWrapper>();

    @Override
    public final void activate() {
        super.activate();
        ((PropertiesChangeSupport) getModel()).addPropertyChangeListener(this);
        updateConnectionWrappers();
        propertyChange(new PropertyChangeEvent(this, WorkflowDescription.PROPERTY_CONNECTIONS, null, null));
        propertyChange(new PropertyChangeEvent(this, WorkflowDescription.PROPERTY_NODES, null, null));
        propertyChange(new PropertyChangeEvent(this, WorkflowDescription.PROPERTY_LABEL, null, null));
    }

    @Override
    public final void deactivate() {
        super.deactivate();
        ((PropertiesChangeSupport) getModel()).removePropertyChangeListener(this);
    }

    @Override
    public final void propertyChange(final PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (WorkflowDescription.PROPERTY_CONNECTIONS.equals(prop)) {
            updateConnectionWrappers();
            for (Object part : getChildren()) {
                if (part instanceof WorkflowNodePart) {
                    ((WorkflowNodePart) part).refreshConnections();

                    // refresh labels
                    refreshConnectionLabels(part);
                }
            }
        } else if (WorkflowDescription.PROPERTY_NODES.equals(prop)) {
            ((WorkflowDescription) getModel()).sortWorkflowNodesByZIndex();
            refreshChildren();
        } else if (WorkflowDescription.PROPERTY_LABEL.equals(prop)) {
            ((WorkflowDescription) getModel()).sortWorkflowLabelsByZIndex();
            refreshChildren();
        }
    }

    private void refreshConnectionLabels(Object part) {
        boolean labelsShown = Activator.getInstance().getPreferenceStore()
            .getBoolean(WorkflowEditor.SHOW_LABELS_PREFERENCE_KEY);
        List<ConnectionPart> sourceAndTargetConnectionParts = new ArrayList<>();
        sourceAndTargetConnectionParts.addAll(((WorkflowNodePart) part).getTargetConnections());
        sourceAndTargetConnectionParts.addAll(((WorkflowNodePart) part).getSourceConnections());
        for (ConnectionPart connectionPart : sourceAndTargetConnectionParts) {
            if (labelsShown) {
                connectionPart.showLabel();
            } else {
                connectionPart.hideLabel();
            }
        }
    }

    /**
     * Getter class called by the children to get all connections represented by {@link ConnectionWrapper}s.
     * 
     * @return List of {@link ConnectionWrapper}s.
     */
    public final List<ConnectionWrapper> getConnections() {
        return connections;
    }

    @Override
    protected List<PropertiesChangeSupport> getModelChildren() {
        List<PropertiesChangeSupport> combinedChildren = new ArrayList<PropertiesChangeSupport>();
        combinedChildren.addAll(((WorkflowDescription) getModel()).getWorkflowLabels());
        combinedChildren.addAll(((WorkflowDescription) getModel()).getWorkflowNodes());
        return combinedChildren;
    }

    /**
     * Helper method to update all {@link ConnectionWrapper}s when a {@link Connection} was added or removed.
     */
    private void updateConnectionWrappers() {

        connections.clear();

        List<Connection> connectionsInModel = ((WorkflowDescription) getModel()).getConnections();

        for (Connection c : connectionsInModel) {

            boolean alreadyPresent = false;
            for (ConnectionWrapper connectionWrapper : connections) {
                if (connectionWrapper.getSource().equals(c.getSourceNode())
                    && connectionWrapper.getTarget().equals(c.getTargetNode())) {
                    alreadyPresent = true;
                    break;
                }
            }

            if (!alreadyPresent) {
                ConnectionWrapper w = new ConnectionWrapper(c.getSourceNode(), c.getTargetNode());
                w.setTargetArrow(true);
                connections.add(w);
            }

        }

        // Add up channels in both directions to show number of channels on GUI
        for (ConnectionWrapper wrapper : connections) {
            for (Connection c : connectionsInModel) {
                if (c.getSourceNode().getIdentifierAsObject().equals(wrapper.getSource().getIdentifierAsObject())
                    && c.getTargetNode().getIdentifierAsObject().equals(wrapper.getTarget().getIdentifierAsObject())) {
                    wrapper.incrementNumberOfConnections();
                }
            }
        }
    }

    @Override
    protected final IFigure createFigure() {
        Figure f = new BackgroundLayer();
        f.setBorder(new MarginBorder(3));
        f.setLayoutManager(new FreeformLayout());

        // Create the static router for the connection layer
        ConnectionLayer connLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
        connLayer.setAntialias(SWT.ON);
        connLayer.setConnectionRouter(new CustomShortestPathConnectionRouter(f));
        return f;
    }

    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new RootComponentEditPolicy());
        installEditPolicy(EditPolicy.LAYOUT_ROLE, new WorkflowXYLayoutEditPolicy());
        installEditPolicy("Snap Feedback", new SnapFeedbackPolicy());
    }

    /**
     * Policy responsible for creating new WorkflowNodes.
     * 
     * @author Heinrich Wendel
     * @author Sascha Zur
     */
    class WorkflowXYLayoutEditPolicy extends XYLayoutEditPolicy {

        @Override
        protected Command createChangeConstraintCommand(final ChangeBoundsRequest request, final EditPart child,
            final Object constraint) {
            if (child instanceof WorkflowNodePart && constraint instanceof Rectangle) {

                // Find all connections between the selected nodes
                List<Connection> relatedConnections = new ArrayList<>();
                if (getModel() instanceof WorkflowDescription) {
                    WorkflowDescription workflowDescription = (WorkflowDescription) getModel();
                    for (Connection connection : workflowDescription.getConnections()) {
                        if (((connection.getTargetNode().getIdentifierAsObject()
                            .equals(((WorkflowNode) child.getModel()).getIdentifierAsObject()))
                            || (connection.getSourceNode().getIdentifierAsObject()
                                .equals(((WorkflowNode) child.getModel()).getIdentifierAsObject())))
                            && checkIfSourceAndTargetAreSelected(connection.getSourceNode(),
                                connection.getTargetNode(), child.getViewer().getSelectedEditParts())) {
                            relatedConnections.add(connection);
                        }
                    }
                }
                return new WorkflowNodeMoveCommand((WorkflowNode) child.getModel(), request, (Rectangle) constraint,
                    relatedConnections);
            }
            if (child instanceof WorkflowLabelPart && constraint instanceof Rectangle) {
                return new WorkflowLabelMoveCommand((WorkflowLabel) child.getModel(), request, (Rectangle) constraint);
            }
            return super.createChangeConstraintCommand(request, child, constraint);
        }

        @Override
        protected Command createChangeConstraintCommand(final EditPart child, final Object constraint) {
            return null;
        }

        @Override
        protected EditPolicy createChildEditPolicy(EditPart child) {
            if (child instanceof WorkflowNodePart) {
                return new NoBorderEditPolicy(child);
            } else {
                return super.createChildEditPolicy(child);
            }
        }

        private boolean checkIfSourceAndTargetAreSelected(WorkflowNode sourceNode, WorkflowNode targetNode,
            List<?> selectedEditParts) {
            boolean sourceContained = false;
            boolean targetContained = false;

            for (Object part : selectedEditParts) {
                if (part instanceof WorkflowNodePart) {
                    WorkflowNode selectedNode = (WorkflowNode) ((WorkflowNodePart) part).getModel();
                    if (sourceNode.getIdentifierAsObject().equals(selectedNode.getIdentifierAsObject())) {
                        sourceContained = true;
                    }
                    if (targetNode.getIdentifierAsObject().equals(selectedNode.getIdentifierAsObject())) {
                        targetContained = true;
                    }
                }
            }
            return sourceContained && targetContained;
        }

        /**
         * New policy to remove the handles from {@link WorkflowNodePart}s.
         * 
         * @author Sascha Zur
         */
        private class NoBorderEditPolicy extends NonResizableEditPolicy {

            private final EditPart child;

            NoBorderEditPolicy(EditPart child) {
                this.child = child;
            }

            @Override
            protected List<MoveHandle> createSelectionHandles() {
                List<MoveHandle> list = new ArrayList<>();
                if (isDragAllowed()) {
                    if (child instanceof WorkflowNodePart && ((WorkflowNode) ((WorkflowNodePart) child).getModel())
                        .getComponentDescription().getComponentInstallation().getComponentInterface().getShape() == ComponentShape.CIRCLE) {
                        list.add(new OvalBorderMoveHandle((GraphicalEditPart) child));
                    } else {
                        list.add(new MoveHandle((GraphicalEditPart) child));
                    }
                }
                return list;
            }
        }

        @Override
        protected Command getCreateCommand(final CreateRequest request) {

            Object childClass = request.getNewObjectType();
            if (childClass == WorkflowNode.class) {
                WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(
                    (WorkflowNode) request.getNewObject(), (WorkflowDescription) getHost().getModel(),
                    (Rectangle) getConstraintFor(request));
                return helper.createCommand();
            }
            if (childClass == WorkflowLabel.class) {
                WorkflowNodeLabelConnectionHelper helper = new WorkflowNodeLabelConnectionHelper(
                    (WorkflowLabel) request.getNewObject(), (WorkflowDescription) getHost().getModel(),
                    (Rectangle) getConstraintFor(request));
                return helper.createCommand();
            }
            return null;
        }
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class key) {
        // Enable Snap to grid/geometry in wf editor for all parts.
        if (key == SnapToHelper.class) {
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
        return super.getAdapter(key);
    }

    /**
     * Class for a custom background image.
     * 
     * @author Heinrich Wendel
     */
    private class BackgroundLayer extends FreeformLayer implements FigureListener {

        private final Image orgImage;

        private Image image;

        private int x;

        private int y;

        BackgroundLayer() {
            orgImage = Activator.getInstance().getImageRegistry().get(Activator.IMAGE_WORKFLOW_EDITOR_BACKGROUND);
            resize();
            addFigureListener(this);
        }

        private void resize() {
            Rectangle targetRect = getBounds().getCopy();
            if (orgImage != null && targetRect.height != 0) {

                float scaleX = (float) targetRect.width / (float) orgImage.getBounds().width;
                float scaleY = (float) targetRect.height / (float) orgImage.getBounds().height;

                int sizeX;
                int sizeY;
                if (scaleX < scaleY) {
                    sizeX = (int) (orgImage.getBounds().width * scaleX);
                    sizeY = (int) (orgImage.getBounds().height * scaleX);
                } else {
                    sizeX = (int) (orgImage.getBounds().width * scaleY);
                    sizeY = (int) (orgImage.getBounds().height * scaleY);
                }

                x = (targetRect.width - sizeX) / 2;
                y = (targetRect.height - sizeY) / 2;

                if (image != null) {
                    image.dispose();
                }
                image = new Image(Display.getDefault(), orgImage.getImageData().scaledTo(sizeX, sizeY));
            }
        }

        @Override
        protected void paintFigure(final Graphics graphics) {
            if (image != null) {
                graphics.drawImage(image, x, y);
            }
            super.paintFigure(graphics);
        }

        @Override
        public void figureMoved(final IFigure arg0) {
            resize();
        }
    }
}
