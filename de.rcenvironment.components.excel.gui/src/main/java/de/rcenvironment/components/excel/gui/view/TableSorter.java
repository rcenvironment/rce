/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.rcenvironment.components.excel.common.ChannelValue;


/**
 * Table sorter.
 *
 * @author Markus Kunde
 */
public class TableSorter extends ViewerSorter {
    
    // private static final int ASCENDING = 0;
    private static final int DESCENDING = 1;
    
    private int propertyIndex;
   
    private int direction = DESCENDING;

    public TableSorter() {
        this.propertyIndex = 0;
        direction = DESCENDING;
    }

    /**
     * Set column of sorter.
     * 
     * @param column the column
     */
    public void setColumn(final int column) {
        if (column == this.propertyIndex) {
            // Same column as last sort; toggle the direction
            direction = 1 - direction;
        } else {
            // New column; do an ascending sort
            this.propertyIndex = column;
            direction = DESCENDING;
        }
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        ChannelValue channelValue1 = (ChannelValue) e1;
        ChannelValue channelValue2 = (ChannelValue) e2;
        int rc = 0;
        switch (propertyIndex) {
        case 0:
            String a = channelValue1.getValues().toString();
            String b = channelValue2.getValues().toString();
            rc = a.compareTo(b);
            break;
        case 1:
            rc = channelValue1.getChannelName().compareTo(channelValue2.getChannelName());         
            break;
        case 2:
            rc = (int) (channelValue1.getIteration() - channelValue2.getIteration());
            break;
        case 3:
            rc = Boolean.valueOf(channelValue1.isInputValue()).compareTo(Boolean.valueOf(channelValue2.isInputValue()));
            break;
        default:
            rc = 0;
        }
        // If descending order, flip the direction
        if (direction == DESCENDING) {
            rc = -rc;
        }
        return rc;
    }
}
