/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Adding dynamic endpoints to the Encoder component. This class extends the base
 * {@link RemoveDynamicEndpointCommand} to enable the use of a placeholder for files. This is done
 * simply by setting the placeholder in the configuration when a {@link FileReferenceTD} is chosen.
 * 
 * @author Sascha Zur
 */
public class EncrypterRemoveDynamicEndpointCommand extends RemoveDynamicEndpointCommand {

    public EncrypterRemoveDynamicEndpointCommand(EndpointType direction, String id, List<String> names,
        Map<String, String> metaData, Refreshable... refreshable) {
        super(direction, id, names, refreshable);
    }

    @Override
    public void execute() {

        // EndpointDescriptionsManager manager;
        // if (direction == EndpointType.Input) {
        // manager = getWorkflowNode().getInputDescriptionsManager();
        // } else {
        // manager = getWorkflowNode().getOutputDescriptionsManager();
        // }
        // if (manager.getEndpointDescription(name).getDataType().equals(DataType.FileReference)) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(name, "");
        // }
        super.execute();

    }

    @Override
    public void undo() {
        super.undo();
        // EndpointDescriptionsManager manager;
        // if (direction == EndpointType.Input) {
        // manager = getWorkflowNode().getInputDescriptionsManager();
        // } else {
        // manager = getWorkflowNode().getOutputDescriptionsManager();
        // }
        // if (manager.getEndpointDescription(name).getDataType().equals(DataType.FileReference)) {
        // getWorkflowNode().getConfigurationDescriptionManager().getConfigurationDescription()
        // .setConfigurationValue(name, "${" + name + "}");
        // }

    }
}
