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
 * Edits one single input and one output with the same name as the input .
 * 
 * @author Sascha Zur
 * @author Martin Misiak FIXED 0014355: {@link #undo()} reverted to the datatype of the new @link {@link EndpointDescription} instead of the
 *         old.
 */
public class EditDynamicInputWithAnotherInputAndOutputCommand extends EditDynamicInputWithOutputCommand {

    private final String inputNameSuffix;

    private String inputGroup;

    private boolean addOrRemoveOtherInput;

    private Map<String, String> metaDataInputWithSuffix;

    public EditDynamicInputWithAnotherInputAndOutputCommand(EndpointDescription oldDescription, EndpointDescription newDescription,
        String inputNameSuffix, String inputGroup, boolean addOrRemoveOtherInput, Refreshable... panes) {
        super(oldDescription, newDescription, panes);
        this.inputNameSuffix = inputNameSuffix;
        this.inputGroup = inputGroup;
        this.addOrRemoveOtherInput = addOrRemoveOtherInput;
        this.metaDataInputWithSuffix = new HashMap<>();
    }

    @Override
    public void execute() {
        EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
        EndpointDescription addInputDesc = inputManager.getEndpointDescription(oldDesc.getName() + inputNameSuffix);
        Map<String, String> metaDataForInput = new HashMap<>();
        metaDataForInput.putAll(newDesc.getMetaData());
        metaDataForInput.putAll(metaDataInputWithSuffix);
        if (!addOrRemoveOtherInput) {
            addInputDesc.setName(newDesc.getName() + inputNameSuffix);
            inputManager.editDynamicEndpointDescription(oldDesc.getName() + inputNameSuffix, newDesc.getName() + inputNameSuffix,
                newDesc.getDataType(), metaDataForInput, addInputDesc.getDynamicEndpointIdentifier(), inputGroup);
        } else {
            if (addInputDesc == null) {
                InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), newDesc.getDynamicEndpointIdentifier(), newDesc.getName(),
                    newDesc.getEndpointDefinition().getDefaultDataType(), inputNameSuffix, inputGroup, metaDataForInput);
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
        Map<String, String> metaDataForInput = new HashMap<>();
        metaDataForInput.putAll(oldDesc.getMetaData());
        metaDataForInput.putAll(metaDataInputWithSuffix);
        if (!addOrRemoveOtherInput) {
            inputConvergedDesc.setName(oldDesc.getName() + inputNameSuffix);
            inputManager.editDynamicEndpointDescription(newDesc.getName() + inputNameSuffix,
                oldDesc.getName() + inputNameSuffix, oldDesc.getDataType(), metaDataForInput,
                newDesc.getDynamicEndpointIdentifier(), inputGroup);
        } else {
            if (inputConvergedDesc == null) {
                InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), oldDesc.getDynamicEndpointIdentifier(), oldDesc.getName(),
                    oldDesc.getEndpointDefinition().getDefaultDataType(), inputNameSuffix, inputGroup, metaDataForInput);
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
        this.metaDataInputWithSuffix.putAll(additionalMetaDataInputWithSuffix);
    }
}
