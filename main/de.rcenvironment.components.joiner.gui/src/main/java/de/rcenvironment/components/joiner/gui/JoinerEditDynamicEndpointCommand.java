/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.joiner.gui;

import org.eclipse.swt.widgets.Combo;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * Special {@link EditDynamicEndpointCommand} for merger.
 * 
 * @author Sascha Zur
 */
public class JoinerEditDynamicEndpointCommand extends WorkflowNodeCommand {

    private int inputCount;

    private DataType newDataType;

    private Refreshable inputPane;

    private Refreshable outputPane;

    private DataType oldDataType;

    private Combo dataTypeCombo;

    public JoinerEditDynamicEndpointCommand(int lastInputCount, DataType oldDataType, DataType newDataType, Combo dataTypes,
        Refreshable inputPane,
        Refreshable outputPane) {
        super();
        this.inputCount = lastInputCount;
        this.oldDataType = oldDataType;
        this.newDataType = newDataType;
        this.inputPane = inputPane;
        this.outputPane = outputPane;
        this.dataTypeCombo = dataTypes;
    }

    @Override
    public void initialize() {

    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public void execute() {
        for (int i = 1; i <= inputCount; i++) {
            EndpointDescription oldInput = getWorkflowNode().getInputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.INPUT_NAME + getString(i));
            EndpointDescription newInput = oldInput.clone();
            newInput.setDataType(newDataType);
            getWorkflowNode().getInputDescriptionsManager()
                .editDynamicEndpointDescription(oldInput.getName(), oldInput.getName(), newInput.getDataType(), newInput.getMetaData());
        }

        EndpointDescription oldOutput = getWorkflowNode().getOutputDescriptionsManager()
            .getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME);
        EndpointDescription newOutput = oldOutput.clone();
        newOutput.setDataType(newDataType);
        getWorkflowNode().getOutputDescriptionsManager()
            .editStaticEndpointDescription(oldOutput.getName(), newOutput.getDataType(), newOutput.getMetaData());

        if (inputPane != null) {
            inputPane.refresh();
        }
        if (outputPane != null) {
            outputPane.refresh();
        }
        dataTypeCombo.select(dataTypeCombo.indexOf(newDataType.getDisplayName()));
    }

    @Override
    public void undo() {
        for (int i = 1; i <= inputCount; i++) {
            EndpointDescription oldInput = getWorkflowNode().getInputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.INPUT_NAME + getString(i));
            EndpointDescription newInput = oldInput.clone();
            newInput.setDataType(oldDataType);
            getWorkflowNode().getInputDescriptionsManager()
                .editDynamicEndpointDescription(oldInput.getName(), oldInput.getName(), newInput.getDataType(), newInput.getMetaData());
        }

        EndpointDescription oldOutput = getWorkflowNode().getOutputDescriptionsManager()
            .getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME);
        EndpointDescription newOutput = oldOutput.clone();
        newOutput.setDataType(oldDataType);
        getWorkflowNode().getOutputDescriptionsManager()
            .editStaticEndpointDescription(newOutput.getName(), newOutput.getDataType(), newOutput.getMetaData());

        if (inputPane != null) {
            inputPane.refresh();
        }
        if (outputPane != null) {
            outputPane.refresh();
        }
        dataTypeCombo.select(dataTypeCombo.indexOf(oldDataType.getDisplayName()));
    }

    // Adds zeros if i is less than 100 so that the order is right.
    private String getString(int i) {
        String result = "";
        if (i < 10) {
            result += "0";
        }
        if (i < 10 * 10) {
            result += "0";
        }
        result += i;
        return result;
    }

}
