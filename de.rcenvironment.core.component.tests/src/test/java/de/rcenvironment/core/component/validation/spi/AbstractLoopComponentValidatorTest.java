/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.spi;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreater;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;

/**
 * 
 * Test for the {@link AbstractLoopComponentValidator}.
 *
 * @author Jascha Riedel
 */
public class AbstractLoopComponentValidatorTest {

    private AbstractLoopComponentValidator validator;

    private ComponentDescriptionMockCreater componentDescriptionMockCreater;

    private List<ComponentValidationMessage> messages;

    /** Basic setup, and creation of the actual validator. */
    @Before
    public void setUp() {
        validator = new AbstractLoopComponentValidator() {

            @Override
            public String getIdentifier() {
                return null;
            }

            @Override
            protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(ComponentDescription componentDescription) {
                return null;
            }

            @Override
            protected List<ComponentValidationMessage> validateLoopComponentSpecific(ComponentDescription componentDescription) {
                return null;
            }
        };

        componentDescriptionMockCreater = new ComponentDescriptionMockCreater();
    }

    /** Test Rerun And Fail but no max reruns set. */
    @Test
    public void testRerunAndFailButNoMaxReruns() {
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail.toString());
        validate();
        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
    }

    /** Test Rerun And Fail with max reruns set. */
    @Test
    public void testRerunAndFailWithMaxRerunsSet() {
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail.toString());
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV, "4");
        validate();
        assertEquals(0, messages.size());
    }

    /** Test Rerun and Discard but no max reruns set. */
    @Test
    public void testRerunAndDiscardButNoMaxRerunsSet() {
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopBehaviorInCaseOfFailure.RerunAndDiscard.toString());
        validate();
        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
    }

    /** Test Rerun and Discard with max reruns set. */
    @Test
    public void testRerunAndDiscardWithMaxRerunsSet() {
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV,
            LoopBehaviorInCaseOfFailure.RerunAndDiscard.toString());
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_DISCARD_NAV, "5");
        validate();
        assertEquals(0, messages.size());
    }

    private void validate() {
        messages = validator.validateComponentSpecific(componentDescriptionMockCreater.createComponentDescriptionMock());
    }
}
