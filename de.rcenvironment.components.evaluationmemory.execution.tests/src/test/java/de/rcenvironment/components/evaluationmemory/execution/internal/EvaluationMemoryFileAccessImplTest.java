/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.testutils.TypedDatumSerializerDefaultStub;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
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

    private List<File> tempFiles = new ArrayList<>();
    
    /**
     * Common setup.
     * 
     * @throws IOException on unexpected failure
     */
    @BeforeClass
    public static void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }
    
    /**
     * Common cleanup.
     * 
     * @throws IOException on unexpected failure
     */
    @After
    public void tearDown() throws IOException {
        for (File file : tempFiles) {
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(file);            
        }
    }

    /**
     * Tests adding and getting values.
     * 
     * @throws IOException on unexpected failure
     */
    @Test
    public void testAddingAndGettingTuple() throws IOException {
        File testFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("some_file");
        tempFiles.add(testFile);
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
        
        // add values of type not-a-value
        SortedMap<String, Double> values5 = new TreeMap<>();
        values5.put(X1, 3.0);
        values5.put(X2, 2.0);

        SortedMap<String, Double> values6 = new TreeMap<>();
        values6.put(Y, null);
        
        SortedMap<String, TypedDatum> inputValues3 = createEndpointValues(values5);
        SortedMap<String, TypedDatum> outputValues4 = createEndpointValues(values6);
        
        fileAccess.addEvaluationValues(inputValues3, outputValues4);
    }
    
    /**
     * Tests validation of evaluation memory.
     * 
     * @throws IOException on unexpected failure
     */
    @Test
    public void testValidation() throws IOException {
        File testFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("some_file");
        tempFiles.add(testFile);
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
        tempFiles.add(testFile2);
        FileUtils.writeStringToFile(testFile2, IOUtils.toString(getClass().getResourceAsStream("/validEvalMem")));
        
        fileAccess = new EvaluationMemoryFileAccessImpl(testFile2.getAbsolutePath());
        fileAccess.setTypedDatumSerializer(new TypedDatumSerializerDefaultStub());
        fileAccess.validateEvaluationMemory(inputs1, outputs1);
            
        File testFile3 = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("some_file3");
        tempFiles.add(testFile3);
        FileUtils.writeStringToFile(testFile3, IOUtils.toString(getClass().getResourceAsStream("/invalidEvalMem")));
        
        fileAccess = new EvaluationMemoryFileAccessImpl(testFile3.getAbsolutePath());
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
            if (values.get(name) != null) {
                inputValues.put(name, createFloatTypedDatumMock(values.get(name)));                
            } else {
                inputValues.put(name, createNotAValueTypedDatumMock());
            }
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
        FloatTD typedDatum = EasyMock.createStrictMock(FloatTD.class);
        EasyMock.expect(typedDatum.getDataType()).andStubReturn(DataType.Float);
        EasyMock.expect(typedDatum.getFloatValue()).andStubReturn(value);
        EasyMock.replay(typedDatum);
        return typedDatum;
    }
    
    private NotAValueTD createNotAValueTypedDatumMock() {
        NotAValueTD typedDatum = EasyMock.createStrictMock(NotAValueTD.class);
        EasyMock.expect(typedDatum.getDataType()).andStubReturn(DataType.NotAValue);
        EasyMock.expect(typedDatum.getIdentifier()).andStubReturn("id");
        EasyMock.expect(typedDatum.getCause()).andStubReturn(NotAValueTD.Cause.InvalidInputs);
        EasyMock.replay(typedDatum);
        return typedDatum;
    }
}
