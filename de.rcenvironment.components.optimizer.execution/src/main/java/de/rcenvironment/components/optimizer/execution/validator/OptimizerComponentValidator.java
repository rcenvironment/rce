/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.execution.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractLoopComponentValidator;

/**
 * Validator for optimizer component.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel
 */
public class OptimizerComponentValidator extends AbstractLoopComponentValidator {

    @Override
    public String getIdentifier() {
        return OptimizerComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateLoopComponentSpecific(
        ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();

        final ComponentValidationMessage noInputMessage = new ComponentValidationMessage(
            ComponentValidationMessage.Type.ERROR, null, "No Objectives",
            "There are no objective functions defined.");
        if (getInputs(componentDescription).isEmpty()) {
            messages.add(noInputMessage);
        } else {
            Set<EndpointDescription> inputs = getInputs(componentDescription);
            boolean hasObjective = false;
            for (EndpointDescription input : inputs) {
                String dynamicEndpointIdentifier = input.getDynamicEndpointIdentifier();
                if (dynamicEndpointIdentifier != null && dynamicEndpointIdentifier.equals(OptimizerComponentConstants.ID_OBJECTIVE)) {
                    hasObjective = true;
                    break;
                }
            }
            if (!hasObjective) {
                messages.add(noInputMessage);
            }
        }
        final ComponentValidationMessage noOutputMessage = new ComponentValidationMessage(
            ComponentValidationMessage.Type.ERROR, null, "No Design variables",
            "There are no design variables defined.");
        if (getOutputs(componentDescription).isEmpty()) {
            messages.add(noOutputMessage);
        } else {
            Set<EndpointDescription> outputs = getOutputs(componentDescription);
            boolean hasDesign = false;
            for (EndpointDescription output : outputs) {
                String dynamicEndpointIdentifier = output.getDynamicEndpointIdentifier();
                if (dynamicEndpointIdentifier != null && dynamicEndpointIdentifier.equals(OptimizerComponentConstants.ID_DESIGN)) {
                    hasDesign = true;
                    break;
                }
            }
            if (!hasDesign) {
                messages.add(noOutputMessage);
            }
        }
        if (getProperty(componentDescription, OptimizerComponentConstants.ALGORITHMS) == null
            || getProperty(componentDescription, OptimizerComponentConstants.ALGORITHMS).isEmpty()) {
            final ComponentValidationMessage noAlgorithmMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, OptimizerComponentConstants.ALGORITHMS, "No Algorithm",
                "There is no algorithm chosen.");
            messages.add(noAlgorithmMessage);
        }
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        return null;
    }

}
