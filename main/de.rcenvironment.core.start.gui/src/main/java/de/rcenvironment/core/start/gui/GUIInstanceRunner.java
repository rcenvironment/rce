/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.start.gui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.osgi.framework.Version;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.gui.internal.ApplicationWorkbenchAdvisor;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.core.utils.common.concurrent.ThreadGuard;

/**
 * Starts the GUI for RCE.
 * 
 * @author Sascha Zur
 * @author Jan Flink
 * @author Robert Mischke
 * @author Marc Stammerjohann
 * @author Doreen Seider
 */
@SuppressWarnings("restriction")
public final class GUIInstanceRunner extends InstanceRunner {

    private static final int MAX_RECENT_WORKSPACES_HISTORY = 5;

    private static final boolean ALLOW_WORKSPACE_CHOOSER_SUPPRESSION = true;

    private static boolean tryWorkspaceChoosingAgain = false;

    /**
     * Runs the RCE instance in non-headless (GUI) mode.
     * 
     * @return exit code
     * @throws Exception : URL
     */
    @Override
    public int performRun() throws Exception {
        
        int exitCode = 0 - 1;
        
        // trigger execution of "--exec" commands, if they exist
        String[] execCommandTokens = CommandLineArguments.getExecCommandTokens();
        if (execCommandTokens != null) {
            initiateAsyncCommandExecution(execCommandTokens, "execution of --exec commands", false);
        }
        
        storeVersionInformationToSystemProperties();
        
        // mark the GUI thread as forbidden (or at least, critical) for certain operations - misc_ro
        ThreadGuard.setForbiddenThread(Thread.currentThread());
        
        // initialize the GUI
        Display display = PlatformUI.createDisplay();

        // start the workbench - returns as soon as the workbench is closed
        try {
            do {
                // workspace location chooser
                if (!determineWorkspaceLocation(Platform.getInstanceLocation())) {
                    return IApplication.EXIT_OK;
                }
                // If this flag is true, a non-valid workspace was chosen, show the workspace chooser again until a valid workspace is
                // selected or "cancel" is clicked.
            } while (tryWorkspaceChoosingAgain);
            
            int platformUIExitCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
            if (platformUIExitCode == PlatformUI.RETURN_RESTART) {
                exitCode = IApplication.EXIT_RESTART;
            } else {
                exitCode = IApplication.EXIT_OK;
            }
        } finally {
            display.dispose();
        }
        return exitCode;
    }
    
    @Override
    public boolean onInstanceValidationFailures(Map<InstanceValidationResultType, List<InstanceValidationResult>> validationResults) {
        
        if (validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED).size() > 0) {
            InstanceValidationResult result = validationResults.get(InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED).get(0);
            showErrorDialog("Instance validation failure", result.getGuiDialogMessage() + "\n\nRCE will be shutdown.");
            return false;
        }
        
        if (validationResults.get(InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED).size() > 0) {
            for (InstanceValidationResult result : validationResults.get(InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED)) {
                if (!showQuestionDialog("Instance validation failure", result.getGuiDialogMessage()
                    + "\n\nDo you like to proceed anyway?")) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void beforeAwaitShutdown() {
        PlatformUI.getWorkbench();
    }

    @Override
    public void triggerShutdown() {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (!PlatformUI.isWorkbenchRunning()) {
                    return;
                }
                PlatformUI.getWorkbench().close();
            }
        });
    }

    @Override
    public void triggerRestart() {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (!PlatformUI.isWorkbenchRunning()) {
                    return;
                }
                PlatformUI.getWorkbench().restart();
            }
        });
    }
    
    

    private boolean determineWorkspaceLocation(Location workspaceLocation) throws MalformedURLException, IOException {

        // if a workspace was defined using the Eclipse "-data" option, use it as an override and don't use our own mechanism
        if (workspaceLocation.isSet()) {
            log.info("Not using the workspace chooser or locations stored in the RCE profile, "
                + "as a workspace location was already defined (probably using the -data option): " + workspaceLocation.getURL());
            return true;
        }

        WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance();
        File profileDirectory = BootstrapConfiguration.getInstance().getProfileDirectory();

        String lastWorkspaceLocation = workspaceSettings.getLastLocation();
        String recentWorkspacesData = workspaceSettings.getRecentLocationData();

        String defaultWorkdirPath;
        if (lastWorkspaceLocation != null) {
            defaultWorkdirPath = new File(lastWorkspaceLocation).getAbsolutePath();
        } else {
            defaultWorkdirPath = new File(profileDirectory, "workspace").getAbsolutePath();
        }
        String[] oldRecentWorkspaces;
        if (recentWorkspacesData != null) {
            oldRecentWorkspaces = StringUtils.splitAndUnescape(recentWorkspacesData);
        } else {
            oldRecentWorkspaces = new String[] { defaultWorkdirPath };
        }

        ChooseWorkspaceData cwd = new ChooseWorkspaceData(defaultWorkdirPath);
        cwd.setRecentWorkspaces(oldRecentWorkspaces);
        ChooseWorkspaceDialog wd = new ChooseWorkspaceDialog(null, cwd, !ALLOW_WORKSPACE_CHOOSER_SUPPRESSION, true);
        int cwdReturnCode = 0 - 1;

        // NOTE: review the "last location" storage strategy before re-enabling suppression (if
        // desired in the future)
        if (!workspaceSettings.getDontAskAgainSetting() || !ALLOW_WORKSPACE_CHOOSER_SUPPRESSION) {
            cwdReturnCode = wd.open();
            if (cwdReturnCode == Dialog.CANCEL) {
                return false;
            }

            if (!cwd.getShowDialog()) {
                workspaceSettings.setDontAskAgainSetting(true);
            }
        }
        final String currentWorkspace;
        if (cwd.getSelection() != null) {
            currentWorkspace = new File(cwd.getSelection()).getAbsolutePath();
        } else {
            currentWorkspace = defaultWorkdirPath;
        }

        // add to head of recent workspaces list, eliminating duplicates
        List<String> newRecentWorkspaces = new ArrayList<>();
        newRecentWorkspaces.add(0, currentWorkspace);
        int pos = 0;
        while (pos < oldRecentWorkspaces.length && newRecentWorkspaces.size() < MAX_RECENT_WORKSPACES_HISTORY) {
            String oldEntry = oldRecentWorkspaces[pos];
            if (!oldEntry.equals(currentWorkspace)) {
                newRecentWorkspaces.add(oldEntry);
            }
            pos++;
        }

        String[] newRecentWorkspacesArray = newRecentWorkspaces.toArray(new String[newRecentWorkspaces.size()]);

        // although these values are not used on the next start, writing them keeps "File > Restart"
        // working - misc_ro
        cwd.setRecentWorkspaces(newRecentWorkspacesArray);
        cwd.writePersistedData();

        workspaceSettings.updateLocationHistory(currentWorkspace, StringUtils.escapeAndConcat(newRecentWorkspacesArray));
        URL userWSURL = new URL("file", null, currentWorkspace);

        try {
            workspaceLocation.set(userWSURL, true);
            tryWorkspaceChoosingAgain = false;
        } catch (IOException e) {
            showErrorDialog("Workspace", "Workspace directory could not be created or is read-only.");
            // A non-valid workspace was chosen, show the workspace chooser again.
            tryWorkspaceChoosingAgain = true;
            workspaceSettings.setDontAskAgainSetting(false);
        }

        return true;
    }
    
    /**
     * Stores version information in system properties, which can be read by about dialog or version command. In case of RC or SNAPSHOT the
     * property for build hint will be set accordingly.
     */
    private void storeVersionInformationToSystemProperties() {
        Version version = VersionUtils.getVersionOfProduct();
        String buildId = VersionUtils.getBuildIdAsString(version);
        if (buildId == null) {
            buildId = "-";
        }
        System.setProperty("rce.version", VersionUtils.getVersionAsString(version));
        System.setProperty("rce.buildId", buildId);
    }
    
    private void showErrorDialog(String title, String message) {
        MessageDialog.openError(new Shell(), title, message);
    }
    
    private boolean showQuestionDialog(String title, String message) {
        return MessageDialog.openQuestion(new Shell(), title, message);
    }
    
}
