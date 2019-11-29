/*
 * Copyright 2006-2019 DLR, Germany
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

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.gef.EditPart;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.model.api.ComponentImageContainerService;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentShape;
import de.rcenvironment.core.component.model.api.ComponentSize;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionDrawCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentPropertySource;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowDescriptionValidationUtils;
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
 * @author Jascha Riedel (#13765)
 * @author Oliver Seebach
 * @author Jan Flink
 * 
 */
public class WorkflowNodePart extends AbstractGraphicalEditPart implements PropertyChangeListener, NodeEditPart {

    /**
     * The width and height of a small workflow node's bounds. Must be divisible by two after 1 is subtracted.
     */
    public static final int SMALL_WORKFLOW_NODE_WIDTH = 41;

    /**
     * The width and height of a medium sized workflow node. Must be divisible by four after 1 is subtracted.
     */
    public static final int WORKFLOW_NODE_WIDTH = 81;

    private static final int OFFSET_FACTOR = 2;

    private static final int MAX_LABELTEXT_SIZE = 30;

    private static final String LABEL_TEXT_SEPARATOR = "...";

    private static final int MAX_LABEL_WIDTH = 73;

    private static final Image ERROR_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16);

    private static final Image WARNING_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16);

    private static final Image LOCAL_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.LOCAL);

    private static final Image IMITATION_MODE_IMAGE = ImageManager.getInstance()
        .getSharedImage(StandardImages.IMITATION_MODE);

    private static final Image DEPRECATED_IMAGE = ImageManager.getInstance().getSharedImage(StandardImages.DEPRECATED);

    protected final IFigure informationFigure = new ImageFigure(LOCAL_IMAGE);

    {
        final int offsetX = 62;
        final int offsetY = 62;
        final int size = 16;
        informationFigure.setBounds(new Rectangle(offsetX, offsetY, size, size));
        informationFigure.setToolTip(new Label(Messages.localExecutionOnly));
        informationFigure.setVisible(false);
    }

    private final IFigure errorFigure = new ImageFigure(ERROR_IMAGE);
    {
        final int offset = 2;
        final int size = 16;
        errorFigure.setBounds(new Rectangle(offset, offset, size, size));
        errorFigure.setVisible(false);
    }

    private final IFigure warningFigure = new ImageFigure(WARNING_IMAGE);
    {
        final int offsetX = 62;
        final int offsetY = 2;
        final int size = 16;
        warningFigure.setBounds(new Rectangle(offsetX, offsetY, size, size));
        warningFigure.setVisible(false);
    }

    private final IFigure deprecatedFigure = new ImageFigure(DEPRECATED_IMAGE);
    {
        final int offsetX = 23;
        final int offsetY = 17;
        final int size = 32;
        deprecatedFigure.setBounds(new Rectangle(offsetX, offsetY, size, size));
        deprecatedFigure.setToolTip(new Label(Messages.deprecated));
        deprecatedFigure.setVisible(false);
    }

    private final ToolIntegrationContextRegistry toolIntegrationRegistry;

    private String currentLabel = "";

    private Image imageToDispose = null;

    public WorkflowNodePart() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        toolIntegrationRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
    }

    @Override
    public void activate() {
        super.activate();
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                WorkflowDescriptionValidationUtils.validateComponent(getWorkflowNode(), false);
                updateValid();
            }
        });
        ((PropertiesChangeSupport) getModel()).addPropertyChangeListener(this);
    }

    @Override
    public void deactivate() {
        super.deactivate();

        if (imageToDispose != null) {
            imageToDispose.dispose();
        }

        ((PropertiesChangeSupport) getModel()).removePropertyChangeListener(this);

    }

    private List<ComponentValidationMessage> getValidationMessages() {
        return ComponentValidationMessageStore.getInstance()
            .getMessagesByComponentId(getWorkflowNode().getIdentifierAsObject().toString());
    }

    private String getValidationMessageText(final ComponentValidationMessage.Type type) {
        final StringBuilder builder = new StringBuilder();
        for (final ComponentValidationMessage message : getValidationMessages()) {
            if (type == null || type == message.getType()) {
                String messageText = message.getAbsoluteMessage();
                if (messageText == null || messageText.isEmpty()) {
                    final String property = message.getProperty();
                    final String relativeMessage = message.getRelativeMessage();
                    if (property == null || property.isEmpty()) {
                        messageText = relativeMessage;
                    } else {
                        messageText = de.rcenvironment.core.utils.common.StringUtils.format("%s: %s", property,
                            relativeMessage);
                    }
                }
                builder.append("- ");
                builder.append(messageText);
                builder.append("\n\n");
            }
        }
        return builder.toString().trim();
    }

    /**
     * Updates the visual indicators for {@link ComponentValidationMessage}s and refreshes the graphical representation of this
     * {@link WorkflowNodePart}.
     * 
     */
    public void updateValid() {
        final String errorText = getValidationMessageText(ComponentValidationMessage.Type.ERROR);
        errorFigure.setVisible(!errorText.isEmpty());

        errorFigure.setToolTip(new Label(errorText));
        final String warningText = getValidationMessageText(ComponentValidationMessage.Type.WARNING);
        warningFigure.setVisible(!warningText.isEmpty());
        warningFigure.setToolTip(new Label(warningText));

        if (!getWorkflowNode().isEnabled()) {
            errorFigure.setVisible(false);
            warningFigure.setVisible(false);
        }
        refresh();
        refreshVisuals();
        // outcommented because it is likely to cause Mantis Issue #0014726;
        // seeb_ol, November 23, 2016
        // if (errorText.equals("") && warningText.equals("")) {
        // getWorkflowNode().setValid(true);
        // }
    }

    protected WorkflowNode getWorkflowNode() {
        return (WorkflowNode) getModel();
    }

    @Override
    protected IFigure createFigure() {
        final IFigure figure = createBaseFigure();
        ComponentInterface ci = ((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation()
            .getComponentInterface();

        if (ci.getShape() == ComponentShape.CIRCLE) {
            final int size = 16;
            final int newOffset = 3;
            errorFigure.setBounds(new Rectangle(newOffset, newOffset, size, size));
            warningFigure.setBounds(new Rectangle(newOffset, newOffset, size, size));
            final int localX = 22;
            final int localY = 20;
            informationFigure.setBounds(new Rectangle(localX, localY, size, size));
        } else if (ci.getSize() == ComponentSize.SMALL) {
            final int size = 16;
            final int newOffsetX = 22;
            final int newOffsetY = 1;
            warningFigure.setBounds(new Rectangle(newOffsetX, newOffsetY, size, size));
            final int localX = 22;
            final int localY = 22;
            informationFigure.setBounds(new Rectangle(localX, localY, size, size));
        }
        figure.add(errorFigure);
        figure.add(warningFigure);
        figure.add(informationFigure);
        figure.add(deprecatedFigure);
        return figure;
    }

    protected IFigure createBaseFigure() {
        Image image = null;

        ComponentInterface ci = ((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation()
            .getComponentInterface();

        if (ci.getSize() == ComponentSize.SMALL) {
            image = ServiceRegistry.createAccessFor(this).getService(ComponentImageContainerService.class).getComponentImageContainer(ci)
                .getComponentIcon24();
        } else {
            image = ServiceRegistry.createAccessFor(this).getService(ComponentImageContainerService.class).getComponentImageContainer(ci)
                .getComponentIcon32();
        }

        if (image == null) {
            if (ci.getIdentifierAndVersion().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
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
        if (!getWorkflowNode().isEnabled()) {
            return ColorManager.getInstance().getSharedColor(StandardColors.RCE_GREY);
        } else if (ci.getColor() == null) {
            return ColorManager.getInstance().getSharedColor(StandardColors.RCE_COLOR_2);
        } else if (Boolean.valueOf(((WorkflowNode) getModel()).getConfigurationDescription()
            .getConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE))) {
            return ColorManager.getInstance().getSharedColor(StandardColors.RCE_IMITATION);
        } else {
            // TODO There is currently only one ComponentColor specified.
            // However, the ComponentColor class doesn't store RGB values.
            // Therefore, the mapping is performed here.
            return ColorManager.getInstance().getSharedColor(StandardColors.RCE_COLOR_1);
        }
    }

    /**
     * Class for the small workflow components.
     * 
     * @author Sascha Zur
     */
    private class IconLabel extends Label {

        private static final int OFFSET_SMALL_SQUARE_COMPONENT_ICON_X = 8;

        private static final int OFFSET_SMALL_SQUARE_COMPONENT_ICON_Y = 10;

        private static final int OFFSET_SMALL_CIRCLE_COMPONENT_ICON = 9;

        private final Image icon;

        private final ComponentInterface ci;

        IconLabel(Image icon, ComponentInterface ci) {
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
            graphics.drawImage(icon, new Point(this.getLocation().x + offsetX, this.getLocation().y - 1 + offsetY));
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
        if (Boolean.valueOf(node.getConfigurationDescription()
            .getConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE))) {
            enabled += " (imitation mode)";
        }

        return generateTooltipTextBase(node) + ": " + enabled;
    }

    protected String generateTooltipTextBase(WorkflowNode node) {
        if (node.getComponentDescription().getVersion() != null
            && toolIntegrationRegistry.hasTIContextMatchingPrefix(node.getComponentDescription().getIdentifier())) {
            return de.rcenvironment.core.utils.common.StringUtils.format("%s - %s (%s)", node.getName(),
                node.getComponentDescription().getName(), node.getComponentDescription().getVersion());
        } else {
            return de.rcenvironment.core.utils.common.StringUtils.format("%s - %s", node.getName(),
                node.getComponentDescription().getName());
        }
    }

    @Override
    public void refreshVisuals() {
        Point loc = new Point(((WorkflowNode) getModel()).getX(), ((WorkflowNode) getModel()).getY());

        int width = WORKFLOW_NODE_WIDTH;
        int height = WORKFLOW_NODE_WIDTH;
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentInterface()
            .getSize() == ComponentSize.SMALL) {
            width = SMALL_WORKFLOW_NODE_WIDTH;
            height = SMALL_WORKFLOW_NODE_WIDTH;
        }

        Rectangle r = new Rectangle(loc, new Dimension(width, height));
        final Label label = (Label) getFigure();

        String labelText = getWorkflowNode().getName();

        // try to abbreviate label just, when the name of the workflow has
        // changed in the model
        if (!currentLabel.equals(getWorkflowNode().getName())) {
            abbreviateLabel(label, labelText, MAX_LABELTEXT_SIZE);
        }

        // remember original workflow node name from model
        currentLabel = labelText;

        setTooltipText();

        if (((WorkflowNode) getModel()).getComponentDescription().canOnlyBeExecutedLocally()) {
            ((ImageFigure) informationFigure).setImage(LOCAL_IMAGE);
            informationFigure.setToolTip(new Label(Messages.localExecutionOnly));
            informationFigure.setVisible(true);
        } else if (Boolean.valueOf(((WorkflowNode) getModel()).getConfigurationDescription()
            .getConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE))) {
            ((ImageFigure) informationFigure).setImage(IMITATION_MODE_IMAGE);
            informationFigure.setToolTip(new Label(Messages.imitationMode));
            informationFigure.setVisible(true);
        } else {
            informationFigure.setVisible(false);
        }

        deprecatedFigure.setVisible(((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation()
            .getComponentInterface().getIsDeprecated());

        getFigure().setBackgroundColor(getColor(((WorkflowNode) getModel()).getComponentDescription()
            .getComponentInstallation().getComponentInterface()));
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
        ((WorkflowNode) getModel()).setValid(false);
        if (WorkflowNode.PROPERTY_COMMUNICATION_NODE.equals(prop)
            || WorkflowNode.PROPERTY_NODE_ATTRIBUTES.equals(prop)) {
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
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            // small round components -> circle as anchor
            return new EllipseAnchor(getFigure());
        } else {
            boolean hasInverseConnection = checkIfConnectionHasReverseConnectionInWorkflow(connection);
            boolean isSmallComponent = (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation()
                .getComponentInterface().getSize() == ComponentSize.SMALL);
            if (hasInverseConnection && !isSmallComponent) {
                // medium rectangle components -> custom anchor
                return new BidirectionalChopboxAnchor(getFigure(), connection, AnchorType.SOURCE);
            } else {
                // small rectangle components -> box as anchor
                return new ChopboxAnchor(getFigure());
            }
        }

    }

    @Override
    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentInterface()
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
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            // small round components -> circle as anchor
            return new EllipseAnchor(getFigure());
        } else {
            boolean hasInverseConnection = checkIfConnectionHasReverseConnectionInWorkflow(connection);
            boolean isSmallComponent = (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation()
                .getComponentInterface().getSize() == ComponentSize.SMALL);
            if (hasInverseConnection && !isSmallComponent) {
                // medium rectangle components -> custom anchor
                return new BidirectionalChopboxAnchor(getFigure(), connection, AnchorType.TARGET);
            } else {
                // small rectangle components -> box as anchor
                return new ChopboxAnchor(getFigure());
            }
        }
    }

    private boolean checkIfConnectionHasReverseConnectionInWorkflow(ConnectionEditPart connection) {
        List<Connection> connections = new ArrayList<>();
        if (getViewer().getContents().getModel() instanceof WorkflowDescription) {
            connections = ((WorkflowDescription) getViewer().getContents().getModel()).getConnections();
        } else if (getViewer().getContents().getModel() instanceof WorkflowExecutionInformationImpl) {
            connections = ((WorkflowExecutionInformationImpl) getViewer().getContents().getModel())
                .getWorkflowDescription().getConnections();
        }

        for (Connection connectionInWorkflow : connections) {
            if (connectionInWorkflow.getSourceNode().equals(((ConnectionWrapper) connection.getModel()).getTarget())
                && connectionInWorkflow.getTargetNode()
                    .equals(((ConnectionWrapper) connection.getModel()).getSource())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ConnectionAnchor getTargetConnectionAnchor(Request request) {
        if (((WorkflowNode) getModel()).getComponentDescription().getComponentInstallation().getComponentInterface()
            .getShape() == ComponentShape.CIRCLE) {
            return new EllipseAnchor(getFigure());
        } else {
            return new ChopboxAnchor(getFigure());
        }
    }

    @Override
    protected List<ConnectionWrapper> getModelSourceConnections() {
        List<ConnectionWrapper> sourceConnections = new ArrayList<>();

        for (ConnectionWrapper c : ((WorkflowPart) getParent()).getConnections()) {
            if (c.getSource().equals(getModel())) {
                sourceConnections.add(c);
            }
        }

        return sourceConnections;
    }

    @Override
    protected List<ConnectionWrapper> getModelTargetConnections() {
        List<ConnectionWrapper> targetConnections = new ArrayList<>();

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
            List<SnapToHelper> helpers = new ArrayList<>();
            if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED))) {
                helpers.add(new SnapToGeometry(this));
            }
            if (Boolean.TRUE.equals(getViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED))) {
                helpers.add(new SnapToGrid(this));
            }
            if (helpers.isEmpty()) {
                return null;
            } else {
                return new CompoundSnapToHelper(helpers.toArray(new SnapToHelper[0]));
            }
        } else if (type == IPropertySource.class && getModel() instanceof WorkflowNode) {
            return new ComponentPropertySource(getViewer().getEditDomain().getCommandStack(),
                (WorkflowNode) getModel());
        }
        return super.getAdapter(type);
    }

    @Override
    public void performRequest(Request req) {
        if (req.getType().equals(RequestConstants.REQ_OPEN)) {
            try {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .showView("org.eclipse.ui.views.PropertySheet");
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
     * Anchor for the depiction of reconnections, i.e. connections from one component to the same. Handles the source anchor.
     * 
     * @author Oliver Seebach
     */
    class ReconnectionSourceAnchor extends ChopboxAnchor {

        ReconnectionSourceAnchor(IFigure figure) {
            super.setOwner(figure);
        }

        @Override
        protected Rectangle getBox() {

            int x = getOwner().getBounds().getCenter().x - 4; // - 3 to make
                                                              // sure, the
                                                              // connection is
                                                              // routed to the
                                                              // left
            int y = getOwner().getBounds().getCenter().y - getOwner().getBounds().height / 2 - 1;

            return new Rectangle(new Point(x, y), new Point(x, y));
        }
    }

    /**
     * Anchor for the depiction of reconnections, i.e. connections from one component to the same. Handles the target anchor.
     * 
     * @author Oliver Seebach
     */
    class ReconnectionTargetAnchor extends ChopboxAnchor {

        ReconnectionTargetAnchor(IFigure figure) {
            super.setOwner(figure);
        }

        @Override
        protected Rectangle getBox() {

            int x = getOwner().getBounds().getCenter().x - 4; // - 3 to make
                                                              // sure, the
                                                              // connection is
                                                              // routed to the
                                                              // left
            int y = getOwner().getBounds().getCenter().y + getOwner().getBounds().height / 2 + 1;

            return new Rectangle(new Point(x, y), new Point(x, y));
        }

    }

    /**
     * Chopbox anchor which is uses for bidirectional connections. Adds an offset to the anchor to distinquish the connection lines.
     *
     * @author Oliver Seebach
     * @author Jan Flink (minor refactorings)
     */
    private class BidirectionalChopboxAnchor extends ChopboxAnchor {

        private ConnectionEditPart connectionEditPart;

        private AnchorType type;

        BidirectionalChopboxAnchor(IFigure figure, ConnectionEditPart connection, AnchorType type) {
            super.setOwner(figure);
            this.connectionEditPart = connection;
            this.type = type;
        }

        @Override
        protected Rectangle getBox() {
            ConnectionPart correspondingConnectionPart = findInverseConnectionPart(connectionEditPart);
            if (correspondingConnectionPart != null) {
                WorkflowNode targetNode = (WorkflowNode) connectionEditPart.getTarget().getModel();
                WorkflowNode sourceNode = (WorkflowNode) connectionEditPart.getSource().getModel();
                WorkflowDescription workflowDescription = null;
                if (getViewer().getContents().getModel() instanceof WorkflowDescription) {
                    workflowDescription = (WorkflowDescription) getViewer().getContents().getModel();
                } else if (getViewer().getContents().getModel() instanceof WorkflowExecutionInformationImpl) {
                    workflowDescription = ((WorkflowExecutionInformationImpl) getViewer().getContents().getModel())
                        .getWorkflowDescription();
                }
                boolean hasBendpointsInAnyDirection = !ConnectionUtils
                    .findAlreadyExistentBendpointsFromSourceToTarget(sourceNode, targetNode, workflowDescription)
                    .isEmpty();
                hasBendpointsInAnyDirection |= !ConnectionUtils
                    .findAlreadyExistentBendpointsFromSourceToTarget(targetNode, sourceNode, workflowDescription)
                    .isEmpty();
                boolean draggingAtConnection = (connectionEditPart.getSelected() == SWT.SELECTED
                    && getViewer().getControl().getData(WorkflowEditor.DRAG_STATE_BENDPOINT) != null
                    && (boolean) getViewer().getControl().getData(WorkflowEditor.DRAG_STATE_BENDPOINT));

                if (!draggingAtConnection && !hasBendpointsInAnyDirection) {
                    // no bendpoints -> special anchors to distinguish connection lines
                    Rectangle referenceRectangle = getOwner().getBounds();
                    Point sourcePoint = new Point(sourceNode.getX(), sourceNode.getY());
                    Point targetPoint = new Point(targetNode.getX(), targetNode.getY());
                    int nodeWidth = determineSizeOfNode(connectionEditPart);
                    Orientation orientation = determineOrientationForAnchor(sourcePoint, targetPoint, nodeWidth);
                    if (type.equals(AnchorType.TARGET)) {
                        switch (orientation) {
                        case NORTHWEST:
                            return ConnectionAnchorUtils.getBottomRightRect(referenceRectangle);
                        case NORTH:
                            return ConnectionAnchorUtils.getBottomLeftRect(referenceRectangle);
                        case NORTHEAST:
                            return ConnectionAnchorUtils.getLeftLowerRect(referenceRectangle);
                        case WEST:
                            return ConnectionAnchorUtils.getRightLowerRect(referenceRectangle);
                        case EAST:
                            return ConnectionAnchorUtils.getLeftUpperRect(referenceRectangle);
                        case SOUTHWEST:
                            return ConnectionAnchorUtils.getRightUpperRect(referenceRectangle);
                        case SOUTH:
                            return ConnectionAnchorUtils.getTopRightRect(referenceRectangle);
                        case SOUTHEAST:
                            return ConnectionAnchorUtils.getTopLeftRect(referenceRectangle);
                        case MIDDLE:
                        default:
                            return super.getBox();
                        }
                    }
                    if (type.equals(AnchorType.SOURCE)) {

                        switch (orientation) {
                        case NORTHWEST:
                            return ConnectionAnchorUtils.getLeftUpperRect(referenceRectangle);
                        case NORTH:
                            return ConnectionAnchorUtils.getTopLeftRect(referenceRectangle);
                        case NORTHEAST:
                            return ConnectionAnchorUtils.getTopRightRect(referenceRectangle);
                        case WEST:
                            return ConnectionAnchorUtils.getLeftLowerRect(referenceRectangle);
                        case EAST:
                            return ConnectionAnchorUtils.getRightUpperRect(referenceRectangle);
                        case SOUTHWEST:
                            return ConnectionAnchorUtils.getBottomLeftRect(referenceRectangle);
                        case SOUTH:
                            return ConnectionAnchorUtils.getBottomRightRect(referenceRectangle);
                        case SOUTHEAST:
                            return ConnectionAnchorUtils.getRightLowerRect(referenceRectangle);
                        case MIDDLE:
                        default:
                            return super.getBox();
                        }
                    }
                }
            }
            return super.getBox();
        }

        private int determineSizeOfNode(ConnectionEditPart cPart) {
            int nodeWidth = 0;
            EditPart part = null;
            if (type.equals(AnchorType.TARGET)) {
                part = cPart.getTarget();
            } else {
                part = cPart.getSource();
            }
            if (part instanceof WorkflowNodePart) {
                WorkflowNodePart workflowNodePart = (WorkflowNodePart) part;
                nodeWidth = workflowNodePart.getFigure().getBounds().width;
            }
            return nodeWidth;
        }

        @SuppressWarnings("unchecked")
        private ConnectionPart findInverseConnectionPart(ConnectionEditPart cPart) {
            List<Object> allConnections = new ArrayList<>();
            allConnections.addAll(getSourceConnections());
            allConnections.addAll(getTargetConnections());

            for (Object connection : allConnections) {
                if (connection instanceof ConnectionPart) {
                    ConnectionPart connectionPartCandidate = (ConnectionPart) connection;

                    WorkflowNode connectionPartTempsSourceNode = (WorkflowNode) connectionPartCandidate.getSource()
                        .getModel();
                    WorkflowNode connectionPartTempsTargetNode = (WorkflowNode) connectionPartCandidate.getTarget()
                        .getModel();
                    WorkflowNode originalConnectionPartsTargetNode = (WorkflowNode) cPart.getTarget().getModel();
                    WorkflowNode originalConnectionPartsSourceNode = (WorkflowNode) cPart.getSource().getModel();

                    if (connectionPartTempsSourceNode.getIdentifierAsObject()
                        .equals(originalConnectionPartsTargetNode.getIdentifierAsObject())
                        && connectionPartTempsTargetNode.getIdentifierAsObject()
                            .equals(originalConnectionPartsSourceNode.getIdentifierAsObject())) {
                        return connectionPartCandidate;
                    }
                }
            }
            return null;
        }

        private Orientation determineOrientationForAnchor(Point sourcePoint, Point targetPoint, int nodeWidth) {
            Orientation orientation = Orientation.MIDDLE;
            int targetX = targetPoint.x;
            int targetY = targetPoint.y;
            int sourceX = sourcePoint.x;
            int sourceY = sourcePoint.y;

            if (targetY <= sourceY - OFFSET_FACTOR * nodeWidth) {
                if (targetX <= sourceX - OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.NORTHWEST;
                } else if (targetX > sourceX - OFFSET_FACTOR * nodeWidth
                    && targetX <= sourceX + OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.NORTH;
                } else if (targetX > sourceX + OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.NORTHEAST;
                }
            } else if (targetY > sourceY - OFFSET_FACTOR * nodeWidth
                && targetY <= sourceY + OFFSET_FACTOR * nodeWidth) {
                if (targetX <= sourceX - OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.WEST;
                } else if (targetX > sourceX - OFFSET_FACTOR * nodeWidth
                    && targetX <= sourceX + OFFSET_FACTOR * nodeWidth) {
                    // inner part
                    if (targetY <= sourceY - nodeWidth) {
                        if (targetX <= sourceX - nodeWidth) {
                            orientation = Orientation.NORTHWEST;
                        } else if (targetX > sourceX - nodeWidth && targetX <= sourceX + nodeWidth) {
                            orientation = Orientation.NORTH;
                        } else if (targetX > sourceX + nodeWidth) {
                            orientation = Orientation.NORTHEAST;
                        }
                    } else if (targetY > sourceY - nodeWidth && targetY <= sourceY + nodeWidth) {
                        if (targetX <= sourceX - nodeWidth) {
                            orientation = Orientation.WEST;
                        } else if (targetX > sourceX - nodeWidth && targetX <= sourceX + nodeWidth) {
                            orientation = Orientation.MIDDLE;
                        } else if (targetX > sourceX + nodeWidth) {
                            orientation = Orientation.EAST;
                        }
                    } else if (targetY > sourceY + nodeWidth) {
                        if (targetX <= sourceX - nodeWidth) {
                            orientation = Orientation.SOUTHWEST;
                        } else if (targetX > sourceX - nodeWidth && targetX <= sourceX + nodeWidth) {
                            orientation = Orientation.SOUTH;
                        } else if (targetX > sourceX + nodeWidth) {
                            orientation = Orientation.SOUTHEAST;
                        }
                    }
                } else if (targetX > sourceX + OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.EAST;
                }
            } else if (targetY > sourceY + OFFSET_FACTOR * nodeWidth) {
                if (targetX <= sourceX - OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.SOUTHWEST;
                } else if (targetX > sourceX - OFFSET_FACTOR * nodeWidth
                    && targetX <= sourceX + OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.SOUTH;
                } else if (targetX > sourceX + OFFSET_FACTOR * nodeWidth) {
                    orientation = Orientation.SOUTHEAST;
                }
            }

            return orientation;

        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof BidirectionalChopboxAnchor) {
                BidirectionalChopboxAnchor anchor = (BidirectionalChopboxAnchor) object;
                if (anchor.getOwner().equals(this.getOwner())
                    && anchor.connectionEditPart.equals(this.connectionEditPart) && anchor.type.equals(this.type)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 1;
            final int factor = 31;
            hash = hash * factor + getOwner().hashCode();
            hash = hash * factor + connectionEditPart.hashCode();
            hash = hash * factor + type.hashCode();
            return hash;
        }

    }

    /**
     * Defines the corresponding component of connection anchors.
     *
     * @author Jan Flink
     */
    private enum AnchorType {
        /** Anchor at the connections source component. **/
        SOURCE,
        /** Anchor at the connections target component. **/
        TARGET
    }

    /**
     * Orientations between source and target component of a connection.
     *
     * @author Oliver Seebach
     */
    private enum Orientation {
        /** Northwest orientation. */
        NORTHWEST,
        /** North orientation. */
        NORTH,
        /** Northeast orientation. */
        NORTHEAST,
        /** West orientation. */
        WEST,
        /** Middle orientation. */
        MIDDLE,
        /** East orientation. */
        EAST,
        /** Southwest orientation. */
        SOUTHWEST,
        /** South orientation. */
        SOUTH,
        /** Southeast orientation. */
        SOUTHEAST;
    }

}
