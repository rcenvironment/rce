/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration;

import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Listener to open the "Choose Group" dialog to select a Palette Group and insert the selected group into the given text field.
 * 
 * @author Kathrin Schaffert
 */
public class GroupPathChooserButtonListener implements SelectionListener {

    private List<String> groupNames;

    private Text groupPathText;

    private Shell shell;

    public GroupPathChooserButtonListener(List<String> groupNames, Text groupPathText, Shell shell) {
        super();
        this.groupNames = groupNames;
        this.groupPathText = groupPathText;
        this.shell = shell;
    }

    @Override
    public void widgetSelected(SelectionEvent arg0) {
        showGroupSelectionDialog();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);
    }

    private void showGroupSelectionDialog() {
        ElementListSelectionDialog dlg =
            new ElementListSelectionDialog(
                shell,
                new LabelProvider());
        dlg.setElements(groupNames.toArray());
        dlg.setHelpAvailable(false);
        dlg.setMultipleSelection(false);
        dlg.setStatusLineAboveButtons(false);
        dlg.setMessage(Messages.chooseGroupDlgMessage);
        dlg.setTitle(Messages.chooseGroupDlgTitle);
        if (!groupPathText.getText().isEmpty() && groupNames.contains(groupPathText.getText())) {
            dlg.setInitialSelections(groupPathText.getText());
        }
        if (dlg.open() == Window.OK) {
            groupPathText.setText(dlg.getFirstResult().toString());
        }
    }
}
