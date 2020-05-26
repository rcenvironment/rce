/*
 * Copyright 2006-2020 DLR, Germany
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
import org.eclipse.jface.window.Window;
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
 * @author Robert Mischke (fixed issue when project wizard is cancelled by user)
 */
public class ShowExampleAction implements IIntroAction {

    private static final String WORKFLOW_EXAMPLES_PROJECT_FIRST_WORKFLOW_NAME = "01_01_Hello_World.wf";

    private static final String WORKFLOW_EXAMPLES_PROJECT_FIRST_FOLDER_NAME = "01_First Steps";

    private static final String WORKFLOW_EXAMPLES_PROJECT_NAME = "Workflow Examples Project";

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Executes when sample card at welcome screen is clicked.
     */
    @Override
    public void run(IIntroSite site, Properties properties) {
        try {
            if (!tryImportWorkflowExamplesProject()) {
                return;
            }
            loadFirstExampleWorkflowIntoWorkflowEditor();
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

    private boolean tryImportWorkflowExamplesProject() throws CoreException {
        // We only explicitly handle a errors occurring in the first method `ensureExampleProjectExistsOnDisk` here since that is the only
        // method which requires input from the user. Hence, in that method the user may request cancellation of the process, which we have
        // to handle gracefully (i.e., without throwing an exception). The remaining methods `ensureExampleProjectExistsInWorkspace` and
        // `ensureExampleProjectIsOpened` do not require user input and thus their only failure mode is throwing an exception
        if (ensureExampleProjectExistsOnDisk()) {
            // Since `ensureExampleProjectExistsInWorkspace` determines whether there exists a project with name WORKFLOW_EXAMPLES_PROJECT
            // in the workspace and since that name is hardcoded as a constant in this class, we implicitly assume here that the user did
            // not change the name of the workflow example project when importing it
            ensureExampleProjectExistsInWorkspace();
            ensureExampleProjectIsOpened();
            return true;
        } else {
            return false;
        }

    }

    private boolean ensureExampleProjectExistsOnDisk() {
        if (doesExampleProjectExistOnDisk()) {
            return true;
        } else {
            return showExampleProjectImportDialog();
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

    private boolean doesExampleProjectExistOnDisk() {
        final ProjectFile projectFile = getExampleProjectFile();
        return projectFile.exists();
    }

    private ProjectFile getExampleProjectFile() {
        final IPath absolutePathToWorkspace = getAbsolutePathToWorkspace();

        final IPath absolutePathToExampleProjectFolder = absolutePathToWorkspace.append(WORKFLOW_EXAMPLES_PROJECT_NAME);
        return ProjectFile.createForProjectFolder(absolutePathToExampleProjectFolder);
    }

    private IPath getAbsolutePathToWorkspace() {
        final URL workspaceURL = Platform.getInstanceLocation().getURL();

        throwExceptionIfURLIsNull(workspaceURL);

        // At this point we silently assume that the workspace is located on the user's local machine, i.e., we do not support remote
        // workspace locations
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

    /**
     * This method returns false if importing the example project onto disk fails for any reason, including termination of the import by the
     * user. This may, e.g., happen because the user cancels the import wizard.
     * 
     * @return True if the user has imported the example project onto disk via the wizard, false otherwise
     */
    private boolean showExampleProjectImportDialog() {
        WizardDialog dialog = new WizardDialog(null, new RCEExampleProjectWizard());
        return (dialog.open() == Window.OK);
    }

    private void loadFirstExampleWorkflowIntoWorkflowEditor() throws PartInitException {
        IPath absolutePathToWorkspace = getAbsolutePathToWorkspace();

        final IPath pathToFirstExampleWorkflow = absolutePathToWorkspace
            .append(WORKFLOW_EXAMPLES_PROJECT_NAME)
            .append(WORKFLOW_EXAMPLES_PROJECT_FIRST_FOLDER_NAME)
            .append(WORKFLOW_EXAMPLES_PROJECT_FIRST_WORKFLOW_NAME);
        final WorkflowFile firstExampleWorkflowFile = WorkflowFile.fromPath(pathToFirstExampleWorkflow);

        if (firstExampleWorkflowFile.canBeLoadedIntoWorkflowEditor()) {
            IWorkbenchPage page = getWorkflowEditorPage();
            firstExampleWorkflowFile.loadIntoWorkbenchPage(page);
        }
    }

    private IWorkbenchPage getWorkflowEditorPage() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }
}
