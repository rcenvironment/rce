/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.utils.incubator.PasteListeningText;
import de.rcenvironment.core.gui.utils.incubator.PasteListeningText.PasteListener;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Abstract class for SSHConnectionDialogs, such as AddSSHConnectionDialog and EditSSHConnectionDialog.
 * 
 * @author Brigitte Boden
 * @author Oliver Seebach
 */
public abstract class AbstractSshConnectionDialog extends Dialog implements PasteListener, VerifyListener {
    
    private static final String KEYFILE_AUTH_WITH_PASSPHRASE = "Keyfile with passphrase protection";

    private static final String KEYFILE_AUTH_WITHOUT_PASSPHRASE = "Keyfile without passphrase protection";

    private static final String PASSPHRASE_AUTH = "Passphrase";

    private static final int DIALOG_WINDOW_OFFSET_Y = 100;

    private static final int DIALOG_WINDOW_OFFSET_X = 150;
    
    private static final String COLON = ":";

    private static final String NAME_LABEL = "Name" + COLON;

    private static final String HOST_LABEL = "Host" + COLON;

    private static final String PORT_LABEL = "Port" + COLON;

    private static final String USERNAME_LABEL = "Username:";

    private static final String AUTH_TYPE_LABEL = "Authentication type:";

    private static final String KEYFILE_LABEL = "SSH key file:";

    private static final String PASSPHRASE_LABEL = "Passphrase:";

    private static final int CHECKBOX_LABEL_WIDTH = 300;
    
    private static Text portTextField;
    
    private static PasteListeningText hostTextField;

    protected String connectionName = "";

    protected String host = "";

    protected String port = "";

    protected String username = "";

    protected String passphrase = "";

    protected String keyfileLocation = "";

    protected String hint = "";
    
    protected String networkContactPointID = "";
    
    private boolean storePassphrase = true;

    private boolean useKeyFile = false;

    private boolean connectImmediately = true;

    private boolean usePassphrase = true;

    private Button useDefaultNameButton;
    
    private Button storePasswordButton;
    
    private Label nameLabel;

    private boolean isDefaultName = true;

    protected AbstractSshConnectionDialog(Shell parentShell) {
        super(parentShell);
    }

    public AbstractSshConnectionDialog(Shell parentShell, String connectionName, String host, int port,
        String username, String keyfileLocation, boolean usePassphrase, boolean storePassphrase, boolean connectImmediately) {
        super(parentShell);
        this.connectionName = connectionName;
        this.host = host;
        this.port = Integer.toString(port);
        String hostAndPortString = StringUtils.format("%s:%s", host, port);
        this.username = username;
        this.connectImmediately = connectImmediately;
        this.storePassphrase = storePassphrase;
        this.isDefaultName = (connectionName.equals(hostAndPortString));
        this.keyfileLocation = keyfileLocation;
        if (keyfileLocation == null) {
            this.keyfileLocation = "";
        }
        this.useKeyFile = !this.keyfileLocation.isEmpty();
        this.usePassphrase = usePassphrase;
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
        hostTextField = new PasteListeningText(container, SWT.SINGLE | SWT.BORDER);
        hostTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        if (!host.isEmpty()) {
            hostTextField.setText(host); 
        }
        hostTextField.addPasteListener(this);
        Label cpLabelPort = new Label(container, SWT.NULL);
        cpLabelPort.setText(PORT_LABEL);
        portTextField = new Text(container, SWT.SINGLE | SWT.BORDER);
        portTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        if (!port.isEmpty()) {
            portTextField.setText(port);  
        }
        portTextField.addVerifyListener(this);
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

        final Label placeholderLabel = new Label(container, SWT.NONE); // used for layouting
        placeholderLabel.setText("");
       
        useDefaultNameButton = new Button(container, SWT.CHECK);
        useDefaultNameButton.setText("Use default name (host" + COLON + "port)");
        useDefaultNameButton.setLayoutData(useDefaultCheckboxGridData);
        useDefaultNameButton.setSelection(isDefaultName);
        useDefaultNameButton.addSelectionListener(new SelectionListener() {
        
            @Override
            public void widgetSelected(SelectionEvent e) {
                nameText.setEnabled(!useDefaultNameButton.getSelection());
                nameText.setText("");
                if (useDefaultNameButton.getSelection()) {
                    if (!hostTextField.getText().isEmpty() 
                        && !portTextField.getText().isEmpty()) {
                        nameText.setText(hostTextField.getText() 
                            + COLON + portTextField.getText());
                    }
                }
                updateOkButtonActivation();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetDefaultSelected(e);
            }
        });

        Label usernameLabel = new Label(container, SWT.NULL);
        usernameLabel.setText(USERNAME_LABEL);
        final Text usernameText = new Text(container, SWT.SINGLE | SWT.BORDER);
        usernameText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        usernameText.setText(username);

        Label authTypeLabel = new Label(container, SWT.NULL);
        authTypeLabel.setText(AUTH_TYPE_LABEL);

        final Combo authTypeCombo = new Combo(container, SWT.READ_ONLY);
        String[] authTypes = { PASSPHRASE_AUTH, KEYFILE_AUTH_WITH_PASSPHRASE, KEYFILE_AUTH_WITHOUT_PASSPHRASE };
        authTypeCombo.setItems(authTypes);
        if (useKeyFile) {
            if (usePassphrase) {
                authTypeCombo.select(1);
            } else {
                authTypeCombo.select(2);
            }
        } else {
            authTypeCombo.select(0);
        }

        final Label keyfileLabel = new Label(container, SWT.NULL);
        keyfileLabel.setText(KEYFILE_LABEL);
        keyfileLabel.setVisible(useKeyFile);

        final Composite keyfileComposite = new Composite(container, SWT.NONE);
        GridLayout klayout = new GridLayout(2, false);
        keyfileComposite.setLayout(klayout);
        keyfileComposite.setVisible(useKeyFile);
        keyfileComposite.setLayoutData((new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL)));

        final Text keyfileText = new Text(keyfileComposite, SWT.SINGLE | SWT.BORDER);
        keyfileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        keyfileText.setText(keyfileLocation);

        createFileChooserButton(parent, keyfileComposite, keyfileText);

        final Label passphraseLabel = new Label(container, SWT.NULL);
        passphraseLabel.setText(PASSPHRASE_LABEL);
        passphraseLabel.setVisible(usePassphrase);

        final Text passphraseText = new Text(container, SWT.SINGLE | SWT.BORDER);
        passphraseText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        passphraseText.setText(passphrase);
        passphraseText.setEchoChar('*');
        passphraseText.setVisible(usePassphrase);

        @SuppressWarnings("unused") final Label placeholderLabel2 = new Label(container, SWT.NONE); // used for layouting

        storePasswordButton = new Button(container, SWT.CHECK);
        storePasswordButton.setSelection(true);
        storePasswordButton.setText("Store passphrase");
        storePasswordButton.setLayoutData(storePassphraseCheckboxGridData);
        storePasswordButton.setSelection(storePassphrase);
        storePasswordButton.setVisible(usePassphrase);

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

        defineListenerForAuthenticationType(authTypeCombo, keyfileLabel, keyfileComposite, passphraseLabel, passphraseText);

        initVariablesAndCreateListeners(hostTextField, portTextField, nameText, usernameText, passphraseText, keyfileText,
            immediateConnectButton);
        return container;
    }


    
    /**
    *
    * Paste host and port.
    * @param text String
    * 
    **/
    @Override
    public void paste(String text) {
        CustomPasteHandler.paste(text, hostTextField, portTextField);
    }

    @Override
    public void verifyText(VerifyEvent e) {
        String currentText = ((Text) e.widget).getText();
        String portID = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);

        e.doit = CustomPasteHandler.isValidPort(portID);
    }

    private void defineListenerForAuthenticationType(final Combo authTypeCombo, final Label keyfileLabel, final Composite keyfileComposite,
        final Label passphraseLabel, final Text passphraseText) {
        authTypeCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                useKeyFile =
                    authTypeCombo.getText().equals(KEYFILE_AUTH_WITH_PASSPHRASE)
                        || authTypeCombo.getText().equals(KEYFILE_AUTH_WITHOUT_PASSPHRASE);
                usePassphrase = !authTypeCombo.getText().equals(KEYFILE_AUTH_WITHOUT_PASSPHRASE);
                keyfileLabel.setVisible(useKeyFile);
                keyfileComposite.setVisible(useKeyFile);
                passphraseLabel.setVisible(usePassphrase);
                passphraseText.setVisible(usePassphrase);
                storePasswordButton.setVisible(usePassphrase);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
    }

    private void createFileChooserButton(final Composite parent, Composite container, final Text keyfileText) {
        final Button keyfileButton = new Button(container, SWT.NONE);
        keyfileButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
            false, 1, 1));
        keyfileButton.setText("...");
        keyfileButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                FileDialog dialog = new FileDialog(parent.getShell());
                dialog.setText("Choose private key file");
                String result = dialog.open();
                if (result != null) {
                    keyfileText.setText(result);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
    }

    private void initVariablesAndCreateListeners(final PasteListeningText hostTextFieldInit, final Text portTextFieldInit,
        final Text nameTextInit,  final Text usernameText, final Text passphraseText,
        final Text keyfileText, final Button immediateConnectButton) {
      
        hostTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                host = hostTextField.getText();
                if (useDefaultNameButton.getSelection()) {
                    if (!hostTextField.getText().isEmpty()
                        && !portTextField.getText().isEmpty()) {
                        nameTextInit.setText(hostTextField.getText()
                            + COLON + portTextField.getText());
                    } else {
                        nameTextInit.setText("");
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
                    if (!hostTextField.getText().isEmpty()
                        && !portTextField.getText().isEmpty()) {
                        nameTextInit.setText(hostTextField.getText()
                            + COLON + portTextField.getText());
                    } else {
                        nameTextInit.setText("");
                    }
                }
                updateOkButtonActivation();
            }
        });

        connectionName = nameTextInit.getText();
        nameTextInit.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                connectionName = nameTextInit.getText();
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

        keyfileLocation = keyfileText.getText();
        keyfileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                keyfileLocation = keyfileText.getText();
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
                    nameTextInit.setEnabled(false);
                    if (!hostTextField.getText().isEmpty() 
                        &&  !portTextField.getText().isEmpty()) {
                        nameTextInit.setText(hostTextField.getText() 
                            + COLON + portTextField.getText());
                    } else {
                        nameTextInit.setText("");
                    }
                } else {
                    // if selection is disabled
                    nameTextInit.setEnabled(true);
                    nameTextInit.setText("");
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

    /**
     * Get entered passphrase.
     * 
     * @return the passphrase, or null, if key file authentication without passphrase is used.
     */
    public String getPassphrase() {
        if (usePassphrase) {
            return passphrase;
        }
        return null;
    }

    /**
     * Get entered keyfile location.
     * 
     * @return the keyfile location, or null, if password authentication is used.
     */
    public String getKeyfileLocation() {
        if (useKeyFile) {
            return keyfileLocation;
        }
        return null;
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

    /**
     * If the passphrase should be stored in the secure store.
     * 
     * @return true, if a passphrase is used and should be stored in the secure store.
     */
    public boolean shouldStorePassPhrase() {
        if (usePassphrase) {
            return storePassphrase;
        }
        return false;
    }

    public boolean getUsePassphrase() {
        return usePassphrase;
    }

    protected void updateOkButtonActivation() {
        getButton(IDialogConstants.OK_ID).setEnabled(!host.isEmpty() && !port.isEmpty() && !username.isEmpty());
    }
}
