/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution;

import static org.easymock.EasyMock.anyObject;

import java.io.File;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.testutils.ScriptingServiceStubFactory;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * 
 * Integration test for {@link SwitchComponent}.
 * 
 * @author David Scholz
 * @author Doreen Seider
 * @author Alexander Weinert
 */
public class SwitchComponentTest {

    private static final String FLOAT_2_0 = "2.0";

    private static final String SPACE = " ";

    private static final String NOT = "not";

    private static final String FALSE = "False";

    private static final String FLOAT_1_0 = "1.0";

    private static final String EQUALS = "==";

    private static final String OR = " or ";

    private static final String TRUE = "True";

    private static final String FLOAT_11_1 = "11.1";

    private static final String RETURN_VALUE = "returnValue";

    private static final String SHIFT = " << 3";

    private static final String AND = " and ";

    private static final String INPUT_X = "x";

    private static final String INPUT_Y = "y";

    private static final String SYNTAX_ERROR = "Syntax error:";

    private static final String NOT_DEFINED = "not defined";

    private static final String NOT_SUPPORTED_IN_SCRIPT = "not supported in script";

    /**
     * Expected exception if script/validation fails.
     */
    @Rule
    public ExpectedException scriptException = ExpectedException.none();

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
        component = new ComponentTestWrapper(new SwitchComponent(), context);
        TempFileServiceAccess.setupUnitTestEnvironment();
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();

        ComponentDataManagementService componentDataManagementServiceMock = EasyMock.createMock(ComponentDataManagementService.class);
        FileReferenceTD dummyFileReference = typedDatumFactory.createFileReference("", "");

        EasyMock.expect(componentDataManagementServiceMock.createFileReferenceTDFromLocalFile(anyObject(ComponentContext.class),
            anyObject(File.class), anyObject(String.class))).andReturn(dummyFileReference);
        EasyMock.replay(componentDataManagementServiceMock);

        context.addService(ComponentDataManagementService.class, componentDataManagementServiceMock);
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
     * Tests behavior if no script is defined.
     * 
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testNoScriptInput() throws ComponentException {
        final String condition = "";

        final ScriptEngine engine = createUnusedScriptEngineMock();
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.addSimulatedInput(SwitchComponentConstants.DATA_INPUT_NAME, SwitchComponentConstants.DATA_INPUT_NAME, DataType.Float,
            false,
            null);
        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createFloat(3.0));
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);

        scriptException.expect(ComponentException.class);
        scriptException.expectMessage("No condition is defined");

        component.start();

        EasyMock.verify(scriptingService, engine);
    }

    /**
     *
     * Test behavior if condition is true. (with historyDataItem)
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testTrueScript() throws ComponentException {
        addSimpleInputAndOutput();

        final String condition = INPUT_X + " < " + INPUT_Y;
        final String sanityCondition = createEvaluationScript("11.1 < 11.1");
        final String actualCondition = createEvaluationScript("1.0 < 2.0");

        final ScriptEngine engine = createEvaluatingScriptEngineMock(sanityCondition, actualCondition, Boolean.TRUE);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        context.setConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM, Boolean.toString(true));
        component.start();
        component.processInputs();

        EasyMock.verify(scriptingService, engine);

        Assert.assertEquals(1, context.getCapturedOutput(SwitchComponentConstants.TRUE_OUTPUT).size());
        Assert.assertEquals(0, context.getCapturedOutput(SwitchComponentConstants.FALSE_OUTPUT).size());
    }

    /**
     *
     * Test behavior if condition is false. (with historyDataItem)
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testFalseScript() throws ComponentException {
        addSimpleInputAndOutput();

        final String condition = INPUT_X + " > " + INPUT_Y;
        final String sanityCondition = createEvaluationScript("11.1 > 11.1");
        final String actualCondition = createEvaluationScript("1.0 > 2.0");

        final ScriptEngine engine = createEvaluatingScriptEngineMock(sanityCondition, actualCondition, Boolean.FALSE);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        context.setConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM, Boolean.toString(true));
        component.start();
        component.processInputs();

        EasyMock.verify(scriptingService, engine);

        Assert.assertEquals(0, context.getCapturedOutput(SwitchComponentConstants.TRUE_OUTPUT).size());
        Assert.assertEquals(1, context.getCapturedOutput(SwitchComponentConstants.FALSE_OUTPUT).size());
    }

    /**
     *
     * Test if syntax errors in script are recognized.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testSyntaxErrorInScript() throws ComponentException {
        addSimpleInputAndOutput();

        final String condition = "(" + INPUT_Y + ">" + INPUT_X;
        final String sanityCondition = createEvaluationScript("(11.1>11.1");

        final ScriptEngine engine = createThrowingScriptEngineMock(sanityCondition, SYNTAX_ERROR);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        scriptException.expect(ComponentException.class);
        scriptException.expectMessage(SYNTAX_ERROR);
        component.start();

        EasyMock.verify(engine, scriptingService);
    }

    /**
     * Test if invalid input names are recognized.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testUseOfInvalidInputNames() throws ComponentException {
        addSimpleInputAndOutput();

        final String condition = INPUT_Y + ">EvilName";
        final String sanityCondition = createEvaluationScript("11.1>EvilName");

        final ScriptEngine engine = createThrowingScriptEngineMock(sanityCondition, NOT_DEFINED);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        scriptException.expect(ComponentException.class);
        scriptException.expectMessage(NOT_DEFINED);
        component.start();

        EasyMock.verify(engine, scriptingService);
    }

    /**
     *
     * Test behavior if invalid data types are used.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testUseOfInvalidDataTypesInScript() throws ComponentException {
        context.addSimulatedInput(SwitchComponentConstants.DATA_INPUT_NAME, SwitchComponentConstants.DATA_INPUT_NAME,
            DataType.Matrix, false, null);
        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createMatrix(2, 2));

        final String condition = SwitchComponentConstants.DATA_INPUT_NAME + " < 3";
        final String sanityCondition = createEvaluationScript("To_forward < 3");

        final ScriptEngine engine = createThrowingScriptEngineMock(sanityCondition, NOT_SUPPORTED_IN_SCRIPT);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        scriptException.expect(ComponentException.class);
        scriptException.expectMessage(NOT_SUPPORTED_IN_SCRIPT);
        component.start();

        EasyMock.verify(engine, scriptingService);
    }

    /**
     * Test operators.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testAllOperators() throws ComponentException {
        addSimpleInputAndOutput();
        StringBuilder conditionBuilder = new StringBuilder();
        StringBuilder sanityConditionBuilder = new StringBuilder();
        StringBuilder actualConditionBuilder = new StringBuilder();

        for (String operator : SwitchComponentConstants.OPERATORS) {
            conditionBuilder.append(INPUT_X);
            sanityConditionBuilder.append(FLOAT_11_1);
            actualConditionBuilder.append(FLOAT_1_0);
            if (operator.equals(NOT)) {
                conditionBuilder.append(AND + operator + SPACE);
                sanityConditionBuilder.append(AND + operator + SPACE);
                actualConditionBuilder.append(AND + operator + SPACE);
            } else if (operator.equals(FALSE) || operator.equals(TRUE)) {
                conditionBuilder.append(EQUALS + operator + OR);
                sanityConditionBuilder.append(EQUALS + operator + OR);
                actualConditionBuilder.append(EQUALS + operator + OR);
            } else {
                conditionBuilder.append(SPACE + operator + SPACE);
                sanityConditionBuilder.append(SPACE + operator + SPACE);
                actualConditionBuilder.append(SPACE + operator + SPACE);
            }
            conditionBuilder.append(INPUT_Y);
            sanityConditionBuilder.append(FLOAT_11_1);
            actualConditionBuilder.append(FLOAT_2_0);
            conditionBuilder.append(AND);
            sanityConditionBuilder.append(AND);
            actualConditionBuilder.append(AND);
        }
        conditionBuilder.append(INPUT_Y);
        sanityConditionBuilder.append(FLOAT_11_1);
        actualConditionBuilder.append(FLOAT_2_0);
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, conditionBuilder.toString());

        final String sanityConditionWithValues = createEvaluationScript(sanityConditionBuilder.toString());
        final String actualConditionWithValues = createEvaluationScript(actualConditionBuilder.toString());

        final ScriptEngine engine =
            createEvaluatingScriptEngineMock(sanityConditionWithValues, actualConditionWithValues, Boolean.FALSE);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        component.start();
        component.processInputs();

        EasyMock.verify(engine, scriptingService);
    }

    /**
     *
     * Test valid data types in script.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testValidDataTypes() throws ComponentException {
        context.addSimulatedInput(INPUT_X, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Float, true, null);
        context.addSimulatedInput(INPUT_Y, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Integer, true, null);
        context.addSimulatedInput(SwitchComponentConstants.DATA_INPUT_NAME, SwitchComponentConstants.DATA_INPUT_NAME,
            DataType.Boolean, false, null);
        context.addSimulatedOutput(SwitchComponentConstants.TRUE_OUTPUT, SwitchComponentConstants.TRUE_OUTPUT, DataType.Boolean, false,
            null);
        context.addSimulatedOutput(SwitchComponentConstants.FALSE_OUTPUT, SwitchComponentConstants.FALSE_OUTPUT, DataType.Boolean, false,
            null);
        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createInteger(2));
        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createBoolean(true));

        final String condition = INPUT_X + " < " + INPUT_Y + OR + SwitchComponentConstants.DATA_INPUT_NAME;
        final String sanityCondition = createEvaluationScript("11.1 < 11 or True");
        final String actualCondition = createEvaluationScript("1.0 < 2 or True");

        final ScriptEngine engine =
            createEvaluatingScriptEngineMock(sanityCondition, actualCondition, Boolean.FALSE);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);


        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);

        component.start();
        component.processInputs();

        EasyMock.verify(engine, scriptingService);
    }

    /**
     *
     * Test behavior if script is null.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testNullSwitch() throws ComponentException {
        context.addSimulatedInput(SwitchComponentConstants.DATA_INPUT_NAME, SwitchComponentConstants.DATA_INPUT_NAME, DataType.Float,
            false,
            null);
        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createFloat(3.0));
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, null);

        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(null);
        context.addService(ScriptingService.class, scriptingService);

        scriptException.expect(ComponentException.class);
        scriptException.expectMessage("No condition is defined");
        component.start();

        EasyMock.verify(scriptingService);
    }

    /**
     *
     * Test behavior if script contains "<<" or ">>".
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testScriptWithShiftAndFloatInputs() throws ComponentException {

        final String condition = INPUT_X + SHIFT;
        final String sanityCondition = createEvaluationScript("11.1 << 3");

        final ScriptEngine engine = createThrowingScriptEngineMock(sanityCondition, SYNTAX_ERROR);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.addSimulatedInput(INPUT_X, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Float, true, null);
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        scriptException.expect(ComponentException.class);
        scriptException.expectMessage(SYNTAX_ERROR);
        component.start();

        EasyMock.verify(scriptingService, engine);
    }

    /**
     *
     * Test behavior if script contains "<<" or ">>".
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testScriptWithShiftAndIntegerInputs() throws ComponentException {
        final String condition = INPUT_X + SHIFT;
        final String sanityCondition = createEvaluationScript("11 << 3");

        final ScriptEngine engine = createSanityCheckScriptEngineMock(sanityCondition);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.addSimulatedInput(INPUT_X, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Integer, true, null);
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        component.start();

        EasyMock.verify(scriptingService, engine);
    }

    /**
     *
     * Test behavior if script contains "<<" or ">>".
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testScriptWithShiftAndBooleanInputs() throws ComponentException {
        final String condition = INPUT_X + SHIFT;
        final String sanityCondition = createEvaluationScript("True << 3");

        final ScriptEngine engine = createSanityCheckScriptEngineMock(sanityCondition);
        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.addSimulatedInput(INPUT_X, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Boolean, true, null);
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        component.start();

        EasyMock.verify(scriptingService, engine);
    }

    /**
     *
     * Tests if outputs are closed properly based on the configuration set.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testOutputsNotClosed() throws ComponentException {
        testClosingOutputs(SwitchComponentConstants.NEVER_CLOSE_OUTPUTS_KEY, true, false, false, false);
    }

    /**
     *
     * Tests if outputs are closed properly based on the configuration set.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testClosingOutputsOnTrue() throws ComponentException {
        testClosingOutputs(SwitchComponentConstants.CLOSE_OUTPUTS_ON_TRUE_KEY, false, false, true, true);
    }

    /**
     *
     * Tests if outputs are closed properly based on the configuration set.
     *
     * @throws ComponentException on unexpected component failures.
     */
    @Test
    public void testClosingOutputsOnFalse() throws ComponentException {
        testClosingOutputs(SwitchComponentConstants.CLOSE_OUTPUTS_ON_FALSE_KEY, true, false, false, true);
    }

    private void testClosingOutputs(String config, boolean firstValue, boolean outputsClosedAfterFirstRun,
        boolean secondValue, boolean outputsClosedAfterSecondRun) throws ComponentException {

        final String condition = SwitchComponentConstants.DATA_INPUT_NAME;
        final String sanityCondition = createEvaluationScript(TRUE);
        final String actualConditionTrue = createEvaluationScript(TRUE);
        final String actualConditionFalse = createEvaluationScript(FALSE);

        final ScriptEngine engine = EasyMock.createStrictMock(ScriptEngine.class);

        try {
            EasyMock.expect(engine.eval(sanityCondition)).andReturn(null);
            if (firstValue) {
                EasyMock.expect(engine.eval(actualConditionTrue)).andReturn(null);
                EasyMock.expect(engine.get(RETURN_VALUE)).andReturn(Boolean.TRUE);
            } else {
                EasyMock.expect(engine.eval(actualConditionFalse)).andReturn(null);
                EasyMock.expect(engine.get(RETURN_VALUE)).andReturn(Boolean.FALSE);
            }
            if (secondValue) {
                EasyMock.expect(engine.eval(actualConditionTrue)).andReturn(null);
                EasyMock.expect(engine.get(RETURN_VALUE)).andReturn(Boolean.TRUE);
            } else {
                EasyMock.expect(engine.eval(actualConditionFalse)).andReturn(null);
                EasyMock.expect(engine.get(RETURN_VALUE)).andReturn(Boolean.FALSE);
            }
        } catch (ScriptException e) {
            // This will never happen, as we are calling eval(String) on a mocked ScriptEngine instead of the actual implementation
        }
        EasyMock.replay(engine);


        final ScriptingService scriptingService = ScriptingServiceStubFactory.createDefaultMock(engine);
        context.addService(ScriptingService.class, scriptingService);

        context.addSimulatedInput(SwitchComponentConstants.DATA_INPUT_NAME, null,
            DataType.Boolean, false, null);
        context.addSimulatedOutput(SwitchComponentConstants.TRUE_OUTPUT, null, DataType.Boolean, false, null);
        context.addSimulatedOutput(SwitchComponentConstants.FALSE_OUTPUT, null, DataType.Boolean, false, null);
        context.setConfigurationValue(SwitchComponentConstants.CONDITION_KEY, condition);
        context.setConfigurationValue(config, "true");
        component.start();

        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createBoolean(firstValue));
        component.processInputs();

        if (outputsClosedAfterFirstRun) {
            Assert.assertEquals(2, context.getCapturedOutputClosings().size());
        } else {
            Assert.assertEquals(0, context.getCapturedOutputClosings().size());
        }

        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createBoolean(secondValue));
        component.processInputs();

        if (outputsClosedAfterSecondRun) {
            Assert.assertEquals(2, context.getCapturedOutputClosings().size());
        } else {
            Assert.assertEquals(0, context.getCapturedOutputClosings().size());
        }
    }

    private void addSimpleInputAndOutput() {
        context.addSimulatedInput(INPUT_X, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Float, true, null);
        context.addSimulatedInput(INPUT_Y, SwitchComponentConstants.CONDITION_INPUT_ID, DataType.Float, true, null);
        context.addSimulatedInput(SwitchComponentConstants.DATA_INPUT_NAME, SwitchComponentConstants.DATA_INPUT_NAME, DataType.Float,
            false,
            null);
        context.addSimulatedOutput(SwitchComponentConstants.TRUE_OUTPUT, SwitchComponentConstants.TRUE_OUTPUT, DataType.Float, false, null);
        context.addSimulatedOutput(SwitchComponentConstants.FALSE_OUTPUT, SwitchComponentConstants.FALSE_OUTPUT, DataType.Float, false,
            null);
        context.setInputValue(INPUT_X, typedDatumFactory.createFloat(1.0));
        context.setInputValue(INPUT_Y, typedDatumFactory.createFloat(2.0));
        context.setInputValue(SwitchComponentConstants.DATA_INPUT_NAME, typedDatumFactory.createFloat(3.0));
    }

    private String createEvaluationScript(String conditionWithValues) {
        return "if " + conditionWithValues + ":\n    returnValue=True\nelse:\n    returnValue=False";
    }

    private ScriptEngine createUnusedScriptEngineMock() {
        final ScriptEngine engine = EasyMock.createStrictMock(ScriptEngine.class);
        EasyMock.replay(engine);
        return engine;
    }

    /**
     * @param sanityScript The script used by the switch component to check for basic syntax errors in the condition
     * @return A mocked script engine that only expects .eval(sanityScript)
     */
    private ScriptEngine createSanityCheckScriptEngineMock(String sanityScript) {
        final ScriptEngine engine = EasyMock.createStrictMock(ScriptEngine.class);

        try {
            EasyMock.expect(engine.eval(sanityScript)).andReturn(null);
        } catch (ScriptException e) {
            // This will never happen, as we are calling eval(String) on a mocked ScriptEngine instead of the actual implementation
            return null;
        }

        EasyMock.replay(engine);
        return engine;
    }

    /**
     * @param sanityScript The script used by the switch component to check for basic syntax errors in the condition
     * @param actualScript The actual script used to check the condition
     * @param returnValue The value that shall be returned upon querying 'returnValue' after the execution of the condition
     * @return A mocked script engine that first expects .eval(sanityScript), then .eval(actualScript), and finally .get("returnValue")
     */
    private ScriptEngine createEvaluatingScriptEngineMock(String sanityScript, String actualScript, Object returnValue) {
        final ScriptEngine engine = EasyMock.createStrictMock(ScriptEngine.class);

        try {
            EasyMock.expect(engine.eval(sanityScript)).andReturn(null);
            EasyMock.expect(engine.eval(actualScript)).andReturn(null);
        } catch (ScriptException e) {
            // This will never happen, as we are calling eval(String) on a mocked ScriptEngine instead of the actual implementation
            return null;
        }

        EasyMock.expect(engine.get(RETURN_VALUE)).andReturn(returnValue);

        EasyMock.replay(engine);
        return engine;
    }

    /**
     * @param script The script used by the switch component to check for basic syntax errors in the condition
     * @param message The message that shall be contained in the thrown
     * @return A mocked script engine that expects .eval(script) and throws a ScriptException with the given message in response
     */
    private ScriptEngine createThrowingScriptEngineMock(String script, String message) {
        final ScriptEngine engine = EasyMock.createStrictMock(ScriptEngine.class);

        try {
            EasyMock.expect(engine.eval(script)).andThrow(new ScriptException(message));
        } catch (ScriptException e) {
            // This will never happen, as we are calling eval(String) on a mocked ScriptEngine instead of the actual implementation
            return null;
        }

        EasyMock.replay(engine);
        return engine;
    }

}
