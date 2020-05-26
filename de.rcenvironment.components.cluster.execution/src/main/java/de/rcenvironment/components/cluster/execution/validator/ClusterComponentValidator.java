/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.cluster.execution.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractSshExecutorComponentValidator;

/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate cluster
 * component configuration.
 * 
 * @author Doreen Seider
 * @author Jascha Riedel
 */
public class ClusterComponentValidator extends AbstractSshExecutorComponentValidator {
    // this class does not actually extend the
    // AbstractSshExecutorComponentVaidator. It only needs its methods.

    @Override
    public String getIdentifier() {
        return ClusterComponentConstants.COMPONENT_ID;
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

    @Override
    protected List<ComponentValidationMessage> validateSshComponentSpecific(ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();

        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_HOST));
        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_PORT));
        messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_SANDBOXROOT));
        if (!Boolean.valueOf(
                getProperty(componentDescription, ClusterComponentConstants.KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR))) {
            messages.addAll(checkIfStringIsConfigured(componentDescription, SshExecutorConstants.CONFIG_KEY_SCRIPT));
        }

        return messages;
    }

}
