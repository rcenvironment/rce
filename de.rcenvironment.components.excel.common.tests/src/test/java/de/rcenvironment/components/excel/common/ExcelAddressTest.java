/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.common;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.Test;


/**
 * Test class for accessing specific addresses in Excel files.
 * 
 * @author Markus Kunde
 * @author Oliver Seebach
 * 
 */
public class ExcelAddressTest {

    private static final int S256 = 256;

    private static final int S65536 = 65536;

    private static final String A5 = "A5";

    private static final String A1 = "A1";

    private static final String TABELLE1 = "Tabelle1";

    private static final String I_TABELLE = "I_Tabelle";

    private static final String TABELLE1_5_10 = "Tabelle1!5:10";

    private static final String TABELLE1_5_5 = "Tabelle1!5:5";

    private static final String TABELLE1_A_D = "Tabelle1!$A:$D";

    private static final String TABELLE1_A_A = "Tabelle1!A:A";

    private static final String TABELLE1_A1_D5 = "Tabelle1!A1:D5";

    private static final String TABELLE1_A1 = "Tabelle1!A1";
    
    private static final String WRONGADDRESS = "Taelle2!A10:E15";
    private static final String WRONGADDRESS2 = "Tabelle1!A1:";

    private static final String EXCEPTION_ADDRESS_MSG = "Wrong address not recognized.";

    private static final String EXTERNAL_TEST_NOTEXCELFILE = "externalFiles/Feedback_Tixi.txt";

    private static final String EXTERNAL_TEST_EXCELFILE = "externalFiles/ExcelTester_Address.xls";

    private ExcelAddress valid1;

    private ExcelAddress valid2;

    private ExcelAddress valid3;

    private ExcelAddress valid4;

    private ExcelAddress valid5;

    private ExcelAddress valid6;

    private ExcelAddress valid7;

    /**
     * setUp().
     */
    @Before
    public void setUp() {
        try {
            valid1 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A1);
            valid2 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A1_D5);
            valid3 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A_A);
            valid4 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A_D);
            valid5 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_5_5);
            valid6 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_5_10);
            valid7 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), I_TABELLE);
        } catch (ExcelException e) {
            fail("Failed to set up Excel addresses");
        }
    }

    /**
     * testAddressFileString.
     * 
     */
    @Test
    public void testAddressFileString() {
        try {
            ExcelAddress addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A1);
            addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A1_D5);
            addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A_A);
            addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_A_D);
            addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_5_5);
            addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), TABELLE1_5_10);
            addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), I_TABELLE);
            assertTrue(addr != null);
        } catch (ExcelException e) {
            fail("Failed to get Excel addresses");
        }
    }
    
    /**
     * testWrongAddressFileString.
     * 
     */
    @Test(expected = ExcelException.class)
    public void testWrongAddressFileString() {
        ExcelAddress addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), WRONGADDRESS);
        assertTrue(addr != null);
    }
    
    /**
     * testWrongAddress2FileString.
     * 
     */
    @Test(expected = ExcelException.class)
    public void testWrongAddress2FileString() {
        ExcelAddress addr = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), WRONGADDRESS2);
        assertTrue(addr != null);
    }
    
    /**
     * testAddressExpander.
     * 
     */
    @Test
    public void testAddressExpander() {
        ExcelAddress validTemp = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), "Tabelle1!C5");
        ExcelAddress validTemp2 = new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), "Tabelle1!C5:F10");
        
        
        ExcelAddress test1 = ExcelAddress.getExcelAddressForTableRange(new File(EXTERNAL_TEST_EXCELFILE), valid1, 5, 3);
        
        assertEquals("Tabelle1!A1:C5", test1.getFullAddress());
        assertEquals(valid1.getWorkSheetName(), test1.getWorkSheetName());
        assertEquals(valid1.getFirstCell(), test1.getFirstCell());
        assertEquals("C5", test1.getLastCell());
        assertEquals(5, test1.getNumberOfRows());
        assertEquals(3, test1.getNumberOfColumns());
        assertEquals(valid1.getBeginningRowNumber(), test1.getBeginningRowNumber());
        assertEquals(valid1.getBeginningColumnNumber(), test1.getBeginningColumnNumber());
        assertEquals(null, test1.getUserDefinedName());
        
        
        ExcelAddress test2 = ExcelAddress.getExcelAddressForTableRange(new File(EXTERNAL_TEST_EXCELFILE), valid2, 8, 9);
        
        assertEquals("Tabelle1!A1:I8", test2.getFullAddress());
        assertEquals(valid2.getWorkSheetName(), test2.getWorkSheetName());
        assertEquals(valid2.getFirstCell(), test2.getFirstCell());
        assertEquals("I8", test2.getLastCell());
        assertEquals(8, test2.getNumberOfRows());
        assertEquals(9, test2.getNumberOfColumns());
        assertEquals(valid2.getBeginningRowNumber(), test2.getBeginningRowNumber());
        assertEquals(valid2.getBeginningColumnNumber(), test2.getBeginningColumnNumber());
        assertEquals(null, test2.getUserDefinedName());
        
        
        ExcelAddress test3 = ExcelAddress.getExcelAddressForTableRange(new File(EXTERNAL_TEST_EXCELFILE), valid2, 2, 3);
        
        assertEquals("Tabelle1!A1:C2", test3.getFullAddress());
        assertEquals(valid2.getWorkSheetName(), test3.getWorkSheetName());
        assertEquals(valid2.getFirstCell(), test3.getFirstCell());
        assertEquals("C2", test3.getLastCell());
        assertEquals(2, test3.getNumberOfRows());
        assertEquals(3, test3.getNumberOfColumns());
        assertEquals(valid2.getBeginningRowNumber(), test3.getBeginningRowNumber());
        assertEquals(valid2.getBeginningColumnNumber(), test3.getBeginningColumnNumber());
        assertEquals(null, test3.getUserDefinedName());
        

        ExcelAddress test4 = ExcelAddress.getExcelAddressForTableRange(new File(EXTERNAL_TEST_EXCELFILE), validTemp, 5, 3);
        
        assertEquals("Tabelle1!C5:E9", test4.getFullAddress());
        assertEquals(validTemp.getWorkSheetName(), test4.getWorkSheetName());
        assertEquals(validTemp.getFirstCell(), test4.getFirstCell());
        assertEquals("E9", test4.getLastCell());
        assertEquals(5, test4.getNumberOfRows());
        assertEquals(3, test4.getNumberOfColumns());
        assertEquals(validTemp.getBeginningRowNumber(), test4.getBeginningRowNumber());
        assertEquals(validTemp.getBeginningColumnNumber(), test4.getBeginningColumnNumber());
        assertEquals(null, test4.getUserDefinedName());
        

        ExcelAddress test5 = ExcelAddress.getExcelAddressForTableRange(new File(EXTERNAL_TEST_EXCELFILE), validTemp2, 8, 9);
        
        assertEquals("Tabelle1!C5:K12", test5.getFullAddress());
        assertEquals(validTemp2.getWorkSheetName(), test5.getWorkSheetName());
        assertEquals(validTemp2.getFirstCell(), test5.getFirstCell());
        assertEquals("K12", test5.getLastCell());
        assertEquals(8, test5.getNumberOfRows());
        assertEquals(9, test5.getNumberOfColumns());
        assertEquals(validTemp2.getBeginningRowNumber(), test5.getBeginningRowNumber());
        assertEquals(validTemp2.getBeginningColumnNumber(), test5.getBeginningColumnNumber());
        assertEquals(null, test5.getUserDefinedName());
        
        
        ExcelAddress test6 = ExcelAddress.getExcelAddressForTableRange(new File(EXTERNAL_TEST_EXCELFILE), validTemp2, 2, 3);
        
        assertEquals("Tabelle1!C5:E6", test6.getFullAddress());
        assertEquals(validTemp2.getWorkSheetName(), test6.getWorkSheetName());
        assertEquals(validTemp2.getFirstCell(), test6.getFirstCell());
        assertEquals("E6", test6.getLastCell());
        assertEquals(2, test6.getNumberOfRows());
        assertEquals(3, test6.getNumberOfColumns());
        assertEquals(validTemp2.getBeginningRowNumber(), test6.getBeginningRowNumber());
        assertEquals(validTemp2.getBeginningColumnNumber(), test6.getBeginningColumnNumber());
        assertEquals(null, test6.getUserDefinedName());
    }

    /**
     * testAddressFileStringWrongAddress.
     * 
     */
    @Test
    public void testAddressFileStringWrongAddress() {
        try {
            new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), "Tabelle1!A1s:D");
            fail(EXCEPTION_ADDRESS_MSG);
        } catch (ExcelException e) {
            assertTrue(true);
        }

        try {
            new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), "Tabelle1!A");
            fail(EXCEPTION_ADDRESS_MSG);
        } catch (ExcelException e) {
            assertTrue(true);
        }

        try {
            new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), "Tabelle1!5");
            fail(EXCEPTION_ADDRESS_MSG);
        } catch (ExcelException e) {
            assertTrue(true);
        }

        try {
            new ExcelAddress(new File(EXTERNAL_TEST_EXCELFILE), "A1:B5");
            fail(EXCEPTION_ADDRESS_MSG);
        } catch (ExcelException e) {
            assertTrue(true);
        }
    }

    /**
     * testAddressFileStringException1.
     * 
     */
    @Test
    public void testAddressFileStringException1() {
        try {
            new ExcelAddress(new File(EXTERNAL_TEST_NOTEXCELFILE), TABELLE1_A1_D5);
            fail("Wrong file not recognized.");
        } catch (ExcelException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                assertTrue(true);
            } else {
                fail("Unexpected cause for ExcelException. Found " + e.getCause().getClass() + " but expected IllegalArgumentException.");
            }
        }

    }

    /**
     * testGetFullAddress.
     * 
     */
    @Test
    public void testGetFullAddress() {
        assertEquals(TABELLE1_A1, valid1.getFullAddress());
        assertEquals(TABELLE1_A1_D5, valid2.getFullAddress());
        assertEquals("Tabelle1!A1:A65536", valid3.getFullAddress());
        assertEquals("Tabelle1!A1:D65536", valid4.getFullAddress());
        assertEquals("Tabelle1!A5:IV5", valid5.getFullAddress());
        assertEquals("Tabelle1!A5:IV10", valid6.getFullAddress());
        assertEquals(TABELLE1_A1_D5, valid7.getFullAddress());
    }

    /**
     * testGetUserDefinedName.
     * 
     */
    @Test
    public void testGetUserDefinedName() {
        assertEquals(null, valid1.getUserDefinedName());
        assertEquals(null, valid2.getUserDefinedName());
        assertEquals(null, valid3.getUserDefinedName());
        assertEquals(null, valid4.getUserDefinedName());
        assertEquals(null, valid5.getUserDefinedName());
        assertEquals(null, valid6.getUserDefinedName());
        assertEquals(I_TABELLE, valid7.getUserDefinedName());
    }

    /**
     * testGetWorkSheetName.
     * 
     */
    @Test
    public void testGetWorkSheetName() {
        assertEquals(TABELLE1, valid1.getWorkSheetName());
        assertEquals(TABELLE1, valid2.getWorkSheetName());
        assertEquals(TABELLE1, valid3.getWorkSheetName());
        assertEquals(TABELLE1, valid4.getWorkSheetName());
        assertEquals(TABELLE1, valid5.getWorkSheetName());
        assertEquals(TABELLE1, valid6.getWorkSheetName());
        assertEquals(TABELLE1, valid7.getWorkSheetName());
    }

    /**
     * testGetFirstCell.
     * 
     */
    @Test
    public void testGetFirstCell() {
        assertEquals(A1, valid1.getFirstCell());
        assertEquals(A1, valid2.getFirstCell());
        assertEquals(A1, valid3.getFirstCell());
        assertEquals(A1, valid4.getFirstCell());
        assertEquals(A5, valid5.getFirstCell());
        assertEquals(A5, valid6.getFirstCell());
        assertEquals(A1, valid7.getFirstCell());
    }

    /**
     * testGetLastCell.
     * 
     */
    @Test
    public void testGetLastCell() {
        assertEquals(A1, valid1.getLastCell());
        assertEquals("D5", valid2.getLastCell());
        assertEquals("A65536", valid3.getLastCell());
        assertEquals("D65536", valid4.getLastCell());
        assertEquals("IV5", valid5.getLastCell());
        assertEquals("IV10", valid6.getLastCell());
        assertEquals("D5", valid7.getLastCell());
    }

    /**
     * testGetNumberOfRows.
     * 
     */
    @Test
    public void testGetNumberOfRows() {
        assertEquals(1, valid1.getNumberOfRows());
        assertEquals(5, valid2.getNumberOfRows());
        assertEquals(S65536, valid3.getNumberOfRows());
        assertEquals(S65536, valid4.getNumberOfRows());
        assertEquals(1, valid5.getNumberOfRows());
        assertEquals(6, valid6.getNumberOfRows());
        assertEquals(5, valid7.getNumberOfRows());
    }

    /**
     * testGetNumberOfColumns.
     * 
     */
    @Test
    public void testGetNumberOfColumns() {
        assertEquals(1, valid1.getNumberOfColumns());
        assertEquals(4, valid2.getNumberOfColumns());
        assertEquals(1, valid3.getNumberOfColumns());
        assertEquals(4, valid4.getNumberOfColumns());
        assertEquals(S256, valid5.getNumberOfColumns());
        assertEquals(S256, valid6.getNumberOfColumns());
        assertEquals(4, valid7.getNumberOfColumns());
    }

    /**
     * testGetBeginningRowNumber.
     * 
     */
    @Test
    public void testGetBeginningRowNumber() {
        assertEquals(1, valid1.getBeginningRowNumber());
        assertEquals(1, valid2.getBeginningRowNumber());
        assertEquals(1, valid3.getBeginningRowNumber());
        assertEquals(1, valid4.getBeginningRowNumber());
        assertEquals(5, valid5.getBeginningRowNumber());
        assertEquals(5, valid6.getBeginningRowNumber());
        assertEquals(1, valid7.getBeginningRowNumber());
    }

    /**
     * testGetBeginningColumnNumber.
     * 
     */
    @Test
    public void testGetBeginningColumnNumber() {
        assertEquals(1, valid1.getBeginningColumnNumber());
        assertEquals(1, valid2.getBeginningColumnNumber());
        assertEquals(1, valid3.getBeginningColumnNumber());
        assertEquals(1, valid4.getBeginningColumnNumber());
        assertEquals(1, valid5.getBeginningColumnNumber());
        assertEquals(1, valid6.getBeginningColumnNumber());
        assertEquals(1, valid7.getBeginningColumnNumber());
    }
}
