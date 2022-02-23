/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Standard bundle activator.
 * 
 * @author Robert Mischke
 */
public class Activator extends AbstractUIPlugin {

    private static Activator instance = null;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        instance = null;
    }

    public static Activator getInstance() {
        return instance;
    }
}
