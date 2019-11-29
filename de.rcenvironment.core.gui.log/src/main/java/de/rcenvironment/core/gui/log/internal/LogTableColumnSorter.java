/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.rcenvironment.core.log.SerializableLogEntry;

/**
 * Provides functionality of sorting a table column. 
 * You can choose which column to sort on and in which direction. 
 * 
 * @author Enrico Tappert
 */
public class LogTableColumnSorter extends ViewerSorter {

    private static final int FIRST_IS_EQUAL = 0;

    private static final int SORT_ASCENDING = 1;

    private static final int SORT_DESCENDING = 2;

    private int myDirection;

    private int myColumnToSort;

    /**
     * 
     * Sets the default sorting column and direction.
     * 
     */
    public LogTableColumnSorter() {
        myColumnToSort = 3;
        myDirection = SORT_DESCENDING;
    }

    /**
     * 
     * Lets set another column than default one to sort on.
     * 
     * @param column The index (beginning with 0) of column to sort.
     */
    public void setColumn(int column) {

        if (column == myColumnToSort) {
            // same column as last sort: toggle direction
            
            if (SORT_ASCENDING == myDirection) {
                myDirection = SORT_DESCENDING;
            } else {
                myDirection = SORT_ASCENDING;
            }
        } else {
            // new column to sort
            
            myColumnToSort = column;
            myDirection = SORT_ASCENDING;
        }
    }

    @Override
    public int compare(Viewer viewer, Object object1, Object object2) {

        int returnValue = FIRST_IS_EQUAL;

        if (object1 instanceof SerializableLogEntry && object2 instanceof SerializableLogEntry) {
            SerializableLogEntry le1 = (SerializableLogEntry) object1;
            SerializableLogEntry le2 = (SerializableLogEntry) object2;

            switch (myColumnToSort) {

            // level column
            case 0:
                returnValue = new Integer(le1.getLevel()).compareTo(le2.getLevel());
                break;

            // message column
            case 1:
                returnValue = le1.getMessage().compareTo(le2.getMessage());
                break;

            // bundle column
            case 2:
                returnValue = le1.getBundleName().compareTo(le2.getBundleName());
                break;

            // platform column
            case 3:
                returnValue = le1.getPlatformIdentifer().toString().compareTo(le2.getPlatformIdentifer().toString());
                break;
            // date/time column
            case 4:
                returnValue = new Long(le1.getTime()).compareTo(le2.getTime());
                break;

            // shouldn't occur
            default:
                break;
            }

            // is DESCENDING? then flip sorting!
            if (SORT_DESCENDING == myDirection) {
                returnValue = -returnValue;
            }
        }

        return returnValue;
    }
}
