/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.components.excel.common.internal.ExcelServiceOLE;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.testutils.TypedDatumFactoryDefaultStub;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.rce.components.excel.commons.ExcelAddress;


/**
 * Test class for ExcelFile.
 * 
 *  
 * Unit test (not unit plugin/osgi test) needs jacob dll libs in java library path. 
 * To execute this test as a usual unit test, please add vm-argument with path to jacob dlls.
 * -Djava.library.path=${workspace_loc:de.rcenvironment.libraries.custom.win32}/native
 * 
 *
 * @author Markus Kunde
 * @author Oliver Seebach
 */
public class ExcelServiceOLETest {

    private static final double DELTA = (1/10);
    
    private static final int S20_RESULT = 20;

    private static final int S15_RESULT = 15;

    private static final int S30_RESULT = 30;

    private static final String EXTERNAL_TEST_EXCELFILE = "externalFiles/ExcelTester_OLE.xls";

    private static final String EXTERNAL_TEMP_EXCELFILE = "externalFiles/ExcelTesterTemp.xls";
    
    private ExcelService excelService;
    
    private File xlFile = new File(EXTERNAL_TEMP_EXCELFILE);
    
    private TypedDatumFactory typedDatumFactory = new TypedDatumFactoryDefaultStub();
        
    /**
     * SetUp method.
     * @throws Exception errors
     * 
     */
    @Before
    public void setUp() throws Exception {
        
        FileUtils.copyFile(new File(EXTERNAL_TEST_EXCELFILE), new File(EXTERNAL_TEMP_EXCELFILE));
        excelService = new ExcelServiceOLE(typedDatumFactory);  
    }
    
    /**
     * Deletes temp Excel file.
     * 
     * @throws java.lang.Exception if something goes wrong
     */
    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File(EXTERNAL_TEMP_EXCELFILE));
    }

    /**
     * Get macros test.
     * 
     */
    @Test
    public void testGetMacrosOLE() {               
        String[] macros = excelService.getMacros(xlFile);      
        assertEquals(1, macros.length);       
        assertEquals("Modul1.Makro1", macros[0]);
    }

    /**
     * run macros test.
     * @throws IOException io error
     * @throws IllegalArgumentException illegal argument
     * @throws InvalidFormatException  invalid format
     * 
     */
    @Test
    public void testRunMacrosOLE() throws InvalidFormatException, IllegalArgumentException, IOException {        
        assertEquals(DataType.Empty, 
            excelService.getValueOfCells(xlFile, new ExcelAddress(xlFile, "Tabelle1!C8")).getTypedDatumOfCell(0, 0).getDataType());
        
        excelService.runMacro(xlFile, "Modul1.Makro1");
        
        
        assertEquals(S30_RESULT,
            ((FloatTD) excelService.getValueOfCells(xlFile, new ExcelAddress(xlFile, "Tabelle1!C8")).getTypedDatumOfCell(0, 0))
                .getFloatValue(), DELTA);
    }

    /**
     * recalculate formulas test.
     * @throws IOException io error
     * @throws IllegalArgumentException illegal argument
     * @throws InvalidFormatException invalid format
     * 
     */
    @Test
    public void testRecalculateFormulasOLE() throws InvalidFormatException, IllegalArgumentException, IOException {
        assertEquals(S15_RESULT,
            ((FloatTD) excelService.getValueOfCells(xlFile, new ExcelAddress(xlFile, "Tabelle1!A8")).getTypedDatumOfCell(0, 0))
                .getFloatValue(), DELTA);
    
        SmallTableTD values = typedDatumFactory.createSmallTable(1, 1);
        values.setTypedDatumForCell(typedDatumFactory.createInteger(6), 0, 0);
        excelService.setValues(xlFile, new ExcelAddress(xlFile, "Tabelle1!A1"), values);
        
        excelService.recalculateFormulas(xlFile);

        assertEquals(S20_RESULT, ((FloatTD) excelService.getValueOfCells(xlFile, new ExcelAddress(xlFile, "Tabelle1!A8"))
                    .getTypedDatumOfCell(0, 0)).getFloatValue(), DELTA);
    }

}
