/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import java.util.List;
import java.util.Vector;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.Variant;

/**
 * General class for the containers WorkBooks and WorkSheets in Excel which share several common
 * functions.
 * 
 * @author Philipp Fischer
 */
public class OleXLContainer extends OleXLGeneral {

    /**
     * General constructor.
     * 
     * @param oleContainer Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLContainer(OleAutomation oleContainer, OleControlSite controlSite) {
        super(oleContainer, controlSite);
    }

    /**
     * Lists all items in a container.
     * 
     * @return All the names of the items currently stored in the container.
     */
    protected List<String> listOfItems() {
        // Get the amount of workbooks open in excel and create the return vector with the according
        // size
        final int countItems = countItems();
        List<String> vecWorkbookNames = new Vector<String>(countItems);
        ((Vector<String>) vecWorkbookNames).setSize(countItems);

        // Look into each workbook, get the name and store it to the output-vector
        for (int i = 0; i < countItems; i++) {
            String itemName = getItemByIndex(i + 1).getName();
            vecWorkbookNames.set(i, itemName);
        }

        // hand back the vector containing all names of workbooks
        return vecWorkbookNames;
    }

    /**
     * Adds an item to the container.
     * 
     * @return Automation object which is used by the derived classes to give access to the added
     *         WorkSheet or WorkBook.
     */
    public OleAutomation addItem() {
        return invoke("Add").getAutomation();
    }

    /**
     * Count the items stored in the container.
     * 
     * @return Number of items stored in the container.
     */
    protected int countItems() {
        return getProperty("Count").getInt();
    }

    /**
     * Hands back an item of the container by its storage index.
     * 
     * @param index Storage place of the item starting with index 1.
     * @return Item from the specified index.
     */
    protected OleXLItem getItemByIndex(int index) {
        Variant varIndex = new Variant(index);
        return getItemByVariant(varIndex);
    }

    /**
     * Hands back an item of the container by its name.
     * 
     * @param name The name under which the item is stored.
     * @return Item corresponding to the specified name.
     */
    protected OleXLItem getItemByName(String name) {
        Variant varName = new Variant(name);
        return getItemByVariant(varName);
    }

    /**
     * Common function to access an item by a variant. This function is called by the ByName and
     * ByIndex equivalent.
     * 
     * @param variant Either the name as string or index as integer.
     * @return Item corresponding to the variant.
     */
    protected OleXLItem getItemByVariant(Variant variant) {
        // get the ID of the item property and prepare the variant for the index
        Variant[] varIndex = new Variant[] { variant };
        int itemID = getIDofName("Item");

        // Access the item property with the given index and hand back the OleXLWorkbook
        OleAutomation oleWorkbook = oleObject.getProperty(itemID, varIndex).getAutomation();
        logOleError(oleObject.getLastError());
        return new OleXLItem(oleWorkbook, controlSite);
    }

}
