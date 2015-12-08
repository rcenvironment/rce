/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.documentation;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;

import de.rcenvironment.core.component.integration.ToolIntegrationDocumentationService;
import de.rcenvironment.core.gui.utils.common.EditorsHelper;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Helper class for showing the documentation of an integrated tool.
 * 
 * @author Sascha Zur
 */
public final class ToolIntegrationDocumentationGUIHelper {

    private static ToolIntegrationDocumentationGUIHelper instance = new ToolIntegrationDocumentationGUIHelper();

    private static AtomicBoolean isCurrentlyLoading = new AtomicBoolean(false);

    /**
     * Open documentation of an integrated tool, if it exists. If multiple versions of the
     * documentation exist, show a dialog to select the wanted documentation.
     * 
     * @param toolIdentifier of the component with version.
     */
    public void showComponentDocumentation(final String toolIdentifier) {
        ServiceRegistryPublisherAccess serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        final ToolIntegrationDocumentationService tids =
            serviceRegistryAccess.getService(ToolIntegrationDocumentationService.class);
        Map<String, String> componentInstallationsWithDocumentation = tids.getComponentDocumentationList(toolIdentifier);
        if (componentInstallationsWithDocumentation.size() == 1) {
            Entry<String, String> documentationEntry = componentInstallationsWithDocumentation.entrySet().iterator().next();
            final String hashValue = documentationEntry.getKey();
            final String nodeID = documentationEntry.getValue();
            setupJob(toolIdentifier, tids, hashValue, nodeID);
        } else if (componentInstallationsWithDocumentation.size() > 1) {
            ToolIntegrationDocumentationChooserDialog chooser =
                new ToolIntegrationDocumentationChooserDialog(new Shell(Display.getCurrent()),
                    componentInstallationsWithDocumentation, toolIdentifier);
            chooser.create();

            if (chooser.open() == 0 && chooser.getSelectedHash() != null) {
                final String hashValue = chooser.getSelectedHash();
                final String nodeID = componentInstallationsWithDocumentation.get(hashValue);
                setupJob(toolIdentifier, tids, hashValue, nodeID);
            }
        } else {
            MessageBox noDocumentationBox = new MessageBox(Display.getDefault().getActiveShell());
            noDocumentationBox.setText("No Documentation");
            noDocumentationBox.setMessage("Documentation for selected tool not available in the current network.");
            noDocumentationBox.open();
        }
    }

    private void setupJob(final String toolIdentifier, final ToolIntegrationDocumentationService tids, final String hashValue,
        final String nodeID) {
        Job job = new Job("Tool Documentation") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Fetching tool documentation", 2);

                downloadAndOpenDocumentation(toolIdentifier, tids, nodeID, hashValue, monitor);
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void downloadAndOpenDocumentation(String identifier, ToolIntegrationDocumentationService tids, String nodeID,
        String hashValue, IProgressMonitor monitor) {
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
        isCurrentlyLoading.set(false);
    }

    public static ToolIntegrationDocumentationGUIHelper getInstance() {
        return instance;
    }

}
