/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.tests.integration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptor;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.testutils.ThroughputLimiter;

/**
 * State holder for the client side of integration tests.
 *
 * @author Robert Mischke
 */
class MockClientStateHolder implements ClientSideUplinkSessionEventHandler {

    private final Function<ToolExecutionRequest, MockToolExecutionProvider> actualToolExecutionRequestHandler;

    private final Map<String, List<ToolDescriptor>> lastReceivedComponentLists = Collections.synchronizedMap(new HashMap<>());

    private MockToolExecutionProvider lastExecutionProvider;

    private String assignedDestinationIdPrefix;

    private boolean sessionActiveState;

    private CountDownLatch sessionInitCompleteLatch = new CountDownLatch(1);

    private Map<String, String> customHandshakeParameters;

    private boolean sessionInTerminalState;

    private String sessionErrorMessage;

    // a general "something went wrong" flag that can be used to actively fail a surrounding test; not globally tested so far
    private boolean inconsistentStateFlag;

    private UplinkConnection uplinkConnection;

    private ThroughputLimiter outgoingThroughputLimiter;

    private ThroughputLimiter incomingThroughputLimiter;

    MockClientStateHolder(Function<ToolExecutionRequest, MockToolExecutionProvider> actualToolExecutionRequestHandler) {
        this.actualToolExecutionRequestHandler = actualToolExecutionRequestHandler;
    }

    @Override
    public void onSessionActivating(String namespaceId, String destinationIdPrefix) {
        assignedDestinationIdPrefix = destinationIdPrefix;
        if (sessionActiveState) { // consistency check
            inconsistentStateFlag = true;
            throw new IllegalStateException("Received more than one activation event");
        }
        sessionActiveState = true;
        sessionInitCompleteLatch.countDown();
    }

    @Override
    public void onActiveSessionTerminating() {
        if (!sessionActiveState) { // consistency check
            inconsistentStateFlag = true;
            throw new IllegalStateException("Received 'terminating' while not active");
        }
        sessionActiveState = false;
    }

    @Override
    public synchronized void onFatalErrorMessage(UplinkProtocolErrorType errorType, String errorMessage) {
        Objects.requireNonNull(errorType); // consistency check
        if (sessionErrorMessage != null) {
            inconsistentStateFlag = true;
            throw new IllegalStateException(
                "Received more than one connection error event; first: '" + sessionErrorMessage + "', additional: '" + errorMessage + "'");
        }
        sessionErrorMessage = errorMessage;
    }

    @Override
    public synchronized void onSessionInFinalState(boolean reasonableToRetry) {
        this.sessionInTerminalState = true;
        sessionInitCompleteLatch.countDown();
    }

    public synchronized boolean isSessionActive() {
        return sessionActiveState;
    }

    public synchronized void setSessionErrorMessage(String sessionErrorMessage) {
        this.sessionErrorMessage = sessionErrorMessage;
    }

    public synchronized boolean isSessionInTerminalState() {
        return sessionInTerminalState;
    }

    public synchronized String getSessionErrorMessage() {
        return sessionErrorMessage;
    }

    public MockToolExecutionProvider getLastExecutionProvider() {
        return lastExecutionProvider;
    }

    public Map<String, List<ToolDescriptor>> getKnownComponentsByDestinationId() {
        return lastReceivedComponentLists;
    }

    @Override
    public void processToolDescriptorListUpdate(ToolDescriptorListUpdate update) {
        this.lastReceivedComponentLists.put(update.getDestinationId(), update.getToolDescriptors());
    }

    @Override
    public ToolExecutionProvider setUpToolExecutionProvider(ToolExecutionRequest request) {
        // delegate to the actual handler; this stub only exists to save the generated instance for querying it later
        lastExecutionProvider = actualToolExecutionRequestHandler.apply(request);
        return lastExecutionProvider;
    }

    @Override
    public Optional<SizeValidatedDataSource> provideToolDocumentationData(String sourceId, String docReferenceId) {
        return null;
    }

    public void setAssignedDestinationIdPrefix(String string) {
        this.assignedDestinationIdPrefix = string;
    }

    public String getAssignedDestinationIdPrefix() {
        return assignedDestinationIdPrefix;
    }

    public void waitForSessionInitCompletion(long timeoutMsec) throws InterruptedException, TimeoutException {
        if (!sessionInitCompleteLatch.await(timeoutMsec, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException();
        }
    }

    public Map<String, String> getCustomHandshakeParameters() {
        return customHandshakeParameters;
    }

    public void setCustomHandshakeParameters(Map<String, String> customHandshakeParameters) {
        this.customHandshakeParameters = customHandshakeParameters;
    }

    public boolean getInconsistentStateFlag() {
        return inconsistentStateFlag;
    }

    public UplinkConnection getUplinkConnection() {
        return uplinkConnection;
    }

    public void setUplinkConnection(UplinkConnection uplinkConnection) {
        this.uplinkConnection = uplinkConnection;
    }

    public void setThroughputLimiters(ThroughputLimiter outgoingThroughputLimiter, ThroughputLimiter incomingThroughputLimiter) {
        this.outgoingThroughputLimiter = outgoingThroughputLimiter;
        this.incomingThroughputLimiter = incomingThroughputLimiter;
    }

    public ThroughputLimiter getOutgoingThroughputLimiter() {
        return outgoingThroughputLimiter;
    }

    public ThroughputLimiter getIncomingThroughputLimiter() {
        return incomingThroughputLimiter;
    }
}
