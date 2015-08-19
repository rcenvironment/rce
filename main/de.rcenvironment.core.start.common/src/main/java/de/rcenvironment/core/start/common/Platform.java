/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utilities class to govern the RCE platform.
 * 
 * @author Christian Weiss
 * @author Robert Mischke
 */
// TODO rename to "Instance" - misc_ro
public class Platform {

    private static boolean isHeadless;

    private static CountDownLatch shutdownLatch = new CountDownLatch(1);

    private static InstanceRunner runner;

    protected Platform() {
        // do nothing
    }

    /**
     * Returns whether the RCE platform is started in headless mode.
     * 
     * @return the headless state
     */
    public static boolean isHeadless() {
        return isHeadless;
    }

    public static void setHeadless(boolean isHeadlessOn) {
        isHeadless = isHeadlessOn;
    }

    /**
     * Injects the {@link InstanceRunner} to use for startup. Trying to call this method twice
     * throws an exception (to detect invalid setups).
     * 
     * @param newRunner the new instance
     */
    public static synchronized void setRunner(InstanceRunner newRunner) {
        if (runner != null) {
            throw new IllegalStateException("Tried to set runner " + newRunner + " when one is already configured: "
                + runner);
        }
        runner = newRunner;
    }

    /**
     * Fetches the {@link InstanceRunner} to use for startup. Throws an exception if no runner has
     * been set.
     * 
     * @return the runner instance (guaranteed to be non-null)
     */
    public static synchronized InstanceRunner getRunner() {
        if (runner == null) {
            throw new IllegalStateException("Internal error: No instance runner configured");
        }
        return runner;
    }

    /**
     * Waits for the RCE Platform to shut down.
     * 
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    /**
     * Waits for the RCE Platform to shut down.
     * 
     * @param timeout the amount of time to wait
     * @param unit the time unit of the timeout argument
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void awaitShutdown(final long timeout, final TimeUnit unit) throws InterruptedException {
        getRunner().beforeAwaitShutdown();
        shutdownLatch.await(timeout, unit);
    }

    /**
     * Shuts down the RCE platform instance.
     * 
     */
    public static void shutdown() {
        shutdownLatch.countDown();
        getRunner().triggerShutdown();
    }

    /**
     * Return whether or not the RCE platform has been shut down.
     * 
     * @return true, if the RCE platform has been shut down.
     */
    public static boolean isShutdown() {
        return shutdownLatch.getCount() == 0;
    }

    /* default */static void resetShutdown() {
        shutdownLatch = new CountDownLatch(1);
    }

    /**
     * Restarts the RCE platform.
     */
    public static void restart() {
        getRunner().triggerRestart();
    }

}
