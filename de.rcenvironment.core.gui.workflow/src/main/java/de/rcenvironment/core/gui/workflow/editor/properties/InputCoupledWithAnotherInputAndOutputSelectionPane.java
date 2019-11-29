/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputWithAnotherInputAndOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputWithAnotherInputAndOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputWithAnotherPossibleInputAndOutputCommand;

/**
 * Input pane for forwarding endpoint that need just one output.
 *
 * @author Sascha Zur
 */
public class InputCoupledWithAnotherInputAndOutputSelectionPane extends ForwardingEndpointSelectionPane {

    private final String dynEndpointId;

    private final String addDynInputId;

    private final String inputNameSuffix;

    private final Refreshable outputPane;

    private Map<String, String> metaDataInput = new HashMap<>();

    private Map<String, String> metaDataInputWithSuffix = new HashMap<>();

    private Map<String, String> metaDataOutput = new HashMap<>();

    public InputCoupledWithAnotherInputAndOutputSelectionPane(String title, String endpointId, String addDynInputId,
        String inputNameSuffix, WorkflowNodeCommand.Executor executor, Refreshable outputPane) {
        super(title, EndpointType.INPUT, endpointId, new String[] { endpointId, addDynInputId }, executor);
        this.dynEndpointId = endpointId;
        this.addDynInputId = addDynInputId;
        this.inputNameSuffix = inputNameSuffix;
        this.outputPane = outputPane;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        metaDataInput.putAll(metaData);
        metaDataInputWithSuffix.putAll(metaData);
        WorkflowNodeCommand command =
            new AddDynamicInputWithAnotherInputAndOutputCommand(dynEndpointId, addDynInputId, inputNameSuffix, name, type, metaDataInput,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane);
        ((AddDynamicInputWithAnotherInputAndOutputCommand) command).addMetaDataToInputWithSuffix(metaDataInputWithSuffix);
        ((AddDynamicInputWithAnotherInputAndOutputCommand) command).setMetaDataOutput(metaDataOutput);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command;
        if (oldDescription.getDynamicEndpointIdentifier().equals(dynEndpointIdToManage)) {
            command = new EditDynamicInputWithAnotherInputAndOutputCommand(oldDescription, newDescription, inputNameSuffix,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, false, this, outputPane);
            ((EditDynamicInputWithAnotherInputAndOutputCommand) command).addMetaDataToInputWithSuffix(metaDataInputWithSuffix);
            ((EditDynamicInputWithAnotherInputAndOutputCommand) command).setMetaDataOutput(metaDataOutput);
        } else {
            command = new EditDynamicInputCommand(oldDescription.getDynamicEndpointIdentifier(), oldDescription.getName(),
                newDescription.getName(), newDescription.getDataType(), newDescription.getMetaData(), this);
        }
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command =
            new RemoveDynamicInputWithAnotherPossibleInputAndOutputCommand(dynEndpointId, names, inputNameSuffix, this,
                outputPane);
        execute(command);
    }

    @Override
    public void setMetaDataInput(Map<String, String> metaDataInput) {
        this.metaDataInput = metaDataInput;
    }

    public void setMetaDataInputWithSuffix(Map<String, String> metaDataInputWithSuffix) {
        this.metaDataInputWithSuffix = metaDataInputWithSuffix;
    }

    public void setMetaDataOutput(Map<String, String> metaDataOutput) {
        this.metaDataOutput = metaDataOutput;
    }

}
