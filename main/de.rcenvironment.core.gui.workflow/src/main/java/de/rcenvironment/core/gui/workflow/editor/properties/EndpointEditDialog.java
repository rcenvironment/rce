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
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A dialog for editing a single endpoint configuration.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 */
public class EndpointEditDialog extends Dialog {

    private static final String MINUS = "-";

    private static final String COLON = ":";

    private static final int GROUPS_MIN_WIDTH = 235;

    private static final int MINIMUM_HEIGHT = 125;

    private static final int MINIMUM_WIDTH = 250;

    protected final ComponentInstanceProperties configuration;

    protected final EndpointType type;

    protected Text textfieldName;

    protected Combo comboDataType;

    protected String initialName;

    protected String currentName;

    protected DataType currentDataType;

    protected EndpointDefinition.InputDatumHandling currentInputHandling;

    protected EndpointDefinition.InputExecutionContraint currentInputExecutionConstraint;

    protected String title;

    protected List<TypeSelectionOption> typeSelectionOptions;

    protected Map<Widget, String> widgetToKeyMap;

    protected EndpointMetaDataDefinition metaData;

    protected Map<String, String> metadataValues;

    protected EndpointDescriptionsManager epManager;

    private final String id;

    private final boolean isStatic;

    private Map<String, DataType> guiNameToDataType;

    private Combo comboInputDatumHandling;

    private Map<String, EndpointDefinition.InputDatumHandling> guiNameToInputDatumHandling;

    private Combo comboInputExecutionContraint;

    private Map<String, InputExecutionContraint> guiNameToInputExecutionConstraint;

    /**
     * Dialog for creating or editing an endpoint.
     * 
     * @param parentShell parent Shell
     * @param actionType
     * @param configuration the containing endpoint manager
     */
    public EndpointEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic,
        EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.configuration = configuration;
        type = direction;
        this.id = id;
        this.isStatic = isStatic;
        if (direction == EndpointType.INPUT) {
            epManager = configuration.getInputDescriptionsManager();
        } else {
            epManager = configuration.getOutputDescriptionsManager();
        }
        this.title = StringUtils.format(Messages.title, actionType, direction);
        this.metaData = metaData;
        this.metadataValues = metadataValues;

        if (!isStatic) { // if static initializeValues is called with the actual name
            setDataType();
            setInputHandling();
            setInputExecutionConstraint();
        }
    }

    private void setDataType() {
        if (isStatic || epManager.getEndpointDescription(currentName) != null) {
            currentDataType = epManager.getEndpointDescription(currentName).getDataType();
        } else {
            currentDataType = epManager.getDynamicEndpointDefinition(id).getDefaultDataType();
        }
    }

    private void setInputHandling() {
        if (metadataValues.containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)) {
            currentInputHandling = EndpointDefinition.InputDatumHandling.valueOf(
                metadataValues.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING));
        } else {
            if (isStatic || epManager.getEndpointDescription(currentName) != null) {
                currentInputHandling = epManager.getEndpointDescription(currentName).getEndpointDefinition()
                    .getDefaultInputDatumHandling();
            } else {
                currentInputHandling = epManager.getDynamicEndpointDefinition(id).getDefaultInputDatumHandling();
            }
        }
    }

    private void setInputExecutionConstraint() {
        if (metadataValues.containsKey(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
            currentInputExecutionConstraint = InputExecutionContraint.valueOf(
                metadataValues.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
        } else {
            if (isStatic || epManager.getEndpointDescription(currentName) != null) {
                currentInputExecutionConstraint = epManager.getEndpointDescription(currentName).getEndpointDefinition()
                    .getDefaultInputExecutionConstraint();
            } else {
                currentInputExecutionConstraint = epManager.getDynamicEndpointDefinition(id).getDefaultInputExecutionConstraint();
            }
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));
        GridData g = new GridData(GridData.FILL_BOTH);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g);

        createEndpointSettings(container);

        Composite configHeader = new Composite(container, SWT.FILL);
        configHeader.setLayout(new GridLayout(3, false));
        Label sep = new Label(configHeader, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.FILL);
        sep.setLayoutData(g);
        Label sectionTitle = new Label(configHeader, SWT.NONE);
        sectionTitle.setText(Messages.configurationHeader);
        Label sep2 = new Label(configHeader, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.FILL);
        sep2.setLayoutData(g);
        configHeader.setLayoutData(g);

        createConfigurationArea(container);
        return container;
    }

    protected Control createConfigurationArea(Composite parent) {
        widgetToKeyMap = new HashMap<Widget, String>();
        if (!metaData.getMetaDataKeys().isEmpty()) {
            Composite settingsComposite = (Composite) super.createDialogArea(parent);
            GridData g = new GridData(GridData.FILL, GridData.FILL, true, true);
            settingsComposite.setLayoutData(g);
            if (metaData != null) {
                Map<String, Map<Integer, String>> groups = new TreeMap<String, Map<Integer, String>>();
                for (String key : metaData.getMetaDataKeys()) {
                    String group = metaData.getGuiGroup(key);
                    Map<Integer, String> groupTree;
                    if (groups.containsKey(group)) {
                        groupTree = groups.get(group);
                    } else {
                        groupTree = new TreeMap<Integer, String>();
                    }
                    int position = metaData.getGuiPosition(key);
                    if (position < 0) {
                        groupTree.put(groupTree.size(), key);
                    } else {
                        while (groupTree.containsKey(position)) {
                            position++;
                        }
                        groupTree.put(position, key);
                    }
                    groups.put(group, groupTree);
                }
                for (String groupKey : groups.keySet()) {
                    createSettingsTab(settingsComposite, groupKey, groups.get(groupKey));
                }
            }

            return settingsComposite;
        } else {
            Label noMetaData = new Label(parent, SWT.NONE);
            noMetaData.setText(StringUtils.format(Messages.noConfig, type));
            GridData g = new GridData(GridData.FILL, GridData.FILL, true, true);
            g.horizontalAlignment = SWT.CENTER;
            noMetaData.setLayoutData(g);
            return parent;
        }
    }

    protected void createSettingsTab(Composite composite, String groupTitle, Map<Integer, String> sortedKeyMap) {
        Group configGroup = new Group(composite, SWT.CENTER);
        GridData g = new GridData(GridData.FILL_BOTH);
        configGroup.setLayoutData(g);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.CENTER;
        g.minimumWidth = GROUPS_MIN_WIDTH;
        configGroup.setText(groupTitle);
        configGroup.setLayout(new GridLayout(2, false));
        createSettings(sortedKeyMap, configGroup);
        if (configGroup.getChildren().length == 0) {
            configGroup.dispose();
        }
    }

    /**
     * Creates the settings.
     *
     * @param sortedKeyMap the sorted key map
     * @param container the container
     */
    private void createSettings(Map<Integer, String> sortedKeyMap, Composite container) {
        for (String key : sortedKeyMap.values()) {
            if (!metaData.getVisibility(key).equals(Visibility.developerConfigurable)
                && metadataIsActive(key, metaData.getActivationFilter(key))) {

                String value = metadataValues.get(key);
                if (value == null || value.equals("")) {
                    value = metaData.getDefaultValue(key);
                    metadataValues.put(key, value);
                }
                if (metaData.getDataType(key).equals(EndpointMetaDataConstants.TYPE_BOOL)) {
                    Button newCheckbox = createLabelAndCheckbox(container, metaData.getGuiName(key) + COLON, value);
                    widgetToKeyMap.put(newCheckbox, key);
                    newCheckbox.addSelectionListener(new SelectionChangedListener());
                } else if ((metaData.getPossibleValues(key) == null || metaData.getPossibleValues(key).contains("*"))) {
                    Text newTextfield = createLabelAndTextfield(container,
                        metaData.getGuiName(key) + COLON, metaData.getDataType(key), value);
                    widgetToKeyMap.put(newTextfield, key);
                    newTextfield.addModifyListener(new MethodPropertiesModifyListener());
                } else {
                    Combo newCombo = createLabelAndCombo(container, metaData.getGuiName(key) + COLON,
                        key, value);
                    widgetToKeyMap.put(newCombo, key);
                    newCombo.addModifyListener(new MethodPropertiesModifyListener());
                }
            }
        }
    }

    private boolean metadataIsActive(String key, Map<String, List<String>> activationFilter) {
        if (activationFilter != null) {
            boolean hasActiveFilter = false;

            for (String config : activationFilter.keySet()) {
                for (String value : activationFilter.get(config)) {
                    if (configuration.getConfigurationDescription().getActualConfigurationValue(config).equals(value)) {
                        hasActiveFilter = true;
                    }
                }
            }
            return hasActiveFilter;
        }
        return true;
    }

    private Button createLabelAndCheckbox(Composite container, String text, String value) {
        new Label(container, SWT.NONE).setText(text);
        Button result = new Button(container, SWT.CHECK);
        result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        if (value.equals("true")) {
            result.setSelection(true);
        } else {
            result.setSelection(false);
        }
        return result;
    }

    private Combo createLabelAndCombo(Composite container, String text, String key, String value) {
        new Label(container, SWT.NONE).setText(text);
        Combo combo = new Combo(container, SWT.READ_ONLY);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        for (String entry : metaData.getGuiNamesOfPossibleValues(key)) {
            combo.add(entry);
        }
        combo.select(metaData.getPossibleValues(key).indexOf(value));
        combo.setEnabled(metaData.getPossibleValues(key).size() > 1);
        return combo;
    }

    protected Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        new Label(container, SWT.NONE).setText(text);
        Text result = new Text(container, SWT.SINGLE | SWT.BORDER);
        result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        result.setText(value);
        if (dataType.equals(EndpointMetaDataConstants.TYPE_INT)) {
            result.addVerifyListener(new NumericalTextConstraintListener(result,
                NumericalTextConstraintListener.ONLY_INTEGER));
            if (value.equals(MINUS)) {
                result.setText("");
            }
        }
        if (dataType.equals(EndpointMetaDataConstants.TYPE_FLOAT)) {
            result.addVerifyListener(new NumericalTextConstraintListener(result,
                NumericalTextConstraintListener.ONLY_FLOAT));
            if (value.equals(MINUS)) {
                result.setText("");
            }
        }
        if (dataType.equals(EndpointMetaDataConstants.TYPE_FLOAT_GREATER_ZERO)) {
            result.addVerifyListener(new NumericalTextConstraintListener(result,
                NumericalTextConstraintListener.ONLY_FLOAT | NumericalTextConstraintListener.GREATER_ZERO));
            if (value.equals(MINUS)) {
                result.setText("");
            }
        }
        return result;
    }

    protected void createEndpointSettings(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridData g = new GridData(GridData.FILL, GridData.FILL, true, true);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(g);

        GridData textGridData = new GridData();
        textGridData.grabExcessHorizontalSpace = false;

        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText(Messages.name + COLON);
        nameLabel.setLayoutData(textGridData);

        textfieldName = new Text(container, SWT.SINGLE | SWT.BORDER);
        textfieldName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        textfieldName.setEditable((!isStatic && !epManager.getDynamicEndpointDefinition(id).isNameReadOnly()));
        textfieldName.addListener(SWT.Verify, new AlphanumericalTextContraintListener(true, true));

        // store initial name to skip validation if unchanged
        initialName = currentName;

        // set initial input when editing
        if (currentName != null) {
            textfieldName.setText(currentName);
        }

        Label dataTypeLabel = new Label(container, SWT.NONE);
        dataTypeLabel.setText(Messages.dataType + COLON);
        dataTypeLabel.setLayoutData(textGridData);
        createDataTypeComboBox(container);

        if (type == EndpointType.INPUT) {
            Label inputHandlingLabel = new Label(container, SWT.NONE);
            inputHandlingLabel.setText("Handling" + COLON);
            inputHandlingLabel.setLayoutData(textGridData);
            createInputHandlingComboBox(container);

            Label executionConstraintLabel = new Label(container, SWT.NONE);
            executionConstraintLabel.setText("Constraint" + COLON);
            executionConstraintLabel.setLayoutData(textGridData);
            createInputExecutionContraintComboBox(container);
        }

    }

    private void createDataTypeComboBox(Composite container) {
        comboDataType = new Combo(container, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
        comboDataType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        List<DataType> possibleDataTypes;
        if (isStatic) {
            possibleDataTypes = epManager.getStaticEndpointDefinition(currentName).getPossibleDataTypes();
        } else {
            if (epManager.getDynamicEndpointDefinition(id) != null) {
                possibleDataTypes = epManager.getDynamicEndpointDefinition(id).getPossibleDataTypes();
            } else {
                possibleDataTypes = new LinkedList<DataType>();
            }
        }
        guiNameToDataType = new HashMap<String, DataType>();
        List<String> dataTypesGuiNames = new LinkedList<String>();
        for (DataType t : possibleDataTypes) {
            dataTypesGuiNames.add(t.getDisplayName());
            guiNameToDataType.put(t.getDisplayName(), t);
        }
        Collections.sort(dataTypesGuiNames);
        comboDataType.setItems(dataTypesGuiNames.toArray(new String[dataTypesGuiNames.size()]));

        if (currentDataType != null) {
            comboDataType.select(comboDataType.indexOf(currentDataType.getDisplayName()));
        } else {
            comboDataType.select(comboDataType.indexOf(epManager.getDynamicEndpointDefinition(id).getDefaultDataType()
                .getDisplayName()));
        }

        comboDataType.setEnabled(possibleDataTypes.size() > 1);
    }

    private void createInputHandlingComboBox(Composite container) {
        comboInputDatumHandling = new Combo(container, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
        comboInputDatumHandling.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        List<EndpointDefinition.InputDatumHandling> possibleInputinputHandlingOptions;
        if (isStatic) {
            possibleInputinputHandlingOptions = epManager.getStaticEndpointDefinition(currentName).getInputDatumOptions();
        } else {
            if (epManager.getDynamicEndpointDefinition(id) != null) {
                possibleInputinputHandlingOptions = epManager.getDynamicEndpointDefinition(id).getInputDatumOptions();
            } else {
                possibleInputinputHandlingOptions = new LinkedList<EndpointDefinition.InputDatumHandling>();
            }
        }
        guiNameToInputDatumHandling = new HashMap<String, EndpointDefinition.InputDatumHandling>();
        List<String> inputHandlingGuiNames = new LinkedList<String>();
        for (EndpointDefinition.InputDatumHandling t : possibleInputinputHandlingOptions) {
            inputHandlingGuiNames.add(t.getDisplayName());
            guiNameToInputDatumHandling.put(t.getDisplayName(), t);
        }
        Collections.sort(inputHandlingGuiNames);
        comboInputDatumHandling.setItems(inputHandlingGuiNames.toArray(new String[inputHandlingGuiNames.size()]));

        if (currentInputHandling != null) {
            comboInputDatumHandling.select(comboInputDatumHandling.indexOf(currentInputHandling.getDisplayName()));
        } else {
            comboInputDatumHandling.select(comboInputDatumHandling.indexOf(epManager.getDynamicEndpointDefinition(id)
                .getDefaultInputDatumHandling().getDisplayName()));
        }

        comboInputDatumHandling.setEnabled(possibleInputinputHandlingOptions.size() > 1);
    }

    private void createInputExecutionContraintComboBox(Composite container) {
        comboInputExecutionContraint = new Combo(container, SWT.BORDER | SWT.READ_ONLY | SWT.RIGHT);
        comboInputExecutionContraint.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        List<EndpointDefinition.InputExecutionContraint> possibleInputHandlingOptions;
        if (isStatic) {
            possibleInputHandlingOptions = epManager.getStaticEndpointDefinition(currentName)
                .getInputExecutionConstraintOptions();
        } else {
            if (epManager.getDynamicEndpointDefinition(id) != null) {
                possibleInputHandlingOptions = epManager.getDynamicEndpointDefinition(id).getInputExecutionConstraintOptions();
            } else {
                possibleInputHandlingOptions = new LinkedList<EndpointDefinition.InputExecutionContraint>();
            }
        }
        guiNameToInputExecutionConstraint = new HashMap<String, EndpointDefinition.InputExecutionContraint>();
        List<String> executionConstraintsGuiNames = new LinkedList<String>();
        for (EndpointDefinition.InputExecutionContraint t : possibleInputHandlingOptions) {
            executionConstraintsGuiNames.add(t.getDisplayName());
            guiNameToInputExecutionConstraint.put(t.getDisplayName(), t);
        }
        Collections.sort(executionConstraintsGuiNames);
        comboInputExecutionContraint.setItems(executionConstraintsGuiNames.toArray(new String[executionConstraintsGuiNames.size()]));

        if (currentInputExecutionConstraint != null) {
            comboInputExecutionContraint.select(comboInputExecutionContraint.indexOf(currentInputExecutionConstraint.getDisplayName()));
        } else {
            comboInputExecutionContraint.select(comboInputExecutionContraint.indexOf(epManager.getDynamicEndpointDefinition(id)
                .getDefaultInputExecutionConstraint().getDisplayName()));
        }

        comboInputExecutionContraint.setEnabled(possibleInputHandlingOptions.size() > 1);
    }

    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);
        getShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);

        // initial validation
        validateInput();
        // set listeners here so the ok button is initialized
        installModifyListeners();
        validateInput();
    }

    private void installModifyListeners() {
        ModifyListener modifyListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        };
        textfieldName.addModifyListener(modifyListener);
        comboDataType.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                validateInput();
            }

        });
    }

    protected void validateInput() {

        String name = getNameInputFromUI();
        // initialName is null if not set, so it will not be equal when naming a new endpoint
        boolean nameIsValid = name.equals(initialName);
        nameIsValid |= epManager.isValidEndpointName(name);

        // enable/disable "ok"
        getButton(IDialogConstants.OK_ID).setEnabled(nameIsValid & validateMetaDataInputs());
    }

    /**
     * Returns Widget of fitting key or null.
     * 
     * @param key key of widget
     * @return widget or null
     */
    protected Widget getWidget(final String key) {
        for (Entry<Widget, String> entry : widgetToKeyMap.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Validated all current inputs in the dialog.
     */
    protected boolean validateMetaDataInputs() {
        boolean isValid = true;
        for (Widget widget : widgetToKeyMap.keySet()) {
            String key = "";
            if (metaData.getMetaDataKeys().contains(widgetToKeyMap.get(widget))) {
                key = widgetToKeyMap.get(widget);
            }
            String dataType = metaData.getDataType(key);
            String validation = metaData.getValidation(key);
            boolean visible = metaData.isDefinedForDataType(key, guiNameToDataType.get(comboDataType.getText()));
            boolean enabled = checkActivationFilter(metaData.getGuiActivationFilter(key), metadataValues) && visible;
            if (!dataType.equals(EndpointMetaDataConstants.TYPE_BOOL)
                && (metaData.getPossibleValues(key) == null || metaData.getPossibleValues(key).contains("*"))) {
                if (((Text) widget).getText().equals("") && (visible && enabled)
                    && (validation != null && (validation.contains("required")))) {
                    isValid = false;
                } else if (!((Text) widget).getText().equals("")) {
                    if (dataType.equalsIgnoreCase(EndpointMetaDataConstants.TYPE_INT)) {
                        int value = Integer.MAX_VALUE;
                        try {
                            value = Integer.parseInt(((Text) widget).getText());
                            isValid &= checkValidation(value, validation);
                        } catch (NumberFormatException e) {
                            value = Integer.MAX_VALUE;
                            isValid &= false;
                        }
                    }
                    if (dataType.equalsIgnoreCase(EndpointMetaDataConstants.TYPE_BOOL)) {
                        try {
                            Boolean.parseBoolean(((Text) widget).getText());
                        } catch (NumberFormatException e) {
                            isValid &= false;
                        }
                    }
                    if (dataType.equalsIgnoreCase(EndpointMetaDataConstants.TYPE_FLOAT)) {
                        double value = Double.MAX_VALUE;
                        try {
                            value = Double.parseDouble(((Text) widget).getText());
                            isValid &= checkValidation(value, validation);
                        } catch (NumberFormatException e) {
                            value = Double.MAX_VALUE;
                            isValid &= false;
                        }
                    }
                }
            }

            if (widget instanceof Text) {
                ((Text) widget).getParent().setVisible(visible);
                ((Text) widget).setEnabled(enabled);
            } else if (widget instanceof Combo) {
                ((Combo) widget).getParent().setVisible(visible);
                ((Combo) widget).setEnabled(enabled);
            } else if (widget instanceof Button) {
                ((Button) widget).getParent().setVisible(visible);
                ((Button) widget).setEnabled(enabled);
            }
            if (!enabled) {
                metadataValues.put(widgetToKeyMap.get(widget), MINUS);
            } else if (widget instanceof Text) {
                metadataValues.put(widgetToKeyMap.get(widget), ((Text) widget).getText());
            }
        }
        return isValid;
    }

    static boolean checkActivationFilter(Map<String, List<String>> filter, Map<String, String> metaDataValues) {
        if (filter != null && !filter.isEmpty()) {
            for (String filterKey : filter.keySet()) {
                for (String filterValues : filter.get(filterKey)) {
                    if (metaDataValues.get(filterKey) != null && metaDataValues.get(filterKey).equals(filterValues)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    protected boolean checkValidation(double value, String validation) {
        boolean result = true;
        if (validation != null && !validation.equals("")) {
            String[] splitValidations = validation.split(",");
            for (String argument : splitValidations) {
                if (argument.contains("<=")) {
                    double restriction = Double.parseDouble(argument.substring(2));
                    if (value > restriction) {
                        result = false;
                    }
                } else if (argument.contains(">=")) {
                    double restriction = Double.parseDouble(argument.substring(2));
                    if (value < restriction) {
                        result = false;
                    }
                } else if (argument.contains("<")) {
                    double restriction = Double.parseDouble(argument.substring(1));
                    if (value >= restriction) {
                        result = false;
                    }
                } else if (argument.contains(">")) {
                    double restriction = Double.parseDouble(argument.substring(1));
                    if (value <= restriction) {
                        result = false;
                    }
                }
            }
        }
        return result;
    }

    private boolean checkValidation(int value, String validation) {
        return checkValidation((double) value, validation);
    }

    /**
     * Updates meta data values and returns them.
     * 
     * @return meta data as {@link Map}
     */
    public Map<String, String> getMetadataValues() {
        if (type == EndpointType.INPUT) {
            metadataValues.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING, currentInputHandling.name());
            metadataValues.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT, currentInputExecutionConstraint.name());
        }
        return metadataValues;
    }

    /**
     * ModifyListener for changing the new values in the given MethodDescription.
     * 
     * @author Sascha Zur
     */
    private class MethodPropertiesModifyListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent event) {
            Widget source = (Widget) event.getSource();
            if (metaData.getMetaDataKeys().contains(widgetToKeyMap.get(source))) {
                String value = null;
                if (source instanceof Text) {
                    value = ((Text) source).getText();
                } else if (source instanceof Combo) {
                    int index = ((Combo) source).getSelectionIndex();
                    value = metaData.getPossibleValues(widgetToKeyMap.get(source)).get(index);
                }
                metadataValues.put(widgetToKeyMap.get(source), value);
            }

            validateInput();
        }
    }

    /**
     * Listener for changing checkbox values.
     * 
     * @author Sascha Zur
     */
    private class SelectionChangedListener extends SelectionAdapter {

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            Button source = (Button) e.getSource();
            if (metaData.getMetaDataKeys().contains(widgetToKeyMap.get(source))) {
                metadataValues.put(widgetToKeyMap.get(source), "" + source.getSelection());
            }
            validateInput();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            widgetDefaultSelected(e);
        }
    }

    /**
     * Initialize values.
     * 
     * @param name name of endpoint
     */
    public void initializeValues(String name) {
        currentName = name;
        setDataType();
        setInputHandling();
        setInputExecutionConstraint();
    }

    public String getChosenName() {
        return currentName;
    }

    public DataType getChosenDataType() {
        return currentDataType;
    }

    protected String getNameInputFromUI() {
        return textfieldName.getText();
    }

    protected DataType getTypeSelectionFromUI() {
        return guiNameToDataType.get(comboDataType.getText());
    }

    @Override
    protected void okPressed() {
        currentName = getNameInputFromUI();
        currentDataType = getTypeSelectionFromUI();
        if (type == EndpointType.INPUT) {
            currentInputHandling = guiNameToInputDatumHandling.get(comboInputDatumHandling.getText());
            currentInputExecutionConstraint = guiNameToInputExecutionConstraint.get(comboInputExecutionContraint.getText());
        }
        callSuperOkPressed();
    }

    protected void callSuperOkPressed() {
        super.okPressed();
    }

    @Override
    public void setBlockOnOpen(boolean shouldBlock) {
        super.setBlockOnOpen(true);
    }
}
