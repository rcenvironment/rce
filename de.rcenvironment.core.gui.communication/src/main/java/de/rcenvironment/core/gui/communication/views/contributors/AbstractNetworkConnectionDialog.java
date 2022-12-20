/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.PasteListeningText;
import de.rcenvironment.core.gui.utils.incubator.PasteListeningText.PasteListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Abstract class for NetworkConnectionDialogs, such as AddNetworkConnectionDialog and EditNetworkConnectionDialog.
 * 
 * @author Oliver Seebach
 * @author Hendrik Abbenhaus
 * @author Kathrin Schaffert
 */
public abstract class AbstractNetworkConnectionDialog extends TitleAreaDialog implements ModifyListener, VerifyListener, PasteListener {

    protected static final String ERROR_MESSAGE = "One of the connection settings is missing.";

    protected static final String DEFAULT_TITLE = "Network Connection Dialog";

    protected static final String DEFAULT_MESSAGE = "Configure network connection";

    protected static final String CONNECT_ON_STARTUP = "connectOnStartup";

    protected static final String USE_DEFAULT_SETTINGS = "useDefaultSettings";

    protected static final String AUTO_RETRY = "autoRetry";

    protected static final String AUTO_RETRY_INITIAL_DELAY_STR = "autoRetryInitialDelay";

    protected static final String AUTO_RETRY_MAXI_DELAY_STR = "autoRetryMaximumDelay";

    protected static final String AUTO_RETRY_DELAY_MULTIPL = "autoRetryDelayMultiplier";

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

    private static final String SETTINGS = "Settings for Auto Reconnect";

    private static final int CHECKBOX_LABEL_WIDTH = 300;

    protected String title;

    protected String connectionName = "";

    protected String networkContactPointID = "";

    protected String hint = "";

    protected String host = "";

    protected String port = "";

    protected String errorMessage;

    protected ConnectionSettings connectionSettings = new ConnectionSettings();

    private NetworkContactPoint parsedNetworkContactPoint;

    private final Log log = LogFactory.getLog(getClass());

    private Text portTextField;

    private PasteListeningText hostTextField;

    private Button useDefaultNameButton;

    private Button useDefaultSettings;

    private Text nameText;

    private Text initialDelayTextField;

    private Text maximumDelayTextField;

    private Text delayMultiplierTextField;

    private boolean isDefaultName = true;

    private Composite container;

    protected AbstractNetworkConnectionDialog(Shell parentShell) {
        super(parentShell);
    }

    protected AbstractNetworkConnectionDialog(Shell parentShell, String connectionName, String connectionString) {
        super(parentShell);
        this.connectionName = connectionName;
        this.networkContactPointID = connectionString;
        this.host = networkContactPointID.substring(0, networkContactPointID.indexOf(COLON));
        this.port = networkContactPointID.substring(networkContactPointID.indexOf(COLON) + 1);
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
    public void create() {
        super.create();
        updateAutoRetrySettings();
        setTitle(DEFAULT_TITLE);
        if (errorMessage != null) {
            setMessage(errorMessage, IMessageProvider.ERROR);
        } else {
            setMessage(DEFAULT_MESSAGE);
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        container = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        container.setLayoutData(containerGridData);
        container.setLayout(layout);

        GridData checkboxGridData = new GridData();
        checkboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        checkboxGridData.horizontalSpan = 2;

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

        Label nameLabel = new Label(container, SWT.NULL);
        nameLabel.setText(NAME_LABEL);

        nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        nameText.setText(connectionName);
        nameText.setEnabled(!isDefaultName);

        final Label placeholderLabel = new Label(container, SWT.NONE); // used for layouting
        placeholderLabel.setText("");

        GridData useDefaultCheckboxGridData = new GridData();
        useDefaultCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        useDefaultCheckboxGridData.horizontalSpan = 1;

        useDefaultNameButton = new Button(container, SWT.CHECK);
        useDefaultNameButton.setText("Use default name (host" + COLON + "port)");
        useDefaultNameButton.setLayoutData(useDefaultCheckboxGridData);
        useDefaultNameButton.setSelection(isDefaultName);
        useDefaultNameButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                nameText.setEnabled(!useDefaultNameButton.getSelection());
                nameText.setText("");
                if (useDefaultNameButton.getSelection() && !hostTextField.getText().isEmpty()
                    && !portTextField.getText().isEmpty()) {
                    nameText.setText(hostTextField.getText()
                        + COLON + portTextField.getText());
                }
                updateOkButtonActivation();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetDefaultSelected(e);
            }
        });

        Label separator1 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator1.setLayoutData(separatorGridData);

        final Button autoRetryButton = new Button(container, SWT.CHECK);
        autoRetryButton.setSelection(connectionSettings.isAutoRetry());
        autoRetryButton.setText("Try reconnect after error");
        autoRetryButton.setLayoutData(checkboxGridData);
        autoRetryButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                connectionSettings.setAutoRetry(autoRetryButton.getSelection());
                setTextFieldActivation();
                useDefaultSettings.setEnabled(connectionSettings.isAutoRetry());
                updateOkButtonActivation();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        buildSettingsField();

        Label separator2 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator2.setLayoutData(separatorGridData);

        final Button immediateConnectButton = new Button(container, SWT.CHECK);
        immediateConnectButton.setSelection(connectionSettings.isConnectOnStartup());
        immediateConnectButton.setText("Connect immediately");
        immediateConnectButton.setLayoutData(checkboxGridData);

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
        immediateConnectButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                connectionSettings.setConnectOnStartup(immediateConnectButton.getSelection());
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
     * 
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

        Group settingsGroup = new Group(container, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(2, false));
        GridData settingsData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        settingsData.horizontalSpan = 2;
        settingsGroup.setLayoutData(settingsData);
        settingsGroup.setText(SETTINGS);

        useDefaultSettings = new Button(settingsGroup, SWT.CHECK);
        useDefaultSettings.setSelection(connectionSettings.isUseDefaultSettings());
        useDefaultSettings.setText("Use default settings");
        useDefaultSettings.setLayoutData(settingsData);

        useDefaultSettings.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                connectionSettings.setUseDefaultSettings(useDefaultSettings.getSelection());
                setTextFieldActivation();
                if (useDefaultSettings.getSelection()) {
                    connectionSettings.setDefaultValues();
                    updateAutoRetrySettings();
                }
            }
        });

        Label initialDelay = new Label(settingsGroup, SWT.NULL);
        initialDelay.setText("Initial Delay (sec):");
        initialDelayTextField = new Text(settingsGroup, SWT.SINGLE | SWT.BORDER);
        initialDelayTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        initialDelayTextField
            .addVerifyListener(new NumericalTextConstraintListener(WidgetGroupFactory.ONLY_INTEGER | WidgetGroupFactory.GREATER_ZERO));
        initialDelayTextField.addModifyListener(evt -> updateOkButtonActivation());

        Label maximumDelay = new Label(settingsGroup, SWT.NULL);
        maximumDelay.setText("Maximum Delay (sec):");
        maximumDelayTextField = new Text(settingsGroup, SWT.SINGLE | SWT.BORDER);
        maximumDelayTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        maximumDelayTextField
            .addVerifyListener(new NumericalTextConstraintListener(WidgetGroupFactory.ONLY_INTEGER | WidgetGroupFactory.GREATER_ZERO));
        maximumDelayTextField.addModifyListener(evt -> updateOkButtonActivation());

        Label delayMultiplier = new Label(settingsGroup, SWT.NULL);
        delayMultiplier.setText("Delay Multiplier (>=1.0):");
        delayMultiplierTextField = new Text(settingsGroup, SWT.SINGLE | SWT.BORDER);
        delayMultiplierTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        delayMultiplierTextField
            .addVerifyListener(
                new NumericalTextConstraintListener(WidgetGroupFactory.ONLY_FLOAT | WidgetGroupFactory.GREATER_OR_EQUAL_ONE));
        delayMultiplierTextField.addModifyListener(evt -> updateOkButtonActivation());

        updateUseDefaultCheckboxActivation();
        setTextFieldActivation();

    }

    protected void updateAutoRetrySettings() {
        initialDelayTextField.setText(String.valueOf(connectionSettings.getAutoRetryInitialDelay()));
        maximumDelayTextField.setText(String.valueOf(connectionSettings.getAutoRetryMaximumDelay()));
        delayMultiplierTextField.setText(String.valueOf(connectionSettings.getAutoRetryDelayMultiplier()));
    }

    private void setTextFieldActivation() {
        initialDelayTextField.setEnabled(!connectionSettings.isUseDefaultSettings() && connectionSettings.isAutoRetry());
        maximumDelayTextField.setEnabled(!connectionSettings.isUseDefaultSettings() && connectionSettings.isAutoRetry());
        delayMultiplierTextField.setEnabled(!connectionSettings.isUseDefaultSettings() && connectionSettings.isAutoRetry());
    }

    private void updateUseDefaultCheckboxActivation() {
        boolean useDefault = connectionSettings.getAutoRetryInitialDelay() == ConnectionSettings.INITIAL_DELAY_DEFAULT_VAL
            && connectionSettings.getAutoRetryMaximumDelay() == ConnectionSettings.MAX_DELAY_DEFAULT_VAL
            && connectionSettings.getAutoRetryDelayMultiplier() == ConnectionSettings.DELAY_MULTIPLIER_DEFAULT_VAL;
        useDefaultSettings.setSelection(useDefault);
        connectionSettings.setUseDefaultSettings(useDefault);
        useDefaultSettings.setEnabled(connectionSettings.isAutoRetry());
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

        catchSettingsFields();

        try {
            String hostAndPortString = StringUtils.format("%s:%s", host, port);
            String temp =
                hostAndPortString + "(" + connectionSettings.getSettingsString() + COM + CONNECT_ON_STARTUP + " = "
                    + connectionSettings.isConnectOnStartup() + COM + USE_DEFAULT_SETTINGS + " = "
                    + connectionSettings.isUseDefaultSettings() + COM + AUTO_RETRY + " = "
                    + connectionSettings.isAutoRetry() + ")";
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
            String error = StringUtils.format("'%s' is invalid for the host. It must be of format e.g. 192.168.0.15",
                host);
            log.debug(error);
            MessageDialog.openError(this.getParentShell(), "Invalid format", error);
        }
    }

    private void catchSettingsFields() {
        try {
            connectionSettings.setAutoRetryInitialDelay(Long.parseLong(initialDelayTextField.getText()));
        } catch (NumberFormatException e) {
            // invalid settings can only be stored, if auto retry is set to false
            // otherwise the ok button is disabled
            // in case auto retry = false, the settings will never be used
            // K. Schaffert, 01.12.2022
        }
        try {
            connectionSettings.setAutoRetryMaximumDelay(Long.parseLong(maximumDelayTextField.getText()));
        } catch (NumberFormatException e) {
            // nothing to do here > as above
        }
        try {
            connectionSettings.setAutoRetryDelayMultiplier(Double.parseDouble(delayMultiplierTextField.getText()));
        } catch (NumberFormatException e) {
            // nothing to do here > as above
        }
    }

    private String removeEmptySpaces(String string) {
        return string.replaceAll("\\s", "");
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
        return connectionSettings.isConnectOnStartup();
    }

    protected void activateDefaultName() {
        isDefaultName = true;
    }

    protected void deactivateDefaultName() {
        isDefaultName = false;
    }

    private void updateOkButtonActivation() {
        boolean enabled = !host.isEmpty() && !port.isEmpty();
        if (enabled && (connectionSettings.isAutoRetry() && !connectionSettings.isUseDefaultSettings())) {
            enabled = !initialDelayTextField.getText().isEmpty() && !maximumDelayTextField.getText().isEmpty()
                && !delayMultiplierTextField.getText().isEmpty();
        }
        getButton(IDialogConstants.OK_ID).setEnabled(enabled);
        if (enabled) {
            setMessage(DEFAULT_MESSAGE);
        } else {
            setMessage(ERROR_MESSAGE, IMessageProvider.ERROR);
        }
    }
}
