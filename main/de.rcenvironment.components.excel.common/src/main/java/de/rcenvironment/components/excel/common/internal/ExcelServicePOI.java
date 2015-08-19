/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.excel.common.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import de.rcenvironment.components.excel.common.ExcelException;
import de.rcenvironment.components.excel.common.ExcelService;
import de.rcenvironment.components.excel.common.GarbageDestroyer;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.rce.components.excel.commons.ExcelAddress;


/**
 * Excel file representation with access to its data.
 * 
 * @author Markus Kunde
 */
public class ExcelServicePOI implements ExcelService {

    protected static final Log LOGGER = LogFactory.getLog(ExcelServicePOI.class);
    
    protected static final int BLOCKING_ITERATIONMAX = 600; // regarding POI interface unstable behavior

    protected static final int BLOCKING_SLEEP = 50; // regarding POI interface unstable behavior
    
    
    /* Exception messages. */
    private static final String EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED = "Excel file is not found or cannot be opened.";

    private static final String EXCMSG_EXCEL_FILE_HAS_AN_INVALID_FORMAT = "Excel file has an invalid format.";

    private static final String EXCMSG_EXCEL_FILE_CANNOT_NOT_FOUND = "Excel file cannot not found.";
    
    private static final String EXCMSG_CANNOT_SAVE_FILE_WITH_RESULT_DATA = "Cannot save file with result data.";
   
    private static final String EXCMSG_EXCEL_FILE_CANNOT_CLOSED = "Excel file access cannot be closed.";
    
    private static TypedDatumFactory typedDatumFactory;
    

    /**
     * Default Constructor.
     * 
     */
    public ExcelServicePOI() {}
    
    /**
     * Constructor to get typedDatumFactory not from RCE-service into ExcelService class.
     * 
     * @param typedDatumFactory the typed datum factory
     */
    public ExcelServicePOI(final TypedDatumFactory typedDatumFactory) {
        ExcelServicePOI.typedDatumFactory = typedDatumFactory;
    }
    
    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumFactory = newTypedDatumService.getFactory();
    }
  
    protected void unbindTypedDatumService(TypedDatumService oldTypedDatumService) {}
    
    /**
     * Just a simple test method if Excel file is really an Excel file.
     * 
     * @param xlFile Excel file
     * @throws ExcelException thrown if not a real Excel file
     */
    protected void initialTest(final File xlFile) throws ExcelException {        
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(xlFile));
            boolean isXlsx = POIXMLDocument.hasOOXMLHeader(bis);            
            bis.reset();
            boolean isXls = POIFSFileSystem.hasPOIFSHeader(bis);
            
            if (!isXlsx && !isXls) {
                throw new ExcelException("Excel file is maybe no Excel file?");
            }
        } catch (FileNotFoundException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            throw new ExcelException("Excel file is maybe no Excel file?", e);
        } catch (IOException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED, e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }
    }
    
    
    
    @Override
    public void setValues(File xlFile, ExcelAddress addr, SmallTableTD values) throws ExcelException {
        setValues(xlFile, xlFile, addr, values);
    }

    
    
    @Override
    public void setValues(File xlFile, File newFile, ExcelAddress addr, SmallTableTD values) throws ExcelException {
        try {
            if (xlFile != null) {
                InputStream inp = null;
                FileOutputStream fileOut = null;
                inp = new FileInputStream(xlFile);

                try {
                    // Setting values in Excel file 
                    org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create(inp);
                    Sheet sheet = wb.getSheet(addr.getWorkSheetName());

                    int addressRowCorrection = addr.getBeginningRowNumber() - 1; // Excel address is
                                                                                 // 1-based while POI is
                                                                                 // 0-based
                    int addressColumnCorrection = addr.getBeginningColumnNumber() - 1; // Excel address
                                                                                       // is 1-based
                                                                                       // while POI is
                                                                                       // 0-based

                    for (int row = addressRowCorrection; row < addressRowCorrection + addr.getNumberOfRows()
                        && row <= addressRowCorrection + (values.getRowCount() - 1); row++) {
                        Row r = sheet.getRow(row);
                        if (r == null) {
                            r = sheet.createRow(row);
                        }
                        for (int col = addressColumnCorrection; col < addressColumnCorrection + addr.getNumberOfColumns()
                            && col <= addressColumnCorrection + (values.getColumnCount() - 1); col++) {
                            Cell cell = r.createCell(col);
                            TypedDatum data = values.getTypedDatumOfCell(row - addressRowCorrection, col - addressColumnCorrection);

                            if (data == null) {
                                continue;
                            }

                            switch (data.getDataType()) {
                            case ShortText:
                                cell.setCellType(Cell.CELL_TYPE_STRING);
                                cell.setCellValue(((ShortTextTD) data).getShortTextValue());
                                break;
                            case Float:
                                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                                cell.setCellValue(((FloatTD) data).getFloatValue());
                                break;
                            case Integer:
                                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                                cell.setCellValue(((IntegerTD) data).getIntValue());
                                break;
                            case Boolean:
                                cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
                                cell.setCellValue(((BooleanTD) data).getBooleanValue());
                                break;
                            case DateTime:
                                cell.setCellType(Cell.CELL_TYPE_STRING);
                                cell.setCellValue(((DateTimeTD) data).getDateTime().getTime());
                                break;
                            case Empty:
                                cell.setCellType(Cell.CELL_TYPE_BLANK);
                                break;
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
                    for (int i = 0; i < BLOCKING_ITERATIONMAX; i++) {
                        try {
                            if (newFile != null) {
                                // Write to file
                                fileOut = new FileOutputStream(newFile);
                                wb.write(fileOut);
                                break;
                            }
                        } catch (FileNotFoundException e) {
                            LOGGER.debug("File not found. (Method: setValueOfCells). Iteration: " + i + ". Retrying.");
                            if (i == (BLOCKING_ITERATIONMAX - 1)) {
                                // Last iteration was not successful
                                LOGGER.error(EXCMSG_CANNOT_SAVE_FILE_WITH_RESULT_DATA);
                                throw new ExcelException(EXCMSG_CANNOT_SAVE_FILE_WITH_RESULT_DATA, e);
                            }
                        } catch (IOException e) {
                            throw new ExcelException(EXCMSG_CANNOT_SAVE_FILE_WITH_RESULT_DATA, e);
                        }
                        try {
                            Thread.sleep(BLOCKING_SLEEP);
                        } catch (InterruptedException e) {
                            LOGGER.error(e);
                        }
                    }
                } finally {
                    if (inp != null) {
                        try {
                            inp.close();
                        } catch (IOException e) {
                            LOGGER.debug("Apache Poi: Closing of input stream does not work. (Method: setValueOfCells)");
                        }
                    }
                    if (fileOut != null) {
                        try {
                            fileOut.flush();
                            fileOut.close();
                        } catch (IOException e) {
                            LOGGER.debug("Apache Poi: Closing of output stream does not work. (Method: setValueOfCells)");
                        }
                    }
                    
                    //Not nice, but workbook-object will not released.
                    new Thread(new GarbageDestroyer()).start();
                }

                //Recalculate formulas 
                recalculateFormulas(xlFile);
            }
        } catch (FileNotFoundException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_NOT_FOUND, e);
        } catch (InvalidFormatException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_HAS_AN_INVALID_FORMAT, e);
        } catch (IOException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED, e);
        }
    }

    
    
    @Override
    public SmallTableTD getValueOfCells(File xlFile, ExcelAddress addr) throws ExcelException {
        // recalculate Formulas
        recalculateFormulas(xlFile);

        // Read with POI
        SmallTableTD retValues = null;

        if (xlFile != null) {
            // Reads with POI
            InputStream inp = null;
            try {
                inp = new FileInputStream(xlFile);

                org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create(inp);
                Sheet sheet = wb.getSheet(addr.getWorkSheetName());

                retValues = typedDatumFactory.createSmallTable(addr.getNumberOfRows(), addr.getNumberOfColumns());

                int addressRowCorrection = addr.getBeginningRowNumber() - 1; // Excel address is 1-based while POI is 0-based
                int addressColumnCorrection = addr.getBeginningColumnNumber() - 1; // Excel address is 1-based
                                                                                   // while POI is 0-based

                for (int row = addressRowCorrection; row < addressRowCorrection + addr.getNumberOfRows(); row++) {
                    Row r = sheet.getRow(row);
                    if (r == null) {
                        r = sheet.createRow(row);
                    }
                    for (int col = addressColumnCorrection; col < addressColumnCorrection + addr.getNumberOfColumns(); col++) {
                        Cell cell = r.getCell(col);
                        TypedDatum data;

                        // If cell is empty
                        if (cell == null) {
                            data = typedDatumFactory.createEmpty();
                            retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                            continue;
                        }

                        switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            data = typedDatumFactory.createShortText(cell.getRichStringCellValue().getString());
                            retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                data = typedDatumFactory.createDateTime(cell.getDateCellValue().getTime());
                                retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                            } else {
                                double rawNumber = cell.getNumericCellValue();
                                long lRawNumber = (long) rawNumber; 
                                if ((rawNumber - lRawNumber) == 0) { // For discovering if value may be of type 'long'
                                    data = typedDatumFactory.createInteger(lRawNumber);
                                } else {
                                    data = typedDatumFactory.createFloat(rawNumber);
                                }
                                retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                            }
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                            data = typedDatumFactory.createBoolean(cell.getBooleanCellValue());
                            retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                            break;
                        case Cell.CELL_TYPE_FORMULA:
                            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                            try {
                                CellValue cellValue = evaluator.evaluate(cell);
                                switch (cellValue.getCellType()) {
                                case Cell.CELL_TYPE_BOOLEAN:
                                    data = typedDatumFactory.createBoolean(cellValue.getBooleanValue());                                  
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                case Cell.CELL_TYPE_NUMERIC:
                                    data = typedDatumFactory.createFloat(cellValue.getNumberValue());
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                case Cell.CELL_TYPE_STRING:
                                    data = typedDatumFactory.createShortText(cellValue.getStringValue());
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                default:
                                    data = typedDatumFactory.createEmpty();
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                }
                            } catch (org.apache.poi.ss.formula.eval.NotImplementedException e) {
                                // In case evaluator.evalualte(cell) does not implement a specific formula
                                switch (cell.getCachedFormulaResultType()) {
                                case Cell.CELL_TYPE_BOOLEAN:
                                    data = typedDatumFactory.createBoolean(cell.getBooleanCellValue());
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                case Cell.CELL_TYPE_NUMERIC:
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        data = typedDatumFactory.createDateTime(cell.getDateCellValue().getTime());
                                        retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    } else {
                                        double rawNumber = cell.getNumericCellValue();
                                        long lRawNumber = (long) rawNumber; 
                                        if ((rawNumber - lRawNumber) == 0) { // For discovering if value may be of type 'long'
                                            data = typedDatumFactory.createInteger(lRawNumber);
                                        } else {
                                            data = typedDatumFactory.createFloat(rawNumber);
                                        }
                                        retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    }
                                    break;
                                case Cell.CELL_TYPE_STRING:
                                    data = typedDatumFactory.createShortText(cell.getStringCellValue());
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                default:
                                    data = typedDatumFactory.createEmpty();
                                    retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                                    break;
                                }
                            }
                            break;
                        default:
                            data = typedDatumFactory.createEmpty();
                            retValues.setTypedDatumForCell(data, row - addressRowCorrection, col - addressColumnCorrection);
                            break;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_NOT_FOUND, e);
            } catch (InvalidFormatException e) {
                throw new ExcelException(EXCMSG_EXCEL_FILE_HAS_AN_INVALID_FORMAT, e);
            } catch (IOException e) {
                throw new ExcelException(EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED, e);
            } finally {
                if (inp != null) {
                    try {
                        inp.close();
                    } catch (IOException e) {
                        LOGGER.debug("Apache Poi: Closing of input stream does not work. (Method: getValueOfCells)");
                    }
                }
                
                //Not nice, but workbook-object will not released.
                new Thread(new GarbageDestroyer()).start();
            }
        }
        return retValues;
    }

    
    
    @Override
    public ExcelAddress[] getUserDefinedCellNames(File xlFile) throws ExcelException {
        InputStream inp = null;
        ExcelAddress[] names;

        try {
            inp = new FileInputStream(xlFile);
            org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create(inp);
            int noNames = wb.getNumberOfNames();
            names = new ExcelAddress[noNames];
            for (int i = 0; i < noNames; i++) {
                names[i] = new ExcelAddress(xlFile, wb.getNameAt(i).getNameName());
            }
        } catch (FileNotFoundException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_NOT_FOUND, e);
        } catch (InvalidFormatException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_HAS_AN_INVALID_FORMAT, e);
        } catch (IOException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED, e);
        } catch (IllegalArgumentException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED, e);
        } finally {
            if (inp != null) {
                try {
                    inp.close();
                } catch (IOException e) {
                    throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_CLOSED, e);
                }
            }
            
            //Not nice, but workbook-object will not released.
            new Thread(new GarbageDestroyer()).start();
        }

        return names;
    }

    
    
    @Override
    public String[] getMacros(File xlFile) throws ExcelException {
        throw new ExcelException("Excel is using POI implementation only. Cannot receive macro names.");
    }

    
    
    @Override
    public void runMacro(File xlFile, String macroname) throws ExcelException {
        throw new ExcelException("Excel is using POI implementation only. Cannot execute macro.");
    }

    
    
    @Override
    public void recalculateFormulas(File xlFile) throws ExcelException {
        InputStream inp = null;
        try {
            inp = new FileInputStream(xlFile);
            org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create(inp);
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            for (int sheetNum = 0; sheetNum < wb.getNumberOfSheets(); sheetNum++) {
                Sheet sheet = wb.getSheetAt(sheetNum);
                for (Row r : sheet) {
                    for (Cell c : r) {
                        if (c.getCellType() == Cell.CELL_TYPE_FORMULA) {
                            evaluator.evaluateFormulaCell(c);
                        }
                    }
                }
            }
        } catch (NotImplementedException e) {
            throw new ExcelException("Formula will be tried to evaluate which is not known in Apache POI.", e);
        } catch (FileNotFoundException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_NOT_FOUND, e);
        } catch (InvalidFormatException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_HAS_AN_INVALID_FORMAT, e);
        } catch (IOException e) {
            throw new ExcelException(EXCMSG_EXCEL_FILE_IS_NOT_FOUND_OR_CANNOT_BE_OPENED, e);
        }  finally {
            if (inp != null) {
                try {
                    inp.close();
                } catch (IOException e) {
                    throw new ExcelException(EXCMSG_EXCEL_FILE_CANNOT_CLOSED, e);
                }
            }
          
            //Not nice, but workbook-object will not released.
            new Thread(new GarbageDestroyer()).start();
        }
    }



    @Override
    public boolean isValidExcelFile(File xlFile) {
        try {
            initialTest(xlFile);
        } catch (ExcelException e) {
            return false;
        }
        return true;
    }
}
