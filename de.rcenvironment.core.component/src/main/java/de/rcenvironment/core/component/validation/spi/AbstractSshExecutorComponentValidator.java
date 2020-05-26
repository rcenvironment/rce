/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.spi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate cluster
 * component configuration.
 * 
 * @author Doreen Seider
 * @author Jascha Riedel
 */
public abstract class AbstractSshExecutorComponentValidator extends AbstractComponentValidator {

    /**
     * Return null if there are no validation steps necessary.
     * 
     * @param componentDescription
     * @return
     */
    protected abstract List<ComponentValidationMessage> validateSshComponentSpecific(
            ComponentDescription componentDescription);

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        final List<ComponentValidationMessage> messages = new ArrayList<>();

        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_HOST));
        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_PORT));
        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_SANDBOXROOT));
        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_SCRIPT));

        List<ComponentValidationMessage> componentSpecificMessages = validateSshComponentSpecific(componentDescription);
        if (componentSpecificMessages != null) {
            messages.addAll(validateSshComponentSpecific(componentDescription));
        }

        return messages;
    }

    protected List<ComponentValidationMessage> checkIfStringIsConfigured(ComponentDescription componentDescription,
            String configurationKey) {
        final List<ComponentValidationMessage> messages = new LinkedList<>();

        final String value = getProperty(componentDescription, configurationKey);
        if (value == null || value.isEmpty()) {
            ComponentValidationMessage validationMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, configurationKey,
                    StringUtils.format("%s must be configured", configurationKey),
                    StringUtils.format("%s must be configured", configurationKey));
            messages.add(validationMessage);
        }

        return messages;
    }

}
