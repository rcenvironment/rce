/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Abstract class for SSHConnectionDialogs, such as AddSSHConnectionDialog and EditSSHConnectionDialog.
 * 
 * @author Brigitte Boden
 * @author Oliver Seebach
 */
public abstract class AbstractSshConnectionDialog extends Dialog {

    private static final int DIALOG_WINDOW_OFFSET_Y = 100;

    private static final int DIALOG_WINDOW_OFFSET_X = 150;

    private static final String INVALID_IP = "0.0.0.0";

    private static final String NAME_LABEL = "Name:";

    private static final String HOST_LABEL = "Host:";

    private static final String PORT_LABEL = "Port:";

    private static final String USERNAME_LABEL = "Username:";

    private static final String PASSPHRASE_LABEL = "Passphrase:";

    private static final int CHECKBOX_LABEL_WIDTH = 300;

    private static final String COLON = ":";

    protected String connectionName = "";

    protected String host = "";

    protected String port = "";

    protected String username = "";

    protected String passphrase = "";

    protected String hint = "";

    private boolean connectImmediately = true;

    private boolean storePassphrase = true;

    private Button useDefaultNameButton;

    private Button storePasswordButton;

    private Label nameLabel;

    private boolean isDefaultName = true;

    protected AbstractSshConnectionDialog(Shell parentShell) {
        super(parentShell);
    }

    public AbstractSshConnectionDialog(Shell parentShell, String connectionName, String host, int port,
        String username, boolean storePassphrase, boolean connectImmediately) {
        super(parentShell);
        this.connectionName = connectionName;
        this.host = host;
        this.port = Integer.toString(port);
        String hostAndPortString = StringUtils.format("%s:%s", host, port);
        this.username = username;
        this.connectImmediately = connectImmediately;
        this.storePassphrase = storePassphrase;
        this.isDefaultName = (connectionName.equals(hostAndPortString));
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        // remove scrollbar
        if (shell.getHorizontalBar() != null) {
            shell.getHorizontalBar().dispose();
        }
        // place shell in the middle of the screen
        shell.setLocation(shell.getParent().getLocation().x + shell.getParent().getSize().x / 2 - DIALOG_WINDOW_OFFSET_X,
            shell.getParent().getLocation().y + shell.getParent().getSize().y / 2 - DIALOG_WINDOW_OFFSET_Y);
        // set shell label to "Connection" as fallback
        shell.setText("Connection");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        GridLayout layout = new GridLayout(2, false);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, false, false);
        container.setLayoutData(containerGridData);
        container.setLayout(layout);

        GridData useDefaultCheckboxGridData = new GridData();
        useDefaultCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        useDefaultCheckboxGridData.horizontalSpan = 1;

        GridData storePassphraseCheckboxGridData = new GridData();
        storePassphraseCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        storePassphraseCheckboxGridData.horizontalSpan = 1;

        GridData connectImmediateCheckboxGridData = new GridData();
        connectImmediateCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        connectImmediateCheckboxGridData.horizontalSpan = 2;

        Label cpLabelHost = new Label(container, SWT.NULL);
        cpLabelHost.setText(HOST_LABEL);

        final Text hostTextField = new Text(container, SWT.SINGLE | SWT.BORDER);
        hostTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        if (!host.isEmpty()) {
            hostTextField.setText(host);
        }

        Label cpLabelPort = new Label(container, SWT.NULL);
        cpLabelPort.setText(PORT_LABEL);

        final Text portTextField = new Text(container, SWT.SINGLE | SWT.BORDER);
        portTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        if (!port.isEmpty()) {
            portTextField.setText(port);
        }

        portTextField.addVerifyListener(new VerifyListener() {

            @Override
            public void verifyText(VerifyEvent e) {
                String currentText = ((Text) e.widget).getText();
                String portID = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);

                final int maxPort = 65535;
                try {
                    int portNum = Integer.valueOf(portID);
                    if (portNum <= 0 || portNum > maxPort) {
                        e.doit = false;
                    }

                } catch (NumberFormatException ex) {
                    if (!portID.equals("")) {
                        e.doit = false;

                    }
                }
            }
        });

        GridData separatorGridData = new GridData();
        separatorGridData.horizontalAlignment = GridData.FILL;
        separatorGridData.grabExcessHorizontalSpace = true;
        separatorGridData.horizontalSpan = 2;

        nameLabel = new Label(container, SWT.NULL);
        nameLabel.setText(NAME_LABEL);

        final Text nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        nameText.setText(connectionName);
        nameText.setEnabled(!isDefaultName);

        @SuppressWarnings("unused") final Label placeholderLabel = new Label(container, SWT.NONE); // used for layouting

        useDefaultNameButton = new Button(container, SWT.CHECK);
        useDefaultNameButton.setSelection(true);
        useDefaultNameButton.setText("Use default name (host" + COLON + "port)");
        useDefaultNameButton.setLayoutData(useDefaultCheckboxGridData);
        useDefaultNameButton.setSelection(isDefaultName);

        Label usernameLabel = new Label(container, SWT.NULL);
        usernameLabel.setText(USERNAME_LABEL);

        final Text usernameText = new Text(container, SWT.SINGLE | SWT.BORDER);
        usernameText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        usernameText.setText(username);

        Label passphraseLabel = new Label(container, SWT.NULL);
        passphraseLabel.setText(PASSPHRASE_LABEL);

        final Text passphraseText = new Text(container, SWT.SINGLE | SWT.BORDER);
        passphraseText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        passphraseText.setText(passphrase);
        passphraseText.setEchoChar('*');

        @SuppressWarnings("unused") final Label placeholderLabel2 = new Label(container, SWT.NONE); // used for layouting

        storePasswordButton = new Button(container, SWT.CHECK);
        storePasswordButton.setSelection(true);
        storePasswordButton.setText("Store passphrase");
        storePasswordButton.setLayoutData(storePassphraseCheckboxGridData);
        storePasswordButton.setSelection(storePassphrase);

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(separatorGridData);

        final Button immediateConnectButton = new Button(container, SWT.CHECK);
        immediateConnectButton.setSelection(connectImmediately);
        immediateConnectButton.setText("Connect immediately");
        immediateConnectButton.setLayoutData(connectImmediateCheckboxGridData);

        final Label persistHint = new Label(container, SWT.NULL);
        GridData hintGridData = new GridData();
        hintGridData.horizontalSpan = 2;
        persistHint.setText(hint);
        persistHint.setLayoutData(hintGridData);

        initVariablesAndCreateListeners(hostTextField, portTextField, nameText, usernameText, passphraseText, immediateConnectButton);
        return container;
    }

    private void initVariablesAndCreateListeners(final Text hostTextField, final Text portTextField, final Text nameText,
        final Text usernameText,
        final Text passphraseText, final Button immediateConnectButton) {

        hostTextField.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                host = hostTextField.getText();
                if (useDefaultNameButton.getSelection()) {
                    if (!hostTextField.getText().isEmpty() && !portTextField.getText().isEmpty()) {
                        nameText.setText(hostTextField.getText() + COLON + portTextField.getText());
                    } else {
                        nameText.setText("");
                    }
                }
                updateOkButtonActivation();
            }
        });

        portTextField.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                port = portTextField.getText();
                if (useDefaultNameButton.getSelection()) {
                    if (!hostTextField.getText().isEmpty() && !portTextField.getText().isEmpty()) {
                        nameText.setText(hostTextField.getText() + COLON + portTextField.getText());
                    } else {
                        nameText.setText("");
                    }

                }
                updateOkButtonActivation();
            }
        });

        connectionName = nameText.getText();
        nameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                connectionName = nameText.getText();
            }
        });

        username = usernameText.getText();
        usernameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                username = usernameText.getText();
                updateOkButtonActivation();
            }
        });

        passphrase = passphraseText.getText();
        passphraseText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                passphrase = passphraseText.getText();
            }
        });

        connectImmediately = immediateConnectButton.getSelection();
        immediateConnectButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                connectImmediately = immediateConnectButton.getSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
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

        useDefaultNameButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (useDefaultNameButton.getSelection()) {
                    // if selection is enabled
                    nameText.setEnabled(false);

                    if (!hostTextField.getText().isEmpty() && !portTextField.getText().isEmpty()) {
                        nameText.setText(hostTextField.getText() + COLON + portTextField.getText());
                    } else {
                        nameText.setText("");
                    }

                } else {
                    // if selection is disabled
                    nameText.setEnabled(true);
                    nameText.setText("");
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetDefaultSelected(e);
            }
        });
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateOkButtonActivation();
    }

    @Override
    protected void okPressed() {

        if (host.contains(INVALID_IP)) {
            MessageDialog.openError(this.getParentShell(), "Invalid host address", "The IP address 0.0.0.0"
                + " configured for the network connection is invalid. For a local connection please use 127.0.0.1 or 'localhost' instead.");
            return;
        }
        super.okPressed();

    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control buttonBar = super.createButtonBar(parent);
        // pack shell after button bar is created to fit dialog window to proper size
        parent.getShell().pack();
        return buttonBar;
    }

    @Override
    protected void setShellStyle(int newShellStyle) {
        super.setShellStyle(SWT.OK | SWT.CANCEL | SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public String getUsername() {
        return username;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public boolean getConnectImmediately() {
        return connectImmediately;
    }

    protected void activateDefaultName() {
        isDefaultName = true;
    }

    protected void deactivateDefaultName() {
        isDefaultName = false;
    }

    public boolean getStorePassPhrase() {
        return storePassphrase;
    }

    private void updateOkButtonActivation() {
        getButton(IDialogConstants.OK_ID).setEnabled(!host.isEmpty() && !port.isEmpty() && !username.isEmpty());
    }
}
