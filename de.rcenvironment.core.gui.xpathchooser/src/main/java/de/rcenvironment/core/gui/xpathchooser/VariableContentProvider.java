/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * ContentProvider for the XSD TreeView.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class VariableContentProvider implements IStructuredContentProvider {

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(final Object object) {
        if (object == null) {
            return new Object[] {};
        }
        if (object instanceof List) {
            return ((List<?>) object).toArray(new VariableEntry[] {});
        }
        return new Object[] {};
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {}

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(final Viewer arg0, final Object oldInput, final Object newInput) {}

}
