/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.start.common.Instance;
import de.rcenvironment.core.start.common.InstanceRunner;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;

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
        InstanceRunner instanceRunner = Instance.getInstanceRunner();
        int runnerResult = instanceRunner.run();

        final List<String> statsLines = StatsCounter.getFullReportAsStandardTextRepresentation();
        final String mergedStatsLines = org.apache.commons.lang3.StringUtils.join(statsLines, "\n");
        log.debug("Statistics counters on Application shutdown:\n" + mergedStatsLines);

        log.debug("Thread pool state on Application shutdown:\n" + SharedThreadPool.getInstance().getFormattedStatistics(true, true));

        return runnerResult;
    }

    @Override
    public void stop() {
        Instance.shutdown();
    }

}
