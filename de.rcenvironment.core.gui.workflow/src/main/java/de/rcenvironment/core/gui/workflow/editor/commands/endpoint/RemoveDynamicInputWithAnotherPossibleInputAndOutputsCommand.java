/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Removes one single input, another input with given suffix and two outputs - one with the same
 * name as the input and one with the same name as the input + given suffix.
 * 
 * @author Sascha Zur
 */
public class RemoveDynamicInputWithAnotherPossibleInputAndOutputsCommand extends RemoveDynamicInputWithOutputsCommand {

    private final String addDynInputId;
    
    private final String inputNameSuffix;

    private final List<String> inputsWithSuffixInput;

    public RemoveDynamicInputWithAnotherPossibleInputAndOutputsCommand(String dynEndpointId, String addDynInputId, String inputNameSuffix,
        String addDynOutputId, String outputNameSuffix, List<String> names, Refreshable... panes) {
        super(dynEndpointId, addDynOutputId, outputNameSuffix, names, panes);
        this.addDynInputId = addDynInputId;
        this.inputNameSuffix = inputNameSuffix;
        this.inputsWithSuffixInput = new LinkedList<>();
    }

    @Override
    public void initialize() {
        for (String name : names) {
            oldDescriptions.put(name, getProperties().getInputDescriptionsManager().getEndpointDescription(name));
            EndpointDescription endpointDescription = getProperties().getInputDescriptionsManager()
                .getEndpointDescription(name + inputNameSuffix);
            if (endpointDescription != null) {
                inputsWithSuffixInput.add(name);
            }
        }
    }

    @Override
    public void execute() {
        for (String name : names) {
            EndpointDescription endpointDescription = getProperties().getInputDescriptionsManager()
                .getEndpointDescription(name + inputNameSuffix);
            if (endpointDescription != null) {
                InputWithOutputsCommandUtils.removeInputWithSuffix(getProperties(), name, inputNameSuffix);
            }
        }
        super.execute();
    }

    @Override
    public void undo() {
        for (String name : names) {
            EndpointDescription oldDescription = oldDescriptions.get(name);
            if (inputsWithSuffixInput.contains(name)) {
                InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), addDynInputId, name, oldDescription.getDataType(),
                    inputNameSuffix, LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP);
            }
        }
        super.undo();
    }
}
