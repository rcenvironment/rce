/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.examples.decrypter.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.examples.decrypter.common.DecrypterComponentConstants;
import de.rcenvironment.components.examples.decrypter.execution.Messages;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;

/**
 * Validator for Decrypter component. This validator is used for checking if
 * some configuration is set correctly or if inputs/outputs were created. If a
 * validation is false, the GUI will automatically show it by coloring the
 * widget for the configuration red (error) or yellow (warning) and marking the
 * component in the workflow editor. Note that the validator must be set active
 * in the plugin.xml.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel
 */
public class DecrypterComponentValidator extends AbstractComponentValidator {

    @Override
    public String getIdentifier() {
        return DecrypterComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<ComponentValidationMessage>();

        // Check is algorithm is selected
        String algorithmSelection = getProperty(componentDescription, DecrypterComponentConstants.CONFIG_KEY_ALGORITHM);
        if (algorithmSelection == null || algorithmSelection.isEmpty()) {
            final ComponentValidationMessage noAlgorithmSelectedMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DecrypterComponentConstants.CONFIG_KEY_ALGORITHM,
                    Messages.noAlgorithmLarge, Messages.noAlgorithmSmall);
            messages.add(noAlgorithmSelectedMessage);
        }
        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
            ComponentDescription componentDescription) {
        return null;
    }

}
