/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.api.ComponentGroupPathRules;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.gui.integration.common.IntegrationHelper;
import de.rcenvironment.core.gui.integration.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Page for choosing an existent ToolIntegration configuration.
 * 
 * @author Sascha Zur
 * @author Adrian Stock
 */
public class ChooseConfigurationPage extends ToolIntegrationWizardPage {

    private static final Log LOGGER = LogFactory.getLog(ChooseConfigurationPage.class);

    private static final String JSON_SUFFIX = ".json";

    private Text textChosenConfig;

    private List toolList;

    private ListViewer templateListViewer;

    private final ToolIntegrationWizard wizard;

    private final boolean isEditWizard;

    private Button newIntegrationButton = null;

    private Button loadInactiveConfigurationButton;

    private Button loadTemplateButton;

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private final Collection<IntegrationContext> allIntegrationContexts;

    private final Map<String, Map<String, Object>> allConfigurations;

    private Map<String, String> displayedToolNamesToToolNames;
    
    private IntegrationHelper integrationHelper = new IntegrationHelper();

    protected static ChooseConfigurationPage createWithoutPreselectedTool(String pageName,
        Collection<IntegrationContext> allIntegrationContexts,
        ToolIntegrationWizard wizard, String type) {
        return new ChooseConfigurationPage(pageName, allIntegrationContexts, wizard, type);
    }

    protected ChooseConfigurationPage(String pageName, Collection<IntegrationContext> allIntegrationContexts,
        ToolIntegrationWizard wizard, String type) {
        super(pageName);
        setTitle(pageName);

        if (ToolIntegrationConstants.NEW_WIZARD.equals(type)) {
            this.isEditWizard = false;
        } else if (ToolIntegrationConstants.EDIT_WIZARD.equals(type)) {
            this.isEditWizard = true;
        } else {
            throw new IllegalArgumentException(StringUtils.format("Unexpected wizard type '%s'", type));
        }

        if (this.isNewWizard()) {
            setDescription(Messages.newIntegrationDescription);
        } else {
            setDescription(Messages.editIntegrationDescription);
        }

        this.wizard = wizard;
        this.allIntegrationContexts = allIntegrationContexts;
        allConfigurations = new TreeMap<>();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);
        GridData containerData =
            new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        container.setLayoutData(containerData);

        if (isNewWizard()) {
            createNewIntegrationPart(container);
            createTemplatePart(container);
            createInactivePart(container);
        } else {
            new Label(container, SWT.NONE).setText(Messages.chooseConfigToEdit);
        }

        final Map<String, String> configs = readExistingConfigurations();
        ListViewer toolListViewer = new ListViewer(container, SWT.SINGLE | SWT.BORDER);
        toolListViewer.addDoubleClickListener(new ToolIntegrationDoubleClickListener(this));
        toolList = toolListViewer.getList();
        GridData toolListData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        toolList.setLayoutData(toolListData);

        displayedToolNamesToToolNames = new TreeMap<>();

        for (String toolName : configs.keySet()) {
            String displayedToolName = createDisplayedToolName(toolName);
            displayedToolNamesToToolNames.put(displayedToolName, toolName);
            toolList.add(displayedToolName);
        }

        if (toolList.getItemCount() == 0) {
            toolList.setEnabled(false);
        }
        if (loadInactiveConfigurationButton != null && toolList.getItemCount() == 0) {
            loadInactiveConfigurationButton.setEnabled(false);
        }
        toolList.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (toolList.isEnabled()) {
                    int[] selectedItems = toolList.getSelectionIndices();
                    textChosenConfig.setText("");
                    if (selectedItems.length > 0) {

                        String toolName = getToolNameFromDisplayedToolName(toolList.getItem(selectedItems[0]));

                        loadToolConfigurationIntoWizard(toolName);
                    }
                    setPageComplete(true);
                    if (isEditWizard()) {

                        ToolIntegrationWizard wiz = (ToolIntegrationWizard) getWizard();
                        wiz.updateAllPages();

                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        if (isEditWizard()) {
            Button loadConfigButton = new Button(container, SWT.PUSH);
            loadConfigButton.setText(Messages.loadConfigurationButton);
            loadConfigButton.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    FileDialog dialog = new FileDialog(getShell());
                    dialog.setFilterExtensions(new String[] { "*.json" });
                    String file = dialog.open();
                    if (file != null) {
                        File configJson = new File(file);
                        loadConfigurationFromFile(configJson);
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent arg0) {
                    widgetSelected(arg0);
                }
            });

        }
        Composite chosenComposite = new Composite(container, SWT.NONE);
        chosenComposite.setLayout(new GridLayout(2, false));
        chosenComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        new Label(chosenComposite, SWT.NONE).setText(Messages.selectedConfig);

        textChosenConfig = new Text(chosenComposite, SWT.BORDER);
        textChosenConfig.setEditable(false);
        textChosenConfig.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                setPageComplete((textChosenConfig.getText() != null && !textChosenConfig.getText().equals("")));
            }
        });
        textChosenConfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        textChosenConfig.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        if (newIntegrationButton != null) {
            newIntegrationButton.setSelection(true);
            toolList.setEnabled(false);
            textChosenConfig.setEnabled(false);
            toolList.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        }
        setControl(container);
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();

        if (isNewWizard()) {
            helpSystem.setHelp(this.getControl(), "de.rcenvironment.core.gui.wizard.toolintegration.integration_chooseconfig");
        } else {
            helpSystem.setHelp(this.getControl(), "de.rcenvironment.core.gui.wizard.toolintegration.integration_editconfig");
        }
    }

    public void selectToolConfiguration(String toolname) {
        this.loadToolConfigurationIntoWizard(toolname);
        this.selectTool(toolname);
    }

    private void loadToolConfigurationIntoWizard(String toolName) {
        final Map<String, String> configs = readExistingConfigurations();
        File configJson = new File(configs.get(toolName));
        loadConfigurationFromFile(configJson);
    }

    private String getToolNameFromDisplayedToolName(String displayedToolName) {
        return displayedToolNamesToToolNames.get(displayedToolName);
    }

    private void selectTool(String toolname) {
        final int toolIndex = getToolIndexByToolname(toolname);
        toolList.setSelection(toolIndex);
    }

    private int getToolIndexByToolname(String toolname) {
        for (int i = 0; i < toolList.getItemCount(); ++i) {
            final String currentDisplayedName = toolList.getItem(i);
            final String currentToolName = getToolNameFromDisplayedToolName(currentDisplayedName);
            if (currentToolName.equals(toolname)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Toolname " + toolname + " not found in toollist");
    }

    private void createInactivePart(Composite container) {
        loadInactiveConfigurationButton = new Button(container, SWT.RADIO);
        loadInactiveConfigurationButton.setText(Messages.chooseInactiveButton);
        loadInactiveConfigurationButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (loadInactiveConfigurationButton.getSelection()) {
                    setPageComplete(false);
                    toolList.setEnabled(true);
                    textChosenConfig.setEnabled(true);
                    toolList.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                    toolList.deselectAll();
                    templateListViewer.getControl().setEnabled(false);
                    templateListViewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    templateListViewer.getList().deselectAll();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
    }

    private void createTemplatePart(Composite container) {
        loadTemplateButton = new Button(container, SWT.RADIO);
        loadTemplateButton.setText(Messages.newConfigurationFromTemplateButton);
        loadTemplateButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (loadTemplateButton.getSelection()) {
                    setPageComplete(false);
                    templateListViewer.getControl().setEnabled(true);
                    textChosenConfig.setEnabled(true);
                    templateListViewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                    templateListViewer.getList().deselectAll();
                    toolList.setEnabled(false);
                    textChosenConfig.setEnabled(false);
                    toolList.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    toolList.deselectAll();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        final Map<String, Map<String, Object>> templates = readTemplates();
        TemplateListContentProvider templateListContentProvider = new TemplateListContentProvider();
        TemplateListData templateListData = new TemplateListData();
        templateListViewer = new ListViewer(container, SWT.SINGLE | SWT.BORDER);
        templateListViewer.setContentProvider(templateListContentProvider);
        templateListViewer.setComparator(new ViewerComparator() {

            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                return ((TemplateListItem) e1).toString().compareTo(((TemplateListItem) e2).toString());
            }

        });
        templateListViewer.getControl().setEnabled(false);
        templateListViewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        GridData templateListGridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        templateListViewer.getControl().setLayoutData(templateListGridData);
        for (String templateFileName : templates.keySet()) {
            String integrationType = (String) templates.get(templateFileName).get(IntegrationConstants.INTEGRATION_TYPE);
            if (integrationType == null) {
                integrationType = IntegrationContextType.COMMON.toString();
            }
            TemplateListItem item = new TemplateListItem(templateFileName, integrationType);
            String templateDisplayName = (String) templates.get(templateFileName).get(ToolIntegrationConstants.KEY_TEMPLATE_NAME);
            if (templateDisplayName != null) {
                item.setDisplayName(templateDisplayName);
            }
            templateListData.add(item);
        }
        templateListViewer.setInput(templateListData);
        if (loadTemplateButton != null && templateListData.getItems().isEmpty()) {
            loadTemplateButton.setEnabled(false);
            templateListViewer.getControl().setEnabled(false);
        }
        templateListViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent arg0) {
                if (templateListViewer.getControl().isEnabled()) {
                    IStructuredSelection selection = (IStructuredSelection) templateListViewer.getSelection();
                    if (!selection.isEmpty()) {
                        TemplateListItem item = (TemplateListItem) selection.getFirstElement();
                        for (Entry<String, Map<String, Object>> e : templates.entrySet()) {
                            if (item != null && e.getKey().equals(item.getFilename())) {
                                wizard.removeAdditionalPages();
                                wizard.setPreviousConfiguration(e.getValue(), new File(e.getKey()));
                                if ((String) e.getValue().get(IntegrationConstants.INTEGRATION_TYPE) != null) {
                                    ((ToolIntegrationWizard) getWizard()).setIntegrationType((String) e.getValue().get(
                                        IntegrationConstants.INTEGRATION_TYPE), true);
                                    wizard.setAdditionalPages((String) e.getValue().get(IntegrationConstants.INTEGRATION_TYPE));
                                } else {
                                    ((ToolIntegrationWizard) getWizard())
                                        .setIntegrationType(IntegrationContextType.COMMON.toString(), true);
                                }
                                if (wizard.getConfigurationMap().containsKey(ToolIntegrationConstants.KEY_TEMPLATE_NAME)) {
                                    wizard.getConfigurationMap().remove(ToolIntegrationConstants.KEY_TEMPLATE_NAME);
                                }
                            }
                        }
                    }
                    setPageComplete(true);
                }
            }
        });

        templateListViewer.addDoubleClickListener(new ToolIntegrationDoubleClickListener(this));
    }

    private Map<String, Map<String, Object>> readTemplates() {
        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
        for (IntegrationContext context : allIntegrationContexts) {
            File templateRootPath =
                new File(context.getRootPathToToolIntegrationDirectory() + File.separator + ToolIntegrationConstants.TEMPLATE_PATH);
            if (!templateRootPath.exists()) {
                templateRootPath.mkdirs();
            }
            readTemplateDir(result, templateRootPath);
            File[] readOnlyList = context.getReadOnlyPathsList();
            for (File dir : readOnlyList) {
                readTemplateDir(result, new File(dir.getAbsolutePath() + File.separator + ToolIntegrationConstants.TEMPLATE_PATH));
            }
        }
        return result;
    }

    private void readTemplateDir(Map<String, Map<String, Object>> result, File templateRootPath) {
        if (templateRootPath != null && templateRootPath.listFiles() != null) {
            for (File f : templateRootPath.listFiles()) {
                if (f.isFile() && f.getName().endsWith(JSON_SUFFIX)) {
                    try {
                        @SuppressWarnings("unchecked") Map<String, Object> configurationMap =
                            mapper.readValue(f, new HashMap<String, Object>().getClass());
                        result.put(f.getAbsolutePath(), configurationMap);
                    } catch (IOException e) {
                        LOGGER.error("Integration: Could not read templates: ", e);
                    }
                }
            }
        }
    }

    @Override
    public void setPageComplete(boolean complete) {
        if (complete && isEditWizard()) {
            complete &= textChosenConfig.getText() != null && !textChosenConfig.getText().isEmpty();
        } else if (complete && isNewWizard()
            && loadInactiveConfigurationButton.getSelection()) {
            complete &= textChosenConfig.getText() != null && !textChosenConfig.getText().isEmpty();
        } else if (complete && isNewWizard()
            && loadTemplateButton.getSelection()) {
            complete &= templateListViewer.getSelection() != null && !templateListViewer.getSelection().isEmpty();
        }
        super.setPageComplete(complete);
    }

    private void createNewIntegrationPart(Composite container) {
        newIntegrationButton = new Button(container, SWT.RADIO);
        newIntegrationButton.setText(StringUtils.format(Messages.newConfigurationButton,
            IntegrationContextType.COMMON.toString()));
        newIntegrationButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (newIntegrationButton.getSelection()) {
                    textChosenConfig.setText("");
                    Map<String, Object> newConfiguration = new HashMap<String, Object>();
                    newConfiguration.put(IntegrationConstants.INTEGRATION_TYPE,
                        IntegrationContextType.COMMON.toString());
                    wizard.setPreviousConfiguration(newConfiguration, null);
                    wizard.removeAdditionalPages();
                    toolList.setEnabled(false);
                    templateListViewer.getControl().setEnabled(false);
                    textChosenConfig.setEnabled(false);
                    toolList.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    toolList.deselectAll();
                    templateListViewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                    templateListViewer.getList().deselectAll();
                    setPageComplete(true);
                    wizard.setIntegrationType(IntegrationContextType.COMMON.toString(),
                        false);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        for (final IntegrationContext context : allIntegrationContexts) {
            if (!context.getContextId().equals(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID)) {
                final Button newExtensionIntegrationButton = new Button(container, SWT.RADIO);
                newExtensionIntegrationButton.setText(StringUtils.format(Messages.newConfigurationButton, context.getContextTypeString()));
                newExtensionIntegrationButton.addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        if (newExtensionIntegrationButton.getSelection()) {
                            textChosenConfig.setText("");
                            Map<String, Object> newConfiguration = new HashMap<String, Object>();
                            newConfiguration.put(IntegrationConstants.INTEGRATION_TYPE, context.getContextTypeString());
                            wizard.setPreviousConfiguration(newConfiguration, null);
                            wizard.removeAdditionalPages();
                            wizard.setAdditionalPages(context.getContextTypeString());
                            toolList.setEnabled(false);
                            templateListViewer.getControl().setEnabled(false);
                            textChosenConfig.setEnabled(false);
                            toolList.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                            toolList.deselectAll();
                            templateListViewer.getControl().setBackground(
                                Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                            templateListViewer.getList().deselectAll();
                            setPageComplete(true);
                            wizard.setIntegrationType(context.getContextTypeString(), false);
                        }
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent arg0) {
                        widgetSelected(arg0);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadConfigurationFromFile(File configJson) {
        if (configJson.exists() && configJson.isFile()) {
            Map<String, Object> configurationMap = null;
            try {
                configurationMap = mapper.readValue(configJson, HashMap.class);
            } catch (IOException e) {
                LOGGER.error(e);
            }

            if (configurationMap != null) {
                textChosenConfig.setText(configJson.getAbsolutePath());
            } else {
                textChosenConfig.setText("");
            }
            wizard.setPreviousConfiguration(configurationMap, configJson);
            // if tool is active, the integration wizard is supposed to behave like an edit wizard
            // otherwise it should behave as if the tool is integrated -schr_m
            wizard.setIsEdit(configurationMap != null && configurationMap.get(IntegrationConstants.IS_ACTIVE) != null
                && (Boolean) configurationMap.get(IntegrationConstants.IS_ACTIVE));
            wizard.removeAdditionalPages();
            if (configurationMap != null) {
                wizard.setAdditionalPages((String) configurationMap.get(IntegrationConstants.INTEGRATION_TYPE));
            }
        } else {
            wizard.setPreviousConfiguration(null, null);
            wizard.removeAdditionalPages();
        }
    }

    private class ToolConfiguration {

        private Map<String, Object> configurationMap;

        protected ToolConfiguration(Map<String, Object> configurationMap) {
            this.configurationMap = configurationMap;
        }

        public boolean isActive() {
            final Object isActiveValue = configurationMap.get(IntegrationConstants.IS_ACTIVE);
            return isActiveValue != null && ((Boolean) isActiveValue);
        }

        public boolean isInactive() {
            final Object isActiveValue = configurationMap.get(IntegrationConstants.IS_ACTIVE);
            return isActiveValue != null && !((Boolean) isActiveValue);
        }

        public String getToolName() {
            return (String) configurationMap.get(IntegrationConstants.KEY_COMPONENT_NAME);
        }

        public Map<String, Object> toMap() {
            return new HashMap<>(this.configurationMap);
        }
    }

    private class ToolIntegrationContextDirectory {

        private final File directoryFile;

        ToolIntegrationContextDirectory(File directoryFile) {
            this.directoryFile = directoryFile;
        }

        private boolean isPotentiallyValid() {
            return directoryFile.exists() && directoryFile.isDirectory() && directoryFile.listFiles() != null
                && directoryFile.listFiles().length > 0;
        }

        public File[] listFiles() {
            return this.directoryFile.listFiles();
        }

    }

    private class ToolIntegrationDirectory {

        private final File toolFolder;

        ToolIntegrationDirectory(File toolFolder) {
            this.toolFolder = toolFolder;
        }

        private boolean isPotentiallyValid() {
            return toolFolder != null && toolFolder.isDirectory() && !toolFolder.getName().equals("null")
                && toolFolder.listFiles() != null && toolFolder.listFiles().length > 0;
        }
        
        public File getToolFolder() {
            return toolFolder;
        }

    }

    private Map<String, String> readExistingConfigurations() {

        Map<String, String> allConfigs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (IntegrationContext context : allIntegrationContexts) {
            String configFolder = context.getRootPathToToolIntegrationDirectory();
            final ToolIntegrationContextDirectory toolIntegrationContextFolder =
                new ToolIntegrationContextDirectory(new File(configFolder, context.getNameOfToolIntegrationDirectory()));

            if (!toolIntegrationContextFolder.isPotentiallyValid()) {
                continue;
            }

            for (File toolFolder : toolIntegrationContextFolder.listFiles()) {
                final ToolIntegrationDirectory integrationDirectory = new ToolIntegrationDirectory(toolFolder);
                if (!integrationDirectory.isPotentiallyValid()) {
                    continue;
                }

                final Optional<File> f = integrationHelper.tryFindConfigurationFile(context, integrationDirectory.getToolFolder());
                if (!f.isPresent()) {
                    continue;
                }

                try {
                    @SuppressWarnings("unchecked") ToolConfiguration toolConfiguration =
                        new ToolConfiguration(mapper.readValue(f.get(), HashMap.class));

                    final String toolName = toolConfiguration.getToolName();
                    if ((this.isNewWizard() && toolConfiguration.isInactive())
                        || (this.isEditWizard() && toolConfiguration.isActive())) {
                        allConfigs.put(toolName, f.get().getAbsolutePath());
                    }
                    allConfigurations.put(toolName, toolConfiguration.toMap());
                } catch (IOException e) {
                    LOGGER.error("Could not read configuration file: ", e);
                }
            }
        }
        return allConfigs;
    }

    private boolean isNewWizard() {
        return !isEditWizard;
    }

    private boolean isEditWizard() {
        return isEditWizard;
    }

    /**
     * @return the path to the chosen configuration for other pages.
     */
    public String getChoosenConfigPath() {
        return textChosenConfig.getText();
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        if (isNewWizard()) {
            helpSystem.displayHelp("de.rcenvironment.core.gui.wizard.toolintegration.integration_chooseconfig");
        } else {
            helpSystem.displayHelp("de.rcenvironment.core.gui.wizard.toolintegration.integration_editconfig");
        }

    }

    @Override
    public void setConfigMap(Map<String, Object> newConfigurationMap) {

    }

    @Override
    public void updatePage() {
        if (isEditWizard()
            && isCurrentPage()
            && (textChosenConfig.getText() == null || textChosenConfig.getText().isEmpty())) {
            setPageComplete(false);
        }
    }

    /**
     * show next page for double click event.
     * 
     * @param iWizardPage to show.
     */
    public void showPage(IWizardPage iWizardPage) {
        getContainer().showPage(iWizardPage);
    }

    /**
     * Creates the string that will be displayed in the edit / remove page.
     * 
     * @param toolName of the tool
     * 
     * @return a string that will be displayed in the gui.
     */
    public String createDisplayedToolName(String toolName) {
        String type = IntegrationContextType.COMMON.toString();
        if (allConfigurations.get(toolName).get(IntegrationConstants.INTEGRATION_TYPE) != null) {
            type = (String) allConfigurations.get(toolName).get(IntegrationConstants.INTEGRATION_TYPE);
        }
        @SuppressWarnings({ "rawtypes", "unchecked" }) ArrayList<LinkedHashMap> launchSettings =
            (ArrayList<LinkedHashMap>) allConfigurations.get(toolName).get(IntegrationConstants.KEY_LAUNCH_SETTINGS);
        @SuppressWarnings("unchecked") String version =
            ((LinkedHashMap<Object, String>) (launchSettings.get(0))).get(IntegrationConstants.KEY_VERSION);
        Object[] toolInformation = { toolName, version, type };

        String displayedToolName = StringUtils.format("%s (%s; Type: %s)", toolInformation);
        boolean disabled = false;
        StringBuilder errorString = new StringBuilder();
        if (ComponentIdRules.validateComponentIdRules(toolName).isPresent()) {
            errorString.append(" [INVALID TOOL NAME]");
            disabled = true;
        }
        if (ComponentIdRules.validateComponentVersionRules(version).isPresent()) {
            errorString.append(" [INVALID VERSION]");
            disabled = true;
        }
        if (allConfigurations.get(toolName).get(IntegrationConstants.KEY_GROUPNAME) != null) {
            String groupName = (String) allConfigurations.get(toolName).get(IntegrationConstants.KEY_GROUPNAME);
            if (!groupName.isEmpty() && ComponentGroupPathRules
                .validateComponentGroupPathRules(groupName)
                .isPresent()) {
                errorString.append(" [INVALID GROUP NAME]");
                disabled = true;
            }
        }
        if (disabled) {
            displayedToolName = StringUtils.format("%s%s - DISABLED", displayedToolName, errorString.toString());
        }

        return displayedToolName;
    }
}

/**
 * List of {@link TemplateListItem} as input for the list viewer.
 * 
 * @author Jan Flink
 */
final class TemplateListData {

    private final java.util.List<TemplateListItem> items = new ArrayList<TemplateListItem>();

    protected void add(TemplateListItem item) {
        items.add(item);
    }

    protected java.util.List<TemplateListItem> getItems() {
        return items;
    }

}

/**
 * Content provider for the list of templates.
 * 
 * @author Jan Flink
 */
final class TemplateListContentProvider implements IStructuredContentProvider {

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

    @Override
    public Object[] getElements(Object arg0) {
        return ((TemplateListData) arg0).getItems().toArray();
    }

}

/**
 * Listener for double clicking in lists.
 * 
 * @author Sascha Zur
 */
class ToolIntegrationDoubleClickListener implements IDoubleClickListener {

    private ChooseConfigurationPage page;

    ToolIntegrationDoubleClickListener(ChooseConfigurationPage page) {
        this.page = page;
    }

    @Override
    public void doubleClick(DoubleClickEvent event) {
        if (page.canFlipToNextPage()) {
            IWizardPage[] pages = page.getWizard().getPages();
            page.showPage(pages[1]);
        }

    }

}

/**
 * Item for the template list.
 * 
 * @author Jan Flink
 */
final class TemplateListItem {

    private static final String JSON_SUFFIX = ".json";

    private final String fileName;

    private String displayName;

    private final String integrationType;

    TemplateListItem(String fileName, String integrationType) {
        this.fileName = fileName;
        this.integrationType = integrationType;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    String getFilename() {
        return fileName;
    }

    @Override
    public String toString() {
        String item = "%s (Type: %s)";
        String name;
        if (displayName != null) {
            name = displayName;
        } else {
            name = fileName.substring(fileName.lastIndexOf(File.separator) + 1,
                fileName.indexOf(JSON_SUFFIX));
        }
        return StringUtils.format(item, name, integrationType);
    }

}
