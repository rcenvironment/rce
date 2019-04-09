/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Edits one single input and two outputs - one with the same name as the input and one with the same name as the input + given suffix.
 * 
 * @author Sascha Zur
 */
public class EditDynamicInputWithAnotherInputAndOutputsCommand extends EditDynamicInputWithOutputsCommand {

    private final String inputNameSuffix;

    private String inputGroup;

    private boolean addOrRemoveOtherInput;

    private Map<String, String> metaDataInputWithSuffix;

    public EditDynamicInputWithAnotherInputAndOutputsCommand(EndpointDescription oldDescription, EndpointDescription newDescription,
        String inputNameSuffix, String outputNameSuffix, String inputGroup, boolean addOrRemoveOtherInput, Refreshable... panes) {
        super(oldDescription, newDescription, outputNameSuffix, panes);
        this.inputNameSuffix = inputNameSuffix;
        this.inputGroup = inputGroup;
        this.addOrRemoveOtherInput = addOrRemoveOtherInput;
    }

    @Override
    public void execute() {
        EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
        EndpointDescription addInputDesc = inputManager.getEndpointDescription(oldDesc.getName() + inputNameSuffix);
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(newDesc.getMetaData());
        metaData.putAll(metaDataInputWithSuffix);
        if (!addOrRemoveOtherInput) {
            if (addInputDesc != null) {
                addInputDesc.setName(newDesc.getName() + inputNameSuffix);
                inputManager.editDynamicEndpointDescription(oldDesc.getName() + inputNameSuffix, newDesc.getName() + inputNameSuffix,
                    newDesc.getDataType(), metaData, addInputDesc.getDynamicEndpointIdentifier(), inputGroup);
            }
        } else {
            if (addInputDesc == null) {
                InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), newDesc.getDynamicEndpointIdentifier(), newDesc.getName(),
                    newDesc.getDataType(), inputNameSuffix, inputGroup, metaData);
            } else {
                InputWithOutputsCommandUtils.removeInputWithSuffix(getProperties(), oldDesc.getName(), inputNameSuffix);
            }
        }
        super.execute();
    }

    @Override
    public void undo() {
        EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
        EndpointDescription inputConvergedDesc = getProperties().getInputDescriptionsManager()
            .getEndpointDescription(newDesc.getName() + inputNameSuffix);
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(oldDesc.getMetaData());
        metaData.putAll(metaDataInputWithSuffix);
        if (!addOrRemoveOtherInput) {
            if (inputConvergedDesc != null) {
                inputConvergedDesc.setName(oldDesc.getName() + inputNameSuffix);
                inputManager.editDynamicEndpointDescription(newDesc.getName() + inputNameSuffix,
                    oldDesc.getName() + inputNameSuffix, oldDesc.getDataType(), metaData,
                    newDesc.getDynamicEndpointIdentifier(), inputGroup);
            }
        } else {
            if (inputConvergedDesc == null) {
                InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), oldDesc.getDynamicEndpointIdentifier(), oldDesc.getName(),
                    oldDesc.getDataType(), inputNameSuffix, inputGroup, metaData);
            } else {
                InputWithOutputsCommandUtils.removeInputWithSuffix(getProperties(), newDesc.getName(), inputNameSuffix);
            }
        }
        super.undo();
    }

    /**
     * Adds the given meta data to the current ones.
     * 
     * @param additionalMetaDataInputWithSuffix to add.
     */
    public void addMetaDataToInputWithSuffix(Map<String, String> additionalMetaDataInputWithSuffix) {
        metaDataInputWithSuffix = additionalMetaDataInputWithSuffix;
    }
}
