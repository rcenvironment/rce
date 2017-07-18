/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.ArrayList;
import java.util.Arrays;
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
 * @author Doreen Seider
 */
public class EndpointSelectionPane implements Refreshable {

    /**
     * Constant for read only type.
     */
    public static final int NOTHING_READ_ONLY = 1;

    /**
     * Constant for read only type.
     */
    public static final int NAME_AND_TYPE_READ_ONLY = 2;

    /**
     * Constant for read only type.
     */
    public static final int ALL_READ_ONLY = 4;

    private static final String NO_DATA_STRING = "-";

    protected EndpointType endpointType;

    /** The display text describing individual endpoints; usually "Input" or "Output". */
    protected String paneTitle;

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

    protected String dynEndpointIdToManage;

    protected String dynEndpointIdToManagePassed;

    protected List<String> dynEndpointIdsToShow;

    protected List<String> statEndpointNamesToShow;

    protected boolean showEndpointCharacter;

    protected TableColumnLayout tableLayout;

    protected final WorkflowNodeCommand.Executor executor;

    private Map<String, Integer> guiKeyToColumnNumberMap;

    private boolean tableBuilt = false;

    private Map<String, String> metaDataInput = new HashMap<>();

    private boolean refreshDynEndpointIdsToShow = false;

    private boolean refreshStatEndpointNamesToShow = false;

    private int readOnlyType = 0;

    /**
     * @param dynEndpointIdToManage dynamic endpoint identifier to manage or <code>null</code> for none
     * @param dynEndpointIdsToShow list of dyamic endpoint ids to consider or <code>null</code> if all should be considered
     * @param statEndpointNamesToShow list of static endpoint names to consider or <code>null</code> if all should be considered
     */
    public EndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, String[] dynEndpointIdsToShow,
        String[] statEndpointNamesToShow, WorkflowNodeCommand.Executor executor) {
        this(title, direction, dynEndpointIdToManage, dynEndpointIdsToShow, statEndpointNamesToShow, executor, false);
    }

    public EndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, String[] dynEndpointIdsToShow,
        String[] statEndpointNamesToShow, WorkflowNodeCommand.Executor executor, boolean readOnly) {
        this(title, direction, dynEndpointIdToManage, dynEndpointIdsToShow, statEndpointNamesToShow, executor, readOnly, false);
    }

    public EndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, String[] dynEndpointIdsToShow,
        String[] statEndpointNamesToShow, WorkflowNodeCommand.Executor executor, boolean readOnly, boolean showCharacter) {
        this(title, direction, dynEndpointIdToManage, dynEndpointIdsToShow, statEndpointNamesToShow, executor, getButtons(readOnly),
            showCharacter);

    }

    public EndpointSelectionPane(String title, EndpointType direction, String dynEndpointIdToManage, String[] dynEndpointIdsToShow,
        String[] statEndpointNamesToShow, WorkflowNodeCommand.Executor executor, int readOnlyType, boolean showCharacter) {
        this.paneTitle = title;
        this.endpointType = direction;
        this.dynEndpointIdToManagePassed = dynEndpointIdToManage;
        this.showEndpointCharacter = showCharacter;
        if (dynEndpointIdsToShow == null) {
            refreshDynEndpointIdsToShow = true;
        } else {
            this.dynEndpointIdsToShow = new ArrayList<>(Arrays.asList(dynEndpointIdsToShow));
            if (dynEndpointIdToManage != null) {
                this.dynEndpointIdsToShow.add(dynEndpointIdToManage);
            }
        }
        if (statEndpointNamesToShow == null) {
            refreshStatEndpointNamesToShow = true;
        } else {
            this.statEndpointNamesToShow = Arrays.asList(statEndpointNamesToShow);
        }
        this.executor = executor;
        this.readOnlyType = readOnlyType;
        icon = Activator.getInstance().getImageRegistry().get(Activator.IMAGE_RCE_ICON_16);
    }

    private static int getButtons(boolean readOnly) {
        int buttons = ALL_READ_ONLY;
        if (!readOnly) {
            buttons = NOTHING_READ_ONLY;
        }
        return buttons;
    }

    /**
     * Updates the dynamic endpoint identifier to manage.
     * 
     * @param newDynEndpointIdToManage new dynamic endpoint identifier
     */
    public void updateDynamicEndpointIdToManage(String newDynEndpointIdToManage) {
        this.dynEndpointIdToManage = newDynEndpointIdToManage;
        this.dynEndpointIdToManagePassed = newDynEndpointIdToManage;
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
        // Set initial size to avoid bug when size is 0
        client.setSize(1, 1);
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

        // first column - name
        addColumn(0, Messages.name);
        // second column - data type
        addColumn(1, Messages.dataType);
        if (endpointType == EndpointType.INPUT) {
            addColumn(2, "Handling");
            addColumn(3, "Constraint");
        }

        tableBuilt = false;
        if ((readOnlyType & ALL_READ_ONLY) != 1) {
            buttonListener = getButtonListener();
        }
        if ((readOnlyType & NOTHING_READ_ONLY) == 1) {
            buttonAdd = toolkit.createButton(client, EndpointActionType.ADD.getButtonText(), SWT.FLAT);
            buttonAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            buttonAdd.addSelectionListener(buttonListener);
        }
        if ((readOnlyType & (NOTHING_READ_ONLY | NAME_AND_TYPE_READ_ONLY)) != 0) {
            buttonEdit = toolkit.createButton(client, EndpointActionType.EDIT.getButtonText(), SWT.FLAT);
            buttonEdit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            buttonEdit.addSelectionListener(buttonListener);
        }
        if ((readOnlyType & NOTHING_READ_ONLY) == 1) {
            buttonRemove = toolkit.createButton(client, EndpointActionType.REMOVE.getButtonText(), SWT.FLAT);
            buttonRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            buttonRemove.addSelectionListener(buttonListener);
            table.addKeyListener(new DeleteKeyListener());
        }
        if ((readOnlyType & ALL_READ_ONLY) != 1) {
            table.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateButtonActivation();
                }

            });

            fillContextMenu(table);
        }

        section.setClient(client);
        toolkit.paintBordersFor(client);
        section.setExpanded(true);

        return section;
    }

    private void fillContextMenu(Table tab) {
        Menu menu = new Menu(tab);
        if (buttonAdd != null) {
            itemAdd = new MenuItem(menu, SWT.PUSH);
            itemAdd.setText(EndpointActionType.ADD.toString());
            itemAdd.addSelectionListener(buttonListener);
        }
        if (buttonEdit != null) {
            itemEdit = new MenuItem(menu, SWT.PUSH);
            itemEdit.setText(EndpointActionType.EDIT.toString());
            itemEdit.addSelectionListener(buttonListener);
        }
        if (buttonRemove != null) {
            itemRemove = new MenuItem(menu, SWT.PUSH);
            itemRemove.setText(EndpointActionType.REMOVE.toString());
            itemRemove.addSelectionListener(buttonListener);
        }
        tab.setMenu(menu);
    }

    // This is an own method to give other components the possibility to add their own
    // ButtonListener to the pane.
    protected SelectionAdapter getButtonListener() {
        return new ButtonSelectionAdapter();
    }

    /**
     * Set the component instance configuration for configuration handling & storage; must not be null.
     * 
     * @param compInstProps Component configuration
     */
    public void setConfiguration(final ComponentInstanceProperties compInstProps) {
        this.configuration = compInstProps;
        if (endpointType == EndpointType.INPUT) {
            endpointManager = compInstProps.getInputDescriptionsManager();
        } else {
            endpointManager = compInstProps.getOutputDescriptionsManager();
        }
        if (refreshDynEndpointIdsToShow) {
            dynEndpointIdsToShow = EndpointHelper.getAllDynamicEndpointIds(endpointType, compInstProps);
        }
        if (refreshStatEndpointNamesToShow) {
            statEndpointNamesToShow = EndpointHelper.getAllStaticEndpointNames(endpointType, compInstProps);
        }
        if (EndpointHelper.getAllDynamicEndpointIds(endpointType, compInstProps).isEmpty()) {
            dynEndpointIdToManage = null;
        } else {
            dynEndpointIdToManage = dynEndpointIdToManagePassed;
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
            guiKeyToColumnNumberMap = new HashMap<>();
            List<String> shownMetaData =
                EndpointHelper.getMetaDataNamesForTable(endpointType, dynEndpointIdsToShow, statEndpointNamesToShow, getConfiguration());
            int i = 2;
            if (endpointType == EndpointType.INPUT) {
                i = 4;
            }
            for (String key : shownMetaData) {
                guiKeyToColumnNumberMap.put(key, i++);
            }
            if (!tableBuilt) {
                for (String key : shownMetaData) {
                    addColumn(guiKeyToColumnNumberMap.get(key), key);
                }
                if (showEndpointCharacter) {
                    addColumn(i, "Loop level");
                }

            }
            List<String> dynEndpointNames = getDynamicEndpointNames(dynEndpointIdsToShow);
            Collections.sort(dynEndpointNames);
            fillCells(dynEndpointNames, false);
            fillCells(statEndpointNamesToShow, true);
        }

        // Fix a bug in the GUI under some Linux distributions
        final int columnWeight = 20;
        for (int col = 0; col < table.getColumnCount(); col++) {
            tableLayout.setColumnData(table.getColumn(col), new ColumnWeightData(columnWeight, true));
        }
    }

    private void addColumn(int index, String title) {
        tableBuilt = true;
        TableColumn col = null;
        try {
            col = new TableColumn(table, SWT.NONE, index);
            decorateColumn(title, col);
            // Due to a layout gui bug under linux, this exception must be catched.
            // Afterwards, the decoration of the column can be done without an error.
        } catch (AssertionFailedException e) {
            if (e.getMessage().contains("assertion failed: Unknown column layout data")) {
                decorateColumn(title, table.getColumn(index));
            } else {
                throw e;
            }
        }
    }

    private void decorateColumn(String key, TableColumn col) {
        final int columnWeight = 20;
        tableLayout.setColumnData(col, new ColumnWeightData(columnWeight, true));
        col.setText(key);
    }

    /**
     * Gets a List of all dynamic endpoint names from the given configuration in the given direction.
     * 
     * @param direction if it should be in- or outputs.
     * @param id of dynamic endpoints
     * @param configuration to look at
     * @param filter filter for id
     * @return List of all dynamic endpoint names
     */
    private List<String> getDynamicEndpointNames(List<String> endpointIds) {
        List<String> result = new LinkedList<>();
        for (EndpointDescription e : endpointManager.getDynamicEndpointDescriptions()) {
            if (endpointIds.contains(e.getEndpointDefinition().getIdentifier())) {
                result.add(e.getName());
            }
        }
        return result;
    }

    private void fillCells(List<String> endpointNames, boolean staticEndpoints) {
        for (String name : endpointNames) {
            TableItem item = new TableItem(table, SWT.None);
            item.setData(name);
            item.setText(0, name);
            Display display = Display.getCurrent();
            if (readOnlyType != NOTHING_READ_ONLY || endpointManager.getEndpointDescription(name).getEndpointDefinition().isReadOnly()) {
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
                if (endpointManager.getEndpointDescription(name).getEndpointDefinition().isNameReadOnly()) {
                    item.setForeground(0, display.getSystemColor(SWT.COLOR_DARK_GRAY));
                }
            }

            item.setText(1, endpointManager.getEndpointDescription(name).getDataType().getDisplayName());
            if (endpointManager.getEndpointDescription(name).getEndpointDefinition().getPossibleDataTypes().size() < 2) {
                item.setForeground(1, display.getSystemColor(SWT.COLOR_DARK_GRAY));
            }
            int lastIndex = 1;
            if (endpointType == EndpointType.INPUT) {
                if (getMetaData(name).containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)) {
                    item.setText(2, EndpointDefinition.InputDatumHandling.valueOf(getMetaData(name)
                        .get(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)).getDisplayName());
                } else {
                    item.setText(2, endpointManager.getEndpointDescription(name).getEndpointDefinition()
                        .getDefaultInputDatumHandling().getDisplayName());
                }
                if (endpointManager.getEndpointDescription(name).getEndpointDefinition().getInputDatumOptions().size() < 2) {
                    item.setForeground(2, display.getSystemColor(SWT.COLOR_DARK_GRAY));
                } else {
                    item.setForeground(2, display.getSystemColor(SWT.COLOR_BLACK));
                }
                if (getMetaData(name).containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                    item.setText(3, EndpointDefinition.InputExecutionContraint.valueOf(getMetaData(name)
                        .get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)).getDisplayName());
                } else {
                    item.setText(3, endpointManager.getEndpointDescription(name).getEndpointDefinition()
                        .getDefaultInputExecutionConstraint().getDisplayName());
                }
                if (endpointManager.getEndpointDescription(name).getEndpointDefinition().getInputExecutionConstraintOptions().size() < 2) {
                    item.setForeground(3, display.getSystemColor(SWT.COLOR_DARK_GRAY));
                } else {
                    item.setForeground(3, display.getSystemColor(SWT.COLOR_BLACK));
                }
                lastIndex = 3;
            }
            Set<String> metaDataKeys = getMetaDataDescription(name).getMetaDataKeys();
            for (String key : metaDataKeys) {
                if (getMetaDataDescription(name).getVisibility(key) == Visibility.shown
                    && EndpointHelper.checkMetadataFilter(getMetaDataDescription(name).getGuiVisibilityFilter(key), getMetaData(name),
                        configuration.getConfigurationDescription())) {
                    lastIndex = Math.max(lastIndex, guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)));
                    if (getMetaData(name).get(key) != null && !getMetaData(name).get(key).isEmpty()
                        && !getMetaData(name).get(key).matches(ComponentUtils.PLACEHOLDER_REGEX)) {
                        if (EndpointHelper.checkMetadataFilter(getMetaDataDescription(name).getGuiActivationFilter(key),
                            getMetaData(name), configuration.getConfigurationDescription())
                            && guiKeyToColumnNumberMap.get(getMetaDataDescription(name).getGuiName(key)) != null) {
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
                    lastIndex = Math.max(lastIndex, guiKeyToColumnNumberMap.get(key));
                    item.setText(guiKeyToColumnNumberMap.get(key), NO_DATA_STRING);
                    item.setForeground(guiKeyToColumnNumberMap.get(key),
                        display.getSystemColor(SWT.COLOR_DARK_GRAY));
                }
            }
            lastIndex++;
            item.setText(lastIndex,
                endpointManager.getEndpointDescription(name).getEndpointDefinition().getEndpointCharacter().getDisplayName(endpointType));
            item.setForeground(lastIndex, display.getSystemColor(SWT.COLOR_DARK_GRAY));
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
        boolean addible = endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage) != null;
        boolean editable = false;
        boolean removable = false;
        if (hasSelection) {
            boolean containsStaticOrReadOnly = selectionContainsStaticOrReadOnly(Arrays.asList(selection));
            editable = selection.length == 1 && !containsStaticOrReadOnly;
            removable = !containsStaticOrReadOnly;
            if (selection.length == 1) {
                EndpointDefinition definition = endpointManager.getEndpointDescription(selection[0].getText()).getEndpointDefinition();
                editable = (definition.getPossibleDataTypes().size() > 1 && readOnlyType == NOTHING_READ_ONLY)
                    || definition.getInputDatumOptions().size() > 1
                    || definition.getInputExecutionConstraintOptions().size() > 1;
                EndpointMetaDataDefinition metaDescription = definition.getMetaDataDefinition();
                if (metaDescription != null && !metaDescription.getMetaDataKeys().isEmpty()) {
                    for (String key : metaDescription.getMetaDataKeys()) {
                        editable |= isValueEditable(selection[0].getText(), key);
                    }
                }
            }
        }
        if (buttonAdd != null) {
            buttonAdd.setEnabled(addible);
            itemAdd.setEnabled(addible && buttonAdd.isVisible());
        }
        if (buttonEdit != null) {
            buttonEdit.setEnabled(editable);
            itemEdit.setEnabled(editable);
        }
        if (buttonRemove != null) {
            buttonRemove.setEnabled(removable);
            itemRemove.setEnabled(removable && buttonRemove.isVisible());
        }
    }

    private boolean selectionContainsStaticOrReadOnly(List<TableItem> tableItems) {
        for (TableItem item : tableItems) {
            if (endpointManager.getEndpointDescription(item.getText()).getEndpointDefinition().isReadOnly()
                || endpointManager.getEndpointDescription(item.getText()).getEndpointDefinition().isStatic()) {
                return true;
            }
        }
        return false;
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
            if (readOnlyType == NOTHING_READ_ONLY || readOnlyType == NAME_AND_TYPE_READ_ONLY) {
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
        return endpointManager.getEndpointDescription(name).getEndpointDefinition()
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
            Map<String, String> metaData = dialog.getMetadataValues();
            metaData.putAll(metaDataInput);
            executeAddCommand(name, type, metaData);
        }
    }

    protected void onAddClicked() {
        EndpointEditDialog dialog =
            new EndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, dynEndpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
                    .getMetaDataDefinition(),
                new HashMap<String, String>());

        onAddClicked(dialog);
    }

    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new AddDynamicEndpointCommand(endpointType, dynEndpointIdToManage, name, type, metaData, this);
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
        boolean isStaticEndpoint = endpointManager.getEndpointDescription(name).getEndpointDefinition().isStatic();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());
        EndpointEditDialog dialog = new EndpointEditDialog(Display.getDefault().getActiveShell(),
            EndpointActionType.EDIT, configuration, endpointType,
            endpointManager.getEndpointDescription(name).getDynamicEndpointIdentifier(), isStaticEndpoint,
            endpoint.getEndpointDefinition().getMetaDataDefinition(), newMetaData, readOnlyType);
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
        List<String> names = new LinkedList<>();
        for (TableItem element : selection) {
            names.add((String) element.getData());
        }
        executeRemoveCommand(names);
    }

    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new RemoveDynamicEndpointCommand(endpointType, dynEndpointIdToManage, names, this);
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
        Map<String, String> copy = new HashMap<>();
        for (Entry<String, String> e : original.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }

    public void setMetaDataInput(Map<String, String> metaDataInput) {
        this.metaDataInput = metaDataInput;
    }

    @Override
    public void refresh() {
        updateTable();
    }

    public void setEndpointIdToManage(String endpointIdToManage) {
        this.dynEndpointIdToManage = endpointIdToManage;
    }
}
