/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import java.util.Map;

import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Adds dynamic endpoints which are of type {@link DataType#FileReference} or {@link DataType#DirectoryReference} and are selected at
 * workflow start.
 * 
 * @author Mark Geiger
 */
public class InputProviderAddDynamicEndpointCommand extends AddDynamicEndpointCommand {

    public InputProviderAddDynamicEndpointCommand(EndpointType direction, String id, String name, DataType type,
        Map<String, String> metaData, Refreshable... refreshable) {
        super(direction, id, name, type, metaData, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        InputProviderDynamicEndpointCommandUtils.setValueName(getWorkflowNode(), type, name);
    }

    @Override
    public void undo() {
        super.undo();
        getWorkflowNode().getConfigurationDescription().setConfigurationValue(name, null);
        getWorkflowNode().getConfigurationDescription().setConfigurationValue(
            name + PlaceholdersMetaDataConstants.DATA_TYPE, null);
    }
}
