/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.internal;

import java.util.List;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Represents the default validation and is used if no validator is defined.
 * 
 * @author Jascha Riedel
 *
 */
public class DefaultComponentValidator extends AbstractComponentValidator {

    /** Id of Default Validator. */
    public static final String DEFAULT_COMPONENT_VALIDATOR_ID = "de.rcenviornment.defaultComponentValidator";

    @Override
    public String getIdentifier() {
        return DEFAULT_COMPONENT_VALIDATOR_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        return null;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

}
