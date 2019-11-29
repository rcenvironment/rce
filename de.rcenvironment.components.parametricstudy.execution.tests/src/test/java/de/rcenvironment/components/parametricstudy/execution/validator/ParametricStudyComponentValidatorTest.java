/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.execution.validator;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreater;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;

/**
 * 
 * Tests for the {@link ParametricStudyComponentValidator}.
 *
 * @author Jascha Riedel
 */
public class ParametricStudyComponentValidatorTest {

    private ComponentDescriptionMockCreater componentDescriptionMockCreater;

    private ParametricStudyComponentValidator validator;

    /** Basic setup. */
    @Before
    public void setUp() {
        componentDescriptionMockCreater = new ComponentDescriptionMockCreater();
        validator = new ParametricStudyComponentValidator();
    }

    /** Test wrong input configuration. */
    @Test
    public void testNoFlowControllingInputFromLoop() {
        componentDescriptionMockCreater.addConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, "true");
        List<ComponentValidationMessage> messages;
        messages = validator.validateLoopComponentSpecific(componentDescriptionMockCreater.createComponentDescriptionMock());
        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
    }

}
