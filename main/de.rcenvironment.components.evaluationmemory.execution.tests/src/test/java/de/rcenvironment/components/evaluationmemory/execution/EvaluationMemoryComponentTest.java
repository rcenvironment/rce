/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.components.evaluationmemory.execution.internal.EvaluationMemoryAccess;
import de.rcenvironment.components.evaluationmemory.execution.internal.EvaluationMemoryFileAccessService;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link EvaluationMemoryComponent}.
 * 
 * @author Doreen Seider
 */
public class EvaluationMemoryComponentTest {

    private static final String TO_EVAL_X1 = "x1";
    
    private static final String TO_EVAL_X2 = "x2";

    private static final String TO_EVAL_X3 = "x3";

    private static final String EVAL_Y1 = "y1";
    
    private static final String EVAL_Y2 = "y2";
    
    private String memoryFilePath;
    
    private String memoryFilePathAtWfStart;

    private ComponentTestWrapper component;

    private ConvergerComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;
    
    /**
     * Custom subclass of {@link ComponentContextMock} that adds common configuration and query methods.
     * 
     * @author Doreen Seider
     */
    private final class ConvergerComponentContextMock extends ComponentContextMock {

        private static final long serialVersionUID = 1570441783510990090L;

        public void configure(EvaluationMemoryFileAccessService service, boolean fileSelectedAtWfStart) {
            configure(service, fileSelectedAtWfStart, false);
        }
        
        public void configure(EvaluationMemoryFileAccessService service, boolean fileSelectedAtWfStart, boolean considerLoopFailures) {
            context.setConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE, memoryFilePath);
            context.setConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE_WF_START, memoryFilePathAtWfStart);
            context.setConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START, 
                String.valueOf(fileSelectedAtWfStart));
            context.setConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_CONSIDER_LOOP_FAILURES, 
                String.valueOf(considerLoopFailures));
            
            context.addSimulatedInput(TO_EVAL_X1, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, DataType.Float, true, null);
            context.addSimulatedInput(TO_EVAL_X2, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, DataType.Float, true, null);
            context.addSimulatedInput(TO_EVAL_X3, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, DataType.Float, true, null);
            
            context.addSimulatedInput(EVAL_Y1, EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, DataType.Float,
                true, null);
            context.addSimulatedInput(EVAL_Y2, EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, DataType.Float,
                true, null);
            
            context.addSimulatedInput(EvaluationMemoryComponentConstants.INPUT_NAME_LOOP_DONE, null, DataType.Boolean, 
                false, null);
            
            context.addSimulatedOutput(TO_EVAL_X1, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, DataType.Float, true, null);
            context.addSimulatedOutput(TO_EVAL_X2, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, DataType.Float, true, null);
            context.addSimulatedOutput(TO_EVAL_X3, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, DataType.Float, true, null);
            
            context.addSimulatedOutput(EVAL_Y1, EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, DataType.Float,
                true, null);
            context.addSimulatedOutput(EVAL_Y2, EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, DataType.Float,
                true, null);
            
            context.addService(EvaluationMemoryFileAccessService.class, service);
            
        }
    }
    
    /**
     * Common setup.
     * 
     * @throws IOException on unexpected error
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        memoryFilePath = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("file_1").getAbsolutePath();
        memoryFilePathAtWfStart = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("file_2").getAbsolutePath();
        context = new ConvergerComponentContextMock();
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();
    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {
        component.dispose();
    }

    /**
     * Tests if values are forwarded if evaluation memory is empty.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testCheckWithEmptyEvaluationMemory() throws ComponentException, IOException {
        
        FloatTD floatTD1 = typedDatumFactory.createFloat(1.0);
        FloatTD floatTD2 = typedDatumFactory.createFloat(2.0);
        FloatTD floatTD3 = typedDatumFactory.createFloat(3.0);
        
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(false);
        context.configure(accessService, false);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();
        
        context.setInputValue(TO_EVAL_X1, floatTD1);
        context.setInputValue(TO_EVAL_X2, floatTD2);
        context.setInputValue(TO_EVAL_X3, floatTD3);
        component.processInputs();
        
        assertEquals(1, context.getCapturedOutput(TO_EVAL_X1).size());
        assertEquals(floatTD1, context.getCapturedOutput(TO_EVAL_X1).get(0));
        assertEquals(1, context.getCapturedOutput(TO_EVAL_X2).size());
        assertEquals(floatTD2, context.getCapturedOutput(TO_EVAL_X2).get(0));
        assertEquals(1, context.getCapturedOutput(TO_EVAL_X3).size());
        assertEquals(floatTD3, context.getCapturedOutput(TO_EVAL_X3).get(0));

        assertEquals(0, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(0, context.getCapturedOutput(EVAL_Y2).size());

        assertEquals(0, context.getCapturedOutputClosings().size());
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }

    /**
     * Tests if evaluation results are used if evaluation memory is not empty.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testCheckWithNonEmptyEvaluationMemory() throws ComponentException, IOException {
        
        FloatTD floatTD1 = typedDatumFactory.createFloat(1.0);
        FloatTD floatTD2 = typedDatumFactory.createFloat(2.0);
        FloatTD floatTD3 = typedDatumFactory.createFloat(3.0);
        
        FloatTD floatTD4 = typedDatumFactory.createFloat(4.0);
        FloatTD floatTD5 = typedDatumFactory.createFloat(5.0);
        
        Map<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>> values = new HashMap<>();
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        inputValues.put(TO_EVAL_X1, floatTD1);
        inputValues.put(TO_EVAL_X2, floatTD2);
        inputValues.put(TO_EVAL_X3, floatTD3);
        SortedMap<String, TypedDatum> outputValues = new TreeMap<>();
        outputValues.put(EVAL_Y1, floatTD4);
        outputValues.put(EVAL_Y2, floatTD5);
        values.put(inputValues, outputValues);
        
        SortedMap<String, DataType> outputs = new TreeMap<>();
        outputs.put(EVAL_Y1, DataType.Float);
        outputs.put(EVAL_Y2, DataType.Float);
        
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(values, outputs, true);
        context.configure(accessService, true);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();
        
        context.setInputValue(TO_EVAL_X1, floatTD1);
        context.setInputValue(TO_EVAL_X2, floatTD2);
        context.setInputValue(TO_EVAL_X3, floatTD3);
        component.processInputs();
        
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X1).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X2).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X3).size());

        assertEquals(1, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(floatTD4, context.getCapturedOutput(EVAL_Y1).get(0));
        assertEquals(1, context.getCapturedOutput(EVAL_Y2).size());
        assertEquals(floatTD5, context.getCapturedOutput(EVAL_Y2).get(0));

        assertEquals(0, context.getCapturedOutputClosings().size());
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }
    
    /**
     * Tests if evaluation results are stored properly.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testStoreWithValuesOfTypeNotAValue() throws ComponentException, IOException {
        testForwardingWithValuesOfTypeNotAValue(true);
        testForwardingWithValuesOfTypeNotAValue(false);
    }

    private void testForwardingWithValuesOfTypeNotAValue(boolean considerNotAValue) throws IOException, ComponentException {
        Capture<SortedMap<String, TypedDatum>> inputValuesCapture = new Capture<>();
        Capture<SortedMap<String, TypedDatum>> outputCapture = new Capture<>();
        Map<Capture<SortedMap<String, TypedDatum>>, Capture<SortedMap<String, TypedDatum>>> captures = new HashMap<>();
        captures.put(inputValuesCapture, outputCapture);

        FloatTD floatTD = typedDatumFactory.createFloat(1.0);
        NotAValueTD nAVTD = typedDatumFactory.createNotAValue();
        
        Map<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>> values = new HashMap<>();
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        inputValues.put(TO_EVAL_X1, floatTD);
        inputValues.put(TO_EVAL_X2, floatTD);
        inputValues.put(TO_EVAL_X3, floatTD);
        SortedMap<String, TypedDatum> outputValues = new TreeMap<>();
        outputValues.put(EVAL_Y1, nAVTD);
        outputValues.put(EVAL_Y2, floatTD);
        values.put(inputValues, outputValues);
        
        SortedMap<String, DataType> outputs = new TreeMap<>();
        outputs.put(EVAL_Y1, DataType.Float);
        outputs.put(EVAL_Y2, DataType.Float);
        
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(values, outputs, captures, true);
        context.configure(accessService, true, considerNotAValue);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();

        context.setInputValue(TO_EVAL_X1, floatTD);
        context.setInputValue(TO_EVAL_X2, floatTD);
        context.setInputValue(TO_EVAL_X3, floatTD);
        component.processInputs();
        
        if (considerNotAValue) {
            assertEquals(0, context.getCapturedOutput(TO_EVAL_X1).size());
            assertEquals(1, context.getCapturedOutput(EVAL_Y1).size());
        } else {
            assertEquals(1, context.getCapturedOutput(TO_EVAL_X1).size());
            assertEquals(0, context.getCapturedOutput(EVAL_Y1).size());
        }
    }
    
    /**
     * Tests if evaluation results are used if evaluation memory is not empty.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testCheckWithInconsistentEvaluationMemory() throws ComponentException, IOException {
        
        FloatTD floatTD1 = typedDatumFactory.createFloat(1.0);
        FloatTD floatTD2 = typedDatumFactory.createFloat(2.0);
        FloatTD floatTD3 = typedDatumFactory.createFloat(3.0);
        FloatTD floatTD4 = typedDatumFactory.createFloat(4.0);
        
        Map<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>> values = new HashMap<>();
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        inputValues.put(TO_EVAL_X1, floatTD1);
        inputValues.put(TO_EVAL_X2, floatTD2);
        inputValues.put(TO_EVAL_X3, floatTD3);
        values.put(inputValues, null);
        
        SortedMap<String, DataType> outputs = new TreeMap<>();
        outputs.put(EVAL_Y1, DataType.Float);
        outputs.put(EVAL_Y2, DataType.Float);
        
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(values, outputs, false);
        
        context.configure(accessService, false);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();
        
        context.setInputValue(TO_EVAL_X1, floatTD1);
        context.setInputValue(TO_EVAL_X2, floatTD2);
        context.setInputValue(TO_EVAL_X3, floatTD3);
        component.processInputs();
        
        // even if tuple key exists in the store, the values are forwarded as the tuple size doesn't match the number of outputs
        assertEquals(1, context.getCapturedOutput(TO_EVAL_X1).size());
        assertEquals(floatTD1, context.getCapturedOutput(TO_EVAL_X1).get(0));
        assertEquals(1, context.getCapturedOutput(TO_EVAL_X2).size());
        assertEquals(floatTD2, context.getCapturedOutput(TO_EVAL_X2).get(0));
        assertEquals(1, context.getCapturedOutput(TO_EVAL_X3).size());
        assertEquals(floatTD3, context.getCapturedOutput(TO_EVAL_X3).get(0));

        assertEquals(0, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(0, context.getCapturedOutput(EVAL_Y2).size());

        assertEquals(0, context.getCapturedOutputClosings().size());
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }

    /**
     * Tests if evaluation results are stored properly.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testStore() throws ComponentException, IOException {
        
        Capture<SortedMap<String, TypedDatum>> inputValuesCapture = new Capture<>();
        Capture<SortedMap<String, TypedDatum>> outputCapture = new Capture<>();
        Map<Capture<SortedMap<String, TypedDatum>>, Capture<SortedMap<String, TypedDatum>>> captures = new HashMap<>();
        captures.put(inputValuesCapture, outputCapture);
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(
            new HashMap<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>>(), 
            new TreeMap<String, DataType>(), captures, true);
        context.configure(accessService, true);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();

        FloatTD floatTD1 = typedDatumFactory.createFloat(1.0);
        FloatTD floatTD2 = typedDatumFactory.createFloat(2.0);
        FloatTD floatTD3 = typedDatumFactory.createFloat(3.0);
        
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        inputValues.put(TO_EVAL_X1, floatTD1);
        inputValues.put(TO_EVAL_X2, floatTD2);
        inputValues.put(TO_EVAL_X3, floatTD3);

        context.setInputValue(TO_EVAL_X1, floatTD1);
        context.setInputValue(TO_EVAL_X2, floatTD2);
        context.setInputValue(TO_EVAL_X3, floatTD3);
        component.processInputs();
        
        FloatTD floatTD4 = typedDatumFactory.createFloat(4.0);
        FloatTD floatTD5 = typedDatumFactory.createFloat(5.0);
        context.setInputValue(EVAL_Y1, floatTD4);
        context.setInputValue(EVAL_Y2, floatTD5);
        component.processInputs();
        
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X1).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X2).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X3).size());

        assertEquals(1, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(floatTD4, context.getCapturedOutput(EVAL_Y1).get(0));
        assertEquals(1, context.getCapturedOutput(EVAL_Y2).size());
        assertEquals(floatTD5, context.getCapturedOutput(EVAL_Y2).get(0));

        assertEquals(0, context.getCapturedOutputClosings().size());
        
        assertEquals(inputValues, inputValuesCapture.getValue());
        
        SortedMap<String, TypedDatum> outputValues = outputCapture.getValue();
        assertEquals(floatTD4, outputValues.get(EVAL_Y1));
        assertEquals(floatTD5, outputValues.get(EVAL_Y2));
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }
    
    /**
     * Tests if evaluation results are stored properly.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testCheckAndStoreWithIOFailure() throws ComponentException, IOException {
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerServiceCreatingFailingMemoryAccessInstances(false);
        context.configure(accessService, false);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();

        FloatTD floatTD1 = typedDatumFactory.createFloat(1.0);
        FloatTD floatTD2 = typedDatumFactory.createFloat(2.0);
        FloatTD floatTD3 = typedDatumFactory.createFloat(3.0);

        context.setInputValue(TO_EVAL_X1, floatTD1);
        context.setInputValue(TO_EVAL_X2, floatTD2);
        context.setInputValue(TO_EVAL_X3, floatTD3);
        component.processInputs();
        
        FloatTD floatTD4 = typedDatumFactory.createFloat(4.0);
        FloatTD floatTD5 = typedDatumFactory.createFloat(5.0);
        context.setInputValue(EVAL_Y1, floatTD4);
        context.setInputValue(EVAL_Y2, floatTD5);
        component.processInputs();
        
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X1).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X2).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X3).size());

        assertEquals(1, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(floatTD4, context.getCapturedOutput(EVAL_Y1).get(0));
        assertEquals(1, context.getCapturedOutput(EVAL_Y2).size());
        assertEquals(floatTD5, context.getCapturedOutput(EVAL_Y2).get(0));

        assertEquals(0, context.getCapturedOutputClosings().size());
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }
    
    /**
     * Tests if evaluation results are stored properly.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testStoreIfNoKeyStoredPreviously() throws ComponentException, IOException {
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(true);
        context.configure(accessService, true);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();

        FloatTD floatTD4 = typedDatumFactory.createFloat(4.0);
        FloatTD floatTD5 = typedDatumFactory.createFloat(5.0);
        context.setInputValue(EVAL_Y1, floatTD4);
        context.setInputValue(EVAL_Y2, floatTD5);
        
        try {
            component.processInputs();
            fail("Expected 'processInputs' to fail, as no memory should be available");
        } catch (ComponentException e) {
            assertTrue(e.getMessage().contains("no values"));
        }
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }
    
    /**
     * Tests if evaluation results are stored properly.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testFileAccessFailureHandling() throws ComponentException, IOException {
        context.configure(createFailingFileAccessHandlerService(false), false);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        try {
            component.start();
            fail();
        } catch (ComponentException e) {
            assertTrue(e.getMessage().contains("Failed to access"));
        }
    }
    
    /**
     * Tests if input "Loop done" is handled correctly.
     * 
     * @throws ComponentException on unexpected component failures
     * @throws IOException on unexpected failures
     */
    @Test
    public void testLoopDone() throws ComponentException, IOException {
        EvaluationMemoryFileAccessService accessService = createFileAccessHandlerService(true);
        context.configure(accessService, true);
        component = new ComponentTestWrapper(new EvaluationMemoryComponent(), context);
        component.start();
        
        context.setInputValue(EvaluationMemoryComponentConstants.INPUT_NAME_LOOP_DONE, typedDatumFactory.createBoolean(false));
        component.processInputs();
        
        List<String> outputsClosed = context.getCapturedOutputClosings();
        assertEquals(0, outputsClosed.size());

        assertEquals(0, context.getCapturedOutput(TO_EVAL_X1).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X2).size());
        assertEquals(0, context.getCapturedOutput(TO_EVAL_X3).size());
        assertEquals(0, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(0, context.getCapturedOutput(EVAL_Y2).size());
        
        context.setInputValue(EvaluationMemoryComponentConstants.INPUT_NAME_LOOP_DONE, typedDatumFactory.createBoolean(true));
        component.processInputs();
        
        outputsClosed = context.getCapturedOutputClosings();
        assertEquals(3, outputsClosed.size());
        assertTrue(outputsClosed.contains(TO_EVAL_X1));
        assertTrue(outputsClosed.contains(TO_EVAL_X2));
        assertTrue(outputsClosed.contains(TO_EVAL_X3));

        assertEquals(0, context.getCapturedOutput(EVAL_Y1).size());
        assertEquals(0, context.getCapturedOutput(EVAL_Y2).size());
        
        component.tearDown(Component.FinalComponentState.FINISHED);
        EasyMock.verify(accessService);
    }
    
    private EvaluationMemoryFileAccessService createFileAccessHandlerService(boolean fileFromStart) 
        throws IOException {
        return createFileAccessHandlerService(new HashMap<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>>(), 
            new TreeMap<String, DataType>(),
            new HashMap<Capture<SortedMap<String, TypedDatum>>, Capture<SortedMap<String, TypedDatum>>>(), 
            fileFromStart);
    }
    
    private EvaluationMemoryFileAccessService createFileAccessHandlerService(
        Map<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>> values,
        SortedMap<String, DataType> outputs, boolean fileFromStart) throws IOException {
        return createFileAccessHandlerService(values, outputs,
            new HashMap<Capture<SortedMap<String, TypedDatum>>, Capture<SortedMap<String, TypedDatum>>>(),
            fileFromStart);
    }
    
    private EvaluationMemoryFileAccessService createFileAccessHandlerService(
        Map<SortedMap<String, TypedDatum>, SortedMap<String, TypedDatum>> values, 
        SortedMap<String, DataType> outputs,
        Map<Capture<SortedMap<String, TypedDatum>>, Capture<SortedMap<String, TypedDatum>>> captures, 
        boolean fileFromStart) throws IOException {
        String filePath = memoryFilePath;
        if (fileFromStart) {
            filePath = memoryFilePathAtWfStart;
        }
        EvaluationMemoryAccess fileAccess = EasyMock.createStrictMock(EvaluationMemoryAccess.class);
        for (SortedMap<String, TypedDatum> key : values.keySet()) {
            if (values.get(key) == null) {
                EasyMock.expect(fileAccess.getEvaluationResult(key, outputs)).andStubThrow(new IOException());                
            } else {
                EasyMock.expect(fileAccess.getEvaluationResult(key, outputs)).andStubReturn(values.get(key));
            }
        }
        if (values.isEmpty()) {
            EasyMock.expect(fileAccess.getEvaluationResult(EasyMock.anyObject(SortedMap.class), EasyMock.anyObject(SortedMap.class)))
                .andStubReturn(null);
        }
        for (Capture<SortedMap<String, TypedDatum>> keyCapture : captures.keySet()) {
            fileAccess.addEvaluationValues(EasyMock.capture(keyCapture), EasyMock.capture(captures.get(keyCapture)));
            EasyMock.expectLastCall().asStub();
        }
        if (captures.isEmpty()) {
            fileAccess.addEvaluationValues(EasyMock.anyObject(SortedMap.class), EasyMock.anyObject(SortedMap.class));
            EasyMock.expectLastCall().asStub();
        }
        fileAccess.setInputsOutputsDefinition(EasyMock.anyObject(SortedMap.class), EasyMock.anyObject(SortedMap.class));
        EasyMock.expectLastCall().asStub();
        EasyMock.replay(fileAccess);
        EvaluationMemoryFileAccessService service = EasyMock.createStrictMock(EvaluationMemoryFileAccessService.class);
        EasyMock.expect(service.acquireAccessToMemoryFile(filePath)).andReturn(fileAccess);
        EasyMock.expect(service.releaseAccessToMemoryFile(filePath)).andReturn(true);
        EasyMock.replay(service);
        return service;
    }
    
    @SuppressWarnings("unchecked")
    private EvaluationMemoryFileAccessService createFileAccessHandlerServiceCreatingFailingMemoryAccessInstances(
        boolean fileFromStart) throws IOException {
        String filePath = getFilePath(fileFromStart);
        EvaluationMemoryAccess fileAccess = EasyMock.createStrictMock(EvaluationMemoryAccess.class);
        EasyMock.expect(fileAccess.getEvaluationResult(EasyMock.anyObject(SortedMap.class), 
            EasyMock.anyObject(SortedMap.class))).andThrow(new IOException());
        fileAccess.addEvaluationValues(EasyMock.anyObject(SortedMap.class), EasyMock.anyObject(SortedMap.class));
        EasyMock.expectLastCall().andThrow(new IOException());
        fileAccess.setInputsOutputsDefinition(EasyMock.anyObject(SortedMap.class), EasyMock.anyObject(SortedMap.class));
        EasyMock.expectLastCall().asStub();
        EasyMock.replay(fileAccess);
        EvaluationMemoryFileAccessService service = EasyMock.createStrictMock(EvaluationMemoryFileAccessService.class);
        EasyMock.expect(service.acquireAccessToMemoryFile(filePath)).andReturn(fileAccess);
        EasyMock.expect(service.releaseAccessToMemoryFile(filePath)).andReturn(true);
        EasyMock.replay(service);
        return service;
    }
    
    private EvaluationMemoryFileAccessService createFailingFileAccessHandlerService(boolean fileFromStart) throws IOException {
        String filePath = getFilePath(fileFromStart);
        EvaluationMemoryFileAccessService service = EasyMock.createStrictMock(EvaluationMemoryFileAccessService.class);
        EasyMock.expect(service.acquireAccessToMemoryFile(filePath)).andThrow(new IOException());
        EasyMock.expect(service.releaseAccessToMemoryFile(filePath)).andReturn(true);
        EasyMock.replay(service);
        return service;
    }
    
    private String getFilePath(boolean fileFromStart) {
        String filePath = memoryFilePath;
        if (fileFromStart) {
            filePath = memoryFilePathAtWfStart;
        }
        return filePath;
    }
    
}
