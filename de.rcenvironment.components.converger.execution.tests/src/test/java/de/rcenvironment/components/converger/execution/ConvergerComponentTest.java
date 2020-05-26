/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.converger.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.execution.api.Component;
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

/**
 * Integration test for {@link ConvergerComponent}.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class ConvergerComponentTest {

    private static final String STRING_2 = "2";

    private static final String STRING_1 = "1";

    private static final String STRING_0 = "0";

    private static final String X = "x";

    private static final String Y = "y";

    private static final String Z = "z";

    private static final String N = "n";

    private ComponentTestWrapper component;

    private ConvergerComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    /**
     * Custom subclass of {@link ComponentContextMock} that adds common configuration and query methods.
     * 
     * @author Robert Mischke
     */
    private final class ConvergerComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = 1570441783510990090L;

        public void configure(ComponentContextMock ctx, String epsA, String epsR, String itsToConsider, String maxChecks) {
            configure(ctx, epsA, epsR, itsToConsider, maxChecks, DataType.Float);
        }

        public void configure(ComponentContextMock ctx, String epsA, String epsR, String itsToConsider, String maxChecks,
            DataType dynamicEndpointType) {
            context.setConfigurationValue(ConvergerComponentConstants.KEY_EPS_A, epsA);
            context.setConfigurationValue(ConvergerComponentConstants.KEY_EPS_R, epsR);
            context.setConfigurationValue(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, itsToConsider);
            context.setConfigurationValue(ConvergerComponentConstants.KEY_MAX_CONV_CHECKS, maxChecks);

            addSimulatedOutput(ConvergerComponentConstants.CONVERGED, null, DataType.Boolean, false,
                null, EndpointCharacter.OUTER_LOOP);
            addSimulatedOutput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, dynamicEndpointType, true, null);
            addSimulatedOutput(X + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
                ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY, DataType.Boolean, true, null);
            addSimulatedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE,
                dynamicEndpointType, true, null, EndpointCharacter.OUTER_LOOP);
            context.addSimulatedOutput(ConvergerComponentConstants.CONVERGED, "", DataType.Boolean, true, null,
                EndpointCharacter.OUTER_LOOP);
            context.addSimulatedOutput(ConvergerComponentConstants.CONVERGED_ABSOLUTE, "", DataType.Float, true, null,
                EndpointCharacter.OUTER_LOOP);
            context.addSimulatedOutput(ConvergerComponentConstants.CONVERGED_RELATIVE, "", DataType.Float, true, null,
                EndpointCharacter.OUTER_LOOP);
            addSimulatedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, null, DataType.Boolean, false, null,
                EndpointCharacter.OUTER_LOOP);
        }

        public void configure(ComponentContextMock ctx, String epsA, String epsR, String itsToConsider) {
            configure(ctx, epsA, epsR, itsToConsider, null);
        }

        public void configure(ComponentContextMock ctx, String epsA, String epsR, String itsToConsider, boolean isNestedLoop) {
            configure(ctx, epsA, epsR, itsToConsider, null);
            context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(isNestedLoop));
        }

        public void configure(ComponentContextMock ctx, String epsA, String epsR, String itsToConsider, String maxChecks,
            boolean isNestedLoop) {
            configure(ctx, epsA, epsR, itsToConsider, maxChecks);
            context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, String.valueOf(isNestedLoop));
        }

        public void addSimulatedInput(String name, String endpointId, DataType dataType, boolean isDynamic, double startValue) {
            Map<String, String> metaData = new HashMap<>();
            metaData.put(ConvergerComponentConstants.META_HAS_STARTVALUE, String.valueOf(true));
            metaData.put(ConvergerComponentConstants.META_STARTVALUE, String.valueOf(startValue));
            super.addSimulatedInput(name, endpointId, dataType, isDynamic, metaData);
        }

        public void addSimulatedInput(String name, String endpointId, DataType dataType, boolean isDynamic) {
            super.addSimulatedInput(name, endpointId, dataType, isDynamic, null);
        }

        // TODO without the "assert" method (which cannot be in non-test bundles), this could be
        // moved to the base class
        /**
         * If a single output {@link TypedDatum} was generated on the given output, it is returned; if another number of outputs was
         * generated, an assertion failure is thrown.
         * 
         * @param name the name of the output
         * @return the single {@link TypedDatum}, if present
         */
        public TypedDatum expectSingleOutputDatum(String name) {
            List<TypedDatum> outputDataConverged;
            outputDataConverged = context.getCapturedOutput(name);
            assertEquals(1, outputDataConverged.size());
            TypedDatum typedDatum = outputDataConverged.get(0);
            return typedDatum;
        }

        public void testForSingleBooleanOutput(String name, boolean value) {
            TypedDatum typedDatum = expectSingleOutputDatum(name);
            assertEquals(value, ((BooleanTD) typedDatum).getBooleanValue());
        }

        public void testForSingleValueOutput(String name, double value) {
            TypedDatum typedDatum = expectSingleOutputDatum(name);
            assertEquals(value, ((FloatTD) typedDatum).getFloatValue(), 0.0);
        }

        public void testForNoOutputValueSent(String name) {
            assertTrue(context.getCapturedOutput(name).isEmpty());
        }

        public void testForClosedOutput(String name) {
            assertTrue(getCapturedOutputClosings().contains(name));
        }

        public void testForResetOutputs() {
            assertTrue(isResetOutputsCalled());
        }
    }

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        context = new ConvergerComponentContextMock();
        component = new ComponentTestWrapper(new ConvergerComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();
    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {
        // TODO adapt if other end states are needed
        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Tests basic life-cycle with no attached inputs.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testNoInputs() throws ComponentException {
        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();
    }
    
    /**
     * Tests that a forwarded value can not trigger convergence.
     * 
     * @throws ComponentException on unexpected error
     */
    @Test
    public void testForwardedInputNotTriggerConvergence() throws ComponentException {
        context.addSimulatedInput(X, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true);
        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();
        context.setInputValue(X, typedDatumFactory.createFloat(5.0));
        component.processInputs();
        context.testForSingleValueOutput(X, 5.0);
        context.testForNoOutputValueSent(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);
        context.testForNoOutputValueSent(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE);
    }

    /**
     * Tests basic behavior for values received on a single input.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testSingleInput() throws ComponentException {
        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectNoneOutputIsClosed();
        expectProcessInputsWithoutConverging(X, X, 2.0);
        expectNoneOutputIsClosed();
        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectNoneOutputIsClosed();

        expectProcessInputsWithConverging(X, X, 1.0);

        expectAllOutputsAreClosed(X);
    }

    /**
     * Tests basic behavior for values received on multiple inputs with data type float.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testConvergenceForMultipleInputsWithFloat() throws ComponentException {
        addSimulatedEndpointsToConverge(X, DataType.Float);
        addSimulatedEndpointsToConverge(Y, DataType.Float);
        addSimulatedEndpointsToConverge(Z, DataType.Float);
        addSimulatedEndpointsToForward(N, DataType.Integer);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(Y, typedDatumFactory.createFloat(1.0));
        context.setInputValue(Z, typedDatumFactory.createFloat(1.0));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createFloat(2.0));
        context.setInputValue(Y, typedDatumFactory.createFloat(3.0));
        context.setInputValue(Z, typedDatumFactory.createFloat(4.0));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(Y, typedDatumFactory.createFloat(6.0));
        context.setInputValue(Z, typedDatumFactory.createFloat(8.0));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(Y, typedDatumFactory.createFloat(8.0));
        context.setInputValue(Z, typedDatumFactory.createFloat(9.0));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(Y, typedDatumFactory.createFloat(8.0));
        context.setInputValue(Z, typedDatumFactory.createFloat(7.0));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(Y, typedDatumFactory.createFloat(8.0));
        context.setInputValue(Z, typedDatumFactory.createFloat(7.0));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, true);
        context.testForClosedOutput(ConvergerComponentConstants.CONVERGED);
        context.testForClosedOutput(ConvergerComponentConstants.CONVERGED);
    }

    /**
     * Tests basic behavior for values received on multiple inputs with data type integer.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testConvergenceForMultipleInputsWithInteger() throws ComponentException {
        addSimulatedEndpointsToConverge(X, DataType.Integer);
        addSimulatedEndpointsToConverge(Y, DataType.Integer);
        addSimulatedEndpointsToConverge(Z, DataType.Integer);
        addSimulatedEndpointsToForward(N, DataType.Integer);

        context.configure(context, STRING_0, STRING_0, STRING_1, null, DataType.Integer);
        component.start();

        context.setInputValue(X, typedDatumFactory.createInteger(1L));
        context.setInputValue(Y, typedDatumFactory.createInteger(1L));
        context.setInputValue(Z, typedDatumFactory.createInteger(1L));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createInteger(2L));
        context.setInputValue(Y, typedDatumFactory.createInteger(3L));
        context.setInputValue(Z, typedDatumFactory.createInteger(4L));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createInteger(1L));
        context.setInputValue(Y, typedDatumFactory.createInteger(6L));
        context.setInputValue(Z, typedDatumFactory.createInteger(8L));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createInteger(1L));
        context.setInputValue(Y, typedDatumFactory.createInteger(8L));
        context.setInputValue(Z, typedDatumFactory.createInteger(9L));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createInteger(1L));
        context.setInputValue(Y, typedDatumFactory.createInteger(8L));
        context.setInputValue(Z, typedDatumFactory.createInteger(7L));
        component.processInputs();
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);

        context.setInputValue(X, typedDatumFactory.createInteger(1L));
        context.setInputValue(Y, typedDatumFactory.createInteger(8L));
        context.setInputValue(Z, typedDatumFactory.createInteger(7L));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, true);
        context.testForClosedOutput(ConvergerComponentConstants.CONVERGED);
        context.testForClosedOutput(ConvergerComponentConstants.CONVERGED);
    }

    /**
     * Tests if 'maximum convergence check' constraint is considered properly.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void tesMaximumConvergenceCheckConstraint() throws ComponentException {
        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);

        context.configure(context, STRING_0, STRING_0, STRING_1, STRING_2);
        component.start();

        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectProcessInputsWithoutConverging(X, X, 2.0);

        expectProcessInputsWithoutConvergingButWithDone(X, X, 1.0);

        expectAllOutputsAreClosed(X);
    }

    /**
     * Tests if 'iterations to consider' constraint is considered properly.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testIterationToConsiderConstraint() throws ComponentException {
        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);

        context.configure(context, STRING_0, STRING_0, STRING_2);
        component.start();

        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectProcessInputsWithoutConverging(X, X, 2.0);
        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectProcessInputsWithoutConverging(X, X, 1.0);

        expectProcessInputsWithConverging(X, X, 1.0);

        expectAllOutputsAreClosed(X);
    }

    /**
     * Tests if 'maximum convergence check' constraint is considered properly if 'iterations to consider' constraint is applied as well.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testMaximumConvergenceCheckAndIterationToConsiderConstraint() throws ComponentException {
        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);

        context.configure(context, STRING_0, STRING_0, STRING_2, STRING_2);
        component.start();

        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectProcessInputsWithoutConverging(X, X, 2.0);
        expectProcessInputsWithoutConverging(X, X, 3.0);

        expectProcessInputsWithoutConvergingButWithDone(X, X, 4.0);

        expectAllOutputsAreClosed(X);
    }

    /**
     * Tests reset of the converger component if converged.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testResetOnConverged() throws ComponentException {
        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);
        context.addSimulatedInput(X + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            "startValues", DataType.Float, true, null, EndpointCharacter.OUTER_LOOP);

        context.configure(context, STRING_0, STRING_0, STRING_1, true);
        component.start();

        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectProcessInputsWithoutConverging(X, X, 2.0);

        expectProcessInputsWithConvergingOrDoneOnReset(X, X, 2.0);

        context.testForResetOutputs();

        component.reset();

        expectFinalValuesSentAfterResetIfConverged(X, X, 2.0);

    }

    /**
     * Tests reset of the converger component if maximum convergence check is reached.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testResetOnMaxConvChecksReached() throws ComponentException {
        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);
        context.addSimulatedInput(X + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, "startValues", DataType.Float, true, null,
            EndpointCharacter.OUTER_LOOP);

        context.configure(context, STRING_0, STRING_0, STRING_1, STRING_1, true);
        component.start();

        expectProcessInputsWithoutConverging(X, X, 1.0);
        expectProcessInputsWithConvergingOrDoneOnReset(X, X, 2.0);

        context.testForResetOutputs();

        component.reset();

        expectFinalValuesSentAfterResetIfMaxConvChecksReached(X, X, 2.0);
    }

    /**
     * Tests reset of the converger component if maximum convergence check is reached.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testForwardingValue() throws ComponentException {
        addSimulatedEndpointsToConverge(X, DataType.Float);
        addSimulatedEndpointsToForward(N, DataType.Integer);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(N, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(7, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue());

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(N, typedDatumFactory.createInteger(9));
        component.processInputs();

        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(9,
            ((IntegerTD) context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).get(0)).getIntValue());
    }

    /**
     * Tests reset of the converger component if maximum convergence check is reached.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testForwardingWithStartValueInputs() throws ComponentException {
        addSimulatedEndpointsToConverge(X, DataType.Float);
        context.addSimulatedInput(X + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true);
        addSimulatedEndpointsToForward(N, DataType.Integer);
        context.addSimulatedInput(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        context.setInputValue(X + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createFloat(1.0));
        context.setInputValue(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(X).size());
        assertEquals(7, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue());
        assertEquals(1.0, ((FloatTD) context.getCapturedOutput(X).get(0)).getFloatValue(), 0);
        assertEquals(0, context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());

        context.setInputValue(X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(N, typedDatumFactory.createInteger(9));
        component.processInputs();

        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(0, context.getCapturedOutput(X).size());
        assertEquals(1, context.getCapturedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(9,
            ((IntegerTD) context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).get(0)).getIntValue());
        assertEquals(1.0,
            ((FloatTD) context.getCapturedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).get(0)).getFloatValue(), 0);
    }

    /**
     * Tests reset of the converger component if maximum convergence check is reached.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testWithStartValueInputsAndAsMetaData() throws ComponentException {

        context.addSimulatedInput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true, 5.0);
        context.addSimulatedOutput(X, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true, null);
        context.addSimulatedOutput(X + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
            ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY, DataType.Boolean, true, null);
        context.addSimulatedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
            ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true, null, EndpointCharacter.OUTER_LOOP);

        addSimulatedEndpointsToForward(N, DataType.Integer);
        context.addSimulatedInput(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Integer, true);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        assertEquals(0, context.getCapturedOutput(X).size());

        context.setInputValue(N + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, typedDatumFactory.createInteger(7));
        component.processInputs();

        assertEquals(1, context.getCapturedOutput(N).size());
        assertEquals(0, context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(7, ((IntegerTD) context.getCapturedOutput(N).get(0)).getIntValue());

        assertEquals(1, context.getCapturedOutput(X).size());
        assertEquals(0, context.getCapturedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(5.0, ((FloatTD) context.getCapturedOutput(X).get(0)).getFloatValue(), 0);

        context.setInputValue(X, typedDatumFactory.createFloat(5.0));
        context.setInputValue(N, typedDatumFactory.createInteger(9));
        component.processInputs();

        assertEquals(0, context.getCapturedOutput(N).size());
        assertEquals(1, context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(9,
            ((IntegerTD) context.getCapturedOutput(N + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).get(0)).getIntValue());

        assertEquals(0, context.getCapturedOutput(X).size());
        assertEquals(1, context.getCapturedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).size());
        assertEquals(5.0,
            ((FloatTD) context.getCapturedOutput(X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX).get(0)).getFloatValue(), 0);
    }

    private void expectProcessInputsWithoutConverging(String valueInput, String valueOutput, double value) throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForSingleValueOutput(valueOutput, value);
        context.testForSingleBooleanOutput(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX, false);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);
        context.testForNoOutputValueSent(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE);
    }

    private void expectProcessInputsWithConverging(String valueInput, String valueOutput, double value) throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForNoOutputValueSent(valueOutput);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX);
        context.testForSingleValueOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, value);
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, true);
        context.testForSingleBooleanOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, true);
    }

    private void expectProcessInputsWithConvergingOrDoneOnReset(String valueInput, String valueOutput, double value)
        throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForNoOutputValueSent(valueOutput);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
        context.testForNoOutputValueSent(ConvergerComponentConstants.CONVERGED);
        context.testForNoOutputValueSent(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE);
    }

    private void expectFinalValuesSentAfterResetIfConverged(String valueInput, String valueOutput, double value) throws ComponentException {
        context.testForNoOutputValueSent(valueOutput);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX);
        context.testForSingleValueOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, value);
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, true);
        context.testForSingleBooleanOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, true);
    }

    private void expectFinalValuesSentAfterResetIfMaxConvChecksReached(String valueInput, String valueOutput, double value)
        throws ComponentException {
        context.testForNoOutputValueSent(valueOutput);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX);
        context.testForSingleValueOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, value);
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);
        context.testForSingleBooleanOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, true);
    }

    private void expectProcessInputsWithoutConvergingButWithDone(String valueInput, String valueOutput, double value)
        throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForNoOutputValueSent(valueOutput);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX);
        context.testForSingleValueOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, value);
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);
        context.testForSingleBooleanOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, true);
    }

    private void expectNoneOutputIsClosed() {
        assertEquals(0, context.getCapturedOutputClosings().size());
    }

    private void expectAllOutputsAreClosed(String valueOutput) {
        context.testForClosedOutput(valueOutput);
        context.testForClosedOutput(valueOutput + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX);
        context.testForClosedOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
        context.testForClosedOutput(ConvergerComponentConstants.CONVERGED);
        context.testForClosedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE);
    }

    private void addSimulatedEndpointsToConverge(String name, DataType dataType) {
        context.addSimulatedInput(name, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, dataType, true);
        context.addSimulatedOutput(name, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, dataType, true, null);
        context.addSimulatedOutput(name + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
            ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY, DataType.Boolean, true, null);
        context.addSimulatedOutput(name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
            ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, DataType.Float, true, null, EndpointCharacter.OUTER_LOOP);
    }

    private void addSimulatedEndpointsToForward(String name, DataType dataType) {
        context.addSimulatedInput(name, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, dataType, true);
        context.addSimulatedOutput(name, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, dataType, true, null);
        context.addSimulatedOutput(name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, dataType, true, null, EndpointCharacter.OUTER_LOOP);
    }

}
