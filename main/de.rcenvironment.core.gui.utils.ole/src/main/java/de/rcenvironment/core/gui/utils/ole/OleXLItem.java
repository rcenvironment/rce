/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.Variant;

/**
 * Generalizes functionalities that are common by ExcelWorkbook and Excelsheet.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 * 
 */
public class OleXLItem extends OleXLGeneral {

    /**
     * General constructor.
     * 
     * @param oleItem Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLItem(OleAutomation oleItem, OleControlSite controlSite) {
        super(oleItem, controlSite);
    }

    /**
     * Activates the current item within the excel object.
     */
    public void activate() {
        invokeNoReply("Activate");
    }

    /**
     * Gets the name of the current item that has been associated with it within the excel object.
     * 
     * @return Name of the item as String.
     */
    public String getName() {
        return getProperty("Name").getString();
    }
    
    /**
     * Get all names from workbook.
     * 
     * @return Array of all names
     */
    public String[] getNames() {
        Set<String> namesSet = new HashSet<String>();
        
        Variant namesVariant = getProperty("Names");
        OleAutomation names = namesVariant.getAutomation();
        
        //Number of names
        int countID = names.getIDsOfNames(new String[] {"Count"})[0];
        int numberOfNames = names.getProperty(countID).getInt();
        
        //for each number
        for (int i = 1; i <= numberOfNames; i++) {
            int itemID = names.getIDsOfNames(new String[] {"Item"})[0];
            Variant name = names.invoke(itemID, new Variant[] {new Variant(i)}); 
            OleAutomation nameName = name.getAutomation();
            int nameID = nameName.getIDsOfNames(new String[] {"Name"})[0];
            Variant nameCustom = nameName.getProperty(nameID);
            namesSet.add(nameCustom.getString());
        }
        
        return namesSet.toArray(new String[0]);
    }

    /**
     * Closes the item for example Workbook or WorkSheet.
     */
    public void close() {
        invoke("Close");
    }

    /**
     * Saves the item for example Workbook or WorkSheet.
     * 
     * @param filename Absolute path to file.
     */
    public void saveAs(String filename) {
        Variant[] params = new Variant[1];
        params[0] = new Variant(filename);
        invoke("SaveAs", params);
    }

    /**
     * Factory method to create a WorkBook out of the generalized Item.
     * 
     * @return Workbook with reference to the items OleAutomation object.
     */
    public OleXLWorkbook createWorkbook() {
        return new OleXLWorkbook(oleObject, controlSite);
    }

    /**
     * Factory method to create a WorkSheet out of the generalized Item.
     * 
     * @return WorkSheet with reference to the items OleAutomation object.
     */
    public OleXLWorksheet createWorksheet() {
        return new OleXLWorksheet(oleObject, controlSite);
    }
}
