/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.joiner.gui;

import java.util.HashMap;

import org.eclipse.swt.widgets.Combo;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * Special {@link AddDynamicEndpointCommand} for merger.
 * 
 * @author Sascha Zur
 */
public class JoinerAddOrRemoveDynamicEndpointsCommand extends WorkflowNodeCommand {

    private final int lastInputCount;

    private final int newCount;

    private final DataType lastDataType;

    private final Combo inputCount;

    private final EndpointSelectionPane inputPane;

    public JoinerAddOrRemoveDynamicEndpointsCommand(int currentInputCount, int lastInputCount, int newCount, DataType lastDataType,
        Combo inputCount, EndpointSelectionPane inputPane) {
        super();
        this.lastInputCount = lastInputCount;
        this.newCount = newCount;
        this.lastDataType = lastDataType;
        this.inputCount = inputCount;
        this.inputPane = inputPane;
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

        if (newCount > lastInputCount) {
            for (int i = lastInputCount + 1; i <= newCount; i++) {
                getWorkflowNode().getInputDescriptionsManager().addDynamicEndpointDescription(
                    JoinerComponentConstants.DYNAMIC_INPUT_ID, JoinerComponentConstants.INPUT_NAME + getString(i), lastDataType,
                    new HashMap<String, String>());
            }
        } else if (newCount < lastInputCount) {
            for (int i = lastInputCount; i > newCount && i > 1; i--) {
                getWorkflowNode().getInputDescriptionsManager().removeDynamicEndpointDescription(
                    JoinerComponentConstants.INPUT_NAME + getString(i));
            }
        }
        if (inputCount != null) {
            inputCount.select(newCount - 1);
        }
        if (inputPane != null) {
            inputPane.refresh();
        }
    }

    @Override
    public void undo() {
        if (newCount > lastInputCount) {
            for (int i = lastInputCount + 1; i <= newCount; i++) {
                getWorkflowNode().getInputDescriptionsManager().removeDynamicEndpointDescription(
                    JoinerComponentConstants.INPUT_NAME + getString(i));

            }
        } else if (newCount < lastInputCount) {
            for (int i = lastInputCount; i > newCount && i > 1; i--) {
                getWorkflowNode().getInputDescriptionsManager().addDynamicEndpointDescription(
                    JoinerComponentConstants.DYNAMIC_INPUT_ID, JoinerComponentConstants.INPUT_NAME + getString(i), lastDataType,
                    new HashMap<String, String>());
            }
        }
        if (inputCount != null) {
            inputCount.select(lastInputCount - 1);
        }
        if (inputPane != null) {
            inputPane.refresh();
        }
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
