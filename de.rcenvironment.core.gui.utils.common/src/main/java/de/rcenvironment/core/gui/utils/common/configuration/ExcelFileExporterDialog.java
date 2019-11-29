/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.utils.common.configuration;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.utils.common.excel.legacy.ExcelFileExporter;
import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;


/**
 * 
 * Provides a retry Dialog for the ExcelFileExporter.
 *
 * @author Jascha Riedel
 */
public final class ExcelFileExporterDialog {
    
    private static final int MAX_RETRY_ON_EXCEL_EXPORT = 5;
    
    private ExcelFileExporterDialog() {}
    
    /**
     * 
     * Exports a Matrix of excel cell values to the excelFile.
     * User is prompted for retry if export fails due to FileNotFoundException.
     * 
     * @param excelFile The file which will be created/replaced
     * @param values Matrix of Excel cell values.
     * @return Boolean succes.
     */
    public static boolean exportExcelFile(File excelFile, TypedValue[][] values) {
        boolean success = false;
        int retry = 1;
        int retryCount = 0;
        do {
            try {
                success = ExcelFileExporter.exportValuesToExcelFile(excelFile, values);
                retry = 1;
            } catch (FileNotFoundException e) {
                success = false;
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                MessageDialog dialog = new MessageDialog(shell, Messages.exportToExcelDialogTitle,
                    null,
                    Messages.exportToExcelDialogText + "\n" + "File: " + excelFile.getAbsolutePath(),
                    MessageDialog.ERROR,
                    new String[] {"Retry", "Cancel"},
                    0);
                retry = dialog.open();
                retryCount++;
            }
        } while (retry == 0 && retryCount < MAX_RETRY_ON_EXCEL_EXPORT);
        return success;
    }
    
}
