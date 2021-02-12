/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Command to draw or remove connections via double click.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionEditCommand extends Command {

    private WorkflowDescription originalWorkflowDescription;
    
    private WorkflowDescription editedWorkflowDescription;
    
    private WorkflowDescription memorizedWorkflowDescription;
    
    public ConnectionEditCommand(WorkflowDescription originalWorkflowDescription, WorkflowDescription editedWorkflowDescription) {
        
        this.originalWorkflowDescription = originalWorkflowDescription;
        this.editedWorkflowDescription = editedWorkflowDescription;
        
    }
    
    @Override
    public void execute() {
        memorizedWorkflowDescription = originalWorkflowDescription.clone();
        redo();
    }
    
    @Override
    public void undo() {
        originalWorkflowDescription.replaceConnections(memorizedWorkflowDescription.getConnections());
    }
    
    @Override
    public void redo() {
        originalWorkflowDescription.replaceConnections(editedWorkflowDescription.getConnections());
    }
    
}
