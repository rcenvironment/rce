/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.log.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.log.SerializableLogEntry;

/**
 * Class responsible for copying log entries to the clipboard.
 *
 * @author Doreen Seider
 */
public class CopyToClipboardListener implements SelectionListener {

    private TableViewer myTableViewer;
    
    public CopyToClipboardListener(TableViewer tableViewer) {
        myTableViewer = tableViewer;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        
        ISelection selection = myTableViewer.getSelection();
        List<SerializableLogEntry> logEntries = new ArrayList<SerializableLogEntry>();
        if (selection != null && selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection) selection;
            for (@SuppressWarnings("unchecked")
                Iterator<SerializableLogEntry> iterator = sel.iterator(); iterator.hasNext();) {
                SerializableLogEntry logEntry = iterator.next();
                logEntries.add(logEntry);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (SerializableLogEntry logEntry : logEntries) {
            sb.append(logEntry.toString() + System.getProperty("line.separator")); //$NON-NLS-1$
        }
        
        ClipboardHelper.setContent(sb.toString());
    }

}
