/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.excel.legacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;

/**
 * Exports values to Excel files.
 *
 * @author Markus Kunde
 */
@Deprecated
public final class ExcelFileExporter {
    
    /** File Extension of Excel-files. */
    public static final String FILEEXTENSION_XL2003 = "xls";
    
    /** File Extension of Excel-files. */
    public static final String FILEEXTENSION_XL2010 = "xlsx";
    
    private static final Log LOGGER = LogFactory.getLog(ExcelFileExporter.class);
    
    private static final int BLOCKING_ITERATIONMAX = 600; //Only temporarily
    private static final int BLOCKING_SLEEP = 50; //Only temporarily
    
    private ExcelFileExporter() {}
    
    /**
     * Exports values to an Excel file.
     * 
     * @param xlFile File where values should be exported to. Creating new or overwrite existing file. 
     *               File-extension should be "*.xls" or "*.xlsx"
     * @param values values representing as a 2d array (table format)
     */
    public static void exportValuesToExcelFile(final File xlFile,  final TypedValue[][] values) {
        if (xlFile != null && values != null) {
            // Reads with POI
            FileOutputStream fileOut = null;
            try {

                org.apache.poi.ss.usermodel.Workbook wb;
                if (xlFile.getName().endsWith(FILEEXTENSION_XL2003)) {
                    wb = new HSSFWorkbook();
                } else {
                    wb = new XSSFWorkbook();
                }
                Sheet sheet = wb.createSheet();
                

                
                int noOfRows = values.length;
                int beginningRowNo = 0;
                int beginningColumnNo = 0;

                for (int row = beginningRowNo; row < beginningRowNo + noOfRows; row++) {
                    Row r = sheet.getRow(row);
                    if (r == null) {
                        r = sheet.createRow(row);
                    }
                    for (int col = beginningColumnNo; col < beginningColumnNo + values[row - beginningRowNo].length; 
                        col++) {                        
                        Cell cell = r.createCell(col);
                        TypedValue bv = values[row - beginningRowNo][col - beginningColumnNo];

                        switch (bv.getType()) {
                        case String:
                            cell.setCellType(Cell.CELL_TYPE_STRING);
                            cell.setCellValue(bv.getStringValue());
                            break;
                        case Integer:
                            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                            cell.setCellValue(bv.getIntegerValue());
                            break;
                        case Logic:
                            cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
                            cell.setCellValue(bv.getLogicValue());
                            break;
                        case Real:
                            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                            cell.setCellValue(bv.getRealValue());
                            break;
                        case Empty:
                            cell.setCellType(Cell.CELL_TYPE_BLANK);
                        default:
                            break;
                        }
                    }
                } 
                
                /* 
                 * Solves temporarily the problem with reading from I-Stream and 
                 * writing to O-Stream with the same file handle. 
                 * Causes sometimes exceptions if I-Stream is blocked when trying 
                 * to write. Should be reported to Apache POI.
                 */
                for (int i = 0; i < BLOCKING_ITERATIONMAX; i++) {
                    try {
                        if (xlFile != null) {
                            //Write to file
                            fileOut = new FileOutputStream(xlFile);
                            wb.write(fileOut);
                            break;
                        }
                    } catch (FileNotFoundException e) {
                        LOGGER.debug("Apache Poi: File not found. (Method: setValueOfCells). Iteration: " + i + ". Retrying.");
                        if (i == (BLOCKING_ITERATIONMAX - 1)) {
                            //Last iteration was not successful
                            LOGGER.error("Apache Poi: Cannot save file with result data.");
                            throw e;
                        }
                    }
                    try {
                        Thread.sleep(BLOCKING_SLEEP);
                    } catch (InterruptedException e) {
                        LOGGER.error(e.getStackTrace());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Apache Poi: IO error. (Method: setValueOfCells)");
            } finally {
                if (fileOut != null) {
                    try {
                        fileOut.flush();
                        fileOut.close();
                    } catch (IOException e) {
                        LOGGER.debug("Apache Poi: Closing of output stream does not work. (Method: setValueOfCells)");
                    }
                }
            }
        }
        
    }

}
