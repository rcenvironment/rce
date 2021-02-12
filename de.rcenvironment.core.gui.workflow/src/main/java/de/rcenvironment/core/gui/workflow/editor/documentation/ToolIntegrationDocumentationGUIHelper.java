/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.documentation;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.integration.documentation.ToolIntegrationDocumentationService;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Helper class for showing the documentation of an integrated tool.
 * 
 * @author Sascha Zur
 * @author Brigitte Boden (added support for remote access components)
 */
public final class ToolIntegrationDocumentationGUIHelper {

    private static ToolIntegrationDocumentationGUIHelper instance = new ToolIntegrationDocumentationGUIHelper();

    private static AtomicBoolean isCurrentlyLoading = new AtomicBoolean(false);

    /**
     * Open documentation of an integrated tool, if it exists. If multiple versions of the documentation exist, show a dialog to select the
     * wanted documentation.
     * 
     * @param toolIdentifier of the component with version.
     * @param export if set to true, the documentation will not be opened, but exported to the file system.
     */
    public void showComponentDocumentation(final String toolIdentifier, boolean export) {
        ServiceRegistryPublisherAccess serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        final ToolIntegrationDocumentationService tids =
            serviceRegistryAccess.getService(ToolIntegrationDocumentationService.class);
        Map<String, String> componentInstallationsWithDocumentation = tids.getComponentDocumentationList(toolIdentifier);
        if (componentInstallationsWithDocumentation.size() == 1) {
            Entry<String, String> documentationEntry = componentInstallationsWithDocumentation.entrySet().iterator().next();
            final String hashValue = documentationEntry.getKey();
            final String nodeID = documentationEntry.getValue();
            setupJob(toolIdentifier, tids, hashValue, nodeID, export);
        } else if (componentInstallationsWithDocumentation.size() > 1) {
            ToolIntegrationDocumentationChooserDialog chooser =
                new ToolIntegrationDocumentationChooserDialog(new Shell(Display.getCurrent()),
                    componentInstallationsWithDocumentation, toolIdentifier);
            chooser.create();

            if (chooser.open() == 0 && chooser.getSelectedHash() != null) {
                final String hashValue = chooser.getSelectedHash();
                final String nodeID = componentInstallationsWithDocumentation.get(hashValue);
                setupJob(toolIdentifier, tids, hashValue, nodeID, export);
            }
        } else {
            MessageBox noDocumentationBox = new MessageBox(Display.getDefault().getActiveShell());
            noDocumentationBox.setText("No Documentation");
            noDocumentationBox.setMessage("Documentation for selected tool not available in the current network.");
            noDocumentationBox.open();
        }
    }

    private void setupJob(final String toolIdentifier, final ToolIntegrationDocumentationService tids, final String hashValue,
        final String nodeID, boolean export) {
        Job job = new Job("Tool Documentation") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Fetching tool documentation", 2);

                downloadAndOpenOrExportDocumentation(toolIdentifier, tids, nodeID, hashValue, monitor, export);
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void downloadAndOpenOrExportDocumentation(String identifier, ToolIntegrationDocumentationService tids, String nodeID,
        String hashValue, IProgressMonitor monitor, boolean export) {
        if (isCurrentlyLoading.get()) {
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    MessageBox alreadyLoading = new MessageBox(Display.getDefault().getActiveShell());
                    alreadyLoading.setText("Loading Documentation");
                    alreadyLoading.setMessage("Another documentation is aready loading.");
                    alreadyLoading.open();
                }
            });
            return;
        }
        isCurrentlyLoading.set(true);
        monitor.worked(1);
        File documentationDir = null;
        try {
            documentationDir = tids.getToolDocumentation(identifier, nodeID, hashValue);
        } catch (RemoteOperationException | IOException e1) {
            monitor.worked(1);
            isCurrentlyLoading.set(false);
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    MessageBox errorDownload = new MessageBox(Display.getDefault().getActiveShell());
                    errorDownload.setText("Download failed");
                    errorDownload.setMessage("Download of the documentation failed.\nCause: " + e1.getMessage());
                    errorDownload.open();
                }
            });
            return;
        }
        monitor.worked(1);
        if (export) {
            exportDocumentationToFileSystem(documentationDir);
        } else {
            openDocumentationInEditor(documentationDir);
        }
        isCurrentlyLoading.set(false);
    }

    private void exportDocumentationToFileSystem(File documentationDir) {

        if (documentationDir != null) {
            File[] listFiles = documentationDir.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                // Standard case: 1 documentation file
                // Open file dialog
                if (listFiles.length == 1) {
                    File fileToSave = listFiles[0];

                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            FileDialog fileDialog =
                                new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
                            fileDialog.setText("Save tool documentation to file system...");
                            fileDialog.setFileName(fileToSave.getName());
                            String path = fileDialog.open();
                            if (path == null) {
                                return;
                            }
                            File targetFile = new File(path);
                            try {
                                FileUtils.copyFile(fileToSave, targetFile);
                            } catch (IOException e) {
                                LogFactory.getLog(ToolIntegrationDocumentationGUIHelper.class).error("Could not save documentation: ", e);
                            }
                        }
                    });

                } else {
                    // More than one file in documentation folder, open directory dialog
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            DirectoryDialog dirDialog =
                                new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
                            dirDialog.setText("Save tool documentation to file system...");
                            String path = dirDialog.open();
                            if (path == null) {
                                return;
                            }
                            File targetDir = new File(path);
                            try {
                                for (File f : listFiles) {
                                    if (f.isFile()) {
                                        FileUtils.copyFileToDirectory(f, targetDir);
                                    }
                                }
                            } catch (IOException e) {
                                LogFactory.getLog(ToolIntegrationDocumentationGUIHelper.class).error("Could not save documentation: ", e);
                            }
                        }
                    });
                }
            }
        }
    }

    private void openDocumentationInEditor(File documentationDir) {
        if (documentationDir != null) {
            File[] listFiles = documentationDir.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                File toOpen = null;
                for (File f : listFiles) {
                    if (f.isFile()) {
                        toOpen = f;
                        break;
                    }
                }
                if (toOpen != null) {
                    final File openFile = toOpen;
                    openFile.setReadOnly();
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                EditorsHelper.openExternalFileInEditor(openFile);
                            } catch (PartInitException e) {
                                LogFactory.getLog(ToolIntegrationDocumentationGUIHelper.class).error("Could not open documentation: ",
                                    e);
                            }
                        }
                    });

                }
            }
        }
    }

    public static ToolIntegrationDocumentationGUIHelper getInstance() {
        return instance;
    }

}
