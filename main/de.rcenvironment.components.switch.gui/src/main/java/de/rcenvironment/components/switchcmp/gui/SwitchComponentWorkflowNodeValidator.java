/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.switchcmp.common.ScriptValidation;
import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * 
 * Validator for switch component.
 * 
 * @author David Scholz
 */
public class SwitchComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<>();

        String condition = getProperty(SwitchComponentConstants.CONDITION_KEY);

        if (condition == null || condition.trim().isEmpty()) {
            final WorkflowNodeValidationMessage emptyCondition =
                new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY,
                    Messages.noConditionString, Messages.noConditionString);
            messages.add(emptyCondition);
        } else {
            String errorMessage = ScriptValidation.validateScript(condition, getInputAndConnectionStatus(), getInputsAndDataTypes(), this);
            if (!errorMessage.isEmpty()) {
                final WorkflowNodeValidationMessage scriptError =
                    new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR,
                        SwitchComponentConstants.CONDITION_KEY, errorMessage,
                        errorMessage);
                messages.add(scriptError);
            }
        }

        return messages;
    }

    private Map<String, DataType> getInputsAndDataTypes() {
        Map<String, DataType> inputs = new HashMap<>();
        for (EndpointDescription description : getInputs()) {
            inputs.put(description.getName(), description.getDataType());
        }
        return inputs;
    }

    private Map<String, Boolean> getInputAndConnectionStatus() {
        Map<String, Boolean> inputs = new HashMap<>();

        for (EndpointDescription description : getInputs()) {
            inputs.put(description.getName(), description.isConnected());
        }
        
        return inputs;
    }
}
