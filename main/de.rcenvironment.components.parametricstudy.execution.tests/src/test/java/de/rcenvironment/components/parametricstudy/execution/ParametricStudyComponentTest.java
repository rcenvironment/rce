/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.execution;

import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.components.parametricstudy.common.ParametricStudyService;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyStructure;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.Component.FinalComponentState;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * Integration test for {@link ParametricStudyComponent}.
 * 
 * @author Oliver Seebach
 * @author Doreen Seider
 * @author Jascha Riedel
 */
public class ParametricStudyComponentTest {

    private static final int FIVE_HUNDRED_INT = 500;
    
    private static final int HUNDRED_THOUSAND_INT = 100000;

    private static final String ONE_ONE = "1.1";

    private static final String DONE = "Done";

    private static final Double SOME_DOUBLE = 5.0;

    private static final int LARGE_NUMBER = 100000;

    private static final String FIT_STEP_SIZE_TO_BOUNDS = "fitStepSizeToBounds";

    private static final String FIVE = "5";

    private static final String TEN = "10";

    private static final String ONE = "1";

    private static final String TWO = "2";

    private static final String MINUS = "-";

    private static final String RETURN_VALUE = "ReturnValue";

    private static final String RETURN_VALUE_2 = "AnotherReturnValue";

    private static final String STEP_SIZE = "StepSize";

    private static final String TO_VALUE = "ToValue";

    private static final String FROM_VALUE = "FromValue";

    private static final String DESIGN_VARIABLE = "Design variable";

    private static final String N = "n";

    /** Exception rule. */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Context mockup for parametric study component tests.
     * 
     * @author Oliver Seebach
     */
    private final class ParametricStudyComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = -6574116384120957764L;

    }

    private ComponentTestWrapper component;

    private ParametricStudyComponentContextMock context;

    private ParametricStudyService parametricStudyServiceMock;

    private TypedDatumFactory typedDatumFactory;

    /**
     * Set up Parametric Study tests.
     * 
     * @throws Exception e
     */
    @Before
    public void setUp() throws Exception {
        context = new ParametricStudyComponentContextMock();
        component = new ComponentTestWrapper(new ParametricStudyComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();

        // Create StudyPublisher mock required by ParametricStudyService mock
        StudyPublisher studyPublisherMock = EasyMock.createMock(StudyPublisher.class);
        EasyMock.expect(studyPublisherMock.getStudy()).andReturn(null);
        studyPublisherMock.add(anyObject(StudyDataset.class));
        EasyMock.expectLastCall().anyTimes();
        studyPublisherMock.clearStudy();
        EasyMock.replay(studyPublisherMock);

        // Create ParametricStudyService mock
        parametricStudyServiceMock = EasyMock.createNiceMock(ParametricStudyService.class);
        EasyMock.expect(parametricStudyServiceMock.createPublisher(anyObject(String.class), anyObject(String.class),
            anyObject(StudyStructure.class))).andReturn(studyPublisherMock).anyTimes();
        EasyMock.replay(parametricStudyServiceMock);
        context.addService(ParametricStudyService.class, parametricStudyServiceMock);
    }

    /**
     * Generates meta data for parametric study test.
     * 
     * @param from Start value of parametric study range
     * @param to End value of parametric study range
     * @param stepSize Step size of parametric study range
     * @return
     */
    private Map<String, String> generateParametricStudyMetadata(String from, Boolean useInputAsFrom, String to, Boolean useInputAsTo,
        String stepSize, Boolean useInputAsStepSize, Boolean fitStepSizeToBounds) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(FROM_VALUE, from);
        metadata.put(ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_FROM_VALUE, useInputAsFrom.toString());
        metadata.put(TO_VALUE, to);
        metadata.put(ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_TO_VALUE, useInputAsTo.toString());
        metadata.put(STEP_SIZE, stepSize);
        metadata.put(ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_STEPSIZE_VALUE, useInputAsStepSize.toString());
        metadata.put(FIT_STEP_SIZE_TO_BOUNDS, fitStepSizeToBounds.toString());
        return metadata;
    }

    /**
     * Test with no input and output from 1-10.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testNoInputsSimple() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TEN, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        component.start();

        assertEquals(10, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        checkDoneOutputs(true);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with no input and output from 1-10.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testNoInputsSimpleDescending() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(TEN, false, ONE, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        component.start();

        assertEquals(10, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        checkDoneOutputs(true);
        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with one input and output from 1-5.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void test1InputSimple() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, FIVE, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);

        component.start();
        checkDoneOutputs(false);

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(1.0));

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        checkDoneOutputs(false);
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(2.0));

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        checkDoneOutputs(false);
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(3.0));

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        checkDoneOutputs(false);
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(4.0));

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        checkDoneOutputs(false);
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(5.0));

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        checkDoneOutputs(true);
        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with one input and output from 1-5.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void test1InputSimpleDescending() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(FIVE, false, ONE, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);

        component.start();
        checkDoneOutputs(false);

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(typedDatumFactory.createFloat(5.0), context.getCapturedOutput(DESIGN_VARIABLE).get(0));

        for (double i = 4; i > 0; i--) {
            context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
            component.processInputs();
            checkDoneOutputs(false);
            assertEquals(typedDatumFactory.createFloat(i), context.getCapturedOutput(DESIGN_VARIABLE).get(0));
        }

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        checkDoneOutputs(true);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with one input and output from 1-5.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void test2InputsSimple() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, FIVE, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);
        context.addSimulatedInput(RETURN_VALUE_2, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);

        component.start();
        checkDoneOutputs(false);
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(1.0));

        for (double i = 2; i <= 5; i++) {
            context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
            context.setInputValue(RETURN_VALUE_2, typedDatumFactory.createFloat(SOME_DOUBLE));
            component.processInputs();
            checkDoneOutputs(false);
            assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(i));
        }

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(SOME_DOUBLE));
        context.setInputValue(RETURN_VALUE_2, typedDatumFactory.createFloat(SOME_DOUBLE));
        component.processInputs();
        checkDoneOutputs(true);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with no input and output from 1-10.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testNoInputsManyIterations() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, String.valueOf(LARGE_NUMBER), false, ONE, false, false);
        addSimulatedOutputs(metadata);
        component.start();

        assertEquals(LARGE_NUMBER, context.getCapturedOutput(DESIGN_VARIABLE).size());

        checkDoneOutputs(true);
        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with one input and output from 1-5.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void test1InputManyIterations() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, String.valueOf(LARGE_NUMBER), false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);

        component.start();
        checkDoneOutputs(false);
        for (int i = 2; i <= LARGE_NUMBER; i++) {
            context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
            component.processInputs();
            checkDoneOutputs(false);
            assertEquals(typedDatumFactory.createFloat(i), context.getCapturedOutput(DESIGN_VARIABLE).get(0));
        }

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
        component.processInputs();
        checkDoneOutputs(true);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with no input and output from 1-10.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testInvalidStepSize() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(TEN, false, ONE, false, "-1.1", false, true);
        context.addSimulatedOutput(DESIGN_VARIABLE, "", DataType.Float, false, metadata);

        exception.expect(ComponentException.class);

        component.start();

        component.tearDownAndDispose(Component.FinalComponentState.FAILED);
    }

    /**
     * Test with one input and output from 1-5.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void test1InputLargeNumberNonIntegerStepWidth() throws ComponentException {
        final double stepWidth = 0.123;
        Map<String, String> metadata = generateParametricStudyMetadata("0", false, String.valueOf(LARGE_NUMBER), false,
            String.valueOf(stepWidth), false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);

        component.start();

        for (double i = stepWidth; i <= LARGE_NUMBER; i += stepWidth) {
            context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
            component.processInputs();
            checkDoneOutputs(false);
            final double delta = 0.1;
            assertEquals(i, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), delta);
        }

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
        component.processInputs();
        checkDoneOutputs(true, true, 0);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test without input and fit to step width.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testNoInputsFitToStepWidth() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TEN, false, ONE_ONE, false, true);
        addSimulatedOutputs(metadata);
        component.start();

        // Component takes step size one size bigger to match the given end value
        // I.e. in this case 1.125 instead of 1.1

        assertEquals(9, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 1.0, 2.125, 3.25, 4.375, 5.5, 6.625, 7.75, 8.875, 10.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with 1 input and fit to step width.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void test1InputFitToStepWidth() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TEN, false, ONE_ONE, false, true);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);
        component.start();

        // Component takes step size one size bigger to match the given end value
        // I.e. in this case 1.125 instead of 1.1

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(context.getCapturedOutput(DESIGN_VARIABLE).get(0), typedDatumFactory.createFloat(1.0));

        final double stepWidth = 1.125;

        for (double i = (1 + stepWidth); i < 10; i += stepWidth) {
            context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
            component.processInputs();
            assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), i));
        }

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test with no inputs, fit to step width and descending values.
     * 
     * @throws ComponentException ce
     */
    @Test
    public void testNoInputsFitToStepWidthDescending() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(TEN, false, ONE, false, ONE_ONE, false, true);
        addSimulatedOutputs(metadata);
        component.start();

        // Component takes step size one size bigger to match the given end value
        // I.e. in this case -1.125 instead of -1.1

        assertEquals(9, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 10.0, 8.875, 7.75, 6.625, 5.5, 4.375, 3.25, 2.125, 1.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Tests if values are forwarded as expected.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testForwardingValue() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TWO, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true, null);
        context.addSimulatedInput(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, DataType.Integer, true, null, EndpointCharacter.OUTER_LOOP);
        context.addSimulatedOutput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true, null);

        component.start();
        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createInteger(3));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        assertEquals(3, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue());

        context.setInputValue(N, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(7, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue());
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);

        context.setInputValue(N, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        checkDoneOutputs(true, true, 1);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Tests if values are forwarded as expected.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testForwardingStartValue() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TWO, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true, null);
        context.addSimulatedInput(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, DataType.Integer, true, null, EndpointCharacter.OUTER_LOOP);
        context.addSimulatedOutput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true, null);
        component.start();

        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createInteger(5));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(5, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue(), 0);
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);

        context.setInputValue(N, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(7, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue());
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);

        context.setInputValue(N, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        checkDoneOutputs(true, true, 1);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Tests if values are forwarded as expected.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testReset() throws ComponentException {
        context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(true));
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TWO, false, ONE, false, false);
        addSimulatedOutputs(metadata);
        context.addSimulatedInput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true, null);
        context.addSimulatedInput(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, DataType.Integer, true, null, EndpointCharacter.OUTER_LOOP);
        context.addSimulatedOutput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true, null);

        component.start();

        testOneLoopRun();
        testOneLoopRun();

    }

    /**
     * Tests if values are forwarded as expected.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testDataTypes() throws ComponentException {
        context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(true));
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, TWO, false, ONE, false, false);
        addSimulatedOutputs(metadata);

        context.addSimulatedInput(RETURN_VALUE, "", DataType.Float, true, new HashMap<String, String>());
        context.addSimulatedInput(RETURN_VALUE_2, "", DataType.Integer, true, new HashMap<String, String>());
        final double value = 5.4;
        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(value));
        context.setInputValue(RETURN_VALUE_2, typedDatumFactory.createInteger(2));
        component.start();
        component.processInputs();
    }

    /**
     * 
     * Tests the "From Value" Input as simple test without any other Inputs.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testInputFromValueOnlyParameterInputs() throws ComponentException {
        Map<String, String> metaData = generateParametricStudyMetadata(MINUS, true, TEN, false, ONE, false, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);

        component.start();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE, typedDatumFactory.createFloat(1.0));
        component.processInputs();
        assertEquals(10, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * 
     * Tests the "To Value" Input as simple test without any other Inputs.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testInputToValueOnlyParameterInputs() throws ComponentException {
        Map<String, String> metaData = generateParametricStudyMetadata(ONE, false, MINUS, true, ONE, false, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);

        component.start();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE, typedDatumFactory.createFloat(10));
        component.processInputs();
        assertEquals(10, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * 
     * Tests the "StepSize Value" Input as simple test without any other Inputs.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testInputStepSizeValueOnlyParameterInputs() throws ComponentException {
        Map<String, String> metaData = generateParametricStudyMetadata(ONE, false, TEN, false, MINUS, true, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);

        component.start();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE, typedDatumFactory.createFloat(1));
        component.processInputs();
        assertEquals(10, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * 
     * Tests all input parameters in one run as simple test without any other Inputs.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testAllInputParametersOnlyParameterInputs() throws ComponentException {
        Map<String, String> metaData = generateParametricStudyMetadata(MINUS, true, MINUS, true, MINUS, true, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);
        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);
        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);

        component.start();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE, typedDatumFactory.createFloat(1.0));
        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE, typedDatumFactory.createFloat(10));
        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE, typedDatumFactory.createFloat(1));
        component.processInputs();
        assertEquals(10, context.getCapturedOutput(DESIGN_VARIABLE).size());
        final Double[] expectedValues = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        assertEquals(true, assertListsEqual(context.getCapturedOutput(DESIGN_VARIABLE), expectedValues));

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * 
     * Test Input Parameter From Value with "waiting for evaluation result".
     * 
     * @throws ComponentException on unexpected errors.
     */
    @Test
    public void testInputParameterWithEvaluationResult() throws ComponentException {
        Map<String, String> metaData = generateParametricStudyMetadata(MINUS, true, TWO, false, ONE, false, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);
        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);

        component.start();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE, typedDatumFactory.createFloat(1));

        component.processInputs();

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));

        component.processInputs();
        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);

        component.processInputs(); // processes last evaluation result

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * 
     * Test one of the parameter inputs in combination with a forwarding endpoint.
     * 
     * @throws ComponentException on unexpected error.
     */
    @Test
    public void testInputParameterWithForwardEndpoint() throws ComponentException {
        Map<String, String> metaData = generateParametricStudyMetadata(MINUS, true, TWO, false, ONE, false, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);
        context.addSimulatedInput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true, null);
        context.addSimulatedInput(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, DataType.Float, true, null, EndpointCharacter.OUTER_LOOP);
        context.addSimulatedOutput(N, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true, null);

        component.start();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(0, context.getCapturedOutput(N).size());

        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE, typedDatumFactory.createFloat(1.0));
        context.setInputValue(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createFloat(3.0));

        component.processInputs();

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(3.0, ((FloatTD) context.getCapturedOutput(N).get(0)).getFloatValue(), 0);

        context.setInputValue(N, typedDatumFactory.createFloat(4.0));

        component.processInputs();

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(4.0, ((FloatTD) context.getCapturedOutput(N).get(0)).getFloatValue(), 0);

        context.setInputValue(N, typedDatumFactory.createFloat(5.0));

        component.processInputs();

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(0, context.getCapturedOutput(N).size());

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * 
     * Test if input parameter originates from a driving component (i. e. nested loop)
     * 
     * @throws ComponentException on unexpected error.
     */
    @Test
    public void testInputParameterInNestedLoop() throws ComponentException {
        context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(true));
        Map<String, String> metaData = generateParametricStudyMetadata(MINUS, true, TWO, false, ONE, false, false);
        addSimulatedOutputs(metaData);

        context.addSimulatedInput(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE,
            ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS,
            DataType.Float, true, metaData);
        context.addSimulatedInput(RETURN_VALUE, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, DataType.Float, true, null);

        component.start();

        // context.setInputValue(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE, typedDatumFactory.createBoolean(false));

        // first run
        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE, typedDatumFactory.createFloat(1.0));
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        checkDoneOutputs(false);

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
        component.processInputs();
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
        component.processInputs();
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        component.reset();
        checkDoneOutputs(true);

        // second run
        context.setInputValue(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE, typedDatumFactory.createFloat(2.0));

        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        component.processInputs();

        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2.0, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);

        context.setInputValue(RETURN_VALUE, typedDatumFactory.createFloat(5.0));
        component.processInputs();
        component.reset();

        checkDoneOutputs(true);

        component.tearDownAndDispose(FinalComponentState.FINISHED);
    }

    /**
     * Test if the components cancels sending new design variables in case the component run (start) was canceled.
     * 
     * @throws ComponentException on unexpected error.
     */
    @Test(timeout = FIVE_HUNDRED_INT)
    public void testCancelStart() throws ComponentException {
        Map<String, String> metadata = generateParametricStudyMetadata(ONE, false, "100000", false, ONE, false, false);
        addSimulatedOutputs(metadata);
        ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {
            
            @Override
            public void run() {
                try {
                    final int hundred = 100;
                    Thread.sleep(hundred);
                } catch (InterruptedException e) {
                    return; // test timeout will apply as onStartInterrupted is not called
                }
                component.onStartInterrupted(null);    
            }
        });
        component.start();

        assertTrue(context.getCapturedOutput(DESIGN_VARIABLE).size() < HUNDRED_THOUSAND_INT);
        
        checkDoneOutputs(true);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    private void testOneLoopRun() throws ComponentException {
        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());

        context.setInputValue(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createInteger(5));
        component.processInputs();
        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        assertEquals(0, context.getCapturedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE).size());

        context.setInputValue(N, typedDatumFactory.createInteger(7));
        component.processInputs();
        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(2, ((FloatTD) context.getCapturedOutput(DESIGN_VARIABLE).get(0)).getFloatValue(), 0);
        assertEquals(0, context.getCapturedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE).size());

        context.setInputValue(N, typedDatumFactory.createInteger(9));
        component.processInputs();
        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(0, context.getCapturedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE).size());

        assertEquals(2, context.getCapturedOutputResets().size());
        assertTrue(context.getCapturedOutputResets().contains(DESIGN_VARIABLE));
        assertTrue(context.getCapturedOutputResets().contains(N));
        component.reset();
        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(DESIGN_VARIABLE).size());
        assertEquals(1, context.getCapturedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE).size());
    }

    private void addSimulatedOutputs(Map<String, String> metadata) {
        context.addSimulatedOutput(DESIGN_VARIABLE, "", DataType.Float, false, metadata);
        context.addSimulatedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, "", DataType.Boolean, false, null,
            EndpointCharacter.OUTER_LOOP);
    }

    private void checkDoneOutputs(boolean done) {
        checkDoneOutputs(done, false, 0);
    }

    private void checkDoneOutputs(boolean done, boolean outputsClosed, int dynOutputCount) {
        int countLoopDone;
        if (done) {
            countLoopDone = 1;
        } else {
            countLoopDone = 0;
        }
        assertEquals(countLoopDone, context.getCapturedOutput(DONE).size());
        if (done) {
            assertTrue(((BooleanTD) context.getCapturedOutput(DONE).get(0)).getBooleanValue());
            if (outputsClosed) {
                assertEquals(2 + dynOutputCount, context.getCapturedOutputClosings().size());
            }
        }
    }

    /**
     * Helper method that checks lists for equality.
     * 
     * @param listToCheck List of typed datums to be checked.
     * @param values Values to be compared with the list.
     * @return Whether the values are the same or not.
     */
    private boolean assertListsEqual(List<TypedDatum> listToCheck, Double... values) {
        List<Double> valuesToCheck = Arrays.asList(values);
        if (valuesToCheck.size() != listToCheck.size()) {
            return false;
        } else {
            for (int i = 0; i < valuesToCheck.size(); i++) {
                if (valuesToCheck.get(i) != ((FloatTD) listToCheck.get(i)).getFloatValue()) {
                    return false;
                }
            }
            return true;
        }
    }

}
