/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole.swt;

import de.rcenvironment.core.gui.utils.ole.OleXLApplication;
import de.rcenvironment.core.gui.utils.ole.OleXLUtils;

/**
 * Represents the interchange of data between Excel and SWT.
 *
 * @author Philipp Fischer
 */
public class ExcelToSWTConnection {

    private String cellFullAddress;

    private boolean isWriteConnection;

    private ExcelToWidgetConnector xlToWidgetConnector;

    private OleXLApplication xlApplication;

    public ExcelToSWTConnection(String cellFullAddress, boolean isWriteConnection, ExcelToWidgetConnector xlToWidgetConnector,
        OleXLApplication xlApplication) {
        this.cellFullAddress = cellFullAddress;
        this.isWriteConnection = isWriteConnection;
        this.xlToWidgetConnector = xlToWidgetConnector;
        this.xlApplication = xlApplication;
    }


    public String getCellFullAddress() {
        return cellFullAddress;
    }

    public boolean isWriteConnection() {
        return isWriteConnection;
    }

    public ExcelToWidgetConnector getXlToWidgetConnector() {
        return xlToWidgetConnector;
    }

    /**
     * Interchanges data between Excel and SWT.
     */
    public void execute() {
        String cellAddress = OleXLUtils.getCellAddressOfFullAddress(cellFullAddress);
        String sheetName = OleXLUtils.getSheetNameOfFullAddress(cellFullAddress);

        if (isWriteConnection) {
            // Write value from excel cell to widget (write)
            xlToWidgetConnector.setDataToWidget(xlApplication.getActiveWorkbook().getWorksheets().getWorksheetByName(sheetName)
                .getRange(cellAddress).getValue());
        } else {
            // Write value from Widget to excel cell (read)
            xlApplication.getActiveWorkbook().getWorksheets().getWorksheetByName(sheetName).getRange(cellAddress)
                .setValue(xlToWidgetConnector.getDataFromWidget());
        }
    }
}
