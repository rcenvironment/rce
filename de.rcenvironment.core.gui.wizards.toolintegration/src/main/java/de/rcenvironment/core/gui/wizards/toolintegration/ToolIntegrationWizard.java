/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import org.eclipse.swt.widgets.MessageBox;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
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
 */
public class ToolIntegrationWizard extends Wizard {

    protected static ToolIntegrationService integrationService;

    private static final int MINIMUM_HEIGHT = 700;

    private static final int MINIMUM_WIDTH = 500;

    private static final Log LOGGER = LogFactory.getLog(ToolIntegrationWizard.class);

    protected ToolIntegrationContextRegistry integrationContextRegistry;

    private Map<String, Object> configurationMap;

    private ToolCharacteristicsPage characteristicsPage;

    private InOutputConfigurationPage inOutputPage;

    private PropertyConfigurationPage propertyPage;

    private ToolConfigurationPage toolPage;

    private ScriptConfigurationPage scriptPage;

    private final ServiceRegistryAccess serviceRegistryAccess;

    private ChooseConfigurationPage editConfigurationPage;

    private ToolIntegrationContext integrationContext;

    private final String wizardType;

    private String integrationType;

    private final Map<String, List<ToolIntegrationWizardPage>> additionalPages = new HashMap<>();

    private List<ToolIntegrationWizardPage> currentAdditionalPages = null;

    private boolean configOK;

    private Map<String, Object> previousConfiguration;

    private File toolDocuTarget;

    private File iconTarget;

    /**
     * Constructor for Wizard.
     * 
     * @param type
     */
    public ToolIntegrationWizard(boolean progressMonitor, String type) {
        setNeedsProgressMonitor(progressMonitor);
        configurationMap = new HashMap<>();
        configurationMap.put(ToolIntegrationConstants.INTEGRATION_TYPE, ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_TYPE);
        setWindowTitle(Messages.wizardTitle);
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        integrationService = serviceRegistryAccess.getService(ToolIntegrationService.class);
        integrationContextRegistry = serviceRegistryAccess.getService(ToolIntegrationContextRegistry.class);
        TrayDialog.setDialogHelpAvailable(true);
        wizardType = type;
    }

    @Override
    public void addPages() {
        List<String> toolNames = new LinkedList<>();

        for (String id : integrationService.getIntegratedComponentIds()) {
            toolNames.add(id.substring(id.lastIndexOf(".") + 1));
        }

        List<String> groupNames = new ArrayList<>();
        LogicalNodeId localNode = serviceRegistryAccess.getService(PlatformService.class).getLocalDefaultLogicalNodeId();
        Collection<ComponentInstallation> installations = getInitialComponentKnowledge().getAllInstallations();
        installations = ComponentUtils.eliminateComponentInterfaceDuplicates(installations, localNode);
        for (ComponentInstallation ci : installations) {
            ComponentInterface componentInterface = ci.getComponentRevision().getComponentInterface();
            String groupName = componentInterface.getGroupName();
            if (!groupName.startsWith("_") && !groupNames.contains(groupName)) {
                groupNames.add(groupName);
            }
        }

        Collections.sort(groupNames, String.CASE_INSENSITIVE_ORDER);

        editConfigurationPage =
            new ChooseConfigurationPage(Messages.chooseConfigPageTitle, integrationContextRegistry.getAllIntegrationContexts(), this,
                wizardType);

        characteristicsPage =
            new ToolCharacteristicsPage(Messages.firstToolIntegrationPageTitle, configurationMap, toolNames, groupNames);

        inOutputPage = new InOutputConfigurationPage(Messages.inOuputPage, configurationMap);
        propertyPage = new PropertyConfigurationPage(Messages.propertyPage, configurationMap);
        toolPage = new ToolConfigurationPage(Messages.toolPage, configurationMap);
        scriptPage = new ScriptConfigurationPage(Messages.scriptPage, configurationMap);

        addPage(editConfigurationPage);
        addPage(characteristicsPage);
        addPage(inOutputPage);
        addPage(propertyPage);
        addPage(toolPage);
        addPage(scriptPage);

        this.getShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);

        IntegrationWizardPageContributorRegistry contributorRegistry =
            serviceRegistryAccess.getService(IntegrationWizardPageContributorRegistry.class);
        for (IntegrationWizardPageContributor contributor : contributorRegistry.getAllContributors()) {
            List<ToolIntegrationWizardPage> pages = contributor.getAdditionalPagesList(configurationMap);
            additionalPages.put(contributor.getType(), pages);
            for (ToolIntegrationWizardPage page : pages) {
                addPage(page);
            }
        }

    }

    private DistributedComponentKnowledge getInitialComponentKnowledge() {
        DistributedComponentKnowledgeService registry = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class);
        return registry.getCurrentComponentKnowledge();
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        IWizardPage nextPage = null;
        if (currentAdditionalPages == null) {
            if (page.equals(toolPage)) {
                nextPage = scriptPage;
            } else if (page.equals(scriptPage)) {
                for (String key : additionalPages.keySet()) {
                    for (ToolIntegrationWizardPage addPage : additionalPages.get(key)) {
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
            } else if (currentAdditionalPages.contains(page) && currentAdditionalPages.indexOf(page) < currentAdditionalPages.size() - 1) {
                nextPage = currentAdditionalPages.get(currentAdditionalPages.indexOf(page) + 1);
            } else if (currentAdditionalPages.contains(page) && currentAdditionalPages.indexOf(page) == currentAdditionalPages.size() - 1) {
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
            } else if (page.equals(editConfigurationPage)) {
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

        Job job =
            new Job(StringUtils.format(Messages.integrateToolJobTitle, configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME))) {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    if (checkConfiguration(configurationMap)) {
                        determineIntegrationContext();
                        integrationService.setFileWatcherActive(false);
                        configurationMap.put(ToolIntegrationConstants.IS_ACTIVE, true);
                        handleIcon();

                        Boolean publish = (Boolean) configurationMap.get(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
                        File toolPath = new File(new File(new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                            integrationContext.getNameOfToolIntegrationDirectory()), integrationContext.getToolDirectoryPrefix()),
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
                        if (publish != null && publish) {
                            integrationService.addPublishedTool(toolPath.getAbsolutePath());
                            configurationMap.remove(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
                        } else {
                            integrationService.unpublishTool(toolPath.getAbsolutePath());
                        }
                        integrationService.savePublishedComponents(integrationContext);
                        integrationService.updatePublishedComponents(integrationContext);

                        try {
                            integrationService.writeToolIntegrationFile(configurationMap, integrationContext);
                        } catch (IOException e) {
                            return Status.CANCEL_STATUS;
                        }
                        if (!integrationService.isToolIntegrated(configurationMap, integrationContext)) {
                            integrationService.integrateTool(configurationMap, integrationContext);
                        }
                        integrationService.setFileWatcherActive(true);
                        integrationService.registerRecursive((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME),
                            integrationContext);

                        return Status.OK_STATUS;
                    }
                    return Status.CANCEL_STATUS;
                }

                private void handleIcon() {
                    if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH) != null) {
                        File icon = new File((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH));
                        if (!icon.isAbsolute() && iconTarget != null) {
                            File toolConfigFile =
                                new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                                    integrationContext.getNameOfToolIntegrationDirectory()
                                        + File.separator
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

            };

        job.schedule();

        return checkConfiguration(configurationMap);
    }

    private boolean checkConfiguration(Map<String, Object> configMap) {
        configOK = true;

        if (configMap == null || configMap.isEmpty()) {
            configOK = false;
            if (configOK) {
                if (configMap == null || configMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS) == null) {
                    configOK = false;
                }
                if (configMap == null || configMap.get(ToolIntegrationConstants.KEY_TOOL_NAME) == null) {
                    configOK = false;
                }
            }
        }
        return configOK;
    }

    public boolean isConfigOK() {
        return configOK;
    }

    private void determineIntegrationContext() {
        if (configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null) {
            for (ToolIntegrationContext context : integrationContextRegistry.getAllIntegrationContexts()) {
                if (context.getContextType().equals(configurationMap.get(ToolIntegrationConstants.INTEGRATION_TYPE))) {
                    integrationContext = context;
                }
            }
        } else {
            integrationContext =
                integrationContextRegistry.getToolIntegrationContext(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
        }
    }

    /**
     * Saves the wizards configuration to the given path.
     * 
     * @param folderPath where to store the configuration.
     */
    public void performSaveAs(String folderPath) {
        determineIntegrationContext();
        File toolConfigFile =
            new File(folderPath, integrationContext.getNameOfToolIntegrationDirectory() + File.separator
                + integrationContext.getToolDirectoryPrefix() + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
        try {
            integrationService.writeToolIntegrationFileToSpecifiedFolder(folderPath, configurationMap, integrationContext);
            MessageBox infoDialog = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
            if (toolConfigFile.exists()) {
                infoDialog.setText("Tool saved");
                infoDialog
                    .setMessage(StringUtils.format("Successfully saved tool: %s\nLocation: " + toolConfigFile.getAbsolutePath(),
                        configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
            } else {
                infoDialog.setText("Saving failed");
                infoDialog
                    .setMessage(StringUtils.format("Could not save tool: %s\nLocation tried: " + toolConfigFile.getAbsolutePath(),
                        configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
            }
            infoDialog.open();
        } catch (IOException e) {
            MessageBox errorDialog = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            errorDialog.setText("Saving failed");
            errorDialog.setMessage(
                StringUtils.format(
                    "Failed to save tool configuration to: " + toolConfigFile.getAbsolutePath() + "\nCause: " + e.getMessage(),
                    configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME)));
            errorDialog.open();

        }
    }

    protected void bindIntegrationService(ToolIntegrationService newIntegrationService) {
        integrationService = newIntegrationService;
    }

    protected void bindIntegrationInformationRegistry(ToolIntegrationContextRegistry incIntegrationInformationRegistry) {
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
            if (newPreviousConfiguration != null && newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null) {
                for (ToolIntegrationContext context : integrationContextRegistry.getAllIntegrationContexts()) {
                    if (newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE).equals(context.getContextType())) {
                        integrationContext = context;
                    }
                }
            } else {
                integrationContext =
                    integrationContextRegistry.getToolIntegrationContext(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
            }
        }

        updateAllPages();
        for (Entry<String, List<ToolIntegrationWizardPage>> addPages : additionalPages.entrySet()) {
            if (newPreviousConfiguration != null
                && !addPages.getKey().equals(newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE))) {
                for (ToolIntegrationWizardPage page : addPages.getValue()) {
                    page.setPageComplete(true);
                }
            }
        }
    }

    /**
     * Updated all pages registered in the wizard if a configuration changes.
     */
    public void updateAllPages() {
        for (IWizardPage page : getPages()) {
            boolean isAdditionalPage = false;
            for (String key : additionalPages.keySet()) {
                if (additionalPages.get(key).contains(page)) {
                    isAdditionalPage = true;
                }
            }
            if (!isAdditionalPage || (isAdditionalPage && currentAdditionalPages != null && currentAdditionalPages.contains(page))) {
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
                FileUtils.copyDirectory(new File(previousToolDir, ToolIntegrationConstants.DOCS_DIR_NAME), toolDocuTarget);
            }

            iconTarget = null;
            String iconPath = (String) previousConfiguration.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH);
            if (iconPath != null && !iconPath.isEmpty() && !new File(iconPath).isAbsolute()) {
                if (new File(previousToolDir, iconPath).exists()) {
                    iconTarget = new File(tempToolDir, iconPath);
                    FileUtils.copyFile(new File(previousToolDir, iconPath), iconTarget);
                }
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
            return integrationContextRegistry.getToolIntegrationContext(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
        }
        return integrationContext;
    }

    /**
     * 
     */
    public void open() {
        editConfigurationPage.updatePage();
    }
}
