/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Dialog that helps to manage connections.
 *
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Hendrik Abbenhaus
 */
public class ConnectionDialog extends Dialog {

    private ConnectionDialogComposite connectionDialogComposite;

    private CLabel autoConnectInfoLabel;

    public ConnectionDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.RESIZE | SWT.MAX | SWT.PRIMARY_MODAL);
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        connectionDialogComposite = new ConnectionDialogComposite(parent, SWT.NONE);

        GridLayout gridLayoutComposite = new GridLayout(3, false);
        gridLayoutComposite.horizontalSpacing = 0;
        gridLayoutComposite.marginWidth = 0;
        gridLayoutComposite.marginHeight = 0;
        connectionDialogComposite.setLayout(gridLayoutComposite);

        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        connectionDialogComposite.setLayoutData(gridData);
        

        autoConnectInfoLabel = new CLabel(connectionDialogComposite, SWT.NULL);
        GridData gridLayout4 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
        autoConnectInfoLabel.setLayoutData(gridLayout4);
        autoConnectInfoLabel.setText(Messages.autoConnectInfoText);
        autoConnectInfoLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));

        return parent;
    }

    public ConnectionDialogComposite getConnectionDialogComposite() {
        return connectionDialogComposite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Point getInitialSize() {
        final int width = 512;
        final int height = 384;
        return new Point(width, height);
    }

    public CLabel getAutoConnectInfoLabel() {
        return autoConnectInfoLabel;
    }

}
