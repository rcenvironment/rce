/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator; only used to store the symbolic bundle name.
 * 
 * @author Robert Mischke
 *
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        DiscoveryBootstrapServiceImpl.setSymbolicBundleName(context.getBundle().getSymbolicName());
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        // NOP
    }

}
