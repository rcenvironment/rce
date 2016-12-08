/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.toolkitbridge.api.ToolkitBridge;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.setup.ToolkitFactory;

/**
 * OSGi bundle activator.
 * 
 * @author Robert Mischke
 */
public class Activator implements BundleActivator {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * OSGi lifecycle method.
     * 
     * @param bundleContext the OSGi {@link BundleContext}
     * @throws Exception on bundle or toolkit errors
     */
    public void start(BundleContext bundleContext) throws Exception {

        log.debug("Creating toolkit instance");
        Toolkit toolkit = ToolkitFactory.create(new LiveToolkitConfiguration());

        ToolkitBridge.initialize(toolkit, bundleContext);

        log.debug("Toolkit initialized");
    }

    /**
     * OSGi lifecycle method.
     * 
     * @param context the OSGi {@link BundleContext}
     * @throws Exception on bundle or toolkit shutdown errors
     */
    public void stop(BundleContext context) throws Exception {
        ToolkitBridge.dispose();
    }

}
