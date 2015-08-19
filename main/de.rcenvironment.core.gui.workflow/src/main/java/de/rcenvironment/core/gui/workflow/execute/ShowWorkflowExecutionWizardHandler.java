/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.LoadingWorkflowDescriptionHelper;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Opens the {@link WorkflowExecutionWizard}.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public class ShowWorkflowExecutionWizardHandler extends AbstractHandler {

    private static final String DUPLICATE_ID_WARNING_MSG = "Could not determine duplicate WF ids";
    
    private static final String GETTING_ATTR_WARNING_MSG = " - failed to get file attributes for: ";

    private static final String NODES = "nodes";

    private static final String IDENTIFIER = "identifier";

    private static final Pattern WORKFLOW_FILENAME_PATTERN = Pattern.compile("^.*\\.wf$");

    private static final Log LOGGER = LogFactory.getLog(ShowWorkflowExecutionWizardHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();
    
    private boolean confirm;

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {

        // retrieve the WorkflowDescription
        final WorkflowDescription displayedWorkflowDescription = getDisplayedWorkflowDescription();
        boolean fromFile = true;
        IFile workflowFile = getFirstSelectedWorkflowFile(event);
        confirm = true;

        if (workflowFile == null) {
            workflowFile = getDisplayedWorkflowFile();
            fromFile = false;
        }
        if (workflowFile != null) {

            // Compares the project explorer selection with dirty editors and triggers saveChangesDialog if selection is dirty
            IEditorReference[] currentEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
            for (IEditorReference editor : currentEditors) {
                IEditorPart editorPart = (IEditorPart) editor.getPart(true);
                if (editorPart instanceof WorkflowEditor) {
                    WorkflowEditor workflowEditor = (WorkflowEditor) editorPart;
                    IFile editorFile = ((FileEditorInput) workflowEditor.getEditorInput()).getFile();
                    if (workflowFile.getProject().equals(editorFile.getProject())) {
                        if (workflowFile.getName().equals(editorPart.getTitle()) && editorPart.isDirty()) {
                            saveChangesDialog(editorPart);
                            break;
                        }
                    }
                }
            }
        
            // Exit execute method when saving changes has been canceled
            if (!confirm) {
                return null;
            }
        
            searchAndReplaceDuplicateIDs(workflowFile);
            
            final IFile finalWorkflowFile = workflowFile;
            final boolean finalFromFile = fromFile;
            Job job = new Job("Executing workflow") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask("Loading components", 2);
                        monitor.worked(1);
                        WorkflowDescription workflowDescription = displayedWorkflowDescription;
                        if (finalFromFile) {
                            workflowDescription = LoadingWorkflowDescriptionHelper
                                .loadWorkflowDescription(null, finalWorkflowFile, finalFromFile);
                        }
                        monitor.worked(1);
                        if (workflowDescription != null) {
                            final WorkflowDescription finalWorkflowDescription = workflowDescription;
                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    // instantiate the WorkflowExecutionWizard with the WorkflowDescription
                                    final Wizard workflowExecutionWizard = new WorkflowExecutionWizard(finalWorkflowFile,
                                        finalWorkflowDescription, finalFromFile);
                                    final WorkflowWizardDialog wizardDialog =
                                        new WorkflowWizardDialog(Display.getDefault().getActiveShell(), workflowExecutionWizard);
                                    wizardDialog.setBlockOnOpen(false);
                                    wizardDialog.open();
                                }
                            });
                        }
                    } catch (RuntimeException e) {
                        // caught and only logged as an error dialog already pops up if an error occur
                        LogFactory.getLog(getClass()).error("Loading workflow failed", e);
                    } finally {
                        monitor.done();
                    }
                    return Status.OK_STATUS;
                };
                
            };
            job.setUser(true);
            job.schedule();
        }
        return null;
    }

    private void searchAndReplaceDuplicateIDs(IFile workflowFile) {
        String absoluteFilePath = workflowFile.getLocation().toString();
        Set<IResource> wfsWithDuplicateId = searchForDuplicateWFIdentifier(absoluteFilePath);
        if (wfsWithDuplicateId != null && !wfsWithDuplicateId.isEmpty()) {
            try {
                Path orig = Paths.get(absoluteFilePath);
                BasicFileAttributeView origAttrView = Files.getFileAttributeView(orig, BasicFileAttributeView.class);
                if (origAttrView == null) {
                    LOGGER.warn(DUPLICATE_ID_WARNING_MSG + GETTING_ATTR_WARNING_MSG + absoluteFilePath);
                    return;
                }
                BasicFileAttributes attrOrig = origAttrView.readAttributes();
                for (IResource duplicate : wfsWithDuplicateId) {
                    Path duplicatePath = Paths.get(duplicate.getLocationURI());
                    BasicFileAttributeView attrViewDuplicate = Files.getFileAttributeView(duplicatePath, BasicFileAttributeView.class);
                    if (attrViewDuplicate == null) {
                        LOGGER.warn(DUPLICATE_ID_WARNING_MSG + GETTING_ATTR_WARNING_MSG + absoluteFilePath);
                        continue;
                    }
                    BasicFileAttributes attrDuplicate = attrViewDuplicate.readAttributes();

                    // Comparing the creation time is buggy on linux machines.
                    // It returns the modification time which can be the same as in the original and
                    // thus the original file is modified, doesn't find its placeholder and
                    // falls back to an old placeholder
                    // The bug is fixed in java 8:
                    // http://hg.openjdk.java.net/jdk8/jdk8/jdk/rev/296c9ec816c6
                    if (attrOrig.creationTime().compareTo(attrDuplicate.creationTime()) > 0) {
                        replaceIdentifierInWorkflowFile(absoluteFilePath);
                        workflowFile.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                    } else {
                        replaceIdentifierInWorkflowFile(duplicate.getLocation().toString());
                        duplicate.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                    }
                }
            } catch (CoreException e) {
                LOGGER.warn(DUPLICATE_ID_WARNING_MSG, e);
            } catch (IOException e) {
                LOGGER.warn(DUPLICATE_ID_WARNING_MSG, e);
            }
        }
    }

    private IFile getDisplayedWorkflowFile() {
        final IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (part instanceof IEditorPart) {
            final IEditorPart editor = (IEditorPart) part;
            if (editor instanceof WorkflowEditor) {
                WorkflowEditor workflowEditor = (WorkflowEditor) editor;
                IEditorInput input = workflowEditor.getEditorInput();
                if (input instanceof FileEditorInput) {
                    return ((FileEditorInput) input).getFile();
                } else if (input instanceof FileStoreEditorInput) {
                    MessageDialog.openInformation(part.getSite().getShell(), "Workflow Run",
                        "Please put the workflow into a project of your workspace first.\n\n"
                        + "Drag the workflow from the file system into the project explorer to a project of your choice. "
                        + "You can decide to either copy or only link it.\n\nIf you don't have a project yet, "
                        + "create one first via File->New->Project->General.");
                }
            }
            if (editor.isDirty()) {
                saveChangesDialog(editor);
            }
        }
        return null;
    }


    // Dialog for saving changes in dirty editors
    private void saveChangesDialog(IEditorPart editor) {
        final IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        confirm = MessageDialog.openConfirm(part.getSite().getShell(), Messages.askToSaveUnsavedEditorChangesTitle,
            StringUtils.format(Messages.askToSaveUnsavedEditorChangesMessage, editor.getTitle()));
        if (confirm) {
            editor.doSave(null);
        } 

    }

    private WorkflowDescription getDisplayedWorkflowDescription() {
        final IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (part instanceof IEditorPart) {
            final IEditorPart editor = (IEditorPart) part;
            if (editor instanceof WorkflowEditor) {
                WorkflowEditor workflowEditor = (WorkflowEditor) editor;
                return workflowEditor.getWorkflowDescription();
            }
        }
        return null;
        
    }

    private IFile getFirstSelectedWorkflowFile(final ExecutionEvent event) {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
                Object next = iter.next();
                if (!(next instanceof IFile)) {
                    continue;
                }
                final IFile file = (IFile) next;
                String filename = file.getName();
                if (!WORKFLOW_FILENAME_PATTERN.matcher(filename).matches()) {
                    continue;
                }
                return file;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void replaceIdentifierInWorkflowFile(String filePath) {

        try {
            File wfToEdit = new File(filePath);
            Map<String, Object> wfContent =
                mapper.readValue(wfToEdit, new HashMap<String, Object>().getClass());

            String wfContentString = FileUtils.readFileToString(wfToEdit);

            String idWorkflow = (String) wfContent.get(IDENTIFIER);
            wfContentString = wfContentString.replace(idWorkflow, UUID.randomUUID().toString());
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) wfContent.get(NODES);
            if (nodes != null) {
                for (Map<String, Object> node : nodes) {
                    String nodeID = (String) node.get(IDENTIFIER);
                    wfContentString = wfContentString.replaceAll(nodeID, UUID.randomUUID().toString());
                }
            }
            FileUtils.write(wfToEdit, wfContentString);
        } catch (IOException e) {
            LOGGER.warn(DUPLICATE_ID_WARNING_MSG, e);
        }
    }

    private Set<IResource> searchForDuplicateWFIdentifier(String absoluteFilePath) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        Set<IResource> duplicates = new HashSet<IResource>();
        try {
            @SuppressWarnings("unchecked") Map<String, Object> wfContent =
                mapper.readValue(new File(absoluteFilePath), new HashMap<String, Object>().getClass());

            for (IProject p : root.getProjects()) {
                if (p.isOpen()) {
                    try {
                        for (IResource res : p.members()) {
                            if (res instanceof IFile && WORKFLOW_FILENAME_PATTERN
                                .matcher(((IFile) res).getName()).matches()) {
                                IResource possibleDuplicate = checkFileForDuplicateID(wfContent, absoluteFilePath, res);
                                if (possibleDuplicate != null && !absoluteFilePath.equals(possibleDuplicate.getLocation().toString())) {
                                    duplicates.add(possibleDuplicate);
                                }
                            }
                            if (res instanceof IFolder) {
                                Set<IResource> possibleDuplicate = checkFolderForDuplicateId(wfContent, absoluteFilePath, res);
                                if (possibleDuplicate != null) {
                                    duplicates.addAll(possibleDuplicate);
                                }
                            }
                        }
                    } catch (CoreException e) {
                        LOGGER.warn(DUPLICATE_ID_WARNING_MSG, e);
                    } catch (IOException e) {
                        LOGGER.warn(DUPLICATE_ID_WARNING_MSG, e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn(DUPLICATE_ID_WARNING_MSG, e);
        }
        return duplicates;
    }

    private Set<IResource> checkFolderForDuplicateId(Map<String, Object> wfContent, String absoluteFilePath, IResource res)
        throws JsonParseException,
        JsonMappingException, IOException, CoreException {
        Set<IResource> possibleDuplicates = new HashSet<IResource>();
        for (IResource resource : ((IFolder) res).members()) {
            if (resource instanceof IFolder) {
                Set<IResource> possibleDuplicate = checkFolderForDuplicateId(wfContent, absoluteFilePath, resource);
                if (possibleDuplicate != null) {
                    possibleDuplicates.addAll(possibleDuplicate);
                }
            } else if (resource instanceof IFile
                && WORKFLOW_FILENAME_PATTERN.matcher(((IFile) resource).getName()).matches()) {
                IResource possibleDuplicate = checkFileForDuplicateID(wfContent, absoluteFilePath, resource);
                if (possibleDuplicate != null && !absoluteFilePath.equals(possibleDuplicate.getLocation().toString())) {
                    possibleDuplicates.add(possibleDuplicate);
                }
            }
        }
        return possibleDuplicates;
    }

    @SuppressWarnings("unchecked")
    private IResource checkFileForDuplicateID(Map<String, Object> wfContent, String absoluteFilePath, IResource res) throws IOException,
        JsonParseException,
        JsonMappingException {
        try {
            Map<String, Object> wfContentExisting =
                mapper.readValue(new File(res.getLocation().toString()), new HashMap<String, Object>().getClass());
            if (!absoluteFilePath.equals(res.getLocation().toString())
                && wfContent.get(IDENTIFIER).equals(wfContentExisting.get(IDENTIFIER))) {
                return res;
            }
        } catch (JsonParseException e) {
            LOGGER.debug("Skipped corrupt wf file: \n" + e.getMessage());
        }
        return null;
    }

    /**
     * {@link WizardDialog} sub type to be adapted to the needs of a workflow execution.
     * 
     * @author Christian Weiss
     */
    private static final class WorkflowWizardDialog extends WizardDialog {

        /**
         * The Constructor.
         * 
         * @param activeShell the parent shell
         * @param workflowExecutionWizard the wizard this dialog is working on
         */
        public WorkflowWizardDialog(Shell activeShell, Wizard workflowExecutionWizard) {
            super(activeShell, workflowExecutionWizard);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            Button okButton = getButton(IDialogConstants.FINISH_ID);
            if (okButton != null) {
                okButton.setText(Messages.executionWizardFinishButtonLabel);
            }
        }

    }

}
