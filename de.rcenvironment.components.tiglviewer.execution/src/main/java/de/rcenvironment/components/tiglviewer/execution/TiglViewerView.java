/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.tiglviewer.execution;

import java.io.File;

/**
 * Abstract base class for the TiGL Viewer runtime view.
 * 
 * The concrete implementation is injected into {@link TiglViewerComponent}. This avoids dependencies from the components execution code to
 * GUI code.
 * 
 * @author Jan Flink
 */
public abstract class TiglViewerView {

    /**
     * Opens the given file in a TiGL Viewer instance and shows it within RCE.
     * 
     * @param file The file to show in TiGL Viewer.
     */
    public abstract void showView(File file);

}
