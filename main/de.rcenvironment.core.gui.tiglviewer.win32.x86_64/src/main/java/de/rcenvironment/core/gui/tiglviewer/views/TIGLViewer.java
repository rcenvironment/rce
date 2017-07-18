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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.TCHAR;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Implementing class for integrating the native TIGLViewer into RCE.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 * @author Robert Mischke (changed binaries handling; code cleanup)
 */
@SuppressWarnings("restriction")
public class TIGLViewer extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.rcenvironment.core.gui.tiglviewer.views.TIGLViewer";

    private static final int THREAD_SLEEP = 250;

    private static final int RADIX = 36;

    private static final String DOUBLE_QUOTE = "\"";

    private static final String REGEXP_QUOTED_BACKSLASH = "\\\\";

    /**
     * Logger instance.
     */
    private static final Log LOGGER = LogFactory.getLog(TIGLViewer.class);

    /** Workbench window. */
    public IWorkbenchWindow window;

    /** process ID. **/
    private int processID = 0;

    /** thread ID. **/
    private int threadID = 0;

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

        if (org.apache.commons.exec.OS.isFamilyWindows()) {

            final File unpackedFilesLocation;
            try {
                unpackedFilesLocation = getUnpackedFilesLocation();
            } catch (ConfigurationException e) {
                // TODO (p2): improve error handling?
                LOGGER.error("Failed to locate TiGLViewer binaries: " + e.toString());
                return;
            }

            Composite nativeComposite = new Composite(parent, SWT.EMBEDDED);

            this.window = this.getViewSite().getWorkbenchWindow();
            String secondaryViewId = this.getViewSite().getSecondaryId();

            // create special window-title token as unique identifier
            final String windowTitleToken = Long.toString(Math.abs(new Random().nextLong()), RADIX);
            final String windowTitle = "TIGLViewer-" + windowTitleToken;

            final File exeFile = new File(unpackedFilesLocation, "binaries/TIGLViewer.exe");
            final File ctrlFile = new File(unpackedFilesLocation, "controlfile.xml");

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
                    LOGGER.debug("TIGLViewer: starting TIGLViewer application without file");
                } else {
                    secondaryViewId = secondaryViewId.replaceAll("&#38", ":");

                    File file = new File(secondaryViewId);
                    if (!file.exists()) {
                        // TODO (p3) improve error handling?
                        return;
                    }

                    // TODO (p2): "secondId" is not being escaped - verify that is it always safe against injection
                    sCommand = commonCommandPart + " --filename " + DOUBLE_QUOTE + secondaryViewId + DOUBLE_QUOTE;
                    LOGGER.debug(
                        "TIGLViewer: starting TIGLViewer application with file " + secondaryViewId + " (derived from secondary view id)");
                }

                Runtime.getRuntime().exec(sCommand);
            } catch (IOException e) {
                // FIXME (p1) - check: not aborting in this case? (potentially related: issue 0015334)
                LOGGER.error(e);
            }

            int[] i = { 0, 0 };
            TCHAR tChrTitle = new TCHAR(0, windowTitle, true);
            long handle = 0;

            try {
                while (processID == 0) {
                    Thread.sleep(THREAD_SLEEP);
                    handle = OS.FindWindow(null, tChrTitle);
                    OS.GetWindowThreadProcessId((int) handle, i);
                    this.processID = i[0];
                    this.threadID = i[1];
                    LOGGER.debug("TIGLViewer process startet. PID:" + processID + "  -  TID:" + threadID);
                }
            } catch (InterruptedException e) {
                // FIXME (p2) - check: not aborting in this case?
                LOGGER.error(e);
            }

            OS.SetWindowLongPtr((int) handle, OS.GWL_STYLE, OS.WS_VISIBLE | OS.WS_CLIPCHILDREN | OS.WS_CLIPSIBLINGS);
            OS.SetParent((int) handle, nativeComposite.handle);

        } else {
            Image image = ImageManager.getInstance().getSharedImage(StandardImages.TIGL_ICON);
            Label label = new Label(parent, SWT.NONE);
            label.setText("\n  The TiGL Viewer is not supported by the current operating system");
            Label imageLabel = new Label(parent, SWT.NONE);
            imageLabel.setImage(image);
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
            Runtime.getRuntime().exec("taskkill /F /PID " + this.processID);
            LOGGER.debug("Killing TIGLViewer process");
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }
}
