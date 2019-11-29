/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.joiner.execution;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;

/**
 * 
 * Test for {@link JoinerComponent}.
 * 
 * @author Tobias Rodehutskors
 */
public class JoinerComponentTest {

    private static final String INPUT_X = "x";

    private static final String INPUT_Y = "y";

    /**
     * Expected fails.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ComponentTestWrapper component;

    private ComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    /**
     * Common setup.
     *
     * @throws IOException e
     */
    @Before
    public void setUp() throws IOException {
        context = new ComponentContextMock();
        component = new ComponentTestWrapper(new JoinerComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();
    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();
    }

    /**
     * 
     * Tests behavior if a single input value is available.
     * 
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testSingleInput() throws ComponentException {
        context.addSimulatedOutput(JoinerComponentConstants.OUTPUT_NAME, JoinerComponentConstants.OUTPUT_NAME, DataType.Float, false, null);
        FloatTD inputValue = typedDatumFactory.createFloat(3.0);
        context.setInputValue(INPUT_X, inputValue);

        component.start();
        component.processInputs();

        Assert.assertEquals(1, context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).size());
        Assert.assertEquals(inputValue, context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).get(0));
    }

    /**
     * 
     * Tests behavior if two input values are available to two different inputs.
     * 
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testParallelInput() throws ComponentException {
        context.addSimulatedOutput(JoinerComponentConstants.OUTPUT_NAME, JoinerComponentConstants.OUTPUT_NAME, DataType.Float, false, null);
        FloatTD inputValue1 = typedDatumFactory.createFloat(3.0);
        FloatTD inputValue2 = typedDatumFactory.createFloat(4.0);
        context.setInputValue(INPUT_X, inputValue1);
        context.setInputValue(INPUT_Y, inputValue2);

        component.start();
        component.processInputs();

        Assert.assertEquals(2, context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).size());
        Assert.assertTrue(context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).contains(inputValue1));
        Assert.assertTrue(context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).contains(inputValue2));
    }

    /**
     * 
     * Tests behavior if single input values are available in different processing steps to the same input.
     * 
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testSerialInput() throws ComponentException {
        context.addSimulatedOutput(JoinerComponentConstants.OUTPUT_NAME, JoinerComponentConstants.OUTPUT_NAME, DataType.Float, false, null);
        FloatTD inputValue1 = typedDatumFactory.createFloat(3.0);
        context.setInputValue(INPUT_X, inputValue1);

        component.start();
        component.processInputs();

        Assert.assertEquals(1, context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).size());
        Assert.assertTrue(context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).contains(inputValue1));

        FloatTD inputValue2 = typedDatumFactory.createFloat(4.0);
        context.setInputValue(INPUT_X, inputValue2);
        component.processInputs();

        Assert.assertEquals(1, context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).size());
        Assert.assertTrue(context.getCapturedOutput(JoinerComponentConstants.OUTPUT_NAME).contains(inputValue2));
    }
}
