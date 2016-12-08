/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.configuration.bootstrap.internal;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.bootstrap.LogArchiver;

/**
 * Bundle activator that triggers the {@link BootstrapConfiguration} initialization.
 * 
 * @author Robert Mischke
 * @author Tobias Rodehutskors (added check for custom RCE launcher)
 */
public class Activator implements BundleActivator {

    // TODO this constant is also define in the de.rce.launcher.RCELauncherHelper, but we cannot import it from there. but why?
    private static final String PROP_RCE_LAUNCHER = "de.rcenvironment.launcher";

    @Override
    public void start(BundleContext arg0) {

        String schibboleth = System.getProperty(PROP_RCE_LAUNCHER);
        if (schibboleth == null || !schibboleth.equals(PROP_RCE_LAUNCHER)) {
            Logger.getLogger("bootstrap").log(Level.SEVERE, "RCE was not started with the RCE launcher.");
            System.exit(1);
        }
        
        try {
            BootstrapConfiguration.initialize();
            LogArchiver.run(BootstrapConfiguration.getInstance().getProfileDirectory());
        } catch (IOException e) {
            // circumvent CheckStyle rule to print fatal errors before the log system is initialized
            PrintStream sysErr = System.err;
            sysErr.println("Error: " + e.getMessage());
            // log the stacktrace without introducing a dependency on non-JRE log packages
            Logger.getLogger("bootstrap").log(Level.SEVERE, "Error in early startup", e);
            System.exit(1);
        }
    }

    @Override
    public void stop(BundleContext arg0) {}
}
