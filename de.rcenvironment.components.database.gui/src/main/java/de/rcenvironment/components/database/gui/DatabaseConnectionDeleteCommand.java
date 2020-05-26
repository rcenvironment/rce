/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.database.gui;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


/**
 * Database connection delete command.
 *
 * @author Oliver Seebach
 */
public class DatabaseConnectionDeleteCommand extends Command {

    private Table table;
    
    private int selectedItemIndex;

    private TableItem itemToBeRemoved;
    
    public DatabaseConnectionDeleteCommand(Table table, int selectedItemIndex) {
        super();
        this.table = table;
        this.selectedItemIndex = selectedItemIndex;
    }

    @Override
    public void execute() {
        itemToBeRemoved = table.getItem(selectedItemIndex);
        table.remove(selectedItemIndex);
    }
    
    @Override
    public void redo() {
        execute();
    }

    @Override
    public void undo() {
        TableItem restoredTableItem = new TableItem(table, SWT.NONE, selectedItemIndex);
        for (int i = 0; i < 8; i++){
            restoredTableItem.setText(i, itemToBeRemoved.getText(i));
        }
    }

}
