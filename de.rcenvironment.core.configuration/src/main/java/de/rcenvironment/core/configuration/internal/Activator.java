/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.bootstrap.LaunchParameters;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Activator for the "de.rcenvironment.core.configuration" bundle.
 * 
 * @author Robert Mischke
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // define the global ServiceRegistryAccessFactory for components and GUI code,
        // using this bundle's context as the fallback context; note: moved to a bundle activator
        // (from a service activator) to ensure operation even if OSGi-DS initialization fails.
        ServiceRegistry.setAccessFactory(new OsgiServiceRegistryAccessFactory(context));

        // parse command-line options
        CommandLineArguments.initialize(LaunchParameters.getInstance().getTokens().toArray(new String[0]));
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {

    }

}
