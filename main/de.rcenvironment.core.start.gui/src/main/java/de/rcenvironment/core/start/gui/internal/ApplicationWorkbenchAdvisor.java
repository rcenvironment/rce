/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.utils.incubator.ContextMenuItemRemover;
import de.rcenvironment.core.start.common.Platform;
import de.rcenvironment.core.start.common.validation.PlatformValidationManager;

/**
 * This class advises the creation of the workbench of the {@link Application}.
 * 
 * @author Christian Weiss
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

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
            windowConfigurer.setTitle(String.format(windowTitle, windowConfigurer.getTitle(), platformName));
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
        // validate the platform and exit if not valid
        if (!(new PlatformValidationManager()).validate(false)) {
            Platform.shutdown();
            // TODO use a shutdown solution that does not cause a meaningless stacktrace - misc_ro, April 2014
            throw new RuntimeException("RCE startup validation failed");
        }
        // required to be able to use the Resource view
        IDE.registerAdapters();
    }

    @Override
    public void postStartup() {
        super.postStartup();
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
    }
}
