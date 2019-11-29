/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start;

import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionService;

/**
 * This class represents the default application.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public class Application implements IApplication {

    // note: when changing this message, check whether the Instance Management stdout listener needs backwards compatibility code
    private static final String STDOUT_MESSAGE_EARLY_STARTUP_COMPLETE = "Early startup complete, running main application";

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Object start(IApplicationContext context) throws Exception {

        org.eclipse.core.runtime.Platform.addLogListener(new EclipseLogListener());

        // check command-line options for validity; TODO convert into validator?
        if (CommandLineArguments.hasConfigurationErrors()) {
            log.error("Error parsing command-line options; shutting down");
            // no Eclipse return value for "error", so just terminate
            return IApplication.EXIT_OK;
        }

        Instance.setHeadless(CommandLineArguments.isHeadlessModeRequested());
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

        // working around the CheckStyle rule here as this is one of the few cases where StdOut is actually correct - misc_ro
        PrintStream sysOut = System.out;
        sysOut.println(STDOUT_MESSAGE_EARLY_STARTUP_COMPLETE);

        final StatusCollectionService statusCollectionService = StaticToolkitHolder.getService(StatusCollectionService.class);
        final String statusLineOutputIndent = "  ";
        final String statusLineOutputSeparator = "\n";

        log.debug(statusCollectionService.getCollectedDefaultStateInformation().asMultilineString(
            "Application state after early startup:", statusLineOutputIndent, statusLineOutputSeparator, null));

        InstanceRunner instanceRunner = Instance.getInstanceRunner();
        int runnerResult = instanceRunner.run();

        log.info("Main application shutdown complete, exit code: " + runnerResult);

        return runnerResult;
    }

    @Override
    public void stop() {
        Instance.shutdown();
    }

}
