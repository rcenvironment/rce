/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.database.gui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * Database Connection Adding and Editing Dialog.
 *
 * @author Oliver Seebach
 */
public class DatabaseConnectionAddEditDialog extends Dialog {

    private static final int MINIMUM_DIALOG_WIDTH = 300;
    private Text nameTextfield;
    private Text typeTextfield;
    private Text hostTextfield;
    private Text portTextfield;
    private Text defaultSchemeTextfield;
    private Text usernameTextfield;
    private Text passwordTextfield;
    
    private String name;
    private String type;
    private String host;
    private String port;
    private String defaultScheme;
    private String username;
    private String password;
    
    private boolean fillTextfields = false;
    
    private DatabaseManagementActionType actionType;
    
    protected DatabaseConnectionAddEditDialog(Shell parentShell, DatabaseManagementActionType actionType) {
        super(parentShell);
        this.actionType = actionType;
    }

    @Override
    protected void configureShell(Shell newShell) {
//        newShell.setLayout(new GridLayout(1, false));
        GridData databaseSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        databaseSelectionData.minimumWidth = MINIMUM_DIALOG_WIDTH;
        databaseSelectionData.widthHint = MINIMUM_DIALOG_WIDTH;
        newShell.setLayoutData(databaseSelectionData);
        super.configureShell(newShell);
    }
    
//    protected DatabaseConnectionAddEditDialog(Shell parentShell, TableItem selectedItem,  DatabaseManagementActionType actionType) {
//        super(parentShell);
//        name = selectedItem.getText(0);
//        type = selectedItem.getText(1);
//        host = selectedItem.getText(2);
//        port = selectedItem.getText(3);
//        defaultScheme = selectedItem.getText(4);
//        username = selectedItem.getText(5);
//        password = selectedItem.getText(6);
//        
//        fillTextfields = true;
//        
//        this.actionType = actionType;
//    }

//    protected DatabaseConnectionAddEditDialog(Shell parentShell, DatabaseConnection selectedConnection,  
//    DatabaseManagementActionType actionType) {
//        super(parentShell);
//        name = selectedConnection.getName();
//        type = selectedConnection.getType();
//        host = selectedConnection.getHost();
//        port = selectedConnection.getPort();
//        defaultScheme = selectedConnection.getScheme();
//        username = selectedConnection.getUsername();
//        password = selectedConnection.getPassword();
//        
//        fillTextfields = true;
//        
//        this.actionType = actionType;
//    }
    
    
    
    private void setTextFieldData() {

        nameTextfield.setText(name);
        typeTextfield.setText(type);
        hostTextfield.setText(host);
        portTextfield.setText(port);
        defaultSchemeTextfield.setText(defaultScheme);
        usernameTextfield.setText(username);
        passwordTextfield.setText(password);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));
        GridData g1 = new GridData(GridData.FILL_BOTH);
        g1.grabExcessHorizontalSpace = true;
        g1.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g1);
        
        // --------------------------------
        
        Composite configurationContainer = new Composite(container, SWT.NONE);
        GridData g2 = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
//        GridData g2 = new GridData(GridData.FILL, GridData.FILL, true, true);
        configurationContainer.setLayout(new GridLayout(2, false));
        configurationContainer.setLayoutData(g2);

//        GridData textGridData = new GridData();
        GridData textGridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
//        textGridData.grabExcessHorizontalSpace = true;

        // Db Name
        Label nameLabel = new Label(configurationContainer, SWT.NONE);
        nameLabel.setText("Database name: ");
        nameLabel.setLayoutData(textGridData);

        nameTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        nameTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        // Type
        Label typeLabel = new Label(configurationContainer, SWT.NONE);
        typeLabel.setText("Database Type: ");
        typeLabel.setLayoutData(textGridData);

        typeTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        typeTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        // Host
        Label hostLabel = new Label(configurationContainer, SWT.NONE);
        hostLabel.setText("Host: ");
        hostLabel.setLayoutData(textGridData);

        hostTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        hostTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        // Port
        Label portLabel = new Label(configurationContainer, SWT.NONE);
        portLabel.setText("Port: ");
        portLabel.setLayoutData(textGridData);

        portTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        portTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        // Scheme
        Label defaultSchemeLabel = new Label(configurationContainer, SWT.NONE);
        defaultSchemeLabel.setText("Default Scheme: ");
        defaultSchemeLabel.setLayoutData(textGridData);

        defaultSchemeTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        defaultSchemeTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        GridData separatorGridData = new GridData(GridData.FILL_BOTH);
        separatorGridData.grabExcessHorizontalSpace = true;
        separatorGridData.horizontalAlignment = GridData.CENTER;
        separatorGridData.horizontalSpan = 2;
        Label sep = new Label(configurationContainer, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.FILL);
        sep.setLayoutData(separatorGridData);
        
        // Username
        Label usernameLabel = new Label(configurationContainer, SWT.NONE);
        usernameLabel.setText("Username: ");
        usernameLabel.setLayoutData(textGridData);

        usernameTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        usernameTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        // Password
        Label passwordLabel = new Label(configurationContainer, SWT.NONE);
        passwordLabel.setText("Password: ");
        passwordLabel.setLayoutData(textGridData);

        passwordTextfield = new Text(configurationContainer, SWT.SINGLE | SWT.BORDER);
        passwordTextfield.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        
        if (fillTextfields){
            setTextFieldData();
        }
        
        return configurationContainer;
    }
    
    @Override
    protected void okPressed() {
        
//        if (actionType.equals(DatabaseManagementActionType.ADD)){
//            // DO adding here
//        } else if (actionType.equals(DatabaseManagementActionType.EDIT)){
//            // DO update here
//        }
        
        super.okPressed();
    }
    
    
    
}
