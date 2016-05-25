/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputWithAnotherInputAndOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputWithAnotherInputAndOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputWithAnotherPossibleInputAndOutputCommand;

/**
 * Input pane for forwarding endpoint that need just one output.
 *
 * @author Sascha Zur
 */
public class InputCoupledWithAnotherInputAndOutputSelectionPane extends ForwardingEndpointSelectionPane {

    private final String dynEndpointId;

    private final Refreshable outputPane;

    private final String inputNameSuffix;

    private Map<String, String> metaDataInput = new HashMap<>();

    private Map<String, String> metaDataInputWithSuffix = new HashMap<>();

    private Map<String, String> metaDataOutput = new HashMap<>();

    public InputCoupledWithAnotherInputAndOutputSelectionPane(String title, String dynEndpointId,
        String inputNameSuffix, WorkflowNodeCommand.Executor executor, Refreshable outputPane) {
        super(title, EndpointType.INPUT, executor, false, dynEndpointId, true, true);
        this.dynEndpointId = dynEndpointId;
        this.outputPane = outputPane;
        this.inputNameSuffix = inputNameSuffix;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        metaDataInput.putAll(metaData);
        metaDataInputWithSuffix.putAll(metaData);
        WorkflowNodeCommand command = new AddDynamicInputWithAnotherInputAndOutputCommand(dynEndpointId, name, type, metaDataInput,
            inputNameSuffix, LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane);
        ((AddDynamicInputWithAnotherInputAndOutputCommand) command).addMetaDataToInputWithSuffix(metaDataInputWithSuffix);
        ((AddDynamicInputWithAnotherInputAndOutputCommand) command).setMetaDataOutput(metaDataOutput);
        execute(command);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command =
            new EditDynamicInputWithAnotherInputAndOutputCommand(oldDescription, newDescription, inputNameSuffix,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, false, this, outputPane);
        ((EditDynamicInputWithAnotherInputAndOutputCommand) command).addMetaDataToInputWithSuffix(metaDataInputWithSuffix);
        ((EditDynamicInputWithAnotherInputAndOutputCommand) command).setMetaDataOutput(metaDataOutput);

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
    protected void updateButtonActivation() {
        super.updateButtonActivation();
        TableItem[] selection = table.getSelection();
        if (selection.length == 1 && selection[0].getText().endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
            buttonEdit.setEnabled(false);
            buttonRemove.setEnabled(false);
        }
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
