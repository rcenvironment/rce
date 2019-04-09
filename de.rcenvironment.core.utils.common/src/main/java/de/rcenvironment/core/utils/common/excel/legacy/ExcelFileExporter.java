/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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

    private ExcelFileExporter() {}

    /**
     * Exports values to an Excel file.
     * 
     * @param xlFile File where values should be exported to. Creating new or overwrite existing
     *        file. File-extension should be "*.xls" or "*.xlsx"
     * @param values values representing as a 2d array (table format)
     * @return true, if writing data file was successful.
     * @throws FileNotFoundException from Apache.
     */
    public static boolean exportValuesToExcelFile(final File xlFile, final TypedValue[][] values) throws FileNotFoundException{
        boolean success = true;
        if (xlFile != null && values != null) {
            // Reads with POI
        
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
                for (int col = beginningColumnNo; col < beginningColumnNo + values[row - beginningRowNo].length; col++) {
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
             * Solves temporarily the problem with reading from I-Stream and writing to O-Stream
             * with the same file handle. Causes sometimes exceptions if I-Stream is blocked
             * when trying to write. Should be reported to Apache POI.
             */
            FileOutputStream fileOutStream = null;
            try {
                fileOutStream = new FileOutputStream(xlFile);
                
                wb.write(fileOutStream);
                fileOutStream.flush();
                fileOutStream.close();
                
                success = true;
            } catch (FileNotFoundException e) {
                if (fileOutStream != null) {
                    try {
                        fileOutStream.flush();
                        fileOutStream.close();
                    } catch (IOException e1) {
                        LOGGER.error("Apache Poi: IO error. (Method: setValueOfCells)");
                        success = false;
                    }
                }
                throw e;
            } catch (IOException e) {
                LOGGER.error("Apache Poi: IO error. (Method: setValueOfCells)");
                success = false;
                
            } finally {
                try {
                    if (fileOutStream != null) {
                        fileOutStream.flush();
                        fileOutStream.close();
                    }
                } catch (IOException e) {
                    LOGGER.debug("Apache Poi: Closing of output stream does not work. (Method: setValueOfCells)");
                    success = false;
                }
            }
        }
        return success;
    }
}
