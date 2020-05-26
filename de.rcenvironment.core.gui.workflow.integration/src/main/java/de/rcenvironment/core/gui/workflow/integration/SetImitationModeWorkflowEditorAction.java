/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeDisEnableImitiationModeCommand;

/**
 * {@link WorkflowEditorAction} used to enable and disable the tool run imitation mode if available.
 * 
 * @author Hendrik Abbenhaus
 */
public class SetImitationModeWorkflowEditorAction extends WorkflowEditorAction {

    @Override
    public void run() {
        if (workflowNode.isImitationModeSupported()){
            Set<WorkflowNode> nodes = new HashSet<>();
            nodes.add(workflowNode);
            commandStack.execute(new WorkflowNodeDisEnableImitiationModeCommand(nodes));
        } else {
            MessageBox noImitationModeSupportedMBox = new MessageBox(Display.getDefault().getActiveShell());
            noImitationModeSupportedMBox.setText("Tool run imitation mode");
            noImitationModeSupportedMBox.setMessage(Messages.mockModeNotAvailable);
            noImitationModeSupportedMBox.open();
        }
    }
   
}
