/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.Comparator;

import org.eclipse.swt.SWT;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Helper class providing a Comparator to sort a given attribute in both ways. Used when setting up the content for the table.
 * {@link WorkflowDescriptionContentProvider}.
 * 
 * @author Goekhan Guerkan
 */
public class ComperatorHelper implements Comparator<WorkflowNode> {

    private int direction = SWT.UP;

    private String compareAttribute;

    public ComperatorHelper(int direction, String compareAttribute) {

        this.direction = direction;
        this.compareAttribute = compareAttribute;

    }

    @Override
    public int compare(WorkflowNode node1, WorkflowNode node2) {

        if (compareAttribute.equals(TableSortSelectionListener.COLUMN_NAME)) {

            String compname1 = node1.getName();
            String compname2 = node2.getName();

            if (direction == SWT.UP) {
                return compname1.compareTo(compname2);

            } else {

                return compname2.compareTo(compname1);

            }
        } else if (compareAttribute.equals(TableSortSelectionListener.COLUMN_INSTANCE)) {

            String compname1 = node1.getComponentDescription().getNode().toString();
            String compname2 = node2.getComponentDescription().getNode().toString();

            if (direction == SWT.UP) {
                return compname1.compareTo(compname2);

            } else {

                return compname2.compareTo(compname1);

            }

        } else {
            // this should never happen
            return 0;

        }
    }
}
