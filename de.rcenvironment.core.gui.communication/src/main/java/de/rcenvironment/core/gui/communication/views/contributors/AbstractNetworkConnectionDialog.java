/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
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

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;
import de.rcenvironment.core.gui.utils.incubator.PasteListeningText;
import de.rcenvironment.core.gui.utils.incubator.PasteListeningText.PasteListener;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Abstract class for NetworkConnectionDialogs, such as AddNetworkConnectionDialog and EditNetworkConnectionDialog.
 * 
 * @author Oliver Seebach
 * @author Hendrik Abbenhaus
 */
public abstract class AbstractNetworkConnectionDialog extends Dialog implements ModifyListener, VerifyListener, PasteListener {

    protected static final String ACTIVEMQ_PREFIX = "activemq-tcp:";

    protected static final String COM = ",";

    protected static final String DECIMAL = "[^0-9]";

    private static final String INVALID_IP = "0.0.0.0";

    private static final int DIALOG_WINDOW_OFFSET_Y = 100;

    private static final int DIALOG_WINDOW_OFFSET_X = 150;

    private static final String COLON = ":";

    private static final String NAME_LABEL = "Name" + COLON;

    private static final String HOST_LABEL = "Host" + COLON;

    private static final String PORT_LABEL = "Port" + COLON;

    private static final String AUTO_RETRY_INITIAL_DELAY_STR = "autoRetryInitialDelay";

    private static final String AUTO_RETRY_MAXI_DELAY_STR = "autoRetryMaximumDelay";

    private static final String AUTO_RETRY_DELAY_MULTIPL = "autoRetryDelayMultiplier";

    private static final String SETTINGS = "Settings" + COLON;

    private static final int CHECKBOX_LABEL_WIDTH = 300;

    private static Text portTextField;

    private static PasteListeningText hostTextField;

    protected String connectionName = "";

    protected String networkContactPointID = "";

    protected String hint = "";

    protected String host = "";

    protected String port = "";

    protected String settingsText = "";

    private boolean connectImmediately = true;
    
    private NetworkContactPoint parsedNetworkContactPoint;

    private final Log log = LogFactory.getLog(getClass());

    private Button useDefaultNameButton;

    private Button useDefaultSettings;

    private Text nameText;

    private Text settingsTextField;

    private Label nameLabel;

    private boolean isDefaultName = true;

    private Composite container;

    private ConnectionSettings settings;

    protected AbstractNetworkConnectionDialog(Shell parentShell) {
        super(parentShell);
    }

    public AbstractNetworkConnectionDialog(Shell parentShell, String connectionName, String connectionString) {
        super(parentShell);
        this.connectionName = connectionName;
        this.networkContactPointID = connectionString;
        this.host = networkContactPointID.substring(0, networkContactPointID.indexOf(COLON));
        this.port = networkContactPointID.substring(networkContactPointID.indexOf(COLON) + 1);
        this.settings = new ConnectionSettings();
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
        settings = new ConnectionSettings();
        container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(2, false);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, false, false);
        container.setLayoutData(containerGridData);
        container.setLayout(layout);

        GridData useDefaultCheckboxGridData = new GridData();
        useDefaultCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        useDefaultCheckboxGridData.horizontalSpan = 1;

        GridData connectImmediateCheckboxGridData = new GridData();
        connectImmediateCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        connectImmediateCheckboxGridData.horizontalSpan = 2;

        Label cpLabel = new Label(container, SWT.NULL);
        cpLabel.setText(HOST_LABEL);

        hostTextField = new PasteListeningText(container, SWT.SINGLE | SWT.BORDER);
        hostTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        if (!host.isEmpty()) {
            hostTextField.setText(host);
        }
        hostTextField.addPasteListener(this);
        hostTextField.addModifyListener(this);

        Label cpLabelPort = new Label(container, SWT.NULL);
        cpLabelPort.setText(PORT_LABEL);

        portTextField = new Text(container, SWT.SINGLE | SWT.BORDER);
        portTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        if (!port.isEmpty()) {
            portTextField.setText(port);
        }
        portTextField.addVerifyListener(this);
        portTextField.addModifyListener(this);

        GridData separatorGridData = new GridData();
        separatorGridData.horizontalAlignment = GridData.FILL;
        separatorGridData.grabExcessHorizontalSpace = true;
        separatorGridData.horizontalSpan = 2;

        nameLabel = new Label(container, SWT.NULL);
        nameLabel.setText(NAME_LABEL);

        nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
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
        


        buildSettingsField();
        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(separatorGridData);

        final Button immediateConnectButton = new Button(container, SWT.CHECK);
        immediateConnectButton.setSelection(true);
        immediateConnectButton.setText("Connect immediately");
        immediateConnectButton.setLayoutData(connectImmediateCheckboxGridData);

        final Label persistHint = new Label(container, SWT.NULL);
        GridData hintGridData = new GridData();
        hintGridData.horizontalSpan = 2;
        persistHint.setText(hint);
        persistHint.setLayoutData(hintGridData);

        nameText.addModifyListener(new ModifyListener() {
            
            @SuppressWarnings("unused")
            @Override
            public void modifyText(ModifyEvent e) {
                connectionName = nameText.getText();
              
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

        return container;
    }

   
    
    
    
    @Override
    public void modifyText(ModifyEvent e) {
        host = hostTextField.getText();
        port = portTextField.getText();
        if (useDefaultNameButton.getSelection()) {
            if (!hostTextField.getText().isEmpty() 
                && !portTextField.getText().isEmpty()) {
                nameText.setText(hostTextField.getText()
                    + COLON + portTextField.getText());
            } else {
                nameText.setText("");
            }
        }
        updateOkButtonActivation();
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

    private void buildSettingsField() {
        final int maxDefault = 300;
        final double multi = 1.5;

        Label cpSettingsLbl = new Label(container, SWT.NULL);
        cpSettingsLbl.setText(SETTINGS);

        settingsTextField = new Text(container, SWT.SINGLE | SWT.BORDER);
        settingsTextField.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        settingsTextField.setEnabled(!settingsText.isEmpty());
        settingsTextField.setText(settingsText);
        if (settingsText.isEmpty()) {
            settingsTextField.setText(settings.createStringForsettings(5, maxDefault, multi));
        }

        final Label placeholderLabel = new Label(container, SWT.NONE); // used for layouting
        placeholderLabel.setText("");

        GridData useDefaultData = new GridData();
        useDefaultData.horizontalSpan = 1;
        useDefaultData.grabExcessHorizontalSpace = false;

        useDefaultSettings = new Button(container, SWT.CHECK);
        useDefaultSettings.setSelection(settingsText.isEmpty());
        useDefaultSettings.setText("Use default settings");
        useDefaultSettings.setLayoutData(useDefaultData);

        useDefaultSettings.addSelectionListener(new SelectionAdapter() {

            private ConnectionSettings settings = new ConnectionSettings();

            @Override
            public void widgetSelected(SelectionEvent e) {
                settingsTextField.setEnabled(!useDefaultSettings.getSelection());
                if (useDefaultSettings.getSelection()) {
                    settingsTextField.setText(settings.createStringForsettings(5, maxDefault, multi));
                }
            }
        });

    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(!networkContactPointID.isEmpty());
    }

    @Override
    protected void okPressed() {
        boolean canParse = true;

        if (getNetworkContactPointID().contains(INVALID_IP)) {
            MessageDialog.openError(this.getParentShell(), "Invalid host address", "The IP address 0.0.0.0"
                + " configured for the network connection is invalid. For a local connection please use 127.0.0.1 or 'localhost' instead.");
            return;
        }

        try {
            catchSettingsFields();
        } catch (IllegalArgumentException ex) {
            String errorMessage = StringUtils.format("The settings are not in a valid format.");
            log.debug(errorMessage);
            MessageDialog.openError(this.getParentShell(), "Invalid format", errorMessage);
            return;
        }

        try {
            settings.setConnectOnStartup(connectImmediately);

            String hostAndPortString = StringUtils.format("%s:%s", host, port);
            String temp =
                hostAndPortString + "(" + settings.getSettingsString() + COM + "connectOnStartup=" + settings.isConnectOnStartup() + ")";
            String contactPoint = removeEmptySpaces(temp);

            if (!contactPoint.startsWith(ACTIVEMQ_PREFIX)) {
                contactPoint = ACTIVEMQ_PREFIX + contactPoint;
            }
            parsedNetworkContactPoint = NetworkContactPointUtils.parseStringRepresentation(contactPoint);
        } catch (IllegalArgumentException e) {
            canParse = false;
        }

        if (canParse) {
            super.okPressed();
        } else {
            String errorMessage = StringUtils.format("'%s' is invalid for the host. It must be of format e.g. 192.168.0.15",
                host);
            log.debug(errorMessage);
            MessageDialog.openError(this.getParentShell(), "Invalid format", errorMessage);
        }
    }

    private void catchSettingsFields() {

        String text = settingsTextField.getText();

        if (text.isEmpty()) {
            return;
        }

        if (!text.contains(AUTO_RETRY_INITIAL_DELAY_STR) || !text.contains(AUTO_RETRY_MAXI_DELAY_STR)
            || !text.contains(AUTO_RETRY_DELAY_MULTIPL)) {
            throw new IllegalArgumentException();
        }

        int indexFirstCom = 0;
        int indexSecondCom = 0;

        String numberOnly = settingsTextField.getText().replaceAll("[^0-9=,.]", "");

        if (settingsTextField.getText().contains(AUTO_RETRY_INITIAL_DELAY_STR)) {
            if (!numberOnly.isEmpty() && numberOnly.contains(COM)) {
                indexFirstCom = numberOnly.indexOf(COM);
                String temp = numberOnly.substring(0, indexFirstCom);
                String initDelay = temp.replaceAll(DECIMAL, "");
                settings.setAutoRetryInitialDelay(Integer.parseInt(initDelay));
            }
        }

        if (settingsTextField.getText().contains(AUTO_RETRY_MAXI_DELAY_STR)) {
            indexSecondCom = numberOnly.indexOf(COM, numberOnly.indexOf(COM) + 1);
            String temp = numberOnly.substring(indexFirstCom, indexSecondCom);
            String maxiDelay = temp.replaceAll(DECIMAL, "");
            settings.setAutoRetryMaximumDelay(Integer.parseInt(maxiDelay));

        }

        if (settingsTextField.getText().contains(AUTO_RETRY_DELAY_MULTIPL)) {
            String temp = numberOnly.substring(indexSecondCom);
            String multiplier = temp.replaceAll("[^0-9.]", "");
            settings.setAutoRetryDelayMultiplier(Double.parseDouble(multiplier));

        }

    }
    
    

    private String removeEmptySpaces(String string) {
        String temp = string.replaceAll("\\s", "");
        return temp;
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

    public NetworkContactPoint getParsedNetworkContactPoint() {
        return parsedNetworkContactPoint;
    }

    public String getNetworkContactPointID() {
        return networkContactPointID;
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

    private void updateOkButtonActivation() {
        getButton(IDialogConstants.OK_ID).setEnabled(!host.isEmpty() && !port.isEmpty());
    }

}
