/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants.FileSourceType;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * Manages endpoint descriptions of the Input Provider component.
 * 
 * @author Mark Geiger
 * @author Doreen Seider
 */
public class InputProviderEndpointEditDialog extends EndpointEditDialog implements SelectionListener {

    private Label label;

    private Text textField;

    private Combo combo;

    private Button selectFromFileSystemButton;

    private Composite confContainer;

    private NumericalTextConstraintListener floatListener;

    private NumericalTextConstraintListener integerListener;

    private Button selectFromProjectButton;

    private Button selectAtStartCheckbox;

    private Label atStartLabel;
    
    public InputProviderEndpointEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, Image icon, EndpointMetaDataDefinition metaData,
        Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    @Override
    protected void createEndpointSettings(Composite parent) {
        super.createEndpointSettings(parent);
        comboDataType.addSelectionListener(this);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {}

    @Override
    public void widgetSelected(SelectionEvent event) {

        removeListeners();

        initialize();
    }

    @Override
    protected Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        confContainer = container;

        createValueArea();

        createFileSelectionArea();

        createListeners();

        initialize();

        initializeWithValue(value);

        return textField;
    }

    private void initializeWithValue(String value) {
        if (metadataValues.containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
            && metadataValues.get(InputProviderComponentConstants.META_FILESOURCETYPE)
                .equals(InputProviderComponentConstants.FileSourceType.atWorkflowStart.name())) {
            selectAtStartCheckbox.setSelection(true);
            setSelectedAtStart();
        } else {
            textField.setText(value);
        }
        if (combo != null) {
            combo.select(combo.indexOf(value));
        }
    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        if (combo != null) {
            combo.setBounds(textField.getBounds());
        }
    }

    private void initialize() {

        if (!selectAtStartCheckbox.getSelection()) {
            textField.setText("");
            textField.setEnabled(true);
        }

        String dataType = comboDataType.getText();

        setTextFieldOrComboVisible(true);
        // enabled to enter value at workflow start for all data types
        setDataTypeSelected(true);

        if (dataType.equals(DataType.Integer.getDisplayName())) {
            textField.addVerifyListener(integerListener);
        } else if (dataType.equals(DataType.Float.getDisplayName())) {
            textField.addVerifyListener(floatListener);
        } else if (dataType.equals(DataType.FileReference.getDisplayName())
            || dataType.equals(DataType.DirectoryReference.getDisplayName())) {
            if (!selectAtStartCheckbox.getSelection()) {
                setFileOrDirectorySelected(true);
            }
        } else if (dataType.equals(DataType.Boolean.getDisplayName())) {
            if (combo == null) {
                createBooleanCombo();
            }
            setTextFieldOrComboVisible(false);
            if (!selectAtStartCheckbox.getSelection()) {
                textField.setText(combo.getText());
            }
            combo.setEnabled(!selectAtStartCheckbox.getSelection());
        }
    }

    private void setTextFieldOrComboVisible(boolean textFieldVisible) {
        textField.setVisible(textFieldVisible);
        if (combo != null) {
            combo.setVisible(!textFieldVisible);
        }
    }

    private void createValueArea() {
        label = new Label(confContainer, SWT.NONE);
        label.setText(Messages.value);

        textField = new Text(confContainer, SWT.BORDER);
        textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        textField.setVisible(false);
    }

    private void createFileSelectionArea() {
        Composite fileSelectionComposite = new Composite(confContainer, SWT.NONE);
        fileSelectionComposite.setLayout(new GridLayout(2, true));
        GridData g = new GridData(GridData.FILL, GridData.FILL, true, true);
        g.horizontalSpan = 2;
        fileSelectionComposite.setLayoutData(g);

        Composite atStartComposite = new Composite(fileSelectionComposite, SWT.NONE);
        atStartComposite.setLayout(new GridLayout(2, false));
        g = new GridData();
        g.horizontalSpan = 2;
        g.grabExcessHorizontalSpace = true;
        atStartComposite.setLayoutData(g);
        selectAtStartCheckbox = new Button(atStartComposite, SWT.CHECK);
        selectAtStartCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                setSelectedAtStart();
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {}
        });
        atStartLabel = new Label(atStartComposite, SWT.NONE);
        atStartLabel.setText(Messages.chooseAtWorkflowStart);
        atStartLabel.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent arg0) {
                selectAtStartCheckbox.setSelection(!selectAtStartCheckbox.getSelection());
                setSelectedAtStart();
                validateInput();
            }

            @Override
            public void mouseDown(MouseEvent arg0) {}

            @Override
            public void mouseDoubleClick(MouseEvent arg0) {}
        });
        selectFromProjectButton = new Button(fileSelectionComposite, SWT.PUSH);
        selectFromProjectButton.setText(Messages.selectFromProject);
        g = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        selectFromProjectButton.setLayoutData(g);
        selectFromProjectButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                
                IResource resource;
                if (comboDataType.getText().equals(DataType.DirectoryReference.getDisplayName())) {
                    resource = PropertyTabGuiHelper.selectDirectoryFromActiveProject(confContainer.getShell(),
                        Messages.selectDirectory, Messages.selectDirectoryFromProject);
                } else {
                    resource = PropertyTabGuiHelper.selectFileFromActiveProject(confContainer.getShell(),
                        Messages.selectFile, Messages.selectFileFromProject);
                }
                if (resource != null) {
                    textField.setText(resource.getFullPath().makeRelative().toPortableString());
                    metadataValues.put(InputProviderComponentConstants.META_FILESOURCETYPE,
                        FileSourceType.fromProject.name());                    
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {}
        });
        selectFromFileSystemButton = new Button(fileSelectionComposite, SWT.PUSH);
        selectFromFileSystemButton.setText(Messages.selectFromFileSystem);
        selectFromFileSystemButton.setLayoutData(g);
        selectFromFileSystemButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                String path;
                if (comboDataType.getText().equals(DataType.DirectoryReference.getDisplayName())) {
                    DirectoryDialog dialog = new DirectoryDialog(confContainer.getShell());
                    dialog.setText(Messages.selectDirectory);
                    dialog.setMessage(Messages.selectDirectoryFromFileSystem);
                    path = dialog.open();
                } else {
                    path = PropertyTabGuiHelper.selectFileFromFileSystem(confContainer.getShell(), null,
                        Messages.selectFile);
                }
                if (path != null) {
                    textField.setText(path);
                    metadataValues.put(InputProviderComponentConstants.META_FILESOURCETYPE, FileSourceType.fromFileSystem.name());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {}
        });
        setFileOrDirectorySelected(false);
    }
    
    private void setSelectedAtStart() {
        boolean selectAtStart = selectAtStartCheckbox.getSelection();
        textField.setEnabled(!selectAtStart);
        if (selectAtStart) {
            textField.setText("-");            
        } else {
            textField.setText("");
        }
        String dataType = comboDataType.getText();
        if (dataType.equals(DataType.FileReference.toString())
            || dataType.equals(DataType.DirectoryReference.toString())) {
            selectFromProjectButton.setEnabled(!selectAtStart);
            selectFromFileSystemButton.setEnabled(!selectAtStart);
        }
        if (dataType.equals(DataType.Boolean.toString())) {
            combo.setEnabled(!selectAtStart);
            if (!selectAtStart) {
                textField.setText(combo.getText());
            }
        }
        if (selectAtStart) {
            metadataValues.put(InputProviderComponentConstants.META_FILESOURCETYPE, FileSourceType.atWorkflowStart.name());            
        } else {
            metadataValues.remove(InputProviderComponentConstants.META_FILESOURCETYPE);            
        }
    }

    private void setDataTypeSelected(boolean selected) {
        selectAtStartCheckbox.setEnabled(selected);
        selectFromProjectButton.setEnabled(false);
        selectFromFileSystemButton.setEnabled(false);
        atStartLabel.setEnabled(selected);
    }

    private void setFileOrDirectorySelected(boolean selected) {
        selectAtStartCheckbox.setEnabled(selected);
        selectFromProjectButton.setEnabled(selected);
        selectFromFileSystemButton.setEnabled(selected);
        atStartLabel.setEnabled(selected);
    }

    private void createBooleanCombo() {
        combo = new Combo(confContainer, SWT.READ_ONLY);
        combo.add(Boolean.TRUE.toString().toLowerCase());
        combo.add(Boolean.FALSE.toString().toLowerCase());
        combo.setText(Boolean.TRUE.toString().toLowerCase());
        combo.setBounds(textField.getBounds());
        combo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                textField.setText(combo.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        combo.setVisible(false);
    }

    private void createListeners() {
        floatListener = new NumericalTextConstraintListener(textField, WidgetGroupFactory.ONLY_FLOAT);
        integerListener = new NumericalTextConstraintListener(textField, WidgetGroupFactory.ONLY_INTEGER);
    }

    private void removeListeners() {
        textField.removeVerifyListener(integerListener);
        textField.removeVerifyListener(floatListener);
    }

    @Override
    protected boolean validateMetaDataInputs() {
        boolean isValid = true;
        for (Widget widget : widgetToKeyMap.keySet()) {
            String key = "";
            if (metaData.getMetaDataKeys().contains(widgetToKeyMap.get(widget))) {
                key = widgetToKeyMap.get(widget);
            }
            String dataType = metaData.getDataType(key);
            String validation = metaData.getValidation(key);
            if (!dataType.equals(EndpointMetaDataConstants.TYPE_BOOL)
                && (metaData.getPossibleValues(key) == null || metaData.getPossibleValues(key).contains("*"))) {
                if (((Text) widget).getText().equals("") && (validation != null
                    && (validation.contains(ComponentConstants.INPUT_USAGE_TYPE_REQUIRED))) 
                    && !(comboDataType.getText().equalsIgnoreCase(DataType.ShortText.getDisplayName()))) {
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
        }
        if (!isValid && (comboDataType.getText().equalsIgnoreCase(DataType.FileReference.getDisplayName())
            || comboDataType.getText().equalsIgnoreCase(DataType.DirectoryReference.getDisplayName()))
            && selectAtStartCheckbox.getSelection()) {
            isValid = true;
        }
        return isValid;
    }
}
