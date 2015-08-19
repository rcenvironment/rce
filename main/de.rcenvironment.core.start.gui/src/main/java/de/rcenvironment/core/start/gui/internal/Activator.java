/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.start.common.Platform;
import de.rcenvironment.core.start.gui.GUIRunner;

/**
 * Simple activator to "inject" the GUI instance runner.
 * 
 * @author Robert Mischke
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext arg0) throws Exception {
        LogFactory.getLog(getClass()).debug("Initializing GUI runner");
        Platform.setRunner(new GUIRunner());
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {}

}
