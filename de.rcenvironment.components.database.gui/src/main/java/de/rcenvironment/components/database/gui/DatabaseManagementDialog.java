/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.components.database.common.DatabaseConnection;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;

/**
 * Database management dialog.
 *
 * @author Oliver Seebach
 */
public class DatabaseManagementDialog extends Dialog {

    protected Composite composite;

    protected Composite client;

    protected ComponentInstanceProperties configuration;

    protected Table table;

    protected Button buttonAdd;

    protected Button buttonEdit;

    protected Button buttonRemove;

    protected MenuItem itemAdd;

    protected MenuItem itemEdit;

    protected MenuItem itemRemove;

    protected TableColumnLayout tableLayout;

    protected DatabaseManagementDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.RESIZE);
    }
    
    @Override
    protected void configureShell(Shell shell) {
        shell.setText("Manage Databases");
//        shell.setMinimumSize(500, 300);
        super.configureShell(shell);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Close", true);
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        client = new Composite(parent, SWT.NONE);
        client.setLayout(new GridLayout(2, false));
        final Composite tableComposite = new Composite(client, SWT.NONE);
        tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
        table = new Table(tableComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        table.setHeaderVisible(true);

        GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 5);
        final int minHeight = 300;
        final int minWidth = 500;
        tableLayoutData.heightHint = minHeight; // effectively min height
        tableLayoutData.minimumWidth = minWidth; // effectively min height
        tableComposite.setLayoutData(tableLayoutData);

        final int columnWeight = 20;

        // 1. column - db name
        TableColumn colDbName = new TableColumn(table, SWT.NONE);
        colDbName.setText("Name");
        // 2. column - db type
        TableColumn colType = new TableColumn(table, SWT.NONE);
        colType.setText("Type");
        // 3. column - db host
        TableColumn colHost = new TableColumn(table, SWT.NONE);
        colHost.setText("Host");
        // 4. column - db port
        TableColumn colPort = new TableColumn(table, SWT.NONE);
        colPort.setText("Port");
        // 5. column - db username
        TableColumn colUsername = new TableColumn(table, SWT.NONE);
        colUsername.setText("Username");
        // 6. column - db password
        TableColumn colPassword = new TableColumn(table, SWT.NONE);
        colPassword.setText("Password");
        // 7. column - db default scheme
        TableColumn colDefaultScheme = new TableColumn(table, SWT.NONE);
        colDefaultScheme.setText("Scheme");
        // 8. column - db status
        TableColumn colStatus = new TableColumn(table, SWT.NONE);
        colStatus.setText("Status");
        

        tableLayout.setColumnData(colDbName, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colType, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colHost, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colPort, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colUsername, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colPassword, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colDefaultScheme, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colStatus, new ColumnWeightData(columnWeight, true));
        
        
        TableItem dummyItem = new TableItem(table, SWT.NONE);
        String[] tableData = new String[8];
//        tableData[0] = "name";
//        tableData[1] = "name";
//        tableData[2] = "name";
//        tableData[3] = "name";
//        tableData[4] = "name";
//        tableData[5] = "name";
//        tableData[6] = "name";
//        tableData[7] = "name";
        dummyItem.setText(tableData);
        
        
        buttonAdd = new Button(client, SWT.FLAT);
        buttonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonAdd.setText(EndpointActionType.ADD.toString());
        buttonAdd.addSelectionListener(new AddDatabaseButtonListener());
        buttonEdit = new Button(client, SWT.FLAT);
        buttonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonEdit.setText(EndpointActionType.EDIT.toString());
        buttonEdit.addSelectionListener(new EditDatabaseButtonListener());
        buttonRemove = new Button(client, SWT.FLAT);
        buttonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonRemove.setText(EndpointActionType.REMOVE.toString());
        buttonRemove.addSelectionListener(new RemoveDatabaseButtonListener());

        loadDatabaseConnections();
        
        return super.createDialogArea(parent);
    }
    
    
    private void loadDatabaseConnections(){
        for (DatabaseConnection dbCon : readDatabaseConnectionsFromConfig()){
            parseDbConnectionAndAddToTable(dbCon);
        }
    }
    
    private TableItem parseDbConnectionAndAddToTable(DatabaseConnection dbCon){
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(0, dbCon.getName());
        item.setText(1, dbCon.getType());
        item.setText(2, dbCon.getHost());
        item.setText(3, dbCon.getPort());
        item.setText(4, dbCon.getUsername());
        item.setText(5, dbCon.getPassword());
        item.setText(6, dbCon.getScheme());
        item.setText(7, dbCon.getState());
        return item;
    }
    
    
    private List<DatabaseConnection> readDatabaseConnectionsFromConfig(){
        List<DatabaseConnection> connections = new ArrayList<>();
        
        // retrieve connections here
        
        return connections;
    }
    
    
    
    /**
     * Listener to add database from management.
     *
     * @author Oliver Seebach
     */
    private final class AddDatabaseButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {

//            DatabaseConnectionAddEditDialog addDialog = new DatabaseConnectionAddEditDialog
//            (Display.getDefault().getActiveShell(), DatabaseManagementActionType.ADD);
//            addDialog.open();
            
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);            
        }
    }
    
    /**
     * Listener to edit database from management.
     *
     * @author Oliver Seebach
     */
    private final class EditDatabaseButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            
            TableItem item = table.getSelection()[0];
            
//            DatabaseConnectionAddEditDialog addDialog = new DatabaseConnectionAddEditDialog(
//            Display.getDefault().getActiveShell(), item, DatabaseManagementActionType.EDIT);
//            addDialog.open();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);            
        }
    }
    
    /**
     * Listener to remove database from management.
     *
     * @author Oliver Seebach
     */
    private final class RemoveDatabaseButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {

            int selectedItemIndex = table.getSelectionIndex();
            DatabaseConnectionDeleteCommand deleteCommand = new DatabaseConnectionDeleteCommand(table, selectedItemIndex);
            deleteCommand.execute(); // TODO use command stack here
            
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);            
        }
    }

}
