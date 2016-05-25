/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.io.File;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.execution.spi.WorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Loads {@link WorkflowDescription} from a {@link File} or an {@link IFile} with some GUI-specific handling like updating the workspace
 * after updated was performed.
 * 
 * @author Doreen Seider
 */
public final class GUIWorkflowDescriptionLoaderCallback implements WorkflowDescriptionLoaderCallback {

    /** Preferences key. */
    public static final String PREFS_KEY_UPDATEAUTOMATICALLY = "de.rcenvironment.rce.gui.workflow.editor.updateautomatically";
    
    private static final String WORKFLOW_FILE_ERROR = "Workflow File Error";

    private IFile wfFileFromWorkspace = null;
    
    public GUIWorkflowDescriptionLoaderCallback() {
    }
    
    public GUIWorkflowDescriptionLoaderCallback(IFile wfFile) {
        wfFileFromWorkspace = wfFile;
    }

    @Override
    public void onNonSilentWorkflowFileUpdated(String message, final String backupFilename) {
        refreshWorkflowIfLoadedFromWorkspace();
        Display.getDefault().asyncExec(new Runnable() {
            
            @Override
            public void run() {
                IPreferenceStore prefs = Activator.getInstance().getPreferenceStore();
                if (!prefs.getString(PREFS_KEY_UPDATEAUTOMATICALLY).equals(String.valueOf(true))) {
                    MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        Messages.incompatibleVersionTitle,
                        StringUtils.format(Messages.incompatibleVersionMessage, backupFilename),
                        Messages.updateIncompatibleVersionSilently,
                        false, prefs, PREFS_KEY_UPDATEAUTOMATICALLY);
                    prefs.putValue(PREFS_KEY_UPDATEAUTOMATICALLY, String.valueOf(dialog.getToggleState()));
                }
            }
        });
    }

    @Override
    public void onSilentWorkflowFileUpdated(String message) {
        refreshWorkflowIfLoadedFromWorkspace();
    }
    
    @Override
    public boolean arePartlyParsedWorkflowConsiderValid() {
        return true;
    }

    @Override
    public void onWorkflowFileParsingPartlyFailed(final String backupFilename) {
        refreshWorkflowIfLoadedFromWorkspace();
        Display.getDefault().asyncExec(new Runnable() {
            
            @Override
            public void run() {
                MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                    WORKFLOW_FILE_ERROR, StringUtils.format("Failed to parse the workflow. "
                        + " Most likely reasons:\n\na) Workflow file was opened with a newer version of RCE before.\nb) An integrated tool "
                        + "has changed its inputs/outputs.\n\nSome parts of the workflow were skipped. See log for more details."
                        + "\n\nA backup file was created: %s", backupFilename));
            }
        });
    }
    
    private void refreshWorkflowIfLoadedFromWorkspace() {
        if (wfFileFromWorkspace != null) {
            try {
                wfFileFromWorkspace.getProject().refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
            } catch (CoreException e) {
                LogFactory.getLog(getClass()).error(StringUtils.format(
                    "Failed to refresh the workspace after workflow file '%s' was updated", 
                    wfFileFromWorkspace.getRawLocation().toOSString()));
            }
        }
    }

}
