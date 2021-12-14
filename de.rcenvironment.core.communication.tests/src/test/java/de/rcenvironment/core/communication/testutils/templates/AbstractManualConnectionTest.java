/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.impl.ConnectionSetupServiceImpl;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.testutils.AbstractTransportBasedTest;
import de.rcenvironment.core.communication.testutils.ConnectionSetupStateTracker;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceGroup;
import de.rcenvironment.core.communication.transport.virtual.testutils.VirtualTopology;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;

/**
 * A common base class that defines common tests to verify proper transport operation. Subclasses implement
 * {@link #defineTestConfiguration()} to create a transport-specific test.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractManualConnectionTest extends AbstractTransportBasedTest {

    private static final int SHCDC_TEST_NUM_CONNECTION_ATTEMPTS = 1000;

    private static final int CHCDC_TEST_NUM_THREADS = 50;

    private static final int CHCDC_TEST_NUM_CONNECTION_ATTEMPTS_PER_THREAD = 20;

    private static final int MRM_TEST_NUM_MESSAGES = 100000;

    private static final int MRM_TEST_NUM_SENDER_THREADS = 20;

    private static final int MRM_TEST_SINGLE_MESSAGE_TIMEOUT = 1000;

    private static final String COMMON_SERVER_NODE_NAME = "server";

    private static final int COMMON_SLEEP_TIME_AFTER_SERVER_SHUTDOWN = 2000;

    private static final int COMMON_STATE_CHANGE_WAIT_MSEC = 10 * 1000;

    private static final long COMMON_WAIT_TIME_BEFORE_DISCONNECTING = 100;

    private static final int COMMON_WAIT_TIME_BEFORE_PRINTING_3RD_STATS = 2000;

    private static final int COMMON_WAIT_TIME_BEFORE_PRINTING_2ND_STATS = 5000;

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
        VirtualInstance server = new VirtualInstance(COMMON_SERVER_NODE_NAME);
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
            for (int i = 0; i < SHCDC_TEST_NUM_CONNECTION_ATTEMPTS; i++) {
                log.debug("Starting connection attempt " + (i + 1));
                final String observedRemoteIdString = performConnectDisconnectCycle(setup, stateTracker);
                assertEquals(server.getInstanceNodeSessionId().getInstanceNodeSessionIdString(), observedRemoteIdString);
            }
        } finally {
            String statistics = ConcurrencyUtils.getThreadPoolManagement().getFormattedStatistics(true);
            log.debug(statistics);
            connectionSetupService.removeConnectionSetupListener(stateTracker);
            server.shutDown();
            Thread.sleep(COMMON_SLEEP_TIME_AFTER_SERVER_SHUTDOWN); // avoid irrelevant interruption exception
        }
    }

    /**
     * Tests many connection attempts with a wrong protocol version (for testing against connection/memory leaks).
     * 
     * @throws InterruptedException on uncaught errors
     * @throws TimeoutException on uncaught errors
     */
    @Test
    @Ignore
    public void sequentialWrongProtocolConnectionAttempts() throws InterruptedException, TimeoutException {
        VirtualInstance client = new VirtualInstance("client");
        VirtualInstance server = new VirtualInstance(COMMON_SERVER_NODE_NAME);
        client.registerNetworkTransportProvider(transportProvider);
        server.registerNetworkTransportProvider(transportProvider);
        server.addServerConfigurationEntry(contactPointGenerator.createContactPoint());
        client.simulateCustomProtocolVersion("wrongClientVersion");
        server.start();
        client.start();
        ConnectionSetupServiceImpl connectionSetupService = (ConnectionSetupServiceImpl) client.getConnectionSetupService();
        ConnectionSetup setup = connectionSetupService.createConnectionSetup(server.getDefaultContactPoint(), "", false);
        ConnectionSetupStateTracker stateTracker = new ConnectionSetupStateTracker(setup);
        connectionSetupService.addConnectionSetupListener(stateTracker);
        try {
            for (int i = 0; i < SHCDC_TEST_NUM_CONNECTION_ATTEMPTS; i++) {
                log.debug("Starting connection attempt " + (i + 1));
                ConnectionSetupState initialState = setup.getState();
                assertTrue(ConnectionSetupState.DISCONNECTED == initialState);
                setup.signalStartIntent();
                stateTracker.awaitAndExpect(ConnectionSetupState.CONNECTING, COMMON_STATE_CHANGE_WAIT_MSEC);
                stateTracker.awaitAndExpect(ConnectionSetupState.DISCONNECTED, COMMON_STATE_CHANGE_WAIT_MSEC);
            }
        } finally {
            connectionSetupService.removeConnectionSetupListener(stateTracker);
            server.shutDown();
            Thread.sleep(COMMON_SLEEP_TIME_AFTER_SERVER_SHUTDOWN); // avoid irrelevant interruption exception
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

        final VirtualInstance server = new VirtualInstance(COMMON_SERVER_NODE_NAME, false); // no relay
        server.registerNetworkTransportProvider(transportProvider);
        server.addServerConfigurationEntry(contactPointGenerator.createContactPoint());
        server.start();

        try {
            RunnablesGroup runnablesGroup = ConcurrencyUtils.getFactory().createRunnablesGroup();

            for (int i = 1; i <= CHCDC_TEST_NUM_THREADS; i++) {
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
                            for (int j = 1; j <= CHCDC_TEST_NUM_CONNECTION_ATTEMPTS_PER_THREAD; j++) {
                                attemptCount.incrementAndGet();
                                String attemptId = "connSetup-" + i2 + "-" + j;
                                ConnectionSetup setup =
                                    connectionSetupService.createConnectionSetup(server.getDefaultContactPoint(), attemptId, false);
                                ConnectionSetupStateTracker stateTracker = new ConnectionSetupStateTracker(setup);
                                connectionSetupService.addConnectionSetupListener(stateTracker);
                                log.debug("Starting connection attempt: " + attemptId);
                                try {
                                    final String observedRemoteIdString = performConnectDisconnectCycle(setup, stateTracker);
                                    assertEquals(server.getInstanceNodeSessionIdString(), observedRemoteIdString);
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
            String statistics = ConcurrencyUtils.getThreadPoolManagement().getFormattedStatistics(true);
            log.debug("Immediately after end of test:\n" + statistics);
            Thread.sleep(COMMON_WAIT_TIME_BEFORE_PRINTING_2ND_STATS);
            String statistics2 = ConcurrencyUtils.getThreadPoolManagement().getFormattedStatistics(true);
            log.debug("After a short delay:\n" + statistics2);
            server.shutDown();
            Thread.sleep(COMMON_WAIT_TIME_BEFORE_PRINTING_3RD_STATS); // avoid irrelevant interruption exception
            String statistics3 = ConcurrencyUtils.getThreadPoolManagement().getFormattedStatistics(true);
            log.debug("After server shutdown:\n" + statistics3);

            log.debug(StringUtils.format("Attempt/success count: %d/%d", attemptCount.get(), successCount.get()));
            assertEquals(CHCDC_TEST_NUM_THREADS * CHCDC_TEST_NUM_CONNECTION_ATTEMPTS_PER_THREAD, attemptCount.get());
            assertEquals(CHCDC_TEST_NUM_THREADS * CHCDC_TEST_NUM_CONNECTION_ATTEMPTS_PER_THREAD, successCount.get());
        }
    }

    /**
     * Tests many messages being sent from a client to another client via a relay node; for testing against resource leaks in message
     * sending or forwarding.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    @Ignore
    public void manyRoutedMessages() throws Exception {
        final VirtualInstance client1 = new VirtualInstance("client1");
        final VirtualInstance client2 = new VirtualInstance("client2");
        final VirtualInstance server = new VirtualInstance(COMMON_SERVER_NODE_NAME, true); // relay

        VirtualTopology topology = new VirtualTopology(client1, client2, server);
        VirtualInstanceGroup allNodes = topology.getAsGroup();
        allNodes.registerNetworkTransportProvider(transportProvider);
        NetworkContactPoint serverNCP = contactPointGenerator.createContactPoint();
        server.addServerConfigurationEntry(serverNCP);
        allNodes.start();
        topology.connect(0, 2);
        topology.connect(1, 2);
        topology.waitUntilReachable(0, 1, COMMON_STATE_CHANGE_WAIT_MSEC); // client1->client2

        try {
            log.info("Starting to send messages");
            final Semaphore maxParallelSendLimit = new Semaphore(MRM_TEST_NUM_SENDER_THREADS);
            final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
            for (int i = 0; i < MRM_TEST_NUM_MESSAGES; i++) {
                maxParallelSendLimit.acquire();
                final String requestContent = Integer.toString(i);
                threadPool.execute("Send message", new Runnable() {

                    @Override
                    public void run() {
                        NetworkResponse response;
                        try {
                            response = client1.performRoutedRequest(requestContent, client2.getInstanceNodeSessionId(),
                                MRM_TEST_SINGLE_MESSAGE_TIMEOUT);
                            Serializable responseContent = response.getDeserializedContent();
                            assertTrue(responseContent.toString().startsWith(requestContent));
                            // note: intentionally not reached on errors
                            maxParallelSendLimit.release();
                        } catch (CommunicationException | InterruptedException | ExecutionException | TimeoutException
                            | SerializationException e) {
                            log.error("Error while sending message", e);
                        }
                    }
                });
            }
            log.info("Finished sending messages");
        } finally {
            allNodes.shutDown();
            Thread.sleep(COMMON_SLEEP_TIME_AFTER_SERVER_SHUTDOWN); // avoid irrelevant interruption exception
        }
    }

    /**
     * @return the instance session id reported by the remote node
     */
    private String performConnectDisconnectCycle(ConnectionSetup setup, ConnectionSetupStateTracker stateTracker)
        throws InterruptedException, AssertionError, TimeoutException {
        ConnectionSetupState initialState = setup.getState();
        assertTrue(ConnectionSetupState.DISCONNECTED == initialState);
        setup.signalStartIntent();
        stateTracker.awaitAndExpect(ConnectionSetupState.CONNECTING, COMMON_STATE_CHANGE_WAIT_MSEC);
        stateTracker.awaitAndExpect(ConnectionSetupState.CONNECTED, COMMON_STATE_CHANGE_WAIT_MSEC);
        final String remoteInstanceSessionIdString = setup.getCurrentChannel().getRemoteNodeInformation().getInstanceNodeSessionIdString();
        Thread.sleep(COMMON_WAIT_TIME_BEFORE_DISCONNECTING);
        // TODO also test message round-trip here?
        setup.signalStopIntent();
        stateTracker.awaitAndExpect(ConnectionSetupState.DISCONNECTING, COMMON_STATE_CHANGE_WAIT_MSEC);
        stateTracker.awaitAndExpect(ConnectionSetupState.DISCONNECTED, COMMON_STATE_CHANGE_WAIT_MSEC);
        // return remote id for potential verification
        return remoteInstanceSessionIdString;
    }
}
