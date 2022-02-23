/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Command that creates new WorkflowNodes, Labels and Connections.
 * 
 * @author Oliver Seebach
 */
public class WorkflowNodeLabelConnectionCreateCommand extends Command {

    private static final int MINUS_ONE = -1;

    private static final String OPEN_BRACKET = "(";

    private static final String CLOSE_BRACKET = ")";

    /** The parent WorkflowDescription. */
    private final WorkflowDescription model;

    /** The new WorkflowNode. */
    private final List<WorkflowNode> nodes;

    /** The new WorkflowNode. */
    private final List<Connection> connections;

    /** The new WorkflowNode. */
    private final List<WorkflowLabel> labels;

    /** The constraints. */
    private final List<Rectangle> nodeConstraints;

    /** The logger. */
    private final Log log = LogFactory.getLog(getClass());

    private final List<Rectangle> labelConstraints;

    /**
     * Constructor.
     * 
     * @param node The new WorkflowNode.
     * @param model The parent WorkflowDescription.
     * @param labelConstraintsToCreate
     * @param constraint The constraints for the new node.
     */
    public WorkflowNodeLabelConnectionCreateCommand(List<WorkflowNode> nodes, List<WorkflowLabel> labels, List<Connection> connections,
        WorkflowDescription model, List<Rectangle> nodeConstraints, List<Rectangle> labelConstraintsToCreate) {
        this.nodes = nodes;
        this.connections = connections;
        this.labels = labels;
        this.model = model;
        this.nodeConstraints = nodeConstraints;
        this.labelConstraints = labelConstraintsToCreate;
    }

    @Override
    public void undo() {
        if (connections != null) {
            model.removeConnections(connections);
        }
        if (nodes != null) {
            model.removeWorkflowNodes(nodes);
        }
        if (labels != null) {
            for (WorkflowLabel label : labels) {
                model.removeWorkflowLabel(label);
            }
        }
    }

    @Override
    public void redo() {
        int positionCounter = 0;
        if (nodes != null) {
            List<WorkflowNode> nodesToAdd = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).setLocation(nodeConstraints.get(positionCounter).getLocation().x,
                    nodeConstraints.get(positionCounter).getLocation().y);
                positionCounter++;
                nodesToAdd.add(nodes.get(i));
            }
            model.addWorkflowNodes(nodesToAdd);
        }
        if (connections != null) {
            model.addConnections(connections);
        }
        positionCounter = 0;
        if (labels != null) {
            for (int i = 0; i < labels.size(); i++) {
                labels.get(i).setLocation(labelConstraints.get(positionCounter).getLocation().x,
                    labelConstraints.get(positionCounter).getLocation().y);
                Dimension size = new Dimension(WorkflowLabel.DEFAULT_WIDTH, WorkflowLabel.DEFAULT_HEIGHT);
                if (labelConstraints.get(positionCounter).getSize().width != MINUS_ONE
                    && labelConstraints.get(positionCounter).getSize().height != MINUS_ONE) {
                    size = new Dimension(labelConstraints.get(positionCounter).getSize().width,
                        labelConstraints.get(positionCounter).getSize().height);
                }
                if (size.width == 0) {
                    size.width = WorkflowLabel.DEFAULT_WIDTH;
                }
                if (size.height == 0) {
                    size.height = WorkflowLabel.DEFAULT_HEIGHT;
                }
                labels.get(i).setSize(size.width, size.height);
                positionCounter++;
                model.addWorkflowLabel(labels.get(i));
            }
        }
        positionCounter = 0;
    }

    @Override
    public void execute() {
        if (nodes != null) {
            for (WorkflowNode node : nodes) {
                if (node.getName() == null || node.getName().isEmpty()) {
                    node.setName(getName(node.getComponentDescription().getName()));
                }
            }
        }
        redo();
        // open properties tab
        String message = "Open Properties view failed";
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().
                getActivePage().showView("org.eclipse.ui.views.PropertySheet");
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().
                getActivePage().getActiveEditor().setFocus();
        } catch (PartInitException e) {
            log.error(message, e);
        } catch (NullPointerException e) {
            log.error(message, e);
        }
    }

    /** Helper methods which returns the next unused default name of a component. */
    private String getName(String name) {

        int count = 0;
        for (WorkflowNode n : model.getWorkflowNodes()) {
            if (n.getName().equals(name)) {
                if (name.contains(OPEN_BRACKET)) {
                    try {
                        int index = name.lastIndexOf(OPEN_BRACKET) + 1;
                        count = Integer.valueOf(name.substring(index, name.lastIndexOf(CLOSE_BRACKET)));
                    } catch (NumberFormatException e) {
                        count = 0;
                    }
                    name = name.substring(0, name.lastIndexOf(OPEN_BRACKET));
                }
                count++;
                if (count == 1) {
                    name += " ";
                }
                return getName(name + OPEN_BRACKET + count + CLOSE_BRACKET);
            }
        }
        return name;
    }

}
