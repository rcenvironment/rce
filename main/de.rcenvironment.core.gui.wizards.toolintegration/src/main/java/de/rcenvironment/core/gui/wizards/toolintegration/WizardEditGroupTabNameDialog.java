/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for renaming a configuration group.
 * 
 * @author Sascha Zur
 */
public class WizardEditGroupTabNameDialog extends Dialog {

    private String oldName;

    private String newName;

    protected WizardEditGroupTabNameDialog(Shell parentShell, String oldName) {
        super(parentShell);
        this.oldName = oldName;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
        new Label(content, SWT.None).setText(Messages.newGroupName);
        final Text newNameText = new Text(content, SWT.BORDER);
        newNameText.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL));
        newNameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                newName = newNameText.getText();
                validate();
            }

        });
        newNameText.setText(oldName);
        return content;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.edit);
    }

    private void validate() {
        boolean enabled = true;
        if (newName == null || newName.isEmpty()) {
            enabled = false;
        }
        if (getButton(IDialogConstants.OK_ID) != null) {
            getButton(IDialogConstants.OK_ID).setEnabled(enabled);

        }
    }

    public String getNewName() {
        return newName;
    }
}
