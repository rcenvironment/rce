/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.osgi.BundleUtils;

/**
 * Resolves arbitrary ids to the location of a directory of unpacked files.
 * <p>
 * In the current approach, these arbitrary ids must be declared as the manifest header "RCE-Unpacked-Fileset-Id". Only one active/resoled
 * OSGi bundle must declare the requested id, otherwise lookup must fail with an error.
 * <p>
 * If a unique bundle declaring this id is found, it is checked whether it has been unpacked as expected (ie, whether its location is a
 * directory). Again, an error is thrown if this is not the case.
 * <p>
 * If the bundle is a directory, the relative path specified by the manifeset header "RCE-Unpacked-Fileset-Path" is appended to its
 * location. The resulting location is checked for existence, and if it is found, it is returned as the final result.
 * 
 * @author Robert Mischke
 */
public class UnpackedFilesDirectoryResolver {

    /**
     * The manifest header to specify the (arbitrary) fileset id.
     */
    public static final String MANIFEST_PROPERTY_FILESET_ID = "RCE-Unpacked-Fileset-Id";

    /**
     * The manifest header to specify the relative path within the bundle where the relevant files are located. For now, the same relative
     * path is expected to be used when running from Eclipse vs. when running a deployed product. If this does not match in a future setup,
     * this should be split into two manifest headers.
     */
    public static final String MANIFEST_PROPERTY_BUNDLE_RELATIVE_PATH = "RCE-Unpacked-Fileset-Path";

    /**
     * The manifest header to specify the relative path within the unpacked files directory at which a known file should exist; this is used
     * to verify that not only the files directory, but also its contents are present. If this header is not present at all, a warning will
     * be logged, but the directory resolution will still be considered successful.
     */
    public static final String MANIFEST_PROPERTY_PRESENCE_CHECK_TEST_FILE = "RCE-Unpacked-Fileset-Presence-Test-File";

    private final BundleContext bundleContext;

    // note: values may also be null, which indicates lookup failure
    private final Map<String, File> resolutionCache = new HashMap<>();

    private final File installationRootPath;

    public UnpackedFilesDirectoryResolver(BundleContext context, File installationRootPath) {
        this.bundleContext = context;
        this.installationRootPath = installationRootPath;
    }

    /**
     * Performs the id-to-directory lookup as specified in the class JavaDoc.
     * 
     * @param id the (non-null) id to resolve/lookup
     * @return a non-null path on success
     * @throws ConfigurationException on lookup failure
     */
    public synchronized File resolveIdToUnpackedFilesDirectory(String id) throws ConfigurationException {
        Objects.requireNonNull(id);

        if (resolutionCache.containsKey(id)) {
            File cached = resolutionCache.get(id);
            if (cached == null) {
                throw new ConfigurationException(
                    "This fileset was previously requested, and a lookup failure was cached; check the log file for the initial reason");
            }
            return cached;
        }

        return performLookup(id);
    }

    private File performLookup(String filesetId) throws ConfigurationException {
        Bundle bundleCandidate = findMatchingBundle(filesetId);

        File resolvedPath = resolvePath(filesetId, bundleCandidate);

        resolutionCache.put(filesetId, resolvedPath); // only reached on success

        return resolvedPath;
    }

    private File resolvePath(String lookupId, Bundle bundleCandidate) throws ConfigurationException {

        final Log log = LogFactory.getLog(getClass());

        File absoluteBundleLocation;
        try {
            absoluteBundleLocation = BundleUtils.resolveAbsolutePathToUnpackedBundle(bundleCandidate, installationRootPath);
        } catch (OperationFailureException e) {
            throw new ConfigurationException(e.getMessage());
        }
        final String symbolicName = bundleCandidate.getSymbolicName();

        final String relativePathValue = bundleCandidate.getHeaders().get(MANIFEST_PROPERTY_BUNDLE_RELATIVE_PATH);
        if (StringUtils.isNullorEmpty(relativePathValue)) {
            throw new ConfigurationException("Missing or empty header entry " + MANIFEST_PROPERTY_BUNDLE_RELATIVE_PATH + " in bundle "
                + symbolicName);
        }

        final File resolvedPath = new File(absoluteBundleLocation, relativePathValue);

        if (!resolvedPath.isDirectory()) {
            throw new ConfigurationException(StringUtils.format(
                "Resolved file set id '%s' to bundle '%s', but the resulting path '%s' does not point to an existing directory", lookupId,
                symbolicName, resolvedPath));
        }

        final String presenceCheckTestFilePath = bundleCandidate.getHeaders().get(MANIFEST_PROPERTY_PRESENCE_CHECK_TEST_FILE);
        if (!StringUtils.isNullorEmpty(presenceCheckTestFilePath)) {
            final File presenceCheckTestFile = new File(resolvedPath, presenceCheckTestFilePath);
            if (!presenceCheckTestFile.isFile() || !presenceCheckTestFile.canRead()) {
                throw new ConfigurationException(StringUtils.format(
                    "Resolved file set id '%s' to path '%s', but the configured test file '%s' does not exist or cannot be read", lookupId,
                    resolvedPath, presenceCheckTestFile.getAbsolutePath()));
            }
        } else {
            log.warn("No presence check file configured for unpacked file set " + lookupId + "; skipping check");
        }

        log.debug(StringUtils.format(
            "Resolved file set id '%s' to path '%s' in bundle '%s'", lookupId, resolvedPath, symbolicName));

        return resolvedPath;
    }

    private Bundle findMatchingBundle(String filesetId) throws ConfigurationException {
        Bundle bundleCandidate = null;
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() != Bundle.RESOLVED) {
                // only check resolved bundles, as bundles not matching platform filters are still present as INSTALLED in Eclipse
                continue;
            }
            Dictionary<String, String> headers = bundle.getHeaders();
            String bundleIdCandidate = headers.get(MANIFEST_PROPERTY_FILESET_ID);
            if (filesetId.equals(bundleIdCandidate)) {
                if (bundleCandidate != null) {
                    throw new ConfigurationException(
                        "More than one resolved bundle declares file set id " + filesetId
                            + "; check for proper platform (e.g. OS) filters");
                }
                bundleCandidate = bundle;
            }
        }
        if (bundleCandidate == null) {
            throw new ConfigurationException(
                "No resolved bundle declaring file set id " + filesetId + " found");
        }
        return bundleCandidate;
    }
}
