/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.testutils.ComponentDescriptionMockCreater;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * 
 * Tests the validator of the {@link EvaluationMemoryComponent}. Validator: {@link EvaluationMemoryComponentValidator}.
 *
 * @author Jascha Riedel
 */
public class EvaluationMemoryComponentValidatorTest {

    private ComponentDescriptionMockCreater componentDescriptionMockCreater;

    private EvaluationMemoryComponentValidator validator;

    private List<ComponentValidationMessage> messages;

    /** Basic setup for the tests. */
    @Before
    public void setUp() {
        componentDescriptionMockCreater = new ComponentDescriptionMockCreater();
        validator = new EvaluationMemoryComponentValidator();

    }

    /** Test validation if file is given before wf start but not set. */
    @Test
    public void testValidationIfFileIsGivenButNotSet() {
        componentDescriptionMockCreater.addConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START, "false");
        addSimulatedInput();
        messages = validator.validateComponentSpecific(componentDescriptionMockCreater.createComponentDescriptionMock());
        assertEquals(messages.size(), 1);
        assertEquals(ComponentValidationMessage.Type.ERROR, messages.get(0).getType());
    }

    /** Test validation if file is given before wf start and set. */
    @Test
    public void testValidationIfFileIsGivenAndSet() {
        componentDescriptionMockCreater.addConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START, "false");
        componentDescriptionMockCreater.addConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE, "testFile");
        addSimulatedInput();
        messages = validator.validateComponentSpecific(componentDescriptionMockCreater.createComponentDescriptionMock());
        assertTrue(messages.isEmpty());
    }

    /** Test if no inputs or outputs are defined. */
    @Test
    public void testValidationNoInputsOrOutputs() {
        componentDescriptionMockCreater.addConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START, "true");
        messages = validator.validateComponentSpecific(componentDescriptionMockCreater.createComponentDescriptionMock());
        assertEquals(1, messages.size());
        assertEquals(ComponentValidationMessage.Type.WARNING, messages.get(0).getType());
    }

    private void addSimulatedInput() {
        componentDescriptionMockCreater.addSimulatedInput("test", DataType.Float, new HashMap<String, String>(),
            EndpointDefinition.InputExecutionContraint.Required, false);
    }

}
