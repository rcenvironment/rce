/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.configuration.bootstrap.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.bootstrap.ParameterException;
import de.rcenvironment.core.configuration.bootstrap.SystemExitException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.ui.ErrorTextUI;
import de.rcenvironment.core.utils.incubator.RuntimeDetection;

/**
 * Bundle activator that triggers the {@link BootstrapConfiguration} initialization.
 * 
 * @author Robert Mischke
 * @author Tobias Rodehutskors (added check for custom RCE launcher)
 */
public class Activator implements BundleActivator {

    // TODO this constant is also define in the de.rce.launcher.RCELauncherHelper, but we cannot import it from there. but why?
    private static final String PROP_RCE_LAUNCHER = "de.rcenvironment.launcher";

    private static final String RCE_LAUNCHER_NOT_USED_ERROR_MESSAGE = "RCE was not started with the RCE launcher.";

    @Override
    public void start(BundleContext arg0) {

        if (!RuntimeDetection.isRunningAsTest()) {
            // If we are in a testing environment, the following test doesn't make any sense and should be skipped.

            String schibboleth = System.getProperty(PROP_RCE_LAUNCHER);
            if (schibboleth == null || !schibboleth.equals(PROP_RCE_LAUNCHER)) {
                Logger.getLogger("bootstrap").log(Level.SEVERE, RCE_LAUNCHER_NOT_USED_ERROR_MESSAGE);
                new ErrorTextUI(RCE_LAUNCHER_NOT_USED_ERROR_MESSAGE).run();
                System.exit(0);
            }

        }

        try {
            BootstrapConfiguration.initialize();
        } catch (ProfileException | ParameterException e) {
            // TODO if a ProfileException is thrown while the Lanterna UI is already visible (in the profile selection dialog), a second
            // lanterna windows will be shown

            new ErrorTextUI(e.getMessage()).run();

            // log the stacktrace without introducing a dependency on non-JRE log packages
            Logger.getLogger("bootstrap").log(Level.SEVERE, "Error in early startup", e);
            System.exit(0);
        } catch (SystemExitException e) {
            System.exit(e.getExitCode());
        }
    }

    @Override
    public void stop(BundleContext arg0) {}
}
