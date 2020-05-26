/*
 * Copyright 2006-2020 DLR, Germany
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
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Edits one single input and two outputs - one with the same name as the input and one with the
 * same name as the input + given suffix.
 * 
 * @author Doreen Seider
 * @author Martin Misiak
 * FIXED 0014355: {@link #undo()} reverted to the datatype of the new @link {@link EndpointDescription} instead of the old
 */
public class EditDynamicInputWithOutputsCommand extends EditDynamicInputWithOutputCommand {

    private final String nameSuffix;

    private Map<String, String> metaDataOutputWithSuffix;

    public EditDynamicInputWithOutputsCommand(EndpointDescription oldDescription, EndpointDescription newDescription,
        String nameSuffix, Refreshable... panes) {
        super(oldDescription, newDescription, panes);
        this.nameSuffix = nameSuffix;
    }

    @Override
    public void execute() {

        EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
        EndpointDescription addOutputDesc = outputManager.getEndpointDescription(oldDesc.getName() + nameSuffix);
        addOutputDesc.setName(newDesc.getName() + nameSuffix);
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(newDesc.getMetaData());
        metaData.putAll(metaDataOutputWithSuffix);
        outputManager.editDynamicEndpointDescription(oldDesc.getName() + nameSuffix,
            newDesc.getName() + nameSuffix, newDesc.getDataType(), metaData);
        super.execute();
    }

    @Override
    public void undo() {
        EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
        EndpointDescription outputConvergedDesc = getProperties().getOutputDescriptionsManager()
            .getEndpointDescription(newDesc.getName() + nameSuffix);
        outputConvergedDesc.setName(oldDesc.getName() + nameSuffix);
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(oldDesc.getMetaData());
        metaData.putAll(metaDataOutputWithSuffix);
        outputManager.editDynamicEndpointDescription(newDesc.getName() + nameSuffix,
            oldDesc.getName() + nameSuffix, oldDesc.getDataType(), metaData);
        super.undo();
    }

    public void setMetaDataOutputWithSuffix(Map<String, String> metaDataOutput) {
        this.metaDataOutputWithSuffix = metaDataOutput;
    }

}
