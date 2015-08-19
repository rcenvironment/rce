/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.tiglviewer.views;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.TCHAR;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

/**
 * Implementing class for integrating the native TIGLViewer into RCE.
 * 
 * @author Markus Litz
 * @author Markus Kunde
 */
@SuppressWarnings("restriction")
public class TIGLViewer extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.rcenvironment.cpacs.gui.tiglviewer.views.TIGLViewer";

    private static final int THREAD_SLEEP = 250;

    private static final int RADIX = 36;

    private static final String SLASH = "/";

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
        Composite nativeComposite = new Composite(parent, SWT.EMBEDDED);
        String sCommand;

        this.window = this.getViewSite().getWorkbenchWindow();
        String secondId = this.getViewSite().getSecondaryId();

        // Build special window-title token as unique identifier
        String token = Long.toString(Math.abs(new Random().nextLong()), RADIX);
        String title = "TIGLViewer-" + token;

        String bundlePath = Platform.getBundle("de.rcenvironment.cpacs.gui.tiglviewer.win32").getLocation();
        bundlePath = bundlePath.split("reference:file:")[1];
        if (bundlePath.startsWith(SLASH)) {
            bundlePath = bundlePath.substring(1);
        }
        if (bundlePath.endsWith(SLASH)) {
            bundlePath = bundlePath.substring(0, bundlePath.length() - 1);
        }
        bundlePath = bundlePath + "\"";
        bundlePath = bundlePath.replaceFirst(SLASH, "\"/");

        try {
            if (secondId == null) {
                sCommand = "cmd /C start /MIN " + bundlePath + "/lib/TIGLViewer.exe --windowtitle " + title
                    + " --controlFile " + bundlePath + "/resources/controlfile.xml";
                LOGGER.debug("TIGLViewer: starting TIGLViewer application without file: ");
            } else {
                secondId = secondId.replaceAll("&#38", ":");

                File file = new File(secondId);
                if (!file.exists()) {
                    return;
                }
                sCommand = "cmd /C start /MIN " + bundlePath + "/lib/TIGLViewer.exe --windowtitle " + title
                    + " --controlFile " + bundlePath + "/resources/controlfile.xml --filename \"" + secondId + "\"";
                LOGGER.debug("TIGLViewer: starting TIGLViewer application with file: " + secondId);
            }

            Runtime.getRuntime().exec(sCommand);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        int[] i = { 0, 0 };
        TCHAR tChrTitle = new TCHAR(0, title, true);
        int handle = 0;

        try {
            while (processID == 0) {
                Thread.sleep(THREAD_SLEEP);
                handle = (int) OS.FindWindow(null, tChrTitle);
                OS.GetWindowThreadProcessId(handle, i);
                this.processID = i[0];
                this.threadID = i[1];
                LOGGER.debug("TIGLViewer process startet. PID:" + processID + "  -  TID:" + threadID);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }

        OS.SetWindowLong(handle, OS.GWL_STYLE, OS.WS_VISIBLE | OS.WS_CLIPCHILDREN | OS.WS_CLIPSIBLINGS);
        OS.SetParent(handle, nativeComposite.handle);
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
