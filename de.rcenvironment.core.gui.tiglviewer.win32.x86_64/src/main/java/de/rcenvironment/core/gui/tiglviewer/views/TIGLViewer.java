/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
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

    private static final int LOAD_FILE_TIMEOUT = 90;

    /** process ID. **/
    private int processID = 0;

    private Future<?> grabProcessID;

    private CountDownLatch grabProcessLatch = new CountDownLatch(1);

    private CountDownLatch loadFileLatch = new CountDownLatch(1);

    private Composite parentComposite;


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
        showMessage("Starting TiGL Viewer application and loading TiGL Viewer input file...");
        final File unpackedFilesLocation;
        try {
            unpackedFilesLocation = getUnpackedFilesLocation();
        } catch (ConfigurationException e) {
            LOGGER.error("Failed to locate TiGL Viewer binaries: " + e.toString());
            showMessage("Failed to locate TiGL Viewer binaries. Please see the log file for more details.");
            return;
        }

        // create special window-title token as unique identifier
        final String windowTitleToken = Long.toString(new Random().nextLong(), RADIX);
        final String windowTitle = "TIGLViewer-" + windowTitleToken;


        ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

            @TaskDescription("Starting TiGL Viewer application.")
            @Override
            public void run() {
                final File exeFile = new File(unpackedFilesLocation, "binaries/TIGLViewer.exe");
                final File ctrlFile = new File(unpackedFilesLocation, "controlfile.xml");
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
                        LOGGER.debug(StringUtils.format(
                            "Starting TIGL Viewer application with input file %s", secondaryViewId));
                    }

                    Runtime.getRuntime().exec(sCommand);
                    if (!grabProcessLatch.await(START_PROCESS_TIMEOUT, TimeUnit.SECONDS)) {
                        throw new InterruptedException("Unable to start TiGL Viewer process");
                    }
                    if (!loadFileLatch.await(LOAD_FILE_TIMEOUT, TimeUnit.SECONDS)) {
                        throw new InterruptedException(StringUtils.format(
                            "TiGL Viewer application took too long for loading the input file. Timeout (%ss) reached.", LOAD_FILE_TIMEOUT));
                    }
                } catch (IOException | InterruptedException e) {
                    Display.getDefault().asyncExec(
                        () -> showMessage("Unable to start the TiGL Viewer application. Please see the log file for more details."));
                    grabProcessID.cancel(true);
                    killTiglViewerProcess();
                    LOGGER.error(e);
                    Thread.currentThread().interrupt();
                }
            }
        });

        grabProcessID = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedRateAfterDelay(new Runnable() {


            @TaskDescription("Grabbing the TiGL Viewer applications process ID.")
            public void run() {
                LOGGER.debug("Waiting for TiGL Viewer applications process ID.");
                HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
                IntByReference pidReference = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidReference);
                processID = pidReference.getValue();
                if (processID != 0) {
                    grabProcessLatch.countDown();
                    LOGGER.debug(StringUtils.format("TiGL Viewer application process startet with process ID %s.", processID));
                    User32.INSTANCE.SetWindowLong(hwnd, OS.GWL_STYLE, User32.WS_CLIPSIBLINGS | User32.WS_VISIBLE);

                    Display.getDefault().asyncExec(() -> {
                        if (parentComposite.isDisposed() || processID == 0) {
                            return;
                        }
                        clearMessage();
                        Composite nativeComposite = new Composite(parentComposite, SWT.EMBEDDED);
                        HWND nativeCompositeHwnd = new HWND();
                        nativeCompositeHwnd.setPointer(new Pointer(nativeComposite.handle));
                        User32.INSTANCE.SetParent(hwnd, nativeCompositeHwnd);
                        nativeComposite.pack();
                        nativeComposite.setBounds(nativeComposite.getParent().getBounds());
                    });
                    loadFileLatch.countDown();
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
            throw new RuntimeException("Missing expected file " + exeFile.getAbsolutePath());
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
        if (grabProcessID != null) {
            grabProcessID.cancel(true);
        }
        killTiglViewerProcess();
    }

    private void killTiglViewerProcess() {
        if (processID != 0) {
            LOGGER.debug(StringUtils.format("Stopping and disposing the TiGL Viewer application process with process ID %s.", processID));
            try {
                Runtime.getRuntime().exec("taskkill /F /PID " + processID);
                processID = 0;
            } catch (IOException e) {
                LOGGER.error(StringUtils.format("Unable to stop and dispose process with ID %s.", processID), e);
            }
        }
    }
}
