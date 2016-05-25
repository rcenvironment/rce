/*
 * Copyright (C) 2006-2016 DLR, Germany
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
 * @author Doreen Seider
 */
public final class VersionUtils {

    private static final String VERSION_INFO_RCE_STANDARD = "de.rcenvironment.core.gui.branding.default.versioninfo";

    private static final String PLATFORM_BUNDLES_PREFIX = "de.rcenvironment.platform.";

    private static final Class<?> OWN_CLASS = VersionUtils.class;

    private VersionUtils() {}

    /**
     * @return the OSGi version of the RCE "core" bundles, or <code>null</code> if it could not be determined
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
     * @return the OSGi version of the RCE "platform" bundles, or <code>null</code> if it could not be determined
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
     *         bundles, <code>null</code> if it could not be determined
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
                }
            }
        }
        return version;
    }

    /**
     * @param version {@link Version} to parse
     * @return version number of the {@link Version} given + optional type of version (RC, Snapshot)
     */
    public static String getVersionAsString(Version version) {
        final String suffixSnapshot = "_SNAPSHOT";
        final String suffixRC = "_RC";
        final String suffixQualifier = "qualifier";
        
        String versionNumber = version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
        String versionQualifier = version.getQualifier();
        String versionType = null;
        if (versionQualifier.endsWith(suffixRC)) {
            versionType = "Release_Candidate";
        } else if (versionQualifier.endsWith(suffixSnapshot)) {
            versionType = "Snapshot";
        } else if (versionQualifier.endsWith(suffixQualifier)) {
            versionType = "Development";
        }
        if (versionType == null) {
            return versionNumber;
        } else {
            return StringUtils.format("%s_%s", versionNumber, versionType);            
        }
    }
    
    /**
     * @param version {@link Version} containing the build ID
     * @return build ID of the {@link Version} given or <code>null</code> if no one exists
     */
    public static String getBuildIdAsString(Version version) {
        if (version.getQualifier().endsWith("qualifier")) {
            return null;
        } else {
            return version.getQualifier().split("^" + version.getMajor() + "\\." + version.getMinor() + "\\." + version.getMicro())[0];
        }
    }

}
