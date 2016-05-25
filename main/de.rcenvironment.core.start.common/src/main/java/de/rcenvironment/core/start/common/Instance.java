/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utilities class to govern the RCE platform.
 * 
 * @author Christian Weiss
 * @author Robert Mischke
 * @author Doreen Seider
 * @author David Scholz (minor change: added shutdown requested flag)
 */
public class Instance {

    private static Boolean isHeadless;

    private static Boolean shutdownRequested = false;

    private static CountDownLatch shutdownLatch = new CountDownLatch(1);

    private static InstanceRunner instanceRunner;

    protected Instance() {}

    /**
     * Returns whether the RCE instance is started in headless mode.
     * 
     * @return <code>true</code> if started in headless mode, otherwise <code>false</code>
     * @throws IllegalStateException if mode not yet set
     */
    public static boolean isHeadless() throws IllegalStateException {
        if (isHeadless == null) {
            throw new IllegalStateException("Internal error: Headless mode not yet set");
        }
        return isHeadless;
    }

    /**
     * 
     * Returns whether the RCE instance should be shutdown. This is used to indicate shutdown requests before the {@link InstanceRunner} is
     * created.
     * 
     * @return <code>true</code> if shutdown is requested, otherwise <code>false</code>
     * @throws IllegalStateException if shutdown is requested flag not yet set
     */
    public static boolean isShutdownRequested() {
        return shutdownRequested;
    }

    /**
     * Sets whether this RCE instance is started in headless mode.
     * 
     * @param isHeadlessOn <code>true</code> if started in headless mode, otherwise <code>false</code>
     * @throws IllegalStateException if method is called to change the mode already set
     */
    public static void setHeadless(boolean isHeadlessOn) throws IllegalStateException {
        if (isHeadless != null && isHeadlessOn != isHeadless) {
            throw new IllegalStateException(StringUtils.format("Tried to set the headless mode of this instance to '%s',"
                + " but it was already set to '%s'", isHeadlessOn, isHeadless));
        }
        isHeadless = isHeadlessOn;
    }

    /**
     * Injects the {@link InstanceRunner} to use for startup. Trying to call this method twice throws an exception (to detect invalid
     * setups).
     * 
     * @param runner instance runner to inject
     * @throws IllegalStateException if this method is called twice (to detect invalid setups)
     */
    public static synchronized void setInstanceRunner(InstanceRunner runner) throws IllegalStateException {
        if (instanceRunner != null) {
            throw new IllegalStateException(StringUtils.format("Tried to set instance runner '%s' when one is already configured: '%s'",
                runner, instanceRunner));
        }
        instanceRunner = runner;
    }

    /**
     * Fetches the {@link InstanceRunner} to use for startup.
     * 
     * @return the runner instance (guaranteed to be non-null)
     * @throws IllegalStateException if not instance runner not yet set
     */
    public static synchronized InstanceRunner getInstanceRunner() throws IllegalStateException {
        if (instanceRunner == null) {
            throw new IllegalStateException("Internal error: No instance runner configured");
        }
        return instanceRunner;
    }

    /**
     * Waits for the RCE instance to shut down.
     * 
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    /**
     * Waits for the RCE instance to shut down.
     * 
     * @param timeout the amount of time to wait
     * @param unit the time unit of the timeout argument
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void awaitShutdown(final long timeout, final TimeUnit unit) throws InterruptedException {
        getInstanceRunner().beforeAwaitShutdown();
        shutdownLatch.await(timeout, unit);
    }

    /**
     * Shuts down the RCE instance.
     */
    public static void shutdown() {
        shutdownLatch.countDown();
        if (instanceRunner == null) {
            shutdownRequested = true;
        } else {
            getInstanceRunner().triggerShutdown();
        }
    }

    /**
     * Return whether or not the RCE instance has been shut down.
     * 
     * @return <code>true</code> if the RCE instance has been shut down, otherwise <code>false</code>
     */
    public static boolean isShutdown() {
        return shutdownLatch.getCount() == 0;
    }

    protected static void resetShutdown() {
        shutdownLatch = new CountDownLatch(1);
    }

    /**
     * Restarts the RCE instance.
     */
    public static void restart() {
        getInstanceRunner().triggerRestart();
    }

}
