/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.console;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Provides functionality of sorting a table column. 
 * You can choose which column to sort on and in which direction. 
 * 
 * @author Doreen Seider
 */
public class ConsoleColumnSorter extends ViewerSorter {

    private static final int FIRST_IS_EQUAL = 0;

    private static final int FIRST_IS_GREATER = 1;

    private static final int FIRST_IS_LESS = -1;

    private static final int SORT_ASCENDING = 1;

    private static final int SORT_DESCENDING = 2;

    private int direction;

    private int columnToSort;

    /**
     * Sets the default sorting column and direction.
     */
    public ConsoleColumnSorter() {
        columnToSort = 1;
        direction = SORT_ASCENDING;
    }

    /**
     * Lets set another column than default one to sort on.
     * 
     * @param column The index (beginning with 0) of column to sort.
     */
    public void setColumn(int column) {

        if (column == columnToSort) {
            // same column as last sort: toggle direction
            if (SORT_ASCENDING == direction) {
                direction = SORT_DESCENDING;
            } else {
                direction = SORT_ASCENDING;
            }
        } else {
            // new column to sort
            columnToSort = column;
            direction = SORT_ASCENDING;
        }
    }

    @Override
    public int compare(Viewer viewer, Object object1, Object object2) {

        int returnValue = FIRST_IS_EQUAL;

        if (object1 instanceof ConsoleRow && object2 instanceof ConsoleRow) {
            ConsoleRow cr1 = (ConsoleRow) object1;
            ConsoleRow cr2 = (ConsoleRow) object2;

            switch (columnToSort) {

            // type column
            case 0:
                if (cr1.getType() == cr2.getType()) {
                    returnValue = FIRST_IS_EQUAL;
                } else if (cr1.getType() == ConsoleRow.Type.TOOL_ERROR) {
                    returnValue = FIRST_IS_GREATER;
                } else if (cr1.getType() == ConsoleRow.Type.TOOL_OUT) { 
                    if (cr2.getType() == ConsoleRow.Type.TOOL_ERROR) {
                        returnValue = FIRST_IS_LESS;
                    } else {
                        returnValue = FIRST_IS_GREATER;
                    }
                } else {
                    returnValue = FIRST_IS_LESS;
                }
                break;

            // timestamp column
            case 1:
                if (cr1.getTimestamp() == cr2.getTimestamp()) {
                    returnValue = cr1.compareTo(cr2);
                } else {
                    returnValue = new Long(cr1.getTimestamp()).compareTo(cr2.getTimestamp());
                }
                break;

            // message column
            case 2:
                returnValue = cr1.getPayload().compareTo(cr2.getPayload());
                break;

            // component column
            case 3:
                returnValue = cr1.getComponentName().compareTo(cr2.getComponentName());
                break;
                
            // workflow column
            case 4:
                returnValue = cr1.getWorkflowName().compareTo(cr2.getWorkflowName());
                break;

            // shouldn't occur
            default:
                break;
            }

            // is DESCENDING? then flip sorting!
            if (SORT_DESCENDING == direction) {
                returnValue = -returnValue;
            }
        }

        return returnValue;
    }
}
