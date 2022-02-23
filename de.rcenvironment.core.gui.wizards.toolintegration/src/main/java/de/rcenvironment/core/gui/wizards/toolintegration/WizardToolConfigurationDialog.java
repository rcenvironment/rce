/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A dialog for editing a single endpoint configuration.
 * 
 * @author Sascha Zur
 */
// TODO Out commented code is for SSH execution
public class WizardToolConfigurationDialog extends Dialog {

    private static final int VALIDATION_MESSAGE_HEIGHT_HINT = 55;

    private static final String STRING_INVALID_TOOL_DIRECTORY = "The chosen tool directory is not valid.";

    private static final String STRING_INVALID_WORKING_DIRECTORY = "Invalid path to working directory.";

    // private Text hostText;

    private static final String STRING_INVALID_VERSION = "The chosen version is not valid.\n%s.";

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

    // private Label tempLabel;

    private Map<String, String> oldConfig;

    private final ToolIntegrationContext context;

    private Button limitInstancesButton;

    private Text limitInstancesText;

    private Label message;

    private Composite messageContainer;

    /**
     * Dialog for creating or editing an endpoint.
     * 
     * @param parentShell parent Shell
     * @param title
     * @param context current {@link ToolIntegrationContext}
     * @param configuration the containing endpoint manager
     */
    public WizardToolConfigurationDialog(Shell parentShell, String title, ToolIntegrationContext context) {
        super(parentShell);
        config = new HashMap<>();
        config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, "true");
        config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, "10");
        this.title = title;
        this.context = context;

        setShellStyle(SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
    }

    public WizardToolConfigurationDialog(Shell parentShell, String title, Map<String, String> config, ToolIntegrationContext context) {
        super(parentShell);
        this.config = config;
        oldConfig = new HashMap<>();
        oldConfig.putAll(config);
        this.title = title;
        this.context = context;
        if (config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) == null
            && config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD) == null) {
            config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, "false");
            config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, "10");
        }
        setShellStyle(SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
    }

    @Override
    protected Point getInitialSize() {
        final int width = 600;
        return new Point(width, super.getInitialSize().y);
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
        GridData g =
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING);
        container.setLayoutData(g);
        createPropertySettings(container);
        createMessageComposite(container);
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
        String rwd = config.get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY);

        if (rwd == null) {
            defaultTempDirButton.setSelection(false);
            rootWorkingDirText.setEnabled(true);
            chooseRootDirPathButton.setEnabled(true);
        } else {
            if (rwd.isEmpty()) {
                defaultTempDirButton.setSelection(true);
                rootWorkingDirText.setEnabled(false);
                chooseRootDirPathButton.setEnabled(false);
            } else {
                defaultTempDirButton.setSelection(false);
                rootWorkingDirText.setEnabled(true);
                rootWorkingDirText.setText(config.get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY));
                chooseRootDirPathButton.setEnabled(true);
            }
        }
        for (String key : context.getDisabledIntegrationKeys()) {
            if (ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY.equals(key)) {
                rootWorkingDirText.setEnabled(false);
                defaultTempDirButton.setEnabled(false);
                chooseRootDirPathButton.setEnabled(false);
            }
        }
        if (config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES) != null) {
            setInstanceLimit(ToolIntegrationConstants.KEY_LIMIT_INSTANCES);
        } else if (config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD) != null) {
            setInstanceLimit(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        } else {
            limitInstancesButton.setSelection(false);
            limitInstancesText.setText("");
        }
    }

    private void setInstanceLimit(String key) {
        if (Boolean.parseBoolean(config.get(key))) {
            limitInstancesButton.setSelection(true);
            limitInstancesText.setEnabled(true);
            limitInstancesText.setText(config.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT));
        } else {
            limitInstancesButton.setSelection(false);
            limitInstancesText.setEnabled(false);
            limitInstancesText.setText("");
        }
    }

    private void createMessageComposite(Composite parent) {
        messageContainer = new Composite(parent, SWT.None);
        messageContainer.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(GridData.FILL, GridData.FILL,
            true, true);
        messageContainer.setLayoutData(gridData);
        Label icon = new Label(messageContainer, SWT.NONE);
        icon.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, true));
        icon.setImage(ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16));
        message = new Label(messageContainer, SWT.FILL | SWT.WRAP);
        GridData messageGridData = new GridData(GridData.FILL, GridData.FILL, true, true);
        messageGridData.heightHint = VALIDATION_MESSAGE_HEIGHT_HINT;
        message.setLayoutData(messageGridData);
        message.setText("Validation Message");

    }

    private void setMessage(String text) {
        message.setText(text);
        messageContainer.setVisible(!message.getText().isEmpty());
    }

    protected void createPropertySettings(Composite parent) {
        Composite propertyContainer = new Composite(parent, SWT.None);
        propertyContainer.setLayout(new GridLayout(3, false));
        GridData gridData = new GridData(GridData.FILL, GridData.FILL,
            true, true);
        propertyContainer.setLayoutData(gridData);
        // localhostButton = new Button(propertyContainer, SWT.RADIO);
        // localhostButton.setText(Messages.localHostButtonText);

        // localhostButton.setLayoutData(localhostData);
        // localhostButton.setVisible(false);
        // tempLabel = new Label(propertyContainer, SWT.NONE);
        // tempLabel.setText(Messages.localHostButtonText);
        // GridData localhostData = new GridData();
        // localhostData.horizontalSpan = 3;
        // tempLabel.setLayoutData(localhostData);

        /*
         * hostButton = new Button(propertyContainer, SWT.RADIO); hostButton.setText(Messages.hostButtonText); GridData hostData = new
         * GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL); hostData.horizontalSpan = 3; hostButton.setLayoutData(hostData);
         * hostButton.setEnabled(false); // disabled for now
         * 
         * Label hostLabel = new Label(propertyContainer, SWT.NONE); hostLabel.setText("\t" + Messages.host); hostLabel.setEnabled(false);
         * // disabled for now hostText = new Text(propertyContainer, SWT.BORDER); GridData textGridData = new
         * GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL); textGridData.horizontalSpan = 2;
         * hostText.setLayoutData(textGridData); hostText.setEnabled(false); // disabled for now
         * 
         * Label portLabel = new Label(propertyContainer, SWT.NONE); portLabel.setText("\t" + Messages.port); portLabel.setEnabled(false);
         * // disabled for now portText = new Text(propertyContainer, SWT.BORDER); GridData portGridData = new
         * GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL); portGridData.horizontalSpan = 2;
         * portText.setLayoutData(portGridData); portText.setText(DEFAULT_PORT_TEXT); portText.setEnabled(false); // disabled for now
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

        Label workingDirLabel = new Label(propertyContainer, SWT.NONE);
        workingDirLabel.setText("Working directory (absolute): ");

        rootWorkingDirText = new Text(propertyContainer, SWT.BORDER);
        GridData rootWorkingDirGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);

        rootWorkingDirText.setLayoutData(rootWorkingDirGridData);

        chooseRootDirPathButton = new Button(propertyContainer, SWT.PUSH);
        chooseRootDirPathButton.setText("  ...  ");
        chooseRootDirPathButton.addSelectionListener(new PathChooserButtonListener(rootWorkingDirText, true, getShell()));

        new Label(propertyContainer, SWT.NONE);

        defaultTempDirButton = new Button(propertyContainer, SWT.CHECK);
        GridData defaultTempDirData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        defaultTempDirData.horizontalSpan = 2;
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

        limitInstancesButton = new Button(propertyContainer, SWT.CHECK);
        limitInstancesButton.setText(Messages.limitExecutionInstances);
        limitInstancesText = new Text(propertyContainer, SWT.BORDER);
        GridData limitInstancesGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        limitInstancesGridData.horizontalSpan = 2;
        limitInstancesText.setLayoutData(limitInstancesGridData);
        limitInstancesText.addVerifyListener(new NumericalTextConstraintListener(limitInstancesText,
            NumericalTextConstraintListener.ONLY_INTEGER | NumericalTextConstraintListener.GREATER_ZERO));

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

    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
    }

    private void saveAllConfig() {
        // if (localhostButton.getSelection()) {
        config.put(ToolIntegrationConstants.KEY_HOST, ToolIntegrationConstants.VALUE_LOCALHOST);
        /*
         * } else { // config.put(ToolIntegrationConstants.KEY_HOST, hostText.getText() + HOST_SEPARATOR + // portText.getText()); }
         */
        config.put(ToolIntegrationConstants.KEY_TOOL_DIRECTORY, toolDirectoryText.getText());

        config.put(ToolIntegrationConstants.KEY_VERSION, versionText.getText());

        config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES, "" + limitInstancesButton.getSelection());
        if (limitInstancesButton.getSelection()) {
            config.put(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT, limitInstancesText.getText());
        }
        config.remove(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        if (defaultTempDirButton.getSelection()) {
            config.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY, "");
        } else {
            config.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY, rootWorkingDirText.getText());
        }

    }

    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);
        validateInput();
        installListeners();
    }

    private void installListeners() {
        ModifyListener ml = arg0 -> {
            saveAllConfig();
            validateInput();
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
                 * hostText.setEnabled(hostButton.getSelection()); portText.setEnabled(hostButton.getSelection());
                 * chooseRootDirPathButton.setEnabled(!hostButton.getSelection() && customTempDirButton.getSelection());
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
                rootWorkingDirText.setEnabled(!defaultTempDirButton.getSelection());
                chooseRootDirPathButton.setEnabled(/* !hostButton.getSelection() && */!defaultTempDirButton.getSelection());
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        };
        defaultTempDirButton.addSelectionListener(sl2);
    }

    protected void validateInput() {
        boolean isValidToolDirectory = false;
        boolean isValidVersion = false;
        boolean isValidWorkingDirectory = false;
        boolean isValidLimitation = false;
        setMessage("");
        Optional<String> versionResult = ComponentIdRules.validateComponentVersionRules(versionText.getText());

        isValidToolDirectory = toolDirectoryText.getText() != null && !toolDirectoryText.getText().isEmpty();

        if (versionText.getText() != null && !versionText.getText().isEmpty()) {
            isValidVersion = !versionResult.isPresent();
        }

        if (defaultTempDirButton.getSelection()) {
            isValidWorkingDirectory = true;
        } else {
            isValidWorkingDirectory = rootWorkingDirText.getText() != null && !rootWorkingDirText.getText().isEmpty();
        }
        if (!limitInstancesButton.getSelection()) {
            isValidLimitation = true;
        } else {
            isValidLimitation = limitInstancesText.getText().matches("\\d+");
        }

        if (!isValidLimitation) {
            setMessage("Invalid limitation value.");
        }
        if (!isValidWorkingDirectory) {
            setMessage(STRING_INVALID_WORKING_DIRECTORY);
        }
        if (!isValidVersion) {
            setMessage(StringUtils.format(STRING_INVALID_VERSION, versionResult.get().replaceAll("&", "&&")));
        }
        if (!isValidToolDirectory) {
            setMessage(STRING_INVALID_TOOL_DIRECTORY);
        }
        getButton(IDialogConstants.OK_ID)
            .setEnabled(isValidToolDirectory && isValidVersion && isValidWorkingDirectory && isValidLimitation);

    }

    public Map<String, String> getConfig() {
        return config;
    }
}
