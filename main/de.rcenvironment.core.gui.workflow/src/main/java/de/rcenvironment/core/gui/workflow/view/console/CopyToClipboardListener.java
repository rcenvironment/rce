/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.console;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * Class responsible for copying log entries to the clipboard.
 *
 * @author Doreen Seider
 */
public class CopyToClipboardListener implements SelectionListener {

    private TableViewer tableViewer;
    
    public CopyToClipboardListener(TableViewer aTableViewer) {
        tableViewer = aTableViewer;
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        CopyToClipboardHelper helper = new CopyToClipboardHelper(tableViewer);
        String copyCommand = e.getSource().toString();
        helper.copyToClipboard(copyCommand);
        
    }

}
