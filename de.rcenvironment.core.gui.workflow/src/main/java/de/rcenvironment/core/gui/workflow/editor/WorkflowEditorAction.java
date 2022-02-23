/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * Abstract class for workflow editor actions.
 *
 * @author Christian Weiss
 * @author Doreen Seider
 */
public abstract class WorkflowEditorAction {

    protected WorkflowNode workflowNode;
    
    protected CommandStack commandStack;

    /**
     * Called by RCE to set the selected {@link WorkflowNode}.
     * @param workflowNode selected {@link WorkflowNode}
     */
    public void setWorkflowNode(WorkflowNode workflowNode) {
        this.workflowNode = workflowNode;
    }
    
    /**
     * Called by RCE to set the {@link CommandStack} and run the action.
     */
    public void performAction() {
        commandStack =
            (CommandStack) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart()
                .getAdapter(CommandStack.class);
        run();
    }
    
    /**
     * Performs the action.
     */
    public abstract void run();

    /**
     * The default-implementation always returns true. Subclasses may override this method to dynamically en- and disable themselves.
     * 
     * @return True if this action is enabled for the current workflow node, false otherwise
     */
    public boolean isEnabled() {
        return true;
    }
    
}
