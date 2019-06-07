/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.tiglviewer.views;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementing class for integrating the native TIGLViewer into RCE.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 * @author Robert Mischke (changed binaries handling; code cleanup)
 * @author Jan Flink (changed thread and error handling, removed TiGL Viewer binaries, made it use an existing local TiGL Viewer
 *         installation)
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

    private Future<?> grabApplication;

    private CountDownLatch grabProcessLatch = new CountDownLatch(1);

    private CountDownLatch grabWindowLatch = new CountDownLatch(1);

    private Composite parentComposite;

    private File ctrlFile = null;

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

        showMessage("Waiting for the external TiGL Viewer application to be started...");
        ConfigurationSegment tiglViewerSegment = getTiGLViewerConfigurationSegment();

        String tiglViewerBinaryPath = tiglViewerSegment.getString("binaryPath");
        if (tiglViewerBinaryPath == null) {
            LOGGER.warn(
                "TiGL Viewer integration not configured (correctly). The 'binaryPath' key of the 'tiglViewer'"
                    + " configuration segment returned null.");
            showMessage(
                "The TiGL Viewer integration is not configured.\nPlease see the \"thirdPartyConfiguration\""
                    + " segment in the configuration reference for details.");
            return;
        }

        int startupTimeout;
        if (tiglViewerSegment.getInteger("startupTimeoutSeconds") != null) {
            startupTimeout = tiglViewerSegment.getInteger("startupTimeoutSeconds");
        } else {
            startupTimeout = START_PROCESS_TIMEOUT;
        }

        final boolean embedWindow;
        if (tiglViewerSegment.getBoolean("embedWindow") != null) {
            embedWindow = tiglViewerSegment.getBoolean("embedWindow");
        } else {
            embedWindow = true;
        }

        // create special window-title token as unique identifier
        final String windowTitleToken = Long.toString(new Random().nextLong(), RADIX);
        final String windowTitle = "TIGLViewer-" + windowTitleToken;

        final File exeFile = new File(tiglViewerBinaryPath);
        if (!exeFile.exists() || !exeFile.isFile() || !exeFile.canExecute()) {
            showMessage(StringUtils.format("TiGL Viewer executable not found at the configured path: %s\n"
                + "Please check the TiGL Viewer configuration. See the \"thirdPartyConfiguration\""
                + " segment in the configuration reference for details.", exeFile.getAbsolutePath()));
            LOGGER.error(StringUtils.format("TiGL Viewer executable not found at path: %s", exeFile.getAbsolutePath()));
            return;
        }
        if (!exeFile.canExecute()) {
            showMessage(StringUtils.format("The configured TiGL Viewer file is not executable: %s\n"
                + "Please check the file privileges.", exeFile.getAbsolutePath()));
            LOGGER.error(StringUtils.format("The configured TiGL Viewer file is not executable: %s", exeFile.getAbsolutePath()));
        }
        LOGGER.debug(StringUtils.format("Located TiGL Viewer executable at: %s", exeFile.getAbsolutePath()));

        ConcurrencyUtils.getAsyncTaskService().execute("Starting TiGL Viewer application.", () -> {

            generateControlFile();
            String secondaryViewId = TIGLViewer.this.getViewSite().getSecondaryId();

            final String exeFilePathString = convertToPathStringForExecution(exeFile);
            final String ctrlFilePathString = convertToPathStringForExecution(ctrlFile);

            String commonCommandPart = buildCommonCommandPart(embedWindow, windowTitle, exeFilePathString, ctrlFilePathString);

            final String sCommand;
            try {
                if (secondaryViewId == null) {
                    sCommand = commonCommandPart;
                    LOGGER.debug("Starting TiGL Viewer application without an input file.");
                } else {
                    secondaryViewId = secondaryViewId.replaceAll("&#38", ":");

                    File file = new File(secondaryViewId);
                    if (!file.exists()) {
                        throw new IOException("Could not find input file for the TiGL Viewer: " + secondaryViewId);
                    }

                    // TODO (p2): "secondId" is not being escaped - verify that is it always safe against injection
                    sCommand = commonCommandPart + " --filename " + DOUBLE_QUOTE + secondaryViewId + DOUBLE_QUOTE;
                    LOGGER.debug(StringUtils.format(
                        "Starting TiGL Viewer application with input file %s", secondaryViewId));
                }

                Runtime.getRuntime().exec(sCommand);
                if (!grabProcessLatch.await(startupTimeout, TimeUnit.SECONDS)) {
                    throw new InterruptedException("Unable to identify TiGL Viewer process ID");
                }
                if (!grabWindowLatch.await(LOAD_FILE_TIMEOUT, TimeUnit.SECONDS)) {
                    throw new InterruptedException(StringUtils.format(
                        "The TiGL Viewer application took too long to start. Timeout (%ss) reached.\n"
                            + "This can be caused by too large input files or general problems with the TiGL Viewer application.",
                        LOAD_FILE_TIMEOUT));
                }
            } catch (IOException | InterruptedException e) {
                Display.getDefault().asyncExec(
                    () -> showMessage("Unable to start the TiGL Viewer application.\n" + e.getMessage()));
                grabApplication.cancel(true);
                killTiglViewerProcess();
                disposeControlFile();
                LOGGER.error(e);
                Thread.currentThread().interrupt();
            }

        });

        grabApplication = ConcurrencyUtils.getAsyncTaskService()
            .scheduleAtFixedRateAfterDelay("Grabbing the external TiGL Viewer application window.", () -> {
                LOGGER.debug("Scanning for the TiGL Viewer applications process ID.");
                HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
                IntByReference pidReference = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidReference);
                processID = pidReference.getValue();
                if (processID != 0) {
                    grabProcessLatch.countDown();
                    LOGGER.debug(StringUtils.format("TiGL Viewer application process startet with process ID %s.", processID));
                    if (embedWindow) {
                        User32.INSTANCE.SetWindowLong(hwnd, OS.GWL_STYLE, User32.WS_CLIPSIBLINGS | User32.WS_VISIBLE);
                        Display.getDefault().asyncExec(() -> {
                            if (parentComposite.isDisposed()) {
                                return;
                            }
                            LOGGER.debug(StringUtils.format("Grabbing the external TiGL Viewer window with the title '%s'.", windowTitle));
                            clearMessage();
                            Composite nativeComposite = new Composite(parentComposite, SWT.EMBEDDED);
                            HWND nativeCompositeHwnd = new HWND();
                            nativeCompositeHwnd.setPointer(new Pointer(nativeComposite.handle));
                            User32.INSTANCE.SetParent(hwnd, nativeCompositeHwnd);
                            nativeComposite.pack();
                            nativeComposite.setBounds(nativeComposite.getParent().getBounds());
                            grabWindowLatch.countDown();
                        });
                    } else {
                        Display.getDefault().syncExec(() -> {
                            showMessage(StringUtils.format("TiGL Viewer started externally with the window title '%s'.\n"
                                + "Note: Closing this view also closes the external window.", windowTitle));
                            LOGGER.debug(StringUtils.format("TiGL Viewer started externally with the window title '%s'.", windowTitle));
                        });
                        grabWindowLatch.countDown();
                    }
                    grabApplication.cancel(true);
                }
            }, GRAB_PROCESS_THREAD_DELAY, GRAB_PROCESS_THREAD_DELAY);
    }

    private String buildCommonCommandPart(final boolean embedWindow, final String windowTitle, final String exeFilePathString,
        final String ctrlFilePathString) {
        StringBuilder commonCommandPart = new StringBuilder();
        commonCommandPart.append("cmd /C start ");
        if (embedWindow) {
            commonCommandPart.append("/MIN ");
        }
        commonCommandPart.append(exeFilePathString);
        commonCommandPart.append(" --windowtitle ");
        commonCommandPart.append(windowTitle);
        commonCommandPart.append(" --controlFile ");
        commonCommandPart.append(ctrlFilePathString);
        return commonCommandPart.toString();
    }

    private void generateControlFile() {
        String controlContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<TIGLViewer>\n"
            + "    <console display=\"0\"/>\n"
            + "    <toolbars display=\"1\"/>\n"
            + "</TIGLViewer>";


        try {
            ctrlFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename("controlfile.xml");
        } catch (IOException e) {
            LOGGER.error("Error generating temp file.", e);

        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ctrlFile.getAbsolutePath()))) {
            writer.write(controlContent);
        } catch (IOException e) {
            LOGGER.warn("Error generating controlfile for TiGL Viewer.", e);
        }
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

    private ConfigurationSegment getTiGLViewerConfigurationSegment() {
        final ServiceRegistryAccess serviceRegistry = ServiceRegistry.createAccessFor(this);
        final ConfigurationService confService = serviceRegistry.getService(ConfigurationService.class);
        return confService.getConfigurationSegment("thirdPartyIntegration/tiglViewer");
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
        if (grabApplication != null) {
            grabApplication.cancel(true);
        }
        killTiglViewerProcess();
        disposeControlFile();
    }

    private void disposeControlFile() {
        if (ctrlFile != null) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(ctrlFile);
            } catch (IOException e) {
                LOGGER.error("Failed to dispose controlfile: " + ctrlFile.getAbsolutePath());
            }
        }
    }

    private void killTiglViewerProcess() {
        if (processID != 0) {
            LOGGER.debug(StringUtils.format("Stopping and disposing the TiGL Viewer application with process ID %s.", processID));
            try {
                Runtime.getRuntime().exec("taskkill /F /PID " + processID);
                processID = 0;
            } catch (IOException e) {
                LOGGER.error(StringUtils.format("Unable to stop and dispose process with ID %s.", processID), e);
            }
        }
    }
}
