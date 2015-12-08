/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.testutils.TypedDatumSerializerDefaultStub;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link EvaluationMemoryFileAccessImpl}.
 * 
 * @author Doreen Seider
 *
 */
public class EvaluationMemoryFileAccessImplTest {

    private static final String DON_T_MATCH = "don't match";
    private static final String Y = "y";
    private static final String X2 = "x2";
    private static final String X1 = "x1";
    private File testFile;
    
    /**
     * Common setup.
     * 
     * @throws IOException on unexpected failure
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        testFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("some_file");
    }
    
    /**
     * Common cleanup.
     * 
     * @throws IOException on unexpected failure
     */
    @After
    public void tearDown() throws IOException {
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(testFile);
    }

    /**
     * Tests adding and getting values.
     * 
     * @throws IOException on unexpected failure
     */
    @Test
    public void testAddingAndGettingTuple() throws IOException {
        EvaluationMemoryFileAccessImpl fileAccess = new EvaluationMemoryFileAccessImpl(testFile.getAbsolutePath());
        fileAccess.setTypedDatumSerializer(new TypedDatumSerializerDefaultStub());
        
        SortedMap<String, DataType> inputs = createEndpointsDefinition(X1, X2);
        SortedMap<String, DataType> outputs = createEndpointsDefinition(Y);
        fileAccess.setInputsOutputsDefinition(inputs, outputs);
        
        SortedMap<String, Double> values1 = new TreeMap<>();
        values1.put(X1, 1.0);
        values1.put(X2, 2.0);

        SortedMap<String, Double> values2 = new TreeMap<>();
        values2.put(Y, 3.0);
        
        SortedMap<String, TypedDatum> inputValues1 = createEndpointValues(values1);
        SortedMap<String, TypedDatum> outputValues1 = createEndpointValues(values2);
        
        // add and get values
        fileAccess.addEvaluationValues(inputValues1, outputValues1);
        SortedMap<String, TypedDatum> result = fileAccess.getEvaluationResult(inputValues1, outputs);
        assertEquals(1, result.size());
        assertEquals(((FloatTD) outputValues1.get(outputValues1.firstKey())).getFloatValue(), 
            ((FloatTD) result.get(result.firstKey())).getFloatValue(), 0);
        
        SortedMap<String, Double> values3 = new TreeMap<>();
        values3.put(X1, 3.0);
        values3.put(X2, 2.0);

        SortedMap<String, Double> values4 = new TreeMap<>();
        values4.put(Y, 4.0);
        
        SortedMap<String, TypedDatum> inputValues2 = createEndpointValues(values3);
        SortedMap<String, TypedDatum> outputValues2 = createEndpointValues(values4);
        
        // request tuple for key, which first does not exist 
        result = fileAccess.getEvaluationResult(inputValues2, outputs);
        assertNull(result);
        fileAccess.addEvaluationValues(inputValues2, outputValues2);
        result = fileAccess.getEvaluationResult(inputValues2, outputs);
        assertEquals(1, result.size());
        assertEquals(((FloatTD) outputValues2.get(outputValues2.firstKey())).getFloatValue(), 
            ((FloatTD) result.get(result.firstKey())).getFloatValue(), 0);

        // add key and tuple, which already exists -> file size must not be changed
        long fileSizeBeforeAdding = FileUtils.sizeOf(testFile);
        fileAccess.addEvaluationValues(inputValues2, outputValues2);
        assertEquals(fileSizeBeforeAdding, FileUtils.sizeOf(testFile));
    }
    
    /**
     * Tests validation of evaluation memory.
     * 
     * @throws IOException on unexpected failure
     */
    @Test
    public void testValidation() throws IOException {
        
        EvaluationMemoryFileAccessImpl fileAccess = new EvaluationMemoryFileAccessImpl(testFile.getAbsolutePath());
        fileAccess.setTypedDatumSerializer(new TypedDatumSerializerDefaultStub());
        
        SortedMap<String, DataType> inputs1 = createEndpointsDefinition(X1, X2);
        SortedMap<String, DataType> outputs1 = createEndpointsDefinition(Y);
        
        try {
            fileAccess.validateEvaluationMemory(inputs1, outputs1);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Version information is missing"));
        }
        FileUtils.write(testFile, "version=1");
        try {
            fileAccess.validateEvaluationMemory(inputs1, outputs1);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Type information is missing"));
        }
        FileUtils.write(testFile, "type=de.rcenvironment.evaluationmemory");
        fileAccess.setInputsOutputsDefinition(inputs1, outputs1);
        
        fileAccess.validateEvaluationMemory(inputs1, outputs1);
        
        SortedMap<String, DataType> inputs2 = createEndpointsDefinition(X1);
        
        try {
            fileAccess.validateEvaluationMemory(inputs2, outputs1);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(DON_T_MATCH));
        }
        
        SortedMap<String, Double> values = new TreeMap<>();
        values.put(X1, 1.0);
        SortedMap<String, TypedDatum> inputValues = createEndpointValues(values);
        
        try {
            fileAccess.getEvaluationResult(inputValues, outputs1);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(DON_T_MATCH));
        }
        
        values.put(X2, 2.0);
        
        SortedMap<String, DataType> outputs2 = createEndpointsDefinition(Y, "Z");
        try {
            fileAccess.getEvaluationResult(inputValues, outputs2);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(DON_T_MATCH));
        }
        
        File testFile2 = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("some_file2");
        FileUtils.writeStringToFile(testFile2, IOUtils.toString(getClass().getResourceAsStream("/evalMemTest")));
        
        fileAccess = new EvaluationMemoryFileAccessImpl(testFile2.getAbsolutePath());
        fileAccess.setTypedDatumSerializer(new TypedDatumSerializerDefaultStub());
        try {
            fileAccess.validateEvaluationMemory(inputs1, outputs1);
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(DON_T_MATCH));
        }
    }

    private SortedMap<String, TypedDatum> createEndpointValues(SortedMap<String, Double> values) {
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        for (String name : values.keySet()) {
            inputValues.put(name, createFloatTypedDatumMock(values.get(name)));
        }
        return inputValues;
    }

    private SortedMap<String, DataType> createEndpointsDefinition(String... names) {
        SortedMap<String, DataType> values = new TreeMap<>();
        for (String name : names) {
            values.put(name, DataType.Float);
        }
        return values;
    }
    
    private FloatTD createFloatTypedDatumMock(double value) {
        FloatTD typedDatum = EasyMock.createNiceMock(FloatTD.class);
        EasyMock.expect(typedDatum.getDataType()).andReturn(DataType.Float).anyTimes();
        EasyMock.expect(typedDatum.getFloatValue()).andReturn(value).anyTimes();
        EasyMock.replay(typedDatum);
        return typedDatum;
    }
    

}
