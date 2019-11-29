/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.introduction;

import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import de.rcenvironment.core.gui.wizards.exampleproject.RCEExampleProjectWizard;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * This class opens the "Workflow Examples Project" and loads it in the workspace.
 * 
 * @author Riccardo Dusi
 * @author Alexander Weinert (refactoring and cleanup)
 */
public class ShowExampleAction implements IIntroAction {

    private static final String WORKFLOW_EXAMPLES_PROJECT_FIRST_WORKFLOW_NAME = "01_01_Hello_World.wf";

    private static final String WORKFLOW_EXAMPLES_PROJECT_FIRST_FOLDER_NAME = "01_First Steps";

    // REVIEW (AW): What happens if the user chooses a different name for the workflow examples project in the wizard?
    private static final String WORKFLOW_EXAMPLES_PROJECT_NAME = "Workflow Examples Project";

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Executes when sample card at welcome screen is clicked.
     */
    @Override
    public void run(IIntroSite site, Properties properties) {
        try {
            importWorkflowExamplesProject();

            // REVIEW (AW): Previous error message: "The Project could not be loaded". This message is highly confusing if it occurs in
            // a log file without context
            // log.warn("The Project could not be loaded.");
            // REVIEW (AW): This return did not previously exist, causing the action to proceed even if the Workflow Examples Project
            // could not be loaded

            loadFirstExampleWorkflowIntoWorkflowEditor();

            /*
             * REVIEW (AW): Previous code: if (!successfullyLoaded) { log.warn("The Project could not be loaded in Workflow Editor."); }
             * else { log.debug("The Project is loaded in Workflow Editor."); }
             */

            showWelcomeScreenOnSidePanel();
        } catch (CoreException e) {
            logExceptionAsWarning(e);
        }
    }

    private void showWelcomeScreenOnSidePanel() {
        IIntroPart intropart = PlatformUI.getWorkbench().getIntroManager().getIntro();
        PlatformUI.getWorkbench().getIntroManager().setIntroStandby(intropart, true);
    }

    private void logExceptionAsWarning(CoreException e) {
        log.warn(StringUtils.format("Status: %s\nCause: %s", e.getStatus(), e.getCause()));
    }

    private void importWorkflowExamplesProject() throws CoreException {
        ensureExampleProjectExistsOnDisk();
        ensureExampleProjectExistsInWorkspace();
        ensureExampleProjectIsOpened();
    }

    private void ensureExampleProjectExistsOnDisk() {
        if (!exampleProjectExistsOnDisk()) {
            openWizardDialog();
        }
    }

    private void ensureExampleProjectExistsInWorkspace() throws CoreException {
        final ProjectFile projectFile = getExampleProjectFile();
        final IProjectDescription description = projectFile.getProjectDescription();
        final IProject project = projectFile.getProjectFromWorkspaceByName();
        ensureProjectExists(description, project);
    }

    private void ensureExampleProjectIsOpened() throws CoreException {
        final ProjectFile projectFile = getExampleProjectFile();
        final IProject project = projectFile.getProjectFromWorkspaceByName();
        ensureProjectIsOpened(project);
    }

    private boolean exampleProjectExistsOnDisk() {
        final ProjectFile projectFile = getExampleProjectFile();
        return projectFile.exists();
    }

    private ProjectFile getExampleProjectFile() {
        final IPath absolutePathToWorkspace = getAbsolutePathToWorkspace();

        final IPath absolutePathToExampleProjectFolder = absolutePathToWorkspace.append(WORKFLOW_EXAMPLES_PROJECT_NAME);
        final ProjectFile absolutePathToProjectFile = ProjectFile.createForProjectFolder(absolutePathToExampleProjectFolder);

        return absolutePathToProjectFile;
    }

    private IPath getAbsolutePathToWorkspace() {
        final URL workspaceURL = Platform.getInstanceLocation().getURL();

        throwExceptionIfURLIsNull(workspaceURL);

        // REVIEW (AW): At this point we silently assume that the workspace is located on the user's local machine, i.e., we do not support
        // remote workspace locations
        return getAbsolutePathFromLocalURL(workspaceURL);
    }

    private void throwExceptionIfURLIsNull(final URL workspaceURL) {
        if (workspaceURL == null) {
            throw new IllegalStateException("No workspace defined when importing Workflow Example Project.");
        }
    }

    private IPath getAbsolutePathFromLocalURL(URL workspaceURL) {
        // We have to work around an issue with URL#getPath: The returned absolute path begins with a forward slash, both on Windows-
        // and Linux systems. The path thus starts, e.g., with "/C:/". As this is obviously not a valid path on Windows systems, we have to
        // truncate the first character of the returned path in this case.
        final String absolutePathToWorkspace = workspaceURL.getPath();
        if (isRunningOnWindows()) {
            return new Path(truncateFirstLetterFromString(absolutePathToWorkspace));
        } else {
            return new Path(absolutePathToWorkspace);
        }
    }

    private boolean isRunningOnWindows() {
        return Platform.getOS().equals(Platform.OS_WIN32);
    }

    private String truncateFirstLetterFromString(String value) {
        return value.substring(1);
    }

    private void ensureProjectExists(IProjectDescription description, IProject project) throws CoreException {
        if (!project.exists()) {
            project.create(description, null);
        }
    }

    private void ensureProjectIsOpened(IProject project) throws CoreException {
        if (!project.isOpen()) {
            project.open(null);
        }
    }

    private void openWizardDialog() {
        WizardDialog dialog = new WizardDialog(null, new RCEExampleProjectWizard());
        dialog.open();
    }

    private void loadFirstExampleWorkflowIntoWorkflowEditor() throws PartInitException {
        IPath absolutePathToWorkspace = getAbsolutePathToWorkspace();

        if (absolutePathToWorkspace == null) {
            log.warn("No platform instance location available");
            return;
        }

        // REVIEW (AW): Used to be StringUtils.format("%s/%s/%s/%s", workspaceURL.getPath(), projectDir, subProjectDir, wfFilename);
        final IPath pathToFirstExampleWorkflow = absolutePathToWorkspace.append(WORKFLOW_EXAMPLES_PROJECT_NAME)
            .append(WORKFLOW_EXAMPLES_PROJECT_FIRST_FOLDER_NAME).append(WORKFLOW_EXAMPLES_PROJECT_FIRST_WORKFLOW_NAME);
        final WorkflowFile firstExampleWorkflowFile = WorkflowFile.fromPath(pathToFirstExampleWorkflow);

        if (firstExampleWorkflowFile.canBeLoadedIntoWorkflowEditor()) {
            // REVIEW (AW): There used to be a check for the workspace path being a valid path according to path.isValidPath here
            // Why was this necessary?
            // Original code: if (path.isValidPath(workspacePath.substring(1, workspacePath.length()))) {
            IWorkbenchPage page = getWorkflowEditorPage();
            firstExampleWorkflowFile.loadIntoWorkbenchPage(page);
        }
    }

    private IWorkbenchPage getWorkflowEditorPage() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }
}
