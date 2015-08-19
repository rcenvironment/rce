/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.runtime;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Geometry view of CPACS Writer component.
 * 
 * @author Markus Litz
 * @author Arne Bachmann
 * @author Markus Kunde
 * @author Sascha Zur
 */
public class CpacsGeomView extends AbstractCpacsRuntimeView implements Observer {

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog(CpacsGeomView.class);

    @Deprecated
    public CpacsGeomView() {
        super();
    }

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent, LOGGER);
    }

    /**
     * Open the given file in TIGLViewer.
     * 
     * @param file The file to open
     * @throws PartInitException
     */
    @Override
    protected synchronized void performShowAction(final File tempFile) {
        tempFile.setReadOnly();
        try {
            String secondId = null;
            try {
                secondId = tempFile.getCanonicalPath();
                secondId = secondId.replaceAll(":", "&#38");
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .showView("de.rcenvironment.cpacs.gui.tiglviewer.views.TIGLViewer",
                        secondId, IWorkbenchPage.VIEW_ACTIVATE);
            } catch (IOException e) {
                LOGGER.error(e);
            }
           
        } catch (final PartInitException e) {
            LOGGER.error(e);
            MessageDialog.openWarning(form.getShell(), "Could not open editor", e.getMessage());
        }
    }

    @Override
    public void update(Observable o, Object arg) {}

    @Override
    public void setFocus() {}
}
