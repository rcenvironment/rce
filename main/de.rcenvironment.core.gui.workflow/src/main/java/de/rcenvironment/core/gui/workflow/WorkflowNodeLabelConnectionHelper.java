/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeLabelConnectionCreateCommand;

/**
 * Helper class that simplifies the creation of WorkflowNodeConnectionCreateCommand. This is
 * particularly useful when you want to create a command for a single WorkflowNode or WorkflowLabel.
 * 
 * @author Oliver Seebach
 */
public class WorkflowNodeLabelConnectionHelper {

    private WorkflowNode node = null;

    private WorkflowLabel label = null;

    private WorkflowDescription model = null;

    private Rectangle constraint = null;

    public WorkflowNodeLabelConnectionHelper(WorkflowNode node, WorkflowDescription model, Rectangle constraint) {
        this.node = node;
        this.model = model;
        this.constraint = constraint;
    }

    public WorkflowNodeLabelConnectionHelper(WorkflowLabel label, WorkflowDescription model, Rectangle constraint) {
        this.label = label;
        this.model = model;
        this.constraint = constraint;
    }

    /**
     * Creates the WorkflowNodeLabelConnectionCreateCommand with the node/label, model and
     * constraint as defined in the constructor.
     * 
     * @return the WorkflowNodeLabelConnectionCreateCommand with the node/label, model and
     *         constraint as defined in the constructor.
     */
    public WorkflowNodeLabelConnectionCreateCommand createCommand() {
        List<WorkflowNode> nodes = null;
        if (node != null) {
            nodes = new ArrayList<>();
            nodes.add(node);
        }
        List<WorkflowLabel> labels = null;
        if (label != null) {
            labels = new ArrayList<>();
            labels.add(label);
        }
        List<Rectangle> constraints = null;
        if (constraint != null) {
            constraints = new ArrayList<>();
            constraints.add(constraint);
        }

        WorkflowNodeLabelConnectionCreateCommand command =
            new WorkflowNodeLabelConnectionCreateCommand(nodes, labels, null, model, constraints, constraints);
        return command;
    }

}
