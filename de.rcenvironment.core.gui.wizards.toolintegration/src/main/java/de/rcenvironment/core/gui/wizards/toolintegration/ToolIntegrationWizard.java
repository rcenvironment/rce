/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributor;
import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributorRegistry;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Wizard for integrating a new external tool.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (disabled mixed-in component publishing)
 * @author Alexander Weinert (refactoring)
 */
public class ToolIntegrationWizard extends Wizard {

    /**
     * Once the wizard is finished, it remains to actually integrate the tool. In order to not block the GUI, this is done in a background
     * job. Since this job requires a lot of configuration from the wizard and it would be cumbersome to pass this configuration into the
     * job before running it, the job is implemented as an inner class instead of being contained in a dedicated file.
     * 
     * @author Alexander Weinert
     */
    private final class ToolIntegrationJob extends Job {

        private final Shell shell;

        /**
         * @param name A human-readable name for the Job, see parameter of same name in the constructor of {@link Job}
         * @param shell The shell to use to display error messages to the user
         */
        private ToolIntegrationJob(String name, Shell shell) {
            super(name);
            this.shell = shell;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (!isConfigurationOk()) {
                return Status.CANCEL_STATUS;
            }

            determineIntegrationContext();
            integrationService.setFileWatcherActive(false);
            configurationMap.put(ToolIntegrationConstants.IS_ACTIVE, true);
            handleIcon();
            // tool publication as part of the integration UI is disabled; see Mantis #16044
            // Boolean publish = (Boolean)
            // configurationMap.get(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
            File toolPath = new File(
                new File(
                    new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                        integrationContext.getNameOfToolIntegrationDirectory()),
                    integrationContext.getToolDirectoryPrefix()),
                (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
            if (toolDocuTarget != null && configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH) != null
                && !((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH)).isEmpty()) {
                try {
                    FileUtils.copyDirectory(toolDocuTarget, new File(toolPath, ToolIntegrationConstants.DOCS_DIR_NAME));
                    TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(toolDocuTarget);
                } catch (IOException e) {
                    LOGGER.error("Could not copy icon from temporary tool directory.", e);
                }
            }
            if (configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH) == null
                || ((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH)).isEmpty()) {
                File docsDir = new File(toolPath, ToolIntegrationConstants.DOCS_DIR_NAME);
                if (docsDir.exists()) {
                    FileUtils.deleteQuietly(docsDir);
                }
            }
            // tool publication as part of the integration UI is disabled; see Mantis #16044
            // if (publish != null && publish) {
            // integrationService.addPublishedTool(toolPath.getAbsolutePath());
            // configurationMap.remove(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
            // } else {
            // integrationService.unpublishTool(toolPath.getAbsolutePath());
            // }
            // integrationService.savePublishedComponents(integrationContext);
            // integrationService.updatePublishedComponents(integrationContext);

            try {
                integrationService.writeToolIntegrationFile(configurationMap, integrationContext);
            } catch (IOException e) {
                return Status.CANCEL_STATUS;
            }
            if (!integrationService.isToolIntegrated(configurationMap, integrationContext)) {
                integrationService.integrateTool(configurationMap, integrationContext);
            }

            // Despite its name, the method #getInitialComponentKnowledge returns the
            // component knowledge at the time of the
            // call, i.e., in this case, after the tool integration
            DistributedComponentKnowledge componentKnowledge = getInitialComponentKnowledge();
            final boolean existingComponentEntryPublished = componentKnowledge.getAllLocalInstallations().stream()
                .filter(entry -> {
                    // Filter the stream to contain only those ComponentEntry whose name matches the
                    // tool that we just integrated
                    final String toolName = (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
                    return entry.getComponentInterface().getDisplayName().equals(toolName);
                }).anyMatch(entry -> entry.getType().isRemotelyAccessible());

            if (existingComponentEntryPublished && !isEdit) {
                new UIJob("Show user warning for existing publication entries") {

                    @Override
                    public IStatus runInUIThread(IProgressMonitor arg0) {
                        MessageBox infoDialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                        infoDialog.setText("Tool updated");
                        infoDialog.setMessage(StringUtils.format(
                            "The tool \"%s\" is published due to existing publication entries. "
                                + "Please verify that this tool is actually meant to be published."
                                + "Otherwise unpublish the tool via the \"Component Publishing\" view.",
                            configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
                        infoDialog.open();
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }

            integrationService.setFileWatcherActive(true);
            integrationService.registerRecursive((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                integrationContext);

            new UIJob("Display success message to user") {

                @Override
                public IStatus runInUIThread(IProgressMonitor arg0) {
                    if (shell != null) {
                        MessageBox infoDialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                        if (!isEdit) {
                            infoDialog.setText("Tool integrated");
                            infoDialog.setMessage(
                                StringUtils.format("Tool \"%s\" was successfully integrated.",
                                    configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
                        } else {
                            infoDialog.setText("Tool updated");
                            infoDialog.setMessage(StringUtils.format("Tool \"%s\" was successfully updated.",
                                configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
                        }

                        infoDialog.open();
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();

            return Status.OK_STATUS;
        }

        private void handleIcon() {
            if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH) != null) {
                File icon = new File((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH));
                if (!icon.isAbsolute() && iconTarget != null) {
                    File toolConfigFile = new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                        integrationContext.getNameOfToolIntegrationDirectory() + File.separator
                            + integrationContext.getToolDirectoryPrefix()
                            + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
                    icon = new File(toolConfigFile, icon.getName());
                    try {
                        FileUtils.copyFile(iconTarget, icon);
                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(iconTarget);
                        icon = new File(icon.getName());
                    } catch (IOException e) {
                        LOGGER.error("Could not copy icon from temporary tool directory.", e);
                    }
                }

            }
        }
    }

    protected static ToolIntegrationService integrationService;

    private static final int MINIMUM_HEIGHT = 700;

    private static final int MINIMUM_WIDTH = 500;

    private static final Log LOGGER = LogFactory.getLog(ToolIntegrationWizard.class);

    protected ToolIntegrationContextRegistry integrationContextRegistry;

    // used to differ between integration and update context of wizard. In case
    // integrating and editing continues
    // to differ, separating the functionality into two wizards should be considered
    protected boolean isEdit;

    private Map<String, Object> configurationMap;

    private ToolCharacteristicsPage characteristicsPage;

    private InOutputConfigurationPage inOutputPage;

    private PropertyConfigurationPage propertyPage;

    private ToolConfigurationPage toolPage;

    private ScriptConfigurationPage scriptPage;

    private final ServiceRegistryAccess serviceRegistryAccess;

    private ChooseConfigurationPage chooseConfigurationPage;

    private ToolIntegrationContext integrationContext;

    private final String wizardType;

    private String integrationType;

    private final Map<String, List<ToolIntegrationWizardPage>> additionalPages = new HashMap<>();

    private List<ToolIntegrationWizardPage> currentAdditionalPages = null;

    private Map<String, Object> previousConfiguration;

    private File toolDocuTarget;

    private File iconTarget;

    private Optional<String> preselectedToolname = Optional.empty();

    /**
     * Constructor for Wizard.
     * 
     * @param type
     */
    public ToolIntegrationWizard(boolean progressMonitor, String type) {
        setNeedsProgressMonitor(progressMonitor);
        configurationMap = new HashMap<>();
        configurationMap.put(ToolIntegrationConstants.INTEGRATION_TYPE,
            ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_TYPE);
        setWindowTitle(Messages.wizardTitle);
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        integrationService = serviceRegistryAccess.getService(ToolIntegrationService.class);
        integrationContextRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        TrayDialog.setDialogHelpAvailable(true);
        wizardType = type;
        isEdit = false;
    }

    public ToolIntegrationWizard(boolean progressMonitor, String type, String preselectedToolname) {
        this(progressMonitor, type);
        this.preselectedToolname = Optional.of(preselectedToolname);
    }

    @Override
    public void addPages() {
        List<String> toolNames = new LinkedList<>();

        for (String id : integrationService.getIntegratedComponentIds()) {
            toolNames.add(
                integrationService.getToolConfiguration(id).get(ToolIntegrationConstants.KEY_TOOL_NAME).toString());
        }

        List<String> groupNames = new ArrayList<>();
        LogicalNodeId localNode = serviceRegistryAccess.getService(PlatformService.class)
            .getLocalDefaultLogicalNodeId();
        Collection<DistributedComponentEntry> installations = getInitialComponentKnowledge().getAllInstallations();
        installations = ComponentUtils.eliminateComponentInterfaceDuplicates(installations, localNode);
        for (DistributedComponentEntry ci : installations) {
            ComponentInterface componentInterface = ci.getComponentInterface();
            String toolID = componentInterface.getIdentifier();
            if (toolID.startsWith("de.rcenvironment.integration.common") || toolID.startsWith("de.rcenvironment.integration.cpacs")
                || toolID.startsWith("common") || toolID.startsWith("cpacs")) {
                List<String> groupList = getAllSubgroups(componentInterface.getGroupName());
                for (String groupName : groupList) {
                    if (!groupNames.contains(groupName)) {
                        groupNames.add(groupName);
                    }
                }
            }
        }

        Collections.sort(groupNames, String.CASE_INSENSITIVE_ORDER);

        // We explicitly and hackily exclude the workflow integration context from showing up in the wizard here until we have defined how
        // the user should integrate a workflow
        final Predicate<ToolIntegrationContext> isNotWorkflowIntegrationContext =
            (context -> !context.getClass().getCanonicalName()
                .equals("de.rcenvironment.core.component.integration.workflow.internal.WorkflowIntegrationContext"));

        final Collection<ToolIntegrationContext> integrationContextsToShow =
            integrationContextRegistry.getAllIntegrationContexts().stream()
                .filter(isNotWorkflowIntegrationContext)
                .collect(Collectors.toSet());

        chooseConfigurationPage = ChooseConfigurationPage.createWithoutPreselectedTool(Messages.chooseConfigPageTitle,
            integrationContextsToShow, this, wizardType);

        characteristicsPage = new ToolCharacteristicsPage(Messages.firstToolIntegrationPageTitle, configurationMap,
            toolNames, groupNames);

        inOutputPage = new InOutputConfigurationPage(Messages.inOuputPage, configurationMap);
        propertyPage = new PropertyConfigurationPage(Messages.propertyPage, configurationMap);
        toolPage = new ToolConfigurationPage(Messages.toolPage, configurationMap);
        scriptPage = new ScriptConfigurationPage(Messages.scriptPage, configurationMap);

        addPage(chooseConfigurationPage);
        addPage(characteristicsPage);
        addPage(inOutputPage);
        addPage(propertyPage);
        addPage(toolPage);
        addPage(scriptPage);

        this.getShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);

        IntegrationWizardPageContributorRegistry contributorRegistry = serviceRegistryAccess
            .getService(IntegrationWizardPageContributorRegistry.class);
        for (IntegrationWizardPageContributor contributor : contributorRegistry.getAllContributors()) {
            List<ToolIntegrationWizardPage> pages = contributor.getAdditionalPagesList(configurationMap);
            additionalPages.put(contributor.getType(), pages);
            for (ToolIntegrationWizardPage page : pages) {
                addPage(page);
            }
        }

    }

    private List<String> getAllSubgroups(String groupPath) {
        List<String> strList = new ArrayList<>();
        strList.add(groupPath);
        while (groupPath.contains("/")) {
            int i = groupPath.lastIndexOf("/");
            groupPath = groupPath.substring(0, i);
            strList.add(groupPath);
        }
        return strList;
    }

    private DistributedComponentKnowledge getInitialComponentKnowledge() {
        DistributedComponentKnowledgeService registry = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class);
        return registry.getCurrentSnapshot();
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        IWizardPage nextPage = null;
        if (currentAdditionalPages == null) {
            if (page.equals(toolPage)) {
                nextPage = scriptPage;
            } else if (page.equals(scriptPage)) {
                for (List<ToolIntegrationWizardPage> addPages : additionalPages.values()) {
                    for (ToolIntegrationWizardPage addPage : addPages) {
                        addPage.setPageComplete(true);
                    }
                }
                return null;
            } else {
                nextPage = super.getNextPage(page);
            }
        } else {
            if (page.equals(toolPage)) {
                nextPage = currentAdditionalPages.get(0);
            } else if (currentAdditionalPages.contains(page)
                && currentAdditionalPages.indexOf(page) < currentAdditionalPages.size() - 1) {
                nextPage = currentAdditionalPages.get(currentAdditionalPages.indexOf(page) + 1);
            } else if (currentAdditionalPages.contains(page)
                && currentAdditionalPages.indexOf(page) == currentAdditionalPages.size() - 1) {
                nextPage = scriptPage;
            } else if (page.equals(scriptPage)) {
                return null;
            } else {
                nextPage = super.getNextPage(page);
            }
        }

        if (nextPage != null) {
            ((ToolIntegrationWizardPage) nextPage).updatePage();
        }
        return nextPage;
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        IWizardPage previousPage = null;

        if (currentAdditionalPages == null) {
            if (page.equals(scriptPage)) {
                previousPage = toolPage;
            } else {
                previousPage = super.getPreviousPage(page);
            }
        } else {
            if (page.equals(scriptPage)) {
                previousPage = currentAdditionalPages.get(currentAdditionalPages.size() - 1);
            } else if (currentAdditionalPages.contains(page) && currentAdditionalPages.indexOf(page) > 0) {
                previousPage = currentAdditionalPages.get(currentAdditionalPages.indexOf(page) - 1);
            } else if (currentAdditionalPages.contains(page) && currentAdditionalPages.indexOf(page) == 0) {
                previousPage = scriptPage;
            } else if (page.equals(chooseConfigurationPage)) {
                return null;
            } else {
                previousPage = super.getPreviousPage(page);
            }
        }

        if (previousPage != null) {
            ((ToolIntegrationWizardPage) previousPage).updatePage();
        }
        return previousPage;
    }

    @Override
    public boolean performFinish() {

        // We ``cache'' the shell here while the wizard is still visible in order to be
        // able to display an alert box later on if an error
        // occurs during tool integration
        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        final String integrationJobTitle = StringUtils.format(Messages.integrateToolJobTitle,
            configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        new ToolIntegrationJob(integrationJobTitle, shell).schedule();

        return isConfigurationOk();
    }

    /**
     * Currently, this method only checks whether the configuration map exists, contains a non-null tool name, and contains non-null launch
     * settings. This may be extended in the future.
     * 
     * @return True if the current configuration map contains the minimal key-value mappings to describe a tool to be integrated.
     */
    public boolean isConfigurationOk() {
        if (configurationMap == null || configurationMap.isEmpty()) {
            return false;
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS) == null) {
            return false;
        }

        return configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) != null;
    }

    private void determineIntegrationContext() {
        if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null) {
            for (ToolIntegrationContext context : integrationContextRegistry.getAllIntegrationContexts()) {
                if (context.getContextType()
                    .equalsIgnoreCase(configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE).toString())) {
                    integrationContext = context;
                }
            }
        } else {
            integrationContext = integrationContextRegistry
                .getToolIntegrationContextById(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
        }
        if (integrationContext == null) {
            LOGGER.error("Found no integration context for integration type "
                + configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE));
        }
    }

    /**
     * Saves the wizards configuration to the given path.
     * 
     * @param folderPath where to store the configuration.
     */
    public void performSaveAs(String folderPath) {
        determineIntegrationContext();
        File toolConfigFile = new File(folderPath,
            integrationContext.getNameOfToolIntegrationDirectory() + File.separator
                + integrationContext.getToolDirectoryPrefix()
                + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        try {
            integrationService.writeToolIntegrationFileToSpecifiedFolder(folderPath, configurationMap,
                integrationContext);
            MessageBox infoDialog = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
            if (toolConfigFile.exists()) {
                infoDialog.setText("Tool saved");
                infoDialog.setMessage(
                    StringUtils.format("Successfully saved tool: %s\nLocation: " + toolConfigFile.getAbsolutePath(),
                        configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
            } else {
                infoDialog.setText("Saving failed");
                infoDialog.setMessage(StringUtils.format(
                    "Could not save tool: %s\nLocation tried: " + toolConfigFile.getAbsolutePath(),
                    configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
            }
            infoDialog.open();
        } catch (IOException e) {
            MessageBox errorDialog = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            errorDialog.setText("Saving failed");
            errorDialog
                .setMessage(
                    StringUtils.format(
                        "Failed to save tool configuration to: " + toolConfigFile.getAbsolutePath()
                            + "\nCause: " + e.getMessage(),
                        configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
            errorDialog.open();

        }
    }

    protected void bindIntegrationService(ToolIntegrationService newIntegrationService) {
        integrationService = newIntegrationService;
    }

    protected void bindIntegrationInformationRegistry(
        ToolIntegrationContextRegistry incIntegrationInformationRegistry) {
        integrationContextRegistry = incIntegrationInformationRegistry;
    }

    /**
     * Sets a new configurationMap and updates all pages.
     * 
     * @param newConfigurationMap new map
     */
    public void setConfigurationMap(Map<String, Object> newConfigurationMap) {
        configurationMap = newConfigurationMap;
        determineIntegrationContext();
        for (IWizardPage page : this.getPages()) {
            ((ToolIntegrationWizardPage) page).setConfigMap(configurationMap);
        }
    }

    /**
     * Setter for the configuration map to edit.
     * 
     * @param newPreviousConfiguration loaded map
     * @param configJson where the config came from
     */
    public void setPreviousConfiguration(Map<String, Object> newPreviousConfiguration, File configJson) {
        Map<String, Object> configurationMapCopy = new HashMap<>();
        previousConfiguration = new HashMap<>(newPreviousConfiguration);
        if (newPreviousConfiguration != null) {
            configurationMapCopy.putAll(newPreviousConfiguration);
        }
        setConfigurationMap(configurationMapCopy);
        if (configJson != null) {
            if (newPreviousConfiguration != null
                && newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null) {
                for (ToolIntegrationContext context : integrationContextRegistry.getAllIntegrationContexts()) {
                    if (newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE).toString()
                        .equalsIgnoreCase(context.getContextType())) {
                        integrationContext = context;
                    }
                }
            } else {
                integrationContext = integrationContextRegistry
                    .getToolIntegrationContextById(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
            }
        }

        updateAllPages();
        for (Entry<String, List<ToolIntegrationWizardPage>> addPages : additionalPages.entrySet()) {
            if (newPreviousConfiguration != null && !addPages.getKey()
                .equals(newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE))) {
                for (ToolIntegrationWizardPage page : addPages.getValue()) {
                    page.setPageComplete(true);
                }
            }
        }
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);
        this.preselectedToolname.ifPresent(toolname -> {
            this.chooseConfigurationPage.selectToolConfiguration(toolname);
        });
    }

    /**
     * Updated all pages registered in the wizard if a configuration changes.
     */
    public void updateAllPages() {
        for (IWizardPage page : getPages()) {
            boolean isAdditionalPage = false;
            for (Collection<ToolIntegrationWizardPage> additionalPageList : additionalPages.values()) {
                if (additionalPageList.contains(page)) {
                    isAdditionalPage = true;
                }
            }
            if (!isAdditionalPage || (currentAdditionalPages != null && currentAdditionalPages.contains(page))) {
                ((ToolIntegrationWizardPage) page).updatePage();
            }
        }
        getContainer().updateButtons();
    }

    /**
     * Removes old integrated component for updating the new one.
     * 
     */

    public void removeOldIntegration() {
        integrationService.setFileWatcherActive(false);
        String previousToolName = "";
        String toolName = (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
        File previousToolDir = null;
        if (previousConfiguration != null) {
            previousToolName = (String) previousConfiguration.get(ToolIntegrationConstants.KEY_TOOL_NAME);
            previousToolDir = new File(new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                integrationContext.getNameOfToolIntegrationDirectory()), previousToolName);
        } else {
            previousToolName = toolName;
        }
        if (previousToolDir != null) {
            storeIconAndDocuInTempDir(previousToolDir);
        }
        integrationService.unregisterIntegration(previousToolName, integrationContext);
        integrationService.removeTool(previousToolName, integrationContext);
        if (!previousToolName.equals(toolName)) {

            File delete = previousToolDir;
            try {
                FileUtils.forceDelete(delete);
            } catch (IOException e) {
                LogFactory.getLog(ToolIntegrationWizard.class).error(e);
            }
        } else {
            if (configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH) == null
                || ((String) configurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH)).isEmpty()) {
                File docDir = new File(previousToolDir, ToolIntegrationConstants.DOCS_DIR_NAME);
                if (docDir.listFiles() != null) {
                    for (File f : docDir.listFiles()) {
                        try {
                            FileUtils.forceDelete(f);
                        } catch (IOException e) {
                            LOGGER.error("Could not delete file in tool docs directory: ", e);
                        }
                    }
                }
            }
        }
        integrationService.setFileWatcherActive(true);
    }

    private void storeIconAndDocuInTempDir(File previousToolDir) {
        try {
            File tempToolDir = TempFileServiceAccess.getInstance().createManagedTempDir();
            toolDocuTarget = null;
            if (new File(previousToolDir, ToolIntegrationConstants.DOCS_DIR_NAME).exists()) {
                toolDocuTarget = new File(tempToolDir, ToolIntegrationConstants.DOCS_DIR_NAME);
                FileUtils.copyDirectory(new File(previousToolDir, ToolIntegrationConstants.DOCS_DIR_NAME),
                    toolDocuTarget);
            }

            iconTarget = null;
            String iconPath = (String) previousConfiguration.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH);
            if (iconPath != null && !iconPath.isEmpty() && !new File(iconPath).isAbsolute()
                && new File(previousToolDir, iconPath).exists()) {
                iconTarget = new File(tempToolDir, iconPath);
                FileUtils.copyFile(new File(previousToolDir, iconPath), iconTarget);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not create temporiary tool dir.", e);
        }

    }

    public String getIntegrationType() {
        return integrationType;
    }

    /**
     * Sets the new integration type and creates a new Map.
     * 
     * @param incIntegrationType to set
     * @param template if the new type comes from a template
     */
    public void setIntegrationType(String incIntegrationType, boolean template) {
        this.integrationType = incIntegrationType;
        determineIntegrationContext();
    }

    /**
     * Removes all pages that were added through an extension.
     */
    public void removeAdditionalPages() {
        currentAdditionalPages = null;
    }

    /**
     * Sets the pages that are needed for the given context.
     * 
     * @param contextType that needs additional pages
     */
    public void setAdditionalPages(String contextType) {
        currentAdditionalPages = additionalPages.get(contextType);
    }

    public Map<String, Object> getConfigurationMap() {
        return configurationMap;
    }

    /**
     * Returns the current {@link ToolIntegrationContext} or, if not yet set, the common version of it.
     * 
     * @return current {@link ToolIntegrationContext}
     */
    public ToolIntegrationContext getCurrentContext() {
        if (integrationContext == null) {
            return integrationContextRegistry
                .getToolIntegrationContextById(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
        }
        return integrationContext;
    }

    /**
     * 
     */
    public void open() {
        chooseConfigurationPage.updatePage();
    }

    protected void setIsEdit(boolean isEditNew) {
        this.isEdit = isEditNew;
    }
}
