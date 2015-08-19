/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import java.util.List;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.Variant;

/**
 * Provides functionalities to manipulate the WorkSheets container.
 * 
 * @author Philipp Fischer
 */
public class OleXLWorksheets extends OleXLContainer {

    /**
     * General constructor.
     * 
     * @param oleWorksheets Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLWorksheets(OleAutomation oleWorksheets, OleControlSite controlSite) {
        super(oleWorksheets, controlSite);
    }

    /**
     * Tells if the WorkSheets are visible on the display.
     * 
     * @return true in case they are visible.
     */
    public boolean isVisible() {
        return getProperty("Visible").getBoolean();
    }

    /**
     * Allows to change the visibility of the WorkSheets.
     * 
     * @param visible Set to true to make WorkSheets visible.
     */
    public void setVisible(boolean visible) {
        setProperty("Visible", new Variant(visible));
    }

    /**
     * Generates a list with all the names of currently open WorkSheets in the container.
     * 
     * @return A vector containing the names of the Sheets.
     */
    public List<String> listOfWorksheets() {
        return listOfItems();
    }

    /**
     * Allows to access a WorkSheet by it's index.
     * 
     * @param index Index of the storage location starting at 1.
     * @return WorkSheet corresponding to the given index.
     */
    public OleXLWorksheet getWorksheetByIndex(int index) {
        Variant varIndex = new Variant(index);
        return getWorksheetByVariant(varIndex);
    }

    /**
     * Allows to access a WorkSheet by its internal name.
     * 
     * @param name Name of the WorkSheet given within the excel object.
     * @return WorkSheet corresponding to the given name.
     */
    public OleXLWorksheet getWorksheetByName(String name) {
        Variant varName = new Variant(name);
        return getWorksheetByVariant(varName);
    }

    /**
     * Used by the functions ByName or ByIndex to access a WorkSheet.
     * 
     * @param variant Can either be the index or the name of the WorkSheet to access to.
     * @return WorkSheet indexed by the variant.
     */
    private OleXLWorksheet getWorksheetByVariant(Variant variant) {
        return getItemByVariant(variant).createWorksheet();
    }

    /**
     * Adds a new WorkSheet to the WorkSheets container.
     * 
     * @return The newly added WorkSheet.
     */
    public OleXLWorksheet add() {
        return new OleXLWorksheet(addItem(), controlSite);
    }
}
