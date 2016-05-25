/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.excel.common;


import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;


/**
 * Utility class for handling Excel stuff.
 *
 * @author Markus Kunde
 */
public final class ExcelUtils {
        
    private ExcelUtils() {}
    
    
    /**
     * Makes relative Eclipse iFile and absolute files into absolute files.
     * 
     * @param pathOfFile path of file
     * @return absolute file or null if not existing
     */
    public static File getAbsoluteFile(String pathOfFile) {
        File file = null;
        
        if (pathOfFile == null || pathOfFile.isEmpty()) {
            return file;
        }
        
        IPath path = new Path(pathOfFile);
        
        if (path.isAbsolute()) {
            file = new File(pathOfFile);
            if (!file.exists()) {
                file = null;
            }
        } else {
            IFile fileEclipse = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            if (fileEclipse.exists()) {
                file = fileEclipse.getRawLocation().toFile();
            }
        }
        
        return file;
    }
    
    /**
     * Concats a String array to one single String by separating the Strings  with the given separator.
     * This was moved from TypedValue class, since it doesn't belong there.
     * 
     * @param table Table to concat
     * @param valueSeparator separator to use
     * @param lineSeparator separator to use
     * @return separated list of array content
     */
    public static String smallTableToString(final SmallTableTD table, final String valueSeparator, final String lineSeparator) {
        TypedDatum[][] array = table.toArray();
        if (array == null) {
            return new String();
        }
        StringBuffer result = new StringBuffer();
        for (int row = 0; row < array.length; row++) {
            for (int col = 0; col < array[row].length; col++) {
                
                TypedDatum data = array[row][col];
                String str;
                switch (data.getDataType()) {
                case ShortText:
                    str = ((ShortTextTD) data).getShortTextValue();
                    break;
                case Float:
                    str = String.valueOf(((FloatTD) data).getFloatValue());
                    break;
                case Integer:
                    str = String.valueOf(((IntegerTD) data).getIntValue());
                    break;  
                case Boolean:
                    str = String.valueOf(((BooleanTD) data).getBooleanValue());
                    break;
                case DateTime:
                    str = ((DateTimeTD) data).getDateTime().toString();         
                    break;
                case Empty:
                    str = new String();
                    break;
                default:
                    str = new String();
                    break;
                }
                result.append(str);
                if (col != (array[row].length) - 1) {// not last element
                    result.append(valueSeparator);
                }
            }
            if ((row + 1) < array.length) { //not last element
                result.append(lineSeparator);
            }
        }
        return result.toString();
    }
    
    /**
     * Returns row index of last cell which contains data.
     * 
     * @param dataStore 2d content-array
     * @return row index of last cell which contains data
     */
    public static int getRowIndexLastCellFilled(final TypedDatum[][] dataStore) {
        for (int row = dataStore.length - 1; row >= 0; row--) {
            for (int column = dataStore[0].length - 1; column >= 0; column--) {
                if (dataStore[row][column] != null && dataStore[row][column].getDataType() != DataType.Empty) {
                    return row;
                }
            }
        }
        
        return 0;
    }

    /**
     * Returns column index of last cell which contains data.
     * 
     * @param dataStore 2d content-array
     * @return column index of last cell which contains data
     */
    public static int getColumnIndexLastCellFilled(final TypedDatum[][] dataStore) {
        for (int row = dataStore.length - 1; row >= 0; row--) {
            for (int column = dataStore[0].length - 1; column >= 0; column--) {
                if (dataStore[row][column] != null && dataStore[row][column].getDataType() != DataType.Empty) {
                    return column;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Destroys garbage.
     */
    public static void destroyGarbage() {
        SharedThreadPool.getInstance().execute(new GarbageDestroyer());
    }
}
