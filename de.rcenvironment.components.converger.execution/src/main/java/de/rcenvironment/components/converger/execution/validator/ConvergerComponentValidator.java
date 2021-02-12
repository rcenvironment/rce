/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.converger.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.components.converger.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractLoopComponentValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for converger component.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel
 */
public class ConvergerComponentValidator extends AbstractLoopComponentValidator {

    @Override
    public String getIdentifier() {
        return ConvergerComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateLoopComponentSpecific(
        ComponentDescription componentDescription) {

        List<ComponentValidationMessage> messages = new ArrayList<>();

        final boolean hasInputs = hasInputs(componentDescription);

        if (!hasInputs) {
            final ComponentValidationMessage noInputMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.WARNING, "", Messages.noInput, Messages.noInput);
            messages.add(noInputMessage);
        }
        checkIfDefined(componentDescription, ConvergerComponentConstants.KEY_EPS_A, messages);
        checkIfDefined(componentDescription, ConvergerComponentConstants.KEY_EPS_R, messages);
        checkIfDefined(componentDescription, ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, messages);
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        return null;
    }

    private void checkIfDefined(ComponentDescription componentDescription, String key,
        List<ComponentValidationMessage> messages) {
        String prop = getProperty(componentDescription, key);

        if (prop == null || prop.isEmpty()) {
            String text = StringUtils.format("'%s' is not defined", getPropertyDisplayName(key));
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, key, text, text));
        }
    }

    private String getPropertyDisplayName(String key) {
        if (key.equals(ConvergerComponentConstants.KEY_EPS_A)) {
            return "Absolute convergence";
        } else if (key.equals(ConvergerComponentConstants.KEY_EPS_R)) {
            return "Relative convergence";
        } else if (key.equals(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER)) {
            return "Iterations to consider";
        } else {
            return "";
        }
    }

}
