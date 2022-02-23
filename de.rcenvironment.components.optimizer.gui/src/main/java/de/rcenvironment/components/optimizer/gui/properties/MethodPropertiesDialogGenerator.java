/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;

/**
 * This class is for generating a properties {@link Dialog} for a given optimization method based on a json file.
 * 
 * @author Sascha Zur
 */
public class MethodPropertiesDialogGenerator extends Dialog {
    
    private static final  String TRUE = "true";
    
    private final MethodDescription methodDescription;
    
    
    private Map<Widget, String> widgetToKeyMap;

    protected MethodPropertiesDialogGenerator(Shell parentShell, MethodDescription methodDescription) {
        super(parentShell);
        this.methodDescription = methodDescription;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.algorithmProperties + " - " + methodDescription.getMethodName());
        InputStream path = getClass().getResourceAsStream("/resources/optimizer16.png");
        Image icon = new Image(null, new ImageData(path));
        shell.setImage(icon);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        widgetToKeyMap = new HashMap<Widget, String>();

        Composite dialogContainer = (Composite) super.createDialogArea(parent);
        CTabFolder settingsTabFolder = new CTabFolder(dialogContainer, SWT.BORDER);
        if (methodDescription != null) {
            if (methodDescription.getCommonSettings() != null
                && checkIfSettingsAreGUIRelevant(methodDescription.getCommonSettings())) {
                CTabItem commonSettingsTab = new CTabItem(settingsTabFolder, SWT.NONE);
                commonSettingsTab.setText("Common Settings");
                Composite commonSettingsContainer = new Composite(settingsTabFolder, SWT.NONE);
                commonSettingsContainer.setLayout(new GridLayout(2, true));
                createSettings(methodDescription.getCommonSettings(), commonSettingsContainer);
                commonSettingsTab.setControl(commonSettingsContainer);
            }

            if (methodDescription.getSpecificSettings() != null
                && checkIfSettingsAreGUIRelevant(methodDescription.getSpecificSettings())) {
                CTabItem specificSettingsTab = new CTabItem(settingsTabFolder, SWT.NONE);
                specificSettingsTab.setText("Algorithm Specific Settings");
                Composite specificSettingsContainer = new Composite(settingsTabFolder, SWT.NONE);
                specificSettingsContainer.setLayout(new GridLayout(2, true));
                createSettings(methodDescription.getSpecificSettings(), specificSettingsContainer);
                specificSettingsTab.setControl(specificSettingsContainer);
            }
            if (methodDescription.getResponsesSettings() != null
                && checkIfSettingsAreGUIRelevant(methodDescription.getResponsesSettings())) {
                
                CTabItem responsesSettingsTab = new CTabItem(settingsTabFolder, SWT.NONE);
                responsesSettingsTab.setText("Responses Settings");
                Composite responsesSettingsContainer = new Composite(settingsTabFolder, SWT.NONE);
                responsesSettingsContainer.setLayout(new GridLayout(2, true));
                createSettings(methodDescription.getResponsesSettings(), responsesSettingsContainer);
                responsesSettingsTab.setControl(responsesSettingsContainer);
            }
        }
        return dialogContainer;
    }
    
    private boolean checkIfSettingsAreGUIRelevant(Map<String, Map<String, String> > settings) {
        boolean returnValue = true;
        if (settings.isEmpty()) {
            returnValue = false;
        }
        
        if (settings != null) {
            for (String key : settings.keySet()) {
                if (settings.get(key).get(OptimizerComponentConstants.DONT_SHOW_KEY) != null) {
                    returnValue = false;
                }
            }
        }
        
        return returnValue;
    }
    

    private void createSettings(Map<String, Map<String, String>> settings, Composite container) {
        if (settings != null) {
            String[] sortedSettings = new String[settings.keySet().size()];
            int position = 0 - 1;
            for (String key : settings.keySet()) {
                String orderNumber = settings.get(key).get(OptimizerComponentConstants.GUI_ORDER_KEY);
                if (orderNumber != null) {
                    position = Integer.parseInt(orderNumber) - 1;
                    if (position >= sortedSettings.length) {
                        while (position >= sortedSettings.length || sortedSettings[position] != null) {
                            position--;
                        }
                    } else {
                        while (sortedSettings[position] != null) {
                            position++;
                        }
                    }
                } else {
                    position = sortedSettings.length - 1;
                    while (sortedSettings[position] != null) {
                        position--;
                    }
                }
                sortedSettings[position] = key;
            }
            for (String key : sortedSettings) {
                Map<String, String> currentSetting = settings.get(key);
                if (settings.get(key).get(OptimizerComponentConstants.DONT_SHOW_KEY) == null
                    || !settings.get(key).get(OptimizerComponentConstants.DONT_SHOW_KEY).equalsIgnoreCase(TRUE)) {
                    String value = currentSetting.get(OptimizerComponentConstants.VALUE_KEY);
                    if (value == null || value.equals("")) {
                        value = currentSetting.get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
                    }
                    if (settings.get(key).get(OptimizerComponentConstants.SWTWIDGET_KEY).equals(OptimizerComponentConstants.WIDGET_TEXT)) {
                        Text newTextfield =
                            createLabelAndTextfield(container,
                                currentSetting.get(OptimizerComponentConstants.GUINAME_KEY),
                                currentSetting.get(OptimizerComponentConstants.DATA_TYPE_KEY),
                                value);
                        newTextfield.setData(key);
                        widgetToKeyMap.put(newTextfield, key);
                        newTextfield.addModifyListener(new MethodPropertiesModifyListener());
                    } else if (settings.get(key).get(
                        OptimizerComponentConstants.SWTWIDGET_KEY).equals(OptimizerComponentConstants.WIDGET_COMBO)) {
                        Combo newCombo = createLabelAndCombo(
                            container, currentSetting.get(OptimizerComponentConstants.GUINAME_KEY),
                            currentSetting.get(OptimizerComponentConstants.CHOICES_KEY),
                            value);
                        widgetToKeyMap.put(newCombo, key);
                        newCombo.setData(key);
                        newCombo.addModifyListener(new MethodPropertiesModifyListener());
                    } else if (settings.get(key).get(OptimizerComponentConstants.SWTWIDGET_KEY)
                        .equals(OptimizerComponentConstants.WIDGET_CHECK)) {
                        Button newCheckbox = createLabelAndCheckbox(container,
                            currentSetting.get(OptimizerComponentConstants.GUINAME_KEY),
                            value);
                        widgetToKeyMap.put(newCheckbox, key);
                        newCheckbox.setData(key);
                        newCheckbox.addSelectionListener(new SelectionChangedListener());
                    }
                }
            }

            new Label(container, SWT.NONE).setText("");
            Label horizontalLine = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData lineGridData = new GridData(GridData.FILL_HORIZONTAL | SWT.END);
            horizontalLine.setLayoutData(lineGridData);
            new Label(container, SWT.NONE).setText("");
            Button loadDefaults = new Button(container, SWT.PUSH);
            loadDefaults.setImage(ImageManager.getInstance().getImageDescriptor(StandardImages.RESTORE_DEFAULT).createImage());
            GridData gridData = new GridData();
            gridData.horizontalAlignment = SWT.RIGHT;
            loadDefaults.setLayoutData(gridData);
            loadDefaults.setText(Messages.restoreDefaultAlgorithmProperties);
            loadDefaults.addSelectionListener(new DefaultSelectionListener(container, settings));
        }
    }
    
    /**
     * 
     * Implements the selection listener for the "load default" values button.
     *
     * @author Jascha Riedel
     */
    private class DefaultSelectionListener implements SelectionListener {

        private Composite container;
        
        private Map<String, Map<String, String> > settings;
        
        DefaultSelectionListener(Composite container, Map<String, Map<String, String> > settings) {
            this.container = container;
            this.settings = settings;
        }
        
        
        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            for (Object field : container.getChildren()) {
                if (field instanceof Text) {
                    String key = (String) ((Text) field).getData();
                    if (key != null) {
                        String value = settings.get(key).get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
                        if (value != null) {
                            ((Text) field).setText(value);
                        }
                    }
                }
                if (field instanceof Combo) {
                    String key = (String) ((Combo) field).getData();
                    if (key != null) {
                        String value = settings.get(key).get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
                        if (value != null) {
                            ((Combo) field).setText(value);
                        }
                    }
                    
                }
                if (field instanceof Button) {
                    String key = (String) ((Button) field).getData();
                    
                    if (key != null) {
                        String value = settings.get(key).get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
                        if (value != null) {
                            if (value.equals(TRUE) || value.equals("false")) {
                                ((Button) field).setSelection(Boolean.parseBoolean(value));
                            } else {
                                ((Button) field).setText(value);
                            }
                        }
                    }
                }
            }
        }
        
    }


    private Button createLabelAndCheckbox(Composite container, String text, String value) {
        new Label(container, SWT.NONE).setText(text);
        Button result = new Button(container, SWT.CHECK);
        if (value.equals(TRUE)) {
            result.setSelection(true);
        } else {
            result.setSelection(false);
        }
        return result;
    }

    private Combo createLabelAndCombo(Composite container, String text, String entries, String value) {
        new Label(container, SWT.NONE).setText(text);
        Combo result = new Combo(container, SWT.READ_ONLY);
        String[] entryData = entries.split(OptimizerComponentConstants.SEPARATOR);
        for (String entry : entryData) {
            result.add(entry);
        }
        result.select(result.indexOf(value));
        return result;
    }

    private Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        new Label(container, SWT.NONE).setText(text);
        Text result = new Text(container, SWT.SINGLE | SWT.BORDER);
        result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        result.setText(value);
        if (dataType.equals("Int")) {
            result.addVerifyListener(new NumericalTextConstraintListener(result, WidgetGroupFactory.ONLY_INTEGER));
        } else if (dataType.equals("Real")) {
            result.addVerifyListener(new NumericalTextConstraintListener(result, WidgetGroupFactory.ONLY_FLOAT));
        }
        return result;
    }

    /**
     * Validated all current inputs in the dialog.
     */
    public void validateInputs() {
        boolean isValid = true;
        for (Widget widget : widgetToKeyMap.keySet()) {
            Map<String, String> settings = null;
            if (methodDescription.getCommonSettings().containsKey(widgetToKeyMap.get(widget))) {
                settings = methodDescription.getCommonSettings().get(widgetToKeyMap.get(widget));
            } else if (methodDescription.getSpecificSettings().containsKey(widgetToKeyMap.get(widget))) {
                settings = methodDescription.getSpecificSettings().get(widgetToKeyMap.get(widget));
            } else if (methodDescription.getResponsesSettings().containsKey(widgetToKeyMap.get(widget))) {
                settings = methodDescription.getResponsesSettings().get(widgetToKeyMap.get(widget));
            }
            if (settings != null) {
                String dataType = settings.get(OptimizerComponentConstants.DATA_TYPE_KEY);
                String swtWidget = settings.get(OptimizerComponentConstants.SWTWIDGET_KEY);
                String validation = settings.get(OptimizerComponentConstants.VALIDATION_KEY);
                if (swtWidget.equals(OptimizerComponentConstants.WIDGET_TEXT)) {
                    if (((Text) widget).getText().equals("") && (validation.contains("required"))) {
                        isValid = false;
                    } else if (!((Text) widget).getText().equals("")) {
                        if (dataType.equalsIgnoreCase(OptimizerComponentConstants.TYPE_INT)) {
                            int value = Integer.MAX_VALUE;
                            try {
                                value = Integer.parseInt(((Text) widget).getText());
                                isValid &= checkValidation(value, validation);
                            } catch (NumberFormatException e) {
                                value = Integer.MAX_VALUE;
                                isValid &= false;
                            }
                        }
                        if (dataType.equalsIgnoreCase(OptimizerComponentConstants.TYPE_REAL)) {
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
        }
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    private boolean checkValidation(double value, String validation) {
        boolean result = true;
        if (validation != null && !validation.equals("")) {
            String[] splitValidations = validation.split(OptimizerComponentConstants.SEPARATOR);
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
     * ModifyListener for changing the new values in the given MethodDescription.
     * 
     * @author Sascha Zur
     */
    private class MethodPropertiesModifyListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent arg0) {
            Widget source = (Widget) arg0.getSource();
            if (source instanceof Text) {
                if (methodDescription.getCommonSettings().containsKey(widgetToKeyMap.get(source))) {
                    methodDescription.getCommonSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                        ((Text) source).getText());
                } else if (methodDescription.getSpecificSettings().containsKey(widgetToKeyMap.get(source))) {
                    methodDescription.getSpecificSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                        ((Text) source).getText());
                } else if (methodDescription.getResponsesSettings().containsKey(widgetToKeyMap.get(source))) {
                    methodDescription.getResponsesSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                        ((Text) source).getText());
                }
            } else if (source instanceof Combo) {
                if (methodDescription.getCommonSettings().containsKey(widgetToKeyMap.get(source))) {
                    methodDescription.getCommonSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                        ((Combo) source).getText());
                } else if (methodDescription.getSpecificSettings().containsKey(widgetToKeyMap.get(source))) {
                    methodDescription.getSpecificSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                        ((Combo) source).getText());
                } else if (methodDescription.getResponsesSettings() != null
                    && (methodDescription.getResponsesSettings().containsKey(widgetToKeyMap.get(source)))) {
                    methodDescription.getResponsesSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                        ((Combo) source).getText());
                }
            }
            validateInputs();
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
            if (methodDescription.getCommonSettings().containsKey(widgetToKeyMap.get(source))) {
                methodDescription.getCommonSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                    "" + source.getSelection());
            } else if (methodDescription.getSpecificSettings().containsKey(widgetToKeyMap.get(source))) {
                methodDescription.getSpecificSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                    "" + source.getSelection());
            } else if (methodDescription.getResponsesSettings().containsKey(widgetToKeyMap.get(source))) {
                methodDescription.getResponsesSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                    "" + source.getSelection());
            }
            validateInputs();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            widgetDefaultSelected(e);
        }
    }
}
