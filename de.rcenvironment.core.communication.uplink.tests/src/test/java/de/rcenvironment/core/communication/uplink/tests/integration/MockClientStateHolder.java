/*
 * Copyright 2019-2020 DLR, Germany
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
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolErrorType;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;

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

    private CountDownLatch sessionActivationLatch = new CountDownLatch(1);

    private Map<String, String> customHandshakeParameters;

    private boolean sessionInFinalState;

    private String sessionErrorMessage;

    // a general "something went wrong" flag that can be used to actively fail a surrounding test; not globally tested so far
    private boolean inconsistentStateFlag;

    MockClientStateHolder(Function<ToolExecutionRequest, MockToolExecutionProvider> actualToolExecutionRequestHandler) {
        this.actualToolExecutionRequestHandler = actualToolExecutionRequestHandler;
    }

    @Override
    public void onSessionReady(String namespaceId, String destinationIdPrefix) {
        assignedDestinationIdPrefix = destinationIdPrefix;
        if (sessionActiveState) { // consistency check
            inconsistentStateFlag = true;
            throw new IllegalStateException("Received more than one activation event");
        }
        sessionActiveState = true;
        sessionActivationLatch.countDown();
    }

    @Override
    public void onSessionTerminating() {
        if (!sessionActiveState) { // consistency check
            inconsistentStateFlag = true;
            throw new IllegalStateException("Received more than one activation event");
        }
        sessionActiveState = false;
    }

    @Override
    public synchronized void registerConnectionOrSessionError(UplinkProtocolErrorType errorType, String errorMessage) {
        Objects.requireNonNull(errorType); // consistency check
        if (sessionErrorMessage != null) {
            inconsistentStateFlag = true;
            throw new IllegalStateException(
                "Received more than one connection error event; first: '" + sessionErrorMessage + "', additional: '" + errorMessage + "'");
        }
        sessionErrorMessage = errorMessage;
    }

    @Override
    public synchronized void onSessionInFinalState() {
        this.sessionInFinalState = true;
        sessionActivationLatch.countDown();
    }

    public synchronized boolean isSessionActive() {
        return sessionActiveState;
    }

    public synchronized void setSessionErrorMessage(String sessionErrorMessage) {
        this.sessionErrorMessage = sessionErrorMessage;
    }

    public synchronized boolean isSessionInFinalState() {
        return sessionInFinalState;
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

    public void waitForSessionActivation(long timeoutMsec) throws InterruptedException, TimeoutException {
        if (!sessionActivationLatch.await(timeoutMsec, TimeUnit.MILLISECONDS)) {
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

}
