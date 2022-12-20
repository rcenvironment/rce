/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Utility class for version-related operations. This class differentiates between three types of versions:
 * <ul>
 * <li>Core Version: The version of the bundle <code>de.rcenvironment.core.utils.common</code></li>
 * <li>Platform Version: The version of the bundles starting with <code>de.rcenvironment.platform.</code></li>
 * <li>Product Version: The version of the bundle <code>de.rcenvironment.core.gui.branding.default.versioninfo</code></li>
 * </ul>
 * 
 * @author Robert Mischke
 * @author Jan Flink
 * @author Doreen Seider
 * @author Alexander Weinert
 */
public final class VersionUtils {

    private enum VersionType {

        RELEASE(null, "Release"), // Release versions are not identified by a suffix of their qualifier
        SNAPSHOT("_SNAPSHOT", "Snapshot"),
        RELEASE_CANDIDATE("_RC", "Release_Candidate"),
        DEVELOPMENT("qualifier", "Development"); // used, e.g., when running from an IDE

        private final String suffix;

        private final String displayName;

        VersionType(String suffix, String displayName) {
            this.suffix = suffix;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }

        public boolean isRelease() {
            return this.equals(RELEASE);
        }

        public boolean isReleaseCandidate() {
            return this.equals(RELEASE_CANDIDATE);
        }

        public boolean hasBuildId() {
            return !this.equals(DEVELOPMENT);
        }

        /**
         * Detects the version/build type from a given bundle version qualifier.
         * 
         * @param versionQualifier the qualifier of the tested OSGi bundle version
         * @return The VersionType corresponding to the given qualifier
         */
        public static VersionType fromVersion(Version version) {
            final String qualifier = version.getQualifier();
            if (qualifier.endsWith(VersionType.RELEASE_CANDIDATE.suffix)) {
                return VersionType.RELEASE_CANDIDATE;
            } else if (qualifier.endsWith(VersionType.SNAPSHOT.suffix)) {
                return VersionType.SNAPSHOT;
            } else if (qualifier.endsWith(VersionType.DEVELOPMENT.suffix)) {
                return VersionType.DEVELOPMENT;
            }
            return VersionType.RELEASE;
        }
    }

    private static final Class<?> OWN_CLASS = VersionUtils.class;

    private static final BundleAccessor BUNDLE_ACCESSOR = new BundleAccessor(OWN_CLASS);

    private VersionUtils() {}

    /**
     * @return the OSGi version of the RCE "core" bundles, or <code>null</code> if it could not be determined
     */
    public static Version getCoreBundleVersion() {
        Bundle ownBundle = BUNDLE_ACCESSOR.getOwnBundleOrLogDebug();
        if (ownBundle == null) {
            return null;
        }

        return ownBundle.getVersion();
    }

    /**
     * @return the OSGi version of the RCE "platform" bundles, or <code>null</code> if it could not be determined
     */
    public static Version getPlatformVersion() {
        return getPlatformVersion(BUNDLE_ACCESSOR, VersionUtils::logError);
    }

    /**
     * Delegate for getPlatformVersion(). Created for testing, as this delegate takes the BundleAccessor as a parameter, making it an easily
     * testable pure function
     */
    static Version getPlatformVersion(BundleAccessor accessor, Consumer<String> logErrorParam) {
        final Set<Version> platformVersions = accessor.getPlatformBundles()
            .map(Bundle::getVersion)
            .collect(Collectors.toSet());

        if (platformVersions.isEmpty()) {
            return null;
        }

        // all bundles must have the same version
        if (platformVersions.size() > 1) {
            logErrorParam.accept("Found multiple platform versions: " + platformVersions);
        }

        return platformVersions.iterator().next();
    }

    /**
     * @return the OSGi version of the RCE "product" determined via the product specific branding bundles, <code>null</code> if it could not
     *         be determined
     */
    public static Version getProductVersion() {
        return getProductVersion(BUNDLE_ACCESSOR);
    }

    /**
     * Delegate for getProductVersion(). Created for testing, as this delegate takes the BundleAccessor as a parameter, making it an easily
     * testable pure function
     */
    static Version getProductVersion(BundleAccessor accessor) {
        return accessor.getVersionInfoBundle()
            .map(Bundle::getVersion)
            .orElse(null);
    }

    /**
     * @param version {@link Version} to parse
     * @return version number of the {@link Version} given + optional type of version (RC, Snapshot)
     */
    public static String getVersionAsString(Version version) {

        final String versionNumber = StringUtils.format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro());

        final VersionType versionType = VersionType.fromVersion(version);
        if (versionType.isRelease()) {
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
        if (!VersionType.fromVersion(version).hasBuildId()) {
            return null;
        }

        return version.getQualifier();
    }

    /**
     * @return True if the version of <code>de.rcenvironment.core.utils.common</code> indicates a release or RC build. False if it does not
     *         or if that bundle is not present.
     */
    public static boolean isReleaseOrReleaseCandidate() {
        final Version version = getCoreBundleVersion();
        if (version == null) {
            return false;
        }

        final VersionType versionType = VersionType.fromVersion(version);

        return versionType.isRelease() || versionType.isReleaseCandidate();
    }

    /**
     * Typically, if we want to log statements in a class, we create a field of type {@link Log} and initialize this field during the
     * instantiation of the class. Most methods in this class, however, are static and can thus not access fields. Making that field static
     * would lead to non-negligible memory overhead, as an instance of {@link Log} would have to be kept in memory indefinitely.
     * 
     * Thus, we obtain an instance of {@link Log} on the fly whenever we want to log a message. This method is a convenience method that
     * instantiates a {@link Log} and logs the given message.
     * 
     * @param message a message to be logged with logging level error
     */
    private static void logError(String message) {
        LogFactory.getLog(OWN_CLASS).error(message);
    }

}
