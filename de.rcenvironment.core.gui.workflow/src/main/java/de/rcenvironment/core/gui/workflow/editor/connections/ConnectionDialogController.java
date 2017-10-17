/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.Location;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;

/**
 * Controller class for the connection dialog.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class ConnectionDialogController {

    /** The actual dialog. */
    private ConnectionDialog dialog;

    /** The complete WorkflowDescription. */
    private WorkflowDescription description;

    /** The selected source node (maybe null). */
    private WorkflowNode sourceWorkflowNode;

    /** The selected target node (maybe null). */
    private WorkflowNode targetWorkflowNode;

    private boolean wasDoubleClicked;

    public ConnectionDialogController(WorkflowDescription description, WorkflowNode source, WorkflowNode target, boolean wasDoubleClicked) {

        this.description = description;

        this.sourceWorkflowNode = source;
        this.targetWorkflowNode = target;

        this.wasDoubleClicked = wasDoubleClicked;

        dialog = new ConnectionDialog(Display.getCurrent().getActiveShell());

        dialog.create();

        dialog.getShell().setText(Messages.connectionEditor);
        initialize();

        // prevent auto connect on double click
        if (!wasDoubleClicked) {
            boolean autoConnected = checkForAutoConnection();
            dialog.getAutoConnectInfoLabel().setVisible(autoConnected);
            if (autoConnected) {
                dialog.getConnectionDialogComposite().getTargetTreeViewer().refresh();
                dialog.getConnectionDialogComposite().getSourceTreeViewer().refresh();
            }
        } else {
            dialog.getAutoConnectInfoLabel().setVisible(false);
        }

    }

    /**
     * Shows the dialog.
     * 
     * @return The return code (which button pressed).
     */
    public int open() {
        return dialog.open();
    }

    public WorkflowDescription getWorkflowDescription() {
        return description;
    }

    /**
     * Initializes the GUI.
     */
    private void initialize() {
        dialog.getConnectionDialogComposite().initialize(getWorkflowDescription(), sourceWorkflowNode, targetWorkflowNode);
        dialog.getConnectionDialogComposite().setWasDoubleClicked(wasDoubleClicked);
        dialog.getConnectionDialogComposite().applySourceFilter();
        dialog.getConnectionDialogComposite().applyTargetFilter();

        // start with empty selection
        dialog.getConnectionDialogComposite().getSourceTreeViewer().setSelection(StructuredSelection.EMPTY);
        dialog.getConnectionDialogComposite().getTargetTreeViewer().setSelection(StructuredSelection.EMPTY);
    }

    // if there are exactly 1 input and 1 output and they match in type, pull connection automatically
    private boolean checkForAutoConnection() {
        boolean autoConnected = false;
        if (sourceWorkflowNode != null && targetWorkflowNode != null) {
            // if user tries to connect same node - no auto connect
            if (sourceWorkflowNode.getIdentifier().equals(targetWorkflowNode.getIdentifier())) {
                return false;
            }

            // 1 Input + 1 Output -> try to autoconnect
            if (sourceWorkflowNode.getOutputDescriptionsManager().getEndpointDescriptions().size() == 1
                && targetWorkflowNode.getInputDescriptionsManager().getEndpointDescriptions().size() == 1) {
                EndpointDescription sourceOutput =
                    (EndpointDescription) sourceWorkflowNode.getOutputDescriptionsManager().getEndpointDescriptions().toArray()[0];
                EndpointDescription targetInput =
                    (EndpointDescription) targetWorkflowNode.getInputDescriptionsManager().getEndpointDescriptions().toArray()[0];
                if (sourceOutput.getDataType().equals(targetInput.getDataType())) {
                    // if input is already connected - no auto connect
                    for (Connection connection : description.getConnections()) {
                        if (connection.getInput().getIdentifier().equals(targetInput.getIdentifier())) {
                            return false;
                        }
                    }

                    List<Location> bendpoints =
                        ConnectionUtils.findAlreadyExistentBendpointsFromSourceToTarget(
                            sourceWorkflowNode, targetWorkflowNode, description);

                    Connection newConnection =
                        new Connection(sourceWorkflowNode, sourceOutput, targetWorkflowNode, targetInput, bendpoints);
                    if (!description.getConnections().contains(newConnection)) {
                        description.addConnection(newConnection);
                        return true;
                    }
                }
            } else if (sourceWorkflowNode.getOutputDescriptionsManager().getEndpointDescriptions().size() > 0
                && targetWorkflowNode.getInputDescriptionsManager().getEndpointDescriptions().size() > 0) {
                // what to do with more than one endpoint
                for (EndpointDescription sourceOutput : sourceWorkflowNode.getOutputDescriptionsManager().getEndpointDescriptions()) {
                    for (EndpointDescription targetInput : targetWorkflowNode.getInputDescriptionsManager().getEndpointDescriptions()) {
                        List<Connection> connectionsToBeAdded = new ArrayList<>();
                        if (sourceOutput.getDataType().equals(targetInput.getDataType())
                            && sourceOutput.getName().equals(targetInput.getName())) {
                            // if input is already connected - no auto connect
                            boolean alreadyExists = false;
                            for (Connection connection : description.getConnections()) {
                                if (connection.getInput().getIdentifier().equals(targetInput.getIdentifier())) {
                                    alreadyExists = true;
                                    break;
                                }
                            }
                            List<Location> bendpoints = ConnectionUtils.findAlreadyExistentBendpointsFromSourceToTarget(sourceWorkflowNode,
                                targetWorkflowNode, description);
                            Connection newConnection =
                                new Connection(sourceWorkflowNode, sourceOutput, targetWorkflowNode, targetInput, bendpoints);
                            if (!description.getConnections().contains(newConnection) && !alreadyExists) {
                                connectionsToBeAdded.add(newConnection);
                                autoConnected = true;
                            }
                        }
                        description.addConnections(connectionsToBeAdded);
                    }
                }
            }

        }
        return autoConnected;
    }

}
