/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.common.SwitchCondition;
import de.rcenvironment.components.switchcmp.execution.validator.SwitchComponentValidationMessage;
import de.rcenvironment.core.component.model.endpoint.api.EndpointChange;
import de.rcenvironment.core.component.model.endpoint.api.EndpointChange.Type;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeUtil;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * 
 * Condition section where user can set desired condition.
 * 
 * @author David Scholz
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class SwitchConditionSection extends ValidatingWorkflowNodePropertySection {

    private static final int TABLE_BUTTON_SIZE = 26;

    private static final int COLUMN_WEIGHT_NUMBER = 5;

    private static final int COLUMN_WEIGHT_CONDITION = 60;

    private static final int NUMBER_COLUMN = 0;

    private static final int CONDITION_COLUMN = 1;

    private static final String[] CONDITION_TABLE_TITLES = { "Number #", "Condition Script" };

    private static final String EXCEPTION_MESSAGE_WRITING = "Unexpected Exception occured, while writing JSON content String.";

    protected final Log log = LogFactory.getLog(SwitchConditionSection.class);

    private Button insertChannelButton;

    private Button insertOperatorButton;

    private Button insertConditionButton;

    private Combo channelCombo;

    private Combo operatorCombo;

    private Combo conditionCombo;

    private Button removeButton;

    private Button conditionUpButton;

    private Button conditionDownButton;

    private Table conditionTable;

    private TableViewer viewer;

    private ArrayList<SwitchCondition> contentList;

    private Point selectedPoint;

    private PropertyChangeListener registeredListener;

    private TextEditingSupport textEditingSupport;

    /**
     * 
     * Possible Condition Table Modifications on the {@link SwitchConditionSection}.
     *
     * @author Kathrin Schaffert
     *
     */
    protected enum TableRowBehavior {
        /** Add row. */
        ADD_ROW,
        /** Remove row. */
        REMOVE_ROW,
        /** Move selected row upwards. */
        MOVE_UP,
        /** Move selected row downwards. */
        MOVE_DOWN;
    }

    @Override
    public void createCompositeContent(final Composite parent, final TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        parent.setLayout(new GridLayout(1, false));
        parent.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        Section titleSection = aTabbedPropertySheetPage.getWidgetFactory().createSection(parent, ExpandableComposite.TITLE_BAR);
        titleSection.setLayout(new GridLayout());
        titleSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        titleSection.setText(Messages.conditionFieldString);

        final Composite conditionSectionComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(parent);
        conditionSectionComposite.setLayout(new GridLayout(3, false));
        conditionSectionComposite.setLayoutData(
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        // Definition of the composite on the left side of the table composite
        // +,-,up,down Buttons
        final Composite handlingComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(conditionSectionComposite);
        handlingComposite.setLayout(new GridLayout(1, false));
        handlingComposite.setLayoutData(new GridData(GridData.FILL | GridData.FILL_VERTICAL));

        GridData gridDataButtons = new GridData();
        gridDataButtons.widthHint = TABLE_BUTTON_SIZE;
        gridDataButtons.heightHint = TABLE_BUTTON_SIZE;

        Button addButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(handlingComposite, "+", SWT.PUSH);
        addButton.setLayoutData(gridDataButtons);
        removeButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(handlingComposite, "-", SWT.PUSH);
        removeButton.setLayoutData(gridDataButtons);
        conditionUpButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(handlingComposite, "\u2191", SWT.PUSH);
        conditionUpButton.setLayoutData(gridDataButtons);
        conditionDownButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(handlingComposite, "\u2193", SWT.PUSH);
        conditionDownButton.setLayoutData(gridDataButtons);

        addButton.addSelectionListener(modifyTableSelectionListener(TableRowBehavior.ADD_ROW));
        removeButton.addSelectionListener(modifyTableSelectionListener(TableRowBehavior.REMOVE_ROW));
        conditionUpButton.addSelectionListener(modifyTableSelectionListener(TableRowBehavior.MOVE_UP));
        conditionDownButton.addSelectionListener(modifyTableSelectionListener(TableRowBehavior.MOVE_DOWN));

        // Definition of the table composite
        final Composite conditionTableComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(conditionSectionComposite);
        conditionTableComposite.setLayout(new GridLayout(1, false));
        conditionTableComposite.setLayoutData(
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        // Layout for the Table
        TableColumnLayout layout = new TableColumnLayout();
        conditionTableComposite.setLayout(layout);

        viewer = new TableViewer(conditionTableComposite, SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);

        viewer.setContentProvider(new ArrayContentProvider());

        conditionTable = viewer.getTable();
        conditionTable.setHeaderVisible(true);
        conditionTable.setLinesVisible(true);
        conditionTable.addFocusListener(getConditionTableFocusListener());
        conditionTable.addSelectionListener(getConditionTableSelectionListener());
        conditionTable.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.CONDITION_KEY);
        textEditingSupport = new TextEditingSupport(viewer, conditionTable);
        createColumns(textEditingSupport);

        // Layout the Column Weight
        layout.setColumnData(viewer.getTable().getColumn(0), new ColumnWeightData(COLUMN_WEIGHT_NUMBER));
        layout.setColumnData(viewer.getTable().getColumn(1), new ColumnWeightData(COLUMN_WEIGHT_CONDITION));

        viewer.setInput(contentList);

        // Layout the viewer
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;

        viewer.getControl().setLayoutData(gridData);
        ColumnViewerToolTipSupport.enableFor(viewer);

        // Definition of the composite on the right side of the table composite
        // Insert Buttons and Combos
        final Composite propertiesComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(conditionSectionComposite);
        propertiesComposite.setLayout(new GridLayout(3, false));
        propertiesComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL | SWT.NO_FOCUS));

        Label operatorLabel = new Label(propertiesComposite, SWT.NONE);
        operatorLabel.setText(Messages.operatorsLabelString);
        operatorLabel.setBackground(propertiesComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        operatorLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
        operatorCombo = new Combo(propertiesComposite, SWT.READ_ONLY | SWT.NO_FOCUS);
        operatorCombo.setLayout(new GridLayout());
        operatorCombo.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        setComboOperators();
        operatorCombo.pack();
        insertOperatorButton = aTabbedPropertySheetPage.getWidgetFactory().createButton(propertiesComposite, Messages.insertButtonString,
            SWT.PUSH | SWT.NO_FOCUS);

        insertOperatorButton.addListener(SWT.Selection, new InsertButtonListener(operatorCombo));

        Label channelLabel = new Label(propertiesComposite, SWT.NONE);
        channelLabel.setText(Messages.channelLabelString);
        channelLabel.setBackground(propertiesComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        channelLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
        channelCombo = new Combo(propertiesComposite, SWT.READ_ONLY | SWT.NO_FOCUS);
        channelCombo.setLayout(new GridLayout());
        channelCombo.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        channelCombo.pack();
        insertChannelButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(propertiesComposite, Messages.insertButtonString,
                SWT.PUSH | SWT.NO_FOCUS);
        insertChannelButton.addListener(SWT.Selection, new InsertButtonListener(channelCombo));

        Label conditionLabel = new Label(propertiesComposite, SWT.NONE);
        conditionLabel.setText(Messages.conditionLabelString);
        conditionLabel.setBackground(propertiesComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        conditionLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
        conditionCombo = new Combo(propertiesComposite, SWT.READ_ONLY | SWT.NO_FOCUS);
        conditionCombo.setLayout(new GridLayout());
        conditionCombo.setLayoutData(new GridData(GridData.FILL | GridData.FILL_HORIZONTAL));
        conditionCombo.pack();
        insertConditionButton =
            aTabbedPropertySheetPage.getWidgetFactory().createButton(propertiesComposite, Messages.insertButtonString,
                SWT.PUSH | SWT.NO_FOCUS);
        insertConditionButton.addListener(SWT.Selection, new InsertButtonListener(conditionCombo));

        registerComboListeners(operatorCombo, channelCombo, conditionCombo);

        final Composite parameterComposite = aTabbedPropertySheetPage.getWidgetFactory().createComposite(parent);
        parameterComposite.setLayout(new GridLayout(2, false));
        parameterComposite.setLayoutData(new GridData(GridData.FILL));

        Button firstConditionCheckbox = new Button(parameterComposite, SWT.CHECK);
        firstConditionCheckbox.setData(CONTROL_PROPERTY_KEY, SwitchComponentConstants.WRITE_OUTPUT_KEY);
        firstConditionCheckbox.setSelection(false);

        Label firstConditionLabel = new Label(parameterComposite, SWT.NONE);
        firstConditionLabel.setText(Messages.writeOutputLabel);
        firstConditionLabel.setBackground(parameterComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        setPropertyControlsEnabled(false);
        setTableHandlingButtonsEnabled(false);

    }

    private void registerComboListeners(Combo... combo) {
        for (Combo c : combo) {
            c.addSelectionListener(new ComboSelectionListener());
            c.addFocusListener(new ComboFocusListener());
        }
    }

    private SelectionListener getConditionTableSelectionListener() {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean emptySelection = conditionTable.getSelectionCount() == 0;
                conditionUpButton.setEnabled(!emptySelection && conditionTable.getSelectionIndex() != 0);
                conditionDownButton.setEnabled(!emptySelection && conditionTable.getSelectionIndex() != conditionTable.getItemCount() - 1);
                removeButton.setEnabled(!emptySelection && conditionTable.getItemCount() > 1);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // nothing to do here
            }
        };
    }

    private FocusListener getConditionTableFocusListener() {
        return new FocusListener() {

            @Override
            public void focusLost(FocusEvent arg0) {
                if (!isMousePositionOnControl(removeButton, conditionDownButton, conditionUpButton)) {
                    setTableHandlingButtonsEnabled(false);
                    conditionTable.deselectAll();
                }
            }

            @Override
            public void focusGained(FocusEvent arg0) {
                // nothing to do here
            }
        };
    }

    private void setPropertyControlsEnabled(boolean enable) {
        insertOperatorButton.setEnabled(enable);
        operatorCombo.setEnabled(enable);
        insertChannelButton.setEnabled(enable && channelCombo.getItemCount() > 0);
        channelCombo.setEnabled(enable && channelCombo.getItemCount() > 0);
        insertConditionButton.setEnabled(enable && conditionCombo.getItemCount() > 0);
        conditionCombo.setEnabled(enable && conditionCombo.getItemCount() > 0);
    }

    // Checks whether the mouse cursor position is within the bounds of the controls or not
    private boolean isMousePositionOnControl(Control... controls) {
        for (Control c : controls) {
            if (c != null && c.isEnabled() && new Rectangle(0, 0, c.getSize().x, c.getSize().y)
                .contains(c.toControl(Display.getCurrent().getCursorLocation()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);

        if (registeredListener == null) {
            ComponentInstanceProperties config = getConfiguration();
            registeredListener = evt -> {
                if (evt.getNewValue() instanceof EndpointChange) {
                    EndpointChange ec = (EndpointChange) evt.getNewValue();
                    updateCombos(ec);
                    if (textEditingSupport.getCellEditor().isActivated()) {
                        setPropertyControlsEnabled(true);
                    }
                }
            };
            config.addPropertyChangeListener(registeredListener);
        }
    }

    private void updateCombos(EndpointChange ec) {
        if (ec.getType().equals(Type.Removed)) {
            // if type = removed get old endpoint description
            if (ec.getOldEndpointDescription().getDynamicEndpointIdentifier()
                .equals(SwitchComponentConstants.DATA_INPUT_ID) && !channelCombo.isDisposed()) {
                setInputChannels();
            }
            if (ec.getOldEndpointDescription().getDynamicEndpointIdentifier()
                .equals(SwitchComponentConstants.CONDITION_INPUT_ID) && !conditionCombo.isDisposed()) {
                setConditionInput();
            }
        } else {
            // if type = added or modified get current endpoint description
            if (ec.getEndpointDescription().getDynamicEndpointIdentifier()
                .equals(SwitchComponentConstants.DATA_INPUT_ID) && !channelCombo.isDisposed()) {
                setInputChannels();
            }
            if (ec.getEndpointDescription().getDynamicEndpointIdentifier()
                .equals(SwitchComponentConstants.CONDITION_INPUT_ID) && !conditionCombo.isDisposed()) {
                setConditionInput();
            }
        }
    }

    /**
     * Creates the columns for the condition table.
     */
    private void createColumns(TextEditingSupport textEditSup) {
        TableViewerColumn col0 = createTableViewerColumn(CONDITION_TABLE_TITLES[0]);
        TableViewerColumn col1 = createTableViewerColumn(CONDITION_TABLE_TITLES[1]);
        col0.setLabelProvider(new ConditionColumnLabelProvider(NUMBER_COLUMN));
        col1.setLabelProvider(new ConditionColumnLabelProvider(CONDITION_COLUMN));
        col1.setEditingSupport(textEditSup);
    }

    private TableViewerColumn createTableViewerColumn(String title) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.LEFT);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setResizable(false);
        return viewerColumn;
    }

    /**
     * Adds editing a cell in the table.
     * 
     * @author Kathrin Schaffert
     */
    private class TextEditingSupport extends EditingSupport {

        private final ConditionTableTextCellEditor editor;

        TextEditingSupport(TableViewer viewer, Table table) {
            super(viewer);
            this.editor = new ConditionTableTextCellEditor(table);
        }

        ConditionTableTextCellEditor getCellEditor() {
            return editor;
        }

        @Override
        protected boolean canEdit(Object arg0) {
            return true;
        }

        @Override
        protected CellEditor getCellEditor(Object arg0) {
            return editor;
        }

        @Override
        protected Object getValue(Object arg0) {

            return ((SwitchCondition) arg0).getConditionScript();

        }

        @Override
        protected void setValue(Object arg0, Object arg1) {

            @SuppressWarnings("unchecked") ArrayList<SwitchCondition> inputs = (ArrayList<SwitchCondition>) viewer.getInput();

            if (inputs.contains(((SwitchCondition) arg0))) {

                String configStr = getProperty(SwitchComponentConstants.CONDITION_KEY);

                ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
                int index = ((SwitchCondition) arg0).getConditionNumber() - 1;

                contentList = (ArrayList<SwitchCondition>) SwitchCondition.getSwitchConditionList(configStr);
                if (!arg1.equals("")) {
                    contentList.get(index).setConditionScript(arg1.toString());
                } else {
                    contentList.get(index).setConditionScript("");
                }

                try {
                    if (!configStr.equals(mapper.writeValueAsString(contentList))) {
                        final String oldValue = WorkflowNodeUtil.getConfigurationValue(node, SwitchComponentConstants.CONDITION_KEY);
                        setSwitchConditionProperty(SwitchComponentConstants.CONDITION_KEY, oldValue,
                            mapper.writeValueAsString(contentList));
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(EXCEPTION_MESSAGE_WRITING, e); // should never happen
                }
                viewer.setInput(contentList);
            }
            selectedPoint = textEditingSupport.getCellEditor().getSelection();
            updateErrorStates();
        }

        private void setSwitchConditionProperty(final String key, final String oldValue, final String newValue) {
            if ((oldValue != null && !oldValue.equals(newValue))
                || (oldValue == null && newValue != null)) {
                final WorkflowNodeCommand command = new SetConfigurationValueCommand(key, oldValue, newValue);
                execute(command);
            }
        }

    }

    /**
     * Condition Table {@link TextCellEditor} Implementation.
     * 
     * @author Kathrin Schaffert
     */
    private class ConditionTableTextCellEditor extends TextCellEditor {

        ConditionTableTextCellEditor(Composite parent) {
            super(parent);
        }

        public void activateCell(ViewerCell viewerCell) {
            if (viewerCell != null) {
                conditionTable.forceFocus();
                textEditingSupport.getViewer().editElement(viewerCell.getElement(), CONDITION_COLUMN);
                if (text != null) {
                    text.setSelection(selectedPoint.x, selectedPoint.y);
                }
            }
        }

        @Override
        public void activate() {
            setPropertyControlsEnabled(true);
            super.activate();
        }

        @Override
        protected void deactivate(ColumnViewerEditorDeactivationEvent event) {
            if (!isMousePositionOnControl(insertOperatorButton, insertConditionButton, insertChannelButton,
                operatorCombo, conditionCombo, channelCombo)) {
                setPropertyControlsEnabled(false);

                String oldContent = getContentListAsJSON();
                for (SwitchCondition con:contentList) {
                    String script = con.getConditionScript();
                    String replacedScript = script.trim().replaceAll("\\s+", " ");
                    con.setConditionScript(replacedScript);
                }
                String content = getContentListAsJSON();
                setPropertyNotUndoable(SwitchComponentConstants.CONDITION_KEY, oldContent, content);
            }
            super.deactivate(event);
        }

        private String getContentListAsJSON() {
            try {
                return new ObjectMapper().writeValueAsString(contentList);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(EXCEPTION_MESSAGE_WRITING, e); // should never happen
            }
        }

        @Override
        protected boolean dependsOnExternalFocusListener() {
            return false;
        }

        // sets the cursor to the end of the string instead of selecting the whole one
        @Override
        protected void doSetFocus() {
            super.doSetFocus();
            if (text != null) {
                text.setSelection(text.getText().length());
            }
        }

        @Override
        public boolean isRedoEnabled() {
            return false;
        }

        @Override
        public boolean isUndoEnabled() {
            return false;
        }

        protected Point getSelection() {
            return text.getSelection();
        }
    }

    private SelectionListener modifyTableSelectionListener(TableRowBehavior trb) {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                updateTable(trb);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }

        };
    }

    /**
     * Updates the conditionTable, when respective Buttons (+,-,up,down) are pushed.
     * 
     * @param trb requested TableRowBehavior (ADD_ROW, REMOVE_ROW, MOVE_UP, MOVE_DOWN)
     */

    private void updateTable(TableRowBehavior trb) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        String configStr = getProperty(SwitchComponentConstants.CONDITION_KEY);

        contentList = (ArrayList<SwitchCondition>) SwitchCondition.getSwitchConditionList(configStr);

        int newIndex = switchTableRowBehaviour(trb);

        String selected = getProperty(SwitchComponentConstants.SELECTED_CONDITION);

        try {
            if (!configStr.equals(mapper.writeValueAsString(contentList)) && selected != null
                && Integer.parseInt(selected) <= contentList.size()) {
                setProperty(SwitchComponentConstants.CONDITION_KEY, mapper.writeValueAsString(contentList));
            } else {
                setProperties(SwitchComponentConstants.CONDITION_KEY, mapper.writeValueAsString(contentList),
                    SwitchComponentConstants.SELECTED_CONDITION, null);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(EXCEPTION_MESSAGE_WRITING, e); // should never happen
        }
        if (newIndex != 0) {
            conditionTable.setSelection(newIndex - 1);
            conditionTable.notifyListeners(SWT.Selection, new Event());
        }
        conditionTable.forceFocus();
        updateErrorStates();
    }

    private int switchTableRowBehaviour(TableRowBehavior trb) {
        int index = viewer.getTable().getSelectionIndex();
        int newIndex = 0;
        int size = contentList.size();

        switch (trb) {
        case ADD_ROW:
            contentList.add(new SwitchCondition(size + 1, ""));
            updateOutputDescriptions(getConfiguration(), trb, size);
            newIndex = size + 1;
            break;
        case REMOVE_ROW:
            if (viewer.getTable().getSelectionCount() != 0) {
                contentList.remove(index);
                for (int i = index; i < size - 1; i++) {
                    contentList.get(i).setConditionNumber(i + 1);
                }
                updateOutputDescriptions(getConfiguration(), trb, size);
                updateValidationMessage(index + 1, 0);
                newIndex = index;
                if (newIndex == 0) {
                    newIndex = 1;
                }
            }
            break;
        case MOVE_UP:
            if (viewer.getTable().getSelectionCount() != 0 && index != 0) {
                Collections.swap(contentList, index, index - 1);
                contentList.get(index).setConditionNumber(index + 1);
                contentList.get(index - 1).setConditionNumber(index);
                updateValidationMessage(index, index + 1);
                newIndex = index;
            }
            break;
        case MOVE_DOWN:
            if (viewer.getTable().getSelectionCount() != 0 && index != contentList.size() - 1) {
                Collections.swap(contentList, index + 1, index);
                contentList.get(index).setConditionNumber(index + 1);
                contentList.get(index + 1).setConditionNumber(index + 2);
                updateValidationMessage(index + 1, index + 2);
                newIndex = index + 2;
            }
            break;
        default: // should never happen
            break;
        }
        return newIndex;
    }

    private void updateValidationMessage(int index1, int index2) {
        List<ComponentValidationMessage> messages =
            getMessageStore().getMessagesByComponentId(((WorkflowNode) getConfiguration()).getIdentifierAsObject().toString());
        for (ComponentValidationMessage m : messages) {
            if (m instanceof SwitchComponentValidationMessage) {
                SwitchComponentValidationMessage scvm = (SwitchComponentValidationMessage) m;
                if (scvm.getConditionTableRowNumber() == index2) {
                    scvm.setConditionTableRowNumber(index1);
                } else if (scvm.getConditionTableRowNumber() == index1) {
                    scvm.setConditionTableRowNumber(index2);
                }
            }
        }
    }

    /**
     * 
     * @param configStr configuration string of the SwitchComponentConstants.CONDITION_KEY Property
     * @return length of condition table list
     */
    protected static int getTableContentLength(String configStr) {
        return SwitchCondition.getSwitchConditionList(configStr).size();
    }

    /**
     * 
     * Updates the endpoint selection pane, when a row is added or removed via "+", "-" Button on the {@link SwitchConditionSection}.
     * 
     * @param numOfCon added or removed condition table index
     * @param trb TableRowBehavior of the condition table
     * @param config current configuration
     */

    protected static void updateOutputDescriptions(ComponentInstanceProperties config, TableRowBehavior trb, Integer numOfCon) {

        EndpointDescriptionsManager outputDescManager = config.getOutputDescriptionsManager();
        Set<EndpointDescription> dynEndDescs = config.getInputDescriptionsManager().getDynamicEndpointDescriptions();

        for (EndpointDescription endpointDesc : dynEndDescs) {
            if (!endpointDesc.getDynamicEndpointIdentifier().equals(SwitchComponentConstants.DATA_INPUT_ID)) {
                continue;
            }

            switch (trb) {
            case ADD_ROW:
                Map<String, String> metaData = new HashMap<>();
                if (numOfCon == 0) {
                    outputDescManager.addDynamicEndpointDescription(SwitchComponentConstants.DATA_OUTPUT_ID,
                        endpointDesc.getName() + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH,
                        endpointDesc.getDataType(), metaData);
                    outputDescManager.addDynamicEndpointDescription(SwitchComponentConstants.DATA_OUTPUT_ID,
                        endpointDesc.getName() + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + 1,
                        endpointDesc.getDataType(), metaData);
                } else {
                    outputDescManager.addDynamicEndpointDescription(SwitchComponentConstants.DATA_OUTPUT_ID,
                        endpointDesc.getName() + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION
                            + " " + Integer.toString(numOfCon + 1),
                        endpointDesc.getDataType(), metaData);
                }

                break;
            case REMOVE_ROW:
                if (numOfCon != 1) {
                    outputDescManager.removeDynamicEndpointDescription(
                        endpointDesc.getName() + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION
                            + " " + Integer.toString(numOfCon));
                } else {
                    outputDescManager.removeDynamicEndpointDescription(
                        endpointDesc.getName() + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + 1);
                    outputDescManager.removeDynamicEndpointDescription(
                        endpointDesc.getName() + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH);
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    protected void beforeTearingDownModelBinding() {
        ComponentInstanceProperties config = getConfiguration();
        config.removePropertyChangeListener(registeredListener);
        registeredListener = null;
        selectedPoint = null;
        contentList = null;
        super.beforeTearingDownModelBinding();
    }

    private void setTableHandlingButtonsEnabled(Boolean enable) {
        removeButton.setEnabled(enable);
        conditionUpButton.setEnabled(enable);
        conditionDownButton.setEnabled(enable);
    }

    /**
     * 
     * Set input channels to channel combo box in the {@link SwitchConditionSection}.
     * 
     * @author Kathrin Schaffert
     */
    private void setInputChannels() {
        channelCombo.removeAll();
        ArrayList<String> channels = new ArrayList<>();
        for (EndpointDescription channelName : getConfiguration().getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
            if (channelName.getDynamicEndpointIdentifier().equals(SwitchComponentConstants.DATA_INPUT_ID) && channelName.isRequired()
                && Arrays.asList(SwitchComponentConstants.CONDITION_SCRIPT_DATA_TYPES).contains(channelName.getDataType())) {
                channels.add(channelName.getName());
            }
        }
        Collections.sort(channels);
        channelCombo.setItems(channels.toArray(new String[channels.size()]));
        channelCombo.select(0); // default combo selection
    }

    /**
     * 
     * Set condition inputs to condition combo box in the {@link SwitchConditionSection}.
     * 
     * @author Kathrin Schaffert
     */
    private void setConditionInput() {
        conditionCombo.removeAll();
        ArrayList<String> channels = new ArrayList<>();
        for (EndpointDescription conditionName : getConfiguration().getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
            if (conditionName.getDynamicEndpointIdentifier().equals(SwitchComponentConstants.CONDITION_INPUT_ID)
                && conditionName.isRequired()) {
                channels.add(conditionName.getName());
            }
        }
        Collections.sort(channels);
        conditionCombo.setItems(channels.toArray(new String[channels.size()]));
        conditionCombo.select(0); // default combo selection
    }

    private void setComboOperators() {
        operatorCombo.removeAll();
        operatorCombo.setItems(SwitchComponentConstants.OPERATORS);
        operatorCombo.select(0);
    }

    private ViewerRow getViewerRow() {
        CellEditor cellEditor = textEditingSupport.getCellEditor();
        ViewerCell cell = textEditingSupport.getViewer().getCell(cellEditor.getControl().getLocation());
        if (cell != null) {
            return cell.getViewerRow();
        }
        return null;
    }

    @Override
    protected SwitchConditionSectionUpdater createUpdater() {
        return new SwitchConditionSectionUpdater();
    }

    /**
     * Switch Condition Section {@link DefaultUpdater} implementation of the handler to update the UI.
     * 
     * @author Kathrin Schaffert
     *
     */
    private class SwitchConditionSectionUpdater extends DefaultUpdater {

        @Override
        public void updateControl(final Control control, final String propertyName, String newValue,
            String oldValue) {
            super.updateControl(control, propertyName, newValue, oldValue);
            if (control instanceof Table && newValue != null && !newValue.equals(oldValue)) {

                ArrayList<SwitchCondition> tableValues = null;
                try {
                    tableValues = (ArrayList<SwitchCondition>) SwitchCondition.getSwitchConditionList(newValue);
                } finally {
                    viewer.setInput(tableValues);
                }
                viewer.refresh();
            }
        }
    }

    @Override
    protected void updateErrorStates(final List<ComponentValidationMessage> messages, final Composite parent) {
        if (parent != null && !parent.isDisposed() && viewer.getInput() != null) {
            for (final Control control : parent.getChildren()) {
                if (control.isDisposed()) {
                    continue;
                }
                if (control instanceof Composite) {
                    updateErrorStates(messages, (Composite) control);
                }
                final String key = (String) control.getData(CONTROL_PROPERTY_KEY);
                if (key != null && key.equals(SwitchComponentConstants.CONDITION_KEY)) {
                    @SuppressWarnings("unchecked") ArrayList<SwitchCondition> input = (ArrayList<SwitchCondition>) viewer.getInput();

                    // Reset messages
                    for (SwitchCondition sc : input) {
                        sc.setValidationMessages(null);
                    }

                    for (final ComponentValidationMessage message : messages) {
                        if (message instanceof SwitchComponentValidationMessage) {
                            int currentRow = ((SwitchComponentValidationMessage) message).getConditionTableRowNumber();
                            String errorMessage = ((SwitchComponentValidationMessage) message).getToolTipMessage();

                            int len = viewer.getTable().getItems().length;
                            if (currentRow != 0 && currentRow <= len) {
                                input.get(currentRow - 1).setValidationMessages(errorMessage);
                            }
                        }
                    }
                }
            }
            viewer.refresh();
        }
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        String configStr = getProperty(SwitchComponentConstants.CONDITION_KEY);
        if (configStr == null) {
            SwitchCondition emptyCondition = new SwitchCondition(1, "");
            ArrayList<SwitchCondition> content = new ArrayList<>();
            content.add(emptyCondition);
            try {
                setProperty(SwitchComponentConstants.CONDITION_KEY, mapper.writeValueAsString(content));
                configStr = getProperty(SwitchComponentConstants.CONDITION_KEY);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(EXCEPTION_MESSAGE_WRITING, e); // should never happen
            }
            log.warn(
                "The condition key in the workflow configuration file is damaged or missing. "
                    + "A valid key with an empty condition was generated automatically.");
            log.warn(" Please note that Data Outputs may have been lost and check the workflow configuration file.");
        }

        contentList = (ArrayList<SwitchCondition>) SwitchCondition.getSwitchConditionList(configStr);
        setInputChannels();
        setConditionInput();
        updateErrorStates();
    }

    private class InsertButtonListener implements Listener {

        private Combo combo;

        InsertButtonListener(Combo combo) {
            super();
            this.combo = combo;
        }

        @Override
        public void handleEvent(Event arg0) {
            String s = combo.getText();
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            // suppress warnings because the viewer's content model is represented by a MapContentProvider
            // and the viewer's input is of type Map
            @SuppressWarnings("unchecked") ArrayList<SwitchCondition> input = (ArrayList<SwitchCondition>) viewer.getInput();

            ViewerRow row = getViewerRow();
            if (row == null) {
                return;
            }

            int selectedRowNumber = Integer.parseInt(row.getText(0)) - 1;

            int start = selectedPoint.x;
            int end = selectedPoint.y;

            String currentString = row.getText(CONDITION_COLUMN);

            String firstSubstr = currentString.substring(0, start);
            String lastSubstr = currentString.substring(end);
            String newString = StringUtils.format("%s%s%s", firstSubstr, s, lastSubstr);
            input.get(selectedRowNumber).setConditionScript(newString);

            selectedPoint.x = selectedPoint.x + s.length();
            selectedPoint.y = selectedPoint.x;

            try {
                setProperty(SwitchComponentConstants.CONDITION_KEY, mapper.writeValueAsString(input));
            } catch (IOException e) {
                throw new RuntimeException(EXCEPTION_MESSAGE_WRITING, e); // should never happen
            }
            textEditingSupport.getCellEditor().activateCell(row.getCell(CONDITION_COLUMN));
        }
    }

    private class ComboSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // nothing to do here
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (getViewerRow() != null) {
                textEditingSupport.getCellEditor().activateCell(getViewerRow().getCell(CONDITION_COLUMN));
            }
        }

    }

    private class ComboFocusListener implements FocusListener {

        @Override
        public void focusLost(FocusEvent arg0) {
            if (!isMousePositionOnControl(insertOperatorButton, insertConditionButton, insertChannelButton,
                operatorCombo, conditionCombo, channelCombo)) {
                setPropertyControlsEnabled(false);
            }
        }

        @Override
        public void focusGained(FocusEvent arg0) {
            // nothing to do here
        }

    }

    private class ConditionColumnLabelProvider extends ColumnLabelProvider {

        private int columnIndex;

        ConditionColumnLabelProvider(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof SwitchCondition) {
                SwitchCondition condition = (SwitchCondition) element;
                if (columnIndex == CONDITION_COLUMN) {
                    return condition.getConditionScript();
                }
                if (columnIndex == NUMBER_COLUMN) {
                    return String.valueOf(condition.getConditionNumber());
                }
            }
            return "";
        }

        @Override
        public Color getBackground(Object element) {
            if (element instanceof SwitchCondition) {
                SwitchCondition condition = (SwitchCondition) element;
                if (condition.getValidationMessages() != null && columnIndex == CONDITION_COLUMN) {
                    return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                }
            }
            return null;
        }

        @Override
        public String getToolTipText(Object element) {
            if (element instanceof SwitchCondition) {
                SwitchCondition condition = (SwitchCondition) element;
                return condition.getValidationMessages();
            }
            return null;
        }
    }
}
