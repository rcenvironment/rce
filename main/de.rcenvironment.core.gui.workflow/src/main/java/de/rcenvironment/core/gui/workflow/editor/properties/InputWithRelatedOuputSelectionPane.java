/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputWithOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputWithOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputWithOutputCommand;

/**
 * Endpoint selection pane for forwarding values.
 * 
 * @author Doreen Seider
 */
public class InputWithRelatedOuputSelectionPane extends EndpointSelectionPane {

    private final EndpointSelectionPane outputPane;
    
    public InputWithRelatedOuputSelectionPane(String title, String id, WorkflowNodeCommand.Executor executor, 
        EndpointSelectionPane outputPane) {
        super(title, EndpointType.INPUT, executor, false, id, true);
        this.outputPane = outputPane;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new AddDynamicInputWithOutputCommand(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, 
            name, type, metaData, this, outputPane);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command = new EditDynamicInputWithOutputCommand(oldDescription, newDescription, this, outputPane);
        execute(command);
    }
    
    @Override
    protected void executeRemoveCommand(List<String> names) {
        WorkflowNodeCommand command = new RemoveDynamicInputWithOutputCommand(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, 
            names, this, outputPane);
        execute(command);
    }

}
