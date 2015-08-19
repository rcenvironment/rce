/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;

/**
 * A dialog for editing a single endpoint configuration.
 * 
 * @author Sascha Zur
 */
// TODO Out commented code is for SSH execution
public class WizardToolConfigurationDialog extends Dialog {

    // private Text hostText;

    private Text toolDirectoryText;

    private final Map<String, String> config;

    private final String title;

    /*
     * private Text portText;
     * 
     * private Button hostButton;
     */

    // private Button localhostButton;

    private Text versionText;

    private Text rootWorkingDirText;

    private Button chooseToolDirPathButton;

    private Button chooseRootDirPathButton;

    private Button defaultTempDirButton;

    private Button customTempDirButton;

    private final List<Map<String, String>> allConfigs;

    private Label tempLabel;

    private Map<String, String> oldConfig;

    private boolean isEdit;

    private final ToolIntegrationContext context;

    private Button limitInstancesButton;

    private Text limitInstancesText;

    /**
     * Dialog for creating or editing an endpoint.
     * 
     * @param parentShell parent Shell
     * @param title
     * @param configs
     * @param configuration the containing endpoint manager
     * @param context current {@link ToolIntegrationContext}
     */
    public WizardToolConfigurationDialog(Shell parentShell, String title, List<Map<String, String>> configs,
        ToolIntegrationContext context) {
        super(parentShell);
        config = new HashMap<String, String>();
        config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, "true");
        config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, "10");
        this.title = title;
        allConfigs = configs;
        this.context = context;

        setShellStyle(SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
    }

    public WizardToolConfigurationDialog(Shell parentShell, String title, Map<String, String> config, List<Map<String, String>> configs,
        ToolIntegrationContext context, boolean isEdit) {
        super(parentShell);
        this.config = config;
        oldConfig = new HashMap<String, String>();
        oldConfig.putAll(config);
        this.title = title;
        allConfigs = configs;
        this.isEdit = isEdit;
        this.context = context;
        if (config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) == null) {
            config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, "false");
            config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, "10");
        }
        setShellStyle(SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
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
        GridData g = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        container.setLayoutData(g);
        createPropertySettings(container);
        updateInitValues();
        return container;
    }

    private void updateInitValues() {
        if (config.get(ToolIntegrationConstants.KEY_HOST) != null) {
            config.get(ToolIntegrationConstants.KEY_HOST);
        }
        if (config.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY) != null) {
            toolDirectoryText.setText(config.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY));
        }
        if (config.get(ToolIntegrationConstants.KEY_VERSION) != null) {
            versionText.setText(config.get(ToolIntegrationConstants.KEY_VERSION));
        }
        if (config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) != null) {
            if (Boolean.parseBoolean(config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES))) {
                limitInstancesButton.setSelection(true);
                limitInstancesText.setEnabled(true);
                limitInstancesText.setText(config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT));
            } else {
                limitInstancesButton.setSelection(false);
                limitInstancesText.setEnabled(false);
                limitInstancesText.setText("");
            }
        } else {
            limitInstancesButton.setSelection(false);
            limitInstancesText.setText("");
        }
        String rwd = config.get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY);

        if (rwd != null) {
            if (rwd.isEmpty()) {
                customTempDirButton.setSelection(false);
                defaultTempDirButton.setSelection(true);
                rootWorkingDirText.setEnabled(false);
            } else {
                customTempDirButton.setSelection(true);
                defaultTempDirButton.setSelection(false);
                rootWorkingDirText.setEnabled(true);
                rootWorkingDirText.setText(config.get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY));
            }
        } else {
            customTempDirButton.setSelection(false);
            defaultTempDirButton.setSelection(true);
            rootWorkingDirText.setEnabled(false);
        }
        defaultTempDirButton.setEnabled(true);
        customTempDirButton.setEnabled(true);
        for (String key : context.getDisabledIntegrationKeys()) {
            if (ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY.equals(key)) {
                rootWorkingDirText.setEnabled(false);
                defaultTempDirButton.setEnabled(false);
                customTempDirButton.setEnabled(false);
            }
        }
    }

    protected void createPropertySettings(Composite parent) {
        Composite container2 = new Composite(parent, SWT.NONE);
        container2.setLayout(new GridLayout(1, false));
        GridData g2 = new GridData(GridData.FILL, GridData.FILL, true, true);
        container2.setLayoutData(g2);
        Composite propertyContainer = new Composite(container2, SWT.None);
        propertyContainer.setLayout(new GridLayout(3, false));
        GridData g3 = new GridData(GridData.FILL, GridData.FILL,
            true, true);
        propertyContainer.setLayoutData(g3);
        // localhostButton = new Button(propertyContainer, SWT.RADIO);
        // localhostButton.setText(Messages.localHostButtonText);

        // localhostButton.setLayoutData(localhostData);
        // localhostButton.setVisible(false);
        tempLabel = new Label(propertyContainer, SWT.NONE);
        tempLabel.setText(Messages.localHostButtonText);
        GridData localhostData = new GridData();
        localhostData.horizontalSpan = 3;
        tempLabel.setLayoutData(localhostData);

        /*
         * hostButton = new Button(propertyContainer, SWT.RADIO);
         * hostButton.setText(Messages.hostButtonText); GridData hostData = new
         * GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL); hostData.horizontalSpan =
         * 3; hostButton.setLayoutData(hostData); hostButton.setEnabled(false); // disabled for now
         * 
         * Label hostLabel = new Label(propertyContainer, SWT.NONE); hostLabel.setText("\t" +
         * Messages.host); hostLabel.setEnabled(false); // disabled for now hostText = new
         * Text(propertyContainer, SWT.BORDER); GridData textGridData = new
         * GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
         * textGridData.horizontalSpan = 2; hostText.setLayoutData(textGridData);
         * hostText.setEnabled(false); // disabled for now
         * 
         * Label portLabel = new Label(propertyContainer, SWT.NONE); portLabel.setText("\t" +
         * Messages.port); portLabel.setEnabled(false); // disabled for now portText = new
         * Text(propertyContainer, SWT.BORDER); GridData portGridData = new
         * GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
         * portGridData.horizontalSpan = 2; portText.setLayoutData(portGridData);
         * portText.setText(DEFAULT_PORT_TEXT); portText.setEnabled(false); // disabled for now
         */
        Label toolDirLabel = new Label(propertyContainer, SWT.NONE);
        toolDirLabel.setText(Messages.toolDirectoryRequired);
        toolDirectoryText = new Text(propertyContainer, SWT.BORDER);
        GridData toolDirGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        toolDirectoryText.setLayoutData(toolDirGridData);

        chooseToolDirPathButton = new Button(propertyContainer, SWT.PUSH);
        chooseToolDirPathButton.setText("  ...  ");
        chooseToolDirPathButton.addSelectionListener(new PathChooserButtonListener(toolDirectoryText, true, getShell()));

        Label versionLabel = new Label(propertyContainer, SWT.NONE);
        versionLabel.setText(Messages.versionRequired);
        versionText = new Text(propertyContainer, SWT.BORDER);
        GridData versionGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        versionGridData.horizontalSpan = 2;
        versionText.setLayoutData(versionGridData);

        limitInstancesButton = new Button(propertyContainer, SWT.CHECK);
        limitInstancesButton.setText(Messages.limitExecutionInstances);
        limitInstancesText = new Text(propertyContainer, SWT.BORDER);
        GridData limitInstancesGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        limitInstancesGridData.horizontalSpan = 2;
        limitInstancesText.setLayoutData(limitInstancesGridData);
        limitInstancesText.addVerifyListener(new NumericalTextConstraintListener(limitInstancesText,
            NumericalTextConstraintListener.ONLY_INTEGER));

        limitInstancesButton.setSelection(true);
        limitInstancesButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                limitInstancesText.setEnabled(limitInstancesButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);

            }
        });
        Label workingDirLabel = new Label(propertyContainer, SWT.NONE);
        workingDirLabel.setText("Working directory: ");
        GridData workingDirLabelData = new GridData();
        workingDirLabelData.horizontalSpan = 3;
        workingDirLabel.setLayoutData(workingDirLabelData);

        defaultTempDirButton = new Button(propertyContainer, SWT.RADIO);
        GridData defaultTempDirData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        defaultTempDirData.horizontalSpan = 3;
        defaultTempDirButton.setText(Messages.rceTempUsed);
        defaultTempDirButton.setLayoutData(defaultTempDirData);
        defaultTempDirButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                saveAllConfig();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        customTempDirButton = new Button(propertyContainer, SWT.RADIO);
        customTempDirButton.setText(Messages.useCustomTempDir);
        GridData customTempDirData = new GridData();
        customTempDirButton.setLayoutData(customTempDirData);

        rootWorkingDirText = new Text(propertyContainer, SWT.BORDER);
        GridData rootWorkingDirGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);

        rootWorkingDirText.setLayoutData(rootWorkingDirGridData);

        chooseRootDirPathButton = new Button(propertyContainer, SWT.PUSH);
        chooseRootDirPathButton.setText("  ...  ");
        chooseRootDirPathButton.setEnabled(false);
        chooseRootDirPathButton.addSelectionListener(new PathChooserButtonListener(rootWorkingDirText, true, getShell()));

    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
    }

    private void saveAllConfig() {
        // if (localhostButton.getSelection()) {
        config.put(ToolIntegrationConstants.KEY_HOST, ToolIntegrationConstants.VALUE_LOCALHOST);
        /*
         * } else { // config.put(ToolIntegrationConstants.KEY_HOST, hostText.getText() +
         * HOST_SEPARATOR + // portText.getText()); }
         */
        config.put(ToolIntegrationConstants.KEY_TOOL_DIRECTORY, toolDirectoryText.getText());

        config.put(ToolIntegrationConstants.KEY_VERSION, versionText.getText());

        config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, "" + limitInstancesButton.getSelection());
        if (limitInstancesButton.getSelection()) {
            config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, limitInstancesText.getText());
        }

        if (defaultTempDirButton.getSelection()) {
            config.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY, "");
        } else {
            config.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY, rootWorkingDirText.getText());
        }

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

        /* hostText.addModifyListener(ml); */
        rootWorkingDirText.addModifyListener(ml);
        versionText.addModifyListener(ml);
        toolDirectoryText.addModifyListener(ml);
        limitInstancesText.addModifyListener(ml);
        limitInstancesButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                saveAllConfig();
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                /*
                 * hostText.setEnabled(hostButton.getSelection());
                 * portText.setEnabled(hostButton.getSelection());
                 * chooseRootDirPathButton.setEnabled(!hostButton.getSelection() &&
                 * customTempDirButton.getSelection());
                 * chooseToolDirPathButton.setEnabled(!hostButton.getSelection());
                 */
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        };

        SelectionListener sl2 = new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                rootWorkingDirText.setEnabled(customTempDirButton.getSelection());
                chooseRootDirPathButton.setEnabled(/* !hostButton.getSelection() && */customTempDirButton.getSelection());
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        };
        defaultTempDirButton.addSelectionListener(sl2);
        customTempDirButton.addSelectionListener(sl2);
    }

    protected void validateInput() {

        boolean isValid = true;
        /*
         * if (hostButton.getSelection()) { if (hostText.getText() == null ||
         * hostText.getText().isEmpty()) { isValid = false; } if (portText.getText() == null ||
         * portText.getText().isEmpty()) { isValid = false; } else { try {
         * Integer.parseInt(portText.getText()); } catch (NumberFormatException e) { isValid =
         * false; } } }
         */
        if (toolDirectoryText.getText() == null || toolDirectoryText.getText().isEmpty()) {
            isValid = false;
        }
        if (limitInstancesButton.getSelection() && (limitInstancesText.getText().equals("")
            || !limitInstancesText.getText().matches("\\d+"))) {
            isValid = false;
        }
        if (versionText.getText() == null || versionText.getText().isEmpty()) {
            isValid = false;
        }
        if (isValid && !(isEdit && oldConfig.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY).equals(toolDirectoryText.getText()))) {
            for (Map<String, String> otherConfig : allConfigs) {

                if (otherConfig.get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY).equals(toolDirectoryText.getText())
                /*
                 * && ((localhostButton.getSelection() &&
                 * otherConfig.get(ToolIntegrationConstants.KEY_HOST).equals(
                 * ToolIntegrationConstants.VALUE_LOCALHOST))
                 */
                /*
                 * || (!localhostButton.getSelection() &&
                 * otherConfig.get(ToolIntegrationConstants.KEY_HOST).equals( hostText.getText() +
                 * HOST_SEPARATOR + portText.getText())) )
                 */) {
                    isValid = false;
                }
            }
        }
        if (!defaultTempDirButton.getSelection() && (rootWorkingDirText.getText() == null || rootWorkingDirText.getText().isEmpty())) {
            isValid = false;
        }
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
