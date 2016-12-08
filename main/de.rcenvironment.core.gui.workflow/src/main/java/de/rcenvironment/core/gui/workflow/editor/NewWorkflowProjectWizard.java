/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.InvalidFilenameException;

/**
 * Wizard to create a new workflow file and project if needed.
 * 
 * @author Oliver Seebach
 */
public class NewWorkflowProjectWizard extends Wizard implements INewWizard,
        IPageChangingListener {
  

    /** Workflow name shared among wizard.   */
    public static String sharedWorkflowName;
    
    private static final String NEW_WORKFLOW_TITLE = "New Workflow";

    private static final String WF_EDITOR_ID = "de.rcenvironment.rce.gui.workflow.editor.WorkflowEditor";

    /** The current selection in the navigator. */
    private static IStructuredSelection workbenchSelection;
    
    private static boolean canFinishFlag = false;
    
    private IFile newWorkflowFile = null;
    
    /** Content of the wizard. */
    private NewWorkflowPage workflowPage;

    /** Content of the wizard. */
    private ProjectSelectionPage projectPage;

    private String projectNameToSet;
    
    private String workspaceToFile = "";

    
    public NewWorkflowProjectWizard() {
        super();
    }

    public NewWorkflowProjectWizard(ISelection selection) {
        // For access via handler
        super();
        initialize(selection);
    }

    private void initialize(ISelection selection) {
        sharedWorkflowName = null;
        setNeedsProgressMonitor(true);
        setWindowTitle(NEW_WORKFLOW_TITLE);
        if (selection instanceof IStructuredSelection){
            NewWorkflowProjectWizard.setWorkbenchSelection((IStructuredSelection) selection);        
        }
        allowFinish();       
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        // For access via extension point
        initialize(currentSelection);

    }

    @Override
    public void addPages() {
        workflowPage = new NewWorkflowPage(this, getWorkbenchSelection());
        projectPage = new ProjectSelectionPage(this, getWorkbenchSelection());
        addPage(workflowPage);
        addPage(projectPage);
        
        WizardDialog dialog = (WizardDialog) getContainer();
        if (dialog != null) {
            dialog.addPageChangingListener(this);
        }
    }

    @Override
    public boolean performFinish() {

        final String workflowName = workflowPage.getWorkflownameTextfield().getText();
        final ProjectUsages usage = projectPage.getUsage();
        final String projectName = projectPage.getProjectNameTextField().getText();
        
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doFinish(projectName, workflowName, monitor, usage);
                } catch (CoreException | InvalidFilenameException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error",
                    realException.getMessage());
            return false;
        }
        
        // open the created workflow
        openCreatedWorkflow();
        return true;

    }

    private void openCreatedWorkflow() {
        
        // open workflow editor with respective workflow file
        IEditorInput editorInput = new FileEditorInput(newWorkflowFile);
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().findEditor(WF_EDITOR_ID);
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        
        try {
            activePage.openEditor(editorInput, desc.getId());
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        }
        
        // reveal file in project explorer
        IViewPart view = activePage.findView(IPageLayout.ID_PROJECT_EXPLORER);
        ((ISetSelectionTarget) view).selectReveal(new StructuredSelection(newWorkflowFile));
    }

    

    private void doFinish(String newProjectName, String newWorkflowName,
            IProgressMonitor monitor, ProjectUsages usage) throws CoreException, InvalidFilenameException {

        // create content of empty workflow
        UUID id = UUID.randomUUID();
        String workflowString = "{ \r" + "\"identifier\" : \"" + id + "\", \r"
                + "\"workflowVersion\" : \""
                + WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER + "\"\r"
                + "}";

        //Set project name to null in case it was set to invalid name before, which caused a bug.
        projectNameToSet = null;
        // the project name to be set is the selected one, execept the "new" button is selected
        if ((getWorkbenchSelection() instanceof TreeSelection || getWorkbenchSelection() instanceof StructuredSelection)
                && getWorkbenchSelection() != null && !usage.equals(ProjectUsages.NEW)) {
            if (getWorkbenchSelection().getFirstElement() instanceof IProject) {
                // if project is selected
                projectNameToSet = ((IProject) getWorkbenchSelection()
                        .getFirstElement()).getName();
            } else if (getWorkbenchSelection().getFirstElement() instanceof IFile) {
                // if ifile is selected
                IFile file = ((IFile) getWorkbenchSelection().getFirstElement());
                projectNameToSet = file.getProject().getName();
            } else if (getWorkbenchSelection().getFirstElement() instanceof IFolder) {
                // if ifolder
                IFolder folder = ((IFolder) getWorkbenchSelection().getFirstElement());
                projectNameToSet = folder.getProject().getName();
                workspaceToFile = folder.getFullPath().toOSString().substring(projectNameToSet.length() + 2) + File.separator;
            } else if (getWorkbenchSelection().getFirstElement() instanceof File) {
                // if file
                File file = (File) getWorkbenchSelection().getFirstElement();
                handleFileSelection(file);
            } else if (getWorkbenchSelection().getFirstElement() instanceof TreeSelection) {
                // if treeselection
                TreeSelection treeSelection = ((TreeSelection) getWorkbenchSelection().getFirstElement());
                if (treeSelection.getFirstElement() instanceof File) {
                    File file = (File) treeSelection.getFirstElement();
                    handleFileSelection(file);
                } else if (treeSelection.getFirstElement() instanceof IProject){
                    projectNameToSet = ((IProject) treeSelection.getFirstElement()).getName();
                }
            }
        } else {
            if (!newProjectName.equals("")) {
                projectNameToSet = newProjectName;
            }
        }
        
         // if nothing is selected, the project name is set as the entered workflow name  
        if (projectNameToSet == null){
            usage = ProjectUsages.NEW;
            projectNameToSet = newWorkflowName; 
        }

        // create project if necessary, otherwise just get it from workspace
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectNameToSet);
        if (usage.equals(ProjectUsages.NEW)) {
            project.create(monitor);
            project.open(monitor);
            monitor.worked(1);

        }
        
        // create file and fill with content
        newWorkflowFile = project.getFile(workspaceToFile + newWorkflowName + ".wf");
        CrossPlatformFilenameUtils.throwExceptionIfFilenameNotValid(newWorkflowFile.getName());
        InputStream stream = new ByteArrayInputStream(workflowString.getBytes());
        newWorkflowFile.create(stream, true, monitor);
        
        // make sure finish is not activated afterwards
        preventFinish();
    }
    
    private void handleFileSelection(File file) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath location = Path.fromOSString(file.getAbsolutePath());
        IFile ifile = workspace.getRoot().getFileForLocation(location);
        projectNameToSet = ifile.getProject().getName();
        workspaceToFile = ifile.getProjectRelativePath().toOSString() + File.separator;
    }

    // handle activation of finish button
    @Override
    public boolean canFinish() {
        boolean doFinish = super.canFinish() && canFinishFlag;
        return doFinish;
    }

    /** Allows to finish the wizard. */
    public static void allowFinish() {
        canFinishFlag = true;
    }

    /** Prevents to finish the wizard. */
    public static void preventFinish() {
        canFinishFlag = false;
    }

    @Override
    public void handlePageChanging(PageChangingEvent pageChangingEvent) {
        // handles activation of finish button
        allowFinish();
        if (pageChangingEvent.getTargetPage().getClass()
                .equals(ProjectSelectionPage.class)) {
            projectPage.getProjectNameTextField().setText(sharedWorkflowName);
            if (projectPage.getNewProjectRadioButton().getSelection()) {
                projectPage.getNewProjectRadioButton().notifyListeners(SWT.Selection, new Event());
            } else {
                projectPage.getExistingProjectRadioButton().notifyListeners(SWT.Selection, new Event());
            }
        }
    }

    public static IStructuredSelection getWorkbenchSelection() {
        return workbenchSelection;
    }

    public static void setWorkbenchSelection(IStructuredSelection workbenchSelection) {
        NewWorkflowProjectWizard.workbenchSelection = workbenchSelection;
    }
}
