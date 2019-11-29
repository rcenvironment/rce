/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
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

    private static final double VALUE01 = 0.1;

    private static final double VALUE11 = 1.1;

    /**
     * Throughout this test suite, we often check input and output vectors returned from the class under test. These values have, however,
     * often been serialized and deserialized in the meantime, i.e., the vectors we compare against contain actual instances of TypedDatum.
     * The expected values, in contrast, contain mainly mocked versions of TypedDatum. Hence, the standard equality testing via .equals does
     * not work in this case. Instead, we use this custom matcher to compare TypedDatums to mocks via their interface.
     *
     * @author Alexander Weinert
     */
    private static class FloatVectorMatcher implements IArgumentMatcher {

        private final SortedMap<String, TypedDatum> expected;

        FloatVectorMatcher(SortedMap<String, TypedDatum> expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object someOther) {
            if (!(someOther instanceof SortedMap<?, ?>)) {
                return false;
            }
            
            final SortedMap<String, TypedDatum> other = (SortedMap<String, TypedDatum>) someOther;
            if (other.size() != expected.size()) {
                return false;
            }
            for (Entry<String, TypedDatum> otherEntry : other.entrySet()) {
                final String otherKey = otherEntry.getKey();
                if (!expected.containsKey(otherKey)) {
                    return false;
                }
                final double expectedValue = ((FloatTD) expected.get(otherKey)).getFloatValue();
                final double otherValue = ((FloatTD) otherEntry.getValue()).getFloatValue();
                if (expectedValue != otherValue) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void appendTo(StringBuffer stringBuffer) {
            stringBuffer.append("vecEq({");
            final Iterator<Entry<String, TypedDatum>> it = expected.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, TypedDatum> entry = it.next();
                stringBuffer.append(entry.getKey());
                stringBuffer.append(" -> ");
                stringBuffer.append(entry.getValue().getDataType().toString());
                if (it.hasNext()) {
                    stringBuffer.append(", ");
                }
            }
            stringBuffer.append("})");
        }

        public static SortedMap<String, TypedDatum> eqVec(SortedMap<String, TypedDatum> expected) {
            EasyMock.reportMatcher(new FloatVectorMatcher(expected));
            return null;
        }
    }

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

        SortedMap<String, Double> tolerances = new TreeMap<>();
        tolerances.put(X1, null);
        tolerances.put(X2, null);

        SortedMap<String, Double> values2 = new TreeMap<>();
        values2.put(Y, 3.0);
        
        SortedMap<String, TypedDatum> inputValues1 = createEndpointValues(values1);
        SortedMap<String, TypedDatum> outputValues1 = createEndpointValues(values2);
        
        // add and get values
        fileAccess.addEvaluationValues(inputValues1, outputValues1);
        ToleranceHandling toleranceHandling = EasyMock.createStrictMock(ToleranceHandling.class);
        EasyMock.replay(toleranceHandling);
        SortedMap<String, TypedDatum> result = fileAccess.getEvaluationResult(inputValues1, outputs, tolerances, toleranceHandling);
        assertEquals(1, result.size());
        assertEquals(((FloatTD) outputValues1.get(outputValues1.firstKey())).getFloatValue(), 
            ((FloatTD) result.get(result.firstKey())).getFloatValue(), 0);
        
        SortedMap<String, Double> values3 = new TreeMap<>();
        values3.put(X1, VALUE11);
        values3.put(X2, 2.0);

        SortedMap<String, Double> values4 = new TreeMap<>();
        values4.put(Y, 4.0);
        
        SortedMap<String, TypedDatum> inputValues2 = createEndpointValues(values3);
        SortedMap<String, TypedDatum> outputValues2 = createEndpointValues(values4);
        
        // request tuple for key, which first does not exist 
        toleranceHandling = EasyMock.createStrictMock(ToleranceHandling.class);
        EasyMock
            .expect(toleranceHandling.isInToleranceInterval(EasyMock.eq(inputValues2), EasyMock.eq(tolerances),
                FloatVectorMatcher.eqVec(inputValues1)))
            .andReturn(false);
        Capture<Collection<SortedMap<String, TypedDatum>>> toleratedInputsCapture = new Capture<>();
        EasyMock.expect(toleranceHandling.pickMostToleratedInputs(EasyMock.capture(toleratedInputsCapture), EasyMock.eq(inputValues2)))
            .andReturn(null);
        EasyMock.replay(toleranceHandling);
        result = fileAccess.getEvaluationResult(inputValues2, outputs, tolerances, toleranceHandling);
        assertTrue(toleratedInputsCapture.getValue().isEmpty());
        assertNull(result);
        EasyMock.verify(toleranceHandling);

        // request tuple for key with some tolerance
        tolerances.put(X1, new Double(VALUE01));
        toleranceHandling = EasyMock.createStrictMock(ToleranceHandling.class);
        EasyMock
            .expect(toleranceHandling.isInToleranceInterval(EasyMock.eq(inputValues2), EasyMock.eq(tolerances),
                FloatVectorMatcher.eqVec(inputValues1)))
            .andReturn(true);
        toleratedInputsCapture = new Capture<>();
        EasyMock.expect(toleranceHandling.pickMostToleratedInputs(EasyMock.capture(toleratedInputsCapture), EasyMock.eq(inputValues2)))
            .andReturn(inputValues1);
        EasyMock.replay(toleranceHandling);

        result = fileAccess.getEvaluationResult(inputValues2, outputs, tolerances, toleranceHandling);
        EasyMock.verify(toleranceHandling);

        assertEquals(1, toleratedInputsCapture.getValue().size());
        assertTrue(new FloatVectorMatcher(inputValues1).matches(toleratedInputsCapture.getValue().iterator().next()));

        assertEquals(1, result.size());
        assertEquals(((FloatTD) outputValues1.get(outputValues1.firstKey())).getFloatValue(),
            ((FloatTD) result.get(result.firstKey())).getFloatValue(), 0);

        // Add precise evaluation results, fetch them, and check that even in the presence of tolerance the precise value is returned
        fileAccess.addEvaluationValues(inputValues2, outputValues2);
        toleranceHandling = EasyMock.createStrictMock(ToleranceHandling.class);
        EasyMock.replay(toleranceHandling);
        result = fileAccess.getEvaluationResult(inputValues2, outputs, tolerances, toleranceHandling);
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
        
        SortedMap<String, Double> tolerances = new TreeMap<>();
        tolerances.put(X1, null);

        try {
            fileAccess.getEvaluationResult(inputValues, outputs1, tolerances, createToleranceHandlingMock());
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains(DON_T_MATCH));
        }
        
        values.put(X2, 2.0);
        
        SortedMap<String, DataType> outputs2 = createEndpointsDefinition(Y, "Z");
        try {
            fileAccess.getEvaluationResult(inputValues, outputs2, tolerances, createToleranceHandlingMock());
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

    private ToleranceHandling createToleranceHandlingMock() {
        ToleranceHandling toleranceHandling = EasyMock.createNiceMock(ToleranceHandling.class);
        EasyMock.expect(toleranceHandling.isInToleranceInterval(EasyMock.anyObject(SortedMap.class), EasyMock.anyObject(SortedMap.class),
            EasyMock.anyObject(SortedMap.class))).andStubReturn(false);

        EasyMock
            .expect(toleranceHandling.pickMostToleratedInputs(EasyMock.anyObject(Collection.class), EasyMock.anyObject(SortedMap.class)))
            .andStubReturn(null);

        EasyMock.replay(toleranceHandling);
        return toleranceHandling;
    }

}
