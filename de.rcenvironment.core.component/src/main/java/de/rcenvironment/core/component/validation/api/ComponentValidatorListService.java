/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.api;

import java.util.List;

import de.rcenvironment.core.component.model.api.ComponentDescription;

/**
 * 
 * Interface for the ComponentValidatorListService that maps all component
 * validator services to their component.
 * 
 * @author Jascha Riedel
 *
 */
public interface ComponentValidatorListService {

    /**
     * 
     * @param validatorIdentifier
     *            that uniquely identifies the validator belonging to a
     *            component.
     * @return The {@link ComponentValidator} represented by this validatorId
     *
     *         ComponentValidator getValidator(String validatorIdentifier);
     */

    /**
     * Validates the given componentDescription if a linked validator is
     * registered.
     * 
     * @param componentDescription
     *            the description to validate
     * @param onWorkflowStart
     *            boolean that activate extra validation steps that are only
     *            required or make sense right before a workflow start
     * @return List<ComponentValidationMessage> list of messages
     */
    List<ComponentValidationMessage> validateComponentDescription(ComponentDescription componentDescription,
            boolean onWorkflowStart);

}
