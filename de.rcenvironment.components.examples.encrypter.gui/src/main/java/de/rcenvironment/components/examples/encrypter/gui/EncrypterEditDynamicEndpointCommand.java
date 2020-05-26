/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Editing dynamic endpoints in the Encoder component. This class extends the base
 * {@link EditDynamicEndpointCommand} to enable the use of a placeholder for files. This is done
 * simply by setting the placeholder in the configuration when a {@link FileReferenceTD} is chosen.
 * 
 * @author Sascha Zur
 */
public class EncrypterEditDynamicEndpointCommand extends EditDynamicEndpointCommand {

    public EncrypterEditDynamicEndpointCommand(EndpointType direction, EndpointDescription oldDescription,
        EndpointDescription newDescription, Refreshable... refreshable) {
        super(direction, oldDescription, newDescription, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        // if (oldDesc.getDataType() != DataType.FileReference && newDesc.getDataType() ==
        // DataType.FileReference) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(newDesc.getName(), "${" + newDesc.getName() + "}");
        // }
        // if (oldDesc.getDataType() == DataType.FileReference && newDesc.getDataType() !=
        // DataType.FileReference) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(oldDesc.getName(), "");
        // }
    }

    @Override
    public void undo() {
        super.undo();
        // if (oldDesc.getDataType() == DataType.FileReference && newDesc.getDataType() !=
        // DataType.FileReference) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(oldDesc.getName(), "${" + oldDesc.getName() + "}");
        // }
        // if (oldDesc.getDataType() != DataType.FileReference && newDesc.getDataType() ==
        // DataType.FileReference) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(oldDesc.getName(), "");
        // }
    }
}
