/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel address representation.
 * 
 * @author Markus Kunde
 */
public class ExcelAddress implements Serializable {

    private static final long serialVersionUID = 169274298193312428L;

    private String fullAddress;

    private String worksheetName;

    private String firstCell;

    private String lastCell;

    private int numberOfRows;

    private int numberOfColumns;

    private int beginningRowNumber;

    private int beginningColumnNumber;

    private String userDefinedNameForAddress;

    /**
     * Copy Constructor.
     * 
     * @param addr Excel Address
     */
    public ExcelAddress(final ExcelAddress addr) {
        fullAddress = addr.fullAddress;
        worksheetName = addr.worksheetName;
        firstCell = addr.firstCell;
        lastCell = addr.lastCell;
        numberOfRows = addr.numberOfRows;
        numberOfColumns = addr.numberOfColumns;
        beginningRowNumber = addr.beginningRowNumber;
        beginningColumnNumber = addr.beginningColumnNumber;
        userDefinedNameForAddress = addr.userDefinedNameForAddress;
    }

    /**
     * Constructor for Excel cell address. Address will be validated with concrete Excel file.
     * 
     * @param excelFile Excel file
     * @param rawAddress sheetname and cell reference(s), e. g., "Sheet1!A1:B2"
     * @param ExcelException thrown if not a real Excel file
     */
    public ExcelAddress(final File excelFile, final String rawAddress) throws ExcelException {
        InputStream inp = null;
        try {
            inp = new FileInputStream(excelFile);
            org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create(inp);
            Name name = wb.getName(rawAddress);

            if (name != null) {
                // User defined name
                fullAddress = name.getRefersToFormula();
                fullAddress = StringUtils.remove(fullAddress, ExcelComponentConstants.ABSOLUTEFLAG);

                userDefinedNameForAddress = name.getNameName();

                worksheetName = name.getSheetName();
                worksheetName = cutBeginningAndEndingApostrophe(worksheetName);

                AreaReference ar = new AreaReference(fullAddress, SpreadsheetVersion.EXCEL2007); // Added spreadsheet version due to library
                                                                                                 // upgrade from apache poi 3.8 to 3.17 (JF,
                                                                                                 // 2021-02-02)

                firstCell = StringUtils.split(ar.getFirstCell().formatAsString(), ExcelComponentConstants.DIVIDER_TABLECELLADDRESS)[1];
                lastCell = StringUtils.split(ar.getLastCell().formatAsString(), ExcelComponentConstants.DIVIDER_TABLECELLADDRESS)[1];

                int rowUpperLeft = ar.getFirstCell().getRow();
                int rowLowerRight = ar.getLastCell().getRow();
                numberOfRows = (rowLowerRight - rowUpperLeft) + 1;

                int colUpperLeft = ar.getFirstCell().getCol();
                int colLowerRight = ar.getLastCell().getCol();
                numberOfColumns = (colLowerRight - colUpperLeft) + 1;

                beginningRowNumber = ar.getFirstCell().getRow() + 1;
                beginningColumnNumber = ar.getFirstCell().getCol() + 1;
            } else {
                // No user defined name; test if valid address
                boolean isNewXlFile;

                if (wb instanceof HSSFWorkbook) {
                    isNewXlFile = false;
                } else if (wb instanceof XSSFWorkbook) {
                    isNewXlFile = true;
                } else {
                    throw new ExcelException("Could not determine if Excel file is old-style (Excel 97-2007) "
                        + "or new-style (Excel 2007+) based.");
                }

                // Short validation
                String[] splitAddress = rawAddress.split("!");
                String lastAddressToken = splitAddress[splitAddress.length - 1];
                String exceptionMessage = "Validation of address '" + rawAddress + "' failed. "
                    + "Most likely it's no valid Excel cell address.";
                if (isNewXlFile) {
                    if (splitAddress.length != 2
                        || !(lastAddressToken.matches("^\\$?([A-Za-z]{0,3})\\$?([0-9]{0,7}):?\\$?([A-Za-z]{0,3})\\$?([0-9]{0,7})$"))
                        || (lastAddressToken.matches("[A-Za-z]+"))
                        || (lastAddressToken.matches("[0-9]+"))
                        || (lastAddressToken.startsWith(ExcelComponentConstants.DIVIDER_CELLADDRESS))
                        || (lastAddressToken.endsWith(ExcelComponentConstants.DIVIDER_CELLADDRESS))) {
                        throw new ExcelException(exceptionMessage);
                    }
                } else {
                    if (splitAddress.length != 2
                        || !(lastAddressToken.matches("^\\$?([A-Za-z]{0,2})\\$?([0-9]{0,5}):?\\$?([A-Za-z]{0,2})\\$?([0-9]{0,5})$"))
                        || (lastAddressToken.matches("[A-Za-z]+"))
                        || (lastAddressToken.matches("[0-9]+"))
                        || (lastAddressToken.startsWith(ExcelComponentConstants.DIVIDER_CELLADDRESS))
                        || (lastAddressToken.endsWith(ExcelComponentConstants.DIVIDER_CELLADDRESS))) {
                        throw new ExcelException(exceptionMessage);
                    }
                }
                String rawAddressWithoutAbsoluteFlag = StringUtils.remove(rawAddress, ExcelComponentConstants.ABSOLUTEFLAG);

                worksheetName = StringUtils.split(rawAddressWithoutAbsoluteFlag, ExcelComponentConstants.DIVIDER_TABLECELLADDRESS)[0];
                worksheetName = cutBeginningAndEndingApostrophe(worksheetName);

                firstCell = getAddressPart(rawAddressWithoutAbsoluteFlag, true, isNewXlFile);

                lastCell = getAddressPart(rawAddressWithoutAbsoluteFlag, false, isNewXlFile);

                numberOfRows = getNumberOfRows(firstCell, lastCell);

                numberOfColumns = getNumberOfColumns(firstCell, lastCell);

                beginningRowNumber = Integer.valueOf(getRowNumberOfCell(firstCell));

                beginningColumnNumber = getNumberOfColumnChar(getColumnCharsOfCell(firstCell));

                fullAddress = worksheetName + ExcelComponentConstants.DIVIDER_TABLECELLADDRESS + firstCell
                    + ExcelComponentConstants.DIVIDER_CELLADDRESS + lastCell;

                // Test if full address
                AreaReference ar = new AreaReference(fullAddress, SpreadsheetVersion.EXCEL2007); // Added spreadsheet version due to library
                                                                                                 // upgrade from apache poi 3.8 to 3.17 (JF,
                                                                                                 // 2021-02-02)
                fullAddress = ar.formatAsString();

                Sheet sheet = wb.getSheet(worksheetName);
                if (sheet == null) {
                    throw new ExcelException("Cannot discover sheet in Excel file.");
                }
            }
        } catch (NumberFormatException e) {
            throw new ExcelException(e);
        } catch (FileNotFoundException e) {
            throw new ExcelException(e);
        } catch (IllegalArgumentException e) {
            throw new ExcelException("File is no Excel file.", e);
        } catch (IOException e) {
            throw new ExcelException("Excel file is not found or cannot be opened.", e);
        } finally {
            if (inp != null) {
                try {
                    inp.close();
                } catch (IOException e) {
                    throw new ExcelException("Cannot close access to Excel file.", e);
                }
            }

            // Not nice, but workbook-object will not released.
            ExcelUtils.destroyGarbage();
        }
    }

    private static String cutBeginningAndEndingApostrophe(final String rawString) {
        String resultString;
        resultString = StringUtils.removeStart(rawString, "'");
        resultString = StringUtils.removeEnd(resultString, "'");

        return resultString;
    }

    /**
     * Returns full (relative) Excel cell address.
     * 
     * @return full relative Excel cell address
     */
    public String getFullAddress() {
        return fullAddress;
    }

    /**
     * Returns user defined name or null if there is no user defined name.
     * 
     * @return user defined name or null
     */
    public String getUserDefinedName() {
        return userDefinedNameForAddress;
    }

    /**
     * Returns worksheet name of Excel cell address.
     * 
     * @return worksheet name
     */
    public String getWorkSheetName() {
        return worksheetName;
    }

    /**
     * Returns the upper left cell reference of a cell-area. In case there is only one cell its cell reference will be returned.
     * 
     * @return upper left cell reference.
     */
    public String getFirstCell() {
        return firstCell;
    }

    /**
     * Returns the lower right cell reference of a cell-area. In case there is only one cell its cell reference will be returned.
     * 
     * @return lower right cell reference.
     */
    public String getLastCell() {
        return lastCell;
    }

    /**
     * Returns the number of rows a cell-area is defined over.
     * 
     * @return number of rows
     */
    public int getNumberOfRows() {
        return numberOfRows;
    }

    /**
     * Returns the number of columns a cell-area is defined over.
     * 
     * @return number of columns
     */
    public int getNumberOfColumns() {
        return numberOfColumns;
    }

    /**
     * Returns beginning row number of cell-area. Begins with 1.
     * 
     * @return beginning row number
     */
    public int getBeginningRowNumber() {
        return beginningRowNumber;
    }

    /**
     * Returns beginning column number of cell-area. Begins with 1.
     * 
     * @return beginning column number
     */
    public int getBeginningColumnNumber() {
        return beginningColumnNumber;
    }

    /**
     * Tests if user defined name is valid regarding regex.
     * 
     * @param regex regular expression
     * @return true if regex on user defend name returns true
     */
    public boolean isUserDefindNameOfScheme(final String regex) {
        if (userDefinedNameForAddress != null && !userDefinedNameForAddress.isEmpty() && userDefinedNameForAddress.matches(regex)) {
            return true;
        }
        return false;
    }

    /**
     * Resizes the Excel address for a range. Starting point is first cell of Excel address.
     * 
     * @param excelFile Excel file
     * @param originAddr the address which will be used as a basis
     * @param rows the offset-number of rows the address should have
     * @param columns the offset-number of columns the address should have
     * @return the new ExcelAddress
     */
    public static ExcelAddress getExcelAddressForTableRange(final File excelFile, final ExcelAddress originAddr,
        final int rows, final int columns) {
        ExcelAddress address = new ExcelAddress(originAddr);

        if (rows > 0 && columns > 0) {
            address.lastCell = "";

            String columnName = getNextColumnName(getColumnCharsOfCell(address.firstCell), columns - 1);
            address.lastCell = columnName.concat(String.valueOf(Integer.valueOf(getRowNumberOfCell(address.firstCell)) + rows - 1));

            if (!originAddr.getFirstCell().equalsIgnoreCase(originAddr.getLastCell())) {
                address.fullAddress = StringUtils.removeEnd(address.fullAddress, originAddr.getLastCell());
            }

            if (!address.fullAddress.endsWith(ExcelComponentConstants.DIVIDER_CELLADDRESS)) {
                address.fullAddress = address.fullAddress.concat(ExcelComponentConstants.DIVIDER_CELLADDRESS);
            }

            address.fullAddress = address.fullAddress.concat(address.lastCell);
            address.userDefinedNameForAddress = null;
            address.numberOfColumns = columns;
            address.numberOfRows = rows;

            address = new ExcelAddress(excelFile, address.getFullAddress());
        }
        return address;
    }

    /**
     * Returns upper left address of Excel cell address.
     * 
     * @param address [Table1!A1, Table1!A:A, Table1!1:1, Table1!A1:B5]
     * @param leftAddress flag if left (true) or right (false) address should be discovered.
     * @param isXlsX should be true if Excel file is a "new" one with more rows and columns.
     * @return [A1]
     */
    private static String getAddressPart(final String address, final boolean leftAddress, final boolean isXlsX) {
        int addressFlag;
        if (leftAddress) {
            addressFlag = 0;
        } else {
            addressFlag = 1;
        }

        String cells = address.split(ExcelComponentConstants.DIVIDER_TABLECELLADDRESS)[1];
        // cells = [A1, A, 1, A1:B5]

        String[] normalizedCells = cells.split(ExcelComponentConstants.DIVIDER_CELLADDRESS);

        if (normalizedCells.length == 1) {
            return normalizedCells[0];
        }

        String columnChars = getColumnCharsOfCell(normalizedCells[addressFlag]);
        String rowNumber = getRowNumberOfCell(normalizedCells[addressFlag]);

        if (columnChars == null || columnChars.equals("")) {
            if (leftAddress) {
                columnChars = ExcelComponentConstants.DEFAULTCOLUMNBEGIN;
            } else {
                if (isXlsX) {
                    columnChars = SpreadsheetVersion.EXCEL2007.getLastColumnName();
                } else {
                    columnChars = SpreadsheetVersion.EXCEL97.getLastColumnName();
                }
            }
        }
        if (rowNumber == null || rowNumber.equals("")) {
            if (leftAddress) {
                rowNumber = ExcelComponentConstants.DEFAULTROWBEGIN;
            } else {
                if (isXlsX) {
                    rowNumber = Integer.toString(SpreadsheetVersion.EXCEL2007.getMaxRows());
                } else {
                    rowNumber = Integer.toString(SpreadsheetVersion.EXCEL97.getMaxRows());
                }
            }
        }

        return columnChars + rowNumber;
    }

    /**
     * Get row label number of Excel cell.
     * 
     * @param cellname complete cell name
     * @return row number; empty if there is no row number
     */
    private static String getRowNumberOfCell(final String cellname) {
        String[] result = cellname.split("[\\D]+");
        String returnVal = null;
        if (result.length == 0) {
            returnVal = "";
        } else if (result.length == 1) {
            returnVal = result[0];
        } else {
            returnVal = result[1];
        }
        return returnVal;
    }

    /**
     * Get column label chars of Excel cell.
     * 
     * @param cellname complete cell name
     * @return column chars; empty if there are no column chars
     */
    private static String getColumnCharsOfCell(final String cellname) {
        String[] result = cellname.split("[\\d]+");
        String returnVal = null;
        if (result.length == 0) {
            returnVal = "";
        } else {
            returnVal = result[0];
        }
        return returnVal;
    }

    /**
     * Get number of columns.
     * 
     * @param upperLeftCell upper left cell address
     * @param lowerRightCell lower right cell address
     * @return number of columns
     */
    private static int getNumberOfColumns(final String upperLeftCell, final String lowerRightCell) {
        String tempUl = getColumnCharsOfCell(upperLeftCell);
        if (tempUl != null && !tempUl.equals("")) {
            String tempLr = getColumnCharsOfCell(lowerRightCell);
            if (tempLr != null && !tempLr.equals("")) {
                int number = 1;
                while (!tempUl.equals(tempLr)) {
                    number++;
                    tempUl = getNextColumnName(tempUl, 1);
                }
                return number;
            }
        }
        return 1;
    }

    /**
     * Returns number of location of a column char. E. g.: "F" is 6; "AA" is 27.
     * 
     * @param columnChar Only column char
     * @return number of location
     */
    private static int getNumberOfColumnChar(final String columnChar) {
        return toPos(columnChar);
    }

    private static int toPos(String col) {
        final int twentySix = 26;
        final int thirtySix = 36;
        final int nine = 9;

        int pos = 0;
        for (int i = 0; i < col.length(); i++) {
            pos *= twentySix;
            pos += Integer.parseInt(col.substring(i, i + 1), thirtySix) - nine;
        }
        return pos;
    }

    private static String getOffsetCol(String col, int offset) {
        return toCol(toPos(col) + offset);
    }

    private static String toCol(int pos) {
        final int twentySix = 26;
        final int sixtyFive = 65;

        String col = "";
        while (pos > 0) {
            pos--;
            col = (char) (pos % twentySix + sixtyFive) + col;
            pos = pos / twentySix;
        }
        return col;
    }

    /**
     * Returns next column name regarding an offset.
     * 
     * @param currentColumnName Name of column, e. g., "A"
     * @param offset Offset of column, "1" for next column
     * @return next columnname regarding offset
     */
    private static String getNextColumnName(final String currentColumnName, final int offset) {
        return getOffsetCol(currentColumnName, offset);
    }

    /**
     * Get number of rows.
     * 
     * @param upperLeftCell upper left cell address
     * @param lowerRightCell lower right cell address
     * @return number of rows
     */
    private static int getNumberOfRows(final String upperLeftCell, final String lowerRightCell) {
        String tempUl = getRowNumberOfCell(upperLeftCell);
        if (tempUl != null && !tempUl.equals("")) {
            int ul = Integer.valueOf(tempUl);

            String tempLr = getRowNumberOfCell(lowerRightCell);
            if (tempLr != null && !tempLr.equals("")) {
                int lr = Integer.valueOf(tempLr);
                return (lr - ul) + 1;
            }
        }
        return 1;
    }

}
