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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Version;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.start.common.CommandLineArguments;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.start.common.validation.PlatformValidationManager;
import de.rcenvironment.core.start.gui.internal.ApplicationWorkbenchAdvisor;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.core.utils.common.concurrent.ThreadGuard;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Starts the GUI for RCE.
 * 
 * @author Sascha Zur
 * @author Jan Flink
 * @author Robert Mischke
 * @author Marc Stammerjohann
 */
@SuppressWarnings("restriction")
public final class GUIRunner extends InstanceRunner {

    private static final int MAX_RECENT_WORKSPACES_HISTORY = 5;

    private static final String HINT_SNAPSHOT = "(SNAPSHOT)";

    private static final String HINT_RELEASE_CANDIDATE = "(RELEASE CANDIDATE)";

    private static final String HINT_DEVELOPMENT = "(DEVELOPMENT)";

    private static final String SUFFIX_SNAPSHOT = "_SNAPSHOT";

    private static final String SUFFIX_RC = "_RC";

    private static final String SUFFIX_QUALIFIER = "qualifier";

    private static final boolean ALLOW_WORKSPACE_CHOOSER_SUPPRESSION = true;

    private static boolean tryWorkspaceChoosingAgain = false;

    /**
     * Starts the RCE GUI.
     * 
     * @return exit code
     * @throws Exception : URL
     */
    @Override
    public int run() throws Exception {
        int result = 0 - 1;
        // initialize the GUI
        Display display = PlatformUI.createDisplay();
        Location workspaceLocation = Platform.getInstanceLocation();

        /*
         * Set some system properties to show in the about dialog. In case of RC or SNAPSHOT the property for build hint will be set
         * accordingly.
         */
        Version version = VersionUtils.getVersionOfProduct();
        String versionString = version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
        String qualifier = version.getQualifier();
        String buildHint = "";
        String buildId = qualifier.split("^" + version.getMajor() + "\\." + version.getMinor() + "\\." + version.getMicro())[0];
        if (qualifier.endsWith(SUFFIX_RC)) {
            buildId = buildId.split(SUFFIX_RC)[0];
            buildHint = HINT_RELEASE_CANDIDATE;
        } else if (qualifier.endsWith(SUFFIX_SNAPSHOT)) {
            buildId = buildId.split(SUFFIX_SNAPSHOT)[0];
            buildHint = HINT_SNAPSHOT;
        } else if (qualifier.endsWith(SUFFIX_QUALIFIER)) {
            buildId = "";
            buildHint = HINT_DEVELOPMENT;
        }
        System.setProperty("aboutText.version", versionString);
        System.setProperty("aboutText.buildId", buildId);
        System.setProperty("aboutText.buildHint", buildHint);

        // Write versions to log file
        Log log = LogFactory.getLog(GUIRunner.class);
        log.debug("Core Version: " + VersionUtils.getVersionOfCoreBundles());
        log.debug("Product Version: " + version);
        
        if (checkIfExitRequiredBecauseOfIncorrectLoggingConfiguration()) {
            return IApplication.EXIT_OK;
        }
        
        if (checkIfExitRequiredBecauseOfConfigurationLoadingError()) {
            return IApplication.EXIT_OK;
        }
        
        // trigger execution of "--exec" commands, if they exist
        String[] execCommandTokens = CommandLineArguments.getExecCommandTokens();
        if (execCommandTokens != null) {
            initiateAsyncCommandExecution(execCommandTokens, "execution of --exec commands", false);
        }

        // start the workbench - returns as soon as the workbench is closed
        try {
            // validate the platform and exit if not valid
            if (!(new PlatformValidationManager()).validate(false)) {
                return IApplication.EXIT_OK;
            }
            do {
                // workspace location chooser
                if (!determineWorkspaceLocation(workspaceLocation)) {
                    return IApplication.EXIT_OK;
                }
                // If this flag is true, a non-valid workspace was chosen, show the workspace chooser again until a valid workspace is
                // selected or "cancel" is clicked.
            } while (tryWorkspaceChoosingAgain);

            // mark the GUI thread as forbidden (or at least, critical) for certain operations -
            // misc_ro
            ThreadGuard.setForbiddenThread(Thread.currentThread());
            int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                result = IApplication.EXIT_RESTART;
            } else {
                result = IApplication.EXIT_OK;
            }
        } finally {
            display.dispose();
        }
        return result;
    }

    @Override
    public void onValidationErrors(List<PlatformMessage> messages) {
        // the last encountered error is cached, as only the last error
        // needs to be displayed in BLOCK style
        PlatformMessage lastError = null;
        for (final PlatformMessage error : messages) {
            if (lastError != null) {
                handleError(lastError, StatusManager.SHOW);
            }
            lastError = error;
        }
        handleError(lastError, StatusManager.BLOCK);
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
            displayError("Workspace", "Workspace folder could not be created or is read-only.");
            // A non-valid workspace was chosen, show the workspace chooser again.
            tryWorkspaceChoosingAgain = true;
            workspaceSettings.setDontAskAgainSetting(false);
        }

        return true;
    }

    private void handleError(final PlatformMessage error, final int style) {
        final String errorMessageLabel = StringUtils.format("%s: %s", error.getType(), error.getMessage());
        final IStatus status = new Status(Status.ERROR, error.getBundleSymbolicName(), errorMessageLabel);
        StatusManager.getManager().handle(status, style);
        if (style == StatusManager.BLOCK) {
            displayError(error);
        }
    }

    private void displayError(final PlatformMessage error) {
        displayError("Validation", error.getMessage());
    }

    private void displayError(String title, String message) {
        MessageDialog.openError(new Shell(), title, message);
    }

    private boolean checkIfExitRequiredBecauseOfConfigurationLoadingError() {
        ConfigurationService configurationService = ServiceRegistry.createAccessFor(this).getService(ConfigurationService.class);
        if (configurationService.isUsingDefaultConfigurationValues()) {
            boolean yes = MessageDialog.openQuestion(new Shell(), "Configuration", "Failed to load configuration file. Most likely, "
                + "it has syntax errors. Check the log for details.\n\nDefault configuration values will be applied.\n\nProceed anyway?");
            return !yes;
        }
        return false;
    }
    
    private boolean checkIfExitRequiredBecauseOfIncorrectLoggingConfiguration() {
        if (!isLoggingConfiguredProperly()) {
            MessageDialog.openError(new Shell(), "Logging", ERROR_MESSAGE_INCORRECT_LOGGING_CONFIG + "\n\n"
                + INFO_MESSAGE_INCORRECT_LOGGING_CONFIG);
            return true;
        }
        return false;
    }
}
