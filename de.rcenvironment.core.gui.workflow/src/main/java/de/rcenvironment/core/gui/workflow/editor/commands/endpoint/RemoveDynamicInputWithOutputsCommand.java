/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.List;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Removes one single input and two outputs - one with the same name as the input and one with the same name as the input + given suffix.
 * 
 * @author Doreen Seider
 */
public class RemoveDynamicInputWithOutputsCommand extends RemoveDynamicInputWithOutputCommand {

    private final String addDynOutputId;

    private final String outputNameSuffix;

    public RemoveDynamicInputWithOutputsCommand(String dynEndpointId, String addDynOutputId, String outputNameSuffix,
        List<String> names, Refreshable... panes) {
        super(dynEndpointId, names, panes);
        this.addDynOutputId = addDynOutputId;
        this.outputNameSuffix = outputNameSuffix;
    }

    @Override
    public void initialize() {
        for (String name : names) {
            oldDescriptions.put(name, getProperties().getInputDescriptionsManager().getEndpointDescription(name));
        }
    }

    @Override
    public void execute() {
        for (String name : names) {
            InputWithOutputsCommandUtils.removeOutputWithSuffix(getProperties(), name, outputNameSuffix);
        }
        super.execute();
    }

    @Override
    public void undo() {
        for (String name : names) {
            EndpointDescription oldDescription = oldDescriptions.get(name);
            InputWithOutputsCommandUtils.addOutputWithSuffix(getProperties(), addDynOutputId, name, oldDescription.getDataType(),
                outputNameSuffix);
        }
        super.undo();
    }
}
