/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import java.util.List;
import java.util.Map;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * adding dynamic Endpoints to the Input Provider Component. A
 * 
 * @author Mark Geiger
 */
public class InputProviderRemoveDynamicEndpointCommand extends RemoveDynamicEndpointCommand {

    public InputProviderRemoveDynamicEndpointCommand(EndpointType direction, String id, List<String> names,
        Map<String, String> metaData, Refreshable... refreshable) {
        super(direction, id, names, refreshable);
    }

    @Override
    public void execute() {
        for (String name : names) {
            // not in use, shall be removed?
            EndpointDescriptionsManager manager;
            if (direction == EndpointType.INPUT) {
                manager = getWorkflowNode().getInputDescriptionsManager();
            } else {
                manager = getWorkflowNode().getOutputDescriptionsManager();
            }
            // removes the value entry
            getWorkflowNode().getConfigurationDescription().setConfigurationValue(name, null);
            getWorkflowNode().getConfigurationDescription().setConfigurationValue(name + PlaceholdersMetaDataConstants.DATA_TYPE,
                null);
        }
        super.execute();
    }

    @Override
    public void undo() {
        super.undo();
        for (String name : names) {
            EndpointDescription oldDescription = oldDescriptions.get(name);
            EndpointDescriptionsManager manager;
            if (direction == EndpointType.INPUT) {
                manager = getWorkflowNode().getInputDescriptionsManager();
            } else {
                manager = getWorkflowNode().getOutputDescriptionsManager();
            }
            if (oldDescription.getMetaData().containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
                && oldDescription.getMetaData().get(InputProviderComponentConstants.META_FILESOURCETYPE)
                    .equals(InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART)) {
                InputProviderDynamicEndpointCommandUtils.setValueName(getWorkflowNode(),
                    manager.getEndpointDescription(name).getDataType(),
                    name);
            }
        }
    }
}
