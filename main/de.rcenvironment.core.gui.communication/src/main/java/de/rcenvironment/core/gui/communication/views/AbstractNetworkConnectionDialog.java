/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;

/**
 * Abstract class for NetworkConnectionDialogs, such as AddNetworkConnectionDialog and EditNetworkConnectionDialog.
 * 
 * @author Oliver Seebach
 */
public abstract class AbstractNetworkConnectionDialog extends Dialog {

    protected static final String ACTIVEMQ_PREFIX = "activemq-tcp:";
    
    private static final int DIALOG_WINDOW_OFFSET_Y = 100;

    private static final int DIALOG_WINDOW_OFFSET_X = 150;

    private static final String NAME_LABEL = "Name:";

    private static final String HOST_LABEL = "Host:Port:";

    private static final int CHECKBOX_LABEL_WIDTH = 300;

    protected String connectionName = "";

    protected String networkContactPointID = "";

    protected String hint = "";

    private boolean connectImmediately = true;

    private NetworkContactPoint parsedNetworkContactPoint;

    private final Log log = LogFactory.getLog(getClass());
    
    private Button useDefaultNameButton;

    private Label nameLabel;

    private boolean isDefaultName = true;

    protected AbstractNetworkConnectionDialog(Shell parentShell) {
        super(parentShell);
    }

    public AbstractNetworkConnectionDialog(Shell parentShell, String connectionName, String connectionString) {
        super(parentShell);
        this.connectionName = connectionName;
        this.networkContactPointID = connectionString;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        // remove scrollbar
        if (shell.getHorizontalBar() != null) {
            shell.getHorizontalBar().dispose();
        }
        // place shell in the middle of the screen
        shell.setLocation(shell.getParent().getLocation().x + shell.getParent().getSize().x/2 - DIALOG_WINDOW_OFFSET_X, 
            shell.getParent().getLocation().y + shell.getParent().getSize().y/2 - DIALOG_WINDOW_OFFSET_Y);
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
        
        GridData connectImmediateCheckboxGridData = new GridData();
        connectImmediateCheckboxGridData.widthHint = CHECKBOX_LABEL_WIDTH;
        connectImmediateCheckboxGridData.horizontalSpan = 2;

        Label cpLabel = new Label(container, SWT.NULL);
        cpLabel.setText(HOST_LABEL);

        final Text hostAndPortText = new Text(container, SWT.SINGLE | SWT.BORDER);
        hostAndPortText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        hostAndPortText.setText(networkContactPointID);

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

        @SuppressWarnings("unused")
        final Label placeholderLabel = new Label(container, SWT.NONE); // used for layouting
        
        useDefaultNameButton = new Button(container, SWT.CHECK);
        useDefaultNameButton.setSelection(true);
        useDefaultNameButton.setText("Use default name (host:port)");
        useDefaultNameButton.setLayoutData(useDefaultCheckboxGridData);
        useDefaultNameButton.setSelection(isDefaultName);

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

        // Init public variables and add listeners
        networkContactPointID = hostAndPortText.getText();
        hostAndPortText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                networkContactPointID = hostAndPortText.getText();
                if (useDefaultNameButton.getSelection()) {
                    nameText.setText(networkContactPointID);
                }
            }
        });

        connectionName = nameText.getText();
        nameText.addModifyListener(new ModifyListener() {

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

        useDefaultNameButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (useDefaultNameButton.getSelection()) {
                    // if selection is enabled
                    nameText.setEnabled(false);
                    nameText.setText(hostAndPortText.getText());
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
        return container;
    }
    

    @Override
    protected void okPressed() {
        boolean canParse = false;
        try {
            String contactPoint = getNetworkContactPointID();
            if (!contactPoint.startsWith(ACTIVEMQ_PREFIX)) {
                contactPoint = ACTIVEMQ_PREFIX + contactPoint;
            }
            parsedNetworkContactPoint = NetworkContactPointUtils.parseStringRepresentation(contactPoint);
            canParse = true;
        } catch (IllegalArgumentException e) {
            canParse = false;
        }

        if (!canParse) {
            String contactPoint = getNetworkContactPointID();
            String warningMessage = "";
            String contactPointString = contactPoint.replace(ACTIVEMQ_PREFIX, "");
            if (contactPointString.isEmpty()){
                warningMessage = "Empty Host+Port is not valid.";
            } else {
                warningMessage = "Host+Port \"" + contactPointString + "\" not valid.";
            }
            log.debug(warningMessage);
            MessageBox cpNameWarning = new MessageBox(this.getParentShell(), SWT.ICON_WARNING);
            cpNameWarning.setMessage(warningMessage);
            cpNameWarning.setText("Warning");
            cpNameWarning.open();
        } else {
            super.okPressed();
        }
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

}
