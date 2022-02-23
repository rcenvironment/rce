/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputWithOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputWithOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputWithOutputCommand;

/**
 * Input pane for endpoints that add one output next to a requested input.
 *
 * @author Doreen Seider
 */
public class InputCoupledWithOutputSelectionPane extends ForwardingEndpointSelectionPane {

    private final String dynEndpointId;

    private final Refreshable outputPane;

    private Map<String, String> metaDataInput = new HashMap<>();

    private Map<String, String> metaDataOutput = new HashMap<>();

    public InputCoupledWithOutputSelectionPane(String title, String endpointId, WorkflowNodeCommand.Executor executor,
        Refreshable outputPane) {
        super(title, EndpointType.INPUT, endpointId, new String[] {}, executor);
        this.dynEndpointId = endpointId;
        this.outputPane = outputPane;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        metaDataInput.putAll(metaData);
        WorkflowNodeCommand command = new AddDynamicInputWithOutputCommand(dynEndpointId, name, type, metaDataInput, this, outputPane);
        ((AddDynamicInputWithOutputCommand) command).setMetaDataOutput(metaDataOutput);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command;
        if (oldDescription.getDynamicEndpointIdentifier().equals(dynEndpointIdToManage)) {
            command = new EditDynamicInputWithOutputCommand(oldDescription, newDescription, this, outputPane);
            ((EditDynamicInputWithOutputCommand) command).setMetaDataOutput(metaDataOutput);
        } else {
            command = new EditDynamicInputCommand(oldDescription.getDynamicEndpointIdentifier(), oldDescription.getName(),
                newDescription.getName(), newDescription.getDataType(), newDescription.getMetaData(), this);
        }
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new RemoveDynamicInputWithOutputCommand(dynEndpointId, names, this, outputPane);
        execute(command);
    }

    @Override
    public void setMetaDataInput(Map<String, String> metaDataInput) {
        this.metaDataInput = metaDataInput;
    }

    public void setMetaDataOutput(Map<String, String> metaDataOutput) {
        this.metaDataOutput = metaDataOutput;
    }

}
