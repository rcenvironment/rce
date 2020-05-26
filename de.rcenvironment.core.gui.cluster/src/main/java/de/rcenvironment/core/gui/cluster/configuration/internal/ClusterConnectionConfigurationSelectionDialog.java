/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.cluster.configuration.internal;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Dialog to manage connection configurations and select one to connect.
 *
 * @author Doreen Seider
 */
public class ClusterConnectionConfigurationSelectionDialog extends ElementListSelectionDialog {

    protected static final int NEW = 2;
    
    protected static final int EDIT = 3;
    
    protected static final int DELETE = 4;
    
    protected static final int CONNECT = 5;
    
    private ClusterConnectionConfigurationDialogsController controller;
    
    private Button editButton;
    
    private Button deleteButton;

    private Button connectButton;
    
    private ClusterConnectionConfiguration selectedConfiguration;
    
    private boolean hasInitialElements = false;
    
    public ClusterConnectionConfigurationSelectionDialog(Shell parent, ClusterConnectionConfigurationDialogsController controller) {
        super(parent, new LabelProvider());
        setTitle(Messages.selectHostDialogTitle);
        setMessage(Messages.selectHostDialogMessage);
        ClusterConnectionConfiguration[] configurations = controller.getStoredClusterConnectionConfigurations();
        setElements(configurations);
        hasInitialElements = configurations.length > 0;
        this.controller = controller;
    }
    
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        
        Button newButton = createButton(parent, NEW, Messages.newButtonTitle, false);
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                controller.openNewClusterConnectionConfigurationDialog();
                updateDialog();
            }
        });
        
        editButton = createButton(parent, EDIT, Messages.editButtonTitle, false);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                controller.openEditClusterConnectionConfigurationDialog((ClusterConnectionConfiguration) getSelectedElements()[0]);
                updateDialog();
            }
        });
        editButton.setEnabled(hasInitialElements);
        
        deleteButton = createButton(parent, DELETE, Messages.deleteButtonTitle, false);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                controller.openDeleteConfirmationDialog((ClusterConnectionConfiguration) getSelectedElements()[0]);
                updateDialog();
            }
        });
        deleteButton.setEnabled(hasInitialElements);
        
        connectButton = createButton(parent, CONNECT, Messages.connectButtonTitle, true);
        connectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setSelectedConfiguration();
                setReturnCode(CONNECT);
                close();
            }
        });
        connectButton.setEnabled(hasInitialElements);
        
        Button cancelButton = createButton(parent, CANCEL, Messages.cancelButtonTitle, false);
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setReturnCode(CANCEL);
                close();
            }
        });
    }
        
    @Override
    protected boolean isResizable() {
        return true;
    }
    
    @Override
    protected void handleDefaultSelected() {
        setSelectedConfiguration();
        setReturnCode(CONNECT);
        close();
    }
    
    @Override
    protected void handleSelectionChanged() {
        super.handleSelectionChanged();
        setSelectedConfiguration();
        updateButtons();
    }
    
    protected ClusterConnectionConfiguration getSelectedElement() {
        return selectedConfiguration;
    }
    
    private void setSelectedConfiguration() {
        if (getSelectedElements().length > 0) {
            selectedConfiguration = (ClusterConnectionConfiguration) getSelectedElements()[0];                    
        } else {
            selectedConfiguration = null;
        }
    }
    
    private void updateDialog() {
        setListElements(controller.getStoredClusterConnectionConfigurations());
        updateButtons();
    }

    private void updateButtons() {
        ClusterConnectionConfiguration[] configurations = controller.getStoredClusterConnectionConfigurations();
        boolean enabled = configurations.length > 0 && getSelectedElements().length > 0;
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        connectButton.setEnabled(enabled);
    }
}
