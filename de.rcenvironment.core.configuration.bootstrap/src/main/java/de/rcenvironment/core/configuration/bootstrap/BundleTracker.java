/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Logging bundle events used to be an implicit feature of pax-logging 1.x. Since the migration to 2.x, however, this does not seem to be
 * installed by default anymore. Instead of registering an instance of the similar org.ops4j.pax.logging.spi.support.FrameworkHandler class,
 * we are doing it ourselves here, which allows customizing the output. -- misc_ro, Feb 2020
 * 
 * @author Robert Mischke
 */
public final class BundleTracker implements BundleListener {

    private final Log log = LogFactory.getLog(BundleTracker.class);

    private BundleTracker() {}

    /**
     * Installs a global listener instance; should be called exactly once.
     */
    public static void install() {
        BundleContext context = FrameworkUtil.getBundle(BundleTracker.class).getBundleContext();
        context.addBundleListener(new BundleTracker());
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        final String eventDescription;
        switch (event.getType()) {
        case BundleEvent.INSTALLED:
            eventDescription = "INSTALLED";
            break;
        case BundleEvent.RESOLVED:
            eventDescription = "RESOLVED";
            break;
        case BundleEvent.STARTING:
            eventDescription = "STARTING";
            break;
        case BundleEvent.STARTED:
            eventDescription = "STARTED";
            break;
        case BundleEvent.STOPPING:
            eventDescription = "STOPPING";
            break;
        case BundleEvent.STOPPED:
            eventDescription = "STOPPED";
            break;
        case BundleEvent.UNINSTALLED:
            eventDescription = "UNINSTALLED";
            break;
        case BundleEvent.UPDATED:
            eventDescription = "UPDATED";
            break;
        case BundleEvent.UNRESOLVED:
            eventDescription = "UNRESOLVED";
            break;
        default:
            eventDescription = "[other event: " + event.getType() + "]";
            break;
        }
        // note: not logging the bundle state here, as it is not generally useful; add if needed for debugging
        String message = StringUtils.format("%s %S", event.getBundle(), eventDescription);
        if (event.getBundle() != event.getOrigin()) {
            // A typical case where "origin" is different is when a bundle is being loaded via simpleconfigurator (which is then the
            // "origin"). As this happens on an earlier OSGi start level, this is typically not seen by this listener, though.
            message = message + ", origin=" + event.getOrigin();
        }
        log.debug(message);
    }
}
