/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.integration.IntegrationWatcher;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributor;
import de.rcenvironment.core.gui.wizards.toolintegration.api.IntegrationWizardPageContributorRegistry;
import de.rcenvironment.core.gui.wizards.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Wizard for integrating a new external tool.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationWizard extends Wizard {

    protected static ToolIntegrationService integrationService;

    protected ToolIntegrationContextRegistry integrationContextRegistry;

    private Map<String, Object> configurationMap;

    private ToolCharacteristicsPage characteristicsPage;

    private InOutputConfigurationPage inOutputPage;

    private PropertyConfigurationPage propertyPage;

    private ToolConfigurationPage toolPage;

    private ScriptConfigurationPage scriptPage;

    private final ServiceRegistryAccess serviceRegistryAccess;

    private Map<String, Object> previousConfiguration;

    private ChooseConfigurationPage editConfigurationPage;

    private ToolIntegrationContext integrationContext;

    private final String wizardType;

    private String integrationType;

    private final Map<String, List<ToolIntegrationWizardPage>> additionalPages = new HashMap<String, List<ToolIntegrationWizardPage>>();

    private List<ToolIntegrationWizardPage> currentAdditionalPages = null;

    /**
     * Constructor for Wizard.
     * 
     * @param type
     */
    public ToolIntegrationWizard(boolean progressMonitor, String type) {
        setNeedsProgressMonitor(progressMonitor);
        configurationMap = new HashMap<String, Object>();
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
        List<String> toolNames = new LinkedList<String>();

        for (String id : integrationService.getIntegratedComponentIds()) {
            toolNames.add(id.substring(id.lastIndexOf(".") + 1));
        }

        List<String> groupNames = new ArrayList<String>();
        NodeIdentifier localNode = serviceRegistryAccess.getService(PlatformService.class).getLocalNodeId();
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
        Job job = new Job(String.format(Messages.integrateToolJobTitle, configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME))) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IntegrationWatcher.setWatcherActive(false);
                determineIntegrationContext();
                configurationMap.put(ToolIntegrationConstants.IS_ACTIVE, true);
                if (configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH) != null) {
                    File icon = new File((String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH));
                    if (!icon.isAbsolute()) {
                        File toolConfigFile =
                            new File(integrationContext.getRootPathToToolIntegrationDirectory(),
                                integrationContext.getNameOfToolIntegrationDirectory()
                                    + File.separator
                                    + integrationContext.getToolDirectoryPrefix()
                                    + configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME));
                        icon = new File(toolConfigFile, icon.getName());
                    }
                    if (icon.exists() && icon.isFile()) {
                        configurationMap.put(ToolIntegrationConstants.KEY_TOOL_ICON_PATH, icon.getAbsolutePath());
                    }
                }

                Boolean publish = (Boolean) configurationMap.get(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
                String toolPath = integrationContext.getRootPathToToolIntegrationDirectory() + File.separator
                    + integrationContext.getNameOfToolIntegrationDirectory() + File.separator
                    + integrationContext.getToolDirectoryPrefix()
                    + (String) configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
                if (publish != null && publish) {
                    integrationService.addPublishedTool(toolPath);
                    configurationMap.remove(ToolIntegrationConstants.TEMP_KEY_PUBLISH_COMPONENT);
                } else {
                    integrationService.unpublishTool(toolPath);
                }
                integrationService.savePublishedComponents(integrationContext);
                integrationService.updatePublishedComponents(integrationContext);

                integrationService.writeToolIntegrationFile(configurationMap, integrationContext);
                if (!integrationService.isToolIntegrated(configurationMap, integrationContext)) {
                    integrationService.integrateTool(configurationMap, integrationContext);
                }
                IntegrationWatcher.setWatcherActive(true);
                return Status.OK_STATUS;
            }

        };

        job.schedule();

        return true;
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
        integrationService.writeToolIntegrationFileToSpecifiedFolder(folderPath, configurationMap, integrationContext);
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
            ((ToolIntegrationWizardPage) page).setConfigMap(newConfigurationMap);
        }

    }

    public Map<String, Object> getPreviousConfiguration() {
        return previousConfiguration;
    }

    /**
     * Setter for the configuration map to edit.
     * 
     * @param newPreviousConfiguration loaded map
     * @param configJson where the config came from
     */
    public void setPreviousConfiguration(Map<String, Object> newPreviousConfiguration, File configJson) {
        this.previousConfiguration = newPreviousConfiguration;
        Map<String, Object> configurationMapCopy = new HashMap<String, Object>();
        if (newPreviousConfiguration != null) {
            configurationMapCopy.putAll(newPreviousConfiguration);
        }
        setConfigurationMap(configurationMapCopy);
        if (configJson != null) {
            if (newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE) != null) {
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
            if (addPages.getKey() != newPreviousConfiguration.get(ToolIntegrationConstants.INTEGRATION_TYPE)) {
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
        integrationService.removeTool((String) previousConfiguration.get(ToolIntegrationConstants.KEY_TOOL_NAME),
            integrationContext);
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
        if (incIntegrationType.equals(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_TYPE) && !template) {
            configurationMap = new HashMap<String, Object>();
        }
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
     * Returns the current {@link ToolIntegrationContext} or, if not yet set, the common version of
     * it.
     * 
     * @return current {@link ToolIntegrationContext}
     */
    public ToolIntegrationContext getCurrentContext() {
        if (integrationContext == null) {
            return integrationContextRegistry.getToolIntegrationContext(ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID);
        }
        return integrationContext;
    }
}
