/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.start.gui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * This class advises the creation of windows of the {@link Application}.
 * 
 * @author Christian Weiss
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {


    private static final Log LOGGER = LogFactory.getLog(ApplicationWorkbenchWindowAdvisor.class);

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }
    
    @Override
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }

    @Override
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowPerspectiveBar(true);
        configurer.setShowProgressIndicator(true);
        IPreferenceStore preferenceStore = PlatformUI.getPreferenceStore();
        preferenceStore.setValue(IWorkbenchPreferenceConstants.DOCK_PERSPECTIVE_BAR, "TOP_RIGHT");
    }
    
    @Override
    public void postWindowCreate() {
        super.postWindowCreate();
        //Check, if workspace needs to be migrated from e3 version.
        //In this case, previously opened editor tabs needs to be closed programmatically.
        String workbenchNode = "org.eclipse.ui.workbench";
        String mirgratedPreferenceFlag = "e4Workbench";
        if (!(InstanceScope.INSTANCE.getNode(workbenchNode).getBoolean(mirgratedPreferenceFlag, false))) {
            IPath location = Platform.getLocation();
            IPath workbenchXml = location.addTrailingSeparator().append(".metadata").addTrailingSeparator().append(".plugins")
                .addTrailingSeparator().append(workbenchNode).addTrailingSeparator().append("workbench.xml");
            if (workbenchXml.toFile().exists()) {
                IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
                configurer.getWindow().getActivePage().closeAllEditors(true);
                workbenchXml.toFile().delete();
                LOGGER.debug("'Workbench.xml' file found and deleted. Workbench migrated to e4 platform. "
                    + "Closed editor tabs previously opened with e3 platform.");
            }
            InstanceScope.INSTANCE.getNode(workbenchNode).putBoolean(mirgratedPreferenceFlag, true);
        }
    }
    
    @Override
    public boolean preWindowShellClose() {
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.save(true, null);
        } catch (CoreException e) {
            // swallow
            @SuppressWarnings("unused")
            int i = 0;
        } catch (RuntimeException e) {
            // swallow
            @SuppressWarnings("unused")
            int i = 0;
        }
        return super.preWindowShellClose();
    }
}
