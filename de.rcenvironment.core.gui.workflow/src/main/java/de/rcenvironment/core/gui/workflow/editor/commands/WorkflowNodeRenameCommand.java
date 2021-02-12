/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * Command that changes the name of a WorkflowNode.
 *
 * @author Heinrich Wendel
 */
public class WorkflowNodeRenameCommand extends Command {
    
    /** The new WorkflowNode. */
    private WorkflowNode node;
    
    private WorkflowDescription wDesc;
    
    /** The new name. */
    private String name;
    
    /** The old name. */
    private String oldName;
    
    /**
     * Constructor.
     * 
     * @param node The WorkflowNode to change the name of.
     */
    public WorkflowNodeRenameCommand(WorkflowNode node, WorkflowDescription wDesc) {
        this.node = node;
        this.wDesc = wDesc;
    }

    @Override
    public void execute() {
        oldName = node.getName();
        
        InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.name, 
            Messages.enterName, oldName, new IInputValidator() {
                
                @Override
                public String isValid(String newText) {
                    if (!oldName.equalsIgnoreCase(newText)) {
                        for (WorkflowNode n : wDesc.getWorkflowNodes()) {
                            if (n.getName().equalsIgnoreCase(newText)) {
                                return Messages.renameAlreadyExistsError;
                            } else if (newText.isEmpty()) {
                                return Messages.renameEmptyNameError;
                            }
                        }
                    }
                    return null;
                }
            });
        
        if (dlg.open() == Window.CANCEL) {
            return;
        }

        name = dlg.getValue();

        redo();
    }
    
    @Override
    public void redo() {
        node.setName(name);
    }

    @Override
    public void undo() {
        node.setName(oldName);
    }
}
