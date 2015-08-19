/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * Utility class for version-related operations.
 * 
 * @author Robert Mischke
 * @author Jan Flink
 */
public final class VersionUtils {


    private static final String VERSION_INFO_RCE_STANDARD = "de.rcenvironment.core.gui.branding.default.versioninfo";

    private static final String VERSION_INFO_RCE_CPACS = "de.rcenvironment.cpacs.branding";

    private static final String VERSION_INFO_RCE_TRANSPORT = "de.rcenvironment.transport.branding";

    private static final String PLATFORM_BUNDLES_PREFIX = "de.rcenvironment.platform.";

    private static final Class<?> OWN_CLASS = VersionUtils.class;

    private VersionUtils() {}

    /**
     * @return the OSGi version of the RCE "core" bundles, or null if it could not be determined
     */
    public static Version getVersionOfCoreBundles() {
        Bundle ownBundle = FrameworkUtil.getBundle(OWN_CLASS);
        if (ownBundle == null) {
            return null;
        }
        Version coreVersion = ownBundle.getVersion();
        return coreVersion;
    }

    /**
     * @return the OSGi version of the RCE "platform" bundles, or null if it could not be determined
     */
    public static Version getVersionOfPlatformBundles() {
        Log log = LogFactory.getLog(OWN_CLASS); // not a static field to conserve memory
        Bundle ownBundle = FrameworkUtil.getBundle(OWN_CLASS);
        Bundle[] bundles = ownBundle.getBundleContext().getBundles();
        Version version = null;
        if (bundles == null) {
            log.error("Unexpected error: 'null' bundle list while getting platform version");
        } else {
            // find all platform bundles by their symbolic name
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().startsWith(PLATFORM_BUNDLES_PREFIX)) {
                    Version newVersion = bundle.getVersion();
                    // all bundles must have the same version
                    if (version != null && !version.equals(newVersion)) {
                        log.error("Found more that one platform version: " + newVersion + " and " + version);
                    }
                    version = newVersion;
                }
            }
        }
        return version;
    }

    /**
     * @return the OSGi version of the RCE "product" determined via the product specific branding
     *         bundles, null if it could not be determined
     */
    public static Version getVersionOfProduct() {
        Log log = LogFactory.getLog(OWN_CLASS); // not a static field to conserve memory
        Bundle ownBundle = FrameworkUtil.getBundle(OWN_CLASS);
        Bundle[] bundles = ownBundle.getBundleContext().getBundles();
        Version version = null;
        if (bundles == null) {
            log.error("Unexpected error: 'null' bundle list while getting product version");
        } else {
            // find bundles by their symbolic name
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().startsWith(VERSION_INFO_RCE_STANDARD)) {
                    version = bundle.getVersion();
                    break;
                } else if (bundle.getSymbolicName().startsWith(VERSION_INFO_RCE_CPACS)) {
                    version = bundle.getVersion();
                    break;
                } else if (bundle.getSymbolicName().startsWith(VERSION_INFO_RCE_TRANSPORT)) {
                    version = bundle.getVersion();
                    break;
                }
            }
        }
        return version;
    }

}
