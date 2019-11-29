/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Implementation of {@link EditDynamicEndpointCommand}, which also edits the output with same name
 * and data type.
 * 
 * @author Doreen Seider
 */
public class EditDynamicInputWithOutputCommand extends EditDynamicEndpointCommand {

    private Map<String, String> metaDataOutput;

    public EditDynamicInputWithOutputCommand(final EndpointDescription oldDescription, EndpointDescription newDescription,
        Refreshable... refreshable) {
        super(EndpointType.INPUT, oldDescription, newDescription, refreshable);
    }

    @Override
    public void execute() {
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(newDesc.getMetaData());
        metaData.putAll(metaDataOutput);
        EndpointDescriptionsManager outputDescriptionsManager = getProperties().getOutputDescriptionsManager();
        outputDescriptionsManager.editDynamicEndpointDescription(oldDesc.getName(), newDesc.getName(),
            newDesc.getDataType(), metaData);
        super.execute();
    }

    @Override
    public void undo() {
        EndpointDescriptionsManager outputDescriptionsManager = getProperties().getOutputDescriptionsManager();
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(oldDesc.getMetaData());
        metaData.putAll(metaDataOutput);
        outputDescriptionsManager.editDynamicEndpointDescription(newDesc.getName(), oldDesc.getName(),
            oldDesc.getDataType(), metaData);
        super.undo();
    }

    public void setMetaDataOutput(Map<String, String> metaDataOutput) {
        this.metaDataOutput = metaDataOutput;
    }

}
