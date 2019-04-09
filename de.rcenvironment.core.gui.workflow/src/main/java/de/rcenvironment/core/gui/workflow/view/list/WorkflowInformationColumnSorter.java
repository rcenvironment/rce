/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.list;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;

/**
 * Provides functionality of sorting a table column. 
 * You can choose which column to sort on and in which direction. 
 * 
 * @author Doreen Seider
 */
public class WorkflowInformationColumnSorter extends ViewerSorter {

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
    public WorkflowInformationColumnSorter() {
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

        if (object1 instanceof WorkflowExecutionInformation && object2 instanceof WorkflowExecutionInformation) {
            WorkflowExecutionInformation p1 = (WorkflowExecutionInformation) object1;
            WorkflowExecutionInformation p2 = (WorkflowExecutionInformation) object2;

            switch (columnToSort) {

            // name column
            case 0:
                returnValue = p1.getInstanceName().compareToIgnoreCase(p2.getInstanceName());
                break;

            // status column
            case 1:
                WorkflowState state1 = WorkflowStateModel.getInstance().getState(p1.getExecutionIdentifier());
                WorkflowState state2 = WorkflowStateModel.getInstance().getState(p2.getExecutionIdentifier());
                returnValue = state1.getDisplayName().compareTo(state2.getDisplayName());
                break;

            // node column
            case 2:
                if (p1.getNodeId() == null) {
                    returnValue = FIRST_IS_GREATER;
                } else if (p2.getNodeId() == null) {
                    returnValue = FIRST_IS_LESS;
                } else {
                    returnValue = p1.getNodeId().getAssociatedDisplayName()
                        .compareTo(p2.getNodeId().getAssociatedDisplayName());
                }
                break;
                
            // time column
            case 3:
                if (p1.getStartTime() == p2.getStartTime()) {
                    returnValue = FIRST_IS_EQUAL;
                } else if (p1.getStartTime() > p2.getStartTime()) {
                    returnValue = FIRST_IS_GREATER;
                } else {
                    returnValue = FIRST_IS_LESS;
                }
                break;

             // start node column
            case 4:
                if (p1.getNodeIdStartedExecution() == null) {
                    returnValue = FIRST_IS_GREATER;
                } else if (p2.getNodeIdStartedExecution() == null) {
                    returnValue = FIRST_IS_LESS;
                } else {
                    returnValue = p1.getNodeIdStartedExecution().getAssociatedDisplayName()
                        .compareTo(p2.getNodeIdStartedExecution().getAssociatedDisplayName());
                }
                break;

            // add info column
            case 5:
                if (p1.getAdditionalInformationProvidedAtStart() == null) {
                    returnValue = FIRST_IS_GREATER;
                } else if (p2.getAdditionalInformationProvidedAtStart() == null) {
                    returnValue = FIRST_IS_LESS;
                } else {
                    returnValue = p1.getAdditionalInformationProvidedAtStart()
                        .compareTo(p2.getAdditionalInformationProvidedAtStart());
                }
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
