/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.joiner.gui;

import java.util.HashMap;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * Special {@link AddDynamicEndpointCommand} for merger.
 * 
 * @author Sascha Zur
 */
public class JoinerAddOrRemoveDynamicEndpointsCommand extends WorkflowNodeCommand {

    private final int newCount;

    private int oldInputCount;
    
    private DataType dataType;

    public JoinerAddOrRemoveDynamicEndpointsCommand(int newCount) {
        super();
        this.newCount = newCount;
    }

    @Override
    public void initialize() {
        ConfigurationDescription config = getProperties().getConfigurationDescription();
        oldInputCount = Integer.valueOf(config.getConfigurationValue(JoinerComponentConstants.INPUT_COUNT));
        dataType = DataType.valueOf(config.getConfigurationValue(JoinerComponentConstants.DATATYPE));
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
        if (newCount > oldInputCount) {
            for (int i = oldInputCount + 1; i <= newCount; i++) {
                getWorkflowNode().getInputDescriptionsManager().addDynamicEndpointDescription(
                    JoinerComponentConstants.DYNAMIC_INPUT_ID, JoinerComponentConstants.INPUT_NAME + getString(i), dataType,
                    new HashMap<String, String>());
            }
        } else if (newCount < oldInputCount) {
            for (int i = oldInputCount; i > newCount && i > 1; i--) {
                getWorkflowNode().getInputDescriptionsManager().removeDynamicEndpointDescription(
                    JoinerComponentConstants.INPUT_NAME + getString(i));
            }
        }
        getProperties().getConfigurationDescription().setConfigurationValue(JoinerComponentConstants.INPUT_COUNT, String.valueOf(newCount));
    }

    @Override
    public void undo() {
        if (newCount > oldInputCount) {
            for (int i = oldInputCount + 1; i <= newCount; i++) {
                getWorkflowNode().getInputDescriptionsManager().removeDynamicEndpointDescription(
                    JoinerComponentConstants.INPUT_NAME + getString(i));

            }
        } else if (newCount < oldInputCount) {
            for (int i = oldInputCount; i > newCount && i > 1; i--) {
                getWorkflowNode().getInputDescriptionsManager().addDynamicEndpointDescription(
                    JoinerComponentConstants.DYNAMIC_INPUT_ID, JoinerComponentConstants.INPUT_NAME + getString(i), dataType,
                    new HashMap<String, String>());
            }
        }
        getProperties().getConfigurationDescription().setConfigurationValue(JoinerComponentConstants.INPUT_COUNT,
            String.valueOf(oldInputCount));
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
