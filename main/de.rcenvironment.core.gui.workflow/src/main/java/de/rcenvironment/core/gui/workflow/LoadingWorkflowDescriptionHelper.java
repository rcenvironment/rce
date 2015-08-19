/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
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

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateUtils;
import de.rcenvironment.core.component.workflow.update.api.SimplePersistentWorkflowDescriptionUpdateService;

/**
 * Utility class for loading workflows.
 * 
 * @author Doreen Seider
 */
public final class LoadingWorkflowDescriptionHelper {

    /** pref key. */
    public static final String PREFS_KEY_UPDATEAUTOMATICALLY = "de.rcenvironment.rce.gui.workflow.editor.updateautomatically";

    private static final Log LOGGER = LogFactory.getLog(LoadingWorkflowDescriptionHelper.class);

    private LoadingWorkflowDescriptionHelper() {}

    /**
     * Loads a workflow from a given file and handles potential updates.
     * 
     * @param file .wf file
     * @param workspaceFile not null when the file is in the workspace
     * @param checkForUpdates check for component updates shall be performed
     * @return {@link WorkflowDescription}
     */
    public static WorkflowDescription loadWorkflowDescription(final File file, final IFile workspaceFile, boolean checkForUpdates) {

        boolean silentUpdate = true;

        try {
            if (checkForUpdates) {
                SimplePersistentWorkflowDescriptionUpdateService updateService = new SimplePersistentWorkflowDescriptionUpdateService();
                PersistentWorkflowDescription persistentDescription =
                    updateService
                        .createPersistentWorkflowDescription(
                            IOUtils.toString(getContent(file, workspaceFile), WorkflowConstants.ENCODING_UTF8),
                            Activator
                                .getInstance().getUser());

                boolean hasUpdate = updateService.isUpdateForWorkflowDescriptionAvailable(persistentDescription, false);
                boolean isUpdateNonSilent = hasUpdate;

                if (!hasUpdate) {
                    hasUpdate = updateService.isUpdateForWorkflowDescriptionAvailable(persistentDescription, true);
                }
                if (hasUpdate) {
                    if (isUpdateNonSilent) {

                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                IPreferenceStore prefs = Activator.getInstance().getPreferenceStore();
                                if (!prefs.getString(PREFS_KEY_UPDATEAUTOMATICALLY).equals(String.valueOf(true))) {
                                    MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
                                        PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                                        Messages.incompatibleVersionTitle,
                                        Messages.incompatibleVersionMessage,
                                        Messages.updateIncompatibleVersionSilently,
                                        false, prefs, PREFS_KEY_UPDATEAUTOMATICALLY);
                                    prefs.putValue(PREFS_KEY_UPDATEAUTOMATICALLY, String.valueOf(dialog.getToggleState()));
                                }
                            }
                        });
                    }
                    if (workspaceFile != null) {
                        updateWorkflow(persistentDescription, workspaceFile, isUpdateNonSilent);
                    } else if (file != null) {
                        updateWorkflow(persistentDescription, file, isUpdateNonSilent);
                    }
                }
            }
            return new WorkflowDescriptionPersistenceHandler()
                .readWorkflowDescriptionFromStream(getContent(file, workspaceFile), Activator.getInstance().getUser());
        } catch (IOException | CoreException | ParseException e) {
            handleError(LoadingWorkflowDescriptionHelper.getNameOfWorkflowFile(file, workspaceFile), silentUpdate, e);
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            handleError(LoadingWorkflowDescriptionHelper.getNameOfWorkflowFile(file, workspaceFile), silentUpdate, e);
            throw e;
        }
    }
    
    /**
     * Returns the path of the file passed, which is not null.
     * @param file {@link File} object
     * @param workspaceFile {@link IFile} object
     * @return absolute path of one of the file passed
     */
    public static String getNameOfWorkflowFile(File file, IFile workspaceFile) {
        if (file != null && file.exists()) {
            return file.getAbsolutePath();
        } else if (workspaceFile != null && workspaceFile.exists()) {
            return workspaceFile.getRawLocation().toOSString();
        } else {
            return "File doesn't exist";
        }
    }

    private static InputStream getContent(final File file, final IFile workspaceFile)
        throws FileNotFoundException, CoreException {
        InputStream inputStream = null;
        if (file != null) {
            inputStream = new FileInputStream(file);
        } else if (workspaceFile != null) {
            inputStream = workspaceFile.getContents();
        }
        return inputStream;
    }

    private static void handleError(String filename, boolean silent, final Throwable e) {
        final String message;
        if (silent) {
            LOGGER.error("Failed to open workflow: " + filename, e);
            message = Messages.silentWorkflowUpdateFailureMessage;
        } else {
            LOGGER.error("Failed to update workflow: " + filename, e);
            message = Messages.workflowUpdateFailureMessage;
        }
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.workflowUpdateFailureTitle,
                    String.format(message, e.getMessage()));
            }
        });

    }

    private static void updateWorkflow(final PersistentWorkflowDescription description, final IFile file,
        final boolean hasNonSilentUpdate) {
        try {
            if (hasNonSilentUpdate) {
                String backupFilename = PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(
                    file.getRawLocation().toOSString());
                FileUtils.copyFile(new File(file.getRawLocation().toOSString()),
                    new File(backupFilename + ".wf"));
                file.getProject().refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
            }
            InputStream tempInputStream = IOUtils.toInputStream(new SimplePersistentWorkflowDescriptionUpdateService()
                .performWorkflowDescriptionUpdate(description).getWorkflowDescriptionAsString(), WorkflowConstants.ENCODING_UTF8);
            file.setContents(tempInputStream, true, false, new NullProgressMonitor());
            tempInputStream.close();
        } catch (IOException e) {
            handleError(file.getRawLocation().toOSString(), hasNonSilentUpdate, e);
            throw new RuntimeException(e);
        } catch (CoreException e) {
            handleError(file.getRawLocation().toOSString(), hasNonSilentUpdate, e);
            throw new RuntimeException(e);
        }

    }

    private static void updateWorkflow(final PersistentWorkflowDescription description, final File file,
        final boolean hasNonSilentUpdate) {
        try {
            if (hasNonSilentUpdate) {
                String backupFilename = PersistentWorkflowDescriptionUpdateUtils.getFilenameForBackupFile(
                    file.getName());
                FileUtils.copyFile(file, new File(file.getParentFile().getAbsolutePath(), backupFilename + ".wf"));
            }
        } catch (IOException e) {
            handleError(file.getAbsolutePath(), hasNonSilentUpdate, e);
            throw new RuntimeException(e);
        }
        try (InputStream tempInputStream = IOUtils.toInputStream(new SimplePersistentWorkflowDescriptionUpdateService()
            .performWorkflowDescriptionUpdate(description).getWorkflowDescriptionAsString(), WorkflowConstants.ENCODING_UTF8)) {
            FileUtils.write(file, IOUtils.toString(tempInputStream));
            tempInputStream.close();
        } catch (IOException e) {
            handleError(file.getAbsolutePath(), hasNonSilentUpdate, e);
            throw new RuntimeException(e);
        }

    }
}
