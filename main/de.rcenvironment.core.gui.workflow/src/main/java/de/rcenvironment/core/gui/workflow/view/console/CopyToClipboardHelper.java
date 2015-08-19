/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.execution.api.ConsoleRow;


/**
 * Helper class that handles copying of the current selection to the clipboard.
 *
 * @author Oliver Seebach
 */
public class CopyToClipboardHelper {

    private TableViewer tableViewer;
    
    public CopyToClipboardHelper(TableViewer aTableViewer) {
        tableViewer = aTableViewer;    
    }
    
    /**
     * Copies the current selection to the clipboard.
     * 
     */
    public void copyToClipboard(){
        Clipboard cb = new Clipboard(Display.getDefault());
        ISelection selection = tableViewer.getSelection();
        List<ConsoleRow> consoleRows = new ArrayList<ConsoleRow>();
        if (selection != null && selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection) selection;
            for (@SuppressWarnings("unchecked")
                Iterator<ConsoleRow> iterator = sel.iterator(); iterator.hasNext();) {
                ConsoleRow row = iterator.next();
                consoleRows.add(row);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (ConsoleRow row : consoleRows) {
            sb.append(row.getPayload() + System.getProperty("line.separator")); //$NON-NLS-1$
        }
        TextTransfer textTransfer = TextTransfer.getInstance();
        cb.setContents(new Object[] { sb.toString() }, new Transfer[] { textTransfer });    
    }
    
}
