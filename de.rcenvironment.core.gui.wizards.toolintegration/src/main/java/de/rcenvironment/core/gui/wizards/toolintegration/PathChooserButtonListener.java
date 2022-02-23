/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;

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

    private static final String OPEN_PATH = "Open path...";

    private Text linkedTextfield;

    private Shell shell;

    private boolean directoryDialog;

    private File openPath = null;

    public PathChooserButtonListener(Text linkedTextfield, boolean isDirectoryDialog, Shell shell) {
        this.linkedTextfield = linkedTextfield;
        this.shell = shell;
        directoryDialog = isDirectoryDialog;
    }

    @Override
    public void widgetSelected(SelectionEvent arg0) {
        String selectedPath;
        if (!directoryDialog) {
            if (openPath != null) {
                selectedPath = PropertyTabGuiHelper.selectFileFromFileSystem(shell,
                    new String[] { "*.*" }, OPEN_PATH, openPath.getAbsolutePath());
            } else {
                selectedPath = PropertyTabGuiHelper.selectFileFromFileSystem(shell,
                    new String[] { "*.*" }, OPEN_PATH);
            }
        } else {
            selectedPath = PropertyTabGuiHelper.selectDirectoryFromFileSystem(shell, OPEN_PATH);
        }
        if (selectedPath != null) {
            linkedTextfield.setText(selectedPath);
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);

    }

    public void setOpenPath(File path) {
        openPath = path;
    }
}
