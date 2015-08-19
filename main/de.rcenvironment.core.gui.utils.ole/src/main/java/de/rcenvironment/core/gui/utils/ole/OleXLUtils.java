/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import java.util.regex.Pattern;

/**
 * Provides general utilities that are needed for easy access to excel.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 */
public final class OleXLUtils {

    private static final String COLON = ":";

    private OleXLUtils() {}
    
    /**
     * Hands back the sheet name from a full address.
     * 
     * @param fullAddress A full address that contains the sheet name and cell address (like
     *        "Tabelle1:A2")
     * @return The name of the sheet that is located before the separator (":").
     */
    public static String getSheetNameOfFullAddress(String fullAddress) {
        // Define the Regexp to look for
        Pattern patternSeperator = Pattern.compile(COLON);

        String[] splitAddress = patternSeperator.split(fullAddress);

        Pattern.matches("\\w*", splitAddress[0]);

        return (splitAddress[0]);
    }

    /**
     * Hands back the cell address from a full address.
     * 
     * @param fullAddress A full address that contains the sheet name and cell address (like
     *        "Tabelle1:A2")
     * @return The address of the cell that is located behind the separator (":").
     */
    public static String getCellAddressOfFullAddress(String fullAddress) {
        Pattern patternSeperator = Pattern.compile(COLON);

        String[] splitAddress = patternSeperator.split(fullAddress);

        Pattern.matches("$?\\w*$?\\d*", splitAddress[1]);

        return (splitAddress[1]);
    }

    /**
     * Flag if full address is a cell area.
     * 
     * @param fullAddress A full address that contains the sheet name and cell address (like
     *        "Tabelle1:A2")
     * @return true if address addresses a cell area
     */
    public static boolean isCellAreaOfFullAddress(String fullAddress) {
        Pattern patternSeperator = Pattern.compile(COLON);

        String[] splitAddress = patternSeperator.split(fullAddress);

        if (splitAddress.length > 2) {
            return true;
        }
        return false;
    }

    /**
     * Hands back the second cell address from a full address (in case of cell adrea).
     * 
     * @param fullAddress A full address that contains the sheet name and cell address (like
     *        "Tabelle1:A2:B5")
     * @return The address of the second cell that is located behind the separator (":"). Otherwise
     *         null.
     */
    public static String getSecondCellAddressOfFullAddress(String fullAddress) {
        Pattern patternSeperator = Pattern.compile(COLON);

        String[] splitAddress = patternSeperator.split(fullAddress);

        if (splitAddress.length <= 2) {
            return null;
        }

        Pattern.matches("$?\\w*$?\\d*", splitAddress[2]);

        return (splitAddress[2]);
    }
}
