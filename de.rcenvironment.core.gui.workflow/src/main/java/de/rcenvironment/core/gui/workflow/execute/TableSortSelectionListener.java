/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Listener for sorting the table in the WorkflowExecutionWizard.
 * 
 * @author Goekhan Guerkan
 */

public class TableSortSelectionListener implements SelectionListener {

    /**
     * Constant to check which column was clicked.
     */
    public static final String COLUMN_NAME = "COLUMN_NAME";

    /**
     * Constant to check which column was clicked.
     */
    public static final String COLUMN_INSTANCE = "COLUMN_INSTANCE";

    private TableViewer viewer;

    private TableColumn column;

    private int currentDirection;

    private TableBehaviour updaterTable;

    public TableSortSelectionListener(TableViewer viewer, TableColumn column, int startDirection) {

        this.currentDirection = startDirection;
        this.viewer = viewer;
        this.column = column;
        this.column.addSelectionListener((SelectionListener) this);

    }

    private void setSortDirection() {

        currentDirection = viewer.getTable().getSortDirection();
        if (currentDirection == SWT.UP) {
            viewer.getTable().setSortDirection(SWT.DOWN);

        } else {

            viewer.getTable().setSortDirection(SWT.UP);

        }
    }

    @Override
    public void widgetSelected(SelectionEvent e) {

        updaterTable.saveIndexOfComboBeforeRefresh();
        viewer.getTable().setSortColumn(column);

        updaterTable.disposeWidgets();

        if (column.getData().equals(COLUMN_INSTANCE)) {

            viewer.setContentProvider(new WorkflowDescriptionContentProvider(currentDirection, COLUMN_INSTANCE));
            updaterTable.setCurrentlyUsedSortingColumn(2);

        } else {

            viewer.setContentProvider(new WorkflowDescriptionContentProvider(currentDirection, COLUMN_NAME));
            updaterTable.setCurrentlyUsedSortingColumn(1);

        }

        updaterTable.disposeWidgets();
        viewer.refresh();

        updaterTable.checkifAllClicked();
        updaterTable.checkIfDisableMasterBtn();

        updaterTable.setSavedComboIndex();

        setSortDirection();

    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        this.widgetSelected(e);
    }

    public void setUpdaterTable(TableBehaviour updaterTable) {
        this.updaterTable = updaterTable;
    }

}
