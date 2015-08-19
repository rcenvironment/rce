/*
 * Copyright (C) 2006-2014 DLR, Germany
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
 * Represents the ExcelRange object which represents the cells within an ExcelSheet.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 * 
 */
public class OleXLRange extends OleXLGeneral {

    /**
     * General constructor.
     * 
     * @param oleContainer Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLRange(OleAutomation oleGeneral, OleControlSite controlSite) {
        super(oleGeneral, controlSite);
    }

    /**
     * To get the ranges absolute ExcelAddress (like "$A$5");.
     * 
     * @return Address of the range within the ExcelSheet
     */
    public String getAddressAbsolute() {
        return getProperty("Address").getString();
    }
    
    /**
     * To get the name of the range cell.
     * 
     * @return Name of the range
     */
    public String getName() {
        Variant nameVariant = getProperty("Name");
        if (nameVariant == null) {
            return null;
        }
        OleAutomation name = nameVariant.getAutomation();
        int nameID = name.getIDsOfNames(new String[] { "Name" })[0];
        nameVariant = name.getProperty(nameID);
        return nameVariant.getString();
    }

    /**
     * To get the ranges relative ExcelAddress (like "B20");.
     * 
     * @return Address of the range within the ExcelSheet
     */
    public String getAddressRelative() {
        int addressID = getIDofName("Address");

        /*
         * Following we have to prepare the arguments to get the address as relative (taken from
         * Excel TypeLib)
         * 
         * [propget, helpcontext(0x000100ec)] HRESULT _stdcall Address( [in, optional] VARIANT
         * RowAbsolute, [in, optional] VARIANT ColumnAbsolute, [in, optional, defaultvalue(1)]
         * XlReferenceStyle ReferenceStyle, [in, optional] VARIANT External, [in, optional] VARIANT
         * RelativeTo, [in, lcid] long lcid, [out, retval] BSTR* RHS);
         */

        Variant[] varArguments = new Variant[] { new Variant(false), new Variant(false) };

        // get the address with the set arguments to retrieve it as relative link
        Variant varAddress = oleObject.getProperty(addressID, varArguments);
        logOleError(oleObject.getLastError());
        return varAddress.getString();
    }

    /**
     * Accesses the value of a cell.
     * 
     * @return Value of the cell stored as variant.
     */
    public Variant getValue() {
        return getProperty("Value");
    }

    /**
     * Writes a value to the Cell.
     * 
     * @param varValue Value as variant that is supposed to be written to the cell.
     */
    public void setValue(Variant varValue) {
        setProperty("Value", varValue);
    }
}
