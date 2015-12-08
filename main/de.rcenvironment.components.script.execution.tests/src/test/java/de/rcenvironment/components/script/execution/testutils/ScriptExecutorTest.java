/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.testutils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.scripting.WorkflowConsoleForwardingWriter;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.scripting.python.PythonOutputWriter;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;
import junit.framework.Assert;

/**
 * Abstract class for testing the implementations of {@link ScriptExecutor} since all should have the same results with the same
 * configurations.
 * 
 * @author Sascha Zur
 */
public abstract class ScriptExecutorTest {

    private static final String PRINT_TEST = "print test";

    private static Object[][] dataInputs = new Object[][] {
        { "float", DataType.Float, 1d },
        { "shorttext", DataType.ShortText, "testWert" },
        { "boolean", DataType.Boolean, true },
        { "integer", DataType.Integer, 1L },
        { "file", DataType.FileReference, "" },
        { "dir", DataType.DirectoryReference, "" },
        { "vec", DataType.Vector, null }

    };

    protected ComponentContextMock context;

    protected ScriptExecutor executor;

    /**
     * Test preparing the executor and a new run.
     * 
     * @throws ComponentException e
     * @throws ScriptException e
     */
    @Test
    public void testExecutorLifecycle() throws ComponentException, ScriptException {
        ScriptEngine scriptEngine = getScriptingEngine();
        WorkflowConsoleForwardingWriter stdOutWriter = new WorkflowConsoleForwardingWriter(new Object(), context.getLog(), Type.TOOL_OUT);
        WorkflowConsoleForwardingWriter stdErrWriter = new WorkflowConsoleForwardingWriter(new Object(), context.getLog(), Type.TOOL_ERROR);

        ScriptContext cont = EasyMock.createNiceMock(ScriptContext.class);
        EasyMock.expect(cont.getWriter()).andReturn(stdOutWriter).anyTimes();
        EasyMock.expect(cont.getErrorWriter()).andReturn(stdErrWriter).anyTimes();
        EasyMock.replay(cont);
        EasyMock.expect(scriptEngine.getContext()).andReturn(cont).anyTimes();
        List<Capture<String>> captures = new LinkedList<Capture<String>>();
        for (int i = 0; i < 5; i++) {
            Capture<String> evalCapture = new Capture<String>();
            EasyMock.expect(scriptEngine.eval(EasyMock.capture(evalCapture))).andReturn(0);
            captures.add(evalCapture);
        }
        EasyMock.replay(scriptEngine);
        executor.setScriptEngine(scriptEngine);

        ScriptingService scriptingService = EasyMock.createNiceMock(ScriptingService.class);
        EasyMock.expect(scriptingService.createScriptEngine(getScriptLanguage())).andReturn(scriptEngine).anyTimes();
        EasyMock.replay(scriptingService);
        context.addService(ScriptingService.class, scriptingService);

        TempFileServiceAccess.setupUnitTestEnvironment();
        testPrepareHook();

        executor.prepareExecutor(context);
        executor.prepareNewRun(getScriptLanguage(), PRINT_TEST, null);
        executor.setComponentContext(context);

        try {
            stdOutWriter.write(PythonOutputWriter.CONSOLE_END);
            stdErrWriter.write(PythonOutputWriter.CONSOLE_END);
            stdOutWriter.flush();
            stdErrWriter.flush();
        } catch (IOException e) {
            Logger.getGlobal().log(Level.ALL, e.getMessage());
        }
        executor.runScript();

        boolean hasTestScript = false;
        for (int i = 0; i < 5; i++) {
            if (captures.get(i).hasCaptured() && captures.get(i).getValue().contains(PRINT_TEST)) {
                hasTestScript = true;
            }
        }
        Assert.assertEquals(true, hasTestScript);
    }

    /**
     * Common tear down.
     */
    @After
    public void tearDown() {
        executor = null;
        context = null;
    }

    protected abstract ScriptEngine getScriptingEngine();

    protected abstract ScriptLanguage getScriptLanguage();

    protected abstract void testPrepareHook();

    /**
     * Test post run method.
     * 
     * @throws IOException .
     * 
     * @throws ComponentException .
     */
    @Test
    public void testPostRun() throws ComponentException, IOException {

        ScriptEngine scriptEngine = getScriptingEngine();

        addOutputsToEngine(scriptEngine);
        prepareScriptingUtilsAndContext();

        executor.setComponentContext(context);
        executor.setHistoryDataItem(new ScriptComponentHistoryDataItem());
        executor.setScriptEngine(scriptEngine);
        executor.setStateMap(new HashMap<String, Object>());
        executor.setWorkingPath("");
        executor.setStdoutWriter(EasyMock.createNiceMock(Writer.class));
        executor.setStderrWriter(EasyMock.createNiceMock(Writer.class));
        executor.postRun();

        for (Object[] dataInput : dataInputs) {
            Assert.assertEquals(context.getCapturedOutput((String) dataInput[0]).size(), 1);
        }
    }

    @SuppressWarnings("rawtypes")
    private void addOutputsToEngine(ScriptEngine scriptEngine) {
        Map<String, ArrayList<Object>> outputChannelMap = new HashMap<String, ArrayList<Object>>();

        List list = EasyMock.createNiceMock(List.class);
        Iterator it = EasyMock.createNiceMock(Iterator.class);
        EasyMock.expect(it.next()).andReturn(new Integer(1));
        EasyMock.expect(it);
        EasyMock.expect(list.iterator()).andReturn(it);
        EasyMock.replay(list);

        dataInputs[6][2] = list;
        for (Object[] dataInput : dataInputs) {
            context.addSimulatedOutput((String) dataInput[0], "default", (DataType) dataInput[1], true,
                new HashMap<String, String>());
            List<Object> outputValues = new ArrayList<Object>();
            outputValues.add(dataInput[2]);
            outputChannelMap.put((String) dataInput[0], (ArrayList<Object>) outputValues);
            List<Object> linkedList = new LinkedList<Object>();
            linkedList.add(dataInput[2]);
            EasyMock.expect(scriptEngine.get((String) dataInput[0])).andReturn(linkedList).anyTimes();
        }
        EasyMock.expect(scriptEngine.get("RCE_Dict_OutputChannels")).andReturn(outputChannelMap);
        EasyMock.expect(scriptEngine.get("RCE_STATE_VARIABLES")).andReturn(new HashMap<String, Object>());
        EasyMock.expect(scriptEngine.get("RCE_CloseOutputChannelsList")).andReturn(new LinkedList<String>());
        EasyMock.replay(scriptEngine);
    }

    private void prepareScriptingUtilsAndContext() throws IOException {
        ScriptingUtils su = new ScriptingUtils();
        TypedDatumService tds = EasyMock.createMock(TypedDatumService.class);
        TypedDatumFactory tdf = EasyMock.createMock(TypedDatumFactory.class);
        EasyMock.expect(tds.getFactory()).andReturn(tdf);
        EasyMock.replay(tds);
        addMocksToFactory(tdf);
        EasyMock.replay(tdf);
        su.bindTypedDatumService(tds);
        ComponentDataManagementService cdms = EasyMock.createNiceMock(ComponentDataManagementService.class);
        DirectoryReferenceTD directoryMock = EasyMock.createNiceMock(DirectoryReferenceTD.class);
        EasyMock.expect(directoryMock.getDataType()).andReturn(DataType.DirectoryReference).anyTimes();
        EasyMock.replay(directoryMock);
        EasyMock.expect(
            cdms.createDirectoryReferenceTDFromLocalDirectory(EasyMock.anyObject(ComponentContext.class), EasyMock.anyObject(File.class),
                EasyMock.anyObject(String.class)))
            .andReturn(directoryMock);
        FileReferenceTD fileMock = EasyMock.createNiceMock(FileReferenceTD.class);
        EasyMock.expect(fileMock.getDataType()).andReturn(DataType.FileReference).anyTimes();
        EasyMock.replay(fileMock);
        EasyMock.expect(
            cdms.createFileReferenceTDFromLocalFile(EasyMock.anyObject(ComponentContext.class), EasyMock.anyObject(File.class),
                EasyMock.anyObject(String.class)))
            .andReturn(fileMock);
        EasyMock.replay(cdms);
        context.addService(ComponentDataManagementService.class, cdms);
        su.bindComponentDataManagementService(cdms);
        ConfigurationService configs = EasyMock.createNiceMock(ConfigurationService.class);
        EasyMock.expect(configs.getParentTempDirectoryRoot()).andReturn(new File("")).anyTimes();
        EasyMock.replay(configs);
        context.addService(ConfigurationService.class, configs);

    }

    private void addMocksToFactory(TypedDatumFactory tdf) {
        BooleanTD booleanMock = EasyMock.createMock(BooleanTD.class);
        EasyMock.expect(booleanMock.getDataType()).andReturn(DataType.Boolean);
        EasyMock.replay(booleanMock);
        EasyMock.expect(tdf.createBoolean(EasyMock.anyBoolean())).andReturn(booleanMock);

        FloatTD floatMock = EasyMock.createMock(FloatTD.class);
        EasyMock.expect(floatMock.getDataType()).andReturn(DataType.Float);
        EasyMock.replay(floatMock);
        EasyMock.expect(tdf.createFloat(EasyMock.anyDouble())).andReturn(floatMock);

        IntegerTD intMock = EasyMock.createMock(IntegerTD.class);
        EasyMock.expect(intMock.getDataType()).andReturn(DataType.Integer);
        EasyMock.replay(intMock);
        EasyMock.expect(tdf.createInteger(EasyMock.anyLong())).andReturn(intMock);

        ShortTextTD textMock = EasyMock.createMock(ShortTextTD.class);
        EasyMock.expect(textMock.getDataType()).andReturn(DataType.ShortText);
        EasyMock.replay(textMock);
        EasyMock.expect(tdf.createShortText(EasyMock.anyObject(String.class))).andReturn(textMock);

        VectorTD vectorMock = EasyMock.createMock(VectorTD.class);
        EasyMock.expect(vectorMock.getDataType()).andReturn(DataType.Vector);
        EasyMock.replay(vectorMock);
        EasyMock.expect(tdf.createVector(EasyMock.anyInt())).andReturn(vectorMock);

    }
}
