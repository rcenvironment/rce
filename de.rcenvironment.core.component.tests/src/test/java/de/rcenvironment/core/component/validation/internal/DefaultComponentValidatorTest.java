/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreator;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * 
 * Test for the default Component Validator which in the end is used for every Component.
 *
 * @author Jascha Riedel
 */
public class DefaultComponentValidatorTest {

    private ComponentDescriptionMockCreator componentDescriptionMockCreater;

    private DefaultComponentValidator validator;

    private List<ComponentValidationMessage> messages;

    /** Basic setup. */
    @Before
    public void setUp() {
        componentDescriptionMockCreater = new ComponentDescriptionMockCreator();

        validator = new DefaultComponentValidator();
    }

    /** Test validator without any inputs. */
    @Test
    public void testWithoutAnyInputs() {
        messages = validator.validate(componentDescriptionMockCreater.createComponentDescriptionMock(), false);
        assertEquals(0, messages.size());
    }

    /** Test validator with not required input, not connected. */
    @Test
    public void testWithNotRequiredInputNotConnected() {
        validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint.NotRequired, false);
        assertEquals(0, messages.size());
    }

    /** Test validator with not required input, connected. */
    @Test
    public void testWithNotRequiredInputConnected() {
        validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint.NotRequired, true);
        assertEquals(0, messages.size());
    }

    /** Test with required if connected input, not connected. */
    @Test
    public void testWithRequiredIfConnectedNotConnected() {
        validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint.RequiredIfConnected, false);
        assertEquals(0, messages.size());
    }

    /** Test with required if connected input, connected. */
    @Test
    public void testWithRequiredIfConnectedConnected() {
        validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint.RequiredIfConnected, true);
        assertEquals(0, messages.size());
    }

    /** Test with required input, not connected. */
    @Test
    public void testWithRequiredNotConnected() {
        validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint.Required, false);
        assertEquals(1, messages.size());
    }

    /** Test with required input, connected. */
    @Test
    public void testWithRequiredConnected() {
        validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint.Required, true);
        assertEquals(0, messages.size());
    }

    /** Test with mixture of Inputs. */
    @Test
    public void testWithMixtureOfInputs() {
        componentDescriptionMockCreater.addSimulatedInput("test", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.NotRequired, false);
        componentDescriptionMockCreater.addSimulatedInput("test1", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.NotRequired, true);
        componentDescriptionMockCreater.addSimulatedInput("test2", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.RequiredIfConnected, false);
        componentDescriptionMockCreater.addSimulatedInput("test3", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.RequiredIfConnected, true);
        componentDescriptionMockCreater.addSimulatedInput("test4", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.Required, false);
        componentDescriptionMockCreater.addSimulatedInput("test5", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.Required, true);

        messages = validator.validate(componentDescriptionMockCreater.createComponentDescriptionMock(), false);
        assertEquals(1, messages.size());
    }

    private void validateOneInputWithExecutionConstraint(EndpointDefinition.InputExecutionContraint constraint, boolean isConnected) {
        componentDescriptionMockCreater.addSimulatedInput("test", DataType.Float, new HashMap<String, String>(),
            constraint, isConnected);
        messages = validator.validate(componentDescriptionMockCreater.createComponentDescriptionMock(), false);
    }
}
