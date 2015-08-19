/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.start.common.CommandLineArguments;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.start.common.Platform;

/**
 * This class represents the default application.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public class Application implements IApplication {

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Object start(IApplicationContext context) throws Exception {

        org.eclipse.core.runtime.Platform.addLogListener(new EclipseLogListener());
        
        try {
            String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
            CommandLineArguments.parseArguments(args);
        } catch (IllegalArgumentException e) {
            log.error("Error parsing command-line options", e);
            // no Eclipse return value for "error", so just terminate
            return IApplication.EXIT_OK;
        }

        Platform.setHeadless(CommandLineArguments.isHeadlessModeRequested());
        Bundle currentBundle = FrameworkUtil.getBundle(getClass());
        if (currentBundle != null) {
            log.debug("Running from common launcher bundle " + currentBundle);
        } else {
            throw new IllegalStateException("Internal error: Failed to get launcher bundle");
        }
        final String instanceRunnerBundle;
        if (CommandLineArguments.isHeadlessModeRequested()) {
            instanceRunnerBundle = "de.rcenvironment.core.start.headless";
            // / TODO check: what was this line for? - misc_ro
            // context.applicationRunning();
        } else {
            instanceRunnerBundle = "de.rcenvironment.core.start.gui";
        }
        for (Bundle bundle : currentBundle.getBundleContext().getBundles()) {
            if (instanceRunnerBundle.equals(bundle.getSymbolicName())) {
                log.debug("Starting specific launcher bundle " + bundle);
                bundle.start();
                break;
            }
        }
        InstanceRunner instanceRunner = Platform.getRunner();
        return instanceRunner.run();
    }

    @Override
    public void stop() {
        Platform.shutdown();
    }

}
