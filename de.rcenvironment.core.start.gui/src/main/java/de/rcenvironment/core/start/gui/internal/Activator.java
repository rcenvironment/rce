/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.gui.internal;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.start.gui.GUIInstanceRunner;

/**
 * Simple activator to "inject" the GUI instance runner.
 * 
 * @author Robert Mischke
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext arg0) throws Exception {
        LogFactory.getLog(getClass()).debug("Initializing GUI runner");
        Instance.setInstanceRunner(new GUIInstanceRunner());
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {}

}
