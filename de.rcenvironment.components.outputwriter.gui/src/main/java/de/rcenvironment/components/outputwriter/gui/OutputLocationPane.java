/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * UI part to add and edit OutputLocations in the OutputWriter Component.
 *
 * @author Brigitte Boden
 */

public class OutputLocationPane implements Refreshable {

    protected Section section;

    protected Composite client;

    protected ComponentInstanceProperties configuration;

    protected Table table;

    protected Button buttonAdd;

    protected Button buttonEdit;

    protected Button buttonRemove;

    protected ButtonSelectionAdapter buttonListener;

    protected MenuItem itemAdd;

    protected MenuItem itemEdit;

    protected MenuItem itemRemove;

    protected Image icon;

    protected TableColumnLayout tableLayout;

    protected Executor executor;

    protected ObjectMapper jsonMapper;

    public OutputLocationPane(Executor executor) {
        super();
        this.executor = executor;
        jsonMapper = JsonUtils.getDefaultObjectMapper();
        jsonMapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.Refreshable#refresh()
     */
    @Override
    public void refresh() {
        updateTable();

    }

    /**
     * @return the main Control
     */
    public Control getControl() {
        return section;
    }

    /**
     * Set the component instance configuration for configuration handling & storage; must not be null.
     * 
     * @param configuration Component configuration
     */
    public void setConfiguration(final ComponentInstanceProperties configuration) {
        this.configuration = configuration;
        updateTable();
    }

    /**
     * Creating gui.
     * 
     * @param parent parent Composite
     * @param title Title of selection pane
     * @param toolkit Formtoolkit to use
     * @return control
     */
    public Control createControl(final Composite parent, String title, FormToolkit toolkit) {
        section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText(title);
        client = toolkit.createComposite(section);
        client.setLayout(new GridLayout(2, false));
        final Composite tableComposite = toolkit.createComposite(client);
        tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
        table = toolkit.createTable(tableComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        table.setHeaderVisible(true);

        GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
        final int minHeight = 100;
        tableLayoutData.heightHint = minHeight; // effectively min height
        tableComposite.setLayoutData(tableLayoutData);

        Listener tableListener = getTableListener(parent);
        table.addListener(SWT.Dispose, tableListener);
        table.addListener(SWT.KeyDown, tableListener);
        table.addListener(SWT.MouseMove, tableListener);
        table.addListener(SWT.MouseHover, tableListener);
        table.addListener(SWT.MouseDoubleClick, tableListener);

        final int columnWeight = 20;

        // column - Target Name
        TableColumn colName = new TableColumn(table, SWT.NONE);
        colName.setText(de.rcenvironment.components.outputwriter.gui.Messages.outputLocFilename);
        // column - Target Folder
        TableColumn colFolder = new TableColumn(table, SWT.NONE);
        colFolder.setText(de.rcenvironment.components.outputwriter.gui.Messages.targetFolder);
        // column - Inputs
        TableColumn colInputs = new TableColumn(table, SWT.NONE);
        colInputs.setText(de.rcenvironment.components.outputwriter.gui.Messages.inputsForOutputLocation);

        tableLayout.setColumnData(colInputs, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colName, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(colFolder, new ColumnWeightData(columnWeight, true));

        buttonAdd = toolkit.createButton(client, Messages.add, SWT.FLAT);
        buttonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonEdit = toolkit.createButton(client, Messages.edit, SWT.FLAT);
        buttonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonEdit.setEnabled(false);
        buttonRemove = toolkit.createButton(client, Messages.remove, SWT.FLAT);
        buttonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        buttonRemove.setEnabled(false);

        table.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtonActivation();
            }

        });

        buttonListener = new ButtonSelectionAdapter();
        buttonAdd.addSelectionListener(buttonListener);
        buttonEdit.addSelectionListener(buttonListener);
        buttonRemove.addSelectionListener(buttonListener);

        fillContextMenu(table);
        table.addKeyListener(new DeleteKeyListener());

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);

        return section;
    }

    private void fillContextMenu(Table tab) {
        Menu menu = new Menu(tab);

        itemAdd = new MenuItem(menu, SWT.PUSH);
        itemAdd.setText(Messages.add);
        itemAdd.addSelectionListener(buttonListener);

        itemEdit = new MenuItem(menu, SWT.PUSH);
        itemEdit.setText(Messages.edit);
        itemEdit.addSelectionListener(buttonListener);

        itemRemove = new MenuItem(menu, SWT.PUSH);
        itemRemove.setText(Messages.remove);
        itemRemove.addSelectionListener(buttonListener);

        tab.setMenu(menu);
    }

    private Listener getTableListener(final Composite parent) {
        Listener tableListener = new Listener() {

            private Shell tip = null;

            @Override
            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.Dispose:
                case SWT.KeyDown:
                case SWT.MouseMove:
                    if (tip == null) {
                        break;
                    }
                    tip.dispose();
                    tip = null;
                    break;
                case SWT.MouseHover:
                    break;
                case SWT.MouseDoubleClick:
                    if (buttonEdit != null && buttonEdit.isEnabled() && event.button == 1) {
                        onEditClicked();
                    }
                    break;
                default:
                    break;
                }
            }
        };
        return tableListener;
    }

    /**
     * 
     * Delete by keyboard.
     *
     */
    private class DeleteKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent event) {
            if (event.keyCode == SWT.DEL) {
                if (buttonRemove.isEnabled()) {
                    onRemovedClicked();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent event) {}

    }

    protected void onRemovedClicked() {
        TableItem[] selection = table.getSelection();
        List<String> names = new LinkedList<String>();
        for (int i = 0; i < selection.length; i++) {
            names.add((String) selection[i].getData());
        }
        executeRemoveCommand(names);
    }

    protected void executeRemoveCommand(String name) {
        List<String> nameList = new ArrayList<String>();
        nameList.add(name);
        executeRemoveCommand(nameList);
    }

    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new RemoveOutputLocationsCommand(names, this);
        execute(command);
    }

    protected void updateTable() {
        if (!getControl().isDisposed()) {
            fillTable();
            updateButtonActivation();
        }
    }

    /**
     * Loads the current output locations into the UI table.
     */
    protected void fillTable() {
        if (client.getSize().x != 0) {
            table.removeAll();
            String jsonString = configuration.getConfigurationDescription()
                .getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
            if (jsonString == null) {
                jsonString = "";
            }
            try {
                OutputLocationList list =
                    jsonMapper.readValue(jsonString, OutputLocationList.class);

                for (OutputLocation ol : list.getOutputLocations()) {
                    TableItem item = new TableItem(table, SWT.None);
                    item.setData(ol.getGroupId());
                    item.setText(0, ol.getFilename());
                    item.setText(1, ol.getFolderForSaving());
                    item.setText(2, ol.getInputs().toString());
                }

            } catch (IOException e) {
                LogFactory.getLog(getClass()).debug("Error when reading targets from JSON: " + e.getMessage());
            }

        }
    }

    /**
     * SelectionAdapter for the Add, Edit and Remove Buttons.
     * 
     */
    private class ButtonSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.widget == buttonAdd || e.widget == itemAdd) {
                onAddClicked();
            } else if (e.widget == buttonEdit || e.widget == itemEdit) {
                onEditClicked();
            } else if (e.widget == buttonRemove || e.widget == itemRemove) {
                onRemovedClicked();
            }
        }
    }

    protected void onEditClicked() {
        final String id = (String) table.getSelection()[0].getData();
        OutputLocation selectedLocation = new OutputLocation();
        Set<String> paths = new TreeSet<String>();

        List<String> possibleInputs = getPossibleInputs();

        List<String> selectedInputs = new ArrayList<String>();
        List<String> otherOutputLocationNamesWithPahts = new ArrayList<String>();

        // Parse outputLocation Configuration for initializing the dialog values
        String jsonString = configuration.getConfigurationDescription()
            .getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
        if (jsonString == null) {
            jsonString = "{}";
        }
        try {
            OutputLocationList list = jsonMapper.readValue(jsonString, OutputLocationList.class);

            selectedLocation = list.getOutputLocationById(id);

            // Parse folders for paths and parse selected inputs
            for (OutputLocation ol : list.getOutputLocations()) {
                paths.add(ol.getFolderForSaving());
                if (!ol.getGroupId().equals(id)) {
                    selectedInputs.addAll(ol.getInputs());
                    otherOutputLocationNamesWithPahts.add(ol.getFolderForSaving() + File.separator + ol.getFilename());
                }
            }

        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when parsing JSON file " + e.getMessage());
        }

        OutputLocationEditDialog dialog =
            new OutputLocationEditDialog(Display.getDefault().getActiveShell(), Messages.outputLocationEditDialogTitle, paths,
                possibleInputs, selectedInputs, otherOutputLocationNamesWithPahts);
        dialog.initializeValues(selectedLocation);

        if (dialog.open() == Dialog.OK) {

            // If some values have changed, execute an edit command
            if (!dialog.getChosenFilename().equals(selectedLocation.getFilename())
                || !dialog.getChosenFolderForSaving().equals(
                    selectedLocation.getFolderForSaving())
                || !dialog.getChosenHeader().equals(
                    selectedLocation.getHeader())
                || !dialog.getChosenFormatString().equals(
                    selectedLocation.getFormatString())
                || !dialog.getChosenHandle().equals(
                    selectedLocation.getHandleExistingFile())
                || !dialog.getChosenInputSet().equals(selectedLocation.getInputs())) {
                OutputLocation out =
                    new OutputLocation(id, dialog.getChosenFilename(), dialog.getChosenFolderForSaving(), dialog.getChosenHeader(),
                        dialog.getChosenFormatString(), dialog.getChosenHandle(), dialog.getChosenInputSet());
                WorkflowNodeCommand command = new EditOutputLocationsCommand(out, this);
                execute(command);
            }
        }
        refresh();
    }

    protected void onAddClicked() {
        Set<String> paths = new TreeSet<String>();

        List<String> possibleInputs = getPossibleInputs();

        List<String> selectedInputs = new ArrayList<String>();
        List<String> otherOutputLocationNameswithPaths = new ArrayList<String>();

        // Parse outputLocation Configuration for initializing names, paths and possible inputs
        String jsonString = configuration.getConfigurationDescription()
            .getConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);

        if (jsonString == null) {
            jsonString = "{}";
        }
        try {
            OutputLocationList list = jsonMapper.readValue(jsonString, OutputLocationList.class);

            // Parse folders for paths and parse selected inputs
            for (OutputLocation ol : list.getOutputLocations()) {
                paths.add(ol.getFolderForSaving());
                selectedInputs.addAll(ol.getInputs());
                otherOutputLocationNameswithPaths.add(ol.getFolderForSaving() + File.separator + ol.getFilename());
            }

        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when parsing JSON file " + e.getMessage());
        }

        OutputLocationEditDialog dialog =
            new OutputLocationEditDialog(Display.getDefault().getActiveShell(),
                de.rcenvironment.components.outputwriter.gui.Messages.outputLocationAddDialogTitle, paths, possibleInputs,
                selectedInputs, otherOutputLocationNameswithPaths);
        if (dialog.open() == Dialog.OK) {
            OutputLocation out =
                new OutputLocation(dialog.getChosenFilename(), dialog.getChosenFolderForSaving(), dialog.getChosenHeader(),
                    dialog.getChosenFormatString(), dialog.getChosenHandle(), dialog.getChosenInputSet());
            WorkflowNodeCommand command = new EditOutputLocationsCommand(out, this);
            execute(command);
        }
        refresh();
    }

    private List<String> getPossibleInputs() {
        // Search component configuration for possible inputs (only simple data types can be written into an output location)
        List<String> possibleInputs = new ArrayList<String>();

        for (EndpointDescription input : configuration.getInputDescriptionsManager().getEndpointDescriptions()) {
            if (!input.getDataType().equals(DataType.FileReference) && !input.getDataType().equals(DataType.DirectoryReference)) {
                possibleInputs.add(input.getName());
            }
        }
        return possibleInputs;
    }

    protected void execute(final WorkflowNodeCommand command) {
        if (executor == null) {
            throw new RuntimeException("No executor set for execution of workflow node commands");
        }
        if (command != null) {
            executor.execute(command);
        }
    }

    /**
     * Enabled or disables the "add", "edit" and "remove" buttons.
     */
    protected void updateButtonActivation() {

        TableItem[] selection = table.getSelection();
        boolean hasSelection = selection.length != 0;
        buttonRemove.setEnabled(hasSelection);

        if (selection.length == 1 && !getPossibleInputs().isEmpty()) {
            buttonEdit.setEnabled(true);
        } else {
            buttonEdit.setEnabled(false);
        }

        if (getPossibleInputs().isEmpty()) {
            buttonAdd.setEnabled(false);
            //Only disable table if it is empty
            if (table.getItemCount() == 0) {
                table.setEnabled(false);
            }
        } else {
            buttonAdd.setEnabled(true);
            table.setEnabled(true);
        }

        itemEdit.setEnabled(buttonEdit.isEnabled());
        itemRemove.setEnabled(buttonRemove.isEnabled());
        itemAdd.setEnabled(buttonAdd.isEnabled());
    }

}
