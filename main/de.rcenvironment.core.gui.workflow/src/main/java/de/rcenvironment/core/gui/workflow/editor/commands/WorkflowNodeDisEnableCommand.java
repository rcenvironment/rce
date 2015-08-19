/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * Command to (de-)activate a {@link WorkflowNode}.
 *
 * @author Doreen Seider
 */
public class WorkflowNodeDisEnableCommand extends Command {
    
    private Set<WorkflowNode> nodes;

    private Set<WorkflowNode> nodesToggled = new HashSet<>();

    /**
     * Constructor.
     * 
     * @param node {@link WorkflowNode}
     */
    public WorkflowNodeDisEnableCommand(Set<WorkflowNode> nodes) {
        this.nodes = nodes;
    }
    
    @Override
    public void execute() {
        redo();
    }
    
    @Override
    public void redo() {
        // check if all of the nodes are disabled/enabled, if not enable all disabled ones first
        boolean setEnabled = false;
        for (WorkflowNode node : nodes) {
            if (!node.isEnabled()) {
                setEnabled = true;
                break;
            }
        }
        // if node needs to be toggled, store them for undo
        for (WorkflowNode node : nodes) {
            if (node.isEnabled() != setEnabled) {
                nodesToggled.add(node);
                node.setEnabled(setEnabled);
            }
        }
    }
    
    @Override
    public void undo() {
        for (WorkflowNode node : nodesToggled) {
            node.setEnabled(!node.isEnabled());
        }
    }
    
}
