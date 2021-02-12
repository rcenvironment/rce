/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.tiglviewer.gui.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.components.tiglviewer.execution.TiglViewerComponent;
import de.rcenvironment.components.tiglviewer.gui.runtime.TiglViewerRuntimeView;

/**
 * Simple activator to "inject" the runtime view.
 * 
 * @author Jan Flink
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext arg0) throws Exception {
        TiglViewerComponent.setRuntimeView(new TiglViewerRuntimeView());
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {}

}
