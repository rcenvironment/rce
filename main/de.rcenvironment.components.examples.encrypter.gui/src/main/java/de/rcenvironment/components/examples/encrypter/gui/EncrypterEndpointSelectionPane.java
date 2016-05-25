/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * An adapted EndpointSelectionPane for the Encoder Component. It extends the
 * {@link EndpointSelectionPane} for working with its own {@link WorkflowNodeCommand}s. This
 * commands must be set, so that placeholders for ****.
 * 
 * For using not the default {@link WorkflowNodeCommand}s, the methods executeAddCommand,
 * executeEditCommand and executeRemoveCommand just have to be overwritten.
 * 
 * @author Sascha Zur
 */
public class EncrypterEndpointSelectionPane extends EndpointSelectionPane {

    public EncrypterEndpointSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor,
        boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints);

    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        if (type == DataType.FileReference) {
            WorkflowNodeCommand command = new EncrypterAddDynamicEndpointCommand(endpointType
                , endpointIdToManage, name, type, metaData, this); // null = this
            execute(command);
        } else {
            super.executeAddCommand(name, type, metaData);
        }
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command = new EncrypterEditDynamicEndpointCommand(endpointType, oldDescription, newDescription, this);
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        WorkflowNodeCommand command = new EncrypterRemoveDynamicEndpointCommand(endpointType
            , endpointIdToManage, names, null, this); // null = this
        execute(command);
    }
}
