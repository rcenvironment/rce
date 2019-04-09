/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * A helper class to provide common methods useful in the context of study data export.
 * 
 * @author Oliver Seebach
 *
 */
public final class StudyDataExportMessageHelper {

    private StudyDataExportMessageHelper() {
        // private constructor to prevent instantiation
    }

    /**
     * Shows an information or warning message dialog depending on whether the export was successful
     * or not.
     * 
     * @param success Whether the export was successful or not.
     * @param filePath The path to the file where the data was exported.
     */
    public static void showConfirmationOrWarningMessageDialog(boolean success, String filePath) {
        if (success) {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
                "Success",
                "The data file was successfully exported.\nLocation: " + filePath);
        } else {
            MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
                "Failure",
                "An error occurred while exporting the data file. Location: " + filePath);
        }
    }

}
