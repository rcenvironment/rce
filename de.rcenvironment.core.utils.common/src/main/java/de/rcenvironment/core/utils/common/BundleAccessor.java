/*
 * Copyright 2022-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.common;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Encapsulates obtaining {@link Bundle} objects and contexts of Bundles. Moreover, provides access to versioninfo- and platform bundles.
 * 
 * @author Alexander Weinert
 *
 */
class BundleAccessor {

    private class CachedSupplier<T> implements Supplier<T> {

        private final Supplier<T> supplier;

        private T value = null;

        CachedSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (this.value == null) {
                this.value = supplier.get();
            }

            return this.value;
        }

    }

    // The symbolic name of the bundle determining the product version
    private static final String VERSIONINFO_BUNDLE_NAME = "de.rcenvironment.core.gui.branding.default.versioninfo";

    private static final String PLATFORM_BUNDLES_PREFIX = "de.rcenvironment.platform.";

    private final Class<?> clazz;

    private final Supplier<Bundle> containingBundle;

    private final Supplier<Bundle[]> allBundlesInContext;

    BundleAccessor(Class<?> clazz) {
        this.clazz = clazz;
        this.containingBundle = new CachedSupplier<>(() -> FrameworkUtil.getBundle(clazz));
        this.allBundlesInContext = new CachedSupplier<>(() -> containingBundle.get().getBundleContext().getBundles());
    }

    public Bundle getOwnBundleOrLogDebug() {
        final Bundle ownBundle = containingBundle.get();

        if (ownBundle == null) {
            logDebug("No Bundle available (most likely because we are running in a unit test)");
        }

        return ownBundle;
    }

    public Bundle[] getAllBundlesOrLogError() {
        // We do not technically require the result of obtaining our own bundle here.
        final Bundle ownBundle = getOwnBundleOrLogDebug();
        if (ownBundle == null) {
            return new Bundle[] {};
        }

        Bundle[] bundles = allBundlesInContext.get();
        if (bundles == null) {
            logError("Unexpected error: 'null' bundle list while getting bundles in context of bundle " + ownBundle.getSymbolicName());
            return new Bundle[] {};
        }

        return bundles;
    }

    public Optional<Bundle> getVersionInfoBundle() {
        final Bundle[] bundles = getAllBundlesOrLogError();

        return Stream.of(bundles)
            .filter(bundle -> bundle.getSymbolicName().startsWith(VERSIONINFO_BUNDLE_NAME))
            .findAny();
    }

    public Stream<Bundle> getPlatformBundles() {
        Bundle[] bundles = getAllBundlesOrLogError();

        return Stream.of(bundles)
            // find all platform bundles by their symbolic name
            .filter(bundle -> bundle.getSymbolicName().startsWith(PLATFORM_BUNDLES_PREFIX));
    }

    /**
     * Typically, if we want to log statements in a class, we create a field of type {@link Log} and initialize this field during the
     * instantiation of the class. Most methods in this class, however, are static and can thus not access fields. Making that field static
     * would lead to non-negligible memory overhead, as an instance of {@link Log} would have to be kept in memory indefinitely.
     * 
     * Thus, we obtain an instance of {@link Log} on the fly whenever we want to log a message. This method is a convenience method that
     * instantiates a {@link Log} and logs the given message.
     * 
     * @param message a message to be logged with logging level debug
     */
    protected void logDebug(String message) {
        LogFactory.getLog(clazz).debug(message);
    }

    /**
     * @see BundleAccessor#logDebug(String)
     * @param message a message to be logged with logging level error
     */
    protected void logError(String message) {
        LogFactory.getLog(clazz).error(message);
    }
}
