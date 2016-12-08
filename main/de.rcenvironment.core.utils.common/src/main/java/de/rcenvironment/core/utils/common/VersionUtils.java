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

    // TODO this class needs refactoring: initialization should be done once, and the release types should be an enum - misc_ro

    /**
     * Release build type.
     */
    public static final String VERSION_TYPE_RELEASE = "Release";

    /**
     * Snapshot build type.
     */
    public static final String VERSION_TYPE_SNAPSHOT = "Snapshot";

    /**
     * RC build type.
     */
    public static final String VERSION_TYPE_RELEASE_CANDIDATE = "Release_Candidate";

    /**
     * Development build type, e.g. running from an IDE.
     */
    public static final String VERSION_TYPE_DEVELOPMENT = "Development";

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
     * @return the OSGi version of the RCE "product" determined via the product specific branding bundles, <code>null</code> if it could not
     *         be determined
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

        String versionNumber = version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
        String versionQualifier = version.getQualifier();
        String versionType = getVersionType(versionQualifier);
        if (versionType == VERSION_TYPE_RELEASE) {
            return versionNumber;
        } else {
            return StringUtils.format("%s_%s", versionNumber, versionType);
        }
    }

    /**
     * Detects the version/build type from a given bundle version qualifier.
     * 
     * TODO convert the return value to an enum
     * 
     * @param versionQualifier the qualifier of the tested OSGi bundle version
     * @return a string matching the version type
     */
    public static String getVersionType(String versionQualifier) {
        final String suffixSnapshot = "_SNAPSHOT";
        final String suffixRC = "_RC";
        final String suffixQualifier = "qualifier";

        String versionType = VERSION_TYPE_RELEASE; // default
        if (versionQualifier.endsWith(suffixRC)) {
            versionType = VERSION_TYPE_RELEASE_CANDIDATE;
        } else if (versionQualifier.endsWith(suffixSnapshot)) {
            versionType = VERSION_TYPE_SNAPSHOT;
        } else if (versionQualifier.endsWith(suffixQualifier)) {
            versionType = VERSION_TYPE_DEVELOPMENT;
        }
        return versionType;
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

    /**
     * @return true if the core bundles indicate a release or RC build
     */
    public static boolean isReleaseOrReleaseCandidateBuild() {
        // TODO as stated above, this class needs refactoring - misc_ro
        Version version = getVersionOfCoreBundles(); // TODO review: better to use the product version instead?
        String versionQualifier = version.getQualifier();
        String versionType = getVersionType(versionQualifier);
        return VERSION_TYPE_RELEASE.equals(versionType) || VERSION_TYPE_RELEASE_CANDIDATE.equals(versionType);
    }

}
