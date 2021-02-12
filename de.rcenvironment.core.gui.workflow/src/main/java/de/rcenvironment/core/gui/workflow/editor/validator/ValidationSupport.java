/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidatorListService;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * 
 * This class gives an easy access point to the {@link ComponentValidatorListService}.
 * 
 * @author Jascha Riedel
 *
 */
public final class ValidationSupport {

    private static ValidationSupport instance = null;

    private ValidationSupport() {};

    /**
     * 
     * @return the instance of {@link ValidationSupport}.
     */
    public static ValidationSupport getInstance() {
        if (instance == null) {
            instance = new ValidationSupport();
        }
        return instance;
    }

    /**
     * Validates the given {@link ComponentDescription} using the {@link ComponentValidatorListService}.
     * 
     * @param componentDescription that will be validated.
     * @param onWorkflowStart additional validation steps that are only wanted on workflow start
     * @return List<{@linkComponentValidationMessage}> as result of the validation.
     */
    public List<ComponentValidationMessage> validate(ComponentDescription componentDescription,
        boolean onWorkflowStart) {
        if (!componentDescription.getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
            return ServiceRegistry.createAccessFor(this).getService(ComponentValidatorListService.class)
                .validateComponentDescription(componentDescription, onWorkflowStart);
        }
        return new LinkedList<>();
    }

}
