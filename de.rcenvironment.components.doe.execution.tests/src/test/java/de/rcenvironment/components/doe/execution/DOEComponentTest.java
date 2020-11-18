/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.execution;

import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test class for the DOE execution.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class DOEComponentTest {

    private static final String NUMBER_OF_OUTPUTS_FOR_CHOSEN_METHOD_TOO_FEW = "Number of outputs for chosen method too few";

    private static final String MINIMAL_CUSTOM_TABLE = "[[\"1\"],[\"2\"]]";

    private static final String FIVE = "5";

    private static final String I = "i";

    private static final String TWO = "2";

    private static final String ZERO = "0";

    private static final String TEN = "10";

    private static final String MINUS_TEN = "-10";

    private static final String ONE = "1";

    private static final String MINUS_1 = "-1";

    private static final String Y = "y";

    private static final String X = "x";

    private static final long CANCEL_TEST_TIMEOUT_MSEC = 1500;

    private static final int STATIC_OUTPUTS_COUNT = 2;

    private static final double DELTA = 0.000001;

    /**
     * Exception rule.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ComponentTestWrapper component;

    private ComponentContextMock context;

    /**
     * JUnit setup method.
     * 
     * @throws Exception e
     */
    @Before
    public void setUp() throws Exception {
        context = new ComponentContextMock();
        component = new ComponentTestWrapper(new DOEComponent(), context);
        final TypedDatumService typedDatumServiceMock = context.getService(TypedDatumService.class);
        TempFileServiceAccess.setupUnitTestEnvironment();
        // create stub
        ComponentDataManagementService componentDataManagementServiceMock = EasyMock.createMock(ComponentDataManagementService.class);
        // define stub behavior
        FileReferenceTD dummyFileReference = typedDatumServiceMock.getFactory().createFileReference("", "");
        EasyMock.expect(componentDataManagementServiceMock.createFileReferenceTDFromLocalFile(anyObject(ComponentContext.class),
            anyObject(File.class), anyObject(String.class))).andReturn(dummyFileReference);
        EasyMock.replay(componentDataManagementServiceMock);
        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);
    }

    /**
     * JUnit tear down method.
     * 
     * @throws Exception e
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Test if the DOE can handle input(s).
     * 
     * @throws ComponentException :
     */
    @Test
    public void testWithOneInput() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addStaticOutputs();

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, DOEConstants.INPUT_ID_NAME, DataType.Float, true, null);
        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1));

        component.start();
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());

        final double[] expectedValuesX = { -1, 1, -1, 1 };
        final double[] expectedValuesY = { -10, -10, 10, 10 };
        checkOutput(new double[] { expectedValuesX[0] }, X);
        checkOutput(new double[] { expectedValuesY[0] }, Y);
        for (int i = 1; i < 4; i++) {
            component.processInputs();
            context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1));
            Assert.assertEquals(1, context.getCapturedOutput(X).size());
            Assert.assertEquals(1, context.getCapturedOutput(Y).size());
            checkOutput(new double[] { expectedValuesX[i] }, X);
            checkOutput(new double[] { expectedValuesY[i] }, Y);
            checkLoopDoneSent(false);
        }

        component.processInputs();
        checkLoopDoneSent(true);
        checkClosedOutputs(2);

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test if the DOE can handle invalid input(s) with behavior rerun.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testWithOneInvalidInputRerun() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addStaticOutputs();
        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, DOEConstants.INPUT_ID_NAME, DataType.Float, true, null);

        final double[] expectedValuesX = { -1, -1, 1, -1, 1 };
        final double[] expectedValuesY = { -10, -10, -10, 10, 10 };
        component.start();
        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createNotAValue());
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());
        checkOutput(new double[] { expectedValuesX[0] }, X);
        checkOutput(new double[] { expectedValuesY[0] }, Y);
        for (int i = 1; i < 5; i++) {
            component.processInputs();
            context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1));
            Assert.assertEquals(1, context.getCapturedOutput(X).size());
            Assert.assertEquals(1, context.getCapturedOutput(Y).size());
            checkOutput(new double[] { expectedValuesX[i] }, X);
            checkOutput(new double[] { expectedValuesY[i] }, Y);
            checkLoopDoneSent(false);
        }

        component.processInputs();
        checkLoopDoneSent(true);
        checkClosedOutputs(2);
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test if the DOE can handle invalid input(s) with behavior abort.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testWithOneInvalidInputAbort() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.Fail, null);

        addStaticOutputs();
        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, DOEConstants.INPUT_ID_NAME, DataType.Float, true, null);

        component.start();
        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createNotAValue());
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());
        try {
            component.processInputs();
            fail();
        } catch (ComponentException e) {
            assertTrue(true);
        }
        checkLoopDoneSent(false);
        checkNoOutputsClosed();
    }

    /**
     * Test if the DOE can handle invalid input(s) with behavior rerun.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testWithOneInvalidInputSkip() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.Discard, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, DOEConstants.INPUT_ID_NAME, DataType.Float, true, null);
        addStaticOutputs();

        component.start();
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());
        final double[] expectedValuesX = { -1, 1, -1, 1 };
        checkOutput(new double[] { expectedValuesX[0] }, X);
        final double[] expectedValuesY = { -10, -10, 10, 10 };
        checkOutput(new double[] { expectedValuesY[0] }, Y);
        for (int i = 1; i < 4; i++) {
            context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createNotAValue());
            component.processInputs();
            Assert.assertEquals(1, context.getCapturedOutput(X).size(), DELTA);
            Assert.assertEquals(1, context.getCapturedOutput(Y).size());
            checkOutput(new double[] { expectedValuesX[i] }, X);
            checkOutput(new double[] { expectedValuesY[i] }, Y);
        }

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the full factorial algorithm for DOE with no inputs.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorialNoOutputs() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        exception.expect(ComponentException.class);
        exception.expectMessage(NUMBER_OF_OUTPUTS_FOR_CHOSEN_METHOD_TOO_FEW);
        component.start();
    }

    /**
     * Test the full factorial algorithm for DOE with one inputs.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorialOneOutput() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        exception.expect(ComponentException.class);
        exception.expectMessage(NUMBER_OF_OUTPUTS_FOR_CHOSEN_METHOD_TOO_FEW);
        addNewOutput(X, MINUS_1, ONE);
        component.start();
    }

    /**
     * Test the full factorial algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorialLevelTooLow() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, ONE, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        exception.expect(ComponentException.class);
        exception.expectMessage("Level number for full factorial design too low");

        component.start();
    }

    /**
     * Test the full factorial algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorial() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);
        addStaticOutputs();
        component.start();
        Assert.assertEquals(4, context.getCapturedOutput(X).size());
        Assert.assertEquals(4, context.getCapturedOutput(Y).size());

        final double[] expectedValuesX = { -1, 1, -1, 1 };
        checkOutput(expectedValuesX, X);
        final double[] expectedValuesY = { -10, -10, 10, 10 };
        checkOutput(expectedValuesY, Y);

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the full factorial algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorialWithHistoryData() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        context.setConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM, String.valueOf(true));

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);
        addStaticOutputs();
        component.start();
        Assert.assertEquals(4, context.getCapturedOutput(X).size());
        Assert.assertEquals(4, context.getCapturedOutput(Y).size());

        final double[] expectedValuesX = { -1, 1, -1, 1 };
        checkOutput(expectedValuesX, X);
        final double[] expectedValuesY = { -10, -10, 10, 10 };
        checkOutput(expectedValuesY, Y);

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * 
     * Test the LHC algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmLHCNoOutputs() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        exception.expect(ComponentException.class);
        exception.expectMessage(NUMBER_OF_OUTPUTS_FOR_CHOSEN_METHOD_TOO_FEW);
        component.start();
    }

    /**
     * 
     * Test the LHC algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmLHCOneOutput() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        exception.expect(ComponentException.class);
        exception.expectMessage(NUMBER_OF_OUTPUTS_FOR_CHOSEN_METHOD_TOO_FEW);
        component.start();
    }

    /**
     * Test the LHC algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmLHCThreeRuns() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ZERO, "3", ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        addStaticOutputs();
        component.start();
        Assert.assertEquals(3, context.getCapturedOutput(X).size());
        Assert.assertEquals(3, context.getCapturedOutput(Y).size());
        final double[] expectedValuesX = new double[] { 0.03362467007842257, -0.6016364814685322, 0.5554789329844331 };
        checkOutput(expectedValuesX, X);
        final double[] expectedValuesY = new double[] { 2.941661196547428, 5.166359773569898, -9.14068566084149 };
        checkOutput(expectedValuesY, Y);
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the LHC algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmLHCFiveRuns() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ONE, FIVE, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        addStaticOutputs();
        component.start();
        Assert.assertEquals(5, context.getCapturedOutput(X).size());
        Assert.assertEquals(5, context.getCapturedOutput(Y).size());

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * 
     * Test the Monte Carlo algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmMonteCarloNoOutput() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_MONTE_CARLO, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        addStaticOutputs();
        component.start();
    }

    /**
     * Test the Monte Carlo algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmMonteCarloTwoRuns() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_MONTE_CARLO, ZERO, "3", ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        addStaticOutputs();
        component.start();
        Assert.assertEquals(3, context.getCapturedOutput(X).size());
        Assert.assertEquals(3, context.getCapturedOutput(Y).size());
        final double[] expectedValuesX = new double[] { 0.46193557475331404, 0.2748348507002165, 0.19509055559440358 };
        checkOutput(expectedValuesX, X);
        final double[] expectedValuesY = new double[] { -5.189271686570283, 1.0087401023526787, -3.3356320104670045 };
        checkOutput(expectedValuesY, Y);
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the Monte Carlo algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmMonteCarloFiveRuns() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_MONTE_CARLO, ONE, FIVE, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);
        addStaticOutputs();
        component.start();
        Assert.assertEquals(5, context.getCapturedOutput(X).size());
        Assert.assertEquals(5, context.getCapturedOutput(Y).size());

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the custom table from input for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableInputNoDefaultInput() throws ComponentException {

        addStaticOutputs();
        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_1, ONE);

        context.addSimulatedInput(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME, DOEConstants.CUSTOM_TABLE_ENDPOINT_ID, DataType.Matrix, true,
            new HashMap<String, String>());
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT, ONE, FIVE, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, "");
        // exception.expect(ComponentException.class);
        // exception.expectMessage("No table");
        component.start();
        Assert.assertEquals(0, context.getCapturedOutput(X).size());
        TypedDatumFactory factory = context.getService(TypedDatumService.class).getFactory();
        FloatTD[][] values =
            { { factory.createFloat(1.0), factory.createFloat(1.0) }, { factory.createFloat(2.0), factory.createFloat(2.0) } };
        context.setInputValue(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME,
            factory.createMatrix(values));
        component.processInputs();
        Assert.assertEquals(2, context.getCapturedOutput(X).size());
    }

    /**
     * Test the custom table from input with another input for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableInputWithDefaultInput() throws ComponentException {

        addStaticOutputs();
        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_1, ONE);

        context.addSimulatedInput(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME, DOEConstants.CUSTOM_TABLE_ENDPOINT_ID, DataType.Matrix, true,
            new HashMap<String, String>());
        context.addSimulatedInput(I, DOEConstants.INPUT_ID_NAME, DataType.Float, true, new HashMap<String, String>());
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT, ONE, FIVE, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, "");
        // exception.expect(ComponentException.class);
        // exception.expectMessage("No table");
        component.start();
        Assert.assertEquals(0, context.getCapturedOutput(X).size());
        TypedDatumFactory factory = context.getService(TypedDatumService.class).getFactory();
        FloatTD[][] values =
            { { factory.createFloat(1.0), factory.createFloat(1.0) }, { factory.createFloat(2.0), factory.createFloat(2.0) } };
        context.setInputValue(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME, factory.createMatrix(values));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        context.setInputValue(I, factory.createFloat(1.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(X).size());

    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableNoTable() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, "");
        exception.expect(ComponentException.class);
        exception.expectMessage("No table");
        component.start();

    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableValidTable() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);
        addStaticOutputs();
        final String table = "[[\"1\", \"1\"],[\"2\", \"1\"]]";
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        component.start();

        Assert.assertEquals(2, context.getCapturedOutput(X).size());
        Assert.assertEquals(2, context.getCapturedOutput(Y).size());
        final double[] expectedValuesX = new double[] { 1, 2 };
        checkOutput(expectedValuesX, X);
        final double[] expectedValuesY = new double[] { 1, 1 };
        checkOutput(expectedValuesY, Y);
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableTooFewOutputs() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addStaticOutputs();
        final String table = "[[\"1\", \"1\"],[\"2\", \"1\"]]";
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        component.start();

        Assert.assertEquals(2, context.getCapturedOutput(X).size());
        Assert.assertEquals(0, context.getCapturedOutput(Y).size());
        final double[] expectedValuesX = new double[] { 1, 2 };
        checkOutput(expectedValuesX, X);
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();
    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableTooFewValues() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_1, ONE);

        final String table = MINIMAL_CUSTOM_TABLE;
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        exception.expect(ComponentException.class);
        exception.expectMessage(StringUtils.format("Number of values per sample (%s) is lower than the number of outputs", 1, 2));
        component.start();
    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableEndGreaterStart() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_1, ONE);

        final String table = MINIMAL_CUSTOM_TABLE;
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ONE, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        exception.expect(ComponentException.class);
        component.start();
    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableStartLessZero() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);
        addStaticOutputs();
        final String table = MINIMAL_CUSTOM_TABLE;
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, MINUS_1, TWO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        component.start();
        Assert.assertEquals(2, context.getCapturedOutput(X).size());

    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableMoreRunsThanValues() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);

        final String table = MINIMAL_CUSTOM_TABLE;
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, FIVE, FIVE,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        exception.expect(ComponentException.class);
        exception.expectMessage(StringUtils.format("Start sample value (%s) is greater than the number of samples (%s)", 5, 2));
        component.start();

    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableInvalidEntry() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);

        final String table = "[[\"1\"],[\"null\"]]";
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, table);
        exception.expect(ComponentException.class);
        component.start();
    }

    /**
     * Tests if values are forwarded as expected.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testForwardingValue() throws ComponentException {

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addStaticOutputs();

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true, null);
        context.addSimulatedOutput(I, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true, null);

        component.start();
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());
        Assert.assertEquals(0, context.getCapturedOutput(I).size());

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(I).size());
        Assert.assertEquals(1.0, ((FloatTD) context.getCapturedOutput(I).get(0)).getFloatValue(), 0);

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(7.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(I).size());
        Assert.assertEquals(7.0, ((FloatTD) context.getCapturedOutput(I).get(0)).getFloatValue(), 0);

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(5.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(I).size());
        Assert.assertEquals(5.0, ((FloatTD) context.getCapturedOutput(I).get(0)).getFloatValue(), 0);

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(5.0));
        component.processInputs();
        Assert.assertEquals(0, context.getCapturedOutput(I).size());
        checkLoopDoneSent(true);
        checkClosedOutputs(3);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Tests if values are forwarded as expected.
     * 
     * @throws ComponentException on unexpected errors
     */
    @Test
    public void testForwardingValueWithStartValues() throws ComponentException {

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addStaticOutputs();

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true, null);
        context.addSimulatedInput(I + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, DataType.Float, true, null);
        context.addSimulatedOutput(I, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, DataType.Float, true, null);

        component.start();
        Assert.assertEquals(0, context.getCapturedOutput(X).size());
        Assert.assertEquals(0, context.getCapturedOutput(Y).size());
        Assert.assertEquals(0, context.getCapturedOutput(I).size());

        context.setInputValue(I + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
            context.getService(TypedDatumService.class).getFactory().createFloat(1.0));
        component.processInputs();

        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());
        Assert.assertEquals(1, context.getCapturedOutput(I).size());

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(I).size());
        Assert.assertEquals(1.0, ((FloatTD) context.getCapturedOutput(I).get(0)).getFloatValue(), 0);

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(7.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(I).size());
        Assert.assertEquals(7.0, ((FloatTD) context.getCapturedOutput(I).get(0)).getFloatValue(), 0);

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(5.0));
        component.processInputs();
        Assert.assertEquals(1, context.getCapturedOutput(I).size());
        Assert.assertEquals(5.0, ((FloatTD) context.getCapturedOutput(I).get(0)).getFloatValue(), 0);

        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(5.0));
        component.processInputs();
        Assert.assertEquals(0, context.getCapturedOutput(I).size());
        checkLoopDoneSent(true);
        checkClosedOutputs(3);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);
    }

    /**
     * Test if the components cancels sending new design variables in case the component run (start) was canceled.
     * 
     * @throws ComponentException on unexpected error.
     * @throws InterruptedException on test interruption
     */
    @Test(timeout = CANCEL_TEST_TIMEOUT_MSEC)
    public void testCancelStart() throws ComponentException, InterruptedException {

        final int axisCount = 250;
        final int maximumOutputCount = axisCount * axisCount; // how many outputs the component will generate unless cancelled

        final int waitBeforeCancellation = 150;
        final int waitBeforeTesting = 250;

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, Integer.toString(axisCount), ZERO, ZERO,
            LoopComponentConstants.LoopBehaviorInCaseOfFailure.RerunAndFail, null);

        addStaticOutputs();

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        ConcurrencyUtils.getAsyncTaskService().execute("Cancel the tested component after a short wait", new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(waitBeforeCancellation);
                } catch (InterruptedException e) {
                    return; // test timeout will apply as onStartInterrupted is not called
                }
                component.onStartInterrupted(null);
            }
        });
        component.start();
        Thread.sleep(waitBeforeTesting);

        int generatedXOutputs = context.getCapturedOutput(X).size();
        int generatedYOutputs = context.getCapturedOutput(Y).size();

        final String errorText1 = "The component did not produce any output at all";
        assertTrue(errorText1, generatedXOutputs > 0);
        assertTrue(errorText1, generatedYOutputs > 0);
        final String errorText2 = "The component actually produced the configured maximum number of outputs - "
            + "either the simulated cancellation was too slow, or cancellation is actually broken";
        assertTrue(errorText2, generatedXOutputs < maximumOutputCount);
        assertTrue(errorText2, generatedYOutputs < maximumOutputCount);
        assertTrue("Number of outputs after cancellation is not symmetrical", generatedXOutputs == generatedYOutputs);

        checkLoopDoneSent(true);

        component.tearDownAndDispose(Component.FinalComponentState.FINISHED);

    }

    private void addStaticOutputs() {
        context.addSimulatedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE, "", DataType.Boolean, false,
            new HashMap<String, String>());
        context.addSimulatedOutput(DOEConstants.OUTPUT_NAME_NUMBER_OF_SAMPLES, "", DataType.Integer, false, new HashMap<String, String>(),
            EndpointCharacter.SAME_LOOP);
    }

    private void addNewOutput(String name, String lower, String upper) {
        Map<String, String> metaDatumX = new HashMap<>();
        metaDatumX.put("lower", lower);
        metaDatumX.put("upper", upper);
        context.addSimulatedOutput(name, "", DataType.Float, true, metaDatumX);
    }

    private void checkOutput(double[] expectedValuesX, String outputName) {
        int i = 0;
        for (TypedDatum output : context.getCapturedOutput(outputName)) {
            Assert.assertEquals(expectedValuesX[i++], ((FloatTD) output).getFloatValue(), DELTA);
        }
        Assert.assertEquals(expectedValuesX.length, i);
    }

    private void setDOEConfiguration(String method, String seed, String runNumber, String startSample, String endSample,
        LoopComponentConstants.LoopBehaviorInCaseOfFailure behaviour, String table) {
        context.setConfigurationValue(DOEConstants.KEY_METHOD, method);
        context.setConfigurationValue(DOEConstants.KEY_SEED_NUMBER, seed);
        context.setConfigurationValue(DOEConstants.KEY_RUN_NUMBER, runNumber);
        context.setConfigurationValue(DOEConstants.KEY_START_SAMPLE, startSample);
        context.setConfigurationValue(DOEConstants.KEY_END_SAMPLE, endSample);
        context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_NAV, behaviour.name());
        context.setConfigurationValue(LoopComponentConstants.CONFIG_KEY_MAX_RERUN_BEFORE_FAIL_NAV, "1");
        context.setConfigurationValue(DOEConstants.KEY_TABLE, table);
    }

    private void checkLoopDoneSent(boolean done) {
        if (done) {
            Assert.assertEquals(1, context.getCapturedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE).size());
            Assert.assertEquals(true, ((BooleanTD) context
                .getCapturedOutput(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE).get(0)).getBooleanValue());
        }
    }

    private void checkClosedOutputs(int dynInputCount) {
        assertEquals(STATIC_OUTPUTS_COUNT + dynInputCount, context.getCapturedOutputClosings().size());
    }

    private void checkNoOutputsClosed() {
        assertEquals(0, context.getCapturedOutputClosings().size());
    }

}
