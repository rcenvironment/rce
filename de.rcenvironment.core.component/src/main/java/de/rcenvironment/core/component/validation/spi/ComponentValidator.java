/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.spi;

import java.util.List;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;

/**
 * All class implementing this interface will be added to the validator
 * registry.
 * 
 * @author Jascha Riedel
 *
 */
public interface ComponentValidator {

    /**
     * Validates the provided {@link ComponentDescription} and returns a List of
     * {@link ComponentValidationMessage} as result. May be empty but never
     * null.
     * 
     * @param componentDescription
     *            {@link ComponentDescription} to validate.
     * @param onWorkflowStart
     *            Boolean that determines whether additional validation steps
     *            only required at workflow start are done.
     * @return List<ComponentValidationMessage> of created validation messages.
     */
    List<ComponentValidationMessage> validate(ComponentDescription componentDescription, boolean onWorkflowStart);

    /**
     * Returns the ID of the component that the validator belongs to. (Can be
     * retrieved from the Component Constants belonging to this component).
     * 
     * @return Identifier of the componet
     */
    String getIdentifier();
}
