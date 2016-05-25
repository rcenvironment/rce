/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Removes one single input, another input with given suffix and one output with the same name as
 * the input.
 * 
 * @author Sascha Zur
 */
public class RemoveDynamicInputWithAnotherPossibleInputAndOutputCommand extends RemoveDynamicInputWithOutputCommand {

    private final String inputNameSuffix;

    private final List<String> inputsWithSuffixInput;

    public RemoveDynamicInputWithAnotherPossibleInputAndOutputCommand(String dynamicEndpointId, List<String> names, String inputNameSuffix,
        Refreshable... panes) {
        super(dynamicEndpointId, names, panes);
        this.inputNameSuffix = inputNameSuffix;
        this.inputsWithSuffixInput = new LinkedList<>();
    }

    @Override
    public void initialize() {
        for (String name : names) {
            oldDescriptions.put(name, getProperties().getInputDescriptionsManager().getEndpointDescription(name));
            EndpointDescription endpointDescription = getProperties().getInputDescriptionsManager()
                .getEndpointDescription(name + inputNameSuffix);
            if (!name.endsWith(inputNameSuffix) && endpointDescription != null) {
                inputsWithSuffixInput.add(name);
            }
        }
    }

    @Override
    public void execute() {
        for (String name : names) {
            EndpointDescription endpointDescription = getProperties().getInputDescriptionsManager()
                .getEndpointDescription(name + inputNameSuffix);
            if (!name.endsWith(inputNameSuffix)
                && endpointDescription != null) {
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
                InputWithOutputsCommandUtils.addInputWithSuffix(getProperties(), dynEndpointId, name, oldDescription.getDataType(),
                    inputNameSuffix, "startValues");
            }
        }
        super.undo();
    }
}
