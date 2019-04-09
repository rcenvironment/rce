/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;



/**
 * Dialog for entering passphrase for an SSH connection.
 *
 * @author Brigitte Boden
 */
public class EnterPassphraseDialog extends Dialog {
    
    private static final int CHECKBOX_LABEL_WIDTH = 300;
    
    private static final String DIALOG_TITLE = "Enter passphrase";
    
    private Button storePasswordButton;
    
    private String passphrase = "";
    
    private boolean storePassphrase = false;

    protected EnterPassphraseDialog(Shell parentShell) {
        super(parentShell);
        
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        GridLayout layout = new GridLayout(2, false);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, false, false);
        container.setLayoutData(containerGridData);
        container.setLayout(layout);

        final Text passphraseText = new Text(container, SWT.SINGLE | SWT.BORDER);
        passphraseText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        passphraseText.setText(passphrase);
        passphraseText.setEchoChar('*');
        
        @SuppressWarnings("unused") final Label placeholderLabel = new Label(container, SWT.NONE); // used for layouting

        GridData storePassphraseCheckboxGridData = new GridData();
        storePassphraseCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        storePassphraseCheckboxGridData.horizontalSpan = 1;

        storePasswordButton = new Button(container, SWT.CHECK);
        storePasswordButton.setText("Store passphrase");
        storePasswordButton.setLayoutData(storePassphraseCheckboxGridData);
        storePasswordButton.setSelection(storePassphrase);
        
        passphrase = passphraseText.getText();
        passphraseText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                passphrase = passphraseText.getText();
            }
        });
        
        storePassphrase = storePasswordButton.getSelection();
        storePasswordButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                storePassphrase = storePasswordButton.getSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        
        return container;
    }
    
    public String getPassphrase() {
        return passphrase;
    }
    
    public boolean getStorePassphrase(){
        return storePassphrase;
    }

}
