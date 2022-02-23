/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.osgi;

import java.io.File;

import org.osgi.framework.Bundle;

import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Provides utility methods for OSGi bundles.
 *
 * @author Robert Mischke
 */
public final class BundleUtils {

    private BundleUtils() {}

    /**
     * Locates the absolute root directory of an unpacked bundle. Note that NO verification is made whether this path actually exists, or
     * whether the bundle is actually in unpacked form.
     * 
     * @param bundle the target bundle
     * @param installationRootPath the root path of the whole RCE installation; only needed when this code is called as part of a standalone
     *        build (as opposed to from an IDE)
     * @return the resolved absolute path
     * @throws OperationFailureException if the OSGi bundle.getLocation value has an unexpected format
     */
    public static File resolveAbsolutePathToUnpackedBundle(Bundle bundle, File installationRootPath)
        throws OperationFailureException {
        final String expectedLocationStringPrefix = "reference:file:";
        final String locationString = bundle.getLocation();
        if (!locationString.startsWith(expectedLocationStringPrefix)) {
            throw new OperationFailureException(
                "Unexpected bundle location value (expected prefix '" + expectedLocationStringPrefix + "') for bundle "
                    + bundle.getSymbolicName() + ": " + locationString);
        }
        // cut away the prefix and convert to File for examination
        final String bundleLocationString = locationString.substring(expectedLocationStringPrefix.length());
        final File bundleLocation = new File(bundleLocationString);

        // resolve against the installation root path if the returned bundle location is
        // a relative path (which is the standard behavior in standalone product builds)
        final File absoluteBundleLocation;
        if (bundleLocation.isAbsolute()) {
            absoluteBundleLocation = bundleLocation;
        } else {
            absoluteBundleLocation = new File(installationRootPath, bundleLocationString);
        }
        return absoluteBundleLocation;
    }

}
