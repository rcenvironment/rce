/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.ConnectionEditCommand;
import de.rcenvironment.core.gui.workflow.editor.connections.ConnectionDialogController;


/**
 * Opens the connection editor.
 *
 * @author Doreen Seider
 * @author Oliver Seebach
 * 
 */
public class OpenConnectionEditorHandler extends AbstractWorkflowNodeEditHandler {

    private WorkflowNode source = null;
    private WorkflowNode target = null;
    
    public OpenConnectionEditorHandler(WorkflowNode source, WorkflowNode target) {
        this.source = source;
        this.target = target;
    }
    
    @Override
    void edit() {
        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        WorkflowDescription modelClone = model.clone(); // within dialog, work on a clone of model
        ConnectionDialogController dialogControler = new ConnectionDialogController(modelClone, source, target, true);

        // 0=ok; 1=cancel
        if (dialogControler.open() == 0) {
            List<Connection> originalConnections = model.getConnections();
            List<Connection> modifiedConnections = dialogControler.getWorkflowDescription().getConnections();
            if (!CollectionUtils.isEqualCollection(originalConnections, modifiedConnections)){
                ConnectionEditCommand command = new ConnectionEditCommand(model, dialogControler.getWorkflowDescription());
                commandStack.execute(command);
            }
        } 
    }
}
