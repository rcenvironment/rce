/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.registration.internal;

import java.util.Dictionary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * {@link BundleListener} to regognize when new {@link Bundle}s providing a {@link Component} are
 * installed in oder to start this {@link Bundle}.
 * 
 * @author Doreen Seider
 */
public class ComponentBundleListener implements BundleListener {

    /** The logger for this class. */
    private static final Log LOGGER = LogFactory.getLog(ComponentBundleListener.class);

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.INSTALLED) {
            ComponentBundleListener.handleBundle(event.getBundle());
        }
    }

    /**
     * Checks the given {@link Bundle} if it provides a {@link Component} and declare that in its
     * Manifest and starts it if it does.
     * 
     * @param bundle The given bundle to check and possibly to start.
     */
    @SuppressWarnings("unchecked")
    public static void handleBundle(Bundle bundle) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String componentEntry = headers.get(ComponentConstants.MANIFEST_ENTRY_RCE_COMPONENT);
        if (componentEntry != null && Boolean.valueOf(componentEntry)) {
            if (bundle.getState() == Bundle.RESOLVED) {
                try {
                    bundle.start();
                } catch (BundleException e) {
                    LOGGER.error("Starting a Bundle providng an integrated Component failed: " + bundle.getSymbolicName());
                }
            }
        }
    }
}
