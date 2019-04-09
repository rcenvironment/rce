/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.execution.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreater;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * 
 * Tests for the {@link ConvergerComponentValidator}.
 *
 * @author Jascha Riedel
 */
public class ConvergerComponentValidatorTest {

    private ComponentDescriptionMockCreater componentDescription;

    private ConvergerComponentValidator validator;

    private List<ComponentValidationMessage> messages;

    /** Basic setup. */
    @Before
    public void setUp() {
        componentDescription = new ComponentDescriptionMockCreater();
        validator = new ConvergerComponentValidator();
    }

    /** Tests validation for no Inputs. */
    @Test
    public void testNoInputs() {
        messages = validator.validateLoopComponentSpecific(componentDescription.createComponentDescriptionMock());
        assertEquals(ComponentValidationMessage.Type.WARNING, messages.get(0).getType());
    }

    /** Test whether an input is recognized. */
    @Test
    public void testWithInputs() {

        addSimulatedInput();
        messages = validator.validateLoopComponentSpecific(componentDescription.createComponentDescriptionMock());
        assertEquals(3, messages.size());
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
    }

    /** Tests if validator is happy if all values are correct. */
    @Test
    public void testAllConfigValuesSet() {
        addSimulatedInput();
        componentDescription.addConfigurationValue(ConvergerComponentConstants.KEY_EPS_A, "dummy");
        componentDescription.addConfigurationValue(ConvergerComponentConstants.KEY_EPS_R, "dummy2");
        componentDescription.addConfigurationValue(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, "dummy3");
        messages = validator.validateLoopComponentSpecific(componentDescription.createComponentDescriptionMock());

        assertEquals(0, messages.size());
    }

    /** Test configuration individually. */
    @Test
    public void testEachConfigValue() {
        addSimulatedInput();

        componentDescription.addConfigurationValue(ConvergerComponentConstants.KEY_EPS_A, "a");
        messages = validator.validateLoopComponentSpecific(componentDescription.createComponentDescriptionMock());
        for (ComponentValidationMessage message : messages) {
            assertFalse(message.getProperty().equals(ConvergerComponentConstants.KEY_EPS_A));
        }
        componentDescription = new ComponentDescriptionMockCreater();
        addSimulatedInput();
        componentDescription.addConfigurationValue(ConvergerComponentConstants.KEY_EPS_R, "b");
        messages = validator.validateLoopComponentSpecific(componentDescription.createComponentDescriptionMock());
        for (ComponentValidationMessage message : messages) {
            assertFalse(message.getProperty().equals(ConvergerComponentConstants.KEY_EPS_R));
        }
        componentDescription = new ComponentDescriptionMockCreater();
        addSimulatedInput();
        componentDescription.addConfigurationValue(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, "c");
        messages = validator.validateLoopComponentSpecific(componentDescription.createComponentDescriptionMock());
        for (ComponentValidationMessage message : messages) {
            assertFalse(message.getProperty().equals(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER));
        }
    }

    private void addSimulatedInput() {
        componentDescription.addSimulatedInput("testInput", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.NotRequired, true);
    }
}
