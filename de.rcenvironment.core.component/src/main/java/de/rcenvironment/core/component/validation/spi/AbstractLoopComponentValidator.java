/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.spi;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;

/**
 * Validator for loop components.
 * 
 * @author Doreen Seider
 * @author Jascha Riedel
 */
public abstract class AbstractLoopComponentValidator extends AbstractComponentValidator {

    protected abstract List<ComponentValidationMessage> validateLoopComponentSpecific(
        ComponentDescription componentDescription);

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();

        LoopBehaviorInCaseOfFailure loopBehaviorInCaseOfFailure = LoopBehaviorInCaseOfFailure.fromString(
            getProperty(componentDescription, LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV));

        if (loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndFail)) {
            if (getProperty(componentDescription, LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV) == null
                || getProperty(componentDescription, LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV)
                    .isEmpty()) {
                messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR,
                    LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV, "Define maximum of reruns",
                    "Maximum of reruns missing"));
            }
        }

        if (loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard)) {
            if (getProperty(componentDescription,
                LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_DISCARD_NAV) == null
                || getProperty(componentDescription, LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_DISCARD_NAV)
                    .isEmpty()) {
                messages.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR,
                    LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_DISCARD_NAV, "Define maximum of reruns",
                    "Maximum of reruns missing"));
            }
        }

        List<ComponentValidationMessage> loopComponentSpecificMessages = validateLoopComponentSpecific(componentDescription);
        if (loopComponentSpecificMessages != null) {
            messages.addAll(loopComponentSpecificMessages);
        }

        return messages;
    }

}
