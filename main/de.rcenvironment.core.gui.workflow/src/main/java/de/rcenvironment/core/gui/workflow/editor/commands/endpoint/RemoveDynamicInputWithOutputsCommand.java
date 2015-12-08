/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.List;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Removes one single input and two outputs - one with the same name as the input and one with the
 * same name as the input + given suffix.
 * 
 * @author Doreen Seider
 */
public class RemoveDynamicInputWithOutputsCommand extends RemoveDynamicInputWithOutputCommand {

    private final String nameSuffix;

    public RemoveDynamicInputWithOutputsCommand(String dynamicEndpointId, List<String> names, String nameSuffix, Refreshable... panes) {
        super(dynamicEndpointId, names, panes);
        this.nameSuffix = nameSuffix;
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
            InputWithOutputsCommandUtils.removeOutputWithSuffix(getProperties(), name, nameSuffix);
        }
        super.execute();
    }

    @Override
    public void undo() {
        for (String name : names) {
            EndpointDescription oldDescription = oldDescriptions.get(name);
            InputWithOutputsCommandUtils.addOutputWithSuffix(getProperties(), dynEndpointId, name, oldDescription.getDataType(),
                nameSuffix);
        }
        super.undo();
    }
}
