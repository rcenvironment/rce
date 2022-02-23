/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;

import de.rcenvironment.components.excel.common.ChannelValue;


/**
 * Double click listener for table viewer.
 *
 * @author Markus Kunde
 */
public class DoubleClickListener implements IDoubleClickListener {
    
    /**
     * Parent composite.
     */
    private Composite parent = null;
    
    /**
     * Constructor.
     * 
     * @param composite parent composite
     */
    public DoubleClickListener(final Composite composite) {
        parent = composite;
    }
    
    @Override
    public void doubleClick(DoubleClickEvent event) {
        IStructuredSelection sel = (IStructuredSelection) event.getSelection();
        final ChannelValue cval = (ChannelValue) sel.getFirstElement();
        if (cval != null){
            
            parent.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    Dialog dialog = new DoubleClickDialog(parent.getShell(), cval);
                    dialog.open();
                }
            });
        }
    }
}
