/*
 * Copyright (C) 2006-2012 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.impl.ConnectionSetupServiceImpl;
import de.rcenvironment.core.communication.testutils.AbstractTransportBasedTest;
import de.rcenvironment.core.communication.testutils.ConnectionSetupStateTracker;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.RunnablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;

/**
 * A common base class that defines common tests to verify proper transport operation. Subclasses implement
 * {@link #defineTestConfiguration()} to create a transport-specific test.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractManualConnectionTest extends AbstractTransportBasedTest {

    private static final int WAIT_TIME_BEFORE_PRINTING_3RD_STATS = 2000;

    private static final int WAIT_TIME_BEFORE_PRINTING_2ND_STATS = 5000;

    private static final String DEFAULT_SERVER_NODE_ID = "server";

    private static final int SLEEP_TIME_AFTER_SERVER_SHUTDOWN = 1000;

    private static final int SEQUENTIAL_TEST_CONNECTION_ATTEMPTS = 1000;

    private static final int CONCURRENT_TEST_THREADS = 50;

    private static final int CONCURRENT_TEST_CONNECTION_ATTEMPTS_PER_THREAD = 20;

    private static final int DEFAULT_STATE_CHANGE_WAIT_MSEC = 10 * 1000;

    private static final long WAIT_TIME_BEFORE_DISCONNECTING = 100;

    /**
     * Tests many connections build-up/tear-down cycles in a row. Especially useful for stability testing, and checking for memory leaks.
     * 
     * @throws InterruptedException on uncaught errors
     * @throws TimeoutException on uncaught errors
     */
    @Test
    @Ignore
    public void sequentialHighConnectDisconnectCount() throws InterruptedException, TimeoutException {
        VirtualInstance client = new VirtualInstance("client");
        VirtualInstance server = new VirtualInstance(DEFAULT_SERVER_NODE_ID);
        client.registerNetworkTransportProvider(transportProvider);
        server.registerNetworkTransportProvider(transportProvider);
        server.addServerConfigurationEntry(contactPointGenerator.createContactPoint());
        server.start();
        client.start();
        ConnectionSetupServiceImpl connectionSetupService = (ConnectionSetupServiceImpl) client.getConnectionSetupService();
        ConnectionSetup setup = connectionSetupService.createConnectionSetup(server.getDefaultContactPoint(), "", false);
        ConnectionSetupStateTracker stateTracker = new ConnectionSetupStateTracker(setup);
        connectionSetupService.addConnectionSetupListener(stateTracker);
        try {
            for (int i = 0; i < SEQUENTIAL_TEST_CONNECTION_ATTEMPTS; i++) {
                log.debug("Starting connection attempt " + (i + 1));
                performConnectDisconnectCycle(setup, stateTracker);
            }
        } finally {
            String statistics = SharedThreadPool.getInstance().getFormattedStatistics(true);
            log.debug(statistics);
            connectionSetupService.removeConnectionSetupListener(stateTracker);
            server.shutDown();
            Thread.sleep(SLEEP_TIME_AFTER_SERVER_SHUTDOWN); // avoid irrelevant interruption exception
        }
    }

    /**
     * Tests many connections build-up/tear-down cycles in parallel by multiple "clients". Especially useful for load/stability testing.
     * 
     * @throws InterruptedException on uncaught errors
     */
    @Test
    @Ignore
    public void concurrentHighConnectDisconnectCount() throws InterruptedException {

        final AtomicInteger attemptCount = new AtomicInteger();

        final AtomicInteger successCount = new AtomicInteger();

        final VirtualInstance server = new VirtualInstance(DEFAULT_SERVER_NODE_ID, DEFAULT_SERVER_NODE_ID, false); // no relay
        server.registerNetworkTransportProvider(transportProvider);
        server.addServerConfigurationEntry(contactPointGenerator.createContactPoint());
        server.start();

        try {
            RunnablesGroup runnablesGroup = SharedThreadPool.getInstance().createRunnablesGroup();

            for (int i = 1; i <= CONCURRENT_TEST_THREADS; i++) {
                final int i2 = i;
                runnablesGroup.add(new Runnable() {

                    @Override
                    public void run() {
                        String clientId = "client-" + i2;
                        try {
                            VirtualInstance client = new VirtualInstance(clientId);
                            client.registerNetworkTransportProvider(transportProvider);
                            client.start();
                            ConnectionSetupServiceImpl connectionSetupService =
                                (ConnectionSetupServiceImpl) client.getConnectionSetupService();
                            for (int j = 1; j <= CONCURRENT_TEST_CONNECTION_ATTEMPTS_PER_THREAD; j++) {
                                attemptCount.incrementAndGet();
                                String attemptId = "connSetup-" + i2 + "-" + j;
                                ConnectionSetup setup =
                                    connectionSetupService.createConnectionSetup(server.getDefaultContactPoint(), attemptId, false);
                                ConnectionSetupStateTracker stateTracker = new ConnectionSetupStateTracker(setup);
                                connectionSetupService.addConnectionSetupListener(stateTracker);
                                log.debug("Starting connection attempt: " + attemptId);
                                try {
                                    performConnectDisconnectCycle(setup, stateTracker);
                                    successCount.incrementAndGet();
                                } catch (TimeoutException e) {
                                    log.error(StringUtils.format("Connect attempt %d of thread %d failed with a timeout: %s", i2, j,
                                        e.toString()));
                                }
                                connectionSetupService.removeConnectionSetupListener(stateTracker);
                            }
                            client.shutDown();
                        } catch (InterruptedException e) {
                            log.error("Client " + clientId + " failed", e);
                            throw new RuntimeException(e);
                        } catch (AssertionError e) {
                            log.error("Client " + clientId + " failed", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            runnablesGroup.executeParallel();
        } finally {
            String statistics = SharedThreadPool.getInstance().getFormattedStatistics(true);
            log.debug("Immediately after end of test:\n" + statistics);
            Thread.sleep(WAIT_TIME_BEFORE_PRINTING_2ND_STATS);
            String statistics2 = SharedThreadPool.getInstance().getFormattedStatistics(true);
            log.debug("After a short delay:\n" + statistics2);
            server.shutDown();
            Thread.sleep(WAIT_TIME_BEFORE_PRINTING_3RD_STATS); // avoid irrelevant interruption exception
            String statistics3 = SharedThreadPool.getInstance().getFormattedStatistics(true);
            log.debug("After server shutdown:\n" + statistics3);

            log.debug(StringUtils.format("Attempt/success count: %d/%d", attemptCount.get(), successCount.get()));
            assertEquals(CONCURRENT_TEST_THREADS * CONCURRENT_TEST_CONNECTION_ATTEMPTS_PER_THREAD, attemptCount.get());
            assertEquals(CONCURRENT_TEST_THREADS * CONCURRENT_TEST_CONNECTION_ATTEMPTS_PER_THREAD, successCount.get());
        }
    }

    private void performClientLifeCycle(VirtualInstance server, String clientId, String attemptId) throws InterruptedException {

    }

    private void performConnectDisconnectCycle(ConnectionSetup setup, ConnectionSetupStateTracker stateTracker)
        throws InterruptedException, AssertionError, TimeoutException {
        ConnectionSetupState initialState = setup.getState();
        assertTrue(ConnectionSetupState.DISCONNECTED == initialState);
        setup.signalStartIntent();
        stateTracker.awaitAndExpect(ConnectionSetupState.CONNECTING, DEFAULT_STATE_CHANGE_WAIT_MSEC);
        stateTracker.awaitAndExpect(ConnectionSetupState.CONNECTED, DEFAULT_STATE_CHANGE_WAIT_MSEC);
        assertEquals(DEFAULT_SERVER_NODE_ID, setup.getCurrentChannel().getRemoteNodeInformation().getNodeIdString());
        Thread.sleep(WAIT_TIME_BEFORE_DISCONNECTING);
        // TODO test message round-trip here?
        setup.signalStopIntent();
        stateTracker.awaitAndExpect(ConnectionSetupState.DISCONNECTING, DEFAULT_STATE_CHANGE_WAIT_MSEC);
        stateTracker.awaitAndExpect(ConnectionSetupState.DISCONNECTED, DEFAULT_STATE_CHANGE_WAIT_MSEC);
    }
}
