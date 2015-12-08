/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputWithAnotherInputAndOutputsCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputWithOutputsCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputWithAnotherInputAndOutputsCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputWithOutputsCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputWithAnotherPossibleInputAndOutputsCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * Endpoint selection pane.
 * 
 * @author Doreen Seider
 */
public class ConvergerEndpointSelectionPane extends EndpointSelectionPane {

    private final String dynamicEndpointId;

    private final EndpointSelectionPane outputPane;

    public ConvergerEndpointSelectionPane(String title, String id, Executor executor, EndpointSelectionPane outputPane,
        boolean toConverge) {
        super(title, EndpointType.INPUT, executor, false, id, true);
        this.dynamicEndpointId = id;
        this.outputPane = outputPane;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = null;
        metaData.putAll(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        if (metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
            && Boolean.parseBoolean(metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE))) {
            command = new AddDynamicInputWithOutputsCommand(dynamicEndpointId, name, type,
                metaData, ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, this, outputPane);

        } else {
            command = new AddDynamicInputWithAnotherInputAndOutputsCommand(dynamicEndpointId, name, type,
                metaData, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane);

            ((AddDynamicInputWithAnotherInputAndOutputsCommand) command)
                .addMetaDataToInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        }
        ((AddDynamicInputWithOutputsCommand) command)
            .setMetaDataOutput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        ((AddDynamicInputWithOutputsCommand) command)
            .setMetaDataOutputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        boolean oldHasStartvalue = oldDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
            && Boolean.parseBoolean(oldDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE));
        boolean newHasStartvalue = newDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
            && Boolean.parseBoolean(newDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE));
        WorkflowNodeCommand command = null;
        if (oldHasStartvalue && newHasStartvalue) {
            command = new EditDynamicInputWithOutputsCommand(oldDescription, newDescription,
                ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, this, outputPane);
        } else if (!oldHasStartvalue && !newHasStartvalue) {
            command = new EditDynamicInputWithAnotherInputAndOutputsCommand(oldDescription, newDescription,
                LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, false, this, outputPane);
            ((EditDynamicInputWithAnotherInputAndOutputsCommand) command)
                .addMetaDataToInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        } else {
            command = new EditDynamicInputWithAnotherInputAndOutputsCommand(oldDescription, newDescription,
                LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, true, this, outputPane);
            ((EditDynamicInputWithAnotherInputAndOutputsCommand) command)
                .addMetaDataToInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        }

        ((EditDynamicInputWithOutputsCommand) command)
            .setMetaDataOutput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        ((EditDynamicInputWithOutputsCommand) command)
            .setMetaDataOutputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        List<String> removeNames = new LinkedList<>();
        for (String name : names) {
            if (name.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                removeNames.add(name);
            }
        }
        for (String remove : removeNames) {
            names.remove(remove);
        }
        WorkflowNodeCommand command = new RemoveDynamicInputWithAnotherPossibleInputAndOutputsCommand(dynamicEndpointId, names,
            LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, this, outputPane);
        execute(command);
    }

    @Override
    protected void updateButtonActivation() {
        super.updateButtonActivation();
        TableItem[] selection = table.getSelection();
        if (selection.length == 1 && selection[0].getText().endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
            buttonEdit.setEnabled(false);
            buttonRemove.setEnabled(false);
        }
    }
}
