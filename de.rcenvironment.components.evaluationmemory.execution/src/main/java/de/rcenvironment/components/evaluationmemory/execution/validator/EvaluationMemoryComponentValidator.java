/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Validator for Evaluation Memory component.
 * 
 * @author Doreen Seider
 * @author Jascha Riedel
 */
public class EvaluationMemoryComponentValidator extends AbstractComponentValidator {

    @Override
    public String getIdentifier() {
        return EvaluationMemoryComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {

        List<ComponentValidationMessage> messages = new ArrayList<>();

        boolean selectAtWfStart = Boolean.valueOf(
            getProperty(componentDescription, EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START));

        if (!selectAtWfStart) {
            String memoryFile = getProperty(componentDescription,
                EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE);
            if (memoryFile == null || memoryFile.trim().isEmpty()) {
                final ComponentValidationMessage noFile = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE,
                    "Define a evaluation memory file", "No memory file given");
                messages.add(noFile);
            }
        }
        checkIfAtLeastInputsOrOutputsAreDefined(componentDescription, messages);
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        return null;
    }

    private void checkIfAtLeastInputsOrOutputsAreDefined(ComponentDescription componentDescription,
        List<ComponentValidationMessage> messages) {
        if (getInputs(componentDescription).size() < 1) {
            messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.WARNING, null, "",
                "No inputs/outputs defined"));
        }
    }

}
