/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import java.util.Map;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Adding dynamic endpoints to the Encoder component. This class extends the base
 * {@link AddDynamicEndpointCommand} to enable the use of a placeholder for files. This is done
 * simply by setting the placeholder in the configuration when a {@link FileReferenceTD} is chosen.
 * 
 * @author Sascha Zur
 */
public class EncrypterAddDynamicEndpointCommand extends AddDynamicEndpointCommand {

    public EncrypterAddDynamicEndpointCommand(EndpointType direction, String id, String name, DataType type,
        Map<String, String> metaData, Refreshable... refreshable) {
        super(direction, id, name, type, metaData, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        // if (type == DataType.FileReference) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(name, "${" + name + "}");
        // }
    }

    @Override
    public void undo() {
        super.undo();
        // if (type == DataType.FileReference) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(name, "");
        // }
    }
}
