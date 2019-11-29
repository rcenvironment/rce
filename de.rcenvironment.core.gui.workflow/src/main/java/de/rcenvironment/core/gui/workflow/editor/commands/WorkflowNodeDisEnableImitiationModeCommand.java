/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Command to (de-)activate tool run imitation mode {@link WorkflowNode}.
 *
 * @author Hendrik Abbenhaus
 */
public class WorkflowNodeDisEnableImitiationModeCommand extends Command {

    private Set<WorkflowNode> nodes;

    private Set<WorkflowNode> nodesToggled = new HashSet<>();

    /**
     * Constructor.
     * 
     * @param node {@link WorkflowNode}
     */
    public WorkflowNodeDisEnableImitiationModeCommand(Set<WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void redo() {
        // check if all of the nodes are disabled/enabled, if not enable all disabled ones first
        boolean setActive = false;
        for (WorkflowNode node : nodes) {
            if (!node.isImitiationModeActive()) {
                setActive = true;
                break;
            }
        }
        // if node needs to be toggled, store them for undo
        for (WorkflowNode node : nodes) {
            if (node.isImitiationModeActive() != setActive) {
                nodesToggled.add(node);
                node.setImitiationModeActive(setActive);
            }
        }
    }

    @Override
    public void undo() {
        for (WorkflowNode node : nodesToggled) {
            node.setImitiationModeActive(!node.isImitiationModeActive());
        }
    }

}
