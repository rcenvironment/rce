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

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
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

        if (getProperty(componentDescription, OptimizerComponentConstants.ALGORITHMS) == null
                || getProperty(componentDescription, OptimizerComponentConstants.ALGORITHMS).isEmpty()) {
            // TODO : Add Messages
            final ComponentValidationMessage noAlgorithmMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, OptimizerComponentConstants.ALGORITHMS, "No Algorithm",
                    "Message no algorithm selected placeholder");
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
