/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.console;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Take the whole content to structured pieces.
 * 
 * @author Enrico Tappert
 * @author Doreen Seider
 */
public class ConsoleContentProvider implements IStructuredContentProvider {

    /**
     * {@inheritDoc}
     * 
     * Splits the whole content into structured logging elements object array.
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof Collection<?>) {
            return ((Collection<ConsoleRow>) inputElement).toArray();
        } else {
            // empty default
            return new Object[] {};
        }
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // do nothing
    }

}
