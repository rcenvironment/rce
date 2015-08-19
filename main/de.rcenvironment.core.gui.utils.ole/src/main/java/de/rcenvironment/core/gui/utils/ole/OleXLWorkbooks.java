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
 * OleXLWorkbooks contains the main functionalities to manipulate the excel WorkBooks container.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 */
public class OleXLWorkbooks extends OleXLContainer {

    /**
     * General constructor.
     * 
     * @param oleWorkbooks Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLWorkbooks(OleAutomation oleWorkbooks, OleControlSite controlSite) {
        super(oleWorkbooks, controlSite);
    }

    /**
     * Give access to a WorkBook by it's index.
     * 
     * @param index An index staring with one referring to the WorkBooks storage position.
     * @return The WorkBook from the given index.
     */
    public OleXLWorkbook getWorkbookByIndex(int index) {
        Variant varIndex = new Variant(index);
        return getWorkbookByVariant(varIndex);
    }

    /**
     * Gives access to a WorkBook by it's name.
     * 
     * @param name Name of the WorkBook used in the excel object.
     * @return The Workbook for the given name.
     */
    public OleXLWorkbook getWorkbookByName(String name) {
        Variant varName = new Variant(name);
        return getWorkbookByVariant(varName);
    }

    /**
     * General function that returns the Workbook indexed by a variant. The function calls
     * getItemByVariant from the superclass.
     * 
     * @param variant Either containing the index or name of the WorkBook to get.
     * @return The corresponding WorkBook indexed by the variant.
     */
    private OleXLWorkbook getWorkbookByVariant(Variant variant) {
        return getItemByVariant(variant).createWorkbook();
    }

    /**
     * Get a list of all the WorkBooks within the container WorkBooks.
     * 
     * @return List of names of the Workbooks.
     */
    public List<String> listOfWorkbooks() {
        return listOfItems();
    }

    /**
     * Adds a new Workbook to the container.
     * 
     * @return reference to the newly added WorkBook.
     */
    public OleXLWorkbook add() {
        return new OleXLWorkbook(addItem(), controlSite);
    }
}
