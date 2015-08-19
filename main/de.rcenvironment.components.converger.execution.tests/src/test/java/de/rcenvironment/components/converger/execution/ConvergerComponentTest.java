/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;

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

    private static final String INPUT_X = "x";
    
    private static final String OUTPUT_X = "x";
    
    private static final String INPUT_Y = "y";
    
    private static final String INPUT_Z = "z";
    
    private static final String OUTPUT_CONVERGED = ConvergerComponentConstants.CONVERGED;

    private static final String OUTPUT_OUTER_LOOP_DONE = ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE;

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
            context.setConfigurationValue(ConvergerComponentConstants.KEY_EPS_A, epsA);
            context.setConfigurationValue(ConvergerComponentConstants.KEY_EPS_R, epsR);
            context.setConfigurationValue(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, itsToConsider);
            context.setConfigurationValue(ConvergerComponentConstants.KEY_MAX_CONV_CHECKS, maxChecks);
            
            addSimulatedOutput(OUTPUT_CONVERGED, null, DataType.Boolean, false, null);
            addSimulatedOutput(OUTPUT_X, null, DataType.Boolean, true, null);
            addSimulatedOutput(OUTPUT_X + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, null, DataType.Boolean, true, null);
            addSimulatedOutput(OUTPUT_OUTER_LOOP_DONE, null, DataType.Boolean, false, null);
        }
        
        public void configure(ComponentContextMock ctx, String epsA, String epsR, String itsToConsider) {
            configure(ctx, epsA, epsR, itsToConsider, null);
        }

        // TODO without the "assert" method (which cannot be in non-test bundles), this could be moved to the base class
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
     * Tests basic behavior for values received on a single input.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testSingleInput() throws ComponentException {
        context.addSimulatedInput(INPUT_X, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 2.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        
        expectProcessInputsWithConverging(INPUT_X, OUTPUT_X, 1.0);
        
        expectAllOutputsAreClosed(OUTPUT_X);
    }
    
    /**
     * Tests basic behavior for values received on multiple inputs.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testConvergenceForMultipleInputs() throws ComponentException {
        context.addSimulatedInput(INPUT_X, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);
        context.addSimulatedInput(INPUT_Y, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);
        context.addSimulatedInput(INPUT_Z, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);

        context.configure(context, STRING_0, STRING_0, STRING_1);
        component.start();

        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Z, typedDatumFactory.createFloat(1.0));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);

        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(2.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(3.0));
        context.setInputValue(INPUT_Z, typedDatumFactory.createFloat(4.0));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);

        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(6.0));
        context.setInputValue(INPUT_Z, typedDatumFactory.createFloat(8.0));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);

        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(8.0));
        context.setInputValue(INPUT_Z, typedDatumFactory.createFloat(9.0));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);
        
        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(8.0));
        context.setInputValue(INPUT_Z, typedDatumFactory.createFloat(7.0));
        component.processInputs();
        context.testForSingleBooleanOutput(ConvergerComponentConstants.CONVERGED, false);
        
        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(8.0));
        context.setInputValue(INPUT_Z, typedDatumFactory.createFloat(7.0));
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
        context.addSimulatedInput(INPUT_X, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);

        context.configure(context, STRING_0, STRING_0, STRING_1, STRING_2);
        component.start();

        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 2.0);
        
        expectProcessInputsWithoutConvergingButWithDone(INPUT_X, OUTPUT_X, 1.0);
        
        expectAllOutputsAreClosed(OUTPUT_X);
    }
    
    /**
     * Tests if 'iterations to consider' constraint is considered properly.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testIterationToConsiderConstraint() throws ComponentException {
        context.addSimulatedInput(INPUT_X, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);

        context.configure(context, STRING_0, STRING_0, STRING_2);
        component.start();

        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 2.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        
        expectProcessInputsWithConverging(INPUT_X, OUTPUT_X, 1.0);
        
        expectAllOutputsAreClosed(OUTPUT_X);
    }
    
    /**
     * Tests if 'maximum convergence check' constraint is considered properly if 'iterations to consider' constraint is applied as well.
     * 
     * @throws ComponentException on unexpected component failures
     */
    @Test
    public void testMaximumConvergenceCheckAndIterationToConsiderConstraint() throws ComponentException {
        context.addSimulatedInput(INPUT_X, ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, DataType.Float, true, null);

        context.configure(context, STRING_0, STRING_0, STRING_2, STRING_2);
        component.start();

        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 1.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 2.0);
        expectProcessInputsWithoutConverging(INPUT_X, OUTPUT_X, 3.0);
        
        expectProcessInputsWithoutConvergingButWithDone(INPUT_X, OUTPUT_X, 4.0);
        
        expectAllOutputsAreClosed(OUTPUT_X);
    }
    
    private void expectProcessInputsWithoutConverging(String valueInput, String valueOutput, double value) throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForSingleValueOutput(valueOutput, value);
        context.testForNoOutputValueSent(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
        context.testForSingleBooleanOutput(OUTPUT_CONVERGED, false);
        context.testForNoOutputValueSent(OUTPUT_OUTER_LOOP_DONE);
    }
    
    private void expectProcessInputsWithConverging(String valueInput, String valueOutput, double value) throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForNoOutputValueSent(valueOutput);
        context.testForSingleValueOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, value);
        context.testForSingleBooleanOutput(OUTPUT_CONVERGED, true);
        context.testForSingleBooleanOutput(OUTPUT_OUTER_LOOP_DONE, true);
    }
    
    private void expectProcessInputsWithoutConvergingButWithDone(String valueInput, String valueOutput, double value)
        throws ComponentException {
        context.setInputValue(valueInput, typedDatumFactory.createFloat(value));
        component.processInputs();
        context.testForNoOutputValueSent(valueOutput);
        context.testForSingleValueOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, value);
        context.testForSingleBooleanOutput(OUTPUT_CONVERGED, false);
        context.testForSingleBooleanOutput(OUTPUT_OUTER_LOOP_DONE, true);
    }
    
    private void expectAllOutputsAreClosed(String valueOutput) {
        context.testForClosedOutput(valueOutput);
        context.testForClosedOutput(valueOutput + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX);
        context.testForClosedOutput(OUTPUT_CONVERGED);
        context.testForClosedOutput(OUTPUT_OUTER_LOOP_DONE);
    }
    
    // TODO test: reset; start values configured; some start values configured and some aren't

}
