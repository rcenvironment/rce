/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.gui;

import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Component;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Discipline;
import de.rcenvironment.components.cpacs.vampzeroinitializer.gui.model.Parameter;

/**
 * The tree content provider.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public class ParameterTreeContentProvider implements ITreeContentProvider {

    /**
     * Current input object.
     */
    private Object input;

    @Override
    public Object[] getElements(final Object element) {
        if (element == null) {
            return null;
        }
        if (element instanceof Set<?>) {
            return ((Set<?>) element).toArray(new List<?>[] {}); // array of lists with only one
                                                                 // element
        }
        return null;
    }

    @Override
    public void dispose() {
        input = null;
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object from, final Object to) {
        input = to;
    }

    @Override
    public Object[] getChildren(final Object element) {
        Object[] ret = new Object[] {};
        
        if (element instanceof Set<?>) {
            ret = ((Set<?>) element).toArray(new List<?>[] {}); // array of lists with only one
                                                                 // element
        } else if (element instanceof List<?>) {
            ret = ((List<?>) element).toArray(new Component[] {});
        } else if (element instanceof Component) {
            ret = ((Component) element).getDisciplines().toArray(new Discipline[] {});
        } else if (element instanceof Discipline) {
            ret = ((Discipline) element).getParameters().toArray(new Parameter[] {});
        }
        
        return ret;
    }

    @Override
    public Object getParent(final Object element) {
        Object ret = null;
        
        if (element instanceof Set<?>) { // root element
            ret = null;
        } else if (element instanceof List<?>) {
            ret = input;
        } else if (element instanceof Component) {
            ret = ((Set<?>) input).iterator().next(); // the only list in the set is the parent of
                                                       // any component
        } else if (element instanceof Discipline) {
            ret = ((Discipline) element).getComponent();
        } else if (element instanceof Parameter) {
            ret = ((Parameter) element).getDiscipline();
        }
        return ret;
    }

    @Override
    public boolean hasChildren(final Object element) {
        boolean ret = false;
        if (element instanceof Set<?>) { // root element
            ret = true;
        } else if ((element instanceof List<?>) && (((List<?>) element).size() > 0)) {
            ret = true;
        } else if ((element instanceof Component) && (((Component) element).getDisciplines().size() > 0)) {
            ret = true;
        } else if ((element instanceof Discipline) && (((Discipline) element).getParameters().size() > 0)) {
            ret = true;
        }
        return ret;
    }

}
