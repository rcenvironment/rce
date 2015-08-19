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
import org.eclipse.swt.ole.win32.Variant;

/**
 * Controls the excel WorkSheet object.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 */
public class OleXLWorksheet extends OleXLItem {

    /**
     * General constructor.
     * 
     * @param oleWorksheet Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLWorksheet(OleAutomation oleWorksheet, OleControlSite controlSite) {
        super(oleWorksheet, controlSite);
    }

    /**
     * Gives access to a cell by a given address.
     * 
     * @param address The address of the cell to access to.
     * @return Range that references the addressed cell.
     */
    public OleXLRange getRange(String address) {
        int rangeID = getIDofName("Range");
        Variant[] varArgs = new Variant[] { new Variant(address) };
        OleAutomation oleRange = oleObject.getProperty(rangeID, varArgs).getAutomation();
        logOleError(oleObject.getLastError());
        return new OleXLRange(oleRange, controlSite);
    }
    
    /**
     * Gives access to cells which covers used range of worksheet.
     * 
     * @return Range that references the used range cell(s).
     */
    public OleXLRange getUsedRange() {
        invoke("Activate");
        setProperty("Visible", new Variant(true));
        //Variant[] varArgs = new Variant[] { new Variant(false), new Variant(false) };
        Variant range = getProperty("UsedRange");
        return new OleXLRange(range.getAutomation(), controlSite);
    }
}
