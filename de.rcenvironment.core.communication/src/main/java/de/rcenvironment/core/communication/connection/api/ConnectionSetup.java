/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.api;

import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Represents a connection setup, ie a configured network destination that a logical message channel can be established to.
 * 
 * @author Robert Mischke
 */
public interface ConnectionSetup {

    /**
     * @return the current {@link ConnectionSetupState} of the connection
     */
    ConnectionSetupState getState();

    /**
     * @return the reason for the last disconnect; only non-null in the DISCONNECTING and DISCONNECTED {@link ConnectionSetupState}s.
     */
    DisconnectReason getDisconnectReason();

    /**
     * Initiates an synchronous connection attempt.
     * 
     * @throws CommunicationException on connection errors
     */
    void connectSync() throws CommunicationException;

    /**
     * Signals that an active connection is desired; may trigger an asynchronous connection attempt.
     */
    void signalStartIntent();

    /**
     * Signals that an active connection is not desired (anymore); may trigger an asynchronous disconnect.
     */
    void signalStopIntent();

    /**
     * Utility method for integration tests that waits until the connection has reached the given state, or the timeout has elapsed.
     * 
     * @param targetState the state to wait for
     * @param timeoutMsec the maximum time to wait
     * @throws TimeoutException if the wait time is exceeded
     * @throws InterruptedException on thread interruption
     */
    void awaitState(ConnectionSetupState targetState, int timeoutMsec) throws TimeoutException, InterruptedException;

    /**
     * @return the display name specified on creation
     */
    String getDisplayName();

    /**
     * @return the numeric, JVM-unique id of this setup; for use by console commands, for example
     */
    long getId();

    /**
     * @return true if this connection should automatically try to connect on instance startup
     */
    boolean getConnnectOnStartup();

    /**
     * @return the string definition of the {@link NetworkContactPoint} to connect to
     */
    String getNetworkContactPointString();

    /**
     * @return the currently associated {@link MessageChannel}; only non-null in CONNECTED and DISCONNECTED {@link ConnectionSetupState}s
     */
    MessageChannel getCurrentChannel();

    /**
     * @return the id of the currently associated {@link MessageChannel}; only non-null when CONNECTED
     */
    String getCurrentChannelId();

    /**
     * @return the id of the last associated {@link MessageChannel}; may be null
     */
    String getLastChannelId();

    /**
     * @param netCP the {@link NetworkContactPoint} to compare to
     * @return true iff host and port are equal to the ones in this setup.
     */
    boolean equalsHostAndPort(NetworkContactPoint netCP);

}
