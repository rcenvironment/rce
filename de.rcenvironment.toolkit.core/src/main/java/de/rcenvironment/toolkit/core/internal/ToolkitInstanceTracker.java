/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.internal;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.core.api.Toolkit;

/**
 * Keeps track of all created and undisposed {@link Toolkit} instances to warn about leftover instances on shutdown (especially from
 * unit/integration tests).
 * 
 * @author Robert Mischke
 * 
 */
public final class ToolkitInstanceTracker {

    private static final ToolkitInstanceTracker sharedInstance = new ToolkitInstanceTracker();

    private final Set<Toolkit> activeInstances = new HashSet<>();

    /**
     * The shutdown hook wrapper; moved to a nested class as our code checking rules flag new Thread() as suspicious.
     * 
     * Note: If actual Thread subclasses ever become a problem and the checks are adapted, they must have special treatment of shutdown
     * hooks, as the Java API enforces the {@link Thread} approach.
     * 
     * @author Robert Mischke
     */
    private final class ShutdownHook extends Thread {

        @Override
        public void run() {
            onJvmShutdown();
        }
    }

    private ToolkitInstanceTracker() {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public static ToolkitInstanceTracker getInstance() {
        return sharedInstance;
    }

    /**
     * Registers a new {@link Toolkit} instance; should be called as part of (or after) each creation of an instance.
     * 
     * @param newInstance the new instance
     */
    public synchronized void register(Toolkit newInstance) {
        if (!activeInstances.add(newInstance)) {
            throw new IllegalArgumentException("This toolkit instance was already registered");
        }
    }

    /**
     * Reports that a {@link Toolkit} instance has been shutdown/disposed.
     * 
     * @param oldInstance the disposed instance
     */
    public synchronized void markDisposed(Toolkit oldInstance) {
        if (!activeInstances.remove(oldInstance)) {
            throw new IllegalArgumentException("This toolkit instance was never registered or was already disposed");
        }
    }

    private synchronized void onJvmShutdown() {
        final Log log = LogFactory.getLog(getClass());
        final int count = activeInstances.size();

        if (count == 1) {
            log.debug("There is a single undisposed toolkit instance on JVM shutdown; this can be safely ignored after unit tests");
        } else if (count > 1) {
            log.warn("There are " + count
                + " undisposed toolkit instances left on JVM shutdown; these should be explicitly shut down instead if possible");
        }

        if (count != 0) {
            log.debug("Shutting down " + count + " toolkit instance(s)");
            for (Toolkit instance : activeInstances) {
                instance.shutdown();
            }
        }
    }

}
