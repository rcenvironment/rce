/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Edits dynamic endpoints which are/were of type {@link DataType#FileReference} or {@link DataType#DirectoryReference} and are/were
 * selected at workflow start.
 * 
 * @author Doreen Seider
 */
public class InputProviderEditDynamicEndpointCommand extends EditDynamicEndpointCommand {

    public InputProviderEditDynamicEndpointCommand(EndpointType direction, final EndpointDescription oldDescription,
        final EndpointDescription newDescription, Refreshable... refreshable) {
        super(direction, oldDescription, newDescription, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        editConfigurationValue(oldDesc, newDesc);
    }

    @Override
    public void undo() {
        super.undo();
        editConfigurationValue(newDesc, oldDesc);
    }

    /**
     * 
     * Editing the Configuration value. <br>
     * 
     * Two combinations are in use: <br>
     * for execute - first = oldDesc and second = newDesc <br>
     * for undo - first = newDesc and second = oldDesc
     * 
     * @param first
     * @param second
     */
    private void editConfigurationValue(EndpointDescription first, EndpointDescription second) {
        getWorkflowNode().getConfigurationDescription()
            .setConfigurationValue(first.getName(), null);
        // remove also the data type entry
        getWorkflowNode().getConfigurationDescription()
            .setConfigurationValue(first.getName() + PlaceholdersMetaDataConstants.DATA_TYPE, null);
        if (second.getMetaData().containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
            && second.getMetaData().get(InputProviderComponentConstants.META_FILESOURCETYPE)
                .equals(InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART)) {
            InputProviderDynamicEndpointCommandUtils.setValueName(getWorkflowNode(), second.getDataType(), second.getName());
        }

    }

}
