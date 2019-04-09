/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A {@link org.eclipse.jface.dialogs.Dialog} to display a {@link BeanConfigurationWidget}.
 * 
 * @author Christian Weiss
 */
public class BeanConfigurationDialog extends BeanPropertyDialog {

    /**
     * Instantiates a new {@link BeanConfigurationDialog}.
     * 
     * @param parentShell the parent shell
     */
    public BeanConfigurationDialog(Shell parentShell) {
        super(parentShell);
    }

    /**
     * Instantiates a new {@link BeanConfigurationDialog}.
     * 
     * @param parentShell the parent shell
     */
    public BeanConfigurationDialog(IShellProvider parentShell) {
        super(parentShell);
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.utils.common.configuration.BeanPropertyDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        final BeanConfigurationWidget contents = new BeanConfigurationWidget(parent, SWT.NONE);
        contents.setObject(getObject());
        return contents;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        final Control contents = getContents();
        if (contents instanceof BeanConfigurationWidget) {
            ((BeanConfigurationWidget) contents).resetFocus();
        }
    }

}
