/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.osgi.framework.Version;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.gui.utils.incubator.Sleak;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.gui.internal.ApplicationWorkbenchAdvisor;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadGuard;

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

    private static final int GUI_STARTUP_READINESS_POLLING_INTERVAL_MSEC = 250;

    /**
     * System property for launching Sleak to detect SWT resource leaks.
     */
    private static final String DRCE_DEBUG_SLEAK = "rce.debug.sleak";

    private static boolean tryWorkspaceChoosingAgain = false;

    // prevents processing of shutdown signals before the UI framework is ready, which results in various errors
    private final CountDownLatch readyForShutdownSignalsLatch = new CountDownLatch(1);

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

        boolean startSleak = System.getProperties().containsKey(DRCE_DEBUG_SLEAK);
        if (startSleak) {
            // this needs to be executed before the display has been created
            org.eclipse.ui.internal.misc.Policy.DEBUG_SWT_GRAPHICS = true;
            org.eclipse.ui.internal.misc.Policy.DEBUG_SWT_DEBUG = true;
        }

        // initialize the GUI
        final Display display = PlatformUI.createDisplay();

        if (startSleak) {
            // this needs to be executed after the display has been created
            Sleak sleak = new Sleak();
            sleak.open();
        }

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

            // periodically check the PlatformUI.getWorkbench().isStarting() flag and only allow shutdown signals once it is "false"
            final Runnable enqueueGUIStartupCompletionCheck = new Runnable() {

                @Override
                public void run() {
                    final boolean starting = PlatformUI.getWorkbench().isStarting();
                    if (!starting) {
                        // the application is now as ready as possible for processing shutdown signals, so unblock them
                        readyForShutdownSignalsLatch.countDown();
                    } else {
                        // enqueue this Runnable again
                        display.timerExec(GUI_STARTUP_READINESS_POLLING_INTERVAL_MSEC, this);
                    }
                }
            };

            display.timerExec(GUI_STARTUP_READINESS_POLLING_INTERVAL_MSEC, enqueueGUIStartupCompletionCheck);

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
            showErrorDialog("Instance validation failure", result.getGuiDialogMessage() + "\n\nRCE will be shut down.");
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
        triggerAsyncShutdownOrRestart(false);
    }

    @Override
    public void triggerRestart() {
        triggerAsyncShutdownOrRestart(true);
    }

    private void triggerAsyncShutdownOrRestart(final boolean performRestart) {
        // spawn a new async task to avoid blocking the caller while waiting for the latch -- misc_ro
        ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

            @Override
            @TaskDescription("Process shutdown trigger (GUI mode)")
            public void run() {
                final boolean logWaitingPeriod = readyForShutdownSignalsLatch.getCount() != 0;
                if (logWaitingPeriod) {
                    log.debug("Shutdown triggered during early GUI startup; waiting for completion signal");
                }
                try {
                    // no timeout needed, as there is a timeout guard for the whole shutdown process already
                    readyForShutdownSignalsLatch.await();
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for shutdown readiness (restart flag=" + performRestart + ")");
                    return;
                }
                if (logWaitingPeriod) {
                    log.debug("Received signal that early GUI startup has completed; proceeding with shutdown/restart request");
                }
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (!PlatformUI.isWorkbenchRunning()) {
                            return;
                        }
                        log.debug("Triggering shutdown/restart of GUI platform");
                        if (performRestart) {
                            PlatformUI.getWorkbench().restart();
                        } else {
                            PlatformUI.getWorkbench().close();
                        }
                    }
                });
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

        // this flag is called "standard" here as "default" is ambiguous (see below)
        // standard workspace = the "workspace" sub-folder of the profile directory
        final boolean useStandardWorkspace = CommandLineArguments.isUseDefaultWorkspaceRequested();

        // default workspace = either the last used (stored) workspace or the "standard" workspace, depending on situation
        final String defaultWorkdirPath;
        if (useStandardWorkspace || lastWorkspaceLocation == null) {
            defaultWorkdirPath = new File(profileDirectory, "workspace").getAbsolutePath();
        } else {
            defaultWorkdirPath = new File(lastWorkspaceLocation).getAbsolutePath();
        }

        final String[] oldRecentWorkspaces;
        if (recentWorkspacesData != null) {
            oldRecentWorkspaces = StringUtils.splitAndUnescape(recentWorkspacesData);
        } else {
            oldRecentWorkspaces = new String[] { defaultWorkdirPath };
        }

        ChooseWorkspaceData cwd = new ChooseWorkspaceData(defaultWorkdirPath);
        cwd.setRecentWorkspaces(oldRecentWorkspaces);

        // NOTE: review the "last location" storage strategy before re-enabling suppression (if
        // desired in the future)
        if (!useStandardWorkspace && !workspaceSettings.getDontAskAgainSetting() || !ALLOW_WORKSPACE_CHOOSER_SUPPRESSION) {
            ChooseWorkspaceDialog wd = new ChooseWorkspaceDialog(null, cwd, !ALLOW_WORKSPACE_CHOOSER_SUPPRESSION, true);
            int cwdReturnCode = wd.open();
            if (cwdReturnCode == Dialog.CANCEL) {
                return false;
            }

            if (!cwd.getShowDialog()) {
                workspaceSettings.setDontAskAgainSetting(true);
            }
        }

        final String currentWorkspace;
        if (useStandardWorkspace || cwd.getSelection() == null) {
            currentWorkspace = defaultWorkdirPath;
        } else {
            currentWorkspace = new File(cwd.getSelection()).getAbsolutePath();
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
        MessageDialog.openError(new Shell(SWT.ON_TOP), title, message);
    }

    private boolean showQuestionDialog(String title, String message) {
        return MessageDialog.openQuestion(new Shell(SWT.ON_TOP), title, message);
    }

}
