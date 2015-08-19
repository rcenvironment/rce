/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.commons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.gui.datamanagement.browser.Activator;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Utilities for data management tasks in the RCP workbench environment.
 * 
 * TODO review authors list; may be incomplete
 * 
 * @author Robert Mischke
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
     * @param user the proxy certificate to resolve the reference
     * @throws AuthorizationException :
     * @throws IOException Exception
     * @param rceNodeIdentifier {@link NodeIdentifier} of the RCE node, which store the file to open
     */
    public void saveReferenceToFile(final String dataReferenceId, final String fileReferencePath,
        final String filename, final User user, final NodeIdentifier rceNodeIdentifier) throws AuthorizationException, IOException {
        final File file = new File(filename);
        if (dataReferenceId != null && fileReferencePath == null) {
            dataManagementService.copyReferenceToLocalFile(user, dataReferenceId, file, rceNodeIdentifier);
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
     * @param user the proxy certificate to resolve the reference
     * @param rceNodeIdentifier {@link NodeIdentifier} of the RCE node, which store the file to open
     * @param inTiglViewer true if CPACS File should be opened in TiGL Viewer
     */
    public void tryOpenDataReferenceInReadonlyEditor(final String dataReferenceId, final String fileReferencePath,
        final String filename,
        final User user, final NodeIdentifier rceNodeIdentifier, final boolean inTiglViewer) {

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
                            dataManagementService.copyReferenceToLocalFile(user, dataReferenceId, tempFile,
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
                    
                    if (inTiglViewer){
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
            log.debug("When opening in editor both data reference ID and file reference path are set. Only one of these should be set.");
        } else if (dataReferenceId == null && fileReferencePath == null) {
            log.debug("When opening in editor neither data reference ID nor file reference path are set. One of these should be set.");
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

                    EditorsHelper.openExternalFileInEditor(tempFile);
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
                            .showView("de.rcenvironment.cpacs.gui.tiglviewer.views.TIGLViewer",
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
