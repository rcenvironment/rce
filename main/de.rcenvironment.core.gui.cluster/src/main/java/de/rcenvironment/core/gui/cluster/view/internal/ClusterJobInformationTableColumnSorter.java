/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.view.internal;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.rcenvironment.core.utils.cluster.ClusterJobInformation;

/**
 * Provides functionality of sorting a table column. 
 * You can choose which column to sort on and in which direction. 
 * 
 * @author Doreen Seider
 */
public class ClusterJobInformationTableColumnSorter extends ViewerSorter {

    private static final int FIRST_IS_EQUAL = 0;

    private static final int SORT_ASCENDING = 1;

    private static final int SORT_DESCENDING = 2;

    private int direction;

    private int columnToSort;

    /**
     * Sets the default sorting column and direction.
     */
    public ClusterJobInformationTableColumnSorter() {
        columnToSort = 3;
        direction = SORT_DESCENDING;
    }

    /**
     * Lets set another column than default one to sort on.
     * 
     * @param column The index (beginning with 0) of column to sort.
     */
    public void setColumn(int column) {

        if (column == columnToSort) {
            if (SORT_ASCENDING == direction) {
                direction = SORT_DESCENDING;
            } else {
                direction = SORT_ASCENDING;
            }
        } else {
            columnToSort = column;
            direction = SORT_ASCENDING;
        }
    }

    @Override
    public int compare(Viewer viewer, Object object1, Object object2) {

        int returnValue = FIRST_IS_EQUAL;

        if (object1 instanceof ClusterJobInformation && object2 instanceof ClusterJobInformation) {
            ClusterJobInformation jobInformation1 = (ClusterJobInformation) object1;
            ClusterJobInformation jobInformation2 = (ClusterJobInformation) object2;

            switch (columnToSort) {
            case 0:
                returnValue = jobInformation1.getJobId().compareTo(jobInformation2.getJobId());
                break;
            case 1:
                returnValue = jobInformation1.getJobName().compareTo(jobInformation2.getJobName());
                break;
            case 2:
                returnValue = jobInformation1.getUser().compareTo(jobInformation2.getUser());
                break;
            case 3:
                returnValue = jobInformation1.getQueue().toString().compareTo(jobInformation2.getQueue().toString());
                break;
            case 4:
                returnValue = jobInformation1.getRemainingTime().compareTo(jobInformation2.getRemainingTime());
                break;
            case 5:
                returnValue = jobInformation1.getStartTime().compareTo(jobInformation2.getStartTime());
                break;
            case 6:
                returnValue = jobInformation1.getQueueTime().compareTo(jobInformation2.getQueueTime());
                break;
            case 7:
                returnValue = jobInformation1.getJobState().compareTo(jobInformation2.getJobState());
                break;
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
