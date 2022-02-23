/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.connections.ConnectionDialogController;


/**
 * Command to draw or remove connections.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionDrawCommand extends Command {

    private final Log logger = LogFactory.getLog(getClass());
    
    private WorkflowNode sourceNode;
    
    private WorkflowNode targetNode;
    
    private WorkflowDescription memorizedModel;
    
    private WorkflowDescription currentModel;
    
    private WorkflowDescription modifiedModel;
    
    
    public ConnectionDrawCommand(WorkflowDescription model, WorkflowNode sourceNode) {
        
        memorizedModel = model.clone();
        this.currentModel = model;
        this.sourceNode = sourceNode;
    }
    
    
    @Override
    public void execute() {

        WorkflowDescription modelClone = currentModel.clone();
        
        ConnectionDialogController dialogControler = new ConnectionDialogController(
            modelClone, sourceNode, targetNode, false);
        
        if (sourceNode != null && targetNode != null) {
            if (dialogControler.open() == 0) {
                
                modifiedModel = dialogControler.getWorkflowDescription();
                
                List<Connection> originalConnections = currentModel.getConnections();
                List<Connection> modifiedConnections = modifiedModel.getConnections();
                
                if (!CollectionUtils.isEqualCollection(originalConnections, modifiedConnections)){
                    currentModel.replaceConnections(modifiedConnections);
                }
            } 
        }
    }
    
    @Override
    public void undo() {
        currentModel.replaceConnections(memorizedModel.getConnections());
    }
    
    @Override
    public void redo() {
        if (currentModel != null && modifiedModel != null){
            currentModel.replaceConnections(modifiedModel.getConnections());
        } else {
            logger.warn("Redo of connections did not finish successfully.");
        }
    }
    
    /**
     * Sets the target node.
     * 
     * @param target The target node.
     */
    public void setTarget(WorkflowNode target) {
        this.targetNode = target;
    }
    
}
