/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Updater for a Table: Implementation of {@link Updatable}.
 * 
 * @author Goekhan Guerkan
 */
public class TableBehaviour extends AbstractUpdateBehavior {

    private TableViewer tableViewer;

    private Color color;

    private int currentlyUsedSortingColumn = 1;

    public TableBehaviour(TableViewer tableviewer, WorkflowNodeTargetPlatformLabelProvider instanceProvider,
        CheckboxLabelProvider checkProvider) {

        super(instanceProvider, checkProvider);
        this.tableViewer = tableviewer;

    }

    @Override
    public void updateInstanceColumn(ViewerCell cell) {
        final WorkflowNode workflowNode = (WorkflowNode) cell
            .getElement();
        TableItem item = (TableItem) cell.getItem();

        TableEditor editor = new TableEditor(tableViewer.getTable());
        final CCombo combo = new CCombo(tableViewer.getTable(), SWT.READ_ONLY);

        combo.setData(workflowNode);
        combo.setData(EDITOR, editor);
        combo.setBackground(color);

        combo.addListener(SWT.Resize, new Listener() {

            @Override
            public void handleEvent(final Event argEvent) {
                combo.setText(combo.getText());
            }
        });

        if (!comboList.contains(combo)) {
            comboList.add(combo);

        }

        editor.grabHorizontal = true;
        editor.horizontalAlignment = SWT.BEGINNING;

        editor.setEditor(combo, item, 2);

        if (editingSupport.getValues(workflowNode).size() > 0) {

            for (String value : editingSupport.getValues(workflowNode)) {

                combo.add(value);

            }

        } else {

            combo.setEnabled(false);
            combo.setText("No target instance available");
        }

        final Integer selectionIndex = (Integer) editingSupport
            .getValue(workflowNode);

        if (selectionIndex != null && !(selectionIndex > combo.getItemCount())) {
            combo.select(selectionIndex);
        } else {
            combo.select(0);
        }

        instanceProvider.handleSelection(combo, workflowNode);

        combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (combo != null) {
                    instanceProvider.handleSelection(combo, workflowNode);
                    instanceProvider.getPage().prepareErrorStatement();
                }

            }
        });

        prepareValuesForMasterCombo();

    }

    @Override
    public void setComboColor(Color colorToSet) {
        this.color = colorToSet;
    }

    @Override
    public void refreshColumns() {

        tableViewer.getTable().setRedraw(false);

        checkProvider.clearButtonList();
        instanceProvider.clearComboList();

        // the table is sorted by instances.
        if (currentlyUsedSortingColumn == 2) {
            if (tableViewer.getTable().getSortDirection() == SWT.UP) {
                tableViewer.setContentProvider(new WorkflowDescriptionContentProvider(SWT.UP, TableSortSelectionListener.COLUMN_INSTANCE));

            } else {
                tableViewer
                    .setContentProvider(new WorkflowDescriptionContentProvider(SWT.DOWN, TableSortSelectionListener.COLUMN_INSTANCE));
            }
            // it is sorted by names.
        } else {

            if (tableViewer.getTable().getSortDirection() == SWT.UP) {
                tableViewer.setContentProvider(new WorkflowDescriptionContentProvider(SWT.UP, TableSortSelectionListener.COLUMN_NAME));

            } else {
                tableViewer
                    .setContentProvider(new WorkflowDescriptionContentProvider(SWT.DOWN, TableSortSelectionListener.COLUMN_NAME));
            }

        }

        isCheckBoxColumnEnabled();

        if (columnImageActive) {

            if (allCheckboxesClicked) {
                tableViewer.getTable().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_CHECKED));

            } else {

                tableViewer.getTable().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_UNCHECKED));

            }
        }
        tableViewer.getTable().setRedraw(true);
        tableViewer.getTable().redraw();
        tableViewer.getTable().update();

        checkIfDisableMasterBtn();

    }

    @Override
    public void updateCheckBoxColumn(ViewerCell cell) {

        WorkflowNode node = (WorkflowNode) cell.getElement();

        TableEditor editor = new TableEditor(tableViewer.getTable());
        TableItem item = (TableItem) cell.getItem();
        final Button btn = new Button(tableViewer.getTable(), SWT.CHECK);

        btn.setBackground(cell.getBackground());

        synchronizeButtons();

        if (node.isChecked()) {
            btn.setSelection(true);
        } else {
            btn.setSelection(false);
        }

        checkIfDisableMasterBtn();

        btn.setData(KEY_CHECK, node);
        btn.setData(EDITOR, editor);

        btn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                saveIndexOfComboBeforeRefresh();

                WorkflowNode node = (WorkflowNode) btn.getData(KEY_CHECK);
                node.setChecked(btn.getSelection());
                if (!checkifAllClicked() && columnImageActive) {
                    allCheckboxesClicked = false;
                    tableViewer.getTable().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_UNCHECKED));
                }

                prepareValuesForMasterCombo();
                checkIfDisableMasterBtn();

                setSavedComboIndex();
            }

        });
        btnList.add(btn);

        editor.grabHorizontal = true;
        editor.horizontalAlignment = SWT.LEFT;

        editor.setEditor(btn, item, 0);

        // Disable the checkboxex if only one target instance available.

        if (editingSupport.getValues(node).size() <= 1) {
            btn.setEnabled(false);
        } else {

            btn.setEnabled(true);
        }

        isCheckBoxColumnEnabled();
        if (!columnImageActive) {
            tableViewer.getTable().getColumn(0).setImage(checkDisabled);
        } else {
            tableViewer.getTable().getColumn(0).setImage(uncheckedImg);

        }

    }

    @Override
    public boolean useFilter(String filterText, Object element) {

        WorkflowNode node = (WorkflowNode) element;

        if (filterText == null) {
            return true;
        }
        if (filterText.equals("")) {
            return true;
        }

        String filterTextSmall = filterText.toLowerCase();
        String temp = node.getComponentDescription().getNode().toString().toLowerCase();

        String componentName = node.getName().toLowerCase();
        String targetInstance = temp.replaceFirst("\"", "");

        if (targetInstance.contains(filterTextSmall) || componentName.contains(filterTextSmall)) {
            return true;
        }

        return false;

    }

    /**
     * Checks if all checkboxes are selected.
     * 
     * @return true if all buttons clicked
     */

    public boolean checkifAllClicked() {
        boolean clicked = true;

        List<Button> allEnabledBtns = new ArrayList<Button>();

        for (Button b : btnList) {

            if (b.isEnabled()) {
                allEnabledBtns.add(b);
            }
        }

        for (Button btn : allEnabledBtns) {

            if (!btn.getSelection()) {
                clicked = false;
            }
        }
        if (columnImageActive) {
            if (clicked && btnList.size() > 0) {
                allCheckboxesClicked = true;
                tableViewer.getTable().getColumn(0).setImage(ImageManager.getInstance().getSharedImage(StandardImages.CHECK_CHECKED));

            }
        }
        return clicked;
    }

    private void synchronizeButtons() {

        for (Button b : btnList) {

            WorkflowNode node = (WorkflowNode) b.getData(TableBehaviour.KEY_CHECK);
            b.setSelection(node.isChecked());

        }
    }

    public int getCurrentlyUsedSortingColumn() {
        return currentlyUsedSortingColumn;
    }

    public void setCurrentlyUsedSortingColumn(int currentlyUsedSortingColumn) {
        this.currentlyUsedSortingColumn = currentlyUsedSortingColumn;
    }

}
