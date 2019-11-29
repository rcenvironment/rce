/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.list;

import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


/**
 * ContentProvider for a list of WorkflowInformation objects.
 *
 * @author Heinrich Wendel
 */
public class WorkflowInformationContentProvider implements IStructuredContentProvider {

    @SuppressWarnings("rawtypes")
    @Override
    public Object[] getElements(Object element) {
        if (element instanceof Set) {
            return ((Set) element).toArray();
        } else {
            return new Object[] {};
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
    }

}
