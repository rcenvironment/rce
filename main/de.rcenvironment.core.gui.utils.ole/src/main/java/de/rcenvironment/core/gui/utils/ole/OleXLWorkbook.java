/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;

/**
 * Provides interface to an excel Workbook.
 * 
 * @author Philipp Fischer
 */
public class OleXLWorkbook extends OleXLItem {

    /**
     * General constructor.
     * 
     * @param oleWorkbook Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLWorkbook(OleAutomation oleWorkbook, OleControlSite controlSite) {
        super(oleWorkbook, controlSite);
    }

    /**
     * Provides access to the container holding the Workbook's Worksheets.
     * 
     * @return The Worksheets container.
     */
    public OleXLWorksheets getWorksheets() {
        OleAutomation oleWorksheets = getProperty("Worksheets").getAutomation();
        return new OleXLWorksheets(oleWorksheets, controlSite);
    }
}
