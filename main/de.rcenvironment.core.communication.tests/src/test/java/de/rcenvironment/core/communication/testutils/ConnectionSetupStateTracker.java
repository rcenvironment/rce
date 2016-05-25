/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListener;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListenerAdapter;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;

/**
 * A simple {@link ConnectionSetupListener} for unit/integration tests. It stores all state changes that match the given
 * {@link ConnectionSetup} in a queue, which can then be queried for the expected sequence.
 * 
 * @author Robert Mischke
 */
public class ConnectionSetupStateTracker extends ConnectionSetupListenerAdapter {

    private final ConnectionSetup relevantSetup;

    private BlockingQueue<ConnectionSetupState> queue = new LinkedBlockingDeque<ConnectionSetupState>();

    public ConnectionSetupStateTracker(ConnectionSetup cs) {
        this.relevantSetup = cs;
    }

    @Override
    public void onStateChanged(ConnectionSetup setup, ConnectionSetupState oldState, ConnectionSetupState newState) {
        if (setup != relevantSetup) {
            return;
        }
        queue.add(newState);
    }

    /**
     * Verifies the next received state change, with a timeout.
     * 
     * @param expectedState the next state change that should have been received
     * @param timeoutMsec the maximum timeout to wait for the state change
     * @throws InterruptedException on interruption
     * @throws AssertionError on a state expectation failure
     * @throws TimeoutException on a timeout
     */
    public void awaitAndExpect(ConnectionSetupState expectedState, int timeoutMsec) throws InterruptedException, AssertionError,
        TimeoutException {
        ConnectionSetupState result = queue.poll(timeoutMsec, TimeUnit.MILLISECONDS);
        if (result == null) {
            throw new TimeoutException("Queue returned 'null'");
        }
        assertEquals(relevantSetup.getDisplayName(), expectedState, result);
    }

}
