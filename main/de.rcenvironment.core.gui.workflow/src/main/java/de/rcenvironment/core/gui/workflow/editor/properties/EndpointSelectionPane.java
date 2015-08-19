/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.AssertionFailedException;
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

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.endpoint.EndpointHelper;
import de.rcenvironment.core.gui.workflow.Activator;
import de.rcenvironment.core.gui.workflow.EndpointHandlingHelper;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicEndpointCommand;

/**
 * A UI part to display and edit a set of endpoints managed by a {@link DynamicEndpointManager). 
 *
 * @author Robert Mischke
 * @author Christian Weiss
 * @author Sascha Zur
 */
public class EndpointSelectionPane implements Refreshable {

    private static final String NO_DATA_STRING = "-";

    protected EndpointType endpointType;

    /** The display text describing individual endpoints; usually "Input" or "Output". */
    protected String genericEndpointTitle;

    protected Section section;

    protected Composite client;

    protected ComponentInstanceProperties configuration;

    protected Table table;

    protected Button buttonAdd;

    protected Button buttonEdit;

    protected Button buttonRemove;

    protected MenuItem itemAdd;

    protected MenuItem itemEdit;

    protected MenuItem itemRemove;

    protected SelectionAdapter buttonListener;

    protected EndpointDescriptionsManager endpointManager;

    protected Image icon;

    protected String endpointIdToManage;

    protected boolean showOnlyManagedEndpoints;

    protected boolean showInputExecutionConstraint;

    protected TableColumnLayout tableLayout;

    private final WorkflowNodeCommand.Executor executor;

    private Map<String, Integer> guiKeyToColumnNumberMap;

    private boolean tableBuilt = false;

    private final boolean readonly;

    public EndpointSelectionPane(String genericEndpointTitle, EndpointType direction,
        final WorkflowNodeCommand.Executor executor, boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints) {
        this(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, showOnlyManagedEndpoints, true);
    }

    public EndpointSelectionPane(String genericEndpointTitle, EndpointType direction,
        final WorkflowNodeCommand.Executor executor, boolean readonly, String dynamicEndpointIdToManage, boolean showOnlyManagedEndpoints,
        boolean showInputExecutionConstraint) {
        this.genericEndpointTitle = genericEndpointTitle;
        this.endpointType = direction;
        this.executor = executor;
        this.readonly = readonly;
        endpointIdToManage = dynamicEndpointIdToManage;
        this.showOnlyManagedEndpoints = showOnlyManagedEndpoints;
        this.showInputExecutionConstraint = showInputExecutionConstraint;
        icon = Activator.getInstance().getImageRegistry().get(Activator.IMAGE_RCE_ICON_16);
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

        GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 5);
        final int minHeight = 140;
        tableLayoutData.heightHint = minHeight; // effectively min height
        tableComposite.setLayoutData(tableLayoutData);

        Listener tableListener = getTableListener(parent);
        table.addListener(SWT.Dispose, tableListener);
        table.addListener(SWT.KeyDown, tableListener);
        table.addListener(SWT.MouseMove, tableListener);
        table.addListener(SWT.MouseHover, tableListener);
        table.addListener(SWT.MouseDoubleClick, tableListener);

        final int columnWeight = 20;

        // first column - name
        TableColumn col1 = new TableColumn(table, SWT.NONE);
        col1.setText(Messages.name);
        // second column - data type
        TableColumn col2 = new TableColumn(table, SWT.NONE);
        col2.setText(Messages.dataType);

        if (endpointType == EndpointType.INPUT) {
            TableColumn col3 = new TableColumn(table, SWT.NONE);
            col3.setText("Handling");
            tableLayout.setColumnData(col3, new ColumnWeightData(columnWeight, true));
            if (showInputExecutionConstraint) {
                TableColumn col4 = new TableColumn(table, SWT.NONE);
                col4.setText("Constraint");
                tableLayout.setColumnData(col4, new ColumnWeightData(columnWeight, true));
            }
        }

        tableLayout.setColumnData(col1, new ColumnWeightData(columnWeight, true));
        tableLayout.setColumnData(col2, new ColumnWeightData(columnWeight, true));
        if (!readonly) {
            buttonAdd = toolkit.createButton(client, EndpointActionType.ADD.toString(), SWT.FLAT);
            buttonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            buttonEdit = toolkit.createButton(client, EndpointActionType.EDIT.toString(), SWT.FLAT);
            buttonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            buttonRemove = toolkit.createButton(client, EndpointActionType.REMOVE.toString(), SWT.FLAT);
            buttonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            table.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateButtonActivation();
                }

            });

            buttonListener = getButtonListener();
            buttonAdd.addSelectionListener(buttonListener);
            buttonEdit.addSelectionListener(buttonListener);
            buttonRemove.addSelectionListener(buttonListener);

            fillContextMenu(table);
            table.addKeyListener(new DeleteKeyListener());
        }

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);

        return section;
    }

    private void fillContextMenu(Table tab) {
        Menu menu = new Menu(tab);

        itemAdd = new MenuItem(menu, SWT.PUSH);
        itemAdd.setText(EndpointActionType.ADD.toString());
        itemAdd.addSelectionListener(buttonListener);

        itemEdit = new MenuItem(menu, SWT.PUSH);
        itemEdit.setText(EndpointActionType.EDIT.toString());
        itemEdit.addSelectionListener(buttonListener);

        itemRemove = new MenuItem(menu, SWT.PUSH);
        itemRemove.setText(EndpointActionType.REMOVE.toString());
        itemRemove.addSelectionListener(buttonListener);

        tab.setMenu(menu);
    }

    // This is an own method to give other components the possibility to add their own
    // ButtonListener to the pane.
    protected SelectionAdapter getButtonListener() {
        return new ButtonSelectionAdapter();
    }

    /**
     * Set the component instance configuration for configuration handling & storage; must not be
     * null.
     * 
     * @param configuration Component configuration
     */
    public void setConfiguration(final ComponentInstanceProperties configuration) {
        this.configuration = configuration;
        if (endpointType == EndpointType.INPUT) {
            endpointManager = configuration.getInputDescriptionsManager();
        } else {
            endpointManager = configuration.getOutputDescriptionsManager();
        }
        updateTable();
    }

    protected ComponentInstanceProperties getConfiguration() {
        return configuration;
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
     * Loads the current endpoint data into the UI table.
     */
    protected void fillTable() {
        // Prevent an Exception with some distribution of linux
        if (client.getSize().x != 0) {
            table.removeAll();
            guiKeyToColumnNumberMap = new HashMap<String, Integer>();
            List<String> shownMetaData = EndpointHelper.getMetaDataNamesForTable(endpointType, endpointIdToManage, getConfiguration());
            int i = 2;
            if (endpointType == EndpointType.INPUT) {
                if (showInputExecutionConstraint) {
                    i = 4;
                } else {
                    i = 3;
                }
            }
            for (String key : shownMetaData) {
                guiKeyToColumnNumberMap.put(key, i++);
            }
            if (!tableBuilt) {
                for (String key : shownMetaData) {
                    tableBuilt = true;
                    TableColumn col = null;
                    try {
                        col = new TableColumn(table, SWT.NONE, guiKeyToColumnNumberMap.get(key));
                        decorateColumn(key, col);
                        // Due to a layout gui bug under linux, this exception must be catched.
                        // Afterwards, the decoration of the column can be done without an error.
                    } catch (AssertionFailedException e) {
                        if (e.getMessage().contains("assertion failed: Unknown column layout data")) {
                            decorateColumn(key, table.getColumn(guiKeyToColumnNumberMap.get(key)));
                        } else {
                            throw e;
                        }
                    }
                }
            }
            if (showOnlyManagedEndpoints) {

                if (endpointManager.getDynamicEndpointDefinition(endpointIdToManage) != null) {
                    final List<String> dynamicEndpointNames = EndpointHelper.getDynamicEndpointNames(endpointType, endpointIdToManage,
                        configuration, showOnlyManagedEndpoints);
                    Collections.sort(dynamicEndpointNames);
                    fillCells(dynamicEndpointNames, false);
                } else {
                    List<String> staticEndpointNames = EndpointHelper.getStaticEndpointNames(endpointType, configuration);
                    Collections.sort(staticEndpointNames);
                    fillCells(staticEndpointNames, true);
                }

            } else {

                if (!showOnlyManagedEndpoints) {

                    List<String> staticEndpointNames = EndpointHelper.getStaticEndpointNames(endpointType, configuration);
                    Collections.sort(staticEndpointNames);
                    fillCells(staticEndpointNames, true);
                }
                final List<String> dynamicEndpointNames = EndpointHelper.getDynamicEndpointNames(endpointType, endpointIdToManage,
                    configuration, showOnlyManagedEndpoints);
                Collections.sort(dynamicEndpointNames);
                fillCells(dynamicEndpointNames, false);
            }
        }
    }

    private void decorateColumn(String key, TableColumn col) {
        final int columnWeight = 20;
        tableLayout.setColumnData(col, new ColumnWeightData(columnWeight, true));
        col.setText(key);
    }

    private void fillCells(List<String> endpointNames, boolean staticEndpoints) {
        for (String name : endpointNames) {
            TableItem item = new TableItem(table, SWT.None);
            item.setData(name);
            item.setText(0, name);
            Display display = Display.getCurrent();
            if (readonly || endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription().isReadOnly()) {
                item.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
            }
            if (endpointType == EndpointType.INPUT) {
                item.setImage(0, Activator.getInstance().getImageRegistry().get(Activator.IMAGE_INPUT));
            } else {
                item.setImage(0, Activator.getInstance().getImageRegistry().get(Activator.IMAGE_OUTPUT));
            }

            if (staticEndpoints) {
                item.setForeground(0, display.getSystemColor(SWT.COLOR_DARK_GRAY));
            } else {
                if (endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription().isNameReadOnly()) {
                    item.setForeground(0, display.getSystemColor(SWT.COLOR_DARK_GRAY));
                }
            }

            item.setText(1, endpointManager.getEndpointDescription(name).getDataType().getDisplayName());
            if (endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription().getPossibleDataTypes().size() < 2) {
                item.setForeground(1, display.getSystemColor(SWT.COLOR_DARK_GRAY));
            }

            if (endpointType == EndpointType.INPUT) {
                if (getMetaData(name).containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)) {
                    item.setText(2, EndpointDefinition.InputDatumHandling.valueOf(getMetaData(name)
                        .get(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)).getDisplayName());
                } else {
                    item.setText(2, endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription()
                        .getDefaultInputDatumHandling().getDisplayName());
                }
                if (endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription()
                    .getInputDatumOptions().size() < 2) {
                    item.setForeground(2, display.getSystemColor(SWT.COLOR_DARK_GRAY));
                }
                if (showInputExecutionConstraint) {
                    if (getMetaData(name).containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                        item.setText(3, EndpointDefinition.InputExecutionContraint.valueOf(getMetaData(name)
                            .get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)).getDisplayName());
                    } else {
                        item.setText(3, endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription()
                            .getDefaultInputExecutionConstraint().getDisplayName());
                    }
                    if (endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription()
                        .getInputExecutionConstraintOptions().size() < 2) {
                        item.setForeground(3, display.getSystemColor(SWT.COLOR_DARK_GRAY));
                    }
                } else {
                    item.setText(3, NO_DATA_STRING);
                }
            }

            Set<String> metaDataKeys = getMetaDataDescription(name).getMetaDataKeys();
            for (String key : metaDataKeys) {
                if (getMetaDataDescription(name).getVisibility(key) == Visibility.shown) {
                    if (getMetaData(name).get(key) != null && !getMetaData(name).get(key).isEmpty()
                        && !getMetaData(name).get(key).matches(ComponentUtils.PLACEHOLDER_REGEX)) {
                        if (EndpointEditDialog.checkActivationFilter(getMetaDataDescription(name).getGuiActivationFilter(key),
                            getMetaData(name)) && guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)) != null) {
                            item.setText(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)),
                                getMetaDataWithGuiNames(name).get(key).toString());
                            if (!isValueEditable(name, key)) {
                                item.setForeground(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)),
                                    display.getSystemColor(SWT.COLOR_DARK_GRAY));
                            }
                        } else {
                            item.setText(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)), NO_DATA_STRING);
                            item.setForeground(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)),
                                display.getSystemColor(SWT.COLOR_DARK_GRAY));
                        }

                    } else {
                        if (getMetaData(name).get(key).isEmpty() || getMetaData(name).get(key).matches(ComponentUtils.PLACEHOLDER_REGEX)) {
                            item.setText(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)), NO_DATA_STRING);
                            item.setForeground(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)),
                                display.getSystemColor(SWT.COLOR_DARK_GRAY));
                        } else {
                            if (getMetaDataDescription(name) != null && getMetaDataDescription(name).getDefaultValue(key) != null) {
                                item.setText(guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)),
                                    getMetaDataDescription(name).getDefaultValue(key).toString());
                            }
                        }
                    }
                }
            }
            for (String key : guiKeyToColumnNumberMap.keySet()) {
                if (item.getText(guiKeyToColumnNumberMap.get(key)) == null
                    || item.getText(guiKeyToColumnNumberMap.get(key)).isEmpty()) {
                    item.setText(guiKeyToColumnNumberMap.get(key), NO_DATA_STRING);
                    item.setForeground(guiKeyToColumnNumberMap.get(key),
                        display.getSystemColor(SWT.COLOR_DARK_GRAY));
                }
            }
        }
    }

    private boolean isValueEditable(String name, String key) {
        return !(getMetaDataDescription(name).getPossibleValues(key) != null
            && getMetaDataDescription(name).getPossibleValues(key).size() < 2
            && !getMetaDataDescription(name).getPossibleValues(key).contains(EndpointMetaDataConstants.PLACEHOLDER_ANY_POSSIBLE_VALUE));
    }

    /**
     * Enabled or disables the "add", "edit" and "remove" buttons.
     */
    protected void updateButtonActivation() {
        TableItem[] selection = table.getSelection();
        boolean hasSelection = selection.length != 0;
        boolean isDynamic = false;
        boolean dynamicReadOnly = false;
        if (hasSelection) {
            isDynamic = EndpointHelper.getDynamicEndpointNames(endpointType, endpointIdToManage, configuration,
                showOnlyManagedEndpoints).contains(selection[0].getText());
            if (isDynamic) {
                dynamicReadOnly =
                    endpointManager.getEndpointDescription(selection[0].getText()).getDeclarativeEndpointDescription().isReadOnly();
            }
        }
        buttonRemove.setEnabled(hasSelection && (isDynamic && !dynamicReadOnly));

        boolean editAble = false;
        if (hasSelection && !isDynamic) {
            editAble =
                endpointManager.getEndpointDescription(selection[0].getText()).
                    getDeclarativeEndpointDescription().getPossibleDataTypes()
                    .size() > 1
                    || endpointManager.getEndpointDescription(selection[0].getText()).
                        getDeclarativeEndpointDescription().getInputDatumOptions()
                        .size() > 1
                    || endpointManager.getEndpointDescription(selection[0].getText()).
                        getDeclarativeEndpointDescription().getInputExecutionConstraintOptions()
                        .size() > 1;
            EndpointMetaDataDefinition metaDescription =
                endpointManager.getEndpointDescription(selection[0].getText()).getDeclarativeEndpointDescription()
                    .getMetaDataDefinition();
            if (metaDescription != null && metaDescription.getMetaDataKeys().size() > 0) {
                for (String key : metaDescription.getMetaDataKeys()) {
                    editAble |= isValueEditable(selection[0].getText(), key);

                }
            }
        }
        if (selection.length == 1) {
            buttonEdit.setEnabled(editAble || (isDynamic && !dynamicReadOnly));
        } else {
            buttonEdit.setEnabled(false);
        }

        buttonAdd.setEnabled(endpointManager.getDynamicEndpointDefinition(endpointIdToManage) != null);

        itemEdit.setEnabled(buttonEdit.isEnabled());
        itemRemove.setEnabled(buttonRemove.isEnabled());
        itemAdd.setEnabled(buttonAdd.isEnabled());
    }

    /**
     * @return the main Control
     */
    public Control getControl() {
        return section;
    }

    protected void updateTable() {
        if (!getControl().isDisposed()) {
            fillTable();
            if (!readonly) {
                updateButtonActivation();
            }
        }
    }

    protected DataType getType(String name) {
        return endpointManager.getEndpointDescription(name).getDataType();
    }

    protected Map<String, String> getMetaData(String name) {
        return endpointManager.getEndpointDescription(name).getMetaData();
    }

    protected Map<String, String> getMetaDataWithGuiNames(String name) {
        Map<String, String> metaData = endpointManager.getEndpointDescription(name).getMetaData();
        Map<String, String> metaDataWithGuiNames = new HashMap<>();
        EndpointDefinition endpointDefinition;
        String dynId = endpointManager.getEndpointDescription(name).getDynamicEndpointIdentifier();
        if (dynId != null) {
            endpointDefinition = endpointManager.getDynamicEndpointDefinition(dynId);
        } else {
            endpointDefinition = endpointManager.getStaticEndpointDefinition(name);
        }
        if (endpointDefinition == null) {
            return metaData;
        }

        for (String key : endpointDefinition.getMetaDataDefinition().getMetaDataKeys()) {
            List<String> possibleValues = endpointDefinition.getMetaDataDefinition().getPossibleValues(key);
            if (possibleValues != null && !possibleValues.isEmpty()) {
                List<String> guiNamesOfPossibleValues = endpointDefinition.getMetaDataDefinition().getGuiNamesOfPossibleValues(key);
                if (possibleValues.indexOf(metaData.get(key)) >= 0) {
                    metaDataWithGuiNames.put(key, guiNamesOfPossibleValues.get(possibleValues.indexOf(metaData.get(key))));
                } else {
                    metaDataWithGuiNames.put(key, metaData.get(key));
                }
            } else {
                metaDataWithGuiNames.put(key, metaData.get(key));
            }
        }
        return metaDataWithGuiNames;
    }

    protected EndpointMetaDataDefinition getMetaDataDescription(String name) {
        return endpointManager.getEndpointDescription(name).getDeclarativeEndpointDescription()
            .getMetaDataDefinition();
    }

    /**
     * SelectionAdapter for the button of the pane.
     * 
     * @author Sascha Zur
     */
    private class ButtonSelectionAdapter extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.widget == buttonAdd || e.widget == itemAdd) {
                onAddClicked();
            } else if (e.widget == buttonEdit || e.widget == itemEdit) {
                // edit selected; relies on proper button activation
                onEditClicked();
            } else if (e.widget == buttonRemove || e.widget == itemRemove) {
                // remove selected; relies on proper button activation
                onRemovedClicked();
            }
            // updateTable();
        }
    }

    /**
     * 
     * KeyListener to delete endpoints via keyboard.
     * 
     * @author Marc Stammerjohann
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

    protected void onAddClicked(EndpointEditDialog dialog) {
        if (dialog.open() == Dialog.OK) {
            String name = dialog.getChosenName();
            DataType type = dialog.getChosenDataType();
            executeAddCommand(name, type, dialog.getMetadataValues());
        }
    }

    protected void onAddClicked() {
        EndpointEditDialog dialog =
            new EndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, endpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(endpointIdToManage)
                    .getMetaDataDefinition(), new HashMap<String, String>());

        onAddClicked(dialog);
    }

    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new AddDynamicEndpointCommand(endpointType, endpointIdToManage, name, type, metaData, this);
        execute(command);
    }

    protected void onEditClicked(String name, EndpointEditDialog dialog, Map<String, String> newMetaData) {

        dialog.initializeValues(name);

        if (dialog.open() == Dialog.OK) {

            EndpointDescription oldDesc = endpointManager.getEndpointDescription(name);

            String newName = dialog.getChosenName();
            DataType newType = dialog.getChosenDataType();
            newMetaData = dialog.getMetadataValues();

            if (isEndpointChanged(oldDesc, newName, newType, newMetaData)) {

                if (EndpointHandlingHelper.editEndpointDataType(endpointType, oldDesc, newType)) {
                    editEndpoint(oldDesc, newName, newType, newMetaData);
                }
            }
        }
    }

    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        boolean isStaticEndpoint = EndpointHelper.getStaticEndpointNames(endpointType, configuration).contains(name);
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        EndpointEditDialog dialog =
            new EndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                endpointIdToManage, isStaticEndpoint, endpoint.getDeclarativeEndpointDescription()
                    .getMetaDataDefinition(), newMetaData);

        onEditClicked(name, dialog, newMetaData);
    }

    protected void editEndpoint(EndpointDescription oldDesc, String newName, DataType newType, Map<String, String> newMetaData) {
        EndpointDescription newDesc = endpointManager.getEndpointDescription(oldDesc.getName());

        if (!newName.equals(oldDesc.getName())) {
            newDesc.setName(newName);
        }
        newDesc.setDataType(newType);
        newDesc.setMetaData(newMetaData);
        executeEditCommand(oldDesc, newDesc);
    }

    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command = new EditDynamicEndpointCommand(endpointType, oldDescription, newDescription, this);
        execute(command);
    }

    protected void onRemovedClicked() {
        TableItem[] selection = table.getSelection();
        List<String> names = new LinkedList<String>();
        for (int i = 0; i < selection.length; i++) {
            names.add((String) selection[i].getData());
        }
        executeRemoveCommand(names);
    }

    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new RemoveDynamicEndpointCommand(endpointType, endpointIdToManage, names, this);
        execute(command);
    }

    protected boolean isEndpointChanged(EndpointDescription oldDesc, String newName, DataType newType, Map<String, String> newMetaData) {
        if (oldDesc.getName().equals(newName) && oldDesc.getDataType().equals(newType)) {
            for (String key : newMetaData.keySet()) {
                if (!oldDesc.getMetaData().containsKey(key) || !oldDesc.getMetaDataValue(key).equals(newMetaData.get(key))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    protected Map<String, String> cloneMetaData(Map<String, String> original) {
        Map<String, String> copy = new HashMap<String, String>();
        for (Entry<String, String> e : original.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }

    @Override
    public void refresh() {
        updateTable();
    }
}
