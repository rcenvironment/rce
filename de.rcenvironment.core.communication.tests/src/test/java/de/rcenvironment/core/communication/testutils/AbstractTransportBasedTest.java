/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Base class for JUnit tests related to {@link NetworkTransportProvider} behavior. This includes low-level tests as well as higher-level
 * {@link VirtualInstance} tests.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractTransportBasedTest {

    protected static final int DEFAULT_SAFEGUARD_TEST_TIMEOUT = 60000;

    protected static final int DEFAULT_NODE_REACHABILITY_TIMEOUT = 2000;

    private static final double NANOS_PER_SEC = 1e9;

    /**
     * JUnit {@link Rule} to acquire the running test's name in JUnit 4.
     */
    @Rule
    public final TestName name = new TestName();

    protected final Random randomGenerator = new Random();

    protected final Log log = LogFactory.getLog(getClass());

    protected TestConfiguration testConfiguration;

    protected NetworkTransportProvider transportProvider;

    protected NetworkContactPointGenerator contactPointGenerator;

    protected boolean usingDuplexTransport = false;

    private long startTimeNano;

    /**
     * Setup method to apply the settings defined in {@link #defineTestConfiguration()}.
     */
    @Before
    public void applyInjectedConfiguration() {
        testConfiguration = defineTestConfiguration();
        transportProvider = testConfiguration.getTransportProvider();
        contactPointGenerator = testConfiguration.getContactPointGenerator();
        usingDuplexTransport = transportProvider.supportsRemoteInitiatedConnections();
        Assert.assertTrue(testConfiguration.getDefaultTrafficWaitTimeout() > 0);
        Assert.assertTrue(testConfiguration.getDefaultNetworkSilenceWait() > 0);
        Assert.assertTrue(testConfiguration.getDefaultNetworkSilenceWaitTimeout() > 0);
    }

    /**
     * Common test setup; prepares time measurement.
     */
    @Before
    public void commonTestStart() {
        startTimeNano = System.nanoTime();
    }

    /**
     * Common test tear-down; prints test name, duration, and thread pool statistics.
     */
    @After
    public void commonTestEnd() {
        // get execution time
        double testDuration = (System.nanoTime() - startTimeNano) / NANOS_PER_SEC;
        // get thread statistics
        int threadPoolSize = ConcurrencyUtils.getThreadPoolManagement().getCurrentThreadCount();
        int threadCount = Thread.activeCount();
        // clear thread pool
        int queuedCount = ConcurrencyUtils.getThreadPoolManagement().reset();
        if (queuedCount != 0) {
            log.warn("When resetting the thread pool, " + queuedCount + " Runnables were still queued");
        }
        // log summary
        log.debug(StringUtils.format("End of test '%s'; Duration: %.3f sec; Communication thread pool size: %d; Global thread count: %d",
            getCurrentTestName(), testDuration, threadPoolSize, threadCount));
    }

    protected abstract TestConfiguration defineTestConfiguration();

    protected String getCurrentTestName() {
        return name.getMethodName();
    }

}
