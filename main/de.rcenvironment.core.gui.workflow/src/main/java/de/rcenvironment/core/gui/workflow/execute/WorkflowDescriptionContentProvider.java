/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Returns a list of all workflow nodes to be shown in the target platform selection.
 *
 * @author Heinrich Wendel
 * @author Goekhan Guerkan
 */
final class WorkflowDescriptionContentProvider implements IStructuredContentProvider {

    private String sortBy;

    private int direction;

    WorkflowDescriptionContentProvider(int direction, String sortBy) {
        this.sortBy = sortBy;
        this.direction = direction;
    }

    @Override
    public Object[] getElements(Object element) {
        if (element instanceof WorkflowDescription) {
            List<WorkflowNode> items = ((WorkflowDescription) element).getWorkflowNodes();

            if (sortBy.equals(TableSortSelectionListener.COLUMN_INSTANCE)) {

                Collections.sort(items, new ComperatorHelper(direction, TableSortSelectionListener.COLUMN_INSTANCE));

            } else {

                Collections.sort(items, new ComperatorHelper(direction, TableSortSelectionListener.COLUMN_NAME));
            }

            return items.toArray();

        }
        // this should never happen
        return new Object[] {};
    }

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

}
