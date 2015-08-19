/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Command required to delete connections from the graphical editor.
 * 
 * @author Oliver Seebach
 */
public class ConnectionDeleteCommand extends Command {

    private WorkflowDescription originalModel = null;

    private List<Connection> connectionsToDelete = new ArrayList<>();

    public ConnectionDeleteCommand(WorkflowDescription model, List<Connection> connectionsToDelete) {
        this.originalModel = model;
        this.connectionsToDelete = connectionsToDelete;
    }

    public ConnectionDeleteCommand() {}

    @Override
    public void execute() {
        originalModel.removeConnections(connectionsToDelete);
    }

    @Override
    public void undo() {
        originalModel.addConnections(connectionsToDelete);
    }

    @Override
    public void redo() {
        execute();
    }

    public void setOriginalModel(WorkflowDescription originalModel) {
        this.originalModel = originalModel;
    }

    /**
     * Adds a connection within a connection wrapper to a list to be deleted.
     * 
     * @param connection The connection to be added to the list for deletion
     */
    public void addConnectionForDeletion(Connection connection){
        this.connectionsToDelete.add(connection);
    }
    
}
