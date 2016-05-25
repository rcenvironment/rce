/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.tiglviewer.execution;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.api.LocalExecutionOnly;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * TiGLViewer RCE component.
 * 
 * @author Markus Kunde
 * @author Jan Flink
 */
@LocalExecutionOnly
public class TiglViewerComponent extends DefaultComponent {

    private static final Log LOG = LogFactory.getLog(TiglViewerComponent.class);

    private static TiglViewerView view;

    private ComponentContext componentContext;

    private ComponentDataManagementService dataManagementService;

    private File tempFile = null;

    private boolean firstTime = true;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);

        // Start TiGLViewer runtime view bundle if GUI is available
        Bundle guiBundle = Platform.getBundle("de.rcenvironment.components.tiglviewer.gui");
        try {
            if (guiBundle != null && guiBundle.getState() != Bundle.ACTIVE) {
                LOG.debug("Starting bundle: " + guiBundle);
                guiBundle.start();
            }
        } catch (BundleException e) {
            LOG.error("Failed to start bundle: " + guiBundle, e);
        }
    }

    @Override
    public void processInputs() throws ComponentException {

        TypedDatum td = componentContext.readInput("TiGL Viewer File");

        if (td instanceof FileReferenceTD) {
            FileReferenceTD inputFile = (FileReferenceTD) td;

            if (firstTime) {
                // firstTime logic cannot be done in prepare() because only on this stage filename for temp file is available.
                // TiGLViewer is aware of incoming filename.
                try {
                    tempFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(inputFile.getFileName());
                } catch (IOException e) {
                    throw new ComponentException("Failed to create temporary geometry file required by TiGL Viewer", e);
                }
                try {
                    dataManagementService.copyReferenceToLocalFile(inputFile.getFileReference(), tempFile,
                        componentContext.getDefaultStorageNodeId());
                } catch (IOException e) {
                    throw new ComponentException("Failed to load geometry file into temporary file used by TiGL Viewer", e);
                }
                if (view != null) {
                    view.showView(tempFile);
                } else {
                    throw new ComponentException("Failed to show geometry in TiGL Viewer - most likely,"
                        + " because the TiGL Viewer is not supported on your operating system");
                }

                firstTime = false;
            } else {
                try {
                    dataManagementService.copyReferenceToLocalFile(inputFile.getFileReference(), tempFile,
                        componentContext.getDefaultStorageNodeId());
                } catch (IOException e) {
                    throw new ComponentException("Failed to update geometry in temporary file used by TiGL Viewer", e);
                }
            }

            componentContext.writeOutput("TiGL Viewer File", td);
        }

    }

    /**
     * Injects the TiGL Viewer runtime view.
     * 
     * @param newView The runtime view.
     */
    public static synchronized void setRuntimeView(TiglViewerView newView) {

        if (view != null) {
            LogFactory.getLog(TiglViewerComponent.class).warn("Tried to set TiGL Viewer Runtime View (" + newView + ")"
                + " when one is already configured: " + view);
        }
        view = newView;
    }
}
