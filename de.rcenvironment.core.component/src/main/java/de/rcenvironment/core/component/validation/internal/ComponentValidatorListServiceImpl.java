/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidatorListService;
import de.rcenvironment.core.component.validation.spi.ComponentValidator;

/**
 * Implementation of the {@link ComponentValidatorListService}.
 * 
 * @author Jascha Riedel
 *
 */
public class ComponentValidatorListServiceImpl implements ComponentValidatorListService {

    private Map<String, ComponentValidator> validatorMap = new HashMap<>();

    @Override
    public List<ComponentValidationMessage> validateComponentDescription(ComponentDescription componentDescription,
            boolean onWorkflowStart) {
        String identifier = componentDescription.getComponentInterface().getIdentifierAndVersion()
                .split(ComponentConstants.ID_SEPARATOR)[0];
        if (validatorMap.containsKey(identifier)) {
            return validatorMap.get(identifier).validate(componentDescription, onWorkflowStart);
        } else {
            LogFactory.getLog(this.getClass()).debug("There is no validator registered for the identifier: "
                    + identifier + ". Default Validator used for: " + componentDescription.getName());
            return validatorMap.get(DefaultComponentValidator.DEFAULT_COMPONENT_VALIDATOR_ID)
                    .validate(componentDescription, onWorkflowStart);
        }

    }

    /**
     * Bind method to add the a componentValidator.
     * 
     * @param componentValidator {@link ComponentValidator} to add
     */
    public void addComponentValidator(ComponentValidator componentValidator) {
        validatorMap.put(componentValidator.getIdentifier(), componentValidator);
    }

    /**
     * Unbind method to remove a componentValidator.
     * 
     * @param componentValidator {@link ComponentValidator} to remove
     */
    public void removeComponentValidator(ComponentValidator componentValidator) {
        validatorMap.remove(componentValidator.getIdentifier());
    }

}
