/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.tiglviewer.gui.runtime;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.OS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.components.tiglviewer.execution.TiglViewerView;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Shows a TiGL Viewer as view within RCE.
 * 
 * @author Jan FLink
 */
public class TiglViewerRuntimeView extends TiglViewerView {

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog(TiglViewerRuntimeView.class);

    /**
     * Opens the given file in TiGLViewer.
     * 
     * @param tempFile The input file for the TiGL Viewer
     */
    @Override
    public void showView(File tempFile) {
        try {
            final String secondId = tempFile.getCanonicalPath().replaceAll(":", "&#38");

            IWorkbench wb = PlatformUI.getWorkbench();
            IWorkbenchWindow[] windows = wb.getWorkbenchWindows();
            IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
            if (window == null) {
                window = windows[0];
            }
            final IWorkbenchPage page = window.getActivePage();

            SharedThreadPool.getInstance().execute(new Runnable() {

                @Override
                @TaskDescription("Starting TiGLViewer")
                public void run() {
                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                if (OS.isFamilyWindows()) {
                                    page.showView("de.rcenvironment.core.gui.tiglviewer.views.TIGLViewer",
                                        secondId, IWorkbenchPage.VIEW_ACTIVATE);
                                }
                            } catch (PartInitException e) {
                                LOGGER.error(e);
                            }
                        }
                    });
                }
            });
        } catch (RuntimeException e) {
            LOGGER.error("TiGLViewer component cannot open TiGLViewer. Maybe no GUI available?");
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

}
