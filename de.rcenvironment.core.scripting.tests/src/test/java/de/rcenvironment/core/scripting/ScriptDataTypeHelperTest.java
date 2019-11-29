/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.testutils.TypedDatumFactoryDefaultStub;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import org.junit.Assert;

/**
 * Test class for the {@link ScriptDataTypeHelper}.
 * 
 * @author Sascha Zur
 * @author Martin Misiak
 */
public class ScriptDataTypeHelperTest {
        
    private static final String NAN = "NaN";
    //Boolean,Integer,Long,BigInteger,Float,Double,String
    private static final Object[][] TEST_DATA_TYPES = { {false, true},
                                                        {26, 0, Integer.MAX_VALUE, Integer.MIN_VALUE},
                                                        {26L, 0L, Long.MAX_VALUE, Long.MIN_VALUE},
                                                        {BigInteger.valueOf(26L), BigInteger.valueOf(0L),
                                                         new BigInteger("9923372036854775807")},
                                                        {26f, 26.0f, 0.0f, 0f, Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN,
                                                         Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY},
                                                        {26.0, 26d, 0.0, 0d, Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN,
                                                         Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY},
                                                        {"string", "Infinity", "+Infinity", "-Infinity", "Inf", "+Inf", "-Inf",
                                                         "NaN", "nan", "true", "false", "none", "{}", "[]", "()", ""} };
    

    
    private static final Object[] TEST_DATA_ONE_DIMENSIONAL = {9.2, 8.5, 6.4};
    
    private static final Object[][] TEST_DATA_TWO_DIMENSIONAL_A = { {9.2, 8.5, 6.4},
                                                                    {1.3},
                                                                    {1.2, 2.3, 2.4} };
    
    private static final Object[][] TEST_DATA_TWO_DIMENSIONAL_B = { {9.2, 8.5, 6.4},
                                                                    {1.3, 5.3, 3.5},
                                                                    {1.2, 2.3, 2.4} };
    
    private static final Object[][][] TEST_DATA_THREE_DIMENSIONAL = { { {4.6, 3.5} } };
    
    private static final int NO_VALUE = -1;
    private static final String NO_EXCEPTION_TEXT = "No Exception was thrown for: ";
    private static final String APOS = " \"";
    private static final String RESULT_STRING = ", converted value is ";
    private static final String OF_CLASS = " of class: ";
    
    
    /**
     * 
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ScriptingUtils scriptingUtils = new ScriptingUtils();

    public ScriptDataTypeHelperTest() {
        scriptingUtils.bindTypedDatumService(new TypedDatumServiceDefaultStub());
    }
    
    /**
     * 
     */
    @Before
    public void setUp() {

    }



    private Object arrayToList(Object possibleArray) {
        if (possibleArray instanceof Object[]) {
            Object[] array = (Object[]) possibleArray;
            List<Object> list = new ArrayList<Object>(array.length);
            for (Object o : array) {
                list.add(arrayToList(o));
            }
            return list;
        } else {
            return possibleArray;
        }
    }


    
    /**
     * 
     * Unit-Test for converting Java values to {@link BooleanTD}.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionBooleanTD() throws ComponentException {
        
        DataType reqType = DataType.Boolean;
        boolean[][] conversionResults = {{false, true},
                                         {true, false, true, true},
                                         {true, false, true, true},
                                         {true, false, true},
                                         {true, true, false, false, true, true, true, true, true},
                                         {true, true, false, false, true, true, true, true, true},
                                         {true, true, true, true, true, true, true, true, true, true, false,
                                          false, false, false, false, false}};
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                result = ScriptingUtils.getOutputByType(TEST_DATA_TYPES[i][j], reqType, null, null, null);
                assertTrue(result instanceof BooleanTD);
                BooleanTD convResult = (BooleanTD) (result);
                assertTrue("Conversion to BooleanTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                           + " " + APOS + TEST_DATA_TYPES[i][j] +  APOS,
                            convResult.getBooleanValue() == conversionResults[i][j]);
            }
        }
       
    }
    
    /**
     * 
     * Unit-Test for converting Java values to {@link IntegerTD}.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionIntegerTD() throws ComponentException {
        
        DataType reqType = DataType.Integer;
        
        final long[][] conversionResults =
        {{NO_VALUE, NO_VALUE},
         {26, 0, Integer.MAX_VALUE, Integer.MIN_VALUE},
         {26, 0, Long.MAX_VALUE, Long.MIN_VALUE},
         {26, 0, NO_VALUE},
         {NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE},
         {NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE},
         {NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE,
          NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE}};
        
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                if (conversionResults[i][j] != NO_VALUE) {
                    result = ScriptingUtils.getOutputByType(TEST_DATA_TYPES[i][j], reqType, null, null, null);
                    assertTrue(result instanceof IntegerTD);
                    IntegerTD convResult = (IntegerTD) (result);
                    assertTrue("Conversion to IntegerTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                               + " " + APOS + TEST_DATA_TYPES[i][j] +  APOS,
                               convResult.getIntValue() == conversionResults[i][j]);
                } else {
                    String errorMsg = APOS + TEST_DATA_TYPES[i][j] +  APOS
                        + OF_CLASS + TEST_DATA_TYPES[i][j].getClass().getName();
                    handleFailedConversion(TEST_DATA_TYPES[i][j], reqType, errorMsg);
                }   
            }
        }   
    }
    
    
    /**
     * 
     * Unit-Test for converting Java values to {@link FloatTD}.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionFloatTD() throws ComponentException {
        
        DataType reqType = DataType.Float;
        
        final double[][] conversionResults =
        {{NO_VALUE, NO_VALUE},
         {26d, 0d, Integer.MAX_VALUE, Integer.MIN_VALUE},
         {26d, 0d, Long.MAX_VALUE, Long.MIN_VALUE},
         {26d, 0d, 9923372036854775807d},
         {26d, 26d, 0d, 0d, Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY},
         {26d, 26d, 0d, 0d, Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY},
         {NO_VALUE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, NO_VALUE, NO_VALUE,
          NO_VALUE, Double.NaN, Double.NaN, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE}};
        
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                if (conversionResults[i][j] != NO_VALUE) {
                    result = ScriptingUtils.getOutputByType(TEST_DATA_TYPES[i][j], reqType, null, null, null);
                    assertTrue(result instanceof FloatTD);
                    FloatTD convResult = (FloatTD) (result);
                    //NaN != NaN so need a special string-based check for conversion correctness.
                    if (String.valueOf(conversionResults[i][j]).equals(NAN)) {
                        
                        assertTrue("Conversion to FloatTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                            + " " + APOS + TEST_DATA_TYPES[i][j] + APOS +  RESULT_STRING + convResult.getFloatValue(),
                            String.valueOf(convResult.getFloatValue()).equals(NAN));
                    } else {
                        
                        assertTrue("Conversion to FloatTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                                   + " " + APOS + TEST_DATA_TYPES[i][j] + APOS + RESULT_STRING + convResult.getFloatValue(),
                                   convResult.getFloatValue() == conversionResults[i][j]);
                    }
                } else {
                    String errorMsg = APOS + TEST_DATA_TYPES[i][j] +  APOS
                        + OF_CLASS + TEST_DATA_TYPES[i][j].getClass().getName();
                    handleFailedConversion(TEST_DATA_TYPES[i][j], reqType, errorMsg);
                }   
            }
        }   
    }

    
    private void handleFailedConversion(Object obj, DataType type, String errorMsg) {
        
        try {
            TypedDatum result = ScriptingUtils.getOutputByType(obj, type, null, null, null);
            fail(NO_EXCEPTION_TEXT + errorMsg);
        } catch (ComponentException ex) {
            assertTrue(ex != null);
        }
        
    }
    
    /**
     * 
     * Unit-Test for converting list based Java values to {@link VectorTD}, {@link MatrixTD}, {@link SmallTableTD}.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionListType() throws ComponentException {
        
        DataType reqType;
        TypedDatum result;
        
        List<Object> listOneDim = (ArrayList<Object>) arrayToList(TEST_DATA_ONE_DIMENSIONAL);
        List<Object> listTwoDimA = (ArrayList<Object>) arrayToList(TEST_DATA_TWO_DIMENSIONAL_A);
        List<Object> listTwoDimB = (ArrayList<Object>) arrayToList(TEST_DATA_TWO_DIMENSIONAL_B);
        List<Object> listThreeDim = (ArrayList<Object>) arrayToList(TEST_DATA_THREE_DIMENSIONAL);
        
        //Test VectorTD
        reqType = DataType.Vector;
        result = ScriptingUtils.getOutputByType(listOneDim, reqType, null, null, null);
        assertTrue(result instanceof VectorTD);
        handleFailedConversion(listTwoDimA, reqType, "Converting a 2-dimensional list to VectorTD");
        handleFailedConversion(listTwoDimB, reqType, "Converting a 2-dimensional list to VectorTD");
        handleFailedConversion(listThreeDim, reqType, "Converting a 2+ dimensional list to VectorTD");
        
        //Test MatrixTD
        reqType = DataType.Matrix;
        handleFailedConversion(listOneDim, reqType, "Converting a 1-dimensional list to MatrixTD");
        handleFailedConversion(listTwoDimA, reqType, "Converting a irregular 2-dimensional list to MatrixTD");
        result = ScriptingUtils.getOutputByType(listTwoDimB, reqType, null, null, null);
        assertTrue(result instanceof MatrixTD);
        handleFailedConversion(listThreeDim, reqType, "Converting a 2+ dimensional list to MatrixTD");
        
        //Test SmallTableTD
        reqType = DataType.SmallTable;
        handleFailedConversion(listOneDim, reqType, "Converting a 1-dimensional list to SmallTableTD");
        handleFailedConversion(listTwoDimA, reqType, "Converting a irregular 2-dimensional list to SmallTableTD");
        result = ScriptingUtils.getOutputByType(listTwoDimB, reqType, null, null, null);
        assertTrue(result instanceof SmallTableTD);
        handleFailedConversion(listThreeDim, reqType, "Converting a 2+ dimensional list to SmallTableTD");
       
    }
    
    /**
     * 
     * Unit-Test for converting Java values to {@link VectorTD} elements.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionVectorTDelement() throws ComponentException {
        
        DataType reqType = DataType.Vector;
        
        final double[][] conversionResults =
        {{NO_VALUE, NO_VALUE},
         {26d, 0d, Integer.MAX_VALUE, Integer.MIN_VALUE},
         {26d, 0d, Long.MAX_VALUE, Long.MIN_VALUE},
         {26d, 0d, 9923372036854775807d},
         {26d, 26d, 0d, 0d, Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY},
         {26d, 26d, 0d, 0d, Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY},
         {NO_VALUE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, NO_VALUE, NO_VALUE,
          NO_VALUE, Double.NaN, Double.NaN, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE}};
        
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                
                ArrayList<Object> objList = new ArrayList<Object>();
                objList.add(TEST_DATA_TYPES[i][j]);
                
                if (conversionResults[i][j] != NO_VALUE) { 
                    
                    result = ScriptingUtils.getOutputByType(objList, reqType, null, null, null);
                    assertTrue(result instanceof VectorTD);
                    VectorTD convResult = (VectorTD) (result);
                    //NaN != NaN so need a special string-based check for conversion correctness.
                    if (String.valueOf(conversionResults[i][j]).equals(NAN)) {
                        
                        assertTrue("Conversion to VectorTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                            + " " + APOS + TEST_DATA_TYPES[i][j] + APOS + RESULT_STRING
                            + convResult.getFloatTDOfElement(0).getFloatValue(),
                            String.valueOf(convResult.getFloatTDOfElement(0).getFloatValue()).equals(NAN));
                    } else {
                        
                        assertTrue("Conversion to VectorTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                                   + " " + APOS + TEST_DATA_TYPES[i][j] + APOS + RESULT_STRING
                                   + convResult.getFloatTDOfElement(0).getFloatValue(),
                                   + convResult.getFloatTDOfElement(0).getFloatValue() == conversionResults[i][j]);
                    }
                } else {
                    String errorMsg = APOS + TEST_DATA_TYPES[i][j] +  APOS
                        + OF_CLASS + TEST_DATA_TYPES[i][j].getClass().getName();
                    handleFailedConversion(objList, reqType, errorMsg);
                }   
            }
        }   
    }
    
    /**
     * 
     * Unit-Test for converting Java values to {@link MatrixTD} elements.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionMatrixTDelement() throws ComponentException {
        
        
        DataType reqType = DataType.Matrix;
        
        final double[][] conversionResults =   
        {{NO_VALUE, NO_VALUE},
         {26d, 0d, Integer.MAX_VALUE, Integer.MIN_VALUE},
         {26d, 0d, Long.MAX_VALUE, Long.MIN_VALUE},
         {26d, 0d, 9923372036854775807d},
         {26d, 26d, 0d, 0d, Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY},
         {26d, 26d, 0d, 0d, Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY},
         {NO_VALUE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, NO_VALUE, NO_VALUE,
          NO_VALUE, Double.NaN, Double.NaN, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE}};
        
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                
                ArrayList<Object> rowList = new ArrayList<Object>();
                ArrayList<Object> elementList = new ArrayList<Object>();
                rowList.add(elementList);
                elementList.add(TEST_DATA_TYPES[i][j]);
                
                if (conversionResults[i][j] != NO_VALUE) { 
                    
                    result = ScriptingUtils.getOutputByType(rowList, reqType, null, null, null);
                    assertTrue(result instanceof MatrixTD);
                    MatrixTD convResult = (MatrixTD) (result);
                    //NaN != NaN so need a special string-based check for conversion correctness.
                    if (String.valueOf(conversionResults[i][j]).equals(NAN)) {
                        
                        assertTrue("Conversion to MatrixTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                            + " " + APOS + TEST_DATA_TYPES[i][j] + APOS + RESULT_STRING
                            + convResult.getFloatTDOfElement(0, 0).getFloatValue(),
                            String.valueOf(convResult.getFloatTDOfElement(0, 0).getFloatValue()).equals(NAN));
                    } else {
                        
                        assertTrue("Conversion to MatrixTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                                   + " " + APOS + TEST_DATA_TYPES[i][j] + APOS + RESULT_STRING
                                   + convResult.getFloatTDOfElement(0, 0).getFloatValue(),
                                   + convResult.getFloatTDOfElement(0, 0).getFloatValue() == conversionResults[i][j]);
                    }
                } else {
                    String errorMsg = APOS + TEST_DATA_TYPES[i][j] +  APOS
                        + OF_CLASS + TEST_DATA_TYPES[i][j].getClass().getName();
                    handleFailedConversion(rowList, reqType, errorMsg);
                }   
            }
        }   
    }
    
    /**
     * 
     * Unit-Test for converting Java values to {@link SmallTableTD} elements.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionSmallTableTDelement() throws ComponentException {
        
        
        DataType reqType = DataType.SmallTable;
        TypedDatumFactory factory = new TypedDatumFactoryDefaultStub();
        
        final TypedDatum[][] conversionResults =
        { {factory.createBoolean(false), factory.createBoolean(true)},
          {factory.createInteger(26), factory.createInteger(0),
           factory.createInteger(Integer.MAX_VALUE), factory.createInteger(Integer.MIN_VALUE)},
          {factory.createInteger(26L), factory.createInteger(0L),
           factory.createInteger(Long.MAX_VALUE), factory.createInteger(Long.MIN_VALUE)},
          {factory.createInteger(26), factory.createInteger(0),
           factory.createInteger(NO_VALUE)},
          {factory.createFloat(26f), factory.createFloat(26.0f), factory.createFloat(0.0f),
           factory.createFloat(0f), factory.createFloat(Float.MAX_VALUE),
           factory.createFloat(Float.MIN_VALUE), factory.createFloat(Float.NaN),
           factory.createFloat(Float.NEGATIVE_INFINITY), factory.createFloat(Float.POSITIVE_INFINITY)},
          {factory.createFloat(26.0), factory.createFloat(26d), factory.createFloat(0.0),
           factory.createFloat(0d), factory.createFloat(Double.MAX_VALUE),
           factory.createFloat(Double.MIN_VALUE),
           factory.createFloat(Double.NaN), factory.createFloat(Double.NEGATIVE_INFINITY),
           factory.createFloat(Double.POSITIVE_INFINITY)},
          {factory.createShortText("string"), factory.createFloat(Double.POSITIVE_INFINITY),
           factory.createFloat(Double.POSITIVE_INFINITY), factory.createFloat(Double.NEGATIVE_INFINITY),
           factory.createShortText("Inf"), factory.createShortText("+Inf"), factory.createShortText("-Inf"),
           factory.createFloat(Double.NaN), factory.createFloat(Double.NaN),
           factory.createBoolean(true), factory.createBoolean(false), factory.createShortText("none"),
           factory.createShortText("{}"), factory.createShortText("[]"), factory.createShortText("()"),
           factory.createShortText("")} };
        
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                
                List<Object> rowList = new ArrayList<Object>();
                List<Object> elementList = new ArrayList<Object>();
                rowList.add(elementList);
                elementList.add(TEST_DATA_TYPES[i][j]);
                
                if (!conversionResults[i][j].equals(factory.createInteger(NO_VALUE))) {
                    
                    result = ScriptingUtils.getOutputByType(rowList, reqType, null, null, null);
                    assertTrue(result instanceof SmallTableTD);
                    SmallTableTD convResult = (SmallTableTD) (result);
                    //NaN != NaN so need a special string-based check for conversion correctness.
                    if (String.valueOf(conversionResults[i][j]).equals(NAN)) {
                            
                        assertTrue("Conversion to SmallTableTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                            + " " + APOS + TEST_DATA_TYPES[i][j] + APOS + RESULT_STRING
                            + convResult.getTypedDatumOfCell(0, 0),
                            String.valueOf(convResult.getTypedDatumOfCell(0, 0)).equals(NAN));
                    } else {
                            
                        assertTrue("Conversion to SmallTableTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                                   + " " + APOS + TEST_DATA_TYPES[i][j] +  APOS + RESULT_STRING
                                   + convResult.getTypedDatumOfCell(0, 0),
                                   convResult.getTypedDatumOfCell(0, 0).equals(conversionResults[i][j]));
                    }
                } else {
                    String errorMsg = APOS + TEST_DATA_TYPES[i][j] +  APOS
                        + OF_CLASS + TEST_DATA_TYPES[i][j].getClass().getName();
                    handleFailedConversion(rowList, reqType, errorMsg);
                }
            }
        }   
    }
    
    
    /**
     * 
     * Unit-Test for converting Java values to {@link ShortTextTD}.
     * @throws ComponentException 
     * 
     */
    @Test
    public void testConversionShortTextTD() throws ComponentException {
        
        DataType reqType = DataType.ShortText;
       
        TypedDatum result;
        
        for (int i = 0; i < TEST_DATA_TYPES.length; i++) {
            for (int j = 0; j < TEST_DATA_TYPES[i].length; j++) {
                result = ScriptingUtils.getOutputByType(TEST_DATA_TYPES[i][j], reqType, null, null, null);
                assertTrue(result instanceof ShortTextTD);
                ShortTextTD convResult = (ShortTextTD) (result);
                assertTrue("Conversion to ShortTextTD is not correct for: " + TEST_DATA_TYPES[i][j].getClass().getName()
                           + " " + APOS + TEST_DATA_TYPES[i][j] +  APOS,
                            convResult.getShortTextValue().equals(TEST_DATA_TYPES[i][j].toString()));
            }
        }
       
    }
    
    
    
    /**
     * Test method getObjectOfEntryForPythonOrJython with several data types.
     */
    @Test
    public void testGetObjectOfEntryForPythonOrJython() {
        Object nullObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(null);
        Assert.assertEquals("None", nullObj);

        EmptyTD empty = EasyMock.createStrictMock(EmptyTD.class);
        EasyMock.expect(empty.getDataType()).andReturn(DataType.Empty).once();
        EasyMock.replay(empty);
        Object emptyObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(empty);
        Assert.assertEquals("None", emptyObj);

        BooleanTD booleanTDTrue = EasyMock.createStrictMock(BooleanTD.class);
        EasyMock.expect(booleanTDTrue.getDataType()).andReturn(DataType.Boolean).anyTimes();
        EasyMock.expect(booleanTDTrue.getBooleanValue()).andReturn(true).anyTimes();
        EasyMock.replay(booleanTDTrue);
        Object boolObjTrue = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(booleanTDTrue);
        Assert.assertEquals("True", boolObjTrue);

        BooleanTD booleanTDFalse = EasyMock.createStrictMock(BooleanTD.class);
        EasyMock.expect(booleanTDFalse.getDataType()).andReturn(DataType.Boolean).anyTimes();
        EasyMock.expect(booleanTDFalse.getBooleanValue()).andReturn(false).anyTimes();
        EasyMock.replay(booleanTDFalse);
        Object boolObjFalse = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(booleanTDFalse);
        Assert.assertEquals("False", boolObjFalse);

        FloatTD floatTD = EasyMock.createStrictMock(FloatTD.class);
        EasyMock.expect(floatTD.getDataType()).andReturn(DataType.Float).anyTimes();
        EasyMock.expect(floatTD.getFloatValue()).andReturn(1.0).anyTimes();
        EasyMock.replay(floatTD);
        Object floatObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(floatTD);
        Assert.assertEquals(1.0, floatObj);

        IntegerTD integerTD = EasyMock.createStrictMock(IntegerTD.class);
        EasyMock.expect(integerTD.getDataType()).andReturn(DataType.Integer).anyTimes();
        EasyMock.expect(integerTD.getIntValue()).andReturn(5L).anyTimes();
        EasyMock.replay(integerTD);
        Object intObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(integerTD);
        Assert.assertEquals(5L, intObj);

        ShortTextTD shorttextTD = EasyMock.createStrictMock(ShortTextTD.class);
        EasyMock.expect(shorttextTD.getDataType()).andReturn(DataType.ShortText).anyTimes();
        EasyMock.expect(shorttextTD.getShortTextValue()).andReturn("This is a test").anyTimes();
        EasyMock.replay(shorttextTD);
        Object shortObj = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(shorttextTD);
        Assert.assertEquals("This is a test", shortObj);

    }
}
