/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.dialogs.WorkbenchWizardElement;
import org.eclipse.ui.internal.wizards.AbstractExtensionWizardRegistry;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.wizards.IWizardCategory;
import org.eclipse.ui.wizards.IWizardDescriptor;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.utils.incubator.ContextMenuItemRemover;
import de.rcenvironment.core.start.Application;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * This class advises the creation of the workbench of the {@link Application}.
 * 
 * @author Christian Weiss
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

    private static final int MINIMUM_HEIGHT = 250;

    private static final int MINIMUM_WIDTH = 500;

    private static final Log LOGGER = LogFactory.getLog(ApplicationWorkbenchAdvisor.class);

    private static ConfigurationService configService;

    private static String windowTitle = "%s (%s)";

    public ApplicationWorkbenchAdvisor() {}

    protected void bindConfigurationService(final ConfigurationService configServiceIn) {
        configService = configServiceIn;
    }

    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        IWorkbenchWindowConfigurer windowConfigurer = configurer;
        String platformName = configService.getInstanceName();
        if (platformName != null) {
            windowConfigurer.setTitle(StringUtils.format(windowTitle, windowConfigurer.getTitle(), platformName));
        }
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    @Override
    public String getInitialWindowPerspectiveId() {
        return "de.rcenvironment.rce";
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer) {
        super.initialize(configurer);
        configurer.setSaveAndRestore(true);
        WorkbenchAdvisorDelegate.declareWorkbenchImages(getWorkbenchConfigurer());
    }

    @Override
    public void preStartup() {
        super.preStartup();
        // required to be able to use the Resource view
        IDE.registerAdapters();
    }

    @Override
    public void postStartup() {
        super.postStartup();
        // For 6.2.0, the NPE which occurs sometimes on Linux is just catched (see Mantis issue 12230).
        // After 6.2.0 the cause of the NPE should be investigated (see Mantis issue 0012243) and this code improved. - seid_do, June 2015
        try {
            Display.getDefault().getActiveShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        } catch (NullPointerException e) { 
            LOGGER.warn("Failed to set the minimum size of  RCE's main window");
        }
        // refreshes the Resource Explorer, otherwise projects will not be shown
        IWorkbenchWindow[] workbenchs =
            PlatformUI.getWorkbench().getWorkbenchWindows();
        ProjectExplorer view = null;
        for (IWorkbenchWindow workbench : workbenchs) {
            for (IWorkbenchPage page : workbench.getPages()) {
                view = (ProjectExplorer)
                    page.findView("org.eclipse.ui.navigator.ProjectExplorer");
                break;
            }
        }
        if (view == null) {
            return;
        }
        view.getCommonViewer().setInput(ResourcesPlugin.getWorkspace().getRoot());

        // remove unwanted menu entries from project explorer's context menu
        ContextMenuItemRemover.removeUnwantedMenuEntries(view.getCommonViewer().getControl());

        removeUnwantedNewWizards();

    }

    private void removeUnwantedNewWizards() {

        final Properties unwanted = new Properties();
        try {
            unwanted.load(ApplicationActionBarAdvisor.class.getResourceAsStream("unwanted.properties"));
        } catch (IOException e) {
            LOGGER.error("Failed to remove unwanted elements from UI:", e);
        }

        // remove unwanted action sets
        final String unwantedNewWizardIdsProperty = unwanted.getProperty("unwantedNewWizards");
        if (unwantedNewWizardIdsProperty != null && !unwantedNewWizardIdsProperty.trim().isEmpty()) {
            List<String> unwantedNewWizardIds = Arrays.asList(unwantedNewWizardIdsProperty.split(","));
            IWizardCategory[] categories = WorkbenchPlugin.getDefault().getNewWizardRegistry().getRootCategory().getCategories();
            for (IWizardDescriptor wizard : getAllWizards(categories)) {
                WorkbenchWizardElement wizardElement = (WorkbenchWizardElement) wizard;
                if (unwantedNewWizardIds.contains(wizardElement.getId())) {
                    ((AbstractExtensionWizardRegistry) WorkbenchPlugin.getDefault().getNewWizardRegistry()).
                    removeExtension(wizardElement.getConfigurationElement().getDeclaringExtension(), new Object[] { wizardElement });
                }
            }
        }
    }

    private IWizardDescriptor[] getAllWizards(IWizardCategory... categories) {
        List<IWizardDescriptor> results = new ArrayList<IWizardDescriptor>();
        for (IWizardCategory wizardCategory : categories) {
            results.addAll(Arrays.asList(wizardCategory.getWizards()));
            results.addAll(Arrays.asList(getAllWizards(wizardCategory.getCategories())));
        }
        return results.toArray(new IWizardDescriptor[0]);
    }

}
