/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.util.SortedSet;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.rcenvironment.core.log.SerializableLogEntry;

/**
 * Take the whole content to structured pieces.
 * 
 * @author Enrico Tappert
 */
public class LogContentProvider implements IStructuredContentProvider {

    /**
     * {@inheritDoc}
     * 
     * Splits the whole content into structured logging elements object array.
     * 
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        if (!(inputElement instanceof LogModel)) {
            throw new IllegalArgumentException();
        }
        final LogModel logModel = (LogModel) inputElement;
        final SortedSet<SerializableLogEntry> logEntries = logModel.getLogEntries();
        return logEntries.toArray();
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
