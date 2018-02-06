/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.tiglviewer.views;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.TCHAR;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementing class for integrating the native TIGLViewer into RCE.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 * @author Robert Mischke (changed binaries handling; code cleanup)
 * @author Jan Flink (changed thread and error handling)
 */
@SuppressWarnings("restriction")
public class TIGLViewer extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.rcenvironment.core.gui.tiglviewer.views.TIGLViewer";

    /**
     * Logger instance.
     */
    private static final Log LOGGER = LogFactory.getLog(TIGLViewer.class);

    private static final int RADIX = 36;

    private static final String DOUBLE_QUOTE = "\"";

    private static final String REGEXP_QUOTED_BACKSLASH = "\\\\";

    private static final int GRAB_PROCESS_THREAD_DELAY = 250;

    private static final int START_PROCESS_TIMEOUT = 10;

    /** process ID. **/
    private int processID = 0;

    private Future<?> grabProcessID;

    private CountDownLatch grabProcessLatch = new CountDownLatch(1);

    private Composite parentComposite;

    /**
     * The constructor.
     */
    public TIGLViewer() {}

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     * 
     * @param parent Composite parent
     */
    @Override
    public void createPartControl(Composite parent) {
        this.parentComposite = parent;
        if (org.apache.commons.exec.OS.isFamilyWindows()) {
            startTiGLViewerApplication();
        } else {
            showMessage("The TiGL Viewer integration is not supported by the current operating system");
        }
    }

    private void startTiGLViewerApplication() {
        showMessage("Starting TiGL Viewer application...");
        final File unpackedFilesLocation;
        try {
            unpackedFilesLocation = getUnpackedFilesLocation();
        } catch (ConfigurationException e) {
            LOGGER.error("Failed to locate TiGL Viewer binaries: " + e.toString());
            showMessage("Failed to locate TiGL Viewer binaries. Please see the log file for more details.");
            return;
        }

        // create special window-title token as unique identifier
        final String windowTitleToken = Long.toString(Math.abs(new Random().nextLong()), RADIX);
        final String windowTitle = "TIGLViewer-" + windowTitleToken;

        final File exeFile = new File(unpackedFilesLocation, "binaries/TIGLViewer.exe");
        final File ctrlFile = new File(unpackedFilesLocation, "controlfile.xml");

        ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

            @TaskDescription("Starting TiGL Viewer application.")
            @Override
            public void run() {
                String secondaryViewId = TIGLViewer.this.getViewSite().getSecondaryId();

                // note: it would be an internal product error if these files were missing, so RTEs are sufficient - misc_ro
                verifyFileExistence(exeFile);
                verifyFileExistence(ctrlFile);

                LOGGER.debug("Located TiGLViewer binary at " + exeFile.getAbsolutePath());

                final String exeFilePathString = convertToPathStringForExecution(exeFile);
                final String ctrlFilePathString = convertToPathStringForExecution(ctrlFile);

                final String commonCommandPart = "cmd /C start /MIN " + exeFilePathString + " --windowtitle " + windowTitle
                    + " --controlFile " + ctrlFilePathString;

                final String sCommand;
                try {
                    if (secondaryViewId == null) {
                        sCommand = commonCommandPart;
                        LOGGER.debug("Starting TIGL Viewer application without an input file.");
                    } else {
                        secondaryViewId = secondaryViewId.replaceAll("&#38", ":");

                        File file = new File(secondaryViewId);
                        if (!file.exists()) {
                            throw new IOException("Could not find input file for the TiGL Viewer: " + secondaryViewId);
                        }

                        // TODO (p2): "secondId" is not being escaped - verify that is it always safe against injection
                        sCommand = commonCommandPart + " --filename " + DOUBLE_QUOTE + secondaryViewId + DOUBLE_QUOTE;
                        LOGGER.debug(
                            "Starting TIGL Viewer application with input file " + secondaryViewId
                                + " (derived from secondary view id)");
                    }

                    Runtime.getRuntime().exec(sCommand);
                    if (!grabProcessLatch.await(START_PROCESS_TIMEOUT, TimeUnit.SECONDS)) {
                        throw new InterruptedException("Unable to start TiGL Viewer process");
                    }
                } catch (IOException | InterruptedException e) {
                    grabProcessID.cancel(true);
                    LOGGER.error(e);
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            showMessage("Unable to start the TiGL Viewer application. Please see the log file for more details.");
                        }
                    });
                }
            }
        });

        grabProcessID = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedRateAfterDelay(new Runnable() {

            private final TCHAR tChrTitle = new TCHAR(0, windowTitle, true);

            private final int[] i = { 0, 0 };

            @TaskDescription("Grabbing the TiGL Viewer applications process ID.")
            public void run() {
                LOGGER.debug("Waiting for TiGL Viewer applications process ID.");
                final long handle = OS.FindWindow(null, tChrTitle);
                OS.GetWindowThreadProcessId((int) handle, i);
                processID = i[0];
                if (processID != 0) {
                    LOGGER.debug("TiGL Viewer application process startet with process ID:" + processID);

                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            if (parentComposite.isDisposed()) {
                                return;
                            }
                            clearMessage();
                            Composite nativeComposite = new Composite(parentComposite, SWT.EMBEDDED);
                            OS.SetWindowLongPtr((int) handle, OS.GWL_STYLE,
                                OS.WS_VISIBLE | OS.WS_CLIPSIBLINGS);
                            OS.SetParent((int) handle, nativeComposite.handle);
                            nativeComposite.pack();
                            nativeComposite.setBounds(nativeComposite.getParent().getBounds());
                        }
                    });
                    grabProcessLatch.countDown();
                    grabProcessID.cancel(true);
                }
            }
        }, GRAB_PROCESS_THREAD_DELAY, GRAB_PROCESS_THREAD_DELAY);
    }

    private void showMessage(String message) {
        if (parentComposite.isDisposed()) {
            return;
        }
        clearMessage();
        Composite c = new Composite(parentComposite, SWT.NONE);
        c.setLayout(new GridLayout(1, false));
        Label label = new Label(c, SWT.NONE);
        label.setText(message);
        parentComposite.pack();
    }

    private void clearMessage() {
        for (Control c : parentComposite.getChildren()) {
            c.dispose();
        }
    }

    private File getUnpackedFilesLocation() throws ConfigurationException {
        final ServiceRegistryAccess serviceRegistry = ServiceRegistry.createAccessFor(this);
        final ConfigurationService confService = serviceRegistry.getService(ConfigurationService.class);
        return confService.getUnpackedFilesLocation("tiglviewer");
    }

    private void verifyFileExistence(final File exeFile) {
        if (!exeFile.isFile()) {
            // TODO (p2) use "consistency error" exception here once available
            throw new RuntimeException("Missing expected file " + exeFile);
        }
    }

    private String convertToPathStringForExecution(File file) {
        // execution path hack that inserts double quotes around the path, but excludes the drive letter and colon;
        // cleaned up code-wise, but the approach itself is unchanged from original code - misc_ro
        return file.getAbsolutePath().replaceFirst(REGEXP_QUOTED_BACKSLASH, DOUBLE_QUOTE + REGEXP_QUOTED_BACKSLASH)
            + DOUBLE_QUOTE;
    }

    @Override
    public void setFocus() {
        // Nothing to do here.
    }

    @Override
    public void dispose() {
        try {
            if (grabProcessID != null) {
                grabProcessID.cancel(true);
            }
            if (processID != 0) {
                LOGGER.debug("Killing the TiGL Viewer application process with PID: " + processID);
                Runtime.getRuntime().exec("taskkill /F /PID " + processID);
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }
}
