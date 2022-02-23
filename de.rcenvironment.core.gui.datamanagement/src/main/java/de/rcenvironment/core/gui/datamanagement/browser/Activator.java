/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Activates the Bundle.
 * 
 * @author Doreen Seider
 */
public class Activator extends AbstractUIPlugin {

    private static Activator instance = null;
    
    private static File bundleSpecificTempDir;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        bundleSpecificTempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(bundleSpecificTempDir);
        instance = null;
    }
    
    public static Activator getInstance() {
        return instance;
    }
    
    /**
     * Provides a temp directory, which should be used in this bundle as root dir in all cases a temp directory or file must be created. If
     * the temp doesn't exist for some reason, it will be created anew.
     * 
     * @return the temp dir
     */
    public synchronized File getBundleSpecificTempDir() {
        if (!(bundleSpecificTempDir.exists() && bundleSpecificTempDir.isDirectory())) {
            try {
                bundleSpecificTempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
            } catch (IOException e) {
                LogFactory.getLog(getClass()).error("creating temp dir for dm gui bundle failed", e);
            }
        }
        return bundleSpecificTempDir;
    }

}
