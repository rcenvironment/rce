/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;


/**
 * Command to add a connection to the model.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionAddCommand extends Command{

    private WorkflowDescription model;
    private Connection connection;
    
    public ConnectionAddCommand(WorkflowDescription model, Connection connection) {
        this.model = model;
        this.connection = connection;
    }

    @Override
    public void execute() {
        model.addConnection(connection);
    }

    @Override
    public void undo() {
        model.removeConnection(connection);
    }
    
    @Override
    public void redo() {
        model.addConnection(connection);
    }
    
}
