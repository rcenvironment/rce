/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.EllipseAnchor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.api.ComponentSize;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionDrawCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentPropertySource;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationSupport;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidityStateEvent;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidityStateListener;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Part representing a {@link WorkflowNode}.
 * 
 * <p>
 * For information about how the validation works, see {@link WorkflowNodeValidatorSupport}.
 * </p>
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class WorkflowNodePart extends AbstractGraphicalEditPart implements PropertyChangeListener, NodeEditPart {

    /** The width and height of a small workflow node's bounds. */
    public static final int SMALL_WORKFLOW_NODE_WIDTH = 38;

    /**
     * The width and height of a medium sized workflow node.
     */
    public static final int WORKFLOW_NODE_WIDTH = 75;

    private static final int MAX_LABELTEXT_SIZE = 30;

    private static final String LABEL_TEXT_SEPARATOR = "...";

    private static final int MAX_LABEL_WIDTH = 73;

    private static final Image ERROR_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16);

    private static final Image WARNING_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16);

    private static final Image LOCAL_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.LOCAL);

    private static final Image DEPRECATED_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.DEPRECATED);

    private final WorkflowNodeValidationSupport validationSupport = new WorkflowNodeValidationSupport();

    private final IFigure errorFigure = new ImageFigure(ERROR_IMAGE);
    {
        final int offset = 1;
        final int size = 16;
        errorFigure.setBounds(new Rectangle(offset, offset, size, size));
        errorFigure.setVisible(false);
    }

    private final IFigure warningFigure = new ImageFigure(WARNING_IMAGE);
    {
        final int offsetX = 58;
        final int offsetY = 1;
        final int size = 16;
        warningFigure.setBounds(new Rectangle(offsetX, offsetY, size, size));
        warningFigure.setVisible(false);
    }

    private final IFigure localFigure = new ImageFigure(LOCAL_IMAGE);
    {
        final int offset = 58;
        final int size = 16;
        localFigure.setBounds(new Rectangle(offset, offset, size, size));
        localFigure.setToolTip(new Label(Messages.localExecutionOnly));
        localFigure.setVisible(false);
    }

    private final IFigure deprecatedFigure = new ImageFigure(DEPRECATED_IMAGE);
    {
        final int offset = 21;
        final int size = 32;
        deprecatedFigure.setBounds(new Rectangle(offset, offset, size, size));
        deprecatedFigure.setToolTip(new Label(Messages.deprecated));
        deprecatedFigure.setVisible(false);
    }

    /**
     * {@link WorkflowNodeValidityStateListener} to update the valid state of this
     * {@link WorkflowNodePart} using {@link #updateValid(boolean)}.
     */
    private final WorkflowNodeValidityStateListener validityStateListener = new WorkflowNodeValidityStateListener() {

        @Override
        public void handleWorkflowNodeValidityStateEvent(final WorkflowNodeValidityStateEvent event) {
            updateValid();
        }

    };

    private String currentLabel = "";

    private final ToolIntegrationContextRegistry toolIntegrationRegistry;

    public WorkflowNodePart() {
        validationSupport.addWorkflowNodeValidityStateListener(validityStateListener);
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
    }

    @Override
    public void activate() {
        super.activate();
        ((PropertiesChangeSupport) getModel()).addPropertyChangeListener(this);
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                verifyValid();
            }
        });
    }

    @Override
    public void deactivate() {
        super.deactivate();
        ((PropertiesChangeSupport) getModel()).removePropertyChangeListener(this);
        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription("Clear workflow node in validation support.")
            public void run() {
                validationSupport.setWorkflowNodeAndValidation(null);
            }
        });
    }

    private List<WorkflowNodeValidationMessage> getValidationMessages() {
        final List<WorkflowNodeValidationMessage> result = new LinkedList<WorkflowNodeValidationMessage>(validationSupport.getMessages());
        return result;
    }

    private String getValidationMessageText(final WorkflowNodeValidationMessage.Type type) {
        final StringBuilder builder = new StringBuilder();
        for (final WorkflowNodeValidationMessage message : getValidationMessages()) {
            if (type == null || type == message.getType()) {
                String messageText = message.getAbsoluteMessage();
                if (messageText == null || messageText.isEmpty()) {
                    final String property = message.getProperty();
                    final String relativeMessage = message.getRelativeMessage();
                    if (property == null || property.isEmpty()) {
                        messageText = relativeMessage;
                    } else {
                        messageText = de.rcenvironment.core.utils.common.StringUtils.format("%s: %s", property, relativeMessage);
                    }
                }
                builder.append(messageText);
                builder.append("\n");
            }
        }
        return builder.toString().trim();
    }

    /**
     * Updates the visual indicators for {@link WorkflowNodeValidationMessage}s and refreshes the
     * graphical representation of this {@link WorkflowNodePart}.
     * 
     * @param valid true, if validation yielded not {@link WorkflowNodeValidationMessage}s.
     */
    private void updateValid() {
        final boolean valid = validationSupport.isValid();
        final String errorText = getValidationMessageText(WorkflowNodeValidationMessage.Type.ERROR);
        errorFigure.setVisible(!errorText.isEmpty());
        if (!valid) {
            errorFigure.setToolTip(new Label(errorText));
        }
        final String warningText = getValidationMessageText(WorkflowNodeValidationMessage.Type.WARNING);
        warningFigure.setVisible(!warningText.isEmpty());
        if (!valid) {
            warningFigure.setToolTip(new Label(warningText));
        }
        refresh();
        refreshVisuals();
    }

    protected WorkflowNode getWorkflowNode() {
        return (WorkflowNode) getModel();
    }

    @Override
    protected IFigure createFigure() {
        final IFigure figure = createBaseFigure();
        ComponentInterface ci =
            ((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface();

        if (ci.getShape() == ComponentShape.CIRCLE) {
            final int size = 16;
            final int newOffset = 3;
            errorFigure.setBounds(new Rectangle(newOffset, newOffset, size, size));
            warningFigure.setBounds(new Rectangle(newOffset, newOffset, size, size));
            final int localX = 22;
            final int localY = 20;
            localFigure.setBounds(new Rectangle(localX, localY, size, size));
        } else if (ci.getSize() == ComponentSize.SMALL) {
            final int size = 16;
            final int newOffsetX = 22;
            final int newOffsetY = 1;
            warningFigure.setBounds(new Rectangle(newOffsetX, newOffsetY, size, size));
            final int localX = 22;
            final int localY = 22;
            localFigure.setBounds(new Rectangle(localX, localY, size, size));
        }
        figure.add(errorFigure);
        figure.add(warningFigure);
        figure.add(localFigure);
        figure.add(deprecatedFigure);
        return figure;
    }

    protected IFigure createBaseFigure() {
        Image image = null;
        byte[] icon = null;

        ComponentInterface ci =
            ((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface();

        if (ci.getSize() == ComponentSize.SMALL) {
            icon = ((WorkflowNode) getModel()).getComponentDescription().getIcon24();
        } else {
            icon = ((WorkflowNode) getModel()).getComponentDescription().getIcon32();
        }
        if (icon != null) {
            try {
                image = new Image(Display.getCurrent(), new ByteArrayInputStream(icon));
            } catch (SWTException e) {
                // The images of integrated tools may be broken, so the default will be used.
                LogFactory.getLog(getClass()).info("Could not load tool icon, loading default. ", e);
                image = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_32);
            }
        } else if (ci.getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
            if (ci.getSize() == ComponentSize.SMALL) {
                image = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_24_GREY);
            } else {
                image = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_32_GREY);
            }
        } else {
            if (ci.getSize() == ComponentSize.SMALL) {
                image = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_24);
            } else {
                image = ImageManager.getInstance().getSharedImage(StandardImages.RCE_LOGO_32);
            }
        }
        Color color = getColor(ci);

        if (ci.getSize() == ComponentSize.SMALL) {
            IconLabel iconLabel = new IconLabel(image, ci);
            iconLabel.setOpaque(true);
            iconLabel.setBorder(null);
            iconLabel.setBackgroundColor(color);
            return iconLabel;
        } else {
            final String labelText = ((WorkflowNode) getModel()).getName();
            final Label label = new Label(labelText, image);
            label.setTextPlacement(PositionConstants.SOUTH);
            label.setBorder(new LineBorder());
            label.setOpaque(true);
            label.setBackgroundColor(color);
            return label;
        }

    }

    private Color getColor(ComponentInterface ci) {
        final int dr = 255;
        final int dg = 247;
        final int db = 231;
        Color c = new Color(null, dr, dg, db);
        if (!getWorkflowNode().isEnabled()) {
            final int grey = 0xDD;
            c = new Color(null, grey, grey, grey);
        } else if (ci.getColor() == null) {
            final int r = 0xFF;
            final int g = 0xCC;
            final int b = 0xD2;
            c = new Color(null, r, g, b);
        }
        return c;
    }

    /**
     * Class for the small workflow components.
     * 
     * @author Sascha Zur
     */
    private class IconLabel extends Label {

        private static final int OFFSET_SMALL_SQUARE_COMPONENT_ICON_X = 7;

        private static final int OFFSET_SMALL_SQUARE_COMPONENT_ICON_Y = 10;

        private static final int OFFSET_SMALL_CIRCLE_COMPONENT_ICON = 8;

        private final Image icon;

        private final ComponentInterface ci;

        public IconLabel(Image icon, ComponentInterface ci) {
            this.icon = icon;
            this.ci = ci;
        }

        @Override
        public void paintFigure(Graphics graphics) {

            int offsetX;
            int offsetY;

            if (ci.getShape() == ComponentShape.CIRCLE) {
                offsetX = OFFSET_SMALL_CIRCLE_COMPONENT_ICON;
                offsetY = OFFSET_SMALL_CIRCLE_COMPONENT_ICON;
                graphics.fillOval(this.getBounds());
                graphics.setAntialias(SWT.ON);
                Rectangle b = this.getBounds().getCopy();
                b.width--;
                b.height--;
                graphics.drawOval(b);

                graphics.setAntialias(SWT.OFF);
            } else {
                offsetX = OFFSET_SMALL_SQUARE_COMPONENT_ICON_X;
                offsetY = OFFSET_SMALL_SQUARE_COMPONENT_ICON_Y;
                graphics.fillRectangle(this.getBounds());
                Rectangle b = this.getBounds().getCopy();
                b.width--;
                b.height--;
                graphics.drawRectangle(b);
            }
            graphics.drawImage(icon, new Point(this.getLocation().x + offsetX, this.getLocation().y - 1
                + offsetY));
        }
    }

    protected void setTooltipText() {
        getFigure().setToolTip(new Label(generateTooltipText()));
    }

    protected String generateTooltipText() {
        WorkflowNode node = (WorkflowNode) getModel();
        String enabled = "enabled";
        if (!node.isEnabled()) {
            enabled = "disabled";
        } else if (node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
            enabled = "not available";
        }
        return generateTooltipTextBase(node) + ": " + enabled;
    }

    protected String generateTooltipTextBase(WorkflowNode node) {
        if (node.getComponentDescription().getVersion() != null
            && toolIntegrationRegistry.hasId(node.getComponentDescription().getIdentifier())) {
            return de.rcenvironment.core.utils.common.StringUtils.format("%s - %s (%s)", node.getName(),
                node.getComponentDescription().getName(), node.getComponentDescription().getVersion());
        } else {
            return de.rcenvironment.core.utils.common.StringUtils.format("%s - %s", node.getName(), node.getComponentDescription()
                .getName());
        }
    }

    @Override
    public void refreshVisuals() {
        Point loc = new Point(((WorkflowNode) getModel()).getX(), ((WorkflowNode) getModel()).getY());

        int width = WORKFLOW_NODE_WIDTH;
        int height = WORKFLOW_NODE_WIDTH;
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface()
            .getSize() == ComponentSize.SMALL) {
            width = SMALL_WORKFLOW_NODE_WIDTH;
            height = SMALL_WORKFLOW_NODE_WIDTH;
        }

        Rectangle r = new Rectangle(loc, new Dimension(width, height));
        final Label label = (Label) getFigure();

        String labelText = getWorkflowNode().getName();

        // try to abbreviate label just, when the name of the workflow has changed in the model
        if (!currentLabel.equals(getWorkflowNode().getName())) {
            abbreviateLabel(label, labelText, MAX_LABELTEXT_SIZE);
        }

        // remember original workflow node name from model
        currentLabel = labelText;

        setTooltipText();

        localFigure.setVisible(((WorkflowNode) getModel()).getComponentDescription().canOnlyBeExecutedLocally());
        deprecatedFigure.setVisible(((WorkflowNode) getModel()).getComponentDescription()
            .getComponentInstallation().getComponentRevision().getComponentInterface().getIsDeprecated());

        getFigure().setBackgroundColor(getColor(((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation()
            .getComponentRevision().getComponentInterface()));
        ((GraphicalEditPart) getParent()).setLayoutConstraint(this, label, r);
    }

    private String abbreviateLabel(Label label, String labelText, int currentLength) {
        label.setText(labelText);
        if (label.getFont() == null) {
            label.setFont(Display.getDefault().getSystemFont());
        }
        if (label.getTextBounds().width > MAX_LABEL_WIDTH) {
            String shorterLabelText = "";
            shorterLabelText = StringUtils.abbreviateMiddle(label.getText(), LABEL_TEXT_SEPARATOR, currentLength);
            currentLength--;
            label.setText(shorterLabelText);
            abbreviateLabel(label, shorterLabelText, currentLength);
        }
        return label.getText();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (WorkflowNode.PROPERTY_COMMUNICATION_NODE.equals(prop)) {
            refreshVisuals();
        } else if (WorkflowNode.PROPERTY_NODE_ATTRIBUTES.equals(prop)) {
            refreshVisuals();
        }
    }

    /**
     * Method called by the WorkflowPart to refresh the connections.
     */
    public void refreshConnections() {
        refreshSourceConnections();
        refreshTargetConnections();
    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {

        // handle reconnections
        if (connection != null) {
            if (connection.getSource() != null && connection.getTarget() != null) {
                if (connection.getSource().equals(connection.getTarget())) {
                    return new ReconnectionSourceAnchor(getFigure());
                }
            }
        }

        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            return new EllipseAnchor(getFigure());
        } else {
            return new ChopboxAnchor(getFigure());
        }

    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            return new EllipseAnchor(getFigure());
        } else {
            return new ChopboxAnchor(getFigure());
        }
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {

        // handle reconnections
        if (connection != null) {
            if (connection.getSource() != null && connection.getTarget() != null) {
                if (connection.getSource().equals(connection.getTarget())) {
                    return new ReconnectionTargetAnchor(getFigure());
                }
            }
        }

        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            return new EllipseAnchor(getFigure());
        } else {
            return new ChopboxAnchor(getFigure());
        }
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(Request request) {
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentRevision().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            return new EllipseAnchor(getFigure());
        } else {
            return new ChopboxAnchor(getFigure());
        }
    }

    @Override
    protected List<ConnectionWrapper> getModelSourceConnections() {
        List<ConnectionWrapper> sourceConnections = new ArrayList<ConnectionWrapper>();

        for (ConnectionWrapper c : ((WorkflowPart) getParent()).getConnections()) {
            if (c.getSource().equals(getModel())) {
                sourceConnections.add(c);
            }
        }

        return sourceConnections;
    }

    @Override
    protected List<ConnectionWrapper> getModelTargetConnections() {
        List<ConnectionWrapper> targetConnections = new ArrayList<ConnectionWrapper>();

        for (ConnectionWrapper c : ((WorkflowPart) getParent()).getConnections()) {
            if (c.getTarget().equals(getModel())) {
                targetConnections.add(c);
            }
        }

        return targetConnections;
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        // Enable Snap to grid/geometry in wf editor for the nodepart.
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
        deactivateEditPolicies();
        // allow connections
        installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new ConnectEditPolicy());
    }

    /**
     * Checks for registered editor actions and if it finds one for the selected component, this is
     * invoked. As no action is currently required here, the method is deprecated.
     */
    @Deprecated
    private void performDefaultAction() {
        IConfigurationElement[] confElements = Platform.getExtensionRegistry()
            .getConfigurationElementsFor("de.rcenvironment.core.gui.workflow.editorActions"); //$NON-NLS-1$

        for (final IConfigurationElement confElement : confElements) {

            WorkflowNode node = getWorkflowNode();

            if (node.getComponentDescription().getIdentifier().matches(confElement.getAttribute("component"))
                && confElement.getAttribute("default") != null
                && Boolean.TRUE.toString().matches(confElement.getAttribute("default"))) { //$NON-NLS-1$

                final WorkflowEditorAction action;
                try {
                    Object actionObject = confElement.createExecutableExtension("class");
                    if (!(actionObject instanceof WorkflowEditorAction)) {
                        throw new RuntimeException(de.rcenvironment.core.utils.common.StringUtils.format(
                            "Class in attribute 'class' is not a subtype of '%s'.",
                            WorkflowEditorAction.class.getName()));
                    }
                    action = (WorkflowEditorAction) actionObject;
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
                action.setWorkflowNode(node);
                action.performAction();
                break;
            }
        }
    }

    /**
     * Verifies the valid state of the {@link WorkflowNode} and refreshes the visuals of the
     * {@link WorkflowNodePart}.
     */
    public void verifyValid() {
        WorkflowNode workflowNode = getWorkflowNode();
        validationSupport.setWorkflowNodeAndValidation(workflowNode);
        updateValid();
        workflowNode.setValid(true);
    }

    /**
     * EditPolicy that allows connections.
     * 
     * @author Heinrich Wendel
     */
    class ConnectEditPolicy extends GraphicalNodeEditPolicy {

        @Override
        protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
            ConnectionDrawCommand cmd = (ConnectionDrawCommand) request.getStartCommand();
            cmd.setTarget((WorkflowNode) getHost().getModel());
            return cmd;
        }

        @Override
        protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
            WorkflowNode source = (WorkflowNode) getHost().getModel();
            ConnectionDrawCommand cmd = new ConnectionDrawCommand((WorkflowDescription) getParent().getModel(), source);
            request.setStartCommand(cmd);
            return cmd;
        }

        @Override
        protected Command getReconnectSourceCommand(ReconnectRequest request) {
            return null;
        }

        @Override
        protected Command getReconnectTargetCommand(ReconnectRequest request) {
            return null;
        }
    }

    /**
     * Anchor for the depiction of reconnections, i.e. connections from one component to the same.
     * Handles the source anchor.
     * 
     * @author Oliver Seebach
     */
    class ReconnectionSourceAnchor extends ChopboxAnchor {

        public ReconnectionSourceAnchor(IFigure figure) {
            super.setOwner(figure);
        }

        @Override
        protected Rectangle getBox() {

            int x = getOwner().getBounds().getCenter().x - 4; // - 3 to make sure, the connection is
                                                              // routed to the left
            int y = getOwner().getBounds().getCenter().y - getOwner().getBounds().height / 2 - 1;

            return new Rectangle(new Point(x, y), new Point(x, y));
        }
    }

    /**
     * Anchor for the depiction of reconnections, i.e. connections from one component to the same.
     * Handles the target anchor.
     * 
     * @author Oliver Seebach
     */
    class ReconnectionTargetAnchor extends ChopboxAnchor {

        public ReconnectionTargetAnchor(IFigure figure) {
            super.setOwner(figure);
        }

        @Override
        protected Rectangle getBox() {

            int x = getOwner().getBounds().getCenter().x - 4; // - 3 to make sure, the connection is
                                                              // routed to the left
            int y = getOwner().getBounds().getCenter().y + getOwner().getBounds().height / 2 + 1;

            return new Rectangle(new Point(x, y), new Point(x, y));
        }

    }
}
