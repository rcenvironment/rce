/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
            String errorMessage = ScriptValidation.validateScript(condition, getInputNames(), getSwitchInputDataType(), this);
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

    private DataType getSwitchInputDataType() {
        for (EndpointDescription description : getInputs()) {
            if (description.getName().equals(SwitchComponentConstants.DATA_INPUT_NAME)) {
                return description.getDataType();
            }
        }

        return null;
    }

    private List<String> getInputNames() {
        List<String> inputs = new ArrayList<>();

        for (EndpointDescription description : getInputs()) {
            inputs.add(description.getName());
        }

        return inputs;
    }
}
