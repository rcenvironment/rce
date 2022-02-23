/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import de.rcenvironment.components.switchcmp.common.Messages;
import de.rcenvironment.components.switchcmp.common.ScriptValidation;
import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.common.SwitchCondition;
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
 * @author Kathrin Schaffert
 */

public class SwitchComponentValidator extends AbstractComponentValidator {

    private static final String EXCEPTION_MESSAGE_READING = "Unexpected Exception occured, while reading JSON content String.";

    @Override
    public String getIdentifier() {
        return SwitchComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<>();

        String configStr = getProperty(componentDescription, SwitchComponentConstants.CONDITION_KEY);

        if (configStr == null) {
            final ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY,
                Messages.noConditionKey, Messages.noConditionKey);
            messages.add(validationMessage);

            return messages;
        }

        ObjectMapper mapper = new ObjectMapper();
        CollectionType mapCollectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, SwitchCondition.class);
        ArrayList<SwitchCondition> contentList = new ArrayList<>();
        try {
            contentList = mapper.readValue(configStr, mapCollectionType);
        } catch (IOException e) {
            throw new RuntimeException(EXCEPTION_MESSAGE_READING, e); // should never happen
        }

        if (contentList != null && !contentList.isEmpty()) {
            for (int i = 0; i < contentList.size(); i++) {
                if (contentList.get(i).getConditionScript().trim().isEmpty()) {
                    final SwitchComponentValidationMessage emptyCondition = SwitchComponentValidationMessage.create(
                        ComponentValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY,
                        Messages.noConditionString, i + 1);
                    messages.add(emptyCondition);
                } else {
                    String errorMessage = ScriptValidation.validateScript(contentList.get(i).getConditionScript(),
                        getInputAndConnectionStatus(componentDescription), getInputsAndDataTypes(componentDescription),
                        this);
                    if (!errorMessage.isEmpty()) {
                        final SwitchComponentValidationMessage scriptError = SwitchComponentValidationMessage.create(
                            ComponentValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY, errorMessage,
                            i + 1);
                        messages.add(scriptError);
                    }
                }
            }
        } else {
            final ComponentValidationMessage emptyCondition = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, SwitchComponentConstants.CONDITION_KEY,
                Messages.noConditionString, Messages.noConditionString);
            messages.add(emptyCondition);
        }

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
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
