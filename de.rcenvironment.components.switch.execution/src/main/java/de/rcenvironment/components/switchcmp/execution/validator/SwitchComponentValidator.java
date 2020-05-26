/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.switchcmp.common.ScriptValidation;
import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * 
 * Validator for switch component.
 * 
 * @author David Scholz
 */

public class SwitchComponentValidator extends AbstractComponentValidator {

    @Override
    public String getIdentifier() {
        return SwitchComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<>();

        String condition = getProperty(componentDescription, SwitchComponentConstants.CONDITION_KEY);

        if (condition == null || condition.trim().isEmpty()) {
            // TODO : add messages
            final ComponentValidationMessage emptyCondition = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY,
                    Messages.noConditionString, Messages.noConditionString);
            messages.add(emptyCondition);
        } else {
            String errorMessage = ScriptValidation.validateScript(condition,
                    getInputAndConnectionStatus(componentDescription), getInputsAndDataTypes(componentDescription),
                    this);
            if (!errorMessage.isEmpty()) {
                final ComponentValidationMessage scriptError = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY, errorMessage,
                        errorMessage);
                messages.add(scriptError);
            }
        }

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        // TODO Auto-generated method stub
        return null;
    }

    private Map<String, DataType> getInputsAndDataTypes(ComponentDescription componentDescription) {
        Map<String, DataType> inputs = new HashMap<>();
        for (EndpointDescription description : getInputs(componentDescription)) {
            inputs.put(description.getName(), description.getDataType());
        }
        return inputs;
    }

    private Map<String, Boolean> getInputAndConnectionStatus(ComponentDescription componentDescription) {
        Map<String, Boolean> inputs = new HashMap<>();

        for (EndpointDescription description : getInputs(componentDescription)) {
            inputs.put(description.getName(), description.isConnected());
        }

        return inputs;
    }

}
