/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.configuration;

import java.io.File;
import java.io.FileFilter;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.start.gui.WorkspaceSettings;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Dialog that shows information about the configuration and offers to open sample configuration files.
 * 
 * @author Oliver Seebach
 * @author Robert Mischke
 * @author Sascha Zur
 */
public class ConfigurationInformationDialog extends Dialog {

    private static final int MINIMUM_HEIGHT = 250;

    private static final int MINIMUM_WIDTH = 500;

    private final ConfigurationService configurationService;

    private final SortedMap<String, File> exampleConfigFileNamesAndPaths = new TreeMap<>();

    private String profileConfigPath = "";

    private String installationConfigPath = "";

    private String workspaceLocation = "";

    private boolean dontAskForWorkspaceLocationOnStartup = false;

    private final Log log = LogFactory.getLog(getClass());

    private Button loadButton;

    private org.eclipse.swt.widgets.List examplesList;

    private Button askWorkspaceLocationButton;

    public ConfigurationInformationDialog(Shell parentShell) {
        super(parentShell);
        configurationService = ServiceRegistry.createAccessFor(this).getService(ConfigurationService.class);
        setShellStyle(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.RESIZE);
        initConfigurationFileAndPathMapping();
        loadConfigurationDetails();

    }

    @Override
    protected void configureShell(Shell shell) {
        shell.setText("Configuration Information");
        shell.setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        super.configureShell(shell);
    }

    private void loadConfigurationDetails() {
        profileConfigPath = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_ROOT).getAbsolutePath();
        // systemConfigPath = ...
        installationConfigPath = configurationService.getConfigurablePath(ConfigurablePathId.INSTALLATION_DATA_ROOT).getAbsolutePath();

        // get last workspace location and "don't as again" setting
        WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance();
        workspaceLocation = workspaceSettings.getLastLocation();
        dontAskForWorkspaceLocationOnStartup = workspaceSettings.getDontAskAgainSetting();
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite dialogArea = (Composite) super.createDialogArea(parent);
        dialogArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        dialogArea.setLayout(new GridLayout());

        ScrolledComposite scrolled = new ScrolledComposite(dialogArea,
            SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolled.setLayout(new GridLayout());
        scrolled.setExpandVertical(true);
        scrolled.setExpandHorizontal(true);

        Composite container = new Composite(scrolled, SWT.NONE);
        scrolled.setContent(container);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout());

        // ----------------------------------------------------
        Group informationGroup = new Group(container, SWT.NONE);
        informationGroup.setText("General");

        GridLayout informationGroupGridLayout = new GridLayout(1, true);
        informationGroup.setLayout(informationGroupGridLayout);

        GridData informationGroupGridData = new GridData();
        informationGroupGridData.grabExcessHorizontalSpace = true;
        informationGroupGridData.verticalAlignment = GridData.BEGINNING;
        informationGroupGridData.horizontalAlignment = GridData.FILL;
        informationGroup.setLayoutData(informationGroupGridData);

        CLabel informationLabel = new CLabel(informationGroup, SWT.NONE);
        informationLabel.setText("You need to configure RCE to change the name of your instance, connect your instance to others, etc.\n"
            + "All configuration is done within a single file called \"configuration.json\" in your profile directory \n"
            + "(see 'File System Locations' below). You can easily open and edit it with the \"Configuration > Open Configuration File\" \n"
            + "menu option, or with the corresponding tool bar button.");
        informationLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));
        
        // ----------------------------------------------------
        Group examplesGroup = new Group(container, SWT.NONE);
        examplesGroup.setText("Example Configurations");

        GridLayout examplesGroupGridLayout = new GridLayout(1, true);
        examplesGroup.setLayout(examplesGroupGridLayout);

        GridData examplesGroupGridData = new GridData();
        examplesGroupGridData.grabExcessHorizontalSpace = true;
        examplesGroupGridData.verticalAlignment = GridData.BEGINNING;
        examplesGroupGridData.horizontalAlignment = GridData.FILL;
        examplesGroup.setLayoutData(examplesGroupGridData);

        CLabel exampleLabel = new CLabel(examplesGroup, SWT.NONE);
        exampleLabel
            .setText("The following list shows configuration examples for typical configuration scenarios. You can open them, copy their \n"
                + "content into your own configuration file, and adapt it to suit your needs. A special example file called \n"
                + "\"Configuration Reference\" contains all existing parameters, which you can use for reference.\n ");

        examplesList = new org.eclipse.swt.widgets.List(examplesGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        String[] examplesItems = exampleConfigFileNamesAndPaths.keySet().toArray(
            new String[exampleConfigFileNamesAndPaths.keySet().size()]);
        examplesList.setItems(examplesItems);
        examplesList.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        examplesList.addSelectionListener(new ExampleListSelectionListener());
        examplesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent event) {
                openSelectionInEditor();
                // close dialog.
                closeConfigurationDialog();
            }
        });
        loadButton = new Button(examplesGroup, SWT.PUSH);
        loadButton.setText("Open in editor (read-only)");
        loadButton.setEnabled(false);
        loadButton.addSelectionListener(new LoadButtonSelectionListener());

        // ----------------------------------------------------
        Group locationsGroup = new Group(container, SWT.NONE);
        locationsGroup.setText("File System Locations");
        
        GridLayout locationsGroupGridLayout = new GridLayout(2, false);
        locationsGroup.setLayout(locationsGroupGridLayout);
        
        GridData locationsGroupGridData = new GridData();
        locationsGroupGridData.grabExcessHorizontalSpace = true;
        locationsGroupGridData.verticalAlignment = GridData.BEGINNING;
        locationsGroupGridData.horizontalAlignment = GridData.FILL;
        locationsGroup.setLayoutData(locationsGroupGridData);

        Label installationLabel = new Label(locationsGroup, SWT.NONE);
        installationLabel.setText("Installation:");

        Text installationConfigText = new Text(locationsGroup, SWT.READ_ONLY | SWT.BORDER);
        installationConfigText.setText(installationConfigPath);
        installationConfigText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        installationConfigText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        Label profileLabel = new Label(locationsGroup, SWT.NONE);
        profileLabel.setText("Profile:");

        Text profileConfigText = new Text(locationsGroup, SWT.READ_ONLY | SWT.BORDER);
        profileConfigText.setText(profileConfigPath);
        profileConfigText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        profileConfigText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        Label workspaceLabel = new Label(locationsGroup, SWT.NONE);
        workspaceLabel.setText("Workspace:");

        Text workspaceText = new Text(locationsGroup, SWT.READ_ONLY | SWT.BORDER);
        workspaceText.setText(workspaceLocation);
        workspaceText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        workspaceText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));

        new Label(locationsGroup, SWT.NONE);

        askWorkspaceLocationButton = new Button(locationsGroup, SWT.CHECK);
        askWorkspaceLocationButton.setText("Always use this workspace location (don't ask on startup)");
        askWorkspaceLocationButton.setSelection(dontAskForWorkspaceLocationOnStartup);
        askWorkspaceLocationButton.addSelectionListener(new AskWorkspaceToggleButtonListener());

        GridData checkBoxData = new GridData();
        checkBoxData.horizontalIndent = 10;
        askWorkspaceLocationButton.setLayoutData(checkBoxData);

        // ----------------------------------------------------
        Group applyGroup = new Group(container, SWT.NONE);
        applyGroup.setText("Apply Changes");

        GridLayout applyGroupGridLayout = new GridLayout(1, true);
        applyGroup.setLayout(applyGroupGridLayout);

        GridData applyGroupGridData = new GridData();
        applyGroupGridData.grabExcessHorizontalSpace = true;
        applyGroupGridData.verticalAlignment = GridData.BEGINNING;
        applyGroupGridData.horizontalAlignment = GridData.FILL;
        applyGroup.setLayoutData(applyGroupGridData);

        CLabel applyLabel = new CLabel(applyGroup, SWT.NONE);
        applyLabel
            .setText("You need to restart RCE to apply any changes you made in the configuration file.\n"
                + "Note that there is a \"Restart\" shortcut in the \"File\" menu for this.");
        applyLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));

        // ----------------------------------------------------
        container.setSize(container.computeSize(SWT.NONE,
            SWT.NONE, true));
        scrolled.setMinSize(getInitialSize());
        scrolled.setVisible(true);
        return dialogArea;
    }

    private void initConfigurationFileAndPathMapping() {
        File examplesDir = configurationService.getConfigurablePath(ConfigurablePathId.CONFIGURATION_SAMPLES_LOCATION);
        if (!examplesDir.isDirectory()) {
            log.warn("Expected location for configuration example files does not exist: " + examplesDir.getAbsolutePath());
            return;
        }
        File[] files = examplesDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".sample");
            }
        });
        for (File file : files) {
            String name = file.getName();
            String shortName = WordUtils.capitalize(name.replaceFirst("^configuration\\.json\\.(.+)\\.sample$", "$1").replace("_", " "));
            exampleConfigFileNamesAndPaths.put(shortName, file);
        }
    }

    private void closeConfigurationDialog() {
        this.close();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    private void openSelectionInEditor() {
        if (examplesList.getSelectionCount() == 1) {
            int selectionIndex = examplesList.getSelectionIndex();
            if (selectionIndex < examplesList.getItemCount()) {
                String selection = examplesList.getItem(selectionIndex);
                File configFile = exampleConfigFileNamesAndPaths.get(selection);
                try {
                    configFile.setReadOnly();
                    EditorsHelper.openExternalFileInEditor(configFile);
                } catch (PartInitException e) {
                    log.error("Failed to open profile configuration file in an editor.", e);
                }
            }
        }
    }

    /**
     * Listener that reacts on selections in example config list.
     * 
     * @author Oliver Seebach
     * 
     */
    private final class ExampleListSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            if (examplesList.getSelectionCount() == 1) {
                loadButton.setEnabled(true);
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

    /**
     * Listener for toggling "ask for workspace location on start" button.
     * 
     * @author Oliver Seebach
     * @author Robert Mischke
     */
    private final class AskWorkspaceToggleButtonListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            boolean newDontAskAgainValue = askWorkspaceLocationButton.getSelection();
            WorkspaceSettings.getInstance().setDontAskAgainSetting(newDontAskAgainValue);
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetDefaultSelected(event);
        }
    }

    /**
     * Listener that reacts on click on load button.
     * 
     * @author Oliver Seebach
     * 
     */
    private final class LoadButtonSelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent event) {
            openSelectionInEditor();
            // close dialog.
            closeConfigurationDialog();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
        }
    }

}
