/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.execution;

import static org.easymock.EasyMock.anyObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test class for the DOE execution.
 * 
 * @author Sascha Zur
 */
public class DOEComponentTest {

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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, "", DataType.Float, true, null);
        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1));

        component.start();
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());

        final double[] expectedValuesX = { -1, 1, -1, 1 };
        final double[] expectedValuesY = { -10, -10, 10, 10 };
        checkOutput(new double[] { expectedValuesX[0] }, X);
        checkOutput(new double[] { expectedValuesY[0] }, Y);
        for (int i = 1; i < 3; i++) {
            component.processInputs();
            context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createFloat(1));
            Assert.assertEquals(1, context.getCapturedOutput(X).size());
            Assert.assertEquals(1, context.getCapturedOutput(Y).size());
            checkOutput(new double[] { expectedValuesX[i] }, X);
            checkOutput(new double[] { expectedValuesY[i] }, Y);
        }

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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, "", DataType.Float, true, null);

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
        }

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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_ABORT, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, "", DataType.Float, true, null);

        component.start();
        context.setInputValue(I, context.getService(TypedDatumService.class).getFactory().createNotAValue());
        Assert.assertEquals(1, context.getCapturedOutput(X).size());
        Assert.assertEquals(1, context.getCapturedOutput(Y).size());
        exception.expect(ComponentException.class);
        component.processInputs();
    }

    /**
     * Test if the DOE can handle invalid input(s) with behavior rerun.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testWithOneInvalidInputSkip() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_SKIP, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        context.addSimulatedInput(I, "", DataType.Float, true, null);

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
            Assert.assertEquals(1, context.getCapturedOutput(X).size());
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        exception.expect(ComponentException.class);
        exception.expectMessage(DOEComponent.TOO_FEW_OUTPUTS_EXCEPTION);
        component.start();
    }

    /**
     * Test the full factorial algorithm for DOE with one inputs.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorialOneOutput() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
        exception.expect(ComponentException.class);
        exception.expectMessage(DOEComponent.TOO_FEW_OUTPUTS_EXCEPTION);
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, ONE, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

        exception.expect(ComponentException.class);
        exception.expectMessage(DOEComponent.LEVEL_TOO_LOW_EXCEPTION);

        component.start();
    }

    /**
     * Test the full factorial algorithm for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testAlgorithmFullFactorial() throws ComponentException {
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_FULLFACT, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
        context.setConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM, Boolean.toString(true));

        addNewOutput(X, MINUS_1, ONE);
        addNewOutput(Y, MINUS_TEN, TEN);

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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        exception.expect(ComponentException.class);
        exception.expectMessage(DOEComponent.TOO_FEW_OUTPUTS_EXCEPTION);
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);

        exception.expect(ComponentException.class);
        exception.expectMessage(DOEComponent.TOO_FEW_OUTPUTS_EXCEPTION);
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

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ZERO, "3", ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
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

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_LHC, ONE, FIVE, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_MONTE_CARLO, ZERO, TWO, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
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

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_MONTE_CARLO, ZERO, "3", ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
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

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_MONTE_CARLO, ONE, FIVE, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, null);
        component.start();
        Assert.assertEquals(5, context.getCapturedOutput(X).size());
        Assert.assertEquals(5, context.getCapturedOutput(Y).size());

        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();

    }

    /**
     * Test the custom table for DOE.
     * 
     * @throws ComponentException :
     */
    @Test
    public void testCustomTableNoTable() throws ComponentException {

        addNewOutput(X, MINUS_1, ONE);

        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, "");
        exception.expect(ComponentException.class);
        exception.expectMessage(DOEComponent.TABLE_IS_NULL_OR_EMPTY);
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

        final String table = "[[\"1\", \"1\"],[\"2\", \"1\"]]";
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
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

        final String table = "[[\"1\", \"1\"],[\"2\", \"1\"]]";
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
        exception.expect(ComponentException.class);
        exception.expectMessage(String.format(DOEComponent.NUMBER_OF_VALUES_PER_SAMPLE_LOWER_THAN_THE_NUMBER_OF_OUTPUTS, 1, 2));
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ONE, ZERO, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
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

        final String table = MINIMAL_CUSTOM_TABLE;
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, MINUS_1, TWO, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, FIVE, FIVE, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
        exception.expect(ComponentException.class);
        exception.expectMessage(String.format(DOEComponent.START_SAMPLE_VALUE_HIGHER_THAN_THE_NUMBER_OF_SAMPLES, 5, 2));
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
        setDOEConfiguration(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE, ONE, FIVE, ZERO, ONE, DOEConstants.KEY_BEHAVIOUR_RERUN, table);
        exception.expect(ComponentException.class);
        component.start();
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
            Assert.assertEquals(expectedValuesX[i++], ((FloatTD) output).getFloatValue());
        }
        Assert.assertEquals(expectedValuesX.length, i);
    }

    private void setDOEConfiguration(String method, String seed, String runNumber, String startSample, String endSample,
        String failedRunBehaviour, String table) {
        context.setConfigurationValue(DOEConstants.KEY_METHOD, method);
        context.setConfigurationValue(DOEConstants.KEY_SEED_NUMBER, seed);
        context.setConfigurationValue(DOEConstants.KEY_RUN_NUMBER, runNumber);
        context.setConfigurationValue(DOEConstants.KEY_START_SAMPLE, startSample);
        context.setConfigurationValue(DOEConstants.KEY_END_SAMPLE, endSample);
        context.setConfigurationValue(DOEConstants.KEY_FAILED_RUN_BEHAVIOUR, failedRunBehaviour);
        context.setConfigurationValue(DOEConstants.KEY_TABLE, table);
    }
}
