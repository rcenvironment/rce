/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters the view with the given String in the TextField.
 * 
 * @author Goekhan Guerkan
 */
public class Filter extends ViewerFilter {

    private String searchString;

    private Updatable updater;

    public Filter(Updatable updater) {

        this.updater = updater;
    }

    @Override
    public boolean select(Viewer arg0, Object contentProvider, Object element) {

        return updater.useFilter(searchString, element);

    }

    public void setSearchText(String text) {

        this.searchString = text;

    }

}
