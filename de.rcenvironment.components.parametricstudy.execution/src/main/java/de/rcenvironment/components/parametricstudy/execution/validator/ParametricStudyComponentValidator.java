/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage.Type;
import de.rcenvironment.core.component.validation.spi.AbstractLoopComponentValidator;

/**
 * 
 * Validator for Parametric Study Component.
 *
 * @author Jascha Riedel
 */
public class ParametricStudyComponentValidator extends AbstractLoopComponentValidator {

    @Override
    public String getIdentifier() {
        return ParametricStudyComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateLoopComponentSpecific(
            ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();

        getNestedLoopErrors(componentDescription, messages);

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

    private void getNestedLoopErrors(ComponentDescription componentDescription,
            List<ComponentValidationMessage> messages) {

        if (Boolean.valueOf(getProperty(componentDescription, LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP))) {
            Boolean hasFlowControllingInput = false;
            for (EndpointDescription inputDesc : getInputs(componentDescription)) {
                if (inputDesc.getDynamicEndpointIdentifier().equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)
                        || inputDesc.getDynamicEndpointIdentifier()
                                .equals(ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER)) {
                    hasFlowControllingInput = true;
                    break;
                }
            }

            if (!hasFlowControllingInput) {
                messages.add(new ComponentValidationMessage(Type.ERROR, null,
                        "In a nested loop the parametric study must have a controlling "
                                + "input (i. e. 'forwarding' or 'evaluation result' input).",
                        null));
            }
        }
    }

}
