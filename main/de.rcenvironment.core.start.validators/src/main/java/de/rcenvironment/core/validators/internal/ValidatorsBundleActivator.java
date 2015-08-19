/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.validators.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * {@link BundleActivator} of this bundle providing OSGi services.
 * 
 * @author Christian Weiss
 */
public class ValidatorsBundleActivator implements BundleActivator {
    
    /** The Bundle-SymbolicName of the current bundle. */
    public static String bundleSymbolicName;
    
    private static BundleContext bundleContext;
    
    @Override
    public void start(BundleContext context) throws Exception {
        ValidatorsBundleActivator.bundleContext = context;
        bundleSymbolicName = ValidatorsBundleActivator.bundleContext.getBundle().getSymbolicName();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ValidatorsBundleActivator.bundleContext = null;
    }
}
