/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.api;

import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;

import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.gui.wizards.toolintegration.ToolIntegrationWizard;

/**
 * Extended interface for a {@link WizardPage} for the {@link ToolIntegrationWizard}.
 * 
 * @author Sascha Zur
 */
public abstract class ToolIntegrationWizardPage extends WizardPage {

    protected MenuItem itemAdd;

    protected MenuItem itemEdit;

    protected MenuItem itemRemove;

    protected ToolIntegrationWizardPage(String pageName) {
        super(pageName);
    }

    protected void fillContextMenu(Table tab) {
        Menu menu = new Menu(tab);

        itemAdd = new MenuItem(menu, SWT.PUSH);
        itemAdd.setText(EndpointActionType.ADD.toString());

        itemEdit = new MenuItem(menu, SWT.PUSH);
        itemEdit.setText(EndpointActionType.EDIT.toString());

        itemRemove = new MenuItem(menu, SWT.PUSH);
        itemRemove.setText(EndpointActionType.REMOVE.toString());

        final SelectionAdapter buttonListener = new ButtonSelectionAdapter(itemAdd, itemEdit, itemRemove, tab);

        itemRemove.addSelectionListener(buttonListener);
        itemEdit.addSelectionListener(buttonListener);
        itemAdd.addSelectionListener(buttonListener);

        menu.addMenuListener(new MenuListener() {

            @Override
            public void menuShown(MenuEvent arg0) {
                ((ButtonSelectionAdapter) buttonListener).updateContextActivation();
            }

            @Override
            public void menuHidden(MenuEvent arg0) {

            }
        });
        tab.setMenu(menu);
        ((ButtonSelectionAdapter) buttonListener).updateContextActivation();
    }

    /**
     * SelectionAdapter for the menu context.
     * 
     * @author Goekhan Guerkan
     */
    private class ButtonSelectionAdapter extends SelectionAdapter {

        private MenuItem itemAdd;

        private MenuItem itemEdit;

        private MenuItem itemRemove;

        private Table table;

        ButtonSelectionAdapter(MenuItem itemAdd, MenuItem itemEdit, MenuItem itemRemove, Table tab) {

            this.itemAdd = itemAdd;
            this.itemEdit = itemEdit;
            this.itemRemove = itemRemove;
            this.table = tab;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.widget == (itemAdd)) {
                onAddClicked();
            } else if (e.widget == itemEdit) {
                // edit selected; relies on proper button activation
                onEditClicked();
            } else if (e.widget == itemRemove) {
                // remove selected; relies on proper button activation
                onRemoveClicked();
            }
            // updateTable();
            updateContextActivation();
        }

        private void updateContextActivation() {
            itemEdit.setEnabled(table.getSelectionCount() > 0);
            itemRemove.setEnabled(table.getSelectionCount() > 0);
        }
    }

    /**
     * Sets a new configuration map that is chosen by the user and is used to update the page.
     * 
     * @param newConfigurationMap from the wizard
     */
    public abstract void setConfigMap(Map<String, Object> newConfigurationMap);

    protected void onAddClicked() {}

    protected void onEditClicked() {}

    protected void onRemoveClicked() {}

    /**
     * Updates the page before it is shown. This is for example for refreshing some gui elements.
     */
    public abstract void updatePage();
}
