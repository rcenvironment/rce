/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.commons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.gui.datamanagement.browser.Activator;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Utilities for data management tasks in the RCP workbench environment.
 * 
 * TODO review authors list; may be incomplete
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public final class DataManagementWorkbenchUtils {

    private static final DataManagementWorkbenchUtils INSTANCE = new DataManagementWorkbenchUtils();

    private final DataManagementService dataManagementService;

    private final Log log = LogFactory.getLog(DataManagementWorkbenchUtils.class);

    private DataManagementWorkbenchUtils() {
        dataManagementService = ServiceRegistry.createAccessFor(this).getService(DataManagementService.class);
    }

    public static DataManagementWorkbenchUtils getInstance() {
        return INSTANCE;
    }

    public DataManagementService getDataManagementService() {
        return dataManagementService;
    }

    // FIXME code review; purpose in this utility class?
    /**
     * Javadoc.
     * 
     * @param dataReferenceId the data management reference
     * @param fileReferencePath the reference to the temp file
     * @param filename the filename for the given data
     * @throws AuthorizationException :
     * @throws IOException Exception
     * @param rceNodeIdentifier {@link InstanceNodeSessionId} of the RCE node, which store the file to open
     */
    public void saveReferenceToFile(final String dataReferenceId, final String fileReferencePath,
        final String filename, final ResolvableNodeId rceNodeIdentifier) throws AuthorizationException, IOException {
        final File file = new File(filename);
        if (dataReferenceId != null && fileReferencePath == null) {
            try {
                dataManagementService.copyReferenceToLocalFile(dataReferenceId, file, rceNodeIdentifier);
            } catch (CommunicationException e) {
                throw new RuntimeException(StringUtils.format("Failed to copy data reference from remote node @%s to local file: ",
                    rceNodeIdentifier)
                    + e.getMessage(), e);
            }
        } else if (dataReferenceId == null && fileReferencePath != null) {
            File tempFile = new File(fileReferencePath);
            Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } else if (dataReferenceId != null && fileReferencePath != null) {
            log.debug("When saving file both data reference ID and file reference path are set. Only one of these should be set.");
        } else if (dataReferenceId == null && fileReferencePath == null) {
            log.debug("When saving file neither data reference ID nor file reference path are set. One of these should be set.");
        }
    }

    /**
     * Tries to open a data management reference in a read-only workbench text editor.
     * 
     * @param dataReferenceId the data management reference
     * @param fileReferencePath the reference to the temp file
     * @param filename the filename to use for the given data
     * @param rceNodeIdentifier {@link InstanceNodeSessionId} of the RCE node, which store the file to open
     * @param inTiglViewer true if CPACS File should be opened in TiGL Viewer
     */
    public void tryOpenDataReferenceInReadonlyEditor(final String dataReferenceId, final String fileReferencePath,
        final String filename, final ResolvableNodeId rceNodeIdentifier, final boolean inTiglViewer) {

        if (dataReferenceId != null && fileReferencePath == null) {
            // open = copy to local temporary file + open in editor
            final Job openJob = new Job("Loading data") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    File tempFile = null;
                    try {
                        // acquire local temporary file with the associated filename

                        File tempDir = new File(Activator.getInstance().getBundleSpecificTempDir(), dataReferenceId);
                        tempDir.mkdir();
                        if (!tempDir.mkdir() && !tempDir.exists()) {
                            log.error("Temp directory could not be created or did already exist as file: " + tempDir);
                            return Status.OK_STATUS;
                        }
                        tempFile = new File(tempDir, filename);
                        if (!(tempDir.exists() && tempDir.list().length == 1 && tempDir.list()[0].equals(filename))) {
                            // copy data reference content to local temporary file
                            dataManagementService.copyReferenceToLocalFile(dataReferenceId, tempFile,
                                rceNodeIdentifier);
                        }

                        if (inTiglViewer) {
                            openInTigl(tempFile);

                        } else {
                            openInEditor(tempFile);
                        }
                    } catch (AuthorizationException e) {
                        log.error("Failed to copy datamanagement reference to local file.", e);
                    } catch (IOException e) {
                        log.error("Failed to copy datamanagement reference to local file.", e);
                    } catch (CommunicationException e) {
                        throw new RuntimeException(StringUtils.format(
                            "Failed to copy data reference from remote node @%s to local file: ",
                            rceNodeIdentifier)
                            + e.getMessage(), e);
                    }
                    return Status.OK_STATUS;
                }

            };
            openJob.setUser(true);
            openJob.schedule();
        } else if (dataReferenceId == null && fileReferencePath != null) {
            // otherwise handle it as a file directly
            // open = open in editor directly
            final Job openJob = new Job("Loading data") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    if (inTiglViewer) {
                        openInTigl(new File(fileReferencePath));

                    } else {
                        openInEditor(new File(fileReferencePath));
                    }

                    return Status.OK_STATUS;
                }

            };
            openJob.setUser(true);
            openJob.schedule();
        } else if (dataReferenceId != null && fileReferencePath != null) {
            MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Open in Editor",
                "Failed to open data in editor. Refresh the workflow entry and try again.");
            log.error("When opening in editor both data reference ID and file reference path are set. Only one of these should be set.");
        } else if (dataReferenceId == null && fileReferencePath == null) {
            MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Open in Editor",
                "Failed to open data in editor. Refresh the workflow entry and try again.");
            log.warn("When opening in editor neither data reference ID nor file reference path are set. One of these should be set.");
        }
    }

    private void openInEditor(final File tempFile) {
        // best-effort try to make the file read-only; the actual outcome is ignored
        tempFile.setWritable(false);
        // open in editor
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    String fileName      = tempFile.getName();
                    String fileExtension = EditorsHelper.getExtension(fileName); 
                    if (fileExtension.equals("wf")){
                        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(tempFile.getAbsolutePath()));
                        
                        final IEditorInput editorInput = new FileStoreEditorInput(fileStore);
                        final IEditorPart editor       = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                            .openEditor(editorInput, "de.rcenvironment.rce.gui.workflow.editor.ReadOnlyWorkflowEditor");
                        if (editor != null) { // if internal editor
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                            .showView("org.eclipse.ui.views.PropertySheet");
                            // close editor on RCE shutdown as the file-property relation is gone after RCE is started again
                            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
                
                                @Override
                                public boolean preShutdown(IWorkbench workbench, boolean arg1) {
                                    editor.getSite().getPage().closeEditor(editor, false);
                                    return true;
                                }
                
                                @Override
                                public void postShutdown(IWorkbench workbench) {}
                            });
                        }
                    } else {
                        EditorsHelper.openExternalFileInEditor(tempFile);
                    }
                } catch (final PartInitException e) {
                    log.error("Failed to open datamanagement reference copied to local file in an editor.", e);
                }
            }
        });
    }
    
    
    private void openInTigl(final File tempFile) {

        tempFile.setWritable(false);
        // open in TiGL
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    String secondId = null;
                    try {

                        secondId = tempFile.getCanonicalPath();
                        secondId = secondId.replaceAll(":", "&#38");

                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                            .showView("de.rcenvironment.core.gui.tiglviewer.views.TIGLViewer",
                                secondId, IWorkbenchPage.VIEW_ACTIVATE);

                    } catch (IOException e) {
                        log.error(e);
                    }

                } catch (final PartInitException e) {
                    log.error(e);
                    log.error("Failed to open datamanagement reference copied to local file in the TiGL.", e);
                }
            }
        });

    }

}
