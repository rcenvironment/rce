/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.joiner.execution.validator;

import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Component;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.component.validation.spi.ComponentValidator;

/**
 * Validator for the Joiner. Does not check anything. Every configuration is valid.
 * 
 * @author Kathrin Schaffert
 *
 */
@Component(service = ComponentValidator.class)
public class JoinerComponentValidator extends AbstractComponentValidator {

    @Override
    public String getIdentifier() {
        return JoinerComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        // this implementation is empty on purpose since there is nothing to validate for the joiner
        return new LinkedList<>();
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(ComponentDescription componentDescription) {
        // this implementation is empty on purpose since there is nothing to validate for the joiner
        return new LinkedList<>();
    }

}
