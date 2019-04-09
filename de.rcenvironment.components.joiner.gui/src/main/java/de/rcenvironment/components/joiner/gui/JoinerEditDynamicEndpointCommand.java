/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.joiner.gui;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * Special {@link EditDynamicEndpointCommand} for merger.
 * 
 * @author Sascha Zur
 */
public class JoinerEditDynamicEndpointCommand extends WorkflowNodeCommand {

    private final DataType newDataType;

    private DataType oldDataType;

    private int inputCount;
    
    public JoinerEditDynamicEndpointCommand(DataType newDataType) {
        super();
        this.newDataType = newDataType;
    }

    @Override
    public void initialize() {
        ConfigurationDescription config = getProperties().getConfigurationDescription();
        oldDataType = DataType.valueOf(config.getConfigurationValue(JoinerComponentConstants.DATATYPE));
        inputCount = Integer.valueOf(config.getConfigurationValue(JoinerComponentConstants.INPUT_COUNT));
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
            EndpointDescription newInput = EndpointDescription.copy(oldInput);
            newInput.setDataType(newDataType);
            getWorkflowNode().getInputDescriptionsManager()
                .editDynamicEndpointDescription(oldInput.getName(), oldInput.getName(), newInput.getDataType(), newInput.getMetaData());
        }

        EndpointDescription oldOutput = getWorkflowNode().getOutputDescriptionsManager()
            .getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME);
        EndpointDescription newOutput = EndpointDescription.copy(oldOutput);
        newOutput.setDataType(newDataType);
        getWorkflowNode().getOutputDescriptionsManager()
            .editStaticEndpointDescription(oldOutput.getName(), newOutput.getDataType(), newOutput.getMetaData());
        
        getProperties().getConfigurationDescription().setConfigurationValue(JoinerComponentConstants.DATATYPE, newDataType.name());
    }

    @Override
    public void undo() {
        for (int i = 1; i <= inputCount; i++) {
            EndpointDescription oldInput = getWorkflowNode().getInputDescriptionsManager()
                .getEndpointDescription(JoinerComponentConstants.INPUT_NAME + getString(i));
            EndpointDescription newInput = EndpointDescription.copy(oldInput);
            newInput.setDataType(oldDataType);
            getWorkflowNode().getInputDescriptionsManager()
                .editDynamicEndpointDescription(oldInput.getName(), oldInput.getName(), newInput.getDataType(), newInput.getMetaData());
        }

        EndpointDescription oldOutput = getWorkflowNode().getOutputDescriptionsManager()
            .getEndpointDescription(JoinerComponentConstants.OUTPUT_NAME);
        EndpointDescription newOutput = EndpointDescription.copy(oldOutput);
        newOutput.setDataType(oldDataType);
        getWorkflowNode().getOutputDescriptionsManager()
            .editStaticEndpointDescription(newOutput.getName(), newOutput.getDataType(), newOutput.getMetaData());

        getProperties().getConfigurationDescription().setConfigurationValue(JoinerComponentConstants.DATATYPE, oldDataType.name());
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
