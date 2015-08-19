/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.gui.utils.common.configuration.VariableNameVerifyListener;

/**
 * A dialog for editing a single endpoint configuration.
 * 
 * @author Sascha Zur
 */
public class WizardPropertyEditDialog extends Dialog {

    private Text keyText;

    private Text displayNameText;

    private Map<String, String> config;

    private String title;

    private Text defaultValueText;

    private List<String> allPropertyNames;

    private String oldPropertyName;

    private List<String> allPropertyDisplayNames;

    private Object oldPropertyDisplayName;

    public WizardPropertyEditDialog(Shell parentShell, String title, Map<String, String> config, List<String> allPropertyNames,
        List<String> allPropertyDisplayNames) {
        super(parentShell);
        this.config = config;
        oldPropertyName = config.get(ToolIntegrationConstants.KEY_PROPERTY_KEY);
        oldPropertyDisplayName = config.get(ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME);
        this.title = title;
        this.allPropertyNames = allPropertyNames;
        this.allPropertyDisplayNames = allPropertyDisplayNames;

    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, true));
        GridData g = new GridData(GridData.FILL_BOTH);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g);
        createPropertySettings(container);
        updateInitValues();
        return container;
    }

    private void updateInitValues() {
        if (config.get(PropertyConfigurationPage.KEY_PROPERTY_KEY) != null) {
            keyText.setText(config.get(PropertyConfigurationPage.KEY_PROPERTY_KEY));
        }
        if (config.get(PropertyConfigurationPage.KEY_PROPERTY_DISPLAY_NAME) != null) {
            displayNameText.setText(config.get(PropertyConfigurationPage.KEY_PROPERTY_DISPLAY_NAME));
        }
        if (config.get(PropertyConfigurationPage.KEY_PROPERTY_DEFAULT_VALUE) != null) {
            defaultValueText.setText(config.get(PropertyConfigurationPage.KEY_PROPERTY_DEFAULT_VALUE));
        }
    }

    protected void createPropertySettings(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, true));

        Composite propertyContainer = new Composite(container, SWT.None);
        propertyContainer.setLayout(new GridLayout(2, true));

        Label keyLabel = new Label(propertyContainer, SWT.NONE);
        keyLabel.setText(Messages.keyColon);
        keyText = new Text(propertyContainer, SWT.BORDER);
        GridData textGridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        keyText.setLayoutData(textGridData);
        keyText.addListener(SWT.Verify, new VariableNameVerifyListener(false));

        Label displayName = new Label(propertyContainer, SWT.NONE);
        displayName.setText(Messages.displayNameColon);
        displayNameText = new Text(propertyContainer, SWT.BORDER);
        GridData displayGridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        displayNameText.setLayoutData(displayGridData);

        Label defaultValueLabel = new Label(propertyContainer, SWT.NONE);
        defaultValueLabel.setText(Messages.defaultValueColon);
        defaultValueText = new Text(propertyContainer, SWT.BORDER);
        GridData defaultValueData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        defaultValueText.setLayoutData(defaultValueData);
    }

    private void saveAllConfig() {
        config.put(PropertyConfigurationPage.KEY_PROPERTY_KEY, keyText.getText());
        config.put(PropertyConfigurationPage.KEY_PROPERTY_DISPLAY_NAME, displayNameText.getText());
        config.put(PropertyConfigurationPage.KEY_PROPERTY_DEFAULT_VALUE, defaultValueText.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);
        validateInput();
        installListeners();
    }

    private void installListeners() {
        ModifyListener ml = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                saveAllConfig();
                validateInput();
            }
        };

        keyText.addModifyListener(ml);
        keyText.addListener(SWT.Verify, new VariableNameVerifyListener(true));
        displayNameText.addModifyListener(ml);
        defaultValueText.addModifyListener(ml);

    }

    protected void validateInput() {

        boolean isValid = true;
        if (keyText.getText() == null || keyText.getText().isEmpty()) {
            isValid = false;
        }
        // filter if name is already in use (but remind that on edit it can have the same name)
        if (allPropertyNames != null && allPropertyNames.contains(keyText.getText())) {
            if (oldPropertyName == null || !oldPropertyName.equals(keyText.getText())) {
                isValid = false;
            }
        }
        if (allPropertyDisplayNames != null && allPropertyDisplayNames.contains(displayNameText.getText())) {
            if (oldPropertyDisplayName == null || !oldPropertyDisplayName.equals(displayNameText.getText())) {
                isValid = false;
            }
        }
        if (displayNameText.getText() == null || displayNameText.getText().isEmpty()) {
            isValid = false;
        }
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
