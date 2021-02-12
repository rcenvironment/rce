/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.gui.xpathchooser.model.XSDDocument;
import de.rcenvironment.core.gui.xpathchooser.model.XSDElement;


/**
 * ContentProvider for the XSD TreeView.
 * 
 * @author Heinrich Wendel
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class XSDContentProvider implements ITreeContentProvider {

    /**
     * Get children model elements.
     * We don't allow XSDDocument and XSDAttribute here.
     * @param object The object
     * @return The model elements or empty array
     */
    @Override
    public Object[] getChildren(final Object object) {
        final Object[] objects;
        if (object instanceof XSDElement) {
            objects = ((XSDElement) object).getElements().toArray();
        } else {
            objects = new Object[] { };
        }
        return objects;
    }

    /**
     * Get the parent model element.
     * @param object The object
     * @return The model element or null
     */
    @Override
    public Object getParent(final Object object) {
        final Object parent;
        if (object instanceof XSDElement) {
            parent = ((XSDElement) object).getParent();
        } else {
            parent = null;
        }
        return parent;
    }

    /**
     * Says if there are children.
     * @param object The model element
     * @return True if children available, false if no children
     */
    @Override
    public boolean hasChildren(final Object object) {
        final boolean children;
        if (object instanceof XSDElement) {
            XSDElement element = (XSDElement) object;
            if (element.getElements().size() > 0) {
                children = true;
            } else {
                children = false;
            }
        } else if (object instanceof XSDDocument) {
            children = ((XSDDocument) object).getElements().size() != 0;
        } else {
            children = false;
        }
        return children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object object) {
        return getChildren(object);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(final Viewer arg0, final Object oldInput, final Object newInput) {
    }

}
