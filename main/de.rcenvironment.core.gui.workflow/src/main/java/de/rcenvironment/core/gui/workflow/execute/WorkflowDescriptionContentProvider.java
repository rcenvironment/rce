/*
 * Copyright (C) 2006-2014 DLR, Germany
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
 */
final class WorkflowDescriptionContentProvider implements IStructuredContentProvider {

    @Override
    public Object[] getElements(Object element) {
        if (element instanceof WorkflowDescription) {
            List<WorkflowNode> items = ((WorkflowDescription) element).getWorkflowNodes();
            Collections.sort(items);
            return items.toArray();
        }

        return new Object[] {};
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
    }

}
