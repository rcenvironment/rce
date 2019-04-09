/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.excel.common;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.excel.common.internal.ExcelServicePOI;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.testutils.TypedDatumFactoryDefaultStub;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;

/**
 * Test class for ExcelFile.
 * 
 * @author Markus Kunde
 * @author Oliver Seebach
 */
public class ExcelServicePOITest {
    
    private static final double DELTA = (1/10);

    private static final int S15_VAL = 15;

    private static final int S123_VAL = 123;

    private static final double S123_45_VAL = 123.45;

    private static final String TEST_STRING = "Test String";

    private static final String EXTERNAL_TEST_EXCELFILE = "externalFiles/ExcelTester_POI.xls";
    
    private static final String EXTERNAL_TEST_EXCELFILE_NOTIMPL = "externalFiles/NotImplementedTest.xls";
    
    private static final String EXTERNAL_TEST_EXCELFILE_NOTIMPL_TEMP = "externalFiles/NotImplementedTest_temp.xls";

    private static final String EXTERNAL_TEMP_EXCELFILE = "externalFiles/ExcelTesterTemp.xls";
    
    private static final String EXTERNAL_TEMP2_EXCELFILE = "externalFiles/ExcelTesterTemp2.xls";

    private static final String EXTERNAL_TEST_NOTEXCELFILE = "externalFiles/Feedback_Tixi.txt";

    private ExcelService excelService;
    
    private File xlFile = new File(EXTERNAL_TEMP_EXCELFILE);
    
    private ExcelAddress addr;
    
    private SmallTableTD values;
    
    private TypedDatumFactory typedDatumFactory = new TypedDatumFactoryDefaultStub();

    /**
     * Creates temp Excel file.
     * 
     * @throws java.lang.Exception if something goes wrong
     */
    @Before
    public void setUp() throws Exception {
        FileUtils.copyFile(new File(EXTERNAL_TEST_EXCELFILE), new File(EXTERNAL_TEMP_EXCELFILE));
        excelService = new ExcelServicePOI(typedDatumFactory);
        
        addr = new ExcelAddress(new File(EXTERNAL_TEMP_EXCELFILE), "Tabelle1!A1:D5");

        values = typedDatumFactory.createSmallTable(2, 3);   
        values.setTypedDatumForCell(typedDatumFactory.createShortText(TEST_STRING), 0, 0);
        values.setTypedDatumForCell(typedDatumFactory.createFloat(S123_45_VAL), 0, 1);
        values.setTypedDatumForCell(typedDatumFactory.createBoolean(true), 0, 2);
        values.setTypedDatumForCell(typedDatumFactory.createDateTime(0), 1, 0);
        values.setTypedDatumForCell(typedDatumFactory.createEmpty(), 1, 1);
        values.setTypedDatumForCell(typedDatumFactory.createInteger(S123_VAL), 1, 2);
    }

    /**
     * Deletes temp Excel file.
     * 
     * @throws java.lang.Exception if something goes wrong
     */
    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File(EXTERNAL_TEMP_EXCELFILE));
        FileUtils.deleteQuietly(new File(EXTERNAL_TEMP2_EXCELFILE));
        FileUtils.deleteQuietly(new File(EXTERNAL_TEST_EXCELFILE_NOTIMPL_TEMP));
    }

    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#ExcelFile(java.io.File)}.
     */
    @Test
    public void testExcelFile() {
        excelService = new ExcelServicePOI(typedDatumFactory);
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#ExcelFile(java.io.File)}.
     */
    @Test(expected = ExcelException.class)
    public void testExcelFileWrongFile() {
        excelService = new ExcelServicePOI(typedDatumFactory);
        excelService.getUserDefinedCellNames(new File(EXTERNAL_TEST_NOTEXCELFILE));
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#ExcelFile(java.io.File)}.
     */
    @Test(expected = ExcelException.class)
    public void testGetMacros() {
        excelService = new ExcelServicePOI(typedDatumFactory);
        excelService.getMacros(xlFile);
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#ExcelFile(java.io.File)}.
     */
    @Test(expected = ExcelException.class)
    public void testRunMacro() {
        excelService = new ExcelServicePOI(typedDatumFactory);
        excelService.runMacro(xlFile, TEST_STRING);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI
     * #setValues(de.rcenvironment.components.excel.common.ExcelAddress, de.rcenvironment.rce.component.datatype.ITable)}.
     */
    @Test
    public void testSetValuesExcelAddressITable() {
        excelService.setValues(xlFile, addr, values);
        
        SmallTableTD table = excelService.getValueOfCells(xlFile, addr);
        
        assertEquals(TEST_STRING, ((ShortTextTD) table.getTypedDatumOfCell(0, 0)).getShortTextValue());
        assertEquals(S123_VAL, ((IntegerTD) table.getTypedDatumOfCell(1, 2)).getIntValue(), DELTA);
        assertEquals(S123_45_VAL, ((FloatTD) table.getTypedDatumOfCell(0, 1)).getFloatValue(), DELTA);
        assertEquals("dolor", ((ShortTextTD) table.getTypedDatumOfCell(2, 2)).getShortTextValue());
        assertEquals(4, table.getRowCount() - 1);
        assertEquals(3, table.getColumnCount() - 1);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI
     * #setValues(de.rcenvironment.components.excel.common.ExcelAddress, java.io.File, de.rcenvironment.rce.component.datatype.ITable)}
     * .
     */
    @Test
    public void testSetValuesExcelAddressFileITable() {
        File xlFile2 = new File(EXTERNAL_TEMP2_EXCELFILE); 
        excelService.setValues(xlFile, xlFile2, addr, values);        
        ExcelServicePOI excelFile2 = new ExcelServicePOI(typedDatumFactory);

        ShortTextTD st =
            (ShortTextTD) excelFile2.getValueOfCells(xlFile2, new ExcelAddress(xlFile2, "Tabelle1!A1")).getTypedDatumOfCell(0, 0);
        assertEquals(TEST_STRING, st.getShortTextValue());
        
        IntegerTD integer =
            (IntegerTD) excelFile2.getValueOfCells(xlFile2, new ExcelAddress(xlFile2, "Tabelle1!A2")).getTypedDatumOfCell(0, 0);
        assertEquals(0, integer.getIntValue(), DELTA);
        
        FloatTD floatTD =
            (FloatTD) excelFile2.getValueOfCells(xlFile2, new ExcelAddress(xlFile2, "Tabelle1!B1")).getTypedDatumOfCell(0, 0);
        assertEquals(S123_45_VAL, floatTD.getFloatValue(), DELTA);

        BooleanTD booleanTD =
            (BooleanTD) excelFile2.getValueOfCells(xlFile2, new ExcelAddress(xlFile2, "Tabelle1!C1")).getTypedDatumOfCell(0, 0);
        assertEquals(true, booleanTD.getBooleanValue());
        
    }

    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI
     * #getValueOfCells(de.rcenvironment.components.excel.common.ExcelAddress)}
     * .
     * @throws IOException io error
     * @throws IllegalArgumentException illegal argument
     * @throws InvalidFormatException invalid format
     */
    @Test
    public void testGetValueOfCells() throws InvalidFormatException, IllegalArgumentException, IOException {
        SmallTableTD vals = excelService.getValueOfCells(xlFile, new ExcelAddress(xlFile, "Tabelle1!A1:D8"));
        
        assertEquals(1.0, ((IntegerTD) vals.getTypedDatumOfCell(0, 1)).getIntValue(), DELTA);
        assertEquals("x", ((ShortTextTD) vals.getTypedDatumOfCell(0, 3)).getShortTextValue());
        assertEquals(S15_VAL, ((FloatTD) vals.getTypedDatumOfCell(7, 0)).getFloatValue(), DELTA);
        assertEquals(DataType.Empty, vals.getTypedDatumOfCell(7, 3).getDataType()); 
    }
    
    /**
     * Test if access to table outside of defined range throws an exception.
     * 
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testIndexOutOfBoundsException() {
        SmallTableTD vals = excelService.getValueOfCells(xlFile, new ExcelAddress(xlFile, "Tabelle1!A1:D8"));
        vals.getTypedDatumOfCell(8, 4);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#getUserDefinedCellNames()}.
     */
    @Test
    public void testGetUserDefinedCellNames() {        
        List<String> usernamesList = Arrays.asList("I_einzel", "I_Tabelle", "O_Ausgang", "O_MakroAusgang");
        
        assertEquals(usernamesList.size(), excelService.getUserDefinedCellNames(xlFile).length);
        
        for (ExcelAddress address: excelService.getUserDefinedCellNames(xlFile)) {
            assertTrue(usernamesList.contains(address.getUserDefinedName()));
        }
        
    }
    
    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#recalculateFormulas()}.
     */
    @Test
    public void testrecalculateFormulas() {        
        excelService.recalculateFormulas(xlFile);        
    }
    
    
    /**
     * Test method for
     * {@link de.rcenvironment.rce.components.excel.commons.internal.ExcelServicePOI#recalculateFormulas()}.
     */
    @Test(expected = ExcelException.class)
    public void testrecalculateFormulasNotImpl() {
        try {
            FileUtils.copyFile(new File(EXTERNAL_TEST_EXCELFILE_NOTIMPL), new File(EXTERNAL_TEST_EXCELFILE_NOTIMPL_TEMP));
        } catch (IOException e) {
            Assert.fail();
        }
        excelService.recalculateFormulas(new File(EXTERNAL_TEST_EXCELFILE_NOTIMPL_TEMP));
    }
}
