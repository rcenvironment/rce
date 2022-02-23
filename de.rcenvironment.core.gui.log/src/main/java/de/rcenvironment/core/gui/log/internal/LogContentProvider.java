/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.service.log.LogService;

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

        List<SerializableLogEntry> removeObjectsList = new ArrayList<SerializableLogEntry>();
        final LogModel logModel = (LogModel) inputElement;
        final SortedSet<SerializableLogEntry> logEntries = logModel.getLogEntries();

        for (SerializableLogEntry entry : logEntries) {
            if (entry.getLevel() == LogService.LOG_DEBUG) {
                removeObjectsList.add(entry);
            }
        }

        logEntries.removeAll(removeObjectsList);

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
