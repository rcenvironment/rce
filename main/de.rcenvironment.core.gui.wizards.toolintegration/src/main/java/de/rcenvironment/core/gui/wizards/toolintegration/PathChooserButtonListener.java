/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;

/**
 * Listener for all Buttons that should open a filessystem file dialog and put the selected path
 * into the given text field. If the isDirectoryDialog boolean is true, a DirectoryDialog will open
 * instead.
 * 
 * @author Sascha Zur
 */
public class PathChooserButtonListener implements SelectionListener {

    private Text linkedTextfield;

    private Shell shell;

    private boolean directoryDialog;

    public PathChooserButtonListener(Text linkedTextfield, boolean isDirectoryDialog, Shell shell) {
        this.linkedTextfield = linkedTextfield;
        this.shell = shell;
        directoryDialog = isDirectoryDialog;
    }

    @Override
    public void widgetSelected(SelectionEvent arg0) {
        String selectedPath;
        if (!directoryDialog) {
            selectedPath = PropertyTabGuiHelper.selectFileFromFileSystem(shell,
                new String[] { "*.*" }, "Open path...");
        } else {
            selectedPath = PropertyTabGuiHelper.selectDirectoryFromFileSystem(shell, "Open path...");
        }
        if (selectedPath != null) {
            linkedTextfield.setText(selectedPath);
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);

    }
}
